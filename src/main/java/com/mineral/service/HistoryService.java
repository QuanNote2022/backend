package com.mineral.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mineral.common.BusinessException;
import com.mineral.common.ErrorCode;
import com.mineral.common.PageResult;
import com.mineral.dto.ChatSessionResponse;
import com.mineral.dto.DetectionHistoryQuery;
import com.mineral.dto.DetectionHistoryResponse;
import com.mineral.dto.DetectionResultResponse;
import com.mineral.dto.MineralInfoResponse;
import com.mineral.entity.ChatSessionDO;
import com.mineral.entity.DetectionDO;
import com.mineral.entity.DetectionResultDO;
import com.mineral.entity.MineralDO;
import com.mineral.mapper.ChatSessionMapper;
import com.mineral.mapper.DetectionMapper;
import com.mineral.mapper.DetectionResultMapper;
import com.mineral.mapper.MineralMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 历史记录服务类
 * 处理用户识别历史和聊天历史的查询、删除等业务
 * 提供分页查询、关键词搜索、日期范围筛选等功能
 */
@Service
@RequiredArgsConstructor
public class HistoryService {

    /**
     * 识别记录数据访问对象
     */
    private final DetectionMapper detectionMapper;
    
    /**
     * 识别结果数据访问对象
     */
    private final DetectionResultMapper detectionResultMapper;
    
    /**
     * 矿物信息数据访问对象
     */
    private final MineralMapper mineralMapper;
    
    /**
     * 聊天会话数据访问对象
     */
    private final ChatSessionMapper chatSessionMapper;
    
    /**
     * 日期时间格式化器
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取识别历史记录（分页）
     * 支持关键词搜索（按矿物名称）、日期范围筛选
     *
     * @param userId 用户 ID
     * @param query 查询参数（页码、页大小、关键词、开始日期、结束日期）
     * @return 识别历史分页结果
     */
    public PageResult<DetectionHistoryResponse> getDetectionHistory(String userId, DetectionHistoryQuery query) {
        // 1. 构建查询条件：按用户 ID 过滤
        LambdaQueryWrapper<DetectionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DetectionDO::getUserId, userId);

        // 2. 关键词搜索：通过矿物名称反查识别记录 ID
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            List<String> detectIds = getDetectIdsByKeyword(query.getKeyword());
            if (!detectIds.isEmpty()) {
                wrapper.in(DetectionDO::getDetectId, detectIds);
            } else {
                // 未找到匹配的识别记录，返回空结果
                return PageResult.of(new ArrayList<>(), 0L, query.getPage(), query.getPageSize());
            }
        }

        // 3. 日期范围筛选：开始日期
        if (query.getStartDate() != null) {
            LocalDateTime start = LocalDate.parse(query.getStartDate()).atStartOfDay();
            wrapper.ge(DetectionDO::getCreatedAt, start);
        }

        // 4. 日期范围筛选：结束日期（到当天 23:59:59）
        if (query.getEndDate() != null) {
            LocalDateTime end = LocalDate.parse(query.getEndDate()).atTime(23, 59, 59);
            wrapper.le(DetectionDO::getCreatedAt, end);
        }

        // 5. 按创建时间倒序排列
        wrapper.orderByDesc(DetectionDO::getCreatedAt);

        // 6. 执行分页查询
        Page<DetectionDO> page = new Page<>(query.getPage(), query.getPageSize());
        Page<DetectionDO> resultPage = detectionMapper.selectPage(page, wrapper);

        // 7. 转换为响应对象
        List<DetectionHistoryResponse> list = resultPage.getRecords().stream()
                .map(this::convertToHistoryResponse)
                .collect(Collectors.toList());

        return PageResult.of(list, resultPage.getTotal(), query.getPage(), query.getPageSize());
    }

    /**
     * 根据关键词（矿物名称）获取识别记录 ID 列表
     * 用于实现按矿物名称搜索识别历史
     *
     * @param keyword 矿物名称关键词
     * @return 匹配的识别记录 ID 列表
     */
    private List<String> getDetectIdsByKeyword(String keyword) {
        // 在识别结果表中模糊匹配矿物名称
        LambdaQueryWrapper<DetectionResultDO> resultWrapper = new LambdaQueryWrapper<>();
        resultWrapper.like(DetectionResultDO::getLabel, keyword);
        List<DetectionResultDO> results = detectionResultMapper.selectList(resultWrapper);
        // 去重后返回识别记录 ID 列表
        return results.stream().map(DetectionResultDO::getDetectId).distinct().collect(Collectors.toList());
    }

    /**
     * 删除识别记录（包括关联的识别结果）
     * 级联删除，确保数据一致性
     *
     * @param detectId 识别记录 ID
     * @param userId 用户 ID（用于权限验证）
     * @throws BusinessException 当识别记录不存在或不属于该用户时抛出
     */
    public void deleteDetectionRecord(String detectId, String userId) {
        // 1. 查询并验证识别记录
        DetectionDO detectionDO = detectionMapper.selectById(detectId);
        if (detectionDO == null || !detectionDO.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.DETECTION_RECORD_NOT_FOUND, "识别记录不存在");
        }

        // 2. 删除识别记录
        detectionMapper.deleteById(detectId);

        // 3. 级联删除关联的识别结果
        LambdaQueryWrapper<DetectionResultDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DetectionResultDO::getDetectId, detectId);
        detectionResultMapper.delete(wrapper);
    }

    /**
     * 获取聊天历史记录（分页）
     * 按创建时间倒序排列
     *
     * @param userId 用户 ID
     * @param pageQuery 分页参数（页码、页大小）
     * @return 聊天会话分页结果
     */
    public PageResult<ChatSessionResponse> getChatHistory(String userId, com.mineral.common.PageQuery pageQuery) {
        // 1. 构建查询条件：按用户 ID 过滤，按创建时间倒序
        LambdaQueryWrapper<ChatSessionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSessionDO::getUserId, userId)
                .orderByDesc(ChatSessionDO::getCreatedAt);

        // 2. 执行分页查询
        Page<ChatSessionDO> page = new Page<>(pageQuery.getPage(), pageQuery.getPageSize());
        Page<ChatSessionDO> resultPage = chatSessionMapper.selectPage(page, wrapper);

        // 3. 转换为响应对象
        List<ChatSessionResponse> list = resultPage.getRecords().stream()
                .map(session -> {
                    ChatSessionResponse response = new ChatSessionResponse();
                    response.setSessionId(session.getSessionId());
                    response.setTitle(session.getTitle());
                    response.setMineralName(session.getMineralName());
                    response.setMessageCount(session.getMessageCount());
                    // 格式化时间字段
                    response.setLastActiveAt(session.getLastActiveAt().format(dateFormatter));
                    response.setCreatedAt(session.getCreatedAt().format(dateFormatter));
                    return response;
                })
                .collect(Collectors.toList());

        return PageResult.of(list, resultPage.getTotal(), pageQuery.getPage(), pageQuery.getPageSize());
    }

    /**
     * 将识别记录转换为历史响应对象
     * 包含识别结果和矿物详细信息
     *
     * @param detectionDO 识别记录实体
     * @return 识别历史响应对象
     */
    private DetectionHistoryResponse convertToHistoryResponse(DetectionDO detectionDO) {
        DetectionHistoryResponse response = new DetectionHistoryResponse();
        response.setDetectId(detectionDO.getDetectId());
        response.setImageUrl(detectionDO.getImageUrl());
        response.setCreatedAt(detectionDO.getCreatedAt().format(dateFormatter));

        // 1. 查询该识别记录的所有识别结果
        LambdaQueryWrapper<DetectionResultDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DetectionResultDO::getDetectId, detectionDO.getDetectId());
        List<DetectionResultDO> results = detectionResultMapper.selectList(wrapper);

        // 2. 转换每个识别结果
        List<DetectionResultResponse> resultResponses = results.stream()
                .map(result -> {
                    DetectionResultResponse resp = new DetectionResultResponse();
                    resp.setLabel(result.getLabel());
                    resp.setConfidence(result.getConfidence().doubleValue());
                    // 设置边界框坐标 [x1, y1, x2, y2]
                    resp.setBbox(new Integer[]{result.getBboxX1(), result.getBboxY1(),
                            result.getBboxX2(), result.getBboxY2()});

                    // 3. 查询矿物详细信息
                    LambdaQueryWrapper<MineralDO> mineralWrapper = new LambdaQueryWrapper<>();
                    mineralWrapper.eq(MineralDO::getName, result.getLabel());
                    MineralDO mineralDO = mineralMapper.selectOne(mineralWrapper);
                    if (mineralDO != null) {
                        // 构建矿物信息对象
                        MineralInfoResponse info = new MineralInfoResponse();
                        info.setName(mineralDO.getName());
                        info.setFormula(mineralDO.getFormula());
                        info.setHardness(mineralDO.getHardness());
                        info.setLuster(mineralDO.getLuster());
                        info.setColor(mineralDO.getColor());
                        info.setOrigin(mineralDO.getOrigin());
                        info.setUses(mineralDO.getUses());
                        info.setDescription(mineralDO.getDescription());
                        info.setThumbnail(mineralDO.getThumbnail());
                        resp.setMineralInfo(info);
                    }

                    return resp;
                })
                .collect(Collectors.toList());

        response.setResults(resultResponses);
        return response;
    }
}
