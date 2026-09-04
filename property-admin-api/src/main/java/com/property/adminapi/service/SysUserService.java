package com.property.adminapi.service;

import com.property.adminapi.dto.request.LoginRequest;
import com.property.adminapi.dto.response.LoginResponse;

/**
 * 系统用户服务接口
 */
public interface SysUserService {

    /**
     * 管理员登录
     */
    LoginResponse login(LoginRequest request);
}
