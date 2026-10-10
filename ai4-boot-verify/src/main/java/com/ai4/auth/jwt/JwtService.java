package com.ai4.auth.jwt;

import com.ai4.common.exception.ErrorCode;
import com.ai4.common.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * 负责 JWT 的生成、验签和解析。
 */
@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        // 使用配置中的密钥创建 HMAC 签名密钥。
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 生成包含用户身份和过期时间的 JWT。
     */
    public String generateToken(long userId, String username) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.expireSeconds());

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 验证并解析 JWT，失败时转换为统一认证异常。
     */
    public CurrentUser parseToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Long userId = claims.get("userId", Long.class);
            String username = claims.getSubject();

            return new CurrentUser(userId, username);
        } catch (
                ExpiredJwtException exception) {
            throw new UnauthorizedException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException |
                 IllegalArgumentException exception) {
            throw new UnauthorizedException(ErrorCode.INVALID_TOKEN);
        }
    }

    /**
     * 返回配置的 Token 有效期，单位为秒。
     */
    public long expireSeconds() {
        return jwtProperties.expireSeconds();
    }
}
