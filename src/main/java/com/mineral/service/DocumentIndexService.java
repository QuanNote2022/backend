package com.mineral.service;

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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DocumentIndexService {

    private final FileDocumentMapper fileDocumentMapper;
    private final EmbeddingStore<TextSegment> documentEmbeddingStore;
    private final EmbeddingModel embeddingModel;

    @Value("${upload.path}")
    private String uploadPath;

    private static final int CHUNK_SIZE = 512;
    private static final int MAX_CONTENT_LENGTH = 50000;

    public DocumentIndexService(
            FileDocumentMapper fileDocumentMapper,
            @Qualifier("documentEmbeddingStore") EmbeddingStore<TextSegment> documentEmbeddingStore,
            EmbeddingModel embeddingModel) {
        this.fileDocumentMapper = fileDocumentMapper;
        this.documentEmbeddingStore = documentEmbeddingStore;
        this.embeddingModel = embeddingModel;
    }

    @Async("documentIndexExecutor")
    public void indexDocumentAsync(String documentId) {
        log.info("开始异步索引文档: documentId={}", documentId);
        indexDocument(documentId);
    }

    public void indexDocument(String documentId) {
        FileDocumentDO document = fileDocumentMapper.selectById(documentId);
        if (document == null) {
            log.error("文档不存在: documentId={}", documentId);
            return;
        }

        String filePath = uploadPath + document.getFileUrl().replace("/uploads/", "");
        File file = new File(filePath);

        if (!file.exists()) {
            log.error("文件不存在: {}", filePath);
            document.setStatus(2);
            fileDocumentMapper.updateById(document);
            return;
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
                    return cn.hutool.core.io.FileUtil.readString(file, StandardCharsets.UTF_8);

                case "md":
                case "markdown":
                    return cn.hutool.core.io.FileUtil.readString(file, StandardCharsets.UTF_8);

                case "doc":
                    return extractDocContent(file);

                case "docx":
                    return extractDocxContent(file);

                case "xls":
                case "xlsx":
                    return extractExcelContent(file);

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
            String text = extractor.getText();
            log.info("DOC 文件提取成功: {} 字符", text != null ? text.length() : 0);
            return text != null ? text : "";
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
            log.info("DOCX 文件提取成功: {} 字符", content.length());
        } catch (Exception e) {
            log.error("提取 DOCX 文件内容失败: {}", e.getMessage(), e);
        }
        return content.toString();
    }

    private String extractExcelContent(File file) {
        StringBuilder content = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {
            
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
            log.info("Excel 文件提取成功: {} 字符", content.length());
        } catch (Exception e) {
            log.error("提取 Excel 文件内容失败: {}", e.getMessage(), e);
        }
        return content.toString();
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
}
