package com.property.adminapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.common.result.ApiResult;
import com.property.module.owner.dto.request.OwnerCreateRequest;
import com.property.module.owner.dto.request.OwnerPageQuery;
import com.property.module.owner.dto.request.OwnerUpdateRequest;
import com.property.module.owner.dto.response.OwnerDetailVO;
import com.property.module.owner.dto.response.OwnerVO;
import com.property.framework.web.annotation.OperationLog;
import com.property.module.owner.service.OwnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员端：业主管理
 */
@Tag(name = "业主管理", description = "业主的增删改查")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/admin/owner")
@RequiredArgsConstructor
public class AdminOwnerController {

    private final OwnerService ownerService;

    @Operation(summary = "业主分页查询", description = "支持姓名、手机号、证件号、性别、业主类型、状态等过滤")
    @GetMapping("/page")
    public ApiResult<IPage<OwnerVO>> page(OwnerPageQuery query) {
        return ApiResult.success(ownerService.page(query));
    }

    @Operation(summary = "业主详情")
    @GetMapping("/{id}")
    public ApiResult<OwnerDetailVO> getDetail(@PathVariable Long id) {
        return ApiResult.success(ownerService.getDetail(id));
    }

    @OperationLog(module = "业主管理", action = "新增业主")
    @Operation(summary = "新增业主")
    @PostMapping
    public ApiResult<Long> create(@Valid @RequestBody OwnerCreateRequest request) {
        return ApiResult.success("新增成功", ownerService.create(request));
    }

    @OperationLog(module = "业主管理", action = "修改业主")
    @Operation(summary = "修改业主")
    @PutMapping
    public ApiResult<?> update(@Valid @RequestBody OwnerUpdateRequest request) {
        ownerService.update(request);
        return ApiResult.success("修改成功");
    }

    @OperationLog(module = "业主管理", action = "删除业主")
    @Operation(summary = "删除业主", description = "逻辑删除")
    @DeleteMapping("/{id}")
    public ApiResult<?> delete(@PathVariable Long id) {
        ownerService.delete(id);
        return ApiResult.success("删除成功");
    }
}
