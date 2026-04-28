package com.mineral.config;

import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
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

import java.net.http.HttpClient;
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

    /**
     * 创建 JDK HttpClient Builder，强制使用 HTTP/1.1。
     * JDK HttpClient 默认使用 HTTP/2，但 LM Studio 不支持 HTTP/2，
     * 会导致协议协商失败、请求卡住直到超时。
     */
    private JdkHttpClientBuilder createHttpClientBuilder() {
        HttpClient.Builder jdkBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30));
        return new JdkHttpClientBuilder().httpClientBuilder(jdkBuilder);
    }

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("初始化 LangChain4j ChatLanguageModel, baseUrl={}, model={}", baseUrl, modelName);
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.ofMinutes(5))
                .httpClientBuilder(createHttpClientBuilder())
                .build();
    }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        log.info("初始化 LangChain4j StreamingChatLanguageModel, baseUrl={}, model={}", baseUrl, modelName);
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.ofMinutes(5))
                .httpClientBuilder(createHttpClientBuilder())
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("初始化 LangChain4j EmbeddingModel, baseUrl={}, model={}", baseUrl, embeddingModelName);
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(embeddingModelName)
                .timeout(Duration.ofMinutes(5))
                .httpClientBuilder(createHttpClientBuilder())
                .build();
    }

    @Bean
    public ChatLanguageModel visionChatModel() {
        log.info("初始化 LangChain4j VisionChatModel, baseUrl={}, model={}", baseUrl, visionModelName);
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(visionModelName)
                .timeout(Duration.ofMinutes(5))
                .httpClientBuilder(createHttpClientBuilder())
                .build();
    }
}
