package com.mineral.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class LangChain4jConfig {

    @Value("${dashscope.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${dashscope.api-key:}")
    private String apiKey;

    @Value("${dashscope.model-name:qwen3.5-flash}")
    private String modelName;

    @Value("${dashscope.embedding-model-name:text-embedding-v4}")
    private String embeddingModelName;

    @Value("${dashscope.vision-model-name:qwen-vl-max}")
    private String visionModelName;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("初始化 LangChain4j ChatLanguageModel (DashScope), baseUrl={}, model={}", baseUrl, modelName);
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.ofMinutes(5))
                .build();
    }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        log.info("初始化 LangChain4j StreamingChatLanguageModel (DashScope), baseUrl={}, model={}", baseUrl, modelName);
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.ofMinutes(5))
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("初始化 LangChain4j EmbeddingModel (DashScope), baseUrl={}, model={}", baseUrl, embeddingModelName);
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(embeddingModelName)
                .timeout(Duration.ofMinutes(5))
                .build();
    }

    @Bean
    public ChatLanguageModel visionChatModel() {
        log.info("初始化 LangChain4j VisionChatModel (DashScope), baseUrl={}, model={}", baseUrl, visionModelName);
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(visionModelName)
                .timeout(Duration.ofMinutes(5))
                .build();
    }
}
