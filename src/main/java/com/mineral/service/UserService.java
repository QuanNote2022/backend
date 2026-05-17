package com.mineral.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mineral.common.BusinessException;
import com.mineral.common.ErrorCode;
import com.mineral.dto.*;
import com.mineral.entity.*;
import com.mineral.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务类
 * 处理用户信息管理、密码修改等业务
 */
@Service
@RequiredArgsConstructor
public class UserService {

    /**
     * 用户数据访问对象
     */
    private final UserMapper userMapper;

    /**
     * 密码编码器（BCrypt）
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户统计服务
     */
    private final UserStatsService userStatsService;
    private final DetectionMapper detectionMapper;
    private final DetectionResultMapper detectionResultMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;

    /**
     * 日期时间格式化器
     */
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取用户个人信息
     * 
     * @param userId 用户 ID
     * @return 用户信息响应对象
     * @throws BusinessException 用户不存在时抛出
     */
    public UserProfileResponse getProfile(String userId) {
        // 根据 ID 查询用户
        UserDO userDO = userMapper.selectById(userId);
        if (userDO == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }

        // 构建响应对象
        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(userDO.getUserId());                 // 用户 ID
        response.setUsername(userDO.getUsername());             // 用户名
        response.setEmail(userDO.getEmail());                   // 邮箱
        response.setAvatar(userDO.getAvatar());                 // 头像 URL
        response.setNickname(userDO.getNickname());             // 昵称
        response.setCreatedAt(userDO.getCreatedAt().format(dateFormatter)); // 创建时间
        return response;
    }

    /**
     * 更新用户个人信息
     * 
     * @param userId 用户 ID
     * @param request 更新请求参数（邮箱、头像、昵称）
     * @return 更新后的用户信息
     * @throws BusinessException 用户不存在、邮箱已被使用时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        // 查询用户是否存在
        UserDO userDO = userMapper.selectById(userId);
        if (userDO == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }

        // 如果要更新邮箱，检查是否已被其他用户使用
        if (request.getEmail() != null && !request.getEmail().equals(userDO.getEmail())) {
            LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserDO::getEmail, request.getEmail())
                    .ne(UserDO::getUserId, userId);  // 排除当前用户
            
            if (userMapper.selectOne(wrapper) != null) {
                throw new BusinessException(ErrorCode.EMAIL_EXISTS, "邮箱已被使用");
            }
            userDO.setEmail(request.getEmail());
        }

        // 更新头像（如果提供）
        if (request.getAvatar() != null) {
            userDO.setAvatar(request.getAvatar());
        }

        // 更新昵称（如果提供）
        if (request.getNickname() != null) {
            userDO.setNickname(request.getNickname());
        }

        // 更新到数据库
        userMapper.updateById(userDO);

        // 返回更新后的用户信息
        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(userDO.getUserId());
        response.setUsername(userDO.getUsername());
        response.setEmail(userDO.getEmail());
        response.setAvatar(userDO.getAvatar());
        response.setNickname(userDO.getNickname());
        response.setCreatedAt(userDO.getCreatedAt().format(dateFormatter));
        return response;
    }

    /**
     * 修改用户密码
     * 
     * @param userId 用户 ID
     * @param request 密码修改请求（原密码、新密码）
     * @throws BusinessException 用户不存在、原密码错误时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(String userId, UpdatePasswordRequest request) {
        // 查询用户是否存在
        UserDO userDO = userMapper.selectById(userId);
        if (userDO == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }

        // 验证原密码是否正确
        if (!passwordEncoder.matches(request.getOldPassword(), userDO.getPasswordHash())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_ERROR, "原密码错误");
        }

        // 加密新密码并更新
        userDO.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(userDO);
    }

    /**
     * 导出用户全部数据
     * @param userId 用户 ID
     * @return 用户数据导出对象
     */
    public UserDataExport exportUserData(String userId) {
        UserDataExport export = new UserDataExport();
        export.setExportedAt(LocalDateTime.now().format(dateFormatter));
        export.setProfile(getProfile(userId));

        // 导出所有识别记录
        List<DetectionDO> detections = detectionMapper.selectList(
            new LambdaQueryWrapper<DetectionDO>()
                .eq(DetectionDO::getUserId, userId)
                .orderByDesc(DetectionDO::getCreatedAt)
        );
        List<DetectionHistoryResponse> detectionList = new ArrayList<>();
        for (DetectionDO d : detections) {
            List<DetectionResultDO> results = detectionResultMapper.selectList(
                new LambdaQueryWrapper<DetectionResultDO>()
                    .eq(DetectionResultDO::getDetectId, d.getDetectId())
            );
            detectionList.add(convertToHistoryResponse(d, results));
        }
        export.setDetections(detectionList);

        // 导出所有问答会话及消息
        List<ChatSessionDO> sessions = chatSessionMapper.selectList(
            new LambdaQueryWrapper<ChatSessionDO>()
                .eq(ChatSessionDO::getUserId, userId)
                .orderByDesc(ChatSessionDO::getCreatedAt)
        );
        List<UserDataExport.ChatSessionExport> sessionExports = new ArrayList<>();
        for (ChatSessionDO s : sessions) {
            UserDataExport.ChatSessionExport se = new UserDataExport.ChatSessionExport();
            se.setSession(convertToSessionResponse(s));
            List<ChatMessageDO> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessageDO>()
                    .eq(ChatMessageDO::getSessionId, s.getSessionId())
                    .orderByAsc(ChatMessageDO::getCreatedAt)
            );
            se.setMessages(messages.stream().map(this::convertToMessageResponse).collect(Collectors.toList()));
            sessionExports.add(se);
        }
        export.setChatSessions(sessionExports);

        return export;
    }

    private DetectionHistoryResponse convertToHistoryResponse(DetectionDO d, List<DetectionResultDO> results) {
        DetectionHistoryResponse r = new DetectionHistoryResponse();
        r.setDetectId(d.getDetectId());
        r.setImageUrl(d.getImageUrl());
        r.setCreatedAt(d.getCreatedAt().format(dateFormatter));
        List<DetectionResultResponse> resultList = new ArrayList<>();
        for (DetectionResultDO rr : results) {
            DetectionResultResponse dr = new DetectionResultResponse();
            dr.setLabel(rr.getLabel());
            dr.setConfidence(rr.getConfidence().doubleValue());
            dr.setBbox(new Integer[]{rr.getBboxX1(), rr.getBboxY1(), rr.getBboxX2(), rr.getBboxY2()});
            resultList.add(dr);
        }
        r.setResults(resultList);
        return r;
    }

    private ChatSessionResponse convertToSessionResponse(ChatSessionDO s) {
        ChatSessionResponse r = new ChatSessionResponse();
        r.setSessionId(s.getSessionId());
        r.setTitle(s.getTitle());
        r.setMineralName(s.getMineralName());
        r.setMessageCount(s.getMessageCount());
        r.setLastActiveAt(s.getLastActiveAt() != null ? s.getLastActiveAt().format(dateFormatter) : null);
        r.setCreatedAt(s.getCreatedAt().format(dateFormatter));
        return r;
    }

    private ChatMessageResponse convertToMessageResponse(ChatMessageDO m) {
        ChatMessageResponse r = new ChatMessageResponse();
        r.setMessageId(m.getMessageId());
        r.setSessionId(m.getSessionId());
        r.setRole(m.getRole());
        r.setContent(m.getContent());
        r.setCreatedAt(m.getCreatedAt().format(dateFormatter));
        return r;
    }
}
