package com.mineral.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mineral.common.BusinessException;
import com.mineral.common.ErrorCode;
import com.mineral.config.JwtUtil;
import com.mineral.dto.LoginRequest;
import com.mineral.dto.LoginResponse;
import com.mineral.dto.RegisterRequest;
import com.mineral.entity.User;
import com.mineral.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * 用户注册
     * 
     * @param request 注册请求参数（用户名、密码、邮箱）
     * @return 注册成功的用户 ID
     * @throws BusinessException 用户名或邮箱已存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public String register(RegisterRequest request) {
        // 构建查询条件：检查用户名或邮箱是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername())
                .or()
                .eq(User::getEmail, request.getEmail());
        
        // 查询数据库
        User existingUser = userMapper.selectOne(wrapper);
        if (existingUser != null) {
            // 用户名已存在
            if (existingUser.getUsername().equals(request.getUsername())) {
                throw new BusinessException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
            }
            // 邮箱已存在
            if (existingUser.getEmail().equals(request.getEmail())) {
                throw new BusinessException(ErrorCode.EMAIL_EXISTS, "邮箱已存在");
            }
        }

        // 创建新用户对象
        User user = new User();
        user.setUsername(request.getUsername());                    // 用户名
        user.setPasswordHash(passwordEncoder.encode(request.getPassword())); // 加密密码
        user.setEmail(request.getEmail());                          // 邮箱
        user.setNickname(request.getUsername());                    // 昵称默认为用户名
        user.setIsActive(true);                                     // 激活状态

        // 插入数据库
        userMapper.insert(user);
        
        // 返回用户 ID
        return user.getUserId();
    }

    /**
     * 用户登录
     * 
     * @param request 登录请求参数（用户名、密码）
     * @return 登录响应（JWT Token）
     * @throws BusinessException 用户名或密码错误、用户被禁用时抛出
     */
    public LoginResponse login(LoginRequest request) {
        // 根据用户名查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        
        User user = userMapper.selectOne(wrapper);
        
        // 验证用户是否存在且密码正确
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR, "用户名或密码错误");
        }

        // 检查用户是否被禁用
        if (!user.getIsActive()) {
            throw new BusinessException(ErrorCode.USER_DISABLED, "用户已被禁用");
        }

        // 生成 JWT Token
        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername());
        
        // 返回 Token 和过期时间（86400 秒 = 24 小时）
        return new LoginResponse(token, 86400L);
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
