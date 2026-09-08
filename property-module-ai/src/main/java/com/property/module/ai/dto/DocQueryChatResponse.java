package com.property.module.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DocQuery 非流式 RAG 问答响应。
 */
public record DocQueryChatResponse(
        @JsonProperty("conversation_id") String conversationId,
        String query,
        String answer,
        List<DocQueryCitation> citations,
        @JsonProperty("retrieval_latency_ms") double retrievalLatencyMs,
        @JsonProperty("generation_latency_ms") double generationLatencyMs) {
}