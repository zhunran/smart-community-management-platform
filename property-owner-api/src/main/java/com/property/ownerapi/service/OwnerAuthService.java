package com.property.ownerapi.service;

import com.property.ownerapi.dto.request.OwnerLoginRequest;
import com.property.ownerapi.dto.response.OwnerLoginResponse;

/**
 * 业主认证服务接口
 */
public interface OwnerAuthService {

    /**
     * 业主手机号+密码登录
     */
    OwnerLoginResponse login(OwnerLoginRequest request);

    /**
     * 校验验证码
     * @param sessionCaptcha Session 中存储的验证码（可能为 null）
     * @param inputCaptcha 用户输入的验证码
     * @throws com.property.common.exception.BusinessException 验证码错误时抛出
     */
    void verifyCaptcha(String sessionCaptcha, String inputCaptcha);
}
