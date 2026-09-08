package com.property.module.ai.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * DocQuery（自研 RAG）对接配置。
 *
 * <p>默认 {@code enabled=false}，即默认不创建 DocQueryClient 客户端，确保 RAG 宕机或未部署时
 * 对现有 AI 客服主链路零影响。仅当显式配置 {@code docquery.enabled=true} 时才启用对接。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "docquery")
public class DocQueryProperties {

    /** 是否启用 DocQuery 对接，默认关闭 */
    private boolean enabled = false;

    /** DocQuery 服务地址，如 http://docquery:8000 */
    private String baseUrl = "http://localhost:8000";

    /** API Key（对应 DocQuery 的 ADMIN_API_KEY / X-API-Key 请求头） */
    private String apiKey = "";

    /** 连接超时 */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /** 读取超时（LLM 生成较慢，建议 >= 60s） */
    private Duration readTimeout = Duration.ofSeconds(60);

    /** RAG 模式 */
    private String ragMode = "basic";

    /** 知识库（中文业务需显式指定 shanggongyuan） */
    private String knowledgeBase = "shanggongyuan";
}