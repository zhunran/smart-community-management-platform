package com.property.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * 费用类型枚举
 */
@Getter
@AllArgsConstructor
public enum FeeTypeEnum {

    PROPERTY_FEE("property_fee", "物业费"),
    WATER_FEE("water_fee", "水费"),
    ELECTRICITY_FEE("electricity_fee", "电费"),
    GAS_FEE("gas_fee", "燃气费"),
    PARKING_FEE("parking_fee", "车位费"),
    MAINTENANCE_FEE("maintenance_fee", "维修费"),
    OTHER("other", "其他");

    private final String code;
    private final String label;

    public static FeeTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (FeeTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    /** 安全版：返回 Optional，避免 NPE */
    public static Optional<FeeTypeEnum> fromCodeSafe(String code) {
        return Optional.ofNullable(fromCode(code));
    }
}
