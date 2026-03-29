package com.mineral.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送消息请求 DTO
 */
@Data
public class SendMessageRequest {
    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String content;
    
    /**
     * 矿物上下文
     */
    private String mineralContext;
}
