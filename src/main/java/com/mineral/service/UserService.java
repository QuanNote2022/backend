package com.mineral.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mineral.common.BusinessException;
import com.mineral.common.ErrorCode;
import com.mineral.dto.UpdatePasswordRequest;
import com.mineral.dto.UpdateProfileRequest;
import com.mineral.dto.UserProfileResponse;
import com.mineral.entity.User;
import com.mineral.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

/**
 * 用户服务类
 * 处理用户信息管理、密码修改等业务
 */
@Service
@RequiredArgsConstructor
public class UserService {

    /**
     * 用户数据访问对象
     */
    private final UserMapper userMapper;

    /**
     * 密码编码器（BCrypt）
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 日期时间格式化器
     */
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取用户个人信息
     * 
     * @param userId 用户 ID
     * @return 用户信息响应对象
     * @throws BusinessException 用户不存在时抛出
     */
    public UserProfileResponse getProfile(String userId) {
        // 根据 ID 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }

        // 构建响应对象
        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(user.getUserId());                 // 用户 ID
        response.setUsername(user.getUsername());             // 用户名
        response.setEmail(user.getEmail());                   // 邮箱
        response.setAvatar(user.getAvatar());                 // 头像 URL
        response.setNickname(user.getNickname());             // 昵称
        response.setCreatedAt(user.getCreatedAt().format(dateFormatter)); // 创建时间
        return response;
    }

    /**
     * 更新用户个人信息
     * 
     * @param userId 用户 ID
     * @param request 更新请求参数（邮箱、头像、昵称）
     * @return 更新后的用户信息
     * @throws BusinessException 用户不存在、邮箱已被使用时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        // 查询用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }

        // 如果要更新邮箱，检查是否已被其他用户使用
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getEmail, request.getEmail())
                    .ne(User::getUserId, userId);  // 排除当前用户
            
            if (userMapper.selectOne(wrapper) != null) {
                throw new BusinessException(ErrorCode.EMAIL_EXISTS, "邮箱已被使用");
            }
            user.setEmail(request.getEmail());
        }

        // 更新头像（如果提供）
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        // 更新昵称（如果提供）
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }

        // 更新到数据库
        userMapper.updateById(user);

        // 返回更新后的用户信息
        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setAvatar(user.getAvatar());
        response.setNickname(user.getNickname());
        response.setCreatedAt(user.getCreatedAt().format(dateFormatter));
        return response;
    }

    /**
     * 修改用户密码
     * 
     * @param userId 用户 ID
     * @param request 密码修改请求（原密码、新密码）
     * @throws BusinessException 用户不存在、原密码错误时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(String userId, UpdatePasswordRequest request) {
        // 查询用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }

        // 验证原密码是否正确
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_ERROR, "原密码错误");
        }

        // 加密新密码并更新
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }
}
