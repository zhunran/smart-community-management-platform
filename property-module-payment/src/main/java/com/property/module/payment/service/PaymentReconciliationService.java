package com.property.module.payment.service;

import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.module.bill.entity.PaymentOrderEntity;
import com.property.module.bill.repository.BillPaymentMapper;
import com.property.module.payment.enums.PaymentStatusEnum;
import com.property.module.payment.service.AlipayService.PaymentQueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付对账服务
 *
 * 扫描待支付的订单，调用支付宝查询真实状态，自动修正本地记录。
 * 作为异步通知的补充，防止通知丢失导致订单一直卡在"待支付"。
 *
 * 对账事务委托给 {@link PaymentReconciliationTxService} 确保 @Transactional 生效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "alipay.app-id")
public class PaymentReconciliationService {

    private final BillPaymentMapper billPaymentMapper;
    private final AlipayService alipayService;
    private final PaymentReconciliationTxService reconciliationTxService;

    /**
     * 执行对账：扫描待支付订单，逐条调用支付宝查询
     *
     * @param createTimeBefore 只对在此时间之前创建的订单进行对账（避免刚创建的订单立即被查）
     * @param batchSize        每次最多处理条数
     * @return 处理成功的笔数
     */
    public int reconcile(LocalDateTime createTimeBefore, int batchSize) {
        List<PaymentOrderEntity> pendingList = billPaymentMapper.selectPendingReconciliation(createTimeBefore, batchSize);
        if (pendingList.isEmpty()) {
            log.debug("对账扫描：无待处理订单");
            return 0;
        }

        log.info("对账扫描：发现 {} 笔待处理订单", pendingList.size());
        int successCount = 0;

        for (PaymentOrderEntity payment : pendingList) {
            try {
                // 1. 调用支付宝查询（安全包装，异常不抛）
                PaymentQueryResult queryResult = alipayService.queryTradeSafe(
                        payment.getPaymentNo(), payment.getCreateTime());

                // 2. 委托事务 Service 处理（@Transactional 生效）
                boolean ok = reconciliationTxService.reconcileOne(payment, queryResult);
                if (ok) successCount++;
            } catch (Exception e) {
                log.error("对账异常 [paymentNo={}]", payment.getPaymentNo(), e);
            }
        }

        log.info("对账完成：成功 {} / {} 笔", successCount, pendingList.size());
        return successCount;
    }

    /**
     * 单笔支付记录手动对账
     *
     * @param paymentNo 支付单号
     * @return 对账结果描述
     */
    public String reconcileByPaymentNo(String paymentNo) {
        // 1. 查询支付记录
        PaymentOrderEntity payment = billPaymentMapper.selectByPaymentNo(paymentNo);
        if (payment == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "支付记录不存在");
        }

        // 2. 只对在线支付（支付宝）进行对账
        if (payment.getPaymentMethod() != 1) {
            return "非在线支付，无需对账";
        }

        // 3. 检查状态是否已是终态
        PaymentStatusEnum currentStatus = PaymentStatusEnum.fromValue(payment.getPaymentStatus());
        if (currentStatus == null || currentStatus.isFinal()) {
            return "当前支付状态已是终态，无需对账 [status=" + currentStatus.getLabel() + "]";
        }

        // 4. 调用支付宝查询交易状态（安全包装，异常不抛）
        PaymentQueryResult queryResult = alipayService.queryTradeSafe(payment.getPaymentNo(), payment.getCreateTime());

        // 5. 如果查询超时
        if (queryResult.isTimeout()) {
            return "查询支付宝交易状态超时，请稍后重试";
        }

        // 6. 委托事务 Service 处理
        boolean ok = reconciliationTxService.reconcileOne(payment, queryResult);
        if (ok) {
            String msg = "对账成功，交易状态已更新";
            if (queryResult.getTradeStatus() != null) {
                msg += " [支付宝状态=" + queryResult.getTradeStatus() + "]";
            }
            return msg;
        } else {
            return "对账未产生变更（可能状态已是最新或不需处理）";
        }
    }
}
