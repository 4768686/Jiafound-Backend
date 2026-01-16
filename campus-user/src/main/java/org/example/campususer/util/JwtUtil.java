package org.example.campususer.util;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.example.campususer.config.JwtConfig;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

/**
 * JWT 工具类
 * 用于解析和验证 JWT Token
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtConfig jwtConfig;

    /**
     * 从 Token 中解析出 userId
     * @param token JWT Token
     * @return userId
     */
    public String getUserIdFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
            
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getSubject();
        } catch (Exception e) {
            throw new RuntimeException("Token 解析失败: " + e.getMessage());
        }
    }

    /**
     * 从 Token 中解析出 roleType
     * @param token JWT Token
     * @return roleType (User, Admin, SuperAdmin)
     */
    public String getRoleTypeFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
            
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.get("roleType", String.class);
        } catch (Exception e) {
            throw new RuntimeException("Token 解析失败: " + e.getMessage());
        }
    }

    /**
     * 验证 Token 是否有效
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
            
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从请求头中提取并解析 userId
     * @param authHeader Authorization 请求头的值
     * @return userId
     */
    public String getUserIdFromHeader(String authHeader) {
        String token = jwtConfig.extractToken(authHeader);
        if (token == null) {
            throw new RuntimeException("无效的 Authorization 头");
        }
        return getUserIdFromToken(token);
    }
}
