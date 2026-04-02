package com.mineral.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mineral.common.BusinessException;
import com.mineral.common.ErrorCode;
import com.mineral.common.PageQuery;
import com.mineral.common.PageResult;
import com.mineral.dto.ChatMessageResponse;
import com.mineral.dto.ChatSessionResponse;
import com.mineral.dto.CreateSessionRequest;
import com.mineral.entity.ChatMessageDO;
import com.mineral.entity.ChatSessionDO;
import com.mineral.entity.DetectionDO;
import com.mineral.mapper.ChatMessageMapper;
import com.mineral.mapper.ChatSessionMapper;
import com.mineral.mapper.DetectionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天会话服务类
 * 处理聊天会话创建、消息管理、会话删除等业务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final DetectionMapper detectionMapper;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model-name:qwen3.5:0.8b}")
    private String modelName;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 创建聊天会话
     */
    @Transactional(rollbackFor = Exception.class)
    public ChatSessionResponse createSession(CreateSessionRequest request, String userId) {
        if (request.getDetectId() != null) {
            DetectionDO detectionDO = detectionMapper.selectById(request.getDetectId());
            if (detectionDO == null || !detectionDO.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.DETECTION_RECORD_NOT_FOUND, "识别记录不存在");
            }
        }

        String title = request.getMineralName() != null ? 
                request.getMineralName() + "矿物咨询" : "新会话";

        ChatSessionDO session = new ChatSessionDO();
        session.setSessionId(IdUtil.getSnowflakeNextIdStr());
        session.setUserId(userId);
        session.setTitle(title);
        session.setMineralName(request.getMineralName());
        session.setDetectId(request.getDetectId());
        session.setMessageCount(0);
        session.setLastActiveAt(LocalDateTime.now());

        chatSessionMapper.insert(session);

        return convertToResponse(session);
    }

    /**
     * 获取用户的会话列表（分页）
     */
    public PageResult<ChatSessionResponse> getSessions(String userId, PageQuery pageQuery) {
        LambdaQueryWrapper<ChatSessionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSessionDO::getUserId, userId)
                .orderByDesc(ChatSessionDO::getLastActiveAt);

        Page<ChatSessionDO> page = new Page<>(pageQuery.getPage(), pageQuery.getPageSize());
        Page<ChatSessionDO> resultPage = chatSessionMapper.selectPage(page, wrapper);

        List<ChatSessionResponse> list = resultPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResult.of(list, resultPage.getTotal(), pageQuery.getPage(), pageQuery.getPageSize());
    }

    /**
     * 获取会话的所有消息
     */
    public List<ChatMessageResponse> getSessionMessages(String sessionId, String userId) {
        ChatSessionDO session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND, "会话不存在");
        }

        LambdaQueryWrapper<ChatMessageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessageDO::getSessionId, sessionId)
                .orderByAsc(ChatMessageDO::getCreatedAt);

        List<ChatMessageDO> messages = chatMessageMapper.selectList(wrapper);
        return messages.stream().map(this::convertToMessageResponse).collect(Collectors.toList());
    }

    /**
     * 删除会话及其所有消息
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(String sessionId, String userId) {
        ChatSessionDO session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND, "会话不存在");
        }

        chatSessionMapper.deleteById(sessionId);

        LambdaQueryWrapper<ChatMessageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessageDO::getSessionId, sessionId);
        chatMessageMapper.delete(wrapper);
    }

    /**
     * 调用 Ollama API 生成回复（流式）
     * 直接使用 WebClient 调用 Ollama API
     */
    public Flux<String> chatWithOllama(String sessionId, String content, String mineralContext) {
        String systemPrompt = buildSystemPrompt(mineralContext);
        
        String userMessage = mineralContext != null && !mineralContext.isEmpty() 
            ? String.format("[矿物上下文：%s]\n\n用户问题：%s", mineralContext, content)
            : content;
        
        log.info("调用 Ollama API（流式），sessionId={}, userMessage={}", sessionId, userMessage);
        
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("stream", true);
        
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);
        
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);
        
        requestBody.put("messages", messages);
        
        WebClient webClient = webClientBuilder.build();
        
        return webClient.post()
            .uri(ollamaBaseUrl + "/api/chat")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToFlux(String.class)
            .mapNotNull(response -> {
                try {
                    JsonNode node = objectMapper.readTree(response);
                    JsonNode messageNode = node.get("message");
                    if (messageNode != null) {
                        JsonNode contentNode = messageNode.get("content");
                        if (contentNode != null) {
                            String token = contentNode.asText();
                            log.debug("收到 token: {}", token);
                            return token;
                        }
                    }
                } catch (Exception e) {
                    log.error("解析响应失败: {}", e.getMessage());
                }
                return null;
            })
            .filter(token -> token != null && !token.isEmpty())
            .doOnSubscribe(s -> log.info("开始接收 Ollama 流式响应"))
            .doOnNext(token -> log.debug("发送 token: {}", token))
            .doOnComplete(() -> log.info("Ollama 流式响应完成"))
            .doOnError(e -> log.error("Ollama 流式响应错误: {}", e.getMessage()));
    }

    /**
     * 调用 Ollama API 生成回复（阻塞式）
     */
    public String chatWithOllamaBlocking(String sessionId, String content, String mineralContext) {
        String systemPrompt = buildSystemPrompt(mineralContext);
        
        String userMessage = mineralContext != null && !mineralContext.isEmpty() 
            ? String.format("[矿物上下文：%s]\n\n用户问题：%s", mineralContext, content)
            : content;
        
        log.info("调用 Ollama API（阻塞式），sessionId={}, userMessage={}", sessionId, userMessage);
        
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("stream", false);
        
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);
        
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);
        
        requestBody.put("messages", messages);
        
        WebClient webClient = webClientBuilder.build();
        
        try {
            String response = webClient.post()
                .uri(ollamaBaseUrl + "/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            JsonNode node = objectMapper.readTree(response);
            JsonNode messageNode = node.get("message");
            if (messageNode != null) {
                JsonNode contentNode = messageNode.get("content");
                if (contentNode != null) {
                    return contentNode.asText();
                }
            }
            return "";
        } catch (Exception e) {
            log.error("调用 Ollama API 失败: {}", e.getMessage(), e);
            return "调用 AI 服务失败: " + e.getMessage();
        }
    }

    private ChatSessionResponse convertToResponse(ChatSessionDO session) {
        ChatSessionResponse response = new ChatSessionResponse();
        response.setSessionId(session.getSessionId());
        response.setTitle(session.getTitle());
        response.setMineralName(session.getMineralName());
        response.setMessageCount(session.getMessageCount());
        response.setLastActiveAt(session.getLastActiveAt().format(dateFormatter));
        response.setCreatedAt(session.getCreatedAt().format(dateFormatter));
        return response;
    }

    private ChatMessageResponse convertToMessageResponse(ChatMessageDO message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setMessageId(message.getMessageId());
        response.setSessionId(message.getSessionId());
        response.setRole(message.getRole());
        response.setContent(message.getContent());
        response.setCreatedAt(message.getCreatedAt().format(dateFormatter));
        return response;
    }

    private String buildSystemPrompt(String mineralContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个专业的矿物识别助手，专门帮助用户了解矿物相关知识。\n");
        sb.append("请用简洁、准确、友好的中文回答用户的问题。\n");
        
        if (mineralContext != null && !mineralContext.isEmpty()) {
            sb.append("当前对话围绕特定矿物展开，请结合以下矿物信息回答：").append(mineralContext).append("\n");
        }
        
        sb.append("如果用户的问题超出你的知识范围，请诚实地告知，不要编造信息。");
        
        return sb.toString();
    }
}
