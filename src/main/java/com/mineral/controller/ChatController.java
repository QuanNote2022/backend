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
import java.util.concurrent.atomic.AtomicInteger;

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
     * 测试 Ollama 连接（阻塞式，用于诊断）
     */
    @GetMapping("/test-ollama")
    public ApiResponse<String> testOllama() {
        try {
            log.info("开始测试 Ollama 连接（阻塞式）");
            String response = chatService.chatWithOllamaBlocking("test-session", "Hello, 请回复'测试成功'", null);
            log.info("Ollama 响应: {}", response);
            return ApiResponse.success("Ollama 连接正常", response);
        } catch (Exception e) {
            log.error("Ollama 连接失败", e);
            return ApiResponse.error(500, "Ollama 连接失败: " + e.getMessage());
        }
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
     */
    @PostMapping(value = "/session/{sessionId}/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(
            @PathVariable String sessionId,
            @Valid @RequestBody SendMessageRequest request,
            HttpServletRequest httpRequest) {
        log.info("发送消息：sessionId={}, content={}", sessionId, request.getContent());
        
        SseEmitter emitter = new SseEmitter(180_000L);
        AtomicInteger tokenCount = new AtomicInteger(0);
        
        Flux<String> responseFlux = chatService.chatWithOllama(sessionId, request.getContent(), request.getMineralContext());
        
        responseFlux.subscribe(
            token -> {
                try {
                    int count = tokenCount.incrementAndGet();
                    log.info("发送 token #{}: {}", count, token);
                    emitter.send(SseEmitter.event()
                        .data("{\"token\": \"" + escapeJson(token) + "\"}"));
                } catch (IOException e) {
                    log.error("发送 SSE 消息失败: {}", e.getMessage());
                }
            },
            error -> {
                log.error("Ollama 调用失败: {}", error.getMessage(), error);
                try {
                    emitter.send(SseEmitter.event()
                        .data("{\"error\": \"" + escapeJson(error.getMessage()) + "\"}"));
                } catch (IOException ignored) {}
                emitter.completeWithError(error);
            },
            () -> {
                log.info("流完成，共发送 {} 个 token", tokenCount.get());
                try {
                    emitter.send(SseEmitter.event()
                        .data("{\"done\": true, \"messageId\": \"assistant_" + System.currentTimeMillis() + "\"}"));
                    emitter.complete();
                } catch (IOException e) {
                    log.error("发送完成信号失败: {}", e.getMessage());
                }
            }
        );
        
        return emitter;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
