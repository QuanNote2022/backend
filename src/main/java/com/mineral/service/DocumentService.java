package com.mineral.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mineral.common.BusinessException;
import com.mineral.common.ErrorCode;
import com.mineral.entity.FileDocumentDO;
import com.mineral.mapper.FileDocumentMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DocumentService {

    private final FileDocumentMapper fileDocumentMapper;
    private final EmbeddingStore<TextSegment> documentEmbeddingStore;
    private final EmbeddingModel embeddingModel;

    @Value("${upload.path}")
    private String uploadPath;

    private static final List<String> ALLOWED_TYPES = List.of(
            "txt", "md", "markdown",
            "doc", "docx",
            "xls", "xlsx"
    );
    private static final int CHUNK_SIZE = 512;
    private static final int MAX_CONTENT_LENGTH = 50000;

    public DocumentService(
            FileDocumentMapper fileDocumentMapper,
            @Qualifier("documentEmbeddingStore") EmbeddingStore<TextSegment> documentEmbeddingStore,
            EmbeddingModel embeddingModel) {
        this.fileDocumentMapper = fileDocumentMapper;
        this.documentEmbeddingStore = documentEmbeddingStore;
        this.embeddingModel = embeddingModel;
    }

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
            log.info("文件上传成功: documentId={}, fileName={}", document.getDocumentId(), originalFilename);

            return document;
        } catch (IOException e) {
            log.error("文件保存失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件保存失败");
        }
    }

    public void indexDocument(String documentId) {
        FileDocumentDO document = fileDocumentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "文档不存在");
        }

        String filePath = uploadPath + document.getFileUrl().replace("/uploads/", "");
        File file = new File(filePath);

        if (!file.exists()) {
            log.error("文件不存在: {}", filePath);
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "文件不存在");
        }

        log.info("开始索引文档: documentId={}, fileName={}, fileType={}", documentId, document.getFileName(), document.getFileType());

        try {
            String content = extractTextContent(file, document.getFileType());
            if (content == null || content.isEmpty()) {
                log.warn("文档内容为空: documentId={}", documentId);
                document.setStatus(1);
                fileDocumentMapper.updateById(document);
                return;
            }

            if (content.length() > MAX_CONTENT_LENGTH) {
                log.info("文档内容过长，截取前 {} 字符: documentId={}", MAX_CONTENT_LENGTH, documentId);
                content = content.substring(0, MAX_CONTENT_LENGTH);
            }

            log.info("文档内容长度: {} 字符", content.length());

            List<TextSegment> segments = splitIntoChunks(content, documentId);
            log.info("文档分块数量: {}", segments.size());

            int successCount = 0;
            for (int i = 0; i < segments.size(); i++) {
                try {
                    TextSegment segment = segments.get(i);
                    Embedding embedding = embeddingModel.embed(segment).content();
                    documentEmbeddingStore.add(embedding, segment);
                    successCount++;
                    log.debug("索引分块 {}/{}: {} 字符", i + 1, segments.size(), segment.text().length());
                } catch (Exception e) {
                    log.error("索引分块失败: index={}, error={}", i, e.getMessage());
                }
            }

            document.setStatus(1);
            fileDocumentMapper.updateById(document);
            log.info("文档索引完成: documentId={}, totalChunks={}, successChunks={}", documentId, segments.size(), successCount);
        } catch (Exception e) {
            log.error("文档索引失败: documentId={}, error={}", documentId, e.getMessage(), e);
            document.setStatus(2);
            fileDocumentMapper.updateById(document);
        }
    }

    public String extractTextContent(File file, String fileType) {
        try {
            String lowerType = fileType.toLowerCase();

            switch (lowerType) {
                case "txt":
                    return FileUtil.readString(file, StandardCharsets.UTF_8);

                case "md":
                case "markdown":
                    return FileUtil.readString(file, StandardCharsets.UTF_8);

                case "doc":
                    return extractDocContent(file);

                case "docx":
                    return extractDocxContent(file);

                case "xls":
                    return extractXlsContent(file);

                case "xlsx":
                    return extractXlsxContent(file);

                default:
                    log.warn("暂不支持提取文件类型 {} 的文本内容", fileType);
                    return "";
            }
        } catch (Exception e) {
            log.error("提取文件文本失败: file={}, error={}", file.getAbsolutePath(), e.getMessage(), e);
            return "";
        }
    }

    private String extractDocContent(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument doc = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(doc)) {
            return extractor.getText();
        } catch (Exception e) {
            log.error("提取 DOC 文件内容失败: {}", e.getMessage(), e);
            return "";
        }
    }

    private String extractDocxContent(File file) {
        StringBuilder content = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument doc = new XWPFDocument(fis)) {
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            for (XWPFParagraph para : paragraphs) {
                String text = para.getText();
                if (text != null && !text.trim().isEmpty()) {
                    content.append(text).append("\n");
                }
            }
        } catch (Exception e) {
            log.error("提取 DOCX 文件内容失败: {}", e.getMessage(), e);
        }
        return content.toString();
    }

    private String extractXlsContent(File file) {
        StringBuilder content = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {
            extractExcelContent(workbook, content);
        } catch (Exception e) {
            log.error("提取 Excel 文件内容失败: {}", e.getMessage(), e);
        }
        return content.toString();
    }

    private String extractXlsxContent(File file) {
        StringBuilder content = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {
            extractExcelContent(workbook, content);
        } catch (Exception e) {
            log.error("提取 Excel 文件内容失败: {}", e.getMessage(), e);
        }
        return content.toString();
    }

    private void extractExcelContent(Workbook workbook, StringBuilder content) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            content.append("【工作表: ").append(sheet.getSheetName()).append("】\n");

            for (Row row : sheet) {
                StringBuilder rowContent = new StringBuilder();
                for (Cell cell : row) {
                    String cellValue = getCellValue(cell);
                    if (cellValue != null && !cellValue.isEmpty()) {
                        rowContent.append(cellValue).append("\t");
                    }
                }
                if (rowContent.length() > 0) {
                    content.append(rowContent.toString().trim()).append("\n");
                }
            }
            content.append("\n");
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

    private List<TextSegment> splitIntoChunks(String content, String documentId) {
        List<TextSegment> chunks = new ArrayList<>();
        int length = content.length();

        for (int i = 0; i < length; i += CHUNK_SIZE) {
            int end = Math.min(i + CHUNK_SIZE, length);
            String chunk = content.substring(i, end);
            TextSegment segment = TextSegment.from(chunk);
            chunks.add(segment);
        }

        return chunks;
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

        if (!ALLOWED_TYPES.contains(ext.toLowerCase())) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED,
                    "不支持的文件类型，支持: txt, md, doc, docx, xls, xlsx");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED, "文件大小不能超过 10MB");
        }
    }

    private String getFileType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}