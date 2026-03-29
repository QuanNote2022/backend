package com.mineral.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mineral.common.BusinessException;
import com.mineral.common.ErrorCode;
import com.mineral.dto.DetectionResponse;
import com.mineral.dto.DetectionResultResponse;
import com.mineral.dto.MineralCategoryResponse;
import com.mineral.dto.MineralInfoResponse;
import com.mineral.entity.Detection;
import com.mineral.entity.DetectionResult;
import com.mineral.entity.Mineral;
import com.mineral.mapper.DetectionMapper;
import com.mineral.mapper.DetectionResultMapper;
import com.mineral.mapper.MineralMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 矿物识别服务类
 * 处理矿物图片上传、识别、结果查询等业务
 * 当前使用模拟数据进行识别，可替换为真实的 YOLO 模型
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MineralService {

    /**
     * 识别记录数据访问对象
     */
    private final DetectionMapper detectionMapper;

    /**
     * 识别结果数据访问对象
     */
    private final DetectionResultMapper detectionResultMapper;

    /**
     * 矿物信息数据访问对象
     */
    private final MineralMapper mineralMapper;

    /**
     * 日期时间格式化器
     */
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 文件上传路径，从配置文件读取
     */
    @Value("${upload.path}")
    private String uploadPath;

    /**
     * 矿物识别（上传图片）
     * 
     * @param file 上传的图片文件
     * @param userId 用户 ID
     * @return 识别响应（包含识别结果和矿物信息）
     * @throws BusinessException 图片格式不支持或大小超限时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public DetectionResponse detectMineral(MultipartFile file, String userId) {
        // 1. 验证文件
        validateFile(file);

        // 2. 保存文件到本地
        String imageUrl = saveFile(file);

        // 3. 创建识别记录
        Detection detection = new Detection();
        detection.setDetectId(IdUtil.getSnowflakeNextIdStr());  // 生成唯一 ID
        detection.setUserId(userId);                            // 用户 ID
        detection.setImageUrl(imageUrl);                        // 图片 URL
        detectionMapper.insert(detection);

        // 4. 执行识别（当前为模拟识别）
        List<DetectionResultResponse> results = simulateDetection(detection.getDetectId());

        // 5. 返回识别结果
        return buildDetectionResponse(detection, results);
    }

    /**
     * 验证上传的图片文件
     * 
     * @param file 上传的文件
     * @throws BusinessException 格式不支持或大小超限时抛出
     */
    private void validateFile(MultipartFile file) {
        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.IMAGE_FORMAT_NOT_SUPPORTED, "图片格式不支持");
        }

        // 检查文件大小（最大 10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException(ErrorCode.IMAGE_SIZE_EXCEEDED, "图片大小不能超过 10MB");
        }
    }

    /**
     * 保存文件到本地存储
     * 
     * @param file 上传的文件
     * @return 文件访问 URL
     * @throws BusinessException 文件保存失败时抛出
     */
    private String saveFile(MultipartFile file) {
        try {
            // 生成唯一文件名
            String fileName = IdUtil.fastSimpleUUID() + "_" + file.getOriginalFilename();
            // 按日期分目录存储
            String subPath = java.time.LocalDate.now().toString().replace("-", "/");
            String dir = uploadPath + subPath;
            FileUtil.mkdir(dir);
            
            // 保存文件
            File destFile = new File(dir, fileName);
            file.transferTo(destFile);
            
            // 返回访问 URL
            return "/uploads/" + subPath + "/" + fileName;
        } catch (IOException e) {
            log.error("File upload error: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件上传失败");
        }
    }

    /**
     * 模拟矿物识别
     * 当前使用随机数据模拟 YOLO 识别结果
     * 实际项目中应替换为真实的 AI 模型推理
     * 
     * @param detectId 识别记录 ID
     * @return 识别结果列表
     */
    private List<DetectionResultResponse> simulateDetection(String detectId) {
        List<DetectionResultResponse> results = new ArrayList<>();
        
        // 矿物名称列表
        List<String> mineralNames = new ArrayList<>();
        mineralNames.add("石英");
        mineralNames.add("长石");
        mineralNames.add("云母");
        mineralNames.add("方解石");
        mineralNames.add("角闪石");
        
        Random random = new Random();
        
        // 随机生成 1-3 个识别结果
        int resultCount = random.nextInt(3) + 1;
        
        for (int i = 0; i < resultCount; i++) {
            // 随机选择矿物
            String mineralName = mineralNames.get(random.nextInt(mineralNames.size()));
            
            // 查询矿物信息
            LambdaQueryWrapper<Mineral> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Mineral::getName, mineralName);
            Mineral mineral = mineralMapper.selectOne(wrapper);
            
            if (mineral == null) {
                continue;
            }
            
            // 创建识别结果
            DetectionResult result = new DetectionResult();
            result.setDetectId(detectId);
            result.setLabel(mineral.getName());
            result.setConfidence(new BigDecimal("0." + (85 + random.nextInt(14)))); // 置信度 0.85-0.98
            result.setBboxX1(100 + random.nextInt(200));  // 边界框坐标
            result.setBboxY1(100 + random.nextInt(200));
            result.setBboxX2(300 + random.nextInt(200));
            result.setBboxY2(300 + random.nextInt(200));
            
            // 保存到数据库
            detectionResultMapper.insert(result);
            
            // 构建响应对象
            DetectionResultResponse response = new DetectionResultResponse();
            response.setLabel(mineral.getName());
            response.setConfidence(result.getConfidence().doubleValue());
            response.setBbox(new Integer[]{result.getBboxX1(), result.getBboxY1(), 
                                         result.getBboxX2(), result.getBboxY2()});
            
            // 添加矿物详细信息
            MineralInfoResponse mineralInfo = convertToMineralInfo(mineral);
            response.setMineralInfo(mineralInfo);
            
            results.add(response);
        }
        
        return results;
    }

    /**
     * 获取识别详情
     * 
     * @param detectId 识别记录 ID
     * @param userId 用户 ID
     * @return 识别详情响应
     * @throws BusinessException 记录不存在或无权限访问时抛出
     */
    public DetectionResponse getDetectionDetail(String detectId, String userId) {
        // 查询识别记录
        Detection detection = detectionMapper.selectById(detectId);
        if (detection == null) {
            throw new BusinessException(ErrorCode.DETECTION_RECORD_NOT_FOUND, "识别记录不存在");
        }

        // 验证权限（只能查看自己的记录）
        if (!detection.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无权限访问该识别记录");
        }

        // 查询识别结果
        LambdaQueryWrapper<DetectionResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DetectionResult::getDetectId, detectId);
        List<DetectionResult> dbResults = detectionResultMapper.selectList(wrapper);

        // 构建响应
        List<DetectionResultResponse> results = new ArrayList<>();
        for (DetectionResult dbResult : dbResults) {
            // 查询矿物信息
            LambdaQueryWrapper<Mineral> mineralWrapper = new LambdaQueryWrapper<>();
            mineralWrapper.eq(Mineral::getName, dbResult.getLabel());
            Mineral mineral = mineralMapper.selectOne(mineralWrapper);

            DetectionResultResponse response = new DetectionResultResponse();
            response.setLabel(dbResult.getLabel());
            response.setConfidence(dbResult.getConfidence().doubleValue());
            response.setBbox(new Integer[]{dbResult.getBboxX1(), dbResult.getBboxY1(),
                                          dbResult.getBboxX2(), dbResult.getBboxY2()});
            
            if (mineral != null) {
                response.setMineralInfo(convertToMineralInfo(mineral));
            }
            
            results.add(response);
        }

        return buildDetectionResponse(detection, results);
    }

    /**
     * 获取矿物分类统计
     * 
     * @param userId 用户 ID
     * @return 矿物分类及识别次数列表
     */
    public List<MineralCategoryResponse> getCategories(String userId) {
        // 查询用户的所有识别记录
        LambdaQueryWrapper<Detection> detectionWrapper = new LambdaQueryWrapper<>();
        detectionWrapper.eq(Detection::getUserId, userId);
        List<Detection> detections = detectionMapper.selectList(detectionWrapper);

        if (detections.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有识别结果的 ID
        List<String> detectIds = detections.stream()
                .map(Detection::getDetectId)
                .collect(Collectors.toList());

        // 查询所有识别结果
        LambdaQueryWrapper<DetectionResult> resultWrapper = new LambdaQueryWrapper<>();
        resultWrapper.in(DetectionResult::getDetectId, detectIds);
        List<DetectionResult> results = detectionResultMapper.selectList(resultWrapper);

        // 按矿物名称分组统计
        return results.stream()
                .collect(Collectors.groupingBy(
                        DetectionResult::getLabel,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(entry -> {
                    MineralCategoryResponse response = new MineralCategoryResponse();
                    response.setId(IdUtil.getSnowflakeNextIdStr());
                    response.setName(entry.getKey());
                    response.setCount(entry.getValue().intValue());
                    return response;
                })
                .sorted((a, b) -> b.getCount() - a.getCount())  // 按次数降序排序
                .collect(Collectors.toList());
    }

    /**
     * 获取矿物详细信息
     * 
     * @param mineralName 矿物名称
     * @return 矿物信息响应
     * @throws BusinessException 矿物信息不存在时抛出
     */
    public MineralInfoResponse getMineralInfo(String mineralName) {
        // 根据名称查询矿物
        LambdaQueryWrapper<Mineral> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Mineral::getName, mineralName);
        Mineral mineral = mineralMapper.selectOne(wrapper);
        
        if (mineral == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "矿物信息不存在");
        }
        
        return convertToMineralInfo(mineral);
    }

    /**
     * 将矿物实体转换为响应对象
     * 
     * @param mineral 矿物实体
     * @return 矿物信息响应
     */
    private MineralInfoResponse convertToMineralInfo(Mineral mineral) {
        MineralInfoResponse info = new MineralInfoResponse();
        info.setName(mineral.getName());          // 名称
        info.setFormula(mineral.getFormula());    // 化学式
        info.setHardness(mineral.getHardness());  // 硬度
        info.setLuster(mineral.getLuster());      // 光泽
        info.setColor(mineral.getColor());        // 颜色
        info.setOrigin(mineral.getOrigin());      // 产地
        info.setUses(mineral.getUses());          // 用途
        info.setDescription(mineral.getDescription()); // 描述
        info.setThumbnail(mineral.getThumbnail());     // 缩略图
        return info;
    }

    /**
     * 构建识别响应对象
     * 
     * @param detection 识别记录
     * @param results 识别结果列表
     * @return 识别响应
     */
    private DetectionResponse buildDetectionResponse(Detection detection, 
                                                     List<DetectionResultResponse> results) {
        DetectionResponse response = new DetectionResponse();
        response.setDetectId(detection.getDetectId());
        response.setImageUrl(detection.getImageUrl());
        response.setResults(results);
        response.setCreatedAt(detection.getCreatedAt().format(dateFormatter));
        return response;
    }
}
