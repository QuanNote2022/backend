package com.mineral.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    private static final int MAX_RESULTS = 5;
    private static final double MIN_SCORE = 0.7;

    public void indexMessage(String sessionId, String role, String content) {
        String text = String.format("[%s]: %s", role, content);
        TextSegment segment = TextSegment.from(text);
        Embedding embedding = embeddingModel.embed(segment).content();
        embeddingStore.add(embedding, segment);
        log.info("索引消息到向量存储: sessionId={}, role={}", sessionId, role);
    }

    public List<String> searchRelevantContext(String query) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(MAX_RESULTS)
                .minScore(MIN_SCORE)
                .build();
        
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        
        List<String> relevantContexts = searchResult.matches().stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.toList());
        
        log.info("搜索到 {} 条相关上下文", relevantContexts.size());
        return relevantContexts;
    }

    public String buildContextWithHistory(String userQuery, String mineralContext) {
        List<String> relevantHistory = searchRelevantContext(userQuery);
        
        StringBuilder contextBuilder = new StringBuilder();
        
        if (!relevantHistory.isEmpty()) {
            contextBuilder.append("=== 历史对话记录 ===\n");
            for (String history : relevantHistory) {
                contextBuilder.append(history).append("\n");
            }
            contextBuilder.append("=== 历史对话记录结束 ===\n\n");
        }
        
        if (mineralContext != null && !mineralContext.isEmpty()) {
            contextBuilder.append("=== 当前矿物信息 ===\n");
            contextBuilder.append(mineralContext).append("\n");
            contextBuilder.append("=== 当前矿物信息结束 ===\n\n");
        }
        
        return contextBuilder.toString();
    }
}
