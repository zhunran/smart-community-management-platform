package com.property.adminapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.common.result.ApiResult;
import com.property.framework.web.annotation.OperationLog;
import com.property.framework.web.security.SecurityUtil;
import com.property.module.lifeservice.dto.repair.request.RepairOrderAssignRequest;
import com.property.module.lifeservice.dto.repair.request.RepairOrderAuditRequest;
import com.property.module.lifeservice.dto.repair.request.RepairOrderCompleteRequest;
import com.property.module.lifeservice.dto.repair.request.RepairOrderQuery;
import com.property.module.lifeservice.dto.repair.response.RepairOrderVO;
import com.property.module.lifeservice.dto.repair.response.RepairStatisticsVO;
import com.property.module.lifeservice.service.RepairOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "报修工单管理", description = "管理端审核、派单、接单、完工、统计")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/admin/service/repair")
@RequiredArgsConstructor
public class AdminRepairController {

    private final RepairOrderService repairOrderService;

    @Operation(summary = "工单列表", description = "按状态、分类、超时筛选")
    @GetMapping("/page")
    public ApiResult<IPage<RepairOrderVO>> page(RepairOrderQuery query) {
        return ApiResult.success(repairOrderService.adminPage(query));
    }

    @Operation(summary = "工单详情")
    @GetMapping("/{id}")
    public ApiResult<RepairOrderVO> detail(@PathVariable Long id) {
        return ApiResult.success(repairOrderService.adminGetDetail(id));
    }

    @OperationLog(module = "报修工单", action = "审核工单")
    @Operation(summary = "审核工单", description = "通过→待派单 / 驳回→已驳回+原因（行级锁防并发）")
    @PostMapping("/{id}/audit")
    public ApiResult<?> audit(@PathVariable Long id, @Valid @RequestBody RepairOrderAuditRequest request) {
        repairOrderService.audit(id, request);
        return ApiResult.success("审核完成");
    }

    @OperationLog(module = "报修工单", action = "指派维修员")
    @Operation(summary = "指派维修员")
    @PostMapping("/{id}/assign")
    public ApiResult<?> assign(@PathVariable Long id, @Valid @RequestBody RepairOrderAssignRequest request) {
        repairOrderService.assign(id, request);
        return ApiResult.success("派单成功");
    }

    @OperationLog(module = "报修工单", action = "接单")
    @Operation(summary = "维修员接单", description = "仅被指派的维修员本人可接单")
    @PostMapping("/{id}/accept")
    public ApiResult<?> accept(@PathVariable Long id) {
        Long handlerId = SecurityUtil.requireUser().getUserId();
        repairOrderService.accept(id, handlerId);
        return ApiResult.success("接单成功");
    }

    @OperationLog(module = "报修工单", action = "完工")
    @Operation(summary = "维修员完工", description = "维修中→已完成，需填写处理说明")
    @PostMapping("/{id}/complete")
    public ApiResult<?> complete(@PathVariable Long id, @Valid @RequestBody RepairOrderCompleteRequest request) {
        Long handlerId = SecurityUtil.requireUser().getUserId();
        repairOrderService.complete(id, request, handlerId);
        return ApiResult.success("完工成功");
    }

    @Operation(summary = "工单统计", description = "各状态数量 + 平均处理时长")
    @GetMapping("/statistics")
    public ApiResult<RepairStatisticsVO> statistics() {
        return ApiResult.success(repairOrderService.statistics());
    }
}
