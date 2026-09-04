package com.property.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * 访客通行码状态枚举
 */
@Getter
@AllArgsConstructor
public enum VisitorPassStatusEnum {

    VALID(0, "有效"),
    USED_UP(1, "已用尽"),
    EXPIRED(2, "已过期"),
    REVOKED(3, "已撤销");

    private final Integer value;
    private final String label;

    public static VisitorPassStatusEnum fromValue(Integer value) {
        if (value == null) return null;
        for (VisitorPassStatusEnum s : values()) {
            if (s.value.equals(value)) return s;
        }
        return null;
    }

    public static Optional<VisitorPassStatusEnum> fromValueSafe(Integer value) {
        return Optional.ofNullable(fromValue(value));
    }
}
