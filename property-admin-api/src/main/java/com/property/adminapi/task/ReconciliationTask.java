package com.property.adminapi.task;

import com.property.module.payment.service.PaymentReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 支付对账定时任务
 *
 * 定时扫描待支付的订单，调用支付宝查询真实状态，自动修正本地记录。
 * 作为支付宝异步通知的补充，防止通知丢失导致订单卡在"待支付"状态。
 *
 * 阈值说明：
 * - PENDING_MINUTES=30：只对创建超过 30 分钟的订单进行对账
 *   （支付宝异步通知通常在几秒内到达，超过 30 分钟未收到通知说明异常）
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "alipay.app-id")
public class ReconciliationTask {

    /** 只对创建超过 30 分钟的订单进行对账，避免刚创建的订单立即被查 */
    private static final int PENDING_MINUTES = 30;

    /** 每次最多处理 50 笔 */
    private static final int BATCH_SIZE = 50;

    private final PaymentReconciliationService reconciliationService;

    /**
     * 每 2 分钟执行一次对账
     */
    @Scheduled(fixedRate = 120_000)
    public void reconcilePendingPayments() {
        LocalDateTime createTimeBefore = LocalDateTime.now().minusMinutes(PENDING_MINUTES);
        log.info("定时对账开始 [createTimeBefore={}, batchSize={}]", createTimeBefore, BATCH_SIZE);

        try {
            int count = reconciliationService.reconcile(createTimeBefore, BATCH_SIZE);
            if (count > 0) {
                log.info("定时对账完成，修正 {} 笔", count);
            }
        } catch (Exception e) {
            log.error("定时对账异常", e);
        }
    }
}
