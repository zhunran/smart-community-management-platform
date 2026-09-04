package com.property.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * 业主状态枚举
 */
@Getter
@AllArgsConstructor
public enum OwnerStatusEnum {

    SELF_OCCUPIED(1, "自住"),
    RENTED(2, "出租"),
    VACANT(3, "空置");

    private final Integer value;
    private final String label;

    public static OwnerStatusEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (OwnerStatusEnum status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return null;
    }

    /** 安全版：返回 Optional，避免 NPE */
    public static Optional<OwnerStatusEnum> fromValueSafe(Integer value) {
        return Optional.ofNullable(fromValue(value));
    }
}
