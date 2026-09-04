package com.property.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * 评论状态枚举
 */
@Getter
@AllArgsConstructor
public enum CommentStatusEnum {

    PENDING_AUDIT(0, "待审核"),
    NORMAL(1, "正常"),
    DELETED(2, "已删除");

    private final Integer value;
    private final String label;

    public static CommentStatusEnum fromValue(Integer value) {
        if (value == null) return null;
        for (CommentStatusEnum s : values()) {
            if (s.value.equals(value)) return s;
        }
        return null;
    }

    public static Optional<CommentStatusEnum> fromValueSafe(Integer value) {
        return Optional.ofNullable(fromValue(value));
    }
}