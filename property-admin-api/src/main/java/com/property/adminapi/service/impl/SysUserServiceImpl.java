package com.property.adminapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.property.adminapi.dto.request.LoginRequest;
import com.property.adminapi.dto.response.LoginResponse;
import com.property.adminapi.service.SysUserService;
import com.property.common.constant.RoleConstant;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.framework.entity.SysUserEntity;
import com.property.framework.repository.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 系统用户服务实现
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private static final Logger log = LoggerFactory.getLogger(SysUserServiceImpl.class);

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 根据用户名查询用户
        SysUserEntity user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUserEntity>()
                        .eq(SysUserEntity::getUsername, request.getUsername())
                        .eq(SysUserEntity::getDelFlag, 0)
        );

        // 2. 校验用户是否存在
        if (user == null) {
            log.warn("登录失败：用户不存在 [username={}]", request.getUsername());
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户名或密码错误");
        }

        // 3. 校验用户状态
        if (user.getStatus() == null || user.getStatus() == 0) {
            log.warn("登录失败：用户已被禁用 [username={}]", request.getUsername());
            throw new BusinessException(ErrorCode.USER_DISABLED, "该账户已被禁用，请联系管理员");
        }

        // 4. 校验密码（BCrypt）
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("登录失败：密码错误 [username={}]", request.getUsername());
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "用户名或密码错误");
        }

        // 5. 校验通过，返回响应（Token 由 Controller 层签发）
        log.info("登录成功 [userId={}, username={}]", user.getId(), user.getUsername());

        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(getRoleByUserType(user.getUserType()))
                .userType(user.getUserType())
                .build();
    }

    /**
     * 根据用户类型获取角色标识
     */
    private String getRoleByUserType(Integer userType) {
        return switch (userType) {
            case 1 -> RoleConstant.ADMIN;//超级管理员
            case 2 -> RoleConstant.ADMIN;//物业管理员
            case 3 -> RoleConstant.FINANCE;
            case 4 -> RoleConstant.SERVICE;
            case 5 -> RoleConstant.MAINTENANCE;
            default -> RoleConstant.GUEST;
        };
    }
}
