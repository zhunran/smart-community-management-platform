package com.property.module.payment.service;

import com.property.framework.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * 支付回调处理服务入口
 * 流程：验签 → Redis 分布式锁 → 委托 {@link PaymentCallbackTxService} 事务处理
 *
 * <pre>
 *   ┌──────────────────────┐
 *   │  验签 (verifyNotify)  │
 *   └────────┬─────────────┘
 *            │
 *   ┌────────▼─────────────┐
 *   │ 分布式锁 (Redis SETNX)│  ← Redis 分布式锁，防止并发
 *   └────────┬─────────────┘
 *            │
 *   ┌────────▼─────────────┐
 *   │ @Transactional 事务   │  ← PaymentCallbackTxService
 *   │  ├ FOR UPDATE 行级锁  │
 *   │  ├ 幂等检查           │
 *   │  ├ 状态机             │
 *   │  ├ 更新支付记录       │
 *   │  └ 更新账单           │
 *   └────────┬─────────────┘
 *            │
 *   ┌────────▼─────────────┐
 *   │ 释放锁 (Lua unlock)   │
 *   └──────────────────────┘
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(AlipayService.class)
public class PaymentCallbackService {

    private final AlipayService alipayService;
    private final PaymentCallbackTxService paymentCallbackTxService;
    private final RedisUtil redisUtil;

    /** 分布式锁过期时间：30 秒（足够处理回调事务） */
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(30);

    /**
     * 处理支付宝异步通知
     *
     * @param params 支付宝 POST 请求参数
     * @return "success" 或 "failure"
     */
    public String processAlipayNotify(Map<String, String> params) {
        // ========== 1. 验签 ==========
        if (!alipayService.verifyNotify(params)) {
            log.warn("支付宝回调验签失败");
            return "failure";
        }

        AlipayService.PaymentNotifyInfo info = alipayService.parseNotifyParams(params);
        String paymentNo = info.getBillNo();
        if (paymentNo == null || paymentNo.isBlank()) {
            log.warn("支付宝回调缺少商户订单号(out_trade_no)");
            return "failure";
        }

        // 只处理支付成功的通知，其余状态直接返回 success
        if (!"TRADE_SUCCESS".equals(info.getTradeStatus())
                && !"TRADE_FINISHED".equals(info.getTradeStatus())) {
            log.info("支付宝通知非成功状态，跳过 [tradeStatus={}]", info.getTradeStatus());
            return "success";
        }

        // ========== 2. Redis 分布式锁 ==========
        String lockKey = "lock:payment:callback:" + paymentNo;
        String requestId = UUID.randomUUID().toString();

        boolean locked = redisUtil.tryLock(lockKey, requestId, LOCK_TIMEOUT);
        if (!locked) {
            log.warn("获取分布式锁失败 [lockKey={}]", lockKey);
            return "failure";
        }

        try {
            // ========== 3. 委托事务处理 ==========
            return paymentCallbackTxService.process(info);
        } catch (Exception e) {
            log.error("处理支付宝回调异常 [paymentNo={}]", paymentNo, e);
            return "failure";
        } finally {
            // ========== 4. 释放锁 ==========
            boolean released = redisUtil.unlock(lockKey, requestId);
            if (!released) {
                log.warn("释放分布式锁失败（锁可能已过期或被他人持有）[lockKey={}]", lockKey);
            }
        }
    }
}
