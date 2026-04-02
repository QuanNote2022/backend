package com.mineral.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 * 用于生成、解析和验证 JWT Token
 */
@Slf4j
@Component
public class JwtUtil {

    /**
     * JWT 密钥，从配置文件读取
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * JWT 过期时间（毫秒），从配置文件读取
     * 默认 24 小时：86400000
     */
    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * JWT 签名密钥
     */
    private SecretKey key;

    /**
     * 初始化方法，在 Bean 创建后执行
     * 根据密钥字符串生成签名密钥
     */
    @PostConstruct
    public void init() {
        // 使用 HMAC-SHA 算法生成密钥
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     * 
     * @param userId 用户 ID
     * @param username 用户名
     * @return 生成的 JWT 字符串
     */
    public String generateToken(String userId, String username) {
        // 创建 JWT 声明
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);      // 用户 ID
        claims.put("username", username);  // 用户名
        return createToken(claims, userId);
    }

    /**
     * 创建 JWT Token
     * 
     * @param claims JWT 声明
     * @param subject JWT 主题（用户 ID）
     * @return 生成的 JWT 字符串
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();                          // 当前时间
        Date expirationDate = new Date(now.getTime() + expiration);  // 过期时间

        // 构建 JWT
        return Jwts.builder()
                .claims(claims)                      // 设置声明
                .subject(subject)                    // 设置主题
                .issuedAt(now)                       // 设置签发时间
                .expiration(expirationDate)          // 设置过期时间
                .signWith(key)                       // 签名
                .compact();                          // 压缩 JWT
    }

    /**
     * 解析 JWT Token
     * 
     * @param token JWT 字符串
     * @return Claims 对象，包含用户信息；解析失败返回 null
     */
    public Claims parseToken(String token) {
        try {
            // 解析 JWT 并获取声明
            return Jwts.parser()
                    .verifyWith(key)     // 设置签名密钥
                    .build()                // 构建解析器
                    .parseSignedClaims(token)  // 解析 JWT 并获取 Claims
                    .getPayload();             // 获取声明
          } catch (Exception e) {
             // 记录解析失败的日志
            log.error("Parse token error: {}", e.getMessage());
            return null;
         }
    }

    /**
     * 从 JWT Token 中获取用户 ID
     * 
     * @param token JWT 字符串
     * @return 用户 ID；解析失败返回 null
     */
    public String getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("userId", String.class) : null;
    }

    /**
     * 从 JWT Token 中获取用户名
     * 
     * @param token JWT 字符串
     * @return 用户名；解析失败返回 null
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("username", String.class) : null;
    }

    /**
     * 验证 JWT Token 是否有效
     * 
     * @param token JWT 字符串
     * @return true - 有效；false - 无效或已过期
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            // Token 有效且未过期
            return claims != null && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
