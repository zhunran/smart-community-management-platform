package com.property.adminapi.controller;

import com.property.common.result.ApiResult;
import com.property.module.bill.dto.response.DashboardVO;
import com.property.module.bill.dto.response.FeeItemStatVO;
import com.property.module.bill.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员端：首页仪表盘
 */
@Tag(name = "仪表盘", description = "首页统计数据")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "首页统计概览", description = "返回当月应收、实收、累计欠费、收缴率")
    @GetMapping("/overview")
    public ApiResult<DashboardVO> overview(@RequestParam(required = false) String period) {
        return ApiResult.success(dashboardService.getDashboard(period));
    }

    @Operation(summary = "按费用项统计", description = "按费用项目维度返回应收、实收、户数、收缴率")
    @GetMapping("/fee-item-stats")
    public ApiResult<List<FeeItemStatVO>> feeItemStats(@RequestParam(required = false) String period) {
        return ApiResult.success(dashboardService.getFeeItemStats(period));
    }
}
