package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.BlogDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.hmdp.utils.RedisConstants.FEED_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author rolyfish
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IFollowService followService;

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        // Page<Blog> page = query()
        //        .orderByDesc("liked")
        //        .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));

        final Page<Blog> page = page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE),
                new LambdaQueryWrapper<Blog>()
                        .orderByDesc(Blog::getLiked));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户 信息 设置 userName和userIcon
        //records.forEach(blog -> {
        //    this.queryBlogUser(blog);
        //    this.isBlogLiked(blog);
        //});

        this.queryBlogUser(records);
        this.isBlogLiked(records);

        return Result.ok(records);
    }

    @Override
    public Result queryBlogById(Long id) {
        // 1.查询blog
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在！");
        }
        // 2.查询blog有关的用户
        queryBlogUser(blog);
        // 3.查询blog是否被点赞
        isBlogLiked(Collections.singletonList(blog));
        return Result.ok(blog);
    }

    @Override
    @Transactional
    public Result likeBlog(Long id) {
        // 1.获取登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.判断当前登录用户是否已经点赞
        String key = BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null) {
            // 3.如果未点赞，可以点赞
            // 3.1.数据库点赞数 + 1
            //boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            boolean isSuccess = update(new LambdaUpdateWrapper<Blog>().setSql("liked = liked + 1").eq(Blog::getId, id));
            // 3.2.保存用户到Redis的set集合  zadd key value score
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        } else {
            // 4.如果已点赞，取消点赞
            // 4.1.数据库点赞数 -1
            //boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            boolean isSuccess = update(new LambdaUpdateWrapper<Blog>().setSql("liked = liked - 1").eq(Blog::getId, id));
            // 4.2.把用户从Redis的set集合移除
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        String key = BLOG_LIKED_KEY + id;
        // 1.查询top5的点赞用户 zrange key 0 4
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        //final Set<String> execute = stringRedisTemplate.execute(new DefaultRedisScript<>("return redis.call('zrange' , KEYS[1] ,ARGV[1] ,'0' ,'byscore' ,'rev' ,'limit', ARGV[2] ,ARGV[3])", Set.class), Collections.singletonList(key), String.valueOf(System.currentTimeMillis()), String.valueOf(0), String.valueOf(5));
        if (top5 == null || top5.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 2.解析出其中的用户id
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr = StrUtil.join(",", ids);
        // 3.根据用户id查询用户 WHERE id IN ( 5 , 1 ) ORDER BY FIELD(id, 5, 1)
        List<UserDTO> userDTOS = userService.query()
                .in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        // 4.返回
        return Result.ok(userDTOS);
    }

    @Override
    public Result saveBlog(BlogDTO blogDTO) {
        // 0 blogDTO 转 Blog
        final Blog blog = BeanUtil.copyProperties(blogDTO, Blog.class, "id");

        // 1.获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 2.保存探店笔记
        boolean isSuccess = save(blog);
        if (!isSuccess) {
            return Result.fail("新增笔记失败!");
        }
        // 笔记创建时间
        final long saveTime = System.currentTimeMillis();
        // 3.查询笔记作者的所有粉丝 select * from tb_follow where follow_user_id = ?
        //List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
        List<Follow> follows = followService.list(new LambdaQueryWrapper<Follow>()
                .select(Follow::getUserId)
                .eq(ObjectUtil.isNotEmpty(user), Follow::getFollowUserId, user.getId()));
        // 4.推送笔记id给所有粉丝
        for (Follow follow : follows) {
            // 4.1.获取粉丝id
            Long userId = follow.getUserId();
            // 4.2.推送
            String key = FEED_KEY + userId;
            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), saveTime);
        }
        // 5.返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        // 1.获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 2.查询收件箱 ZREVRANGEBYSCORE key Max Min LIMIT offset count
        String key = FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        // 3.非空判断
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }
        // 4.解析数据：blogId、minTime（时间戳）、offset
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0; // 2
        int os = 1; // 2
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) { // 5 4 4 2 2
            // 4.1.获取id
            ids.add(Long.valueOf(Objects.requireNonNull(tuple.getValue())));
            // 4.2.获取分数(时间戳）
            long time = Objects.requireNonNull(tuple.getScore()).longValue();
            if (time == minTime) {
                os++;
            } else {
                minTime = time;
                os = 1;
            }
        }
        // 5.根据id查询blog
        String idStr = StrUtil.join(",", ids);
        List<Blog> blogs = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        // 5.1.查询blog有关的用户
        queryBlogUser(blogs);
        // 5.2.查询blog是否被点赞
        isBlogLiked(blogs);
        //for (Blog blog : blogs) {
        //    queryBlogUser(blog);
        //    isBlogLiked(blog);
        //}
        // 6.封装并返回
        ScrollResult r = new ScrollResult();
        r.setList(blogs);
        r.setOffset(os);
        r.setMinTime(minTime);
        return Result.ok(r);
    }

    @Override
    public Result queryMyBlogPage(Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        final Page<Blog> page = this.page(new Page<>(Long.valueOf(current), SystemConstants.MAX_PAGE_SIZE),
                new LambdaQueryWrapper<Blog>()
                        .eq(Blog::getUserId, user.getId()));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        this.isBlogLiked(records);
        final List<BlogDTO> blogDTOS = BeanUtil.copyToList(records, BlogDTO.class);
        return Result.ok(blogDTOS);
    }

    private void isBlogLiked(Blog blog) {
        // 1.获取登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 用户未登录，无需查询是否点赞
            return;
        }
        Long userId = user.getId();
        // 2.判断当前登录用户是否已经点赞
        String key = "blog:liked:" + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

    /**
     * 笔记是否被点赞过
     *
     * @param blogList 日记集合
     */
    private void isBlogLiked(List<Blog> blogList) {
        // 1.获取登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 用户未登录，无需查询是否点赞
            return;
        }
        Long userId = user.getId();
        blogList.forEach(blogTemp -> {
            String key = "blog:liked:" + blogTemp.getId();
            Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
            // 2.判断当前登录用户是否已经点赞
            blogTemp.setIsLike(score != null);
        });
    }

    /**
     * @param blog 日记
     */
    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    /**
     * 减少io次数 一次性更新 blogList
     */
    private void queryBlogUser(List<Blog> blogList) {
        // 查询用户信息
        final Set<Long> userIds = blogList.stream()
                .collect(HashSet::new, (idSet, blog) -> idSet.add(blog.getUserId()), HashSet::addAll);
        final HashMap<Long, User> userMap = new LambdaQueryChainWrapper<>(userService.getBaseMapper()).in(User::getId, userIds).list()
                .stream()
                .collect(HashMap::new, (userMapTemp, user) -> userMapTemp.put(user.getId(), user), HashMap::putAll);
        blogList.forEach(blog -> {
            final User user = userMap.get(blog.getUserId());
            if (user != null) {
                blog.setName(user.getNickName());
                blog.setIcon(user.getIcon());
            }
        });
    }
}
