package com.property.module.payment.service;

import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.module.bill.entity.BillEntity;
import com.property.module.bill.entity.BillStatusEnum;
import com.property.module.bill.entity.PaymentOrderEntity;
import com.property.module.bill.repository.BillMapper;
import com.property.module.bill.repository.BillPaymentMapper;
import com.property.module.bill.service.BillItemAllocator;
import com.property.module.payment.enums.PaymentStatusEnum;
import com.property.module.payment.util.PaymentTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 支付回调事务处理（独立 Service 确保 @Transactional 生效）
 *
 * 包含：行级锁 → 幂等检查 → 状态机 → 更新支付记录 → 更新账单
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCallbackTxService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BillPaymentMapper billPaymentMapper;
    private final BillMapper billMapper;
    private final BillItemAllocator billItemAllocator;

    /**
     * 在事务内执行支付回调处理
     * <p>
     * 利用 {@code @Transactional} + {@code FOR UPDATE} 实现行级锁，
     * 配合乐观锁 {@code WHERE payment_status = ?} 二次防重。
     *
     * @param info            支付宝通知解析信息
     * @return "success" 或 "failure"
     */
    @Transactional(rollbackFor = Exception.class)
    public String process(AlipayService.PaymentNotifyInfo info) {
        String paymentNo = info.getBillNo();

        // ========== 1. 行级锁 + 幂等检查 ==========
        PaymentOrderEntity payment = billPaymentMapper.selectByPaymentNoForUpdate(paymentNo);
        if (payment == null) {
            log.warn("支付记录不存在 [paymentNo={}]", paymentNo);
            return "failure";
        }

        PaymentStatusEnum currentStatus = PaymentStatusEnum.fromValue(payment.getPaymentStatus());
        if (currentStatus == null) {
            log.warn("支付状态未知 [paymentNo={}, status={}]", paymentNo, payment.getPaymentStatus());
            return "failure";
        }

        // 幂等：已是终态则直接返回成功（支付宝会重试同一条通知）
        if (currentStatus.isFinal()) {
            log.info("支付已是终态，跳过重复处理 [paymentNo={}, status={}]",
                    paymentNo, currentStatus.getLabel());
            return "success";
        }

        // ========== 2. 状态机校验 ==========
        PaymentStatusEnum targetStatus = PaymentStatusEnum.fromAlipayTradeStatus(info.getTradeStatus());
        if (!currentStatus.canTransitionTo(targetStatus)) {
            log.warn("支付状态转换不合法 [paymentNo={}, from={}, to={}]",
                    paymentNo, currentStatus.getLabel(), targetStatus.getLabel());
            return "failure";
        }

        // 非 SUCCESS 状态（如 TRADE_CLOSED）只更新支付记录状态
        if (targetStatus != PaymentStatusEnum.SUCCESS) {
            markPaymentNonSuccess(paymentNo, payment.getPaymentStatus(), targetStatus.getValue(), info);
            return "success";
        }

        // ========== 3. 更新支付记录（乐观锁二次防重）==========
        LocalDateTime payTime = PaymentTimeUtil.parse(info.getGmtPayment());
        int updated = billPaymentMapper.updatePaymentStatus(
                paymentNo,
                payment.getPaymentStatus(),   // 期望的原状态
                PaymentStatusEnum.SUCCESS.getValue(),
                info.getTradeNo(),
                payTime
        );
        if (updated <= 0) {
            // 乐观锁失败：状态已被其他线程修改（极端并发场景）
            log.warn("支付记录乐观锁更新失败 [paymentNo={}, expectedStatus={}]",
                    paymentNo, payment.getPaymentStatus());
            return "failure";
        }

        // ========== 4. 更新账单 ==========
        BillEntity bill = billMapper.selectById(payment.getBillId());
        if (bill == null) {
            log.error("账单不存在 [billId={}]", payment.getBillId());
            return "failure";
        }

        BigDecimal paidSoFar = bill.getPaidAmount() != null ? bill.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal newPaidAmount = paidSoFar.add(payment.getPaymentAmount());
        bill.setPaidAmount(newPaidAmount);
        bill.setStatus(BillStatusEnum.computeAfterPayment(newPaidAmount, bill.getTotalAmount()).getValue());
        billMapper.updateById(bill);

        // 同步分摊到账单明细，保证费用项维度实收统计准确
        billItemAllocator.allocate(bill.getId(), payment.getPaymentAmount());

        log.info("支付宝回调处理成功 [paymentNo={}, tradeNo={}, amount={}, billStatus={}]",
                paymentNo, info.getTradeNo(), info.getTotalAmount(), BillStatusEnum.fromValue(bill.getStatus()).getLabel());
        return "success";
    }

    private void markPaymentNonSuccess(String paymentNo, Integer expectedStatus,
                                          Integer targetStatus, AlipayService.PaymentNotifyInfo info) {
        int updated = billPaymentMapper.updatePaymentStatus(
                paymentNo, expectedStatus, targetStatus, info.getTradeNo(), LocalDateTime.now());
        if (updated > 0) {
            log.info("支付记录状态已更新 [paymentNo={}, status={}]",
                    paymentNo, PaymentStatusEnum.fromValue(targetStatus).getLabel());
        }
    }
}
