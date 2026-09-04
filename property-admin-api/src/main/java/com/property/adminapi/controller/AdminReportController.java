package com.property.adminapi.controller;

import com.property.adminapi.excel.ReportExportService;
import com.property.common.result.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 管理员端：收费报表
 */
@Tag(name = "收费报表", description = "月度/季度收费报表查看与导出")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/admin/report")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportExportService reportExportService;

    @Operation(summary = "导出月度收费报表", description = "按账期导出全部房屋的收费明细报表（含应收、已交、欠费、滞纳金、状态）")
    @GetMapping("/export/monthly")
    public void exportMonthly(
            @RequestParam String period,
            @Parameter(hidden = true) HttpServletResponse response) throws IOException {
        reportExportService.exportMonthlyReport(period, response);
    }
}
