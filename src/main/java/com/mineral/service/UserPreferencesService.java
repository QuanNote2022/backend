package com.mineral.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mineral.entity.UserPreferencesDO;
import com.mineral.mapper.UserPreferencesMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户偏好设置服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferencesService {

    private final UserPreferencesMapper userPreferencesMapper;

    /**
     * 获取用户偏好设置
     * @param userId 用户ID
     * @return 用户偏好设置
     */
    public UserPreferencesDO getUserPreferences(String userId) {
        UserPreferencesDO preferences = userPreferencesMapper.selectById(userId);
        if (preferences == null) {
            // 如果不存在，创建默认设置
            preferences = new UserPreferencesDO();
            preferences.setUserId(userId);
            preferences.setEmailNotification(true);
            preferences.setSystemNotification(true);
            preferences.setTheme("light");
            preferences.setLanguage("zh-CN");
            userPreferencesMapper.insert(preferences);
        }
        return preferences;
    }

    /**
     * 更新用户偏好设置
     * @param userId 用户 ID
     * @param preferences 用户偏好设置（只更新非空字段）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserPreferences(String userId, UserPreferencesDO preferences) {
        UserPreferencesDO existing = userPreferencesMapper.selectById(userId);
        if (existing != null) {
            // 只更新非空字段
            if (preferences.getEmailNotification() != null) {
                existing.setEmailNotification(preferences.getEmailNotification());
            }
            if (preferences.getSystemNotification() != null) {
                existing.setSystemNotification(preferences.getSystemNotification());
            }
            if (preferences.getTheme() != null) {
                existing.setTheme(preferences.getTheme());
            }
            if (preferences.getLanguage() != null) {
                existing.setLanguage(preferences.getLanguage());
            }
            userPreferencesMapper.updateById(existing);
        } else {
            // 创建新设置
            preferences.setUserId(userId);
            // 为 null 的字段设置默认值
            if (preferences.getEmailNotification() == null) {
                preferences.setEmailNotification(true);
            }
            if (preferences.getSystemNotification() == null) {
                preferences.setSystemNotification(true);
            }
            if (preferences.getTheme() == null) {
                preferences.setTheme("light");
            }
            if (preferences.getLanguage() == null) {
                preferences.setLanguage("zh-CN");
            }
            userPreferencesMapper.insert(preferences);
        }
    }

    /**
     * 初始化用户偏好设置
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void initUserPreferences(String userId) {
        LambdaQueryWrapper<UserPreferencesDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPreferencesDO::getUserId, userId);
        UserPreferencesDO existing = userPreferencesMapper.selectOne(wrapper);
        
        if (existing == null) {
            UserPreferencesDO preferences = new UserPreferencesDO();
            preferences.setUserId(userId);
            preferences.setEmailNotification(true);
            preferences.setSystemNotification(true);
            preferences.setTheme("light");
            preferences.setLanguage("zh-CN");
            userPreferencesMapper.insert(preferences);
            log.info("初始化用户偏好设置: userId={}", userId);
        }
    }
}