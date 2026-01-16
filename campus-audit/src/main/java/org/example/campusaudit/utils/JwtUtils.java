package org.example.campusaudit.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {
    @Value("${jwt.secret.key}")
    private String secret;

    // 0.12.0 要求密钥必须是 Key 对象，而不是简单的 String
    private SecretKey getSigningKey() {
        // 直接使用字符串的字节数组，不要使用 BASE64 解码
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // 注意：明文长度必须达到 32 字节（256位）才能支持 HS256 算法
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 解析并验证 Token
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // 代替旧版的 setSigningKey
                .build()                     // 先构建 Parser
                .parseSignedClaims(token)    // 代替旧版的 parseClaimsJws
                .getPayload();               // 代替旧版的 getBody
    }

    /**
     * 生成 Token
     */
    public String createToken(String userId, String role) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("roleType", role)
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(getSigningKey())
                .compact(); // 使用 SecretKey 对象签名
    }
}
