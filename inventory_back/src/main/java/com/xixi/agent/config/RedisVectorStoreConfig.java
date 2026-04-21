package com.xixi.agent.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RedisVectorStoreConfig {
    @Bean(destroyMethod = "close")
    public JedisPooled  ragjedisPooled(RedisProperties  redisProperties){
        DefaultJedisClientConfig.Builder clientConfigBuilder = DefaultJedisClientConfig.builder()
                .database(redisProperties.getDatabase());
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isBlank()) {
            clientConfigBuilder.password(redisProperties.getPassword());
        }
        if (redisProperties.getTimeout() != null) {
            clientConfigBuilder.timeoutMillis((int) redisProperties.getTimeout().toMillis());
        }
        return new JedisPooled(
                new HostAndPort(redisProperties.getHost(), redisProperties.getPort()),
                clientConfigBuilder.build());
    }
    @Bean
    public VectorStore vectorStore(JedisPooled ragjedisPooled,
                                   EmbeddingModel embeddingModel,
                                   @Value("${spring.ai.vectorstore.redis.index-name:inventory-agent-rag-index}") String indexName,
                                   @Value("${spring.ai.vectorstore.redis.prefix:inventory:agent:rag:}") String prefix,
                                   @Value("${spring.ai.vectorstore.redis.initialize-schema:true}") boolean initializeSchema){
        return RedisVectorStore.builder(ragjedisPooled,embeddingModel)
                .indexName(indexName)
                .prefix(prefix)
                .initializeSchema(initializeSchema)
                .metadataFields(
                        RedisVectorStore.MetadataField.tag("docCode"),
                        RedisVectorStore.MetadataField.text("title"),
                        RedisVectorStore.MetadataField.tag("bizIntent"),
                        RedisVectorStore.MetadataField.tag("docType")
                )
                .build();

    }
}
