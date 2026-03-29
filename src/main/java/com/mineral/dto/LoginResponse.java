package com.mineral.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    /**
     * JWT 令牌
     */
    private String token;
    
    /**
     * 过期时间（秒）
     */
    private Long expiresIn;
}
