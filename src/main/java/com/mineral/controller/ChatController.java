package com.mineral.controller;

import com.mineral.common.ApiResponse;
import com.mineral.dto.ChatMessageResponse;
import com.mineral.dto.ChatSessionResponse;
import com.mineral.dto.CreateSessionRequest;
import com.mineral.dto.SendMessageRequest;
import com.mineral.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

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
            sendSseResponse(emitter, sessionId, request.getContent(), userId, request.getMineralContext());
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
     * @param mineralContext 矿物上下文
     * @throws IOException IO 异常
     */
    private void sendSseResponse(SseEmitter emitter, String sessionId, String content, String userId, String mineralContext) 
            throws IOException {
        log.info("发送 SSE 响应：sessionId={}, content={}, mineralContext={}", sessionId, content, mineralContext);

        // 发送初始思考状态
        emitter.send(SseEmitter.event()
                .name("message")
                .data("{\"token\": \"正在思考...\"}"));

        try {
            // 调用 ChatService 获取 Ollama 流式响应
            Flux<String> responseFlux = chatService.chatWithOllama(sessionId, content, mineralContext);
            
            // 订阅流式响应并发送到客户端
            responseFlux.subscribe(
                token -> {
                    try {
                        // 发送 JSON 格式的 token（转义特殊字符）
                        String jsonData = String.format("{\"token\": \"%s\"}", token.replace("\"", "\\\"").replace("\n", "\\n"));
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(jsonData));
                    } catch (IOException e) {
                        log.error("发送 SSE 消息失败：{}", e.getMessage());
                    }
                },
                error -> {
                    log.error("Ollama 调用失败：{}", error.getMessage());
                    try {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data("{\"error\": \"" + error.getMessage() + "\"}"));
                    } catch (IOException e) {
                        // ignore
                    }
                    emitter.completeWithError(error);
                },
                () -> {
                    // 发送完成信号
                    String messageId = "assistant_" + System.currentTimeMillis();
                    try {
                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data(String.format("{\"done\": true, \"messageId\": \"%s\"}", messageId)));
                        log.info("发送完成");
                        emitter.complete();
                    } catch (IOException e) {
                        log.error("发送完成信号失败：{}", e.getMessage());
                    }
                }
            );
        } catch (Exception e) {
            log.error("调用 Ollama 失败：{}", e.getMessage(), e);
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data("{\"error\": \"" + e.getMessage() + "\"}"));
            emitter.completeWithError(e);
        }
    }
}
