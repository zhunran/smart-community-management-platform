package com.property.adminapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.common.result.ApiResult;
import com.property.module.bill.dto.request.FeeItemCreateRequest;
import com.property.module.bill.dto.request.FeeItemPageQuery;
import com.property.module.bill.dto.request.FeeItemUpdateRequest;
import com.property.module.bill.dto.response.FeeItemVO;
import com.property.module.bill.service.FeeItemService;
import com.property.framework.web.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员端：费用项管理
 */
@Tag(name = "费用项管理", description = "费用项的增删改查")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/admin/fee-item")
@RequiredArgsConstructor
public class AdminFeeItemController {

    private final FeeItemService feeItemService;

    @Operation(summary = "费用项分页查询")
    @GetMapping("/page")
    public ApiResult<IPage<FeeItemVO>> page(FeeItemPageQuery query) {
        return ApiResult.success(feeItemService.page(query));
    }

    @Operation(summary = "费用项详情")
    @GetMapping("/{id}")
    public ApiResult<FeeItemVO> getDetail(@PathVariable Long id) {
        return ApiResult.success(feeItemService.getDetail(id));
    }

    @Operation(summary = "全部启用费用项列表", description = "用于下拉选择框")
    @GetMapping("/list")
    public ApiResult<List<FeeItemVO>> listAll() {
        return ApiResult.success(feeItemService.listAll());
    }

    @OperationLog(module = "费用项管理", action = "新增费用项")
    @Operation(summary = "新增费用项")
    @PostMapping
    public ApiResult<Long> create(@Valid @RequestBody FeeItemCreateRequest request) {
        return ApiResult.success("新增成功", feeItemService.create(request));
    }

    @OperationLog(module = "费用项管理", action = "修改费用项")
    @Operation(summary = "修改费用项")
    @PutMapping
    public ApiResult<?> update(@Valid @RequestBody FeeItemUpdateRequest request) {
        feeItemService.update(request);
        return ApiResult.success("修改成功");
    }

    @OperationLog(module = "费用项管理", action = "删除费用项")
    @Operation(summary = "删除费用项")
    @DeleteMapping("/{id}")
    public ApiResult<?> delete(@PathVariable Long id) {
        feeItemService.delete(id);
        return ApiResult.success("删除成功");
    }
}
