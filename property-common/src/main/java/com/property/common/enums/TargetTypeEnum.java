package com.property.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * 点赞目标类型枚举
 */
@Getter
@AllArgsConstructor
public enum TargetTypeEnum {

    POST(1, "帖子"),
    COMMENT(2, "评论");

    private final Integer value;
    private final String label;

    public static TargetTypeEnum fromValue(Integer value) {
        if (value == null) return null;
        for (TargetTypeEnum s : values()) {
            if (s.value.equals(value)) return s;
        }
        return null;
    }

    public static Optional<TargetTypeEnum> fromValueSafe(Integer value) {
        return Optional.ofNullable(fromValue(value));
    }
}