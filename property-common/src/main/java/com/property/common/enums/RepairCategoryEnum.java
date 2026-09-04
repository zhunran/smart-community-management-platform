package com.property.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * 报修分类枚举
 */
@Getter
@AllArgsConstructor
public enum RepairCategoryEnum {

    PLUMBING_ELECTRIC(1, "水电"),
    DOORS_WINDOWS(2, "门窗"),
    ELEVATOR(3, "电梯"),
    PUBLIC_FACILITY(4, "公共设施"),
    OTHER(5, "其他");

    private final Integer value;
    private final String label;

    public static RepairCategoryEnum fromValue(Integer value) {
        if (value == null) return null;
        for (RepairCategoryEnum s : values()) {
            if (s.value.equals(value)) return s;
        }
        return null;
    }

    public static Optional<RepairCategoryEnum> fromValueSafe(Integer value) {
        return Optional.ofNullable(fromValue(value));
    }
}