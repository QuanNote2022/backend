package com.mineral.controller;

import com.mineral.common.ApiResponse;
import com.mineral.common.PageResult;
import com.mineral.dto.AchievementResponse;
import com.mineral.dto.LoginDeviceResponse;
import com.mineral.dto.LoginHistoryResponse;
import com.mineral.dto.UpdatePasswordRequest;
import com.mineral.dto.UpdateProfileRequest;
import com.mineral.dto.UserProfileResponse;
import com.mineral.dto.UserStatsResponse;
import com.mineral.entity.UserPreferencesDO;
import com.mineral.service.LoginDeviceService;
import com.mineral.service.UserPreferencesService;
import com.mineral.service.UserService;
import com.mineral.service.UserStatsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * 处理用户资料查询、更新、密码修改等请求
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserStatsService userStatsService;
    private final LoginDeviceService loginDeviceService;
    private final UserPreferencesService userPreferencesService;

    /**
     * 获取用户资料
     * @param request HTTP 请求
     * @return 用户资料
     */
    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getProfile(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        UserProfileResponse response = userService.getProfile(userId);
        return ApiResponse.success(response);
    }

    /**
     * 更新用户资料
     * @param request HTTP 请求
     * @param updateRequest 更新请求
     * @return 更新后的用户资料
     */
    @PutMapping("/profile")
    public ApiResponse<UserProfileResponse> updateProfile(
            HttpServletRequest request,
            @Valid @RequestBody UpdateProfileRequest updateRequest) {
        String userId = (String) request.getAttribute("userId");
        UserProfileResponse response = userService.updateProfile(userId, updateRequest);
        return ApiResponse.success("更新成功", response);
    }

    /**
     * 修改密码
     * @param request HTTP 请求
     * @param updateRequest 更新密码请求
     * @return 修改结果
     */
    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(
            HttpServletRequest request,
            @Valid @RequestBody UpdatePasswordRequest updateRequest) {
        String userId = (String) request.getAttribute("userId");
        userService.updatePassword(userId, updateRequest);
        return ApiResponse.success("密码修改成功", null);
    }

    /**
     * 获取用户统计数据
     * @param request HTTP 请求
     * @return 用户统计数据
     */
    @GetMapping("/stats")
    public ApiResponse<UserStatsResponse> getStats(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        UserStatsResponse response = userStatsService.getUserStats(userId);
        return ApiResponse.success(response);
    }

    /**
     * 获取用户成就列表
     * @param request HTTP 请求
     * @return 成就列表
     */
    @GetMapping("/achievements")
    public ApiResponse<java.util.List<AchievementResponse>> getAchievements(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        java.util.List<AchievementResponse> response = userStatsService.getUserAchievements(userId);
        return ApiResponse.success(response);
    }

    /**
     * 获取登录设备列表
     * @param request HTTP 请求
     * @return 登录设备列表
     */
    @GetMapping("/devices")
    public ApiResponse<java.util.List<LoginDeviceResponse>> getLoginDevices(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        java.util.List<LoginDeviceResponse> response = loginDeviceService.getLoginDevices(userId);
        return ApiResponse.success(response);
    }

    /**
     * 登出指定设备
     * @param request HTTP 请求
     * @param deviceId 设备ID
     * @return 操作结果
     */
    @DeleteMapping("/devices/{deviceId}")
    public ApiResponse<Void> logoutDevice(HttpServletRequest request, @PathVariable String deviceId) {
        String userId = (String) request.getAttribute("userId");
        loginDeviceService.logoutDevice(userId, deviceId);
        return ApiResponse.success("登出成功", null);
    }

    /**
     * 获取登录历史
     * @param request HTTP 请求
     * @param page 页码
     * @param pageSize 每页大小
     * @return 登录历史列表
     */
    @GetMapping("/login-history")
    public ApiResponse<PageResult<LoginHistoryResponse>> getLoginHistory(HttpServletRequest request, 
                                                                       @RequestParam int page, 
                                                                       @RequestParam int pageSize) {
        String userId = (String) request.getAttribute("userId");
        java.util.List<LoginHistoryResponse> histories = loginDeviceService.getLoginHistory(userId, page, pageSize);
        // 这里简化处理，实际应该查询总数
        long total = 100; // 临时值
        PageResult<LoginHistoryResponse> response = PageResult.of(histories, total, page, pageSize);
        return ApiResponse.success(response);
    }

    /**
     * 获取用户偏好设置
     * @param request HTTP 请求
     * @return 用户偏好设置
     */
    @GetMapping("/preferences")
    public ApiResponse<UserPreferencesDO> getUserPreferences(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        UserPreferencesDO preferences = userPreferencesService.getUserPreferences(userId);
        return ApiResponse.success(preferences);
    }

    /**
     * 更新用户偏好设置
     * @param request HTTP 请求
     * @param preferences 用户偏好设置
     * @return 更新结果
     */
    @PutMapping("/preferences")
    public ApiResponse<Void> updateUserPreferences(HttpServletRequest request, 
                                                  @RequestBody UserPreferencesDO preferences) {
        String userId = (String) request.getAttribute("userId");
        userPreferencesService.updateUserPreferences(userId, preferences);
        return ApiResponse.success("更新成功", null);
    }
}
