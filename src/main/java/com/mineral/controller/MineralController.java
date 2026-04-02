package com.mineral.controller;

import com.mineral.common.ApiResponse;
import com.mineral.dto.DetectionResponse;
import com.mineral.dto.MineralCategoryResponse;
import com.mineral.dto.MineralInfoResponse;
import com.mineral.service.MineralService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 矿物检测控制器
 * 处理矿物识别、分类查询、矿物详情等请求
 */
@RestController
@RequestMapping("/mineral")
@RequiredArgsConstructor
public class MineralController {

    private final MineralService mineralService;

    /**
     * 矿物识别（上传图片进行检测）
     * @param file 上传的图片文件
     * @param request HTTP 请求
     * @return 检测结果
     */
    @PostMapping(value = "/detect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DetectionResponse> detectMineral(
            @RequestParam("image") MultipartFile file,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        DetectionResponse response = mineralService.detectMineral(file, userId);
        return ApiResponse.success("识别成功", response);
    }

    /**
     * 获取检测记录详情
     * @param detectId 检测记录 ID
     * @param request HTTP 请求
     * @return 检测详情
     */
    @GetMapping("/detect/{detectId}")
    public ApiResponse<DetectionResponse> getDetectionDetail(
            @PathVariable String detectId,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        DetectionResponse response = mineralService.getDetectionDetail(detectId, userId);
        return ApiResponse.success(response);
    }

    /**
     * 获取矿物分类列表
     * @param request HTTP 请求
     * @return 矿物分类列表
     */
    @GetMapping("/categories")
    public ApiResponse<List<MineralCategoryResponse>> getCategories(
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        List<MineralCategoryResponse> categories = mineralService.getCategories(userId);
        return ApiResponse.success(categories);
    }

    /**
     * 获取矿物详细信息
     * @param mineralName 矿物名称
     * @return 矿物信息
     */
    @GetMapping("/info/{mineralName}")
    public ApiResponse<MineralInfoResponse> getMineralInfo(@PathVariable String mineralName) {
        MineralInfoResponse response = mineralService.getMineralInfo(mineralName);
        return ApiResponse.success(response);
    }
}
