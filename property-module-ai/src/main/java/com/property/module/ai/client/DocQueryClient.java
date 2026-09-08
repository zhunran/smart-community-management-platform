package com.property.module.ai.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.property.module.ai.config.DocQueryProperties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

/**
 * DocQuery（自研 RAG）客户端。
 *
 * <p>仅当 {@code docquery.enabled=true} 时创建该 Bean；默认关闭，保证 RAG 未部署或宕机时
 * 不影响现有 AI 客服主链路。所有流式调用均带超时与异常兜底（异常时返回空流），由上层编排回退。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "docquery", name = "enabled", havingValue = "true")
public class DocQueryClient {

    private final WebClient webClient;
    private final DocQueryProperties properties;
    private final ObjectMapper objectMapper;

    public DocQueryClient(DocQueryProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.getConnectTimeout().toMillis())
                .responseTimeout(properties.getReadTimeout());

        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("X-API-Key", properties.getApiKey())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * 流式 RAG 问答（SSE）。
     *
     * <p>返回纯 token 文本流（已剥离 {@code data:} 前缀与 JSON 封装），与主链路
     * {@code Flux<String>} 输出格式一致。</p>
     *
     * <p><b>异常策略：</b>连接失败、超时、DocQuery 返回 {@code error} 事件等一律以
     * 受控异常向上传播（不吞错为空流），由编排层统一的 {@code onErrorResume} 回退主链路，
     * 从而保证「DocQuery 宕机/异常时前端收到完整兜底回答，而非空白」。</p>
     *
     * @param query          用户问题
     * @param conversationId 多轮对话 ID（可为 null，表示单轮）
     */
    public Flux<String> chatStream(String query, String conversationId) {
        return webClient.get()
                .uri(uriBuilder -> {
                    UriBuilder builder = uriBuilder
                            .path("/api/v1/chat/stream")
                            .queryParam("query", query)
                            .queryParam("knowledge_base", properties.getKnowledgeBase())
                            .queryParam("rag_mode", properties.getRagMode());
                    if (conversationId != null && !conversationId.isBlank()) {
                        builder.queryParam("conversation_id", conversationId);
                    }
                    return builder.build();
                })
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .mapNotNull(this::extractToken)
                .timeout(properties.getReadTimeout())
                .doOnError(e -> log.warn("DocQuery 流式调用异常，交由上层回退主链路：{}", e.getMessage()));
    }

    /**
     * 解析 DocQuery SSE 事件，仅提取 {@code token} 类型事件中的文本内容。
     *
     * <p>{@code done}/{@code citation}/{@code [DONE]}/空行返回 {@code null}（由 mapNotNull 过滤）；
     * {@code error} 事件抛受控异常，终止本流并向上传播，交由编排层回退主链路。</p>
     */
    private String extractToken(ServerSentEvent<String> sse) {
        if (sse == null) {
            return null;
        }
        String data = sse.data();
        if (data == null) {
            return null;
        }
        String line = data.trim();
        if (line.isEmpty() || "[DONE]".equals(line)) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(line);
            String type = node.path("type").asText();
            if ("token".equals(type)) {
                // 过滤换行符：避免 SSE 写入时被拆成多行 data: 导致前端解析异常
                return node.path("data").asText().replace("\r", "").replace("\n", " ");
            }
            if ("error".equals(type)) {
                throw new IllegalStateException("DocQuery 返回错误事件：" + node.path("data").asText());
            }
            return null;
        } catch (Exception e) {
            log.debug("DocQuery SSE 数据解析失败，跳过该事件：{}", line);
            return null;
        }
    }
}