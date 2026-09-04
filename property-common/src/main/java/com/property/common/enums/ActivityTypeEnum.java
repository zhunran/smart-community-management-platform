package com.property.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * 活动类型枚举
 */
@Getter
@AllArgsConstructor
public enum ActivityTypeEnum {

    FESTIVAL(1, "节日活动"),
    PARENT_CHILD(2, "亲子"),
    SPORTS(3, "运动"),
    LECTURE(4, "讲座"),
    OTHER(5, "其他");

    private final Integer value;
    private final String label;

    public static ActivityTypeEnum fromValue(Integer value) {
        if (value == null) return null;
        for (ActivityTypeEnum s : values()) {
            if (s.value.equals(value)) return s;
        }
        return null;
    }

    public static Optional<ActivityTypeEnum> fromValueSafe(Integer value) {
        return Optional.ofNullable(fromValue(value));
    }
}