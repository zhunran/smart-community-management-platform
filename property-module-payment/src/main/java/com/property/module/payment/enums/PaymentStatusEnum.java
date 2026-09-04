package com.property.module.payment.enums;

import java.util.Optional;
import java.util.Set;

/**
 * 支付状态枚举（对应 t_payment.payment_status）
 *
 * <pre>
 *                    ┌──────────────┐
 *                    │   WAITING    │
 *                    │    (0)       │
 *                    └──────┬───────┘
 *                           │
 *                    ┌──────▼───────┐
 *                    │  PROCESSING  │
 *                    │    (1)       │
 *                    └──────┬───────┘
 *                           │
 *              ┌────────────┼────────────┐
 *              ▼            ▼            ▼
 *        ┌──────────┐ ┌──────────┐ ┌──────────┐
 *        │ SUCCESS  │ │  FAILED  │ │ REFUNDED │
 *        │   (2)    │ │   (3)    │ │   (4)    │
 *        └──────────┘ └──────────┘ └──────────┘
 *              │
 *              ▼
 *        ┌──────────────┐
 *        │PARTIAL_REFUND│
 *        │    (5)       │
 *        └──────────────┘
 * </pre>
 *
 * 核心原则：SUCCESS 是终态，不可再转换。
 */
public enum PaymentStatusEnum {

    WAITING(0, "待支付"),
    PROCESSING(1, "支付中"),
    SUCCESS(2, "支付成功"),
    FAILED(3, "支付失败"),
    REFUNDED(4, "已退款"),
    PARTIAL_REFUND(5, "部分退款");

    private final int value;
    private final String label;

    /** 终态集合——到达这些状态后不再允许任何转换 */
    private static final Set<Integer> FINAL_STATES = Set.of(2, 4, 5);

    PaymentStatusEnum(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() { return value; }
    public String getLabel() { return label; }

    public static PaymentStatusEnum fromValue(Integer value) {
        if (value == null) return null;
        for (PaymentStatusEnum s : values()) {
            if (s.value == value) return s;
        }
        return null;
    }

    /** 安全版：返回 Optional，避免 NPE */
    public static Optional<PaymentStatusEnum> fromValueSafe(Integer value) {
        return Optional.ofNullable(fromValue(value));
    }

    /** 当前状态是否是终态 */
    public boolean isFinal() {
        return FINAL_STATES.contains(this.value);
    }

    /**
     * 是否允许从当前状态转换到目标状态。
     * 核心规则：终态不可转换；仅 WAITING/PROCESSING 可转为 SUCCESS/FAILED。
     */
    public boolean canTransitionTo(PaymentStatusEnum target) {
        if (this.isFinal()) return false;                     // 终态不可再转
        if (target == null) return false;
        if (this == target) return false;                     // 同状态无需处理

        // WAITING(0) 或 PROCESSING(1) → SUCCESS(2) / FAILED(3)
        if ((this == WAITING || this == PROCESSING)
                && (target == SUCCESS || target == FAILED)) {
            return true;
        }
        // SUCCESS(2) → PARTIAL_REFUND(5) / REFUNDED(4)  (退款场景)
        if (this == SUCCESS && (target == PARTIAL_REFUND || target == REFUNDED)) {
            return true;
        }
        return false;
    }

    /**
     * 判断支付宝交易状态到本地支付状态的映射
     */
    public static PaymentStatusEnum fromAlipayTradeStatus(String tradeStatus) {
        if (tradeStatus == null) return FAILED;
        return switch (tradeStatus) {
            case "TRADE_SUCCESS", "TRADE_FINISHED" -> SUCCESS;
            case "TRADE_CLOSED" -> FAILED;
            case "WAIT_BUYER_PAY" -> PROCESSING;
            default -> FAILED;
        };
    }
}
