package com.property.adminapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.common.result.ApiResult;
import com.property.module.bill.dto.request.BillGenerateRequest;
import com.property.module.bill.dto.request.BillPageQuery;
import com.property.module.bill.dto.request.ItemizedPaymentRequest;
import com.property.module.bill.dto.request.ManualPaymentRequest;
import com.property.module.bill.dto.response.BillDetailVO;
import com.property.module.bill.dto.response.BillVO;
import com.property.module.bill.service.BillService;
import com.property.framework.web.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员端：账单管理
 */
@Tag(name = "账单管理", description = "账单生成与查询")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/admin/bills")
@RequiredArgsConstructor
public class AdminBillController {

    private final BillService billService;

    @OperationLog(module = "账单管理", action = "生成账单")
    @Operation(summary = "手动触发生成账单", description = "按月生成，遍历有效房屋，计算物业费/水费/电费等")
    @PostMapping("/generate")
    public ApiResult<Integer> generate(@Valid @RequestBody BillGenerateRequest request) {
        int count = billService.generate(request);
        return ApiResult.success("账单生成完成，共生成" + count + "笔", count);
    }

    @Operation(summary = "账单分页查询", description = "多维度筛选：楼栋、业主、账期（精确/范围）、状态、是否含停车费")
    @GetMapping("/page")
    public ApiResult<IPage<BillVO>> page(BillPageQuery query) {
        return ApiResult.success(billService.page(query));
    }

    @OperationLog(module = "缴费管理", action = "整单缴费")
    @Operation(summary = "管理员手动标记缴费（整单缴清）", description = "现金(CASH=4)或转账(TRANSFER=5)手工入账，更新整单为已缴清")
    @PostMapping("/manual-payment")
    public ApiResult<Long> manualPayment(@Valid @RequestBody ManualPaymentRequest request) {
        Long paymentId = billService.manualPayment(request);
        return ApiResult.success("缴费成功", paymentId);
    }

    @OperationLog(module = "缴费管理", action = "分项缴费")
    @Operation(summary = "管理员分项缴费（支持部分缴费）", description = "指定账单中的部分费用项进行缴费，支持部分金额；未缴清的账单状态变为部分缴费")
    @PostMapping("/itemized-payment")
    public ApiResult<Long> itemizedPayment(@Valid @RequestBody ItemizedPaymentRequest request) {
        Long paymentId = billService.itemizedPayment(request);
        return ApiResult.success("分项缴费成功", paymentId);
    }

    @Operation(summary = "账单明细查看", description = "含分项费用明细列表")
    @GetMapping("/{id}")
    public ApiResult<BillDetailVO> getDetail(@PathVariable Long id) {
        return ApiResult.success(billService.getDetail(id));
    }
}
