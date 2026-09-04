package com.property.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * 启用/禁用枚举
 */
@Getter
@AllArgsConstructor
public enum StatusEnum {

    ENABLED(1, "启用"),
    DISABLED(0, "禁用");

    private final Integer value;
    private final String label;

    public static StatusEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (StatusEnum status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return null;
    }

    /** 安全版：返回 Optional，避免 NPE */
    public static Optional<StatusEnum> fromValueSafe(Integer value) {
        return Optional.ofNullable(fromValue(value));
    }
}
