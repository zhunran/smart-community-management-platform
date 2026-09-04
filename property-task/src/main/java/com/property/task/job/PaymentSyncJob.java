package com.property.task.job;

import com.property.module.payment.service.PaymentReconciliationService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 支付对账定时任务
 *
 * 每 2 小时执行一次，扫描待支付的订单，调用支付宝查询真实状态，自动修正本地记录。
 * 作为支付宝异步通知的补充，防止通知丢失导致订单卡在"待支付"状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(PaymentReconciliationService.class)
public class PaymentSyncJob {

    /** 只对创建超过 30 分钟的订单进行对账 */
    private static final int PENDING_MINUTES = 30;

    /** 每次最多处理 50 笔 */
    private static final int BATCH_SIZE = 50;

    private final PaymentReconciliationService reconciliationService;

    /**
     * 支付对账任务
     */
    @XxlJob("paymentReconcileJob")
    public void reconcile() {
        XxlJobHelper.log("支付对账任务开始");

        LocalDateTime createTimeBefore = LocalDateTime.now().minusMinutes(PENDING_MINUTES);

        try {
            int count = reconciliationService.reconcile(createTimeBefore, BATCH_SIZE);
            String msg = "支付对账完成，修正 " + count + " 笔";
            XxlJobHelper.log(msg);
            log.info(msg);
        } catch (Exception e) {
            XxlJobHelper.log("支付对账异常: " + e.getMessage());
            log.error("支付对账异常", e);
            throw new RuntimeException("支付对账任务失败", e);
        }
    }
}
