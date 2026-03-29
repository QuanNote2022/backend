package com.mineral.controller;

import com.mineral.common.ApiResponse;
import com.mineral.common.PageResult;
import com.mineral.dto.ChatSessionResponse;
import com.mineral.dto.DetectionHistoryQuery;
import com.mineral.dto.DetectionHistoryResponse;
import com.mineral.service.HistoryService;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 历史记录控制器
 * 处理检测历史和聊天历史的查询、删除等请求
 */
@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    /**
     * 获取检测历史记录
     * @param query 查询条件
     * @param request HTTP 请求
     * @return 检测历史记录（分页）
     */
    @GetMapping("/detections")
    public ApiResponse<PageResult<DetectionHistoryResponse>> getDetectionHistory(
            @Valid DetectionHistoryQuery query,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        PageResult<DetectionHistoryResponse> result = historyService.getDetectionHistory(userId, query);
        return ApiResponse.success(result);
    }

    /**
     * 删除检测记录
     * @param detectId 检测记录 ID
     * @param request HTTP 请求
     * @return 删除结果
     */
    @DeleteMapping("/detections/{detectId}")
    public ApiResponse<Void> deleteDetectionRecord(
            @PathVariable String detectId,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        historyService.deleteDetectionRecord(detectId, userId);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 获取聊天历史记录
     * @param page 当前页码
     * @param pageSize 每页大小
     * @param request HTTP 请求
     * @return 聊天历史记录（分页）
     */
    @GetMapping("/chats")
    public ApiResponse<PageResult<ChatSessionResponse>> getChatHistory(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        
        com.mineral.common.PageQuery pageQuery = new com.mineral.common.PageQuery();
        pageQuery.setPage(page);
        pageQuery.setPageSize(pageSize);
        
        PageResult<ChatSessionResponse> result = historyService.getChatHistory(userId, pageQuery);
        return ApiResponse.success(result);
    }
}
