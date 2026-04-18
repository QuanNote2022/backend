package com.mineral.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendMessageRequest {
    @NotBlank(message = "消息内容不能为空")
    private String content;

    private String mineralContext;

    private java.util.List<String> documentIds;
}