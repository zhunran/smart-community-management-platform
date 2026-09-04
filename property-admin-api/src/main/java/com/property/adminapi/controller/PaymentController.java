package com.property.adminapi.controller;

import com.property.common.result.ApiResult;
import com.property.framework.web.annotation.SkipAuth;
import com.property.module.payment.dto.request.PayOrderRequest;
import com.property.module.payment.dto.response.PayOrderResult;
import com.property.module.payment.service.AlipayService;
import com.property.module.payment.service.PaymentCallbackService;
import com.property.module.payment.service.PaymentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员端：支付管理
 */
@Slf4j
@Tag(name = "支付管理", description = "统一支付下单、支付宝回调、交易查询")
@RestController
@RequestMapping("/api/admin/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentOrderService paymentOrderService;
    private final PaymentCallbackService paymentCallbackService;

    @Autowired(required = false)
    private AlipayService alipayService;

    /** 支付完成后同步跳转的业主端页面地址（默认本地开发 5273，Docker 通过环境变量覆盖为 81） */
    @Value("${alipay.return-web-url:http://localhost:5273/records}")
    private String returnWebUrl;

    @Operation(summary = "支付下单", description = "统一支付下单：支付宝在线支付返回表单HTML，现金/转账直接入账")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/create")
    public ApiResult<PayOrderResult> createPayOrder(@Valid @RequestBody PayOrderRequest request,
                                                     HttpServletRequest httpRequest) {
        boolean isMobile = isMobileRequest(httpRequest);
        PayOrderResult result = paymentOrderService.createPayOrder(request, isMobile);
        return ApiResult.success(result);
    }

    @Operation(summary = "支付宝交易查询", description = "根据商户订单号查询支付宝交易状态")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/alipay/query")
    public ApiResult<Object> queryAlipayTrade(@RequestParam String billNo) {
        if (alipayService == null) {
            return ApiResult.error("支付宝服务未启用，请配置 alipay.app-id");
        }
        Object response = alipayService.queryTrade(billNo);
        return ApiResult.success(response);
    }

    @SkipAuth
    @Operation(summary = "支付宝异步通知", description = "支付宝支付成功后异步通知地址（无需鉴权），含验签+分布式锁+幂等+状态机")
    @PostMapping("/alipay/notify")
    public String alipayNotify(@Parameter(hidden = true) HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> params.put(key, values[0]));

        log.info("收到支付宝异步通知 [notifyId={}, tradeNo={}]",
                params.get("notify_id"), params.get("trade_no"));

        // 统一交给回调服务处理（验签+锁+幂等+状态机）
        return paymentCallbackService.processAlipayNotify(params);
    }

    @SkipAuth
    @Operation(summary = "支付宝同步返回", description = "用户支付完成后跳转回本系统（无需鉴权），重定向到业主端支付记录页")
    @GetMapping("/alipay/return")
    public void alipayReturn(@Parameter(hidden = true) HttpServletRequest request,
                             HttpServletResponse response) throws IOException {
        String outTradeNo = request.getParameter("out_trade_no");
        log.info("支付宝同步返回 [out_trade_no={}]", outTradeNo);
        // 重定向到业主端支付记录页（异步通知已更新订单状态）
        response.sendRedirect(returnWebUrl);
    }

    private boolean isMobileRequest(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua == null) {
            return false;
        }
        String uaLower = ua.toLowerCase();
        return uaLower.contains("mobile") || uaLower.contains("android")
                || uaLower.contains("iphone") || uaLower.contains("ipad")
                || uaLower.contains("windows phone");
    }
}
