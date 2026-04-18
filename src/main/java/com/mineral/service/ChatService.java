package com.mineral.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.mineral.entity.FileDocumentDO;
import com.mineral.mapper.ChatMessageMapper;
import com.mineral.mapper.ChatSessionMapper;
import com.mineral.mapper.DetectionMapper;
import com.mineral.mapper.FileDocumentMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final DetectionMapper detectionMapper;
    private final FileDocumentMapper fileDocumentMapper;
    private final UserStatsService userStatsService;
    private final ChatLanguageModel chatLanguageModel;
    private final StreamingChatLanguageModel streamingChatLanguageModel;
    private final RagService ragService;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

        userStatsService.updateUserStats(userId, "chat");

        return convertToResponse(session);
    }

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

        LambdaQueryWrapper<FileDocumentDO> docWrapper = new LambdaQueryWrapper<>();
        docWrapper.eq(FileDocumentDO::getSessionId, sessionId);
        fileDocumentMapper.delete(docWrapper);
    }

    public void saveUserMessage(String sessionId, String content) {
        ChatMessageDO message = new ChatMessageDO();
        message.setMessageId(IdUtil.getSnowflakeNextIdStr());
        message.setSessionId(sessionId);
        message.setRole("user");
        message.setContent(content);
        chatMessageMapper.insert(message);
        ragService.indexMessage(sessionId, "user", content);
        log.info("保存用户消息: sessionId={}, content={}", sessionId, content);

        ChatSessionDO session = chatSessionMapper.selectById(sessionId);
        if (session != null && "新会话".equals(session.getTitle())) {
            String newTitle = generateTitleFromContent(content);
            session.setTitle(newTitle);
            chatSessionMapper.updateById(session);
            log.info("更新会话标题: sessionId={}, newTitle={}", sessionId, newTitle);
        }
    }

    private String generateTitleFromContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "新会话";
        }

        String trimmedContent = content.trim();
        
        if (trimmedContent.length() <= 20) {
            return trimmedContent;
        }

        String title = trimmedContent;

        String[] prefixes = {
            "请介绍一下", "介绍一下", "请介绍", "介绍下",
            "请详细介绍一下", "详细介绍一下", "详细介绍",
            "我想了解一下", "想了解一下", "我想了解", "想了解",
            "请问一下", "请问", "帮我", "帮我查一下",
            "能不能告诉我", "能告诉我", "告诉我",
            "什么是", "什么是", "什么叫",
            "如何理解", "怎么理解", "为什么叫"
        };
        
        for (String prefix : prefixes) {
            if (title.startsWith(prefix)) {
                title = title.substring(prefix.length()).trim();
                break;
            }
        }

        String[] suffixes = {
            "的特点", "的特性", "的用途", "的产地", "的性质",
            "有哪些", "有什么", "是什么", "是怎么样的",
            "呢", "吗", "啊", "呀"
        };
        
        for (String suffix : suffixes) {
            if (title.endsWith(suffix)) {
                title = title.substring(0, title.length() - suffix.length()).trim();
                break;
            }
        }

        if (title.length() > 15) {
            title = title.substring(0, 15) + "...";
        }

        if (title.isEmpty()) {
            title = trimmedContent.substring(0, Math.min(15, trimmedContent.length())) + "...";
        }

        return title;
    }

    public void saveAssistantMessage(String sessionId, String content) {
        ChatMessageDO message = new ChatMessageDO();
        message.setMessageId(IdUtil.getSnowflakeNextIdStr());
        message.setSessionId(sessionId);
        message.setRole("assistant");
        message.setContent(content);
        chatMessageMapper.insert(message);
        ragService.indexMessage(sessionId, "assistant", content);
        log.info("保存AI消息: sessionId={}, content={}", sessionId, content);
    }

    public void updateSessionStats(String sessionId) {
        ChatSessionDO session = chatSessionMapper.selectById(sessionId);
        if (session != null) {
            session.setMessageCount(session.getMessageCount() + 2);
            session.setLastActiveAt(LocalDateTime.now());
            chatSessionMapper.updateById(session);
            log.info("更新会话统计: sessionId={}, messageCount={}", sessionId, session.getMessageCount());
        }
    }

    public void validateSession(String sessionId, String userId) {
        ChatSessionDO session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND, "会话不存在");
        }
    }

    private List<ChatMessageDO> getSessionHistoryMessages(String sessionId) {
        LambdaQueryWrapper<ChatMessageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessageDO::getSessionId, sessionId)
                .orderByAsc(ChatMessageDO::getCreatedAt);
        return chatMessageMapper.selectList(wrapper);
    }

    public Flux<String> chatWithOllama(String sessionId, String content, String mineralContext, List<String> documentIds) {
        String ragContext;
        try {
            ragContext = ragService.buildContextWithHistoryAndDocuments(content, mineralContext, sessionId);
        } catch (Exception e) {
            log.error("构建 RAG 上下文失败: {}", e.getMessage(), e);
            ragContext = "";
        }

        String systemPrompt = buildSystemPrompt(ragContext);

        log.info("调用 LangChain4j 流式 API（RAG增强+文档）， sessionId={}, userMessage={}, documentIds={}", sessionId, content, documentIds);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));

        try {
            List<ChatMessageDO> historyMessages = getSessionHistoryMessages(sessionId);
            for (ChatMessageDO historyMsg : historyMessages) {
                if ("user".equals(historyMsg.getRole())) {
                    messages.add(UserMessage.from(historyMsg.getContent()));
                } else if ("assistant".equals(historyMsg.getRole())) {
                    messages.add(AiMessage.from(historyMsg.getContent()));
                }
            }
        } catch (Exception e) {
            log.error("获取历史消息失败: {}", e.getMessage(), e);
        }

        messages.add(UserMessage.from(content));

        return Flux.create(emitter -> {
            try {
                streamingChatLanguageModel.chat(messages, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        log.debug("收到 token: {}", partialResponse);
                        emitter.next(partialResponse);
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse completeResponse) {
                        log.info("LangChain4j 流式响应完成");
                        emitter.complete();
                    }

                    @Override
                    public void onError(Throwable error) {
                        log.error("LangChain4j 流式响应错误: {}", error.getMessage(), error);
                        emitter.next("抱歉，AI 服务暂时不可用，请稍后重试。");
                        emitter.complete();
                    }
                });
            } catch (Exception e) {
                log.error("调用 LangChain4j API 失败: {}", e.getMessage(), e);
                emitter.next("抱歉，AI 服务调用失败: " + e.getMessage());
                emitter.complete();
            }
        });
    }

    public String chatWithOllamaBlocking(String sessionId, String content, String mineralContext, List<String> documentIds) {
        String ragContext = ragService.buildContextWithHistoryAndDocuments(content, mineralContext, sessionId);
        String systemPrompt = buildSystemPrompt(ragContext);

        log.info("调用 LangChain4j 阻塞式 API（RAG增强+文档） sessionId={}, userMessage={}, documentIds={}", sessionId, content, documentIds);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));

        List<ChatMessageDO> historyMessages = getSessionHistoryMessages(sessionId);
        for (ChatMessageDO historyMsg : historyMessages) {
            if ("user".equals(historyMsg.getRole())) {
                messages.add(UserMessage.from(historyMsg.getContent()));
            } else if ("assistant".equals(historyMsg.getRole())) {
                messages.add(AiMessage.from(historyMsg.getContent()));
            }
        }

        messages.add(UserMessage.from(content));
        try {
            ChatResponse response = chatLanguageModel.chat(messages);
            String responseContent = response.aiMessage().text();
            log.info("LangChain4j 响应: {}", responseContent);
            return responseContent;
        } catch (Exception e) {
            log.error("调用 LangChain4j API 失败: {}", e.getMessage(), e);
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

    private String buildSystemPrompt(String context) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个专业的矿物识别助手，专门帮助用户了解矿物相关知识。\\n");
        sb.append("请用简洁、准确、友好的中文回答用户的问题。\n");

        if (context != null && !context.isEmpty()) {
            sb.append("\n以下是相关的上下文信息，请参考这些信息回答用户问题：\n");
            sb.append(context).append("\n");
        }

        sb.append("\n如果用户的问题超出你的知识范围，请诚实地告知，不要编造信息。");

        return sb.toString();
    }
}