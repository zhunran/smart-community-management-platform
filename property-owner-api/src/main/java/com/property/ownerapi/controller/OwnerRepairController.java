package com.property.ownerapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.common.result.ApiResult;
import com.property.framework.web.security.SecurityUtil;
import com.property.module.lifeservice.dto.repair.request.RepairOrderCreateRequest;
import com.property.module.lifeservice.dto.repair.request.RepairOrderQuery;
import com.property.module.lifeservice.dto.repair.request.RepairOrderRateRequest;
import com.property.module.lifeservice.dto.repair.response.RepairOrderVO;
import com.property.module.lifeservice.service.RepairOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "便民报修", description = "业主端提交报修、我的工单、取消、评价")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/owner/service/repair")
@RequiredArgsConstructor
public class OwnerRepairController {

    private final RepairOrderService repairOrderService;

    @Operation(summary = "提交报修", description = "文字+图片+分类+紧急程度，提交后进入待审核")
    @PostMapping
    public ApiResult<RepairOrderVO> create(@Valid @RequestBody RepairOrderCreateRequest request) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        return ApiResult.success(repairOrderService.create(request, ownerId));
    }

    @Operation(summary = "我的工单")
    @GetMapping("/mine")
    public ApiResult<IPage<RepairOrderVO>> mine(RepairOrderQuery query) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        return ApiResult.success(repairOrderService.myPage(query, ownerId));
    }

    @Operation(summary = "工单详情")
    @GetMapping("/{id}")
    public ApiResult<RepairOrderVO> detail(@PathVariable Long id) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        return ApiResult.success(repairOrderService.getById(id, ownerId));
    }

    @Operation(summary = "取消工单", description = "仅待审核/待派单状态可取消")
    @PostMapping("/{id}/cancel")
    public ApiResult<?> cancel(@PathVariable Long id) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        repairOrderService.cancel(id, ownerId);
        return ApiResult.success("取消成功");
    }

    @Operation(summary = "评价工单", description = "仅已完成工单可评，1-5星+评语")
    @PostMapping("/{id}/rate")
    public ApiResult<?> rate(@PathVariable Long id, @Valid @RequestBody RepairOrderRateRequest request) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        repairOrderService.rate(id, request, ownerId);
        return ApiResult.success("评价成功");
    }
}
