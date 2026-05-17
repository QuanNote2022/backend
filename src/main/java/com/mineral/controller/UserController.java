package com.mineral.controller;

import com.mineral.common.ApiResponse;
import com.mineral.common.PageResult;
import com.mineral.dto.*;
import com.mineral.entity.UserPreferencesDO;
import com.mineral.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

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
    private final HistoryService historyService;

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
     * 导出用户全部数据
     * @param request HTTP 请求
     * @return JSON 文件附件
     */
    @GetMapping("/data/export")
    public ResponseEntity<byte[]> exportData(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        UserDataExport data = userService.exportUserData(userId);

        String json;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            json = mapper.writeValueAsString(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename("mineral-data-export.json", StandardCharsets.UTF_8)
            .build());
        headers.setContentLength(bytes.length);

        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    /**
     * 清除历史记录
     * @param request HTTP 请求
     * @param body 请求体（type: detect | chat | all）
     * @return 操作结果
     */
    @DeleteMapping("/history")
    public ApiResponse<Void> clearHistory(HttpServletRequest request, @RequestBody Map<String, String> body) {
        String userId = (String) request.getAttribute("userId");
        String type = body.getOrDefault("type", "all");
        historyService.clearHistory(userId, type);
        return ApiResponse.success("清除成功", null);
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
     * @param preferences 用户偏好设置（只需传递需要更新的字段）
     * @return 更新结果
     */
    @PutMapping("/preferences")
    public ApiResponse<Void> updateUserPreferences(HttpServletRequest request, 
                                                  @RequestBody UserPreferencesDO preferences) {
        String userId = (String) request.getAttribute("userId");
        // 只更新非空字段
        userPreferencesService.updateUserPreferences(userId, preferences);
        return ApiResponse.success("更新成功", null);
    }
}
