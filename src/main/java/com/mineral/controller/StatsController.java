package com.mineral.controller;

import com.mineral.common.ApiResponse;
import com.mineral.dto.MineralFrequencyResponse;
import com.mineral.dto.StatsOverviewResponse;
import com.mineral.service.StatsService;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 统计控制器
 * 处理用户数据统计、矿物频次统计等请求
 */
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /**
     * 获取统计概览数据
     * @param request HTTP 请求
     * @return 统计概览
     */
    @GetMapping("/overview")
    public ApiResponse<StatsOverviewResponse> getStatsOverview(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        StatsOverviewResponse response = statsService.getStatsOverview(userId);
        return ApiResponse.success(response);
    }

    /**
     * 获取矿物频次统计
     * @param days 统计天数（默认 30 天）
     * @param request HTTP 请求
     * @return 矿物频次列表
     */
    @GetMapping("/mineral-frequency")
    public ApiResponse<List<MineralFrequencyResponse>> getMineralFrequency(
            @RequestParam(required = false, defaultValue = "30") Integer days,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        List<MineralFrequencyResponse> responses = statsService.getMineralFrequency(userId, days);
        return ApiResponse.success(responses);
    }
}
