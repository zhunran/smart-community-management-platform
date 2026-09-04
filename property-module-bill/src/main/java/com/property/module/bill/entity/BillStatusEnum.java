package com.property.module.bill.entity;

import java.math.BigDecimal;

/**
 * 账单状态枚举
 *
 * <pre>
 * ┌─────────┐    逾期扫描     ┌─────────┐
 * │ UNPAID  │ ─────────────→ │ OVERDUE │
 * │  (0)    │    超过截止日    │   (5)   │
 * └────┬────┘                 └────┬────┘
 *      │                           │
 *      │ 缴费(全额)               │ 缴费(全额)
 *      ↓                           ↓
 * ┌─────────┐               ┌─────────┐
 * │  PAID   │ ←──────────── │  PAID   │
 * │  (2)    │               │  (2)    │
 * └─────────┘               └─────────┘
 *
 * 不可逆转换：PAID/VOIDED/DISCOUNTED 状态不允许再缴费。
 * OVERDUE 状态仍可缴费，缴费后跳转到 PAID。
 * </pre>
 */
public enum BillStatusEnum {

    /** 未缴费 */
    UNPAID(0, "未缴费"),
    /** 部分缴费 */
    PARTIAL(1, "部分缴费"),
    /** 已缴清 */
    PAID(2, "已缴清"),
    /** 已作废（不可缴费） */
    VOIDED(3, "已作废"),
    /** 已减免（不可缴费） */
    DISCOUNTED(4, "已减免"),
    /** 已逾期（超过缴费截止日，不可缴费……还能缴！） */
    OVERDUE(5, "已逾期");

    private final int value;
    private final String label;

    BillStatusEnum(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static BillStatusEnum fromValue(Integer value) {
        if (value == null) return null;
        for (BillStatusEnum s : values()) {
            if (s.value == value) return s;
        }
        return null;
    }

    // ========== 状态转换规则 ==========

    /**
     * 当前状态是否允许缴费操作。
     * UNPAID(0)、PARTIAL(1)、OVERDUE(5) 允许缴费。
     */
    public boolean canPay() {
        return this == UNPAID || this == PARTIAL || this == OVERDUE;
    }

    /**
     * 根据已缴金额和总金额计算缴费后的新状态。
     *
     * @param paidAmount  已缴金额（含本次）
     * @param totalAmount 账单总金额
     * @return 缴费后的新状态
     */
    public static BillStatusEnum computeAfterPayment(BigDecimal paidAmount, BigDecimal totalAmount) {
        if (paidAmount.compareTo(totalAmount) >= 0) {
            return BillStatusEnum.PAID;
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            return BillStatusEnum.PARTIAL;
        } else {
            return BillStatusEnum.UNPAID;
        }
    }

    /**
     * 当前状态是否为非激活终态（不可缴费，不再变化）。
     */
    public boolean isFinal() {
        return this == PAID || this == VOIDED || this == DISCOUNTED;
    }

    /**
     * 当前账单是否被视为逾期（需要扫描罚息）。
     */
    public boolean isOverdueState() {
        return this == UNPAID || this == PARTIAL || this == OVERDUE;
    }
}
