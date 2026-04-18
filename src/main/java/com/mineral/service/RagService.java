package com.mineral.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RagService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingStore<TextSegment> documentEmbeddingStore;
    private final EmbeddingModel embeddingModel;

    private static final int MAX_RESULTS = 10;
    private static final double MIN_SCORE = 0.0;

    public RagService(
            EmbeddingStore<TextSegment> embeddingStore,
            @Qualifier("documentEmbeddingStore") EmbeddingStore<TextSegment> documentEmbeddingStore,
            EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.documentEmbeddingStore = documentEmbeddingStore;
        this.embeddingModel = embeddingModel;
    }

    public void indexMessage(String sessionId, String role, String content) {
        try {
            String text = String.format("[%s]: %s", role, content);
            TextSegment segment = TextSegment.from(text);
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
            log.info("索引消息到向量存储: sessionId={}, role={}, contentLength={}", sessionId, role, content.length());
        } catch (Exception e) {
            log.error("索引消息失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
        }
    }

    public void indexDocument(String documentId, String content) {
        try {
            TextSegment segment = TextSegment.from(content);
            Embedding embedding = embeddingModel.embed(segment).content();
            documentEmbeddingStore.add(embedding, segment);
            log.info("索引文档到向量存储: documentId={}, contentLength={}", documentId, content.length());
        } catch (Exception e) {
            log.error("索引文档失败: documentId={}, error={}", documentId, e.getMessage(), e);
            throw new RuntimeException("索引文档失败: " + e.getMessage(), e);
        }
    }

    public List<String> searchInDocuments(String query) {
        try {
            log.info("开始搜索文档, query={}", query);
            
            Embedding queryEmbedding = embeddingModel.embed(query).content();

            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(MAX_RESULTS)
                    .minScore(MIN_SCORE)
                    .build();

            EmbeddingSearchResult<TextSegment> searchResult = documentEmbeddingStore.search(searchRequest);

            List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();
            log.info("搜索到 {} 条结果", matches.size());

            for (int i = 0; i < matches.size(); i++) {
                EmbeddingMatch<TextSegment> match = matches.get(i);
                log.info("结果 {}: score={}, text={}", i + 1, match.score(), 
                        match.embedded().text().substring(0, Math.min(100, match.embedded().text().length())) + "...");
            }

            List<String> relevantDocs = matches.stream()
                    .filter(match -> match.score() >= 0.3)
                    .map(match -> match.embedded().text())
                    .collect(Collectors.toList());

            log.info("过滤后保留 {} 条相关文档内容 (score >= 0.3)", relevantDocs.size());
            return relevantDocs;
        } catch (Exception e) {
            log.error("搜索文档失败: query={}, error={}", query, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public String buildContextWithHistoryAndDocuments(String userQuery, String mineralContext, String sessionId) {
        StringBuilder contextBuilder = new StringBuilder();

        try {
            List<String> relevantDocs = searchInDocuments(userQuery);
            if (!relevantDocs.isEmpty()) {
                contextBuilder.append("=== 用户上传的文档内容 ===\n");
                for (String doc : relevantDocs) {
                    contextBuilder.append(doc).append("\n");
                }
                contextBuilder.append("=== 文档内容结束 ===\n\n");
            } else {
                log.info("未找到相关文档内容, userQuery={}", userQuery);
            }
        } catch (Exception e) {
            log.error("构建文档上下文失败: {}", e.getMessage(), e);
        }

        if (mineralContext != null && !mineralContext.isEmpty()) {
            contextBuilder.append("=== 当前矿物信息 ===\n");
            contextBuilder.append(mineralContext).append("\n");
            contextBuilder.append("=== 当前矿物信息结束 ===\n\n");
        }

        log.info("构建的上下文长度: {} 字符", contextBuilder.length());
        return contextBuilder.toString();
    }
}