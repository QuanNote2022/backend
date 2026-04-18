package com.mineral.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mineral.common.BusinessException;
import com.mineral.common.ErrorCode;
import com.mineral.entity.FileDocumentDO;
import com.mineral.mapper.FileDocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final FileDocumentMapper fileDocumentMapper;
    private final DocumentIndexService documentIndexService;

    @Value("${upload.path}")
    private String uploadPath;

    private static final List<String> ALLOWED_TYPES = List.of(
            "txt", "md", "markdown",
            "doc", "docx",
            "xls", "xlsx"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Transactional(rollbackFor = Exception.class)
    public FileDocumentDO uploadFile(MultipartFile file, String sessionId, String userId) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String fileType = getFileType(originalFilename);

        String fileName = IdUtil.fastSimpleUUID() + "_" + originalFilename;
        String subPath = "documents/" + java.time.LocalDate.now().toString().replace("-", "/");
        String dir = uploadPath + subPath;
        FileUtil.mkdir(dir);

        try {
            File destFile = new File(dir, fileName);
            file.transferTo(destFile);

            String fileUrl = "/uploads/" + subPath + "/" + fileName;

            FileDocumentDO document = new FileDocumentDO();
            document.setDocumentId(IdUtil.getSnowflakeNextIdStr());
            document.setSessionId(sessionId);
            document.setUserId(userId);
            document.setFileName(originalFilename);
            document.setFileUrl(fileUrl);
            document.setFileType(fileType);
            document.setStatus(0);
            document.setCreatedAt(LocalDateTime.now());

            fileDocumentMapper.insert(document);
            log.info("文件上传成功: documentId={}, fileName={}, fileType={}", document.getDocumentId(), originalFilename, fileType);

            documentIndexService.indexDocumentAsync(document.getDocumentId());

            return document;
        } catch (IOException e) {
            log.error("文件保存失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件保存失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(String documentId) {
        FileDocumentDO document = fileDocumentMapper.selectById(documentId);
        if (document == null) {
            return;
        }

        String filePath = uploadPath + document.getFileUrl().replace("/uploads/", "");
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }

        fileDocumentMapper.deleteById(documentId);
        log.info("文档删除成功: documentId={}", documentId);
    }

    public List<FileDocumentDO> getSessionDocuments(String sessionId) {
        LambdaQueryWrapper<FileDocumentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileDocumentDO::getSessionId, sessionId)
                .orderByDesc(FileDocumentDO::getCreatedAt);
        return fileDocumentMapper.selectList(wrapper);
    }

    private void validateFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String ext = getFileType(originalFilename);

        if (ext.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED, "无法识别文件类型");
        }

        if (!ALLOWED_TYPES.contains(ext.toLowerCase())) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED,
                    "不支持的文件类型: " + ext + "，支持: txt, md, doc, docx, xls, xlsx");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED, "文件大小不能超过 10MB");
        }

        log.info("文件验证通过: filename={}, type={}, size={}", originalFilename, ext, file.getSize());
    }

    private String getFileType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}