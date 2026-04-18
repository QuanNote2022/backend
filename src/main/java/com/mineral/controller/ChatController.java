package com.mineral.controller;

import com.mineral.common.ApiResponse;
import com.mineral.dto.ChatMessageResponse;
import com.mineral.dto.ChatSessionResponse;
import com.mineral.dto.CreateSessionRequest;
import com.mineral.dto.SendMessageRequest;
import com.mineral.entity.FileDocumentDO;
import com.mineral.service.ChatService;
import com.mineral.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final DocumentService documentService;

    @PostMapping("/session")
    public ApiResponse<ChatSessionResponse> createSession(
            @Valid @RequestBody CreateSessionRequest request,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        ChatSessionResponse response = chatService.createSession(request, userId);
        return ApiResponse.success("创建成功", response);
    }

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

    @GetMapping("/session/{sessionId}/messages")
    public ApiResponse<List<ChatMessageResponse>> getSessionMessages(
            @PathVariable String sessionId,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        List<ChatMessageResponse> messages = chatService.getSessionMessages(sessionId, userId);
        return ApiResponse.success(messages);
    }

    @GetMapping("/test-ollama")
    public ApiResponse<String> testOllama() {
        try {
            log.info("开始测试 Ollama 连接（阻塞式）");
            String response = chatService.chatWithOllamaBlocking("test-session", "Hello, 请回复'测试成功'", null, null);
            log.info("Ollama 响应: {}", response);
            return ApiResponse.success("Ollama 连接正常", response);
        } catch (Exception e) {
            log.error("Ollama 连接失败", e);
            return ApiResponse.error(500, "Ollama 连接失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/session/{sessionId}")
    public ApiResponse<Void> deleteSession(
            @PathVariable String sessionId,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        chatService.deleteSession(sessionId, userId);
        return ApiResponse.success("删除成功", null);
    }

    @PostMapping(value = "/session/{sessionId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileDocumentDO> uploadDocument(
            @PathVariable String sessionId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        chatService.validateSession(sessionId, userId);
        FileDocumentDO document = documentService.uploadFile(file, sessionId, userId);
        return ApiResponse.success("上传成功", document);
    }

    @GetMapping("/session/{sessionId}/documents")
    public ApiResponse<List<FileDocumentDO>> getSessionDocuments(
            @PathVariable String sessionId,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        chatService.validateSession(sessionId, userId);
        List<FileDocumentDO> documents = documentService.getSessionDocuments(sessionId);
        return ApiResponse.success(documents);
    }

    @DeleteMapping("/session/{sessionId}/documents/{documentId}")
    public ApiResponse<Void> deleteDocument(
            @PathVariable String sessionId,
            @PathVariable String documentId,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        chatService.validateSession(sessionId, userId);
        documentService.deleteDocument(documentId);
        return ApiResponse.success("删除成功", null);
    }

    @PostMapping(value = "/session/{sessionId}/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(
            @PathVariable String sessionId,
            @Valid @RequestBody SendMessageRequest request,
            HttpServletRequest httpRequest) {
        log.info("发送消息：sessionId={}, content={}, documentIds={}", sessionId, request.getContent(), request.getDocumentIds());

        String userId = (String) httpRequest.getAttribute("userId");

        chatService.validateSession(sessionId, userId);

        chatService.saveUserMessage(sessionId, request.getContent());

        SseEmitter emitter = new SseEmitter(180_000L);
        AtomicInteger tokenCount = new AtomicInteger(0);
        AtomicReference<StringBuilder> assistantResponse = new AtomicReference<>(new StringBuilder());

        Flux<String> responseFlux = chatService.chatWithOllama(sessionId, request.getContent(), request.getMineralContext(), request.getDocumentIds());

        responseFlux.subscribe(
            token -> {
                try {
                    int count = tokenCount.incrementAndGet();
                    log.info("发送 token #{}: {}", count, token);
                    assistantResponse.get().append(token);
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
                String fullResponse = assistantResponse.get().toString();
                chatService.saveAssistantMessage(sessionId, fullResponse);
                chatService.updateSessionStats(sessionId);
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