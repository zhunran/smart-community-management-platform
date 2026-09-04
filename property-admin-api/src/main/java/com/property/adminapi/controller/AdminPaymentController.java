package com.property.adminapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.adminapi.excel.PaymentOrderExcelService;
import com.property.adminapi.excel.PaymentOrderImportListener;
import com.property.adminapi.dto.response.PaymentOrderImportVO;
import com.property.common.result.ApiResult;
import com.property.module.bill.dto.request.PaymentOrderPageQuery;
import com.property.module.bill.dto.response.PaymentOrderVO;
import com.property.module.bill.service.PaymentOrderService;
import com.property.module.payment.service.PaymentReconciliationService;
import com.property.framework.web.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 管理员端：支付记录查询与管理
 */
@Tag(name = "支付记录", description = "缴费记录查询、导入导出与管理")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentOrderService paymentOrderService;
    private final PaymentOrderExcelService paymentOrderExcelService;

    @Autowired(required = false)
    private PaymentReconciliationService reconciliationService;

    @Operation(summary = "支付记录分页查询", description = "支持账单ID、房屋、业主、支付方式、支付状态、时间范围等多维度筛选")
    @GetMapping("/page")
    public ApiResult<IPage<PaymentOrderVO>> page(PaymentOrderPageQuery query) {
        return ApiResult.success(paymentOrderService.page(query));
    }

    @Operation(summary = "支付记录详情", description = "根据支付ID查询单条支付记录详情，含房屋、业主、账单关联信息")
    @GetMapping("/{id}")
    public ApiResult<PaymentOrderVO> getDetail(@PathVariable Long id) {
        return ApiResult.success(paymentOrderService.getDetail(id));
    }

    @OperationLog(module = "缴费管理", action = "手动对账")
    @Operation(summary = "手动对账（同步支付宝状态）", description = "根据支付单号手动触发与支付宝的对账，查询支付宝真实交易状态并更新本地记录")
    @PostMapping("/{paymentNo}/sync")
    public ApiResult<String> syncPayment(@PathVariable String paymentNo) {
        if (reconciliationService == null) {
            return ApiResult.error("支付宝对账服务未启用，请配置 alipay.app-id");
        }
        String msg = reconciliationService.reconcileByPaymentNo(paymentNo);
        return ApiResult.success(msg);
    }

    @Operation(summary = "导出缴费记录 Excel", description = "支持与分页查询相同维度的筛选，流式导出全部匹配记录到 xlsx 文件")
    @GetMapping("/export")
    public void exportExcel(PaymentOrderPageQuery query,
                            @Parameter(hidden = true) HttpServletResponse response) throws IOException {
        paymentOrderExcelService.exportExcel(query, response);
    }

    @OperationLog(module = "缴费管理", action = "批量导入缴费记录")
    @Operation(summary = "导入缴费记录（批量手工入账）", description = "上传 Excel 文件，批量导入缴费记录，支持现金/转账方式。Excel列：账单编号、支付方式（现金/转账）、付款人、支付时间、备注")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<ImportResult> importPayments(@RequestParam("file") MultipartFile file) throws IOException {
        PaymentOrderImportListener listener = paymentOrderExcelService.importExcel(file.getInputStream());

        ImportResult result = new ImportResult();
        result.setSuccessCount(listener.getSuccessCount());
        result.setFailCount(listener.getFailCount());
        result.setFailList(listener.getFailList());

        return ApiResult.success("导入完成", result);
    }

    @lombok.Data
    public static class ImportResult {
        private int successCount;
        private int failCount;
        private List<PaymentOrderImportVO> failList;
    }
}
