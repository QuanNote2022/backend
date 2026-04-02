package com.mineral.controller;

import com.mineral.common.ApiResponse;
import com.mineral.dto.ChatMessageResponse;
import com.mineral.dto.ChatSessionResponse;
import com.mineral.dto.CreateSessionRequest;
import com.mineral.dto.SendMessageRequest;
import com.mineral.service.ChatService;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

/**
 * 聊天控制器
 * 处理聊天会话管理、消息发送等请求
 */
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 创建聊天会话
     * @param request 创建会话请求
     * @param httpRequest HTTP 请求（用于获取用户 ID）
     * @return 创建的会话信息
     */
    @PostMapping("/session")
    public ApiResponse<ChatSessionResponse> createSession(
            @Valid @RequestBody CreateSessionRequest request,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        ChatSessionResponse response = chatService.createSession(request, userId);
        return ApiResponse.success("创建成功", response);
    }

    /**
     * 获取用户会话列表
     * @param page 当前页码
     * @param pageSize 每页大小
     * @param httpRequest HTTP 请求
     * @return 会话列表（分页）
     */
    @GetMapping("/sessions")
    public ApiResponse<Object> getSessions(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        
        com.mineral.common.PageQuery pageQuery = new com.mineral.common.PageQuery();
        pageQuery.setPage(page);
        pageQuery.setPageSize(pageSize);
        
        Object result = chatService.getSessions(userId, pageQuery);
        return ApiResponse.success(result);
    }

    /**
     * 获取指定会话的消息列表
     * @param sessionId 会话 ID
     * @param httpRequest HTTP 请求
     * @return 消息列表
     */
    @GetMapping("/session/{sessionId}/messages")
    public ApiResponse<List<ChatMessageResponse>> getSessionMessages(
            @PathVariable String sessionId,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        List<ChatMessageResponse> messages = chatService.getSessionMessages(sessionId, userId);
        return ApiResponse.success(messages);
    }

    /**
     * 删除指定会话
     * @param sessionId 会话 ID
     * @param httpRequest HTTP 请求
     * @return 删除结果
     */
    @DeleteMapping("/session/{sessionId}")
    public ApiResponse<Void> deleteSession(
            @PathVariable String sessionId,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        chatService.deleteSession(sessionId, userId);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 发送消息（使用 SSE 流式返回）
     * @param sessionId 会话 ID
     * @param request 发送消息请求
     * @param httpRequest HTTP 请求
     * @return SSE 发射器
     */
    @PostMapping(value = "/session/{sessionId}/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(
            @PathVariable String sessionId,
            @Valid @RequestBody SendMessageRequest request,
            HttpServletRequest httpRequest) {
        log.info("发送消息：sessionId={}, content={}", sessionId, request.getContent());
        String userId = (String) httpRequest.getAttribute("userId");
        
        SseEmitter emitter = new SseEmitter(0L);
        
        try {
            sendSseResponse(emitter, sessionId, request.getContent(), userId);
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        
        return emitter;
    }

    /**
     * 发送 SSE 响应（私有方法）
     * @param emitter SSE 发射器
     * @param sessionId 会话 ID
     * @param content 消息内容
     * @param userId 用户 ID
     * @throws IOException IO 异常
     */
    private void sendSseResponse(SseEmitter emitter, String sessionId, String content, String userId) 
            throws IOException {
        log.info("发送 SSE 响应：sessionId={}, content={}", sessionId, content);

        // 发送初始思考状态
        emitter.send(SseEmitter.event()
                .name("message")
                .data("{\"token\": \"正在思考...\"}"));

        String[] tokens = {"您", "好", "！"};

        for (String token : tokens) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // 发送 JSON 格式的 token（转义特殊字符）
            String jsonData = String.format("{\"token\": \"%s\"}", token.replace("\"", "\\\""));
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(jsonData));
        }

        // 发送完成信号
        String messageId = "assistant_" + System.currentTimeMillis();
        emitter.send(SseEmitter.event()
                .name("done")
                .data(String.format("{\"done\": true, \"messageId\": \"%s\"}", messageId)));

        log.info("发送完成");
        emitter.complete();
    }
}
