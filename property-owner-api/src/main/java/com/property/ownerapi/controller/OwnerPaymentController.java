package com.property.ownerapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.common.exception.AuthException;
import com.property.common.result.ApiResult;
import com.property.framework.web.security.SecurityUtil;
import com.property.module.bill.dto.request.PaymentOrderPageQuery;
import com.property.module.bill.dto.response.PaymentOrderVO;
import com.property.module.bill.service.PaymentOrderService;
import com.property.module.payment.dto.request.PayOrderRequest;
import com.property.module.payment.dto.response.PayOrderResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 业主端：支付
 */
@Tag(name = "业主支付", description = "业主发起支付、支付记录查询")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/owner/payment")
@RequiredArgsConstructor
public class OwnerPaymentController {

    private final com.property.module.payment.service.PaymentOrderService paymentCreateService;
    private final PaymentOrderService paymentOrderService;

    @Operation(summary = "支付下单", description = "业主选择账单发起支付宝在线支付，返回支付表单HTML（前端直接渲染后自动跳转）")
    @PostMapping("/create")
    public ApiResult<PayOrderResult> createPayOrder(@Valid @RequestBody PayOrderRequest request,
                                                     HttpServletRequest httpRequest) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        boolean isMobile = isMobileRequest(httpRequest);
        PayOrderResult result = paymentCreateService.createPayOrder(request, isMobile, ownerId);
        return ApiResult.success("支付订单创建成功", result);
    }

    @Operation(summary = "支付记录（分页）", description = "当前业主的支付记录列表，支持按状态、时间范围筛选")
    @GetMapping("/page")
    public ApiResult<IPage<PaymentOrderVO>> page(PaymentOrderPageQuery query) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        query.setOwnerId(ownerId);
        return ApiResult.success(paymentOrderService.page(query));
    }

    @Operation(summary = "支付记录详情", description = "根据支付ID查询单笔支付记录详情")
    @GetMapping("/{id}")
    public ApiResult<PaymentOrderVO> getDetail(@PathVariable Long id) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        PaymentOrderVO vo = paymentOrderService.getDetail(id);

        // 校验归属
        if (vo.getOwnerId() == null || !vo.getOwnerId().equals(ownerId)) {
            throw new AuthException("无权查看该支付记录");
        }

        return ApiResult.success(vo);
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
