package com.property.module.payment.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 支付时间解析工具类
 *
 * 从 PaymentCallbackTxService 和 PaymentReconciliationTxService 抽取的公共方法。
 * 解析失败时记录警告并返回当前时间作为 fallback（避免 DB NOT NULL 约束异常）。
 */
@Slf4j
public final class PaymentTimeUtil {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private PaymentTimeUtil() {
        // 工具类不可实例化
    }

    /**
     * 解析支付宝返回的支付时间字符串
     *
     * @param gmtPayment 支付宝返回的支付时间 (yyyy-MM-dd HH:mm:ss)
     * @return 解析成功返回对应时间，失败返回 {@link LocalDateTime#now()} 作为 fallback
     */
    public static LocalDateTime parse(String gmtPayment) {
        if (gmtPayment == null || gmtPayment.isBlank()) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(gmtPayment, DATE_FMT);
        } catch (Exception e) {
            log.warn("支付时间解析失败，使用当前时间作为fallback [gmtPayment={}]", gmtPayment);
            return LocalDateTime.now();
        }
    }
}
