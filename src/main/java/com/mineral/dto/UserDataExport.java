package com.mineral.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserDataExport {
    /** 导出时间 */
    private String exportedAt;
    /** 用户信息 */
    private UserProfileResponse profile;
    /** 识别记录列表 */
    private List<DetectionHistoryResponse> detections;
    /** 问答会话列表（含消息） */
    private List<ChatSessionExport> chatSessions;

    @Data
    public static class ChatSessionExport {
        private ChatSessionResponse session;
        private List<ChatMessageResponse> messages;
    }
}
