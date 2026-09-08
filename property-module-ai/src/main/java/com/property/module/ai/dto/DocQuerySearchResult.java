package com.property.module.ai.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DocQuery 语义检索单条结果。
 */
public record DocQuerySearchResult(
        String id,
        String content,
        double score,
        @JsonProperty("rerank_score") Double rerankScore,
        @JsonProperty("rrf_score") Double rrfScore,
        String source,
        Map<String, Object> metadata) {
}