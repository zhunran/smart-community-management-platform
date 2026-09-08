package com.property.module.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DocQuery 引用来源。
 */
public record DocQueryCitation(
        int index,
        @JsonProperty("source_url") String sourceUrl,
        String title,
        String h2,
        @JsonProperty("chunk_id") String chunkId) {
}