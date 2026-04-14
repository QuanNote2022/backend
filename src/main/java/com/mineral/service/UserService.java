package com.mineral.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mineral.common.BusinessException;
import com.mineral.common.ErrorCode;
import com.mineral.dto.UpdatePasswordRequest;
import com.mineral.dto.UpdateProfileRequest;
import com.mineral.dto.UserProfileResponse;
import com.mineral.entity.UserDO;
import com.mineral.mapper.UserMapper;
import com.mineral.service.UserStatsService;
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
     * 用户统计服务
     */
    private final UserStatsService userStatsService;

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
        UserDO userDO = userMapper.selectById(userId);
        if (userDO == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }

        // 构建响应对象
        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(userDO.getUserId());                 // 用户 ID
        response.setUsername(userDO.getUsername());             // 用户名
        response.setEmail(userDO.getEmail());                   // 邮箱
        response.setAvatar(userDO.getAvatar());                 // 头像 URL
        response.setNickname(userDO.getNickname());             // 昵称
        response.setCreatedAt(userDO.getCreatedAt().format(dateFormatter)); // 创建时间
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
        UserDO userDO = userMapper.selectById(userId);
        if (userDO == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }

        // 如果要更新邮箱，检查是否已被其他用户使用
        if (request.getEmail() != null && !request.getEmail().equals(userDO.getEmail())) {
            LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserDO::getEmail, request.getEmail())
                    .ne(UserDO::getUserId, userId);  // 排除当前用户
            
            if (userMapper.selectOne(wrapper) != null) {
                throw new BusinessException(ErrorCode.EMAIL_EXISTS, "邮箱已被使用");
            }
            userDO.setEmail(request.getEmail());
        }

        // 更新头像（如果提供）
        if (request.getAvatar() != null) {
            userDO.setAvatar(request.getAvatar());
        }

        // 更新昵称（如果提供）
        if (request.getNickname() != null) {
            userDO.setNickname(request.getNickname());
        }

        // 更新到数据库
        userMapper.updateById(userDO);

        // 返回更新后的用户信息
        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(userDO.getUserId());
        response.setUsername(userDO.getUsername());
        response.setEmail(userDO.getEmail());
        response.setAvatar(userDO.getAvatar());
        response.setNickname(userDO.getNickname());
        response.setCreatedAt(userDO.getCreatedAt().format(dateFormatter));
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
        UserDO userDO = userMapper.selectById(userId);
        if (userDO == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }

        // 验证原密码是否正确
        if (!passwordEncoder.matches(request.getOldPassword(), userDO.getPasswordHash())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_ERROR, "原密码错误");
        }

        // 加密新密码并更新
        userDO.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(userDO);
    }
}
