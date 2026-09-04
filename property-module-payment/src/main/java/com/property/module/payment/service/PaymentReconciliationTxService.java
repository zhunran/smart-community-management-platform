package com.property.module.payment.service;

import com.property.module.bill.entity.BillEntity;
import com.property.module.bill.entity.BillStatusEnum;
import com.property.module.bill.entity.PaymentOrderEntity;
import com.property.module.bill.repository.BillMapper;
import com.property.module.bill.repository.BillPaymentMapper;
import com.property.module.payment.enums.PaymentStatusEnum;
import com.property.module.payment.util.PaymentTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付对账事务处理（独立 Service 确保 @Transactional 通过 AOP 生效）
 *
 * 包含：行级锁 → 幂等 → 状态机 → 更新支付记录 → 更新账单
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconciliationTxService {

    private final BillPaymentMapper billPaymentMapper;
    private final BillMapper billMapper;

    /**
     * 在事务内对账单笔支付记录
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean reconcileOne(PaymentOrderEntity payment,
                                AlipayService.PaymentQueryResult queryResult) {
        String paymentNo = payment.getPaymentNo();

        // 1. 行级锁 + 幂等检查
        PaymentOrderEntity locked = billPaymentMapper.selectByPaymentNoForUpdate(paymentNo);
        if (locked == null) return false;

        PaymentStatusEnum currentStatus = PaymentStatusEnum.fromValue(locked.getPaymentStatus());
        if (currentStatus == null || currentStatus.isFinal()) {
            return false;
        }

        // 2. 如果查询失败或超时 → 超过阈值则标记失败
        if (queryResult.isTimeout()) {
            long minutes = queryResult.getMinutesSinceCreation();
            if (minutes >= 30) {
                markAsFailedInTx(paymentNo, locked.getPaymentStatus(), "对账超时");
                return true;
            }
            return false;
        }

        // 3. 支付宝返回了结果
        String tradeStatus = queryResult.getTradeStatus();
        if ("WAIT_BUYER_PAY".equals(tradeStatus)) {
            return false; // 用户还在支付页面，跳过
        }

        PaymentStatusEnum targetStatus = PaymentStatusEnum.fromAlipayTradeStatus(tradeStatus);
        if (!currentStatus.canTransitionTo(targetStatus)) {
            log.warn("对账状态转换不合法 [paymentNo={}, from={}, to={}]",
                    paymentNo, currentStatus.getLabel(), targetStatus.getLabel());
            return false;
        }

        // 支付成功 → 更新支付记录 + 更新账单
        if (targetStatus == PaymentStatusEnum.SUCCESS) {
            LocalDateTime payTime = PaymentTimeUtil.parse(queryResult.getGmtPayment());
            int updated = billPaymentMapper.updatePaymentStatus(
                    paymentNo,
                    locked.getPaymentStatus(),
                    PaymentStatusEnum.SUCCESS.getValue(),
                    queryResult.getTradeNo(),
                    payTime
            );
            if (updated <= 0) return false;

            BillEntity bill = billMapper.selectById(locked.getBillId());
            if (bill == null) return false;

            BigDecimal paidSoFar = bill.getPaidAmount() != null ? bill.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal newPaidAmount = paidSoFar.add(locked.getPaymentAmount());
            bill.setPaidAmount(newPaidAmount);
            bill.setStatus(BillStatusEnum.computeAfterPayment(newPaidAmount, bill.getTotalAmount()).getValue());
            billMapper.updateById(bill);

            log.info("对账成功 [paymentNo={}, tradeNo={}, amount={}, billStatus={}]",
                    paymentNo, queryResult.getTradeNo(), locked.getPaymentAmount(),
                    BillStatusEnum.fromValue(bill.getStatus()).getLabel());
            return true;
        }

        // 非成功状态（TRADE_CLOSED 等）→ 只更新支付记录
        int updated = billPaymentMapper.updatePaymentStatus(
                paymentNo,
                locked.getPaymentStatus(),
                targetStatus.getValue(),
                queryResult.getTradeNo(),
                LocalDateTime.now()
        );
        if (updated > 0) {
            log.info("对账更新支付状态 [paymentNo={}, status={}]", paymentNo, targetStatus.getLabel());
        }
        return updated > 0;
    }

    /**
     * 超时标记失败（在事务内执行）
     */
    private void markAsFailedInTx(String paymentNo, Integer expectedStatus, String reason) {
        int updated = billPaymentMapper.updatePaymentStatus(
                paymentNo,
                expectedStatus,
                PaymentStatusEnum.FAILED.getValue(),
                reason,
                LocalDateTime.now()
        );
        if (updated > 0) {
            log.info("支付记录已标记为失败 [paymentNo={}, reason={}]", paymentNo, reason);
        }
    }
}
