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
import com.mineral.entity.ChatMessage;
import com.mineral.entity.ChatSession;
import com.mineral.entity.Detection;
import com.mineral.mapper.ChatMessageMapper;
import com.mineral.mapper.ChatSessionMapper;
import com.mineral.mapper.DetectionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天会话服务类
 * 处理聊天会话创建、消息管理、会话删除等业务
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    /**
     * 聊天会话数据访问对象
     */
    private final ChatSessionMapper chatSessionMapper;

    /**
     * 聊天消息数据访问对象
     */
    private final ChatMessageMapper chatMessageMapper;

    /**
     * 识别记录数据访问对象（用于验证关联的识别记录）
     */
    private final DetectionMapper detectionMapper;

    /**
     * 日期时间格式化器
     */
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 创建聊天会话
     * 
     * @param request 创建请求（可关联识别记录和矿物名称）
     * @param userId 用户 ID
     * @return 创建的会话信息
     * @throws BusinessException 关联的识别记录不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public ChatSessionResponse createSession(CreateSessionRequest request, String userId) {
        // 如果关联了识别记录，验证其存在性
        if (request.getDetectId() != null) {
            Detection detection = detectionMapper.selectById(request.getDetectId());
            if (detection == null || !detection.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.DETECTION_RECORD_NOT_FOUND, "识别记录不存在");
            }
        }

        // 生成会话标题
        String title = request.getMineralName() != null ? 
                request.getMineralName() + "矿物咨询" : "新会话";

        // 创建会话
        ChatSession session = new ChatSession();
        session.setSessionId(IdUtil.getSnowflakeNextIdStr());  // 生成唯一 ID
        session.setUserId(userId);                             // 用户 ID
        session.setTitle(title);                               // 标题
        session.setMineralName(request.getMineralName());      // 矿物名称
        session.setDetectId(request.getDetectId());            // 关联的识别记录 ID
        session.setMessageCount(0);                            // 初始消息数为 0
        session.setLastActiveAt(LocalDateTime.now());          // 设置最后活跃时间

        chatSessionMapper.insert(session);

        return convertToResponse(session);
    }

    /**
     * 获取用户的会话列表（分页）
     * 
     * @param userId 用户 ID
     * @param pageQuery 分页查询参数
     * @return 分页结果（会话列表和总数）
     */
    public PageResult<ChatSessionResponse> getSessions(String userId, PageQuery pageQuery) {
        // 构建查询条件：按用户 ID 过滤，按最后活跃时间降序
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getLastActiveAt);

        // 分页查询
        Page<ChatSession> page = new Page<>(pageQuery.getPage(), pageQuery.getPageSize());
        Page<ChatSession> resultPage = chatSessionMapper.selectPage(page, wrapper);

        // 转换为响应对象
        List<ChatSessionResponse> list = resultPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResult.of(list, resultPage.getTotal(), pageQuery.getPage(), pageQuery.getPageSize());
    }

    /**
     * 获取会话的所有消息
     * 
     * @param sessionId 会话 ID
     * @param userId 用户 ID
     * @return 消息列表（按时间正序）
     * @throws BusinessException 会话不存在或无权限访问时抛出
     */
    public List<ChatMessageResponse> getSessionMessages(String sessionId, String userId) {
        // 验证会话存在性和权限
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND, "会话不存在");
        }

        // 查询消息：按创建时间升序
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreatedAt);

        List<ChatMessage> messages = chatMessageMapper.selectList(wrapper);
        return messages.stream().map(this::convertToMessageResponse).collect(Collectors.toList());
    }

    /**
     * 删除会话及其所有消息
     * 
     * @param sessionId 会话 ID
     * @param userId 用户 ID
     * @throws BusinessException 会话不存在或无权限访问时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(String sessionId, String userId) {
        // 验证会话存在性和权限
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND, "会话不存在");
        }

        // 删除会话
        chatSessionMapper.deleteById(sessionId);

        // 级联删除所有消息
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId);
        chatMessageMapper.delete(wrapper);
    }

    /**
     * 将会话实体转换为响应对象
     * 
     * @param session 会话实体
     * @return 会话响应
     */
    private ChatSessionResponse convertToResponse(ChatSession session) {
        ChatSessionResponse response = new ChatSessionResponse();
        response.setSessionId(session.getSessionId());
        response.setTitle(session.getTitle());
        response.setMineralName(session.getMineralName());
        response.setMessageCount(session.getMessageCount());
        response.setLastActiveAt(session.getLastActiveAt().format(dateFormatter));
        response.setCreatedAt(session.getCreatedAt().format(dateFormatter));
        return response;
    }

    /**
     * 将消息实体转换为响应对象
     * 
     * @param message 消息实体
     * @return 消息响应
     */
    private ChatMessageResponse convertToMessageResponse(ChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setMessageId(message.getMessageId());
        response.setSessionId(message.getSessionId());
        response.setRole(message.getRole());         // user 或 assistant
        response.setContent(message.getContent());   // 消息内容
        response.setCreatedAt(message.getCreatedAt().format(dateFormatter));
        return response;
    }
}
