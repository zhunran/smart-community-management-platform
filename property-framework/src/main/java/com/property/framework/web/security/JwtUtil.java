package com.property.framework.web.security;

import com.property.common.dto.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * Access Token(type=access,短效)用于业务请求鉴权
 * Refresh Token(type=Refresh，长效)用于刷新Access Token
 * 负责 Token 的生成、解析、校验
 */
@Component
@ConditionalOnProperty(name = "jwt.secret")
public class JwtUtil {

    /** Token 类型 claim 名 */
    public static final String CLAIM_TYPE = "type";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;
    private final long expiration;         // Access Token 有效期（毫秒）
    @Getter
    private final long refreshExpiration;  // Refresh Token 有效期（毫秒）

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration,
                   @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;
    }

    public long getAccessExpiration() {
        return expiration;
    }

    /**
     * 生成 Access Token（含用户信息，type=access）
     */
    public String generateAccessToken(LoginUser loginUser) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(loginUser.getUserId()))
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim("username", loginUser.getUsername())
                .claim("realName", loginUser.getRealName())
                .claim("role", loginUser.getRole())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 生成 Refresh Token（仅含 userId，type=refresh）
     */
    public String generateRefreshToken(LoginUser loginUser) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(loginUser.getUserId()))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 Token → Claims（签名/过期校验失败会抛异常）
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 解析 Access Token → LoginUser
     * 若 Token 类型不是 access，抛 JwtException（防止 Refresh Token 用于业务请求）
     */
    public LoginUser getLoginUser(String token) {
        Claims claims = parseToken(token);
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("非法的 Token 类型，要求 access");
        }
        return LoginUser.builder()
                .userId(Long.valueOf(claims.getSubject()))
                .username(claims.get("username", String.class))
                .realName(claims.get("realName", String.class))
                .role(claims.get("role", String.class))
                .token(token)
                .build();
    }

    /**
     * 从任意 Token 中提取 userId（不校验类型）
     */
    public Long getUserId(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }

    /**
     * 判断 Token 是否为指定类型
     */
    public boolean isType(String token, String type) {
        try {
            return type.equals(parseToken(token).get(CLAIM_TYPE, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 获取 Token 剩余有效期（毫秒），已过期返回 0
     */
    public long getRemainingMillis(String token) {
        Date exp = parseToken(token).getExpiration();
        long remaining = exp.getTime() - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }

    /**
     * 校验 Token 是否有效（签名 + 未过期）
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

}
