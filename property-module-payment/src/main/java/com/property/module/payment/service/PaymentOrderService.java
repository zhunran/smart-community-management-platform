package com.property.module.payment.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.framework.web.security.SecurityUtil;
import com.property.module.bill.entity.BillEntity;
import com.property.module.bill.entity.BillStatusEnum;
import com.property.module.bill.entity.PaymentOrderEntity;
import com.property.module.bill.repository.BillMapper;
import com.property.module.bill.repository.BillPaymentMapper;
import com.property.module.bill.service.BillItemAllocator;
import com.property.module.payment.dto.request.PayOrderRequest;
import com.property.module.payment.dto.response.PayOrderResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 支付下单服务
 *
 * 统一处理各类支付方式的下单逻辑：
 * 1. 创建支付记录（t_payment）
 * 2. 在线支付（支付宝）调用第三方 SDK 获取支付表单
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(AlipayService.class)
public class PaymentOrderService {

    private static final String[] PAYMENT_METHOD_NAMES = {"", "支付宝", "微信", "银行卡", "现金", "转账", "其他"};

    private final BillMapper billMapper;
    private final BillPaymentMapper billPaymentMapper;
    private final AlipayService alipayService;
    private final BillItemAllocator billItemAllocator;

    /**
     * 统一支付下单
     *
     * @param request 下单请求
     * @return 下单结果（在线支付返回表单 HTML，线下支付直接返回成功）
     */
    @Transactional(rollbackFor = Exception.class)
    public PayOrderResult createPayOrder(PayOrderRequest request, boolean isMobile) {
        return createPayOrder(request, isMobile, null);
    }

    /**
     * 统一支付下单（含业主归属校验）
     *
     * @param request 下单请求
     * @param isMobile 是否移动端
     * @param ownerId 业主ID（非 null 时校验账单归属）
     * @return 下单结果
     */
    @Transactional(rollbackFor = Exception.class)
    public PayOrderResult createPayOrder(PayOrderRequest request, boolean isMobile, Long ownerId) {
        // 1. 校验支付方式
        Integer method = request.getPaymentMethod();
        if (method == null || (method != 1 && method != 4 && method != 5)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "支付方式仅支持：1-支付宝(ALIPAY)、4-现金(CASH)、5-转账(TRANSFER)");
        }

