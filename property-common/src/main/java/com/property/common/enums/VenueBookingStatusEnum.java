package com.property.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * 场地预约状态枚举
 */
@Getter
@AllArgsConstructor
public enum VenueBookingStatusEnum {

    BOOKED(0, "已预约"),
    USED(1, "已使用"),
    CANCELED(2, "已取消"),
    VIOLATED(3, "已违约");

    private final Integer value;
    private final String label;

    public static VenueBookingStatusEnum fromValue(Integer value) {
        if (value == null) return null;
        for (VenueBookingStatusEnum s : values()) {
            if (s.value.equals(value)) return s;
        }
        return null;
    }

    public static Optional<VenueBookingStatusEnum> fromValueSafe(Integer value) {
        return Optional.ofNullable(fromValue(value));
    }

    /** 是否占用（参与冲突检测的有效预约） */
    public boolean isActive() {
        return this == BOOKED || this == USED;
    }
}
