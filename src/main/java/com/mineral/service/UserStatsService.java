package com.mineral.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mineral.dto.AchievementResponse;
import com.mineral.dto.UserStatsResponse;
import com.mineral.entity.AchievementDO;
import com.mineral.entity.DetectionDO;
import com.mineral.entity.DetectionResultDO;
import com.mineral.entity.UserAchievementDO;
import com.mineral.entity.UserStatsDO;
import com.mineral.mapper.AchievementMapper;
import com.mineral.mapper.DetectionMapper;
import com.mineral.mapper.DetectionResultMapper;
import com.mineral.mapper.UserAchievementMapper;
import com.mineral.mapper.UserStatsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户统计和成就服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsService {

    private final UserStatsMapper userStatsMapper;
    private final AchievementMapper achievementMapper;
    private final UserAchievementMapper userAchievementMapper;
    private final DetectionMapper detectionMapper;
    private final DetectionResultMapper detectionResultMapper;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取用户统计数据
     * @param userId 用户ID
     * @return 用户统计响应
     */
    public UserStatsResponse getUserStats(String userId) {
        UserStatsDO stats = userStatsMapper.selectById(userId);
        if (stats == null) {
            stats = new UserStatsDO();
            stats.setUserId(userId);
            stats.setTotalDetections(0);
            stats.setTotalChats(0);
            stats.setActiveDays(0);
            stats.setConsecutiveDays(0);
            stats.setMineralTypes(0);
            userStatsMapper.insert(stats);
        }
        
        UserStatsResponse response = new UserStatsResponse();
        response.setTotalDetections(stats.getTotalDetections());
        response.setTotalChats(stats.getTotalChats());
        response.setActiveDays(stats.getActiveDays());
        response.setConsecutiveDays(stats.getConsecutiveDays());
        response.setTopMineral(stats.getTopMineral());
        response.setMineralTypes(stats.getMineralTypes());
        
        response.setWeeklyActivity(List.of(5, 8, 3, 12, 6, 9, 4));
        response.setMonthlyActivity(List.of(45, 62, 38, 71));
        
        return response;
    }

    /**
     * 获取用户成就列表
     * @param userId 用户ID
     * @return 成就列表
     */
    public List<AchievementResponse> getUserAchievements(String userId) {
        List<AchievementDO> achievements = achievementMapper.selectList(null);
        
        LambdaQueryWrapper<UserAchievementDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAchievementDO::getUserId, userId);
        List<UserAchievementDO> userAchievements = userAchievementMapper.selectList(wrapper);
        
        Map<String, UserAchievementDO> userAchievementMap = new HashMap<>();
        for (UserAchievementDO ua : userAchievements) {
            userAchievementMap.put(ua.getAchievementId(), ua);
        }
        
        List<AchievementResponse> responseList = new ArrayList<>();
        for (AchievementDO achievement : achievements) {
            AchievementResponse response = new AchievementResponse();
            response.setAchievementId(achievement.getAchievementId());
            response.setName(achievement.getName());
            response.setDescription(achievement.getDescription());
            response.setIcon(achievement.getIcon());
            response.setLevel(achievement.getLevel());
            response.setTargetProgress(achievement.getTargetValue());
            
            UserAchievementDO ua = userAchievementMap.get(achievement.getAchievementId());
            if (ua != null) {
                response.setUnlocked(ua.getUnlocked());
                response.setUnlockedAt(ua.getUnlockedAt() != null ? ua.getUnlockedAt().format(dateFormatter) : null);
                response.setProgress(ua.getProgress());
            } else {
                response.setUnlocked(false);
                response.setProgress(0);
            }
            
            responseList.add(response);
        }
        
        return responseList;
    }

    /**
     * 初始化用户成就数据
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void initUserAchievements(String userId) {
        List<AchievementDO> achievements = achievementMapper.selectList(null);
        for (AchievementDO achievement : achievements) {
            UserAchievementDO userAchievement = new UserAchievementDO();
            userAchievement.setUserId(userId);
            userAchievement.setAchievementId(achievement.getAchievementId());
            userAchievement.setUnlocked(false);
            userAchievement.setProgress(0);
            userAchievementMapper.insert(userAchievement);
        }
    }

    /**
     * 更新用户统计数据
     * @param userId 用户 ID
     * @param statsType 统计类型（detection/chat）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStats(String userId, String statsType) {
        UserStatsDO stats = userStatsMapper.selectById(userId);
        if (stats == null) {
            stats = new UserStatsDO();
            stats.setUserId(userId);
            stats.setTotalDetections(0);
            stats.setTotalChats(0);
            stats.setActiveDays(0);
            stats.setConsecutiveDays(0);
            stats.setMineralTypes(0);
            userStatsMapper.insert(stats);
        }
        
        if ("detection".equals(statsType)) {
            stats.setTotalDetections(stats.getTotalDetections() + 1);
            // 更新矿物种类数
            stats.setMineralTypes(calculateMineralTypes(userId));
            // 更新最常识别的矿物
            stats.setTopMineral(calculateTopMineral(userId));
        } else if ("chat".equals(statsType)) {
            stats.setTotalChats(stats.getTotalChats() + 1);
        }
        
        // 更新活跃天数
        updateActiveDays(stats, userId);
        
        userStatsMapper.updateById(stats);
        
        // 检查成就
        checkAchievements(userId, stats);
        
        log.info("更新用户统计数据：userId={}, totalDetections={}, totalChats={}, mineralTypes={}",
                userId, stats.getTotalDetections(), stats.getTotalChats(), stats.getMineralTypes());
    }

    /**
     * 计算用户识别的矿物种类数
     * @param userId 用户 ID
     * @return 矿物种类数
     */
    private Integer calculateMineralTypes(String userId) {
        // 查询用户的所有识别记录
        LambdaQueryWrapper<DetectionDO> detectionWrapper = new LambdaQueryWrapper<>();
        detectionWrapper.eq(DetectionDO::getUserId, userId);
        List<DetectionDO> detections = detectionMapper.selectList(detectionWrapper);

        if (detections.isEmpty()) {
            return 0;
        }

        // 获取所有识别结果的 ID
        List<String> detectIds = detections.stream()
                .map(DetectionDO::getDetectId)
                .collect(Collectors.toList());

        // 查询所有识别结果
        LambdaQueryWrapper<DetectionResultDO> resultWrapper = new LambdaQueryWrapper<>();
        resultWrapper.in(DetectionResultDO::getDetectId, detectIds);
        List<DetectionResultDO> results = detectionResultMapper.selectList(resultWrapper);

        // 使用 Set 去重，统计矿物种类
        Set<String> uniqueMinerals = results.stream()
                .map(DetectionResultDO::getLabel)
                .collect(Collectors.toSet());

        return uniqueMinerals.size();
    }

    /**
     * 计算用户最常识别的矿物
     * @param userId 用户 ID
     * @return 最常识别的矿物名称
     */
    private String calculateTopMineral(String userId) {
        // 查询用户的所有识别记录
        LambdaQueryWrapper<DetectionDO> detectionWrapper = new LambdaQueryWrapper<>();
        detectionWrapper.eq(DetectionDO::getUserId, userId);
        List<DetectionDO> detections = detectionMapper.selectList(detectionWrapper);

        if (detections.isEmpty()) {
            return "无";
        }

        // 获取所有识别结果的 ID
        List<String> detectIds = detections.stream()
                .map(DetectionDO::getDetectId)
                .collect(Collectors.toList());

        // 查询所有识别结果
        LambdaQueryWrapper<DetectionResultDO> resultWrapper = new LambdaQueryWrapper<>();
        resultWrapper.in(DetectionResultDO::getDetectId, detectIds);
        List<DetectionResultDO> results = detectionResultMapper.selectList(resultWrapper);

        // 统计每种矿物的识别次数
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
     * 更新活跃天数
     * @param stats 用户统计数据
     * @param userId 用户 ID
     */
    private void updateActiveDays(UserStatsDO stats, String userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();

        // 检查今天是否有活动
        boolean hasActivityToday = hasActivityInPeriod(userId, todayStart, todayEnd);

        if (hasActivityToday) {
            // 检查昨天是否有活动
            LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
            LocalDateTime yesterdayEnd = todayStart;
            boolean hasActivityYesterday = hasActivityInPeriod(userId, yesterdayStart, yesterdayEnd);

            if (hasActivityYesterday) {
                stats.setConsecutiveDays(stats.getConsecutiveDays() + 1);
            } else {
                stats.setConsecutiveDays(1);
            }

            // 计算总活跃天数
            int totalActiveDays = calculateTotalActiveDays(userId);
            stats.setActiveDays(totalActiveDays);
        }
    }

    /**
     * 检查用户在指定时间段内是否有活动
     * @param userId 用户 ID
     * @param start 开始时间
     * @param end 结束时间
     * @return 是否有活动
     */
    private boolean hasActivityInPeriod(String userId, LocalDateTime start, LocalDateTime end) {
        // 检查识别活动
        LambdaQueryWrapper<DetectionDO> detectionWrapper = new LambdaQueryWrapper<>();
        detectionWrapper.eq(DetectionDO::getUserId, userId)
                .ge(DetectionDO::getCreatedAt, start)
                .lt(DetectionDO::getCreatedAt, end);
        Long detectionCount = detectionMapper.selectCount(detectionWrapper);

        if (detectionCount > 0) {
            return true;
        }

        // TODO: 检查问答活动（需要 ChatSessionMapper）
        return false;
    }

    /**
     * 计算用户总活跃天数
     * @param userId 用户 ID
     * @return 总活跃天数
     */
    private int calculateTotalActiveDays(String userId) {
        // 查询用户的所有识别记录
        LambdaQueryWrapper<DetectionDO> detectionWrapper = new LambdaQueryWrapper<>();
        detectionWrapper.eq(DetectionDO::getUserId, userId);
        List<DetectionDO> detections = detectionMapper.selectList(detectionWrapper);

        // 使用 Set 去重，统计活跃天数
        Set<LocalDate> activeDates = new HashSet<>();
        detections.forEach(d -> activeDates.add(d.getCreatedAt().toLocalDate()));

        // TODO: 添加问答活动的日期

        return activeDates.size();
    }

    /**
     * 检查并更新成就
     * @param userId 用户ID
     * @param stats 用户统计数据
     */
    private void checkAchievements(String userId, UserStatsDO stats) {
        List<AchievementDO> achievements = achievementMapper.selectList(null);
        
        for (AchievementDO achievement : achievements) {
            LambdaQueryWrapper<UserAchievementDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserAchievementDO::getUserId, userId)
                   .eq(UserAchievementDO::getAchievementId, achievement.getAchievementId());
            UserAchievementDO userAchievement = userAchievementMapper.selectOne(wrapper);
            
            if (userAchievement == null) {
                userAchievement = new UserAchievementDO();
                userAchievement.setUserId(userId);
                userAchievement.setAchievementId(achievement.getAchievementId());
                userAchievement.setUnlocked(false);
                userAchievement.setProgress(0);
            }
            
            int currentValue = 0;
            switch (achievement.getAchievementType()) {
                case "detect":
                    currentValue = stats.getTotalDetections();
                    break;
                case "chat":
                    currentValue = stats.getTotalChats();
                    break;
                case "mineral":
                    currentValue = stats.getMineralTypes();
                    break;
                case "login":
                    currentValue = stats.getConsecutiveDays();
                    break;
            }
            
            userAchievement.setProgress(currentValue);
            
            if (currentValue >= achievement.getTargetValue() && !userAchievement.getUnlocked()) {
                userAchievement.setUnlocked(true);
                userAchievement.setUnlockedAt(LocalDateTime.now());
                log.info("用户 {} 解锁成就: {}", userId, achievement.getName());
            }
            
            if (userAchievement.getId() == null) {
                userAchievementMapper.insert(userAchievement);
            } else {
                userAchievementMapper.updateById(userAchievement);
            }
        }
    }
}