package com.mineral.controller;

import com.mineral.common.ApiResponse;
import com.mineral.dto.LoginRequest;
import com.mineral.dto.LoginResponse;
import com.mineral.dto.RegisterRequest;
import com.mineral.service.AuthService;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 处理用户注册、登录、登出等认证相关请求
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     * @param request 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public ApiResponse<Object> register(@Valid @RequestBody RegisterRequest request) {
        String userId = authService.register(request);
        return ApiResponse.success("注册成功", userId);
    }

    /**
     * 用户登录
     * @param request 登录请求
     * @return 登录结果（包含 JWT 令牌）
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success("登录成功", response);
    }

    /**
     * 用户登出
     * @param request HTTP 请求
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        authService.logout();
        return ApiResponse.success("登出成功", null);
    }
}
