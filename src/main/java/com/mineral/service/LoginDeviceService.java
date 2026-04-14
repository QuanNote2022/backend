package com.mineral.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mineral.dto.LoginDeviceResponse;
import com.mineral.dto.LoginHistoryResponse;
import com.mineral.entity.LoginDeviceDO;
import com.mineral.entity.LoginHistoryDO;
import com.mineral.mapper.LoginDeviceMapper;
import com.mineral.mapper.LoginHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 登录设备服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginDeviceService {

    private final LoginDeviceMapper loginDeviceMapper;
    private final LoginHistoryMapper loginHistoryMapper;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取用户的登录设备列表
     * @param userId 用户ID
     * @return 登录设备列表
     */
    public List<LoginDeviceResponse> getLoginDevices(String userId) {
        LambdaQueryWrapper<LoginDeviceDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LoginDeviceDO::getUserId, userId)
               .orderByDesc(LoginDeviceDO::getLastActiveTime);
        
        List<LoginDeviceDO> devices = loginDeviceMapper.selectList(wrapper);
        
        List<LoginDeviceResponse> responseList = new ArrayList<>();
        for (LoginDeviceDO device : devices) {
            LoginDeviceResponse response = new LoginDeviceResponse();
            response.setDeviceId(device.getDeviceId());
            response.setDeviceName(device.getDeviceName());
            response.setDeviceType(device.getDeviceType());
            response.setOs(device.getOs());
            response.setBrowser(device.getBrowser());
            response.setLoginTime(device.getLoginTime() != null ? device.getLoginTime().format(dateFormatter) : null);
            response.setLastActiveTime(device.getLastActiveTime() != null ? device.getLastActiveTime().format(dateFormatter) : null);
            response.setIpAddress(device.getIpAddress());
            response.setIsCurrent(device.getIsCurrent());
            responseList.add(response);
        }
        
        return responseList;
    }

    /**
     * 登出指定设备
     * @param userId 用户ID
     * @param deviceId 设备ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void logoutDevice(String userId, String deviceId) {
        LambdaQueryWrapper<LoginDeviceDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LoginDeviceDO::getUserId, userId)
               .eq(LoginDeviceDO::getDeviceId, deviceId);
        
        LoginDeviceDO device = loginDeviceMapper.selectOne(wrapper);
        if (device != null && !device.getIsCurrent()) {
            loginDeviceMapper.deleteById(deviceId);
        }
    }

    /**
     * 获取用户的登录历史
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 登录历史列表
     */
    public List<LoginHistoryResponse> getLoginHistory(String userId, int page, int pageSize) {
        LambdaQueryWrapper<LoginHistoryDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LoginHistoryDO::getUserId, userId)
               .orderByDesc(LoginHistoryDO::getLoginTime);
        
        // 分页查询
        int offset = (page - 1) * pageSize;
        wrapper.last("LIMIT " + pageSize + " OFFSET " + offset);
        
        List<LoginHistoryDO> histories = loginHistoryMapper.selectList(wrapper);
        
        List<LoginHistoryResponse> responseList = new ArrayList<>();
        for (LoginHistoryDO history : histories) {
            LoginHistoryResponse response = new LoginHistoryResponse();
            response.setHistoryId(history.getHistoryId());
            response.setLoginTime(history.getLoginTime() != null ? history.getLoginTime().format(dateFormatter) : null);
            response.setDeviceName(history.getDeviceName());
            response.setIpAddress(history.getIpAddress());
            response.setStatus(history.getStatus());
            response.setLocation(history.getLocation());
            responseList.add(response);
        }
        
        return responseList;
    }

    /**
     * 记录登录历史
     * @param userId 用户ID
     * @param deviceName 设备名称
     * @param ipAddress IP地址
     * @param status 登录状态
     * @param location 登录地点
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordLoginHistory(String userId, String deviceName, String ipAddress, String status, String location) {
        LoginHistoryDO history = new LoginHistoryDO();
        history.setHistoryId(java.util.UUID.randomUUID().toString());
        history.setUserId(userId);
        history.setLoginTime(LocalDateTime.now());
        history.setDeviceName(deviceName);
        history.setIpAddress(ipAddress);
        history.setStatus(status);
        history.setLocation(location);
        
        loginHistoryMapper.insert(history);
    }

    /**
     * 记录登录设备
     * @param userId 用户ID
     * @param deviceId 设备ID
     * @param deviceName 设备名称
     * @param deviceType 设备类型
     * @param os 操作系统
     * @param browser 浏览器
     * @param ipAddress IP地址
     * @param isCurrent 是否为当前设备
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordLoginDevice(String userId, String deviceId, String deviceName, 
                                 String deviceType, String os, String browser, 
                                 String ipAddress, boolean isCurrent) {
        // 先将所有设备设置为非当前设备
        if (isCurrent) {
            LambdaQueryWrapper<LoginDeviceDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(LoginDeviceDO::getUserId, userId);
            List<LoginDeviceDO> devices = loginDeviceMapper.selectList(wrapper);
            for (LoginDeviceDO device : devices) {
                device.setIsCurrent(false);
                loginDeviceMapper.updateById(device);
            }
        }
        
        // 检查设备是否已存在
        LoginDeviceDO existingDevice = loginDeviceMapper.selectById(deviceId);
        if (existingDevice != null) {
            // 更新设备信息
            existingDevice.setDeviceName(deviceName);
            existingDevice.setDeviceType(deviceType);
            existingDevice.setOs(os);
            existingDevice.setBrowser(browser);
            existingDevice.setIpAddress(ipAddress);
            existingDevice.setIsCurrent(isCurrent);
            existingDevice.setLastActiveTime(LocalDateTime.now());
            loginDeviceMapper.updateById(existingDevice);
        } else {
            // 创建新设备记录
            LoginDeviceDO device = new LoginDeviceDO();
            device.setDeviceId(deviceId);
            device.setUserId(userId);
            device.setDeviceName(deviceName);
            device.setDeviceType(deviceType);
            device.setOs(os);
            device.setBrowser(browser);
            device.setLoginTime(LocalDateTime.now());
            device.setLastActiveTime(LocalDateTime.now());
            device.setIpAddress(ipAddress);
            device.setIsCurrent(isCurrent);
            loginDeviceMapper.insert(device);
        }
    }
}