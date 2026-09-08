package com.property.module.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DocQuery 语义检索响应。
 */
public record DocQuerySearchResponse(
        String query,
        List<DocQuerySearchResult> results,
        @JsonProperty("latency_ms") double latencyMs) {
}