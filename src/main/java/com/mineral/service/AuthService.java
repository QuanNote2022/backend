package com.mineral.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mineral.common.BusinessException;
import com.mineral.common.ErrorCode;
import com.mineral.config.JwtUtil;
import com.mineral.dto.LoginRequest;
import com.mineral.dto.LoginResponse;
import com.mineral.dto.RegisterRequest;
import com.mineral.entity.UserDO;
import com.mineral.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 认证服务类
 * 处理用户注册、登录、登出等认证相关业务
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * 用户数据访问对象
     */
    private final UserMapper userMapper;

    /**
     * 密码编码器（BCrypt）
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * JWT 工具类
     */
    private final JwtUtil jwtUtil;

    /**
     * 登录设备服务
     */
    private final LoginDeviceService loginDeviceService;

    /**
     * 用户偏好服务
     */
    private final UserPreferencesService userPreferencesService;

    /**
     * 用户注册
     * 
     * @param request 注册请求参数（用户名、密码、邮箱）
     * @return 注册成功的用户 ID
     * @throws BusinessException 用户名或邮箱已存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public String register(RegisterRequest request) {
        // 构建查询条件：检查用户名或邮箱是否已存在
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getUsername, request.getUsername())
                .or()
                .eq(UserDO::getEmail, request.getEmail());
        
        // 查询数据库
        UserDO existingUserDO = userMapper.selectOne(wrapper);
        if (existingUserDO != null) {
            // 用户名已存在
            if (existingUserDO.getUsername().equals(request.getUsername())) {
                throw new BusinessException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
            }
            // 邮箱已存在
            if (existingUserDO.getEmail().equals(request.getEmail())) {
                throw new BusinessException(ErrorCode.EMAIL_EXISTS, "邮箱已存在");
            }
        }

        // 创建新用户对象
        UserDO userDO = new UserDO();
        userDO.setUsername(request.getUsername());                    // 用户名
        userDO.setPasswordHash(passwordEncoder.encode(request.getPassword())); // 加密密码
        userDO.setEmail(request.getEmail());                          // 邮箱
        userDO.setNickname(request.getUsername());                    // 昵称默认为用户名
        userDO.setIsActive(true);                                     // 激活状态

        // 插入数据库
        userMapper.insert(userDO);
        
        // 初始化用户偏好设置
        userPreferencesService.initUserPreferences(userDO.getUserId());
        
        // 返回用户 ID
        return userDO.getUserId();
    }

    /**
     * 用户登录
     * 
     * @param request 登录请求参数（用户名、密码）
     * @return 登录响应（JWT Token）
     * @throws BusinessException 用户名或密码错误、用户被禁用时抛出
     */
    public LoginResponse login(LoginRequest request) {
        // 获取 HTTP 请求
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ipAddress = httpServletRequest.getRemoteAddr();
        
        // 根据用户名查询用户
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getUsername, request.getUsername());
        
        UserDO userDO = userMapper.selectOne(wrapper);
        
        // 验证用户是否存在且密码正确
        if (userDO == null || !passwordEncoder.matches(request.getPassword(), userDO.getPasswordHash())) {
            // 记录失败的登录历史
            loginDeviceService.recordLoginHistory(
                request.getUsername(), 
                "未知设备", 
                ipAddress, 
                "failed", 
                "未知"
            );
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR, "用户名或密码错误");
        }

        // 检查用户是否被禁用
        if (!userDO.getIsActive()) {
            throw new BusinessException(ErrorCode.USER_DISABLED, "用户已被禁用");
        }

        // 生成 JWT Token
        String token = jwtUtil.generateToken(userDO.getUserId(), userDO.getUsername());
        
        // 记录登录设备
        String userAgent = httpServletRequest.getHeader("User-Agent");
        java.util.Map<String, String> deviceInfo = parseUserAgent(userAgent);
        loginDeviceService.recordLoginDevice(
            userDO.getUserId(),
            token.substring(0, Math.min(32, token.length())),
            deviceInfo.get("deviceName"),
            deviceInfo.get("deviceType"),
            deviceInfo.get("os"),
            deviceInfo.get("browser"),
            ipAddress,
            true
        );
        
        // 记录登录历史
        loginDeviceService.recordLoginHistory(
            userDO.getUserId(),
            deviceInfo.get("deviceName"),
            ipAddress,
            "success",
            "未知"
        );
        
        // 返回 Token 和过期时间（86400 秒 = 24 小时）
        return new LoginResponse(token, 86400L);
    }
    
    /**
     * 解析 User-Agent 获取设备信息
     * @param userAgent User-Agent 字符串
     * @return 设备信息 Map
     */
    private java.util.Map<String, String> parseUserAgent(String userAgent) {
        java.util.Map<String, String> info = new java.util.HashMap<>();
        
        if (userAgent == null) {
            userAgent = "";
        }
        
        // 判断设备类型
        String deviceType = "desktop";
        String deviceName = "电脑";
        if (userAgent.toLowerCase().contains("mobile") || userAgent.toLowerCase().contains("android")) {
            deviceType = "mobile";
            deviceName = "手机";
        } else if (userAgent.toLowerCase().contains("ipad")) {
            deviceType = "tablet";
            deviceName = "平板";
        }
        
        // 判断操作系统
        String os = "未知";
        if (userAgent.toLowerCase().contains("windows")) {
            os = "Windows";
        } else if (userAgent.toLowerCase().contains("mac")) {
            os = "macOS";
        } else if (userAgent.toLowerCase().contains("linux")) {
            os = "Linux";
        } else if (userAgent.toLowerCase().contains("android")) {
            os = "Android";
        } else if (userAgent.toLowerCase().contains("ios")) {
            os = "iOS";
        }
        
        // 判断浏览器
        String browser = "未知";
        if (userAgent.toLowerCase().contains("chrome")) {
            browser = "Chrome";
        } else if (userAgent.toLowerCase().contains("firefox")) {
            browser = "Firefox";
        } else if (userAgent.toLowerCase().contains("safari")) {
            browser = "Safari";
        } else if (userAgent.toLowerCase().contains("edge")) {
            browser = "Edge";
        } else if (userAgent.toLowerCase().contains("msie") || userAgent.toLowerCase().contains("trident")) {
            browser = "IE";
        }
        
        info.put("deviceName", deviceName);
        info.put("deviceType", deviceType);
        info.put("os", os);
        info.put("browser", browser);
        
        return info;
    }

    /**
     * 用户登出
     * 当前实现为空方法，Token 会在过期后自动失效
     * 如需立即失效，可实现 Token 黑名单机制
     */
    public void logout() {
        // TODO 空实现，Token 会在过期后自动失效
        // 如需立即失效，可使用 Redis 实现 Token 黑名单
    }
}
