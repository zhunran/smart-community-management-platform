package com.property.module.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
// RAG 相关（暂搁置）：
// import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
// import org.springframework.ai.embedding.EmbeddingModel;
// import org.springframework.ai.vectorstore.SimpleVectorStore;
// import org.springframework.ai.vectorstore.VectorStore;
// import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置
 *
 * ChatClient 用于同步对话，Flux<String> 用于 SSE 流式对话。
 * ChatMemory 由 Spring AI 自动配置提供（默认 MessageWindowChatMemory + InMemoryChatMemoryRepository）。
 */
@Configuration
public class AiConfig {

    // RAG 相关（暂搁置）：内存向量库 Bean，待 RAG 功能恢复时取消注释
    // @Bean
    // @ConditionalOnMissingBean(VectorStore.class)
    // public VectorStore vectorStore(EmbeddingModel embeddingModel) {
    //     return SimpleVectorStore.builder(embeddingModel).build();
    // }

    /**
     * ChatClient Bean
     * 注入 ChatClient.Builder（Spring AI 自动配置）和 ChatMemory（自动配置）
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                // RAG 相关（暂搁置）：QuestionAnswerAdvisor 检索增强，待 RAG 恢复时取消注释
                // , QuestionAnswerAdvisor.builder(vectorStore).build()
                .build();
    }

}