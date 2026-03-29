package com.mineral.dto;

import lombok.Data;

/**
 * 用户资料响应 DTO
 */
@Data
public class UserProfileResponse {
    /**
     * 用户 ID
     */
    private String userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 头像 URL
     */
    private String avatar;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 创建时间
     */
    private String createdAt;
}
