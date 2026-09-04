package com.property.ownerapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.property.common.constant.RoleConstant;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.module.owner.entity.OwnerEntity;
import com.property.module.owner.repository.OwnerMapper;
import com.property.ownerapi.dto.request.OwnerLoginRequest;
import com.property.ownerapi.dto.response.OwnerLoginResponse;
import com.property.ownerapi.service.OwnerAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;

/**
 * 业主认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OwnerAuthServiceImpl implements OwnerAuthService {

    private final OwnerMapper ownerMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public OwnerLoginResponse login(OwnerLoginRequest request) {
        // 1. 根据手机号查询业主
        OwnerEntity owner = ownerMapper.selectOne(
                new LambdaQueryWrapper<OwnerEntity>()
                        .eq(OwnerEntity::getPhone, request.getPhone())
                        .eq(OwnerEntity::getDelFlag, 0)
        );

        // 2. 校验业主是否存在
        if (owner == null) {
            log.warn("业主登录失败：手机号未注册 [phone={}]", request.getPhone());
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "手机号或密码错误");
        }

        // 3. 校验业主状态
        if (owner.getStatus() == null || owner.getStatus() != 1) {
            log.warn("业主登录失败：账户已被禁用 [phone={}, status={}]", request.getPhone(), owner.getStatus());
            throw new BusinessException(ErrorCode.USER_DISABLED, "该账户已被禁用，请联系物业管理员");
        }

        // 4. 校验密码（BCrypt）
        if (!passwordEncoder.matches(request.getPassword(), owner.getPassword())) {
            log.warn("业主登录失败：密码错误 [phone={}]", request.getPhone());
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "手机号或密码错误");
        }

        // 5. 更新最后登录时间
        owner.setLastLoginTime(LocalDateTime.now());
        ownerMapper.updateById(owner);

        log.info("业主登录成功 [ownerId={}, phone={}, name={}]", owner.getId(), owner.getPhone(), owner.getOwnerName());

        // 6. 返回响应（Token 由 Controller 层签发）
        return OwnerLoginResponse.builder()
                .ownerId(owner.getId())
                .ownerName(owner.getOwnerName())
                .phone(owner.getPhone())
                .role(RoleConstant.OWNER)
                .build();
    }

    @Override
    public void verifyCaptcha(String sessionCaptcha, String inputCaptcha) {
        if (sessionCaptcha == null || !sessionCaptcha.equalsIgnoreCase(inputCaptcha.trim())) {
            throw new BusinessException(ErrorCode.CAPTCHA_ERROR);
        }
    }
}
