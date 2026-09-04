package com.property.adminapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.common.result.ApiResult;
import com.property.module.bill.dto.request.FeeStandardCreateRequest;
import com.property.module.bill.dto.request.FeeStandardUpdateRequest;
import com.property.module.bill.dto.response.FeeStandardVO;
import com.property.module.bill.service.FeeStandardService;
import com.property.framework.web.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员端：费用标准管理（含季节性调价）
 *
 * 费用标准支持按时间范围生效，可用于季节性调价：
 * - 夏季空调附加费：设置 startDate=2026-06-01, endDate=2026-08-31
 * - 冬季取暖费：设置 startDate=2026-11-01, endDate=2027-03-31
 * 账单生成时自动匹配当前日期生效的标准。
 */
@Tag(name = "费用标准管理", description = "费用标准的增删改查，支持季节性调价")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/admin/fee-standard")
@RequiredArgsConstructor
public class AdminFeeStandardController {

    private final FeeStandardService feeStandardService;

    @Operation(summary = "费用标准分页查询", description = "支持按费用项、房屋、状态筛选")
    @GetMapping("/page")
    public ApiResult<IPage<FeeStandardVO>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long feeItemId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Integer status) {
        return ApiResult.success(feeStandardService.page(current, size, feeItemId, roomId, status));
    }

    @Operation(summary = "按费用项查询生效标准", description = "用于查看某费用项的所有生效标准")
    @GetMapping("/list-by-fee-item")
    public ApiResult<?> listByFeeItemId(@RequestParam Long feeItemId) {
        return ApiResult.success(feeStandardService.listByFeeItemId(feeItemId));
    }

    @OperationLog(module = "费用标准管理", action = "新增费用标准")
    @Operation(summary = "新增费用标准", description = "支持按时间范围生效，适用于季节性调价")
    @PostMapping
    public ApiResult<Long> create(@Valid @RequestBody FeeStandardCreateRequest request) {
        return ApiResult.success("新增成功", feeStandardService.create(request));
    }

    @OperationLog(module = "费用标准管理", action = "修改费用标准")
    @Operation(summary = "修改费用标准", description = "可调整单价、生效时间范围、状态等")
    @PutMapping
    public ApiResult<?> update(@Valid @RequestBody FeeStandardUpdateRequest request) {
        feeStandardService.update(request);
        return ApiResult.success("修改成功");
    }

    @OperationLog(module = "费用标准管理", action = "删除费用标准")
    @Operation(summary = "删除费用标准")
    @DeleteMapping("/{id}")
    public ApiResult<?> delete(@PathVariable Long id) {
        feeStandardService.delete(id);
        return ApiResult.success("删除成功");
    }
}
