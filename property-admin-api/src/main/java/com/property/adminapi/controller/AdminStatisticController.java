package com.property.adminapi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.property.common.result.ApiResult;
import com.property.framework.dto.OperationLogQuery;
import com.property.module.statistic.service.AuditLogStatService;
import com.property.module.statistic.service.FeeTrendStatService;
import com.property.module.statistic.vo.AuditLogVO;
import com.property.module.statistic.vo.AuditSummaryVO;
import com.property.module.statistic.vo.FeeTrendPointVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 管理员端：报表统计（财务缴费趋势 + 操作审计）
 */
@Tag(name = "报表统计", description = "财务缴费趋势与操作审计查询、统计、导出")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/admin/statistic")
@RequiredArgsConstructor
public class AdminStatisticController {

    private final FeeTrendStatService feeTrendStatService;
    private final AuditLogStatService auditLogStatService;

    @Operation(summary = "每日缴费趋势", description = "统计区间内每日缴费人数与金额，默认近 30 天")
    @GetMapping("/fee/trend")
    public ApiResult<List<FeeTrendPointVO>> getFeeTrend(@RequestParam(required = false) LocalDate start,
                                                        @RequestParam(required = false) LocalDate end) {
        return ApiResult.success(feeTrendStatService.getDailyFeeTrend(start, end));
    }

    @Operation(summary = "操作审计日志分页", description = "按操作者/模块/结果/时间范围分页查询操作审计日志")
    @GetMapping("/audit/log")
    public ApiResult<Page<AuditLogVO>> auditLogQuery(@ModelAttribute OperationLogQuery query) {
        return ApiResult.success(auditLogStatService.pageAuditLog(query));
    }

    @Operation(summary = "操作审计聚合统计", description = "按模块/操作者 TopN、失败率、风险动作次数聚合")
    @GetMapping("/audit/summary")
    public ApiResult<AuditSummaryVO> auditSummary(@ModelAttribute OperationLogQuery query) {
        return ApiResult.success(auditLogStatService.getAuditSummary(query));
    }

    @Operation(summary = "导出操作审计日志", description = "按筛选条件导出操作审计日志 Excel")
    @GetMapping("/audit/export")
    public void auditLogExport(@ModelAttribute OperationLogQuery query,
                               @Parameter(hidden = true) HttpServletResponse response) throws IOException {
        if (query == null) {
            query = new OperationLogQuery();
        }
        String fileName = URLEncoder.encode("操作审计" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
                StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xlsx");
        auditLogStatService.exportAuditLog(query, response.getOutputStream());
    }
}
