package com.property.adminapi.service;

import com.property.adminapi.dto.response.LoginResponse;
import com.property.adminapi.dto.request.LoginRequest;

/**
 * 管理员认证服务
 */
public interface AdminAuthService {

    /**
     * 管理员登录：校验用户 → 返回登录响应
     */
    LoginResponse login(LoginRequest request);

    /**
     * 刷新Access Token：返回新的Access Token
     */
    String refresh(String refreshToken);

    /**
     * 管理员登出：吊销Token
     */
    void logout(String accessToken);
}