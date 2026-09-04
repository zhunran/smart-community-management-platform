package com.property.framework.web.security;


import com.property.common.dto.LoginUser;
import com.property.framework.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(JwtUtil.class)
public class TokenService {
    private static final String REFRESH_KEY_PREFIX="token:refresh:";
    private static final String BLACKLIST_KEY_PREFIX="token:blacklist:";

    public String refreshKey(String role,Long userId)
    {
        return REFRESH_KEY_PREFIX+role+":"+userId;
    }

    public String blacklistKey(String accessToken)
    {
        return  BLACKLIST_KEY_PREFIX+accessToken;
    }

    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    //双Token结果
        public record TokenPair(String accessToken, String refreshToken) {
    }
    /**
     * 登陆成功后签发双token，并把Refresh Token存入Redis
     */
    public TokenPair issue(LoginUser loginUser)
    {
        String accessToken=jwtUtil.generateAccessToken(loginUser);
        String refreshToken=jwtUtil.generateRefreshToken(loginUser);

        String refreshKey=refreshKey(loginUser.getRole(),loginUser.getUserId());
        redisUtil.set(refreshKey,refreshToken, Duration.ofMillis(jwtUtil.getRefreshExpiration()));

        return new TokenPair(accessToken,refreshToken);
    }

    /**
     * 用Refresh Token 刷新Access Token
     * 校验：签名有效+type=refresh+与Redis中存储的一致
     *
     * @return 新的Access Token，校验失败返回null
     */
    public String refresh(String refreshToken,String role)
    {
        if (refreshToken==null||!jwtUtil.isType(refreshToken,JwtUtil.TYPE_REFRESH))
        {
            return null;
        }
        Long userId =jwtUtil.getUserId(refreshToken);
        String refreshKey=refreshKey(role,userId);
        Object stored =redisUtil.get(refreshKey);
        //Redis中不存在（已登出/已过期）或与传入不一致→拒绝
        if (stored ==null||!refreshToken.equals(stored.toString()))
        {
            return null;
        }
        //重新构造LoginUser（Refresh Token 仅含userId，业务信息在此处可按需补全）
        LoginUser loginUser= LoginUser.builder()
                .userId(userId)
                .role(role)
                .build();
        return jwtUtil.generateAccessToken(loginUser);
    }
    /**
     * 登出：删除Refresh Token+Access Token 加入黑名单
     */
    public void logout(String accessToken,String role,Long userId) {
        //删除Redis中的Refresh Token
        if (userId != null){
            redisUtil.delete(refreshKey(role,userId));
        }
        //Access Token 加入黑名单，TTL=剩余有效期
        if (accessToken!=null&&jwtUtil.validateToken(accessToken))
        {
            long remaining=jwtUtil.getRemainingMillis(accessToken);
            if (remaining>0)
            {
                redisUtil.set(blacklistKey(accessToken),"1",Duration.ofMillis(remaining));
            }
        }
    }
    /**
     * 判断Access Token是否在黑名单中
     */
    public boolean isBlacklisted(String accessToken)
    {
        return  Boolean.TRUE.equals(redisUtil.hasKey(blacklistKey(accessToken)));
    }

    /**
     * Access Token Cookie的maxAge
     */
    public int getAccessMaxAgeSeconds()
    {
        return (int)(jwtUtil.getAccessExpiration()/1000);
    }
    /**
     * Refresh Token Cookie的maxAge
     */
    public int getRefreshMaxAgeSeconds()
    {
        return (int)(jwtUtil.getRefreshExpiration()/1000);
    }
    /**
     * 从Access Token 中取userId（登出对是定位Refresh key用）
     */
    public  Long getJwtUtilUserId(String accessToken)
    {
        return  jwtUtil.getUserId(accessToken);
    }
}