        // 2. 查询账单（线下支付需加行锁防并发超收）
        BillEntity bill;
        if (method != 1) {
            bill = billMapper.selectByIdForUpdate(request.getBillId());
        } else {
            bill = billMapper.selectById(request.getBillId());
        }
        if (bill == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "账单不存在");
        }

        // 2.5 校验账单归属（业主端防越权）
        if (ownerId != null && (bill.getOwnerId() == null || !bill.getOwnerId().equals(ownerId))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该账单");
        }

        BillStatusEnum currentStatus = BillStatusEnum.fromValue(bill.getStatus());
        if (currentStatus == null || !currentStatus.canPay()) {
            throw new BusinessException(ErrorCode.STATUS_ERROR,
                    "当前账单状态不允许缴费 [status=" + bill.getStatus() + "]");
        }

        // 3. 计算待缴金额
        BigDecimal paidSoFar = bill.getPaidAmount() != null ? bill.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal amount = bill.getTotalAmount().subtract(paidSoFar);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "账单无需缴费");
        }

        // 3.5 防重复下单：在线支付时，同一账单已有待支付订单则复用该订单（重新生成支付宝支付表单）
        if (method == 1) {
            PaymentOrderEntity pendingOrder = billPaymentMapper.selectPendingByBillId(bill.getId());
            if (pendingOrder != null) {
                PayOrderResult reuseResult = new PayOrderResult();
                reuseResult.setPaymentId(pendingOrder.getId());
                reuseResult.setPaymentNo(pendingOrder.getPaymentNo());
                reuseResult.setPaymentMethod(method);
                reuseResult.setPaymentMethodName(getPaymentMethodName(method));
                reuseResult.setOnlinePay(true);
                String subject = "物业费 " + bill.getBillPeriod();
                String formHtml = createAlipayForm(
                        bill.getId(), pendingOrder.getPaymentNo(), pendingOrder.getPaymentAmount(), subject, isMobile);
                reuseResult.setPayFormHtml(formHtml);
                log.info("复用待支付订单重新发起支付 [billId={}, paymentNo={}]",
                        bill.getId(), pendingOrder.getPaymentNo());
                return reuseResult;
            }
        }

        // 4. 生成支付单号
        LocalDateTime now = LocalDateTime.now();
        String paymentNo = generatePaymentNo();
        String payerName = request.getPayerName();
        if (payerName == null || payerName.isBlank()) {
            payerName = "业主ID:" + bill.getOwnerId();
        }

        // 5. 创建支付记录
        // 在线支付（支付宝）→ 待支付（0）；线下支付（现金/转账）→ 支付成功（2）
        Integer paymentStatus = (method == 1) ? 0 : 2;

        Long paymentId = IdWorker.getId();
        String operator = SecurityUtil.getUsername();
        int inserted = billPaymentMapper.insertPayment(
                paymentId,
                paymentNo,
                bill.getId(),
                bill.getRoomId(),
                bill.getOwnerId(),
                method,
                amount,
                now,
                paymentStatus,
                payerName,
                request.getRemark(),
                operator != null ? operator : "system"
        );
        if (inserted <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "创建支付记录失败");
        }

        // 线下支付直接更新账单状态
        if (method != 1) {
            BigDecimal newPaidAmount = paidSoFar.add(amount);
            bill.setPaidAmount(newPaidAmount);
            bill.setStatus(BillStatusEnum.computeAfterPayment(newPaidAmount, bill.getTotalAmount()).getValue());
            billMapper.updateById(bill);
            // 同步分摊到账单明细
            billItemAllocator.allocate(bill.getId(), amount);
            log.info("线下支付成功 [billId={}, paymentNo={}, method={}, amount={}]",
                    bill.getId(), paymentNo, method, amount);
        } else {
            log.info("在线支付已创建 [billId={}, paymentNo={}, method={}, amount={}]",
                    bill.getId(), paymentNo, method, amount);
        }

        // 6. 构建返回结果
        PayOrderResult result = new PayOrderResult();
        result.setPaymentId(paymentId);
        result.setPaymentNo(paymentNo);
        result.setPaymentMethod(method);
        result.setPaymentMethodName(getPaymentMethodName(method));
        result.setOnlinePay(method == 1);

        // 支付宝在线支付：调用 SDK 获取表单 HTML
        if (method == 1) {
            String subject = "物业费 " + bill.getBillPeriod();
            String formHtml = createAlipayForm(bill.getId(), paymentNo, amount, subject, isMobile);
            result.setPayFormHtml(formHtml);
        }

        return result;
    }

    /**
     * 根据设备类型选择支付宝支付产品：移动端走手机网站支付（wap），PC 走电脑网站支付（page）
     */
    private String createAlipayForm(Long billId, String paymentNo, BigDecimal amount, String subject, boolean isMobile) {
        if (isMobile) {
            return alipayService.createWapPay(billId, paymentNo, amount, subject);
        }
        return alipayService.createPagePay(billId, paymentNo, amount, subject);
    }

    /**
     * 生成支付单号：PAY + yyyyMMdd + 4位流水
     */
    private String generatePaymentNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PAY" + datePart;
        String lastNo = billPaymentMapper.selectLastPaymentNo(prefix + "%");
        int seq = 1;
        if (lastNo != null) {
            String lastSeq = lastNo.substring(lastNo.length() - 4);
            try {
                seq = Integer.parseInt(lastSeq) + 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return prefix + String.format("%04d", seq);
    }

    private String getPaymentMethodName(Integer method) {
        if (method == null || method < 0 || method >= PAYMENT_METHOD_NAMES.length) return "";
        return PAYMENT_METHOD_NAMES[method];
    }
}
