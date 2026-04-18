package com.mineral.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.redis.RedisEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    @Value("${embedding.store.type:memory}")
    private String embeddingStoreType;

    @Value("${embedding.store.redis.chat-index:chat_history_index}")
    private String chatIndex;

    @Value("${embedding.store.redis.chat-prefix:chat:history:}")
    private String chatPrefix;

    @Value("${embedding.store.redis.document-index:document_index}")
    private String documentIndex;

    @Value("${embedding.store.redis.document-prefix:document:}")
    private String documentPrefix;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        log.info("初始化 Redis 连接, host={}, port={}", redisHost, redisPort);
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        config.setDatabase(redisDatabase);
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    @Bean
    @Primary
    public EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel) {
        if ("redis".equalsIgnoreCase(embeddingStoreType)) {
            log.info("使用 Redis EmbeddingStore (聊天消息), index={}, prefix={}", chatIndex, chatPrefix);
            return RedisEmbeddingStore.builder()
                    .host(redisHost)
                    .port(redisPort)
                    .dimension(embeddingModel.dimension())
                    .indexName(chatIndex)
                    .prefix(chatPrefix)
                    .build();
        } else {
            log.info("使用 InMemory EmbeddingStore (聊天消息)");
            return new InMemoryEmbeddingStore<>();
        }
    }

    @Bean("documentEmbeddingStore")
    public EmbeddingStore<TextSegment> documentEmbeddingStore(EmbeddingModel embeddingModel) {
        if ("redis".equalsIgnoreCase(embeddingStoreType)) {
            log.info("使用 Redis EmbeddingStore (文档), index={}, prefix={}", documentIndex, documentPrefix);
            return RedisEmbeddingStore.builder()
                    .host(redisHost)
                    .port(redisPort)
                    .dimension(embeddingModel.dimension())
                    .indexName(documentIndex)
                    .prefix(documentPrefix)
                    .build();
        } else {
            log.info("使用 InMemory EmbeddingStore (文档)");
            return new InMemoryEmbeddingStore<>();
        }
    }
}