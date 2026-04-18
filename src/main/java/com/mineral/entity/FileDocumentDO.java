package com.mineral.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_documents")
public class FileDocumentDO {
    @TableId(type = IdType.ASSIGN_ID)
    private String documentId;

    private String sessionId;

    private String userId;

    private String fileName;

    private String fileUrl;

    private String fileType;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}