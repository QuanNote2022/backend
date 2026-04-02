package com.mineral.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mineral.dto.MineralFrequencyResponse;
import com.mineral.dto.StatsOverviewResponse;
import com.mineral.entity.ChatSessionDO;
import com.mineral.entity.DetectionDO;
import com.mineral.entity.DetectionResultDO;
import com.mineral.mapper.ChatSessionMapper;
import com.mineral.mapper.DetectionMapper;
import com.mineral.mapper.DetectionResultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统计分析服务类
 * 处理用户数据统计、矿物识别频率统计等业务
 * 提供统计概览、热门矿物、活跃度分析等功能
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    /**
     * 识别记录数据访问对象
     */
    private final DetectionMapper detectionMapper;
    
    /**
     * 识别结果数据访问对象
     */
    private final DetectionResultMapper detectionResultMapper;
    
    /**
     * 聊天会话数据访问对象
     */
    private final ChatSessionMapper chatSessionMapper;

    /**
     * 获取统计概览信息
     * 包含总识别次数、总会话数、最常识别矿物、周活跃天数
     *
     * @param userId 用户 ID
     * @return 统计概览响应对象
     */
    public StatsOverviewResponse getStatsOverview(String userId) {
        // 1. 统计总识别次数
        LambdaQueryWrapper<DetectionDO> detectionWrapper = new LambdaQueryWrapper<>();
        detectionWrapper.eq(DetectionDO::getUserId, userId);
        Long totalDetections = detectionMapper.selectCount(detectionWrapper);

        // 2. 统计总会话数
        LambdaQueryWrapper<ChatSessionDO> chatWrapper = new LambdaQueryWrapper<>();
        chatWrapper.eq(ChatSessionDO::getUserId, userId);
        Long totalChats = chatSessionMapper.selectCount(chatWrapper);

        // 3. 获取最常识别的矿物
        String topMineral = getTopMineral(userId);

        // 4. 统计近 7 天活跃天数
        int weeklyActiveDays = getWeeklyActiveDays(userId);

        // 5. 构建响应对象
        StatsOverviewResponse response = new StatsOverviewResponse();
        response.setTotalDetections(totalDetections.intValue());
        response.setTotalChats(totalChats.intValue());
        response.setTopMineral(topMineral);
        response.setWeeklyActiveDays(weeklyActiveDays);

        return response;
    }

    /**
     * 获取矿物识别频率统计
     * 统计指定天数内每种矿物的识别次数，按次数降序排列
     *
     * @param userId 用户 ID
     * @param days 统计天数（如 7、30 天）
     * @return 矿物频率统计列表（按识别次数降序）
     */
    public List<MineralFrequencyResponse> getMineralFrequency(String userId, Integer days) {
        // 1. 计算起始时间
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        // 2. 查询指定时间范围内的识别记录
        LambdaQueryWrapper<DetectionDO> detectionWrapper = new LambdaQueryWrapper<>();
        detectionWrapper.eq(DetectionDO::getUserId, userId)
                .ge(DetectionDO::getCreatedAt, startDate);
        List<DetectionDO> detectionDOS = detectionMapper.selectList(detectionWrapper);

        if (detectionDOS.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 提取识别记录 ID 列表
        List<String> detectIds = detectionDOS.stream()
                .map(DetectionDO::getDetectId)
                .collect(Collectors.toList());

        // 4. 查询所有识别结果
        LambdaQueryWrapper<DetectionResultDO> resultWrapper = new LambdaQueryWrapper<>();
        resultWrapper.in(DetectionResultDO::getDetectId, detectIds);
        List<DetectionResultDO> results = detectionResultMapper.selectList(resultWrapper);

        // 5. 按矿物名称分组统计频率
        Map<String, Long> frequencyMap = results.stream()
                .collect(Collectors.groupingBy(
                        DetectionResultDO::getLabel,
                        Collectors.counting()
                ));

        // 6. 转换为响应对象并按次数降序排序
        return frequencyMap.entrySet().stream()
                .map(entry -> {
                    MineralFrequencyResponse response = new MineralFrequencyResponse();
                    response.setMineralName(entry.getKey());
                    response.setCount(entry.getValue().intValue());
                    return response;
                })
                .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());
    }

    /**
     * 获取用户最常识别的矿物
     * 统计所有识别记录中出现频率最高的矿物
     *
     * @param userId 用户 ID
     * @return 最常识别的矿物名称，如果没有识别记录则返回"无"
     */
    private String getTopMineral(String userId) {
        // 1. 查询用户所有识别记录
        LambdaQueryWrapper<DetectionDO> detectionWrapper = new LambdaQueryWrapper<>();
        detectionWrapper.eq(DetectionDO::getUserId, userId);
        List<DetectionDO> detectionDOS = detectionMapper.selectList(detectionWrapper);

        if (detectionDOS.isEmpty()) {
            return "无";
        }

        // 2. 提取识别记录 ID 列表
        List<String> detectIds = detectionDOS.stream()
                .map(DetectionDO::getDetectId)
                .collect(Collectors.toList());

        // 3. 查询所有识别结果
        LambdaQueryWrapper<DetectionResultDO> resultWrapper = new LambdaQueryWrapper<>();
        resultWrapper.in(DetectionResultDO::getDetectId, detectIds);
        List<DetectionResultDO> results = detectionResultMapper.selectList(resultWrapper);

        // 4. 按矿物名称分组统计，找出出现次数最多的
        return results.stream()
                .collect(Collectors.groupingBy(
                        DetectionResultDO::getLabel,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("无");
    }

    /**
     * 获取近 7 天的活跃天数
     * 统计用户在过去 7 天内有多少天进行过识别
     *
     * @param userId 用户 ID
     * @return 活跃天数（0-7）
     */
    private int getWeeklyActiveDays(String userId) {
        // 1. 计算 7 天前的时间
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);

        // 2. 查询近 7 天的识别记录
        LambdaQueryWrapper<DetectionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DetectionDO::getUserId, userId)
                .ge(DetectionDO::getCreatedAt, startDate);

        List<DetectionDO> detectionDOS = detectionMapper.selectList(wrapper);

        // 3. 提取日期并去重，统计活跃天数
        return detectionDOS.stream()
                .map(d -> d.getCreatedAt().toLocalDate())
                .distinct()
                .collect(Collectors.toList())
                .size();
    }
}
