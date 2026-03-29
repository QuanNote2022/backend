package com.mineral.controller;

import com.mineral.common.ApiResponse;
import com.mineral.dto.UpdatePasswordRequest;
import com.mineral.dto.UpdateProfileRequest;
import com.mineral.dto.UserProfileResponse;
import com.mineral.service.UserService;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
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
}
