package com.property.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * 报修工单状态枚举（8 态）
 */
@Getter
@AllArgsConstructor
public enum RepairStatusEnum {

    PENDING_AUDIT(0, "待审核"),
    PENDING_ASSIGN(1, "待派单"),
    ASSIGNED(2, "已派单"),
    REPAIRING(3, "维修中"),
    COMPLETED(4, "已完成"),
    RATED(5, "已评价"),
    REJECTED(6, "已驳回"),
    CANCELED(7, "已取消");

    private final Integer value;
    private final String label;

    public static RepairStatusEnum fromValue(Integer value) {
        if (value == null) return null;
        for (RepairStatusEnum s : values()) {
            if (s.value.equals(value)) return s;
        }
        return null;
    }

    public static Optional<RepairStatusEnum> fromValueSafe(Integer value) {
        return Optional.ofNullable(fromValue(value));
    }

    /** 业主可取消的状态 */
    public boolean canCancel() {
        return this == PENDING_AUDIT || this == PENDING_ASSIGN;
    }

    /** 可评价的状态 */
    public boolean canRate() {
        return this == COMPLETED;
    }

    /** 维修员可接单的状态 */
    public boolean canAccept() {
        return this == ASSIGNED;
    }

    /** 维修员可完工的状态 */
    public boolean canComplete() {
        return this == REPAIRING;
    }

    /** 管理端可审核的状态 */
    public boolean canAudit() {
        return this == PENDING_AUDIT;
    }

    /** 管理端可派单的状态 */
    public boolean canAssign() {
        return this == PENDING_ASSIGN;
    }
}