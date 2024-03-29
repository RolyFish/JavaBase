package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author rolyfish

 */
public interface IUserInfoService extends IService<UserInfo> {

    Result infoDetailById(Long userId);
}
