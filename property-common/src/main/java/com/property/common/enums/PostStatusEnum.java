package com.property.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * 帖子状态枚举
 */
@Getter
@AllArgsConstructor
public enum PostStatusEnum {

    PENDING_AUDIT(0, "待审核"),
    PUBLISHED(1, "已发布"),
    REJECTED(2, "已驳回"),
    DELETED(3, "已删除");

    private final Integer value;
    private final String label;

    public static PostStatusEnum fromValue(Integer value) {
        if (value == null) return null;
        for (PostStatusEnum s : values()) {
            if (s.value.equals(value)) return s;
        }
        return null;
    }

    public static Optional<PostStatusEnum> fromValueSafe(Integer value) {
        return Optional.ofNullable(fromValue(value));
    }
}