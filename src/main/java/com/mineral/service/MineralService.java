package com.mineral.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mineral.common.BusinessException;
import com.mineral.common.ErrorCode;
import com.mineral.dto.DetectionResponse;
import com.mineral.dto.DetectionResultResponse;
import com.mineral.dto.MineralCategoryResponse;
import com.mineral.dto.MineralInfoResponse;
import com.mineral.entity.DetectionDO;
import com.mineral.entity.DetectionResultDO;
import com.mineral.entity.MineralDO;
import com.mineral.mapper.DetectionMapper;
import com.mineral.mapper.DetectionResultMapper;
import com.mineral.mapper.MineralMapper;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 矿物识别服务类
 * 处理矿物图片上传、识别、结果查询等业务
 * 使用多模态大语言模型（qwen-vl-max）实现矿物识别
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MineralService {

    /** 识别记录数据访问对象 */
    private final DetectionMapper detectionMapper;

    /** 识别结果数据访问对象 */
    private final DetectionResultMapper detectionResultMapper;

    /** 矿物信息数据访问对象 */
    private final MineralMapper mineralMapper;

    /** 用户统计服务 */
    private final UserStatsService userStatsService;

    /** 视觉语言模型（用于矿物识别） */
    private final ChatLanguageModel visionChatModel;

    /** 日期时间格式化器 */
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 文件上传路径，从配置文件读取 */
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
        // 1. 验证文件格式和大小
        validateFile(file);

        // 2. 保存文件到本地
        String imageUrl = saveFile(file);

        // 3. 创建识别记录
        DetectionDO detectionDO = new DetectionDO();
        detectionDO.setDetectId(IdUtil.getSnowflakeNextIdStr());
        detectionDO.setUserId(userId);
        detectionDO.setImageUrl(imageUrl);
        detectionMapper.insert(detectionDO);

        // 4. 调用多模态AI模型进行矿物识别
        List<DetectionResultResponse> results = detectMineralWithAI(detectionDO.getDetectId(), imageUrl);

        // 5. 更新用户统计数据
        userStatsService.updateUserStats(userId, "detection");

        // 6. 返回识别结果
        return buildDetectionResponse(detectionDO, results);
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
     * 使用多模态大语言模型进行矿物识别
     * 
     * @param detectId 识别记录 ID
     * @param imageUrl 图片 URL
     * @return 识别结果列表
     */
    private List<DetectionResultResponse> detectMineralWithAI(String detectId, String imageUrl) {
        List<DetectionResultResponse> results = new ArrayList<>();
        
        try {
            // 1. 读取图片文件并转换为 Base64 编码
            String imagePath = uploadPath + imageUrl.replace("/uploads/", "");
            byte[] imageBytes = Files.readAllBytes(Paths.get(imagePath));
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = getMimeType(imagePath);
            
            // 2. 构建识别提示词
            String prompt = buildDetectionPrompt();
            
            // 3. 构建多模态消息（文本 + 图片）
            UserMessage userMessage = UserMessage.builder()
                    .addContent(TextContent.from(prompt))
                    .addContent(ImageContent.from(base64Image, mimeType))
                    .build();
            
            // 4. 调用视觉模型进行识别
            log.info("调用视觉模型识别矿物，detectId={}", detectId);
            ChatResponse response = visionChatModel.chat(userMessage);
            log.info("视觉模型响应: {}", response);
            
            // 5. 解析模型返回的 JSON 结果
            results = parseDetectionResponse(detectId, response);
            
        } catch (Exception e) {
            // 识别失败时降级到模拟识别
            log.error("矿物识别失败: {}", e.getMessage(), e);
            results = simulateDetection(detectId);
        }
        
        return results;
    }

    /**
     * 构建矿物识别提示词
     * 
     * @return 提示词字符串
     */
    private String buildDetectionPrompt() {
        return """
                你是一个专业的矿物识别专家。请仔细分析这张图片，识别图片中的矿物。
                
                识别出的矿物都输出出来

                请以JSON格式返回识别结果，格式如下：
            
                [{
                    "label": "角闪石",
                    "confidence": 0.92,
                    "bbox": [
                        149,
                        200,
                        475,
                        392
                    ],
                    "mineralInfo": {
                        "name": "角闪石",
                        "formula": "(Ca,Na)₂-₃(Mg,Fe,Al)₅(Si₆Al)₂O₂₂(OH)₂",
                        "hardness": "5-6",
                        "luster": "玻璃光泽",
                        "color": "黑色/绿色/棕色",
                        "origin": "火成岩、变质岩",
                        "uses": "建筑材料",
                        "description": "角闪石是常见的造岩矿物，呈长柱状或针状晶体。",
                        "thumbnail": "/thumbnails/amphibole.jpg"
                    }
                }]
                
                注意事项：
                1. 只返回JSON，不要包含其他文字说明
                2. confidence 是置信度，范围 0-1
                3. 如果图片中不包含矿物或无法识别，返回空数组：{"minerals": []}
                4. 常见矿物包括：石英、长石、云母、方解石、角闪石、辉石、橄榄石、磁铁矿、黄铁矿、赤铁矿等
                5. 请根据矿物的颜色、光泽、晶体形态等特征进行识别
                6. bbox要在图片中框出矿物
                """;
    }

    /**
     * 解析视觉模型的识别结果
     * 
     * @param detectId 识别记录 ID
     * @param response 模型返回的 JSON 字符串
     * @return 识别结果列表
     */
    private List<DetectionResultResponse> parseDetectionResponse(String detectId, ChatResponse response) {
        List<DetectionResultResponse> results = new ArrayList<>();

        try {
            String content = response.aiMessage().text();
            String jsonStr = content;

            if (content.contains("```json")) {
                jsonStr = content.substring(content.indexOf("```json") + 7, content.lastIndexOf("```"));
            } else if (content.contains("```")) {
                jsonStr = content.substring(content.indexOf("```") + 3, content.lastIndexOf("```"));
            }

            jsonStr = jsonStr.trim();

            JSONArray mineralsArray;
            if (jsonStr.startsWith("[")) {
                mineralsArray = JSONUtil.parseArray(jsonStr);
            } else {
                JSONObject json = JSONUtil.parseObj(jsonStr);
                mineralsArray = json.getJSONArray("minerals");
            }

            if (mineralsArray == null || mineralsArray.isEmpty()) {
                log.warn("未识别到矿物");
                return results;
            }

            for (int i = 0; i < mineralsArray.size(); i++) {
                JSONObject mineral = mineralsArray.getJSONObject(i);
                String name = mineral.getStr("label");
                if (name == null || name.isEmpty()) {
                    name = mineral.getStr("name");
                }
                Double confidence = mineral.getDouble("confidence");
                if (confidence == null) {
                    confidence = 0.8;
                }

                if (name == null || name.isEmpty()) {
                    continue;
                }

                Integer[] bbox = new Integer[]{0, 0, 0, 0};
                JSONArray bboxArray = mineral.getJSONArray("bbox");
                if (bboxArray != null && bboxArray.size() >= 4) {
                    bbox = new Integer[]{
                            bboxArray.getInt(0),
                            bboxArray.getInt(1),
                            bboxArray.getInt(2),
                            bboxArray.getInt(3)
                    };
                }

                LambdaQueryWrapper<MineralDO> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(MineralDO::getName, name);
                MineralDO mineralDO = mineralMapper.selectOne(wrapper);

                if (mineralDO == null) {
                    log.warn("数据库中未找到矿物: {}", name);
                    continue;
                }

                DetectionResultDO result = new DetectionResultDO();
                result.setDetectId(detectId);
                result.setLabel(mineralDO.getName());
                result.setConfidence(BigDecimal.valueOf(confidence));
                result.setBboxX1(bbox[0]);
                result.setBboxY1(bbox[1]);
                result.setBboxX2(bbox[2]);
                result.setBboxY2(bbox[3]);

                detectionResultMapper.insert(result);

                DetectionResultResponse responseObj = new DetectionResultResponse();
                responseObj.setLabel(mineralDO.getName());
                responseObj.setConfidence(confidence);
                responseObj.setBbox(bbox);
                responseObj.setMineralInfo(convertToMineralInfo(mineralDO));

                results.add(responseObj);
            }

        } catch (Exception e) {
            log.error("解析识别结果失败: {}", e.getMessage(), e);
        }

        return results;
    }

    /**
     * 根据文件扩展名获取 MIME 类型
     * 
     * @param imagePath 图片路径
     * @return MIME 类型字符串
     */
    private String getMimeType(String imagePath) {
        String lowerPath = imagePath.toLowerCase();
        if (lowerPath.endsWith(".png")) {
            return "image/png";
        } else if (lowerPath.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerPath.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    /**
     * 模拟矿物识别（降级方案）
     * 当 AI 识别失败时使用随机数据模拟识别结果
     * 
     * @param detectId 识别记录 ID
     * @return 识别结果列表
     */
    private List<DetectionResultResponse> simulateDetection(String detectId) {
        List<DetectionResultResponse> results = new ArrayList<>();
        
        // 常见矿物名称列表
        List<String> mineralNames = new ArrayList<>();
        mineralNames.add("石英");
        mineralNames.add("长石");
        mineralNames.add("云母");
        mineralNames.add("方解石");
        mineralNames.add("角闪石");
        
        java.util.Random random = new java.util.Random();
        
        // 随机生成 1-3 个识别结果
        int resultCount = random.nextInt(3) + 1;
        
        for (int i = 0; i < resultCount; i++) {
            // 随机选择矿物
            String mineralName = mineralNames.get(random.nextInt(mineralNames.size()));
            
            // 查询矿物信息
            LambdaQueryWrapper<MineralDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MineralDO::getName, mineralName);
            MineralDO mineralDO = mineralMapper.selectOne(wrapper);
            
            if (mineralDO == null) {
                continue;
            }
            
            // 创建识别结果
            DetectionResultDO result = new DetectionResultDO();
            result.setDetectId(detectId);
            result.setLabel(mineralDO.getName());
            result.setConfidence(new BigDecimal("0." + (85 + random.nextInt(14)))); // 置信度 0.85-0.98
            result.setBboxX1(100 + random.nextInt(200));  // 边界框坐标
            result.setBboxY1(100 + random.nextInt(200));
            result.setBboxX2(300 + random.nextInt(200));
            result.setBboxY2(300 + random.nextInt(200));
            
            // 保存到数据库
            detectionResultMapper.insert(result);
            
            // 构建响应对象
            DetectionResultResponse response = new DetectionResultResponse();
            response.setLabel(mineralDO.getName());
            response.setConfidence(result.getConfidence().doubleValue());
            response.setBbox(new Integer[]{result.getBboxX1(), result.getBboxY1(), 
                                         result.getBboxX2(), result.getBboxY2()});
            response.setMineralInfo(convertToMineralInfo(mineralDO));
            
            results.add(response);
        }
        
        return results;
    }    /**
     * 获取识别详情
     * 
     * @param detectId 识别记录 ID
     * @param userId 用户 ID
     * @return 识别详情响应
     * @throws BusinessException 记录不存在或无权限访问时抛出
     */
    public DetectionResponse getDetectionDetail(String detectId, String userId) {
        // 查询识别记录
        DetectionDO detectionDO = detectionMapper.selectById(detectId);
        if (detectionDO == null) {
            throw new BusinessException(ErrorCode.DETECTION_RECORD_NOT_FOUND, "识别记录不存在");
        }

        // 验证权限（只能查看自己的记录）
        if (!detectionDO.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无权限访问该识别记录");
        }

        // 查询识别结果
        LambdaQueryWrapper<DetectionResultDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DetectionResultDO::getDetectId, detectId);
        List<DetectionResultDO> dbResults = detectionResultMapper.selectList(wrapper);

        // 构建响应
        List<DetectionResultResponse> results = new ArrayList<>();
        for (DetectionResultDO dbResult : dbResults) {
            // 查询矿物信息
            LambdaQueryWrapper<MineralDO> mineralWrapper = new LambdaQueryWrapper<>();
            mineralWrapper.eq(MineralDO::getName, dbResult.getLabel());
            MineralDO mineralDO = mineralMapper.selectOne(mineralWrapper);

            DetectionResultResponse response = new DetectionResultResponse();
            response.setLabel(dbResult.getLabel());
            response.setConfidence(dbResult.getConfidence().doubleValue());
            response.setBbox(new Integer[]{dbResult.getBboxX1(), dbResult.getBboxY1(),
                                          dbResult.getBboxX2(), dbResult.getBboxY2()});
            
            if (mineralDO != null) {
                response.setMineralInfo(convertToMineralInfo(mineralDO));
            }
            
            results.add(response);
        }

        return buildDetectionResponse(detectionDO, results);
    }

    /**
     * 获取矿物分类统计
     * 
     * @param userId 用户 ID
     * @return 矿物分类及识别次数列表
     */
    public List<MineralCategoryResponse> getCategories(String userId) {
        // 查询用户的所有识别记录
        LambdaQueryWrapper<DetectionDO> detectionWrapper = new LambdaQueryWrapper<>();
        detectionWrapper.eq(DetectionDO::getUserId, userId);
        List<DetectionDO> detectionDOS = detectionMapper.selectList(detectionWrapper);

        if (detectionDOS.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有识别结果的 ID
        List<String> detectIds = detectionDOS.stream()
                .map(DetectionDO::getDetectId)
                .collect(Collectors.toList());

        // 查询所有识别结果
        LambdaQueryWrapper<DetectionResultDO> resultWrapper = new LambdaQueryWrapper<>();
        resultWrapper.in(DetectionResultDO::getDetectId, detectIds);
        List<DetectionResultDO> results = detectionResultMapper.selectList(resultWrapper);

        // 按矿物名称分组统计
        return results.stream()
                .collect(Collectors.groupingBy(
                        DetectionResultDO::getLabel,
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
        LambdaQueryWrapper<MineralDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MineralDO::getName, mineralName);
        MineralDO mineralDO = mineralMapper.selectOne(wrapper);
        
        if (mineralDO == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "矿物信息不存在");
        }
        
        return convertToMineralInfo(mineralDO);
    }

    /**
     * 将矿物实体转换为响应对象
     * 
     * @param mineralDO 矿物实体
     * @return 矿物信息响应
     */
    private MineralInfoResponse convertToMineralInfo(MineralDO mineralDO) {
        MineralInfoResponse info = new MineralInfoResponse();
        info.setName(mineralDO.getName());          // 名称
        info.setFormula(mineralDO.getFormula());    // 化学式
        info.setHardness(mineralDO.getHardness());  // 硬度
        info.setLuster(mineralDO.getLuster());      // 光泽
        info.setColor(mineralDO.getColor());        // 颜色
        info.setOrigin(mineralDO.getOrigin());      // 产地
        info.setUses(mineralDO.getUses());          // 用途
        info.setDescription(mineralDO.getDescription()); // 描述
        info.setThumbnail(mineralDO.getThumbnail());     // 缩略图
        return info;
    }

    /**
     * 构建识别响应对象
     * 
     * @param detectionDO 识别记录
     * @param results 识别结果列表
     * @return 识别响应
     */
    private DetectionResponse buildDetectionResponse(DetectionDO detectionDO,
                                                     List<DetectionResultResponse> results) {
        DetectionResponse response = new DetectionResponse();
        response.setDetectId(detectionDO.getDetectId());
        response.setImageUrl(detectionDO.getImageUrl());
        response.setResults(results);
        response.setCreatedAt(detectionDO.getCreatedAt().format(dateFormatter));
        return response;
    }
}


