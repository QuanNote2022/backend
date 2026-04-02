package com.mineral.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新个人资料请求 DTO
 */
@Data
public class UpdateProfileRequest {
    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 头像 URL
     */
    private String avatar;

    /**
     * 昵称
     */
    @Size(max = 20, message = "昵称长度不能超过 20 个字符")
    private String nickname;
}
