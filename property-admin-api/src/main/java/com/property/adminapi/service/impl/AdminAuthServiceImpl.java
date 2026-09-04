package com.property.adminapi.service.impl;

import com.property.adminapi.dto.request.LoginRequest;
import com.property.adminapi.dto.response.LoginResponse;
import com.property.adminapi.service.AdminAuthService;
import com.property.adminapi.service.SysUserService;
import com.property.common.constant.RoleConstant;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.framework.web.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final SysUserService sysUserService;
    private final TokenService tokenService;

    @Override
    public LoginResponse login(LoginRequest request) {
        return sysUserService.login(request);
    }

    @Override
    public String refresh(String refreshToken) {
        String newAccess = tokenService.refresh(refreshToken, RoleConstant.ADMIN);
        if (newAccess == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "刷新失败，请重新登录");
        }
        return newAccess;
    }

    @Override
    public void logout(String accessToken) {
        Long userId = null;
        try {
            if (accessToken != null) {
                userId = tokenService.getJwtUtilUserId(accessToken);
            }
        } catch (Exception ignored) {
        }
        tokenService.logout(accessToken, RoleConstant.ADMIN, userId);
    }
}