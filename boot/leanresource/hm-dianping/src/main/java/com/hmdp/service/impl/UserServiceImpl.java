package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_CODE_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_CODE_TTL;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;
import static com.hmdp.utils.RedisConstants.USER_SIGN_KEY;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author rolyfish
 */
@Slf4j
@Setter
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.如果不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }
        // 3.符合，生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 4.保存验证码到 session
        redisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 5.发送验证码
        log.debug("发送短信验证码成功，验证码：{}", code);
        // 返回ok
        return Result.ok(code);
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.如果不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }
        // 3.从redis获取验证码并校验
        String cacheCode = redisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.equals(code)) {
            // 不一致，报错
            return Result.fail("验证码错误");
        }

        // 4.一致，根据手机号查询用户 select * from tb_user where phone = ?
        User user = query().eq("phone", phone).one();

        // 5.判断用户是否存在
        if (user == null) {
            // 6.不存在，创建新用户并保存
            user = createUserWithPhone(phone);
        }

        // 7.保存用户信息到 redis中
        // 7.1.随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString(true);
        // 7.2.将User对象转为HashMap存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        // 7.3.存储
        String tokenKey = LOGIN_USER_KEY + token;
        redisTemplate.opsForHash().putAll(tokenKey, userMap);
        // 7.4.设置token有效期
        redisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 8.返回token
        return Result.ok(token);
    }

    @Override
    public Result sign() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyy:MM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.写入Redis SETBIT key offset 1
        redisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        return signCount(LocalDateTime.now());
    }

    public Result signCount(LocalDateTime now) {
        // 0 获取当前用户id
        final Long userId = UserHolder.getUser().getId();
        // 1 获取当前为本月第几天
        //final Integer currentDay = now.getDayOfMonth();
        final String format = now.format(DateTimeFormatter.ofPattern(":yyyy:MM"));
        // 2 使用bitfield命令,获得本月所有签到记录(10进制返回) BITFIELD key [GET type offset]
        String key = USER_SIGN_KEY + userId + format;
        final List<Long> signs = redisTemplate.opsForValue().bitField(key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(now.getDayOfMonth()))
                        .valueAt(0));
        // 3 结果为空返回0
        if (signs == null || signs.size() == 0) {
            return Result.ok(0);
        }
        // 4 得到结果遍历
        Long sign = signs.get(0);
        if (sign == null || sign == 0) {
            return Result.ok(0);
        }
        int count = 0;
        while (true) {
            if ((sign & 1) == 0) {
                // 4.1 当天未签到 break 跳出循环 返回结果
                break;
            } else {
                // 4.2 当天签到 计数器加一
                count++;
                sign >>>= 1;
            }
        }
        return Result.ok(count);
    }

    //@Override
    //public Result signCount() {
    //    // 1.获取当前登录用户
    //    Long userId = UserHolder.getUser().getId();
    //    // 2.获取日期
    //    LocalDateTime now = LocalDateTime.now();
    //    // 3.拼接key
    //    String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
    //    String key = USER_SIGN_KEY + userId + keySuffix;
    //    // 4.获取今天是本月的第几天
    //    int dayOfMonth = now.getDayOfMonth();
    //    // 5.获取本月截止今天为止的所有的签到记录，返回的是一个十进制的数字 BITFIELD sign:5:202203 GET u14 0
    //    List<Long> result = stringRedisTemplate.opsForValue().bitField(
    //            key,
    //            BitFieldSubCommands.create()
    //                    .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
    //    );
    //    if (result == null || result.isEmpty()) {
    //        // 没有任何签到结果
    //        return Result.ok(0);
    //    }
    //    Long num = result.get(0);
    //    if (num == null || num == 0) {
    //        return Result.ok(0);
    //    }
    //    // 6.循环遍历
    //    int count = 0;
    //    while (true) {
    //        // 6.1.让这个数字与1做与运算，得到数字的最后一个bit位  // 判断这个bit位是否为0
    //        if ((num & 1) == 0) {
    //            // 如果为0，说明未签到，结束
    //            break;
    //        } else {
    //            // 如果不为0，说明已签到，计数器+1
    //            count++;
    //        }
    //        // 把数字右移一位，抛弃最后一个bit位，继续下一个bit位
    //        num >>>= 1;
    //    }
    //    return Result.ok(count);
    //}

    @Override
    public Result signMaxCount() {
        LocalDateTime now = LocalDateTime.now();
        List<Integer> signCountOfMouth = new ArrayList<>();
        final int dayOfMonth = now.getDayOfMonth();
        final LocalDateTime threshold = now.plusDays(-dayOfMonth + 1);
        while (now.isAfter(threshold)) {
            // 获取当日连续签到次数
            final Result result = signCount(now);
            int plusDays;
            if (result == null || result.getData() == null || (Integer) result.getData() == 0) {
                // 日期后移
                plusDays = -1;
                signCountOfMouth.add(0);
            } else {
                final Integer count = (Integer) result.getData();
                plusDays = -count;
                signCountOfMouth.add(count);
            }
            now = now.plusDays(plusDays);
        }
        final Integer max = Collections.max(signCountOfMouth);
        return Result.ok(max);
    }

    private User createUserWithPhone(String phone) {
        // 1.创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        // 2.保存用户
        save(user);
        return user;
    }

    @Override
    public Result createSecKillUserDate(Long num, String basephone) {

        final long basephoneLong = Long.parseLong(basephone);

        String phone;
        final List<String> tockens = new ArrayList<>();
        final int nums = Integer.parseInt(String.valueOf(num));

        final HashSet<String> phones = new HashSet<>();
        for (int i = 1; i <= nums; i++) {
            phone = Long.toString(basephoneLong + i);
            // 发送验证码
            final Result result = sendCode(phone, null);
            // 验证码
            final String code = (String) result.getData();
            final LoginFormDTO loginFormDTO = new LoginFormDTO();
            loginFormDTO.setCode(code);
            loginFormDTO.setPhone(phone);
            final boolean add = phones.add(phone);
            if (!add) {
                log.info("重复号码：" + phone);
            }
            loginFormDTO.setPassword("defaultpassword");
            final Result login = login(loginFormDTO, null);
            tockens.add(login.getData() + ",");
        }
        // 写入文件
        String filePath = "/Users/rolyfish/Desktop/software/apache-jmeter-5.5/jmetertestfile/tockens.txt";
        try (FileWriter fileWriter = new FileWriter(filePath, true)) {

            IOUtils.writeLines(tockens, System.lineSeparator(), fileWriter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }


    public void check() {
        String filePath = "/Users/rolyfish/Desktop/software/apache-jmeter-5.5/jmetertestfile/tockens.txt";
        try (FileReader fileReader = new FileReader(filePath)) {
            final List<String> tokenList = IOUtils.readLines(fileReader);
            tokenList.forEach(token -> {
                // 2.基于TOKEN获取redis中的用户
                Map<Object, Object> userMap = redisTemplate.opsForHash().entries(LOGIN_USER_KEY + token.substring(0, token.length() - 1));
                // 5.将查询到的hash数据转为UserDTO
                UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
                log.info(userDTO + "tocken:" + token);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
