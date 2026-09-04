package com.property.adminapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.common.result.ApiResult;
import com.property.module.parking.dto.request.WarningHandleRequest;
import com.property.module.parking.dto.response.ParkingWarningVO;
import com.property.module.parking.service.ParkingReconciliationService;
import com.property.framework.web.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员端：车位对账与预警管理
 */
@Tag(name = "车位预警", description = "双轨对账执行、预警查看/处理/关闭")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/admin/parking/reconciliation")
@RequiredArgsConstructor
public class AdminParkingReconciliationController {

    private final ParkingReconciliationService reconciliationService;

    @OperationLog(module = "停车对账", action = "执行双轨对账")
    @Operation(summary = "执行双轨对账", description = "扫描车位状态、租赁合同、使用记录，自动生成预警（每次生成新一批预警）")
    @PostMapping("/run")
    public ApiResult<String> run() {
        int count = reconciliationService.reconcile();
        return ApiResult.success("对账完成，生成 " + count + " 条预警");
    }

    @Operation(summary = "预警分页查询", description = "按预警类型和状态筛选")
    @GetMapping("/page")
    public ApiResult<IPage<ParkingWarningVO>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String warningType,
            @RequestParam(required = false) Integer status) {
        return ApiResult.success(reconciliationService.page(current, size, warningType, status));
    }

    @OperationLog(module = "停车对账", action = "处理预警")
    @Operation(summary = "处理预警", description = "标记预警为已处理，需填写处理备注")
    @PostMapping("/{id}/handle")
    public ApiResult<String> handle(@PathVariable Long id, @Valid @RequestBody WarningHandleRequest request) {
        reconciliationService.handle(id, request);
        return ApiResult.success("预警已处理");
    }

    @OperationLog(module = "停车对账", action = "关闭预警")
    @Operation(summary = "关闭预警", description = "直接关闭预警（无需处理备注）")
    @PostMapping("/{id}/close")
    public ApiResult<String> close(@PathVariable Long id, @RequestParam(required = false) String remark) {
        reconciliationService.close(id, remark);
        return ApiResult.success("预警已关闭");
    }
}
