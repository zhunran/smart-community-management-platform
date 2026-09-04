package com.property.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * 报名状态枚举
 */
@Getter
@AllArgsConstructor
public enum SignupStatusEnum {

    SIGNED_UP(0, "已报名"),
    CHECKED_IN(1, "已签到"),
    CANCELED(2, "已取消");

    private final Integer value;
    private final String label;

    public static SignupStatusEnum fromValue(Integer value) {
        if (value == null) return null;
        for (SignupStatusEnum s : values()) {
            if (s.value.equals(value)) return s;
        }
        return null;
    }

    public static Optional<SignupStatusEnum> fromValueSafe(Integer value) {
        return Optional.ofNullable(fromValue(value));
    }
}