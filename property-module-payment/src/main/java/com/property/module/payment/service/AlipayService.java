package com.property.module.payment.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.module.payment.config.AlipayConfig;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 支付宝支付服务
 *
 * 提供支付宝沙箱环境下的支付能力：
 * - 电脑网站支付（AlipayTradePagePay）
 * - 支付异步通知验签
 * - 交易查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "alipay.app-id")
public class AlipayService {

    private final AlipayClient alipayClient;
    private final AlipayConfig alipayConfig;

    // ==================== 支付 ====================

    /**
     * 创建电脑网站支付（跳转到支付宝收银台）
     *
     * @param billId      账单ID
     * @param billNo      账单编号（商户订单号，需唯一）
     * @param totalAmount 支付金额
     * @param subject     商品标题（如 "物业费 2024-01"）
     * @return 支付宝返回的表单 HTML（前端直接渲染即可跳转）
     */
    public String createPagePay(Long billId, String billNo, BigDecimal totalAmount, String subject) {
        // 构建请求
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(alipayConfig.getNotifyUrl());
        request.setReturnUrl(alipayConfig.getReturnWebUrl());

        // 构建业务参数
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(billNo);
        model.setTotalAmount(totalAmount.toString());
        model.setSubject(subject);
        model.setProductCode("FAST_INSTANT_TRADE_PAY");

        request.setBizModel(model);

        try {
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            log.info("支付宝下单成功 [billNo={}, amount={}, tradeNo={}]", billNo, totalAmount, response.getTradeNo());
            return response.getBody();
        } catch (AlipayApiException e) {
            log.error("支付宝下单失败 [billNo={}, amount={}]: code={}, msg={}",
                    billNo, totalAmount, e.getErrCode(), e.getErrMsg());
            throw new BusinessException(ErrorCode.OPERATION_FAILED,
                    "支付宝下单失败：" + e.getErrMsg());
        }
    }

    /**
     * 创建手机网站支付（移动端 H5，跳转支付宝 App 或 H5 收银台）
     *
     * @param billId      账单ID
     * @param billNo      账单编号（商户订单号，需唯一）
     * @param totalAmount 支付金额
     * @param subject     商品标题（如 "物业费 2024-01"）
     * @return 支付宝返回的表单 HTML（前端直接渲染即可跳转）
     */
    public String createWapPay(Long billId, String billNo, BigDecimal totalAmount, String subject) {
        // 构建请求
        AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
        request.setNotifyUrl(alipayConfig.getNotifyUrl());
        request.setReturnUrl(alipayConfig.getReturnWebUrl());

        // 构建业务参数
        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setOutTradeNo(billNo);
        model.setTotalAmount(totalAmount.toString());
        model.setSubject(subject);
        model.setProductCode("QUICK_WAP_WAY");

        request.setBizModel(model);

        try {
            AlipayTradeWapPayResponse response = alipayClient.pageExecute(request);
            log.info("支付宝手机网站支付下单成功 [billNo={}, amount={}]", billNo, totalAmount);
            return response.getBody();
        } catch (AlipayApiException e) {
            log.error("支付宝手机网站支付下单失败 [billNo={}, amount={}]: code={}, msg={}",
                    billNo, totalAmount, e.getErrCode(), e.getErrMsg());
            throw new BusinessException(ErrorCode.OPERATION_FAILED,
                    "支付宝下单失败：" + e.getErrMsg());
        }
    }

    // ==================== 查询 ====================

    /**
     * 查询支付宝交易状态
     *
     * @param billNo 商户订单号（与创建时一致）
     * @return 支付宝交易查询响应
     */
    public AlipayTradeQueryResponse queryTrade(String billNo) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(billNo);
        request.setBizModel(model);

        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            log.info("支付宝交易查询 [billNo={}, tradeStatus={}, tradeNo={}]",
                    billNo, response.getTradeStatus(), response.getTradeNo());
            return response;
        } catch (AlipayApiException e) {
            log.error("支付宝交易查询失败 [billNo={}]: {}", billNo, e.getErrMsg());
            throw new BusinessException(ErrorCode.OPERATION_FAILED,
                    "支付宝交易查询失败：" + e.getErrMsg());
        }
    }

    // ==================== 查询（带超时安全包装） ====================

    /**
     * 查询支付宝交易状态（返回包装结果，异常不抛）
     *
     * @param billNo     商户订单号
     * @param createTime 支付记录创建时间（用于计算已等待分钟数）
     * @return PaymentQueryResult 包装结果
     */
    public PaymentQueryResult queryTradeSafe(String billNo, LocalDateTime createTime) {
        long minutesSinceCreation = createTime != null
                ? Duration.between(createTime, LocalDateTime.now()).toMinutes()
                : 0;

        try {
            AlipayTradeQueryResponse resp = queryTrade(billNo);
            return new PaymentQueryResult(
                    false,                     // 不是超时
                    minutesSinceCreation,
                    resp.getTradeStatus(),
                    resp.getTradeNo(),
                    resp.getSendPayDate() != null ? resp.getSendPayDate().toString() : null,
                    resp.isSuccess()
            );
        } catch (Exception e) {
            return new PaymentQueryResult(
                    true,                      // 超时/异常
                    minutesSinceCreation,
                    null, null, null, false
            );
        }
    }

    /**
     * 支付宝交易查询结果包装
     */
    @Data
    @AllArgsConstructor
    public static class PaymentQueryResult {
        /** 是否查询超时或异常 */
        private boolean timeout;
        /** 支付记录创建至今的分钟数 */
        private long minutesSinceCreation;
        /** 交易状态（TRADE_SUCCESS/TRADE_CLOSED/WAIT_BUYER_PAY） */
        private String tradeStatus;
        /** 支付宝交易号 */
        private String tradeNo;
        /** 交易付款时间 */
        private String gmtPayment;
        /** 查询是否成功返回 */
        private boolean querySuccess;
    }

    // ==================== 通知验签 ====================

    /**
     * 验证支付宝异步通知签名
     *
     * @param params 支付宝 POST 请求参数 Map
     * @return true-验签通过 false-验签失败
     */
    public boolean verifyNotify(Map<String, String> params) {
        try {
            // SDK 提供验签工具
            boolean verified = com.alipay.api.internal.util.AlipaySignature.rsaCheckV1(
                    params, alipayConfig.getAlipayPublicKey(), "UTF-8", "RSA2");
            if (!verified) {
                log.warn("支付宝异步通知验签失败");
            }
            return verified;
        } catch (AlipayApiException e) {
            log.error("支付宝异步通知验签异常", e);
            return false;
        }
    }

    /**
     * 从支付宝异步通知参数中提取支付成功的核心信息
     *
     * @param params 支付宝 POST 请求参数
     * @return PaymentNotifyInfo 记录
     */
    public PaymentNotifyInfo parseNotifyParams(Map<String, String> params) {
        PaymentNotifyInfo info = new PaymentNotifyInfo();
        info.setBillNo(params.get("out_trade_no"));
        info.setTradeNo(params.get("trade_no"));
        info.setTradeStatus(params.get("trade_status"));
        info.setTotalAmount(params.get("total_amount"));
        info.setBuyerId(params.get("buyer_id"));
        info.setBuyerLogonId(params.get("buyer_logon_id"));
        info.setGmtPayment(params.get("gmt_payment"));
        info.setNotifyId(params.get("notify_id"));
        return info;
    }

    // ==================== 内部 VO ====================

    /**
     * 支付宝异步通知参数 VO
     */
    @lombok.Data
    public static class PaymentNotifyInfo {
        /** 商户订单号（billNo） */
        private String billNo;
        /** 支付宝交易号 */
        private String tradeNo;
        /** 交易状态（TRADE_SUCCESS / TRADE_FINISHED） */
        private String tradeStatus;
        /** 订单金额 */
        private String totalAmount;
        /** 买家支付宝用户ID */
        private String buyerId;
        /** 买家支付宝账号 */
        private String buyerLogonId;
        /** 交易付款时间 */
        private String gmtPayment;
        /** 通知ID */
        private String notifyId;
    }
}
