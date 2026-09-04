package com.property.module.statistic.service;


import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.property.framework.dto.OperationLogQuery;
import com.property.framework.entity.SysOperationLogEntity;
import com.property.framework.repository.SysOperationLogMapper;
import com.property.module.statistic.constant.RiskActionConstant;
import com.property.module.statistic.repository.FeeTrendMapper;
import com.property.module.statistic.vo.AuditLogExportRow;
import com.property.module.statistic.vo.AuditLogVO;
import com.property.module.statistic.vo.AuditModuleStatVO;
import com.property.module.statistic.vo.AuditSummaryVO;
import com.property.module.statistic.vo.AuditUserStatVO;
import lombok.RequiredArgsConstructor;
import java.io.OutputStream;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditLogStatService {
    private final SysOperationLogMapper sysOperationLogMapper;
    private final FeeTrendMapper feeTrendMapper;

    public Page<AuditLogVO> pageAuditLog(OperationLogQuery query)
    {
        LambdaQueryWrapper<SysOperationLogEntity> wrapper = buildQuery(query);

        Page<SysOperationLogEntity> page = new Page<>(query.getPageNum(), query.getPageSize());
        List<AuditLogVO> logVOS = sysOperationLogMapper.selectPage(page, wrapper).getRecords().stream()
                .map(entity -> {
                    AuditLogVO vo = new AuditLogVO();
                    BeanUtils.copyProperties(entity, vo);
                    return vo;
                })
                .toList();
        Page<AuditLogVO> auditLogVOPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        auditLogVOPage.setRecords(logVOS);
        return auditLogVOPage;
    }

    public AuditSummaryVO getAuditSummary(OperationLogQuery query)
    {
        LocalDateTime[] range = resolveRange(query.getStart(), query.getEnd());
        LocalDateTime start = range[0];
        LocalDateTime end = range[1];

        // 总/失计数复用分页查询的统计口径：status 可空
        Long totalCount =
                sysOperationLogMapper.selectCount(new LambdaQueryWrapper<SysOperationLogEntity>()
                        .ge(SysOperationLogEntity::getCreateTime, start)
                        .lt(SysOperationLogEntity::getCreateTime, end.plusDays(1))
                        .eq(query.getStatus() != null, SysOperationLogEntity::getStatus, query.getStatus()));
        Long failCount =
                sysOperationLogMapper.selectCount(new LambdaQueryWrapper<SysOperationLogEntity>()
                        .ge(SysOperationLogEntity::getCreateTime, start)
                        .lt(SysOperationLogEntity::getCreateTime, end.plusDays(1))
                        .eq(true, SysOperationLogEntity::getStatus, 0));

        // 失败率：除 0 防护
        BigDecimal failRate = BigDecimal.ZERO;
        if (totalCount != null && totalCount > 0) {
            failRate = BigDecimal.valueOf(failCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalCount), 2, java.math.RoundingMode.HALF_UP);
        }

        List<AuditModuleStatVO> moduleTop = feeTrendMapper.selectModuleCount(start, end.plusDays(1), query.getStatus(), query.getUserName());
        List<AuditUserStatVO> userTop = feeTrendMapper.selectUserStat(start, end.plusDays(1));
        List<Map<String, Object>> actionCount = feeTrendMapper.selectAction(start, end.plusDays(1));

        // 风险动作过滤（§3.3）：行键为 SQL 列标签原样（action/count），从 Map 行中提取
        Map<String, Long> riskActionCount = new LinkedHashMap<>();
        if (actionCount != null) {
            actionCount.forEach(row -> {
                Object action = row.get("action");
                Object count = row.get("count");
                if (action != null && count instanceof Number number
                        && RiskActionConstant.isRisk(action.toString())) {
                    riskActionCount.put(action.toString(), number.longValue());
                }
            });
        }

        return new AuditSummaryVO(totalCount, failCount, failRate, moduleTop, userTop, riskActionCount);
    }

    /**
     * 导出操作审计（§3.4）
     *
     * 复用 {@link #buildQuery} 的筛选条件，按 2000/批 拉取全量并写入 Excel，
     * 中文文件名做 URL 编码，流式写出到响应。
     */
    public void exportAuditLog(OperationLogQuery query, OutputStream out) throws IOException {
         // 计数与每页查询都基于同一条件 wrapper（新建避免 last/orderBy 累加，保持一致筛选）
         long total = sysOperationLogMapper.selectCount(buildCondition(query));

         // 复用 ReportExportService 已验证的写法：ExcelWriter + WriteSheet，close() 内部调用 finish() 收尾
         try (var outputStream = out) {
             ExcelWriter excelWriter = EasyExcel.write(out, AuditLogExportRow.class).build();
             WriteSheet writeSheet = EasyExcel.writerSheet("操作审计").build();
             try {
                 final int pageSize = 2000;
                 for (int i = 0; (long) i * pageSize < total; i++) {
                     List<AuditLogExportRow> rows = sysOperationLogMapper.selectList(
                                     buildCondition(query).orderByDesc(SysOperationLogEntity::getCreateTime)
                                             .last("LIMIT " + (long) i * pageSize + "," + pageSize)
                             ).stream().map(AuditLogStatService::toExportRow).toList();
                     excelWriter.write(rows, writeSheet);
                 }
             } finally {
                 excelWriter.finish();
             }
         }
     }

    private static AuditLogExportRow toExportRow(SysOperationLogEntity entity) {
        AuditLogExportRow row = new AuditLogExportRow();
        row.setCreateTime(entity.getCreateTime());
        row.setUserName(entity.getUserName());
        row.setRealName(entity.getRealName());
        row.setModule(entity.getModule());
        row.setAction(entity.getAction());
        row.setRequestMethod(entity.getRequestMethod());
        row.setRequestUrl(entity.getRequestUrl());
        row.setIpAddress(entity.getIpAddress());
        row.setStatusText(entity.getStatus() != null && entity.getStatus() == 1 ? "成功" : "失败");
        row.setCostTime(entity.getCostTime());
        row.setResultMsg(entity.getResultMsg());
        return row;
    }

    /** 仅条件（无排序/分页），供分页与导出的批处理各自追加 orderBy/LIMIT */
    private LambdaQueryWrapper<SysOperationLogEntity> buildCondition(OperationLogQuery query) {
        LocalDateTime[] range = resolveRange(query.getStart(), query.getEnd());
        LocalDateTime start = range[0];
        LocalDateTime end = range[1];

        // 半开区间 [start, end 次日)：end 当天也会被纳入；筛选项可空
        return new LambdaQueryWrapper<SysOperationLogEntity>()
                .ge(SysOperationLogEntity::getCreateTime, start)
                .lt(SysOperationLogEntity::getCreateTime, end.plusDays(1))
                .eq(query.getStatus() != null, SysOperationLogEntity::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getModule()), SysOperationLogEntity::getModule, query.getModule())
                .like(StringUtils.hasText(query.getKeyword()), SysOperationLogEntity::getRealName, query.getKeyword())
                .like(StringUtils.hasText(query.getUserName()), SysOperationLogEntity::getUserName, query.getUserName());
    }

    /** 复用 Phase 2 的筛选条件：半开区间 [start, end 次日) + 可选筛选 + 按时间倒序 */
    private LambdaQueryWrapper<SysOperationLogEntity> buildQuery(OperationLogQuery query) {
        return buildCondition(query).orderByDesc(SysOperationLogEntity::getCreateTime);
    }

    /** 默认时间兜底 + 边界校验：都空→近30天；单边→以锚补30天；都传→直接用 */
    private LocalDateTime[] resolveRange(LocalDateTime start, LocalDateTime end)
    {
        if (start != null && end == null) {
            end = start.plusDays(29);
        } else if (start == null && end != null) {
            start = end.minusDays(29);
        } else if (start == null) {
            end = LocalDateTime.now();
            start = end.minusDays(29);
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("选取时间不合理");
        }
        if (ChronoUnit.DAYS.between(start, end) > 365) {
            throw new IllegalArgumentException("时间区间不能超过1年");
        }
        return new LocalDateTime[]{start, end};
    }
}
