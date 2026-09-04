package com.property.adminapi.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.property.module.bill.entity.BillEntity;
import com.property.module.bill.repository.BillMapper;
import com.property.module.owner.service.OwnerService;
import com.property.module.housing.service.RoomDataService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 收费报表导出服务
 *
 * 按账期导出所有房屋的收费明细报表，含应收/已交/欠费/滞纳金/状态。
 * 支持按楼栋分组展示、每栋小计、全表合计，使用 EasyExcel 流式导出。
 */
@Service
@RequiredArgsConstructor
public class ReportExportService {

    private static final String[] STATUS_NAMES = {"未缴费", "部分缴费", "已缴清", "已作废", "已减免", "已逾期"};
    private static final int PAGE_SIZE = 2000;

    private static final List<List<String>> HEAD = List.of(
            List.of("楼栋名称"), List.of("单元名称"), List.of("房号"),
            List.of("业主姓名"), List.of("账单编号"), List.of("账期"),
            List.of("应收金额"), List.of("已交金额"), List.of("欠费金额"),
            List.of("滞纳金"), List.of("缴费状态"), List.of("缴费截止日")
    );

    private final BillMapper billMapper;
    private final RoomDataService roomDataService;
    private final OwnerService ownerService;

    /**
     * 导出月度收费报表
     *
     * @param period   账期（如 2026-06）
     * @param response HTTP 响应
     */
    public void exportMonthlyReport(String period, HttpServletResponse response) throws IOException {
        String fileName = URLEncoder.encode("收费报表" + period, StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xlsx");

        // 查询该账期的所有账单（按房屋ID升序，自然按楼栋分组）
        LambdaQueryWrapper<BillEntity> wrapper = new LambdaQueryWrapper<BillEntity>()
                .eq(BillEntity::getBillPeriod, period)
                .ne(BillEntity::getStatus, 3)
                .orderByAsc(BillEntity::getRoomId);

        long total = billMapper.selectCount(wrapper);
        List<RowData> allRows = new ArrayList<>();

        // 分页查询所有账单并转成 RowData
        for (int i = 0; i * PAGE_SIZE < total; i++) {
            List<BillEntity> bills = billMapper.selectList(
                    wrapper.last("LIMIT " + (i * PAGE_SIZE) + "," + PAGE_SIZE)
            );
            for (BillEntity bill : bills) {
                allRows.add(toRowData(bill));
            }
        }

        // 按楼栋分组
        Map<String, List<RowData>> grouped = allRows.stream()
                .collect(Collectors.groupingBy(
                        r -> r.buildingName != null ? r.buildingName : "未知楼栋",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 构建 Excel 数据行
        List<List<Object>> dataRows = new ArrayList<>();
        BigDecimal grandTotalAmount = BigDecimal.ZERO;
        BigDecimal grandPaidAmount = BigDecimal.ZERO;
        BigDecimal grandArrearsAmount = BigDecimal.ZERO;
        BigDecimal grandLateFee = BigDecimal.ZERO;

        for (Map.Entry<String, List<RowData>> entry : grouped.entrySet()) {
            String buildingName = entry.getKey();
            List<RowData> buildingRows = entry.getValue();

            BigDecimal bldTotal = BigDecimal.ZERO;
            BigDecimal bldPaid = BigDecimal.ZERO;
            BigDecimal bldArrears = BigDecimal.ZERO;
            BigDecimal bldLateFee = BigDecimal.ZERO;

            for (RowData r : buildingRows) {
                dataRows.add(rowToList(r));
                bldTotal = bldTotal.add(r.totalAmount);
                bldPaid = bldPaid.add(r.paidAmount);
                bldArrears = bldArrears.add(r.arrearsAmount);
                bldLateFee = bldLateFee.add(r.lateFee);
            }

            // 楼栋小计行
            dataRows.add(Arrays.asList(
                    "【" + buildingName + "】小计", "", "", "", "", "",
                    fmt(bldTotal), fmt(bldPaid), fmt(bldArrears), fmt(bldLateFee), "", ""
            ));
            // 空行分隔
            dataRows.add(Arrays.asList("", "", "", "", "", "", "", "", "", "", "", ""));

            grandTotalAmount = grandTotalAmount.add(bldTotal);
            grandPaidAmount = grandPaidAmount.add(bldPaid);
            grandArrearsAmount = grandArrearsAmount.add(bldArrears);
            grandLateFee = grandLateFee.add(bldLateFee);
        }

        // 全表合计行
        dataRows.add(Arrays.asList(
                "【合计】", "", "", "", "", "",
                fmt(grandTotalAmount), fmt(grandPaidAmount),
                fmt(grandArrearsAmount), fmt(grandLateFee), "", ""
        ));

        // 写入 Excel
        try (ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).head(HEAD).build()) {
            WriteSheet writeSheet = EasyExcel.writerSheet("收费报表-" + period).build();
            excelWriter.write(dataRows, writeSheet);
        }
    }

    private List<Object> rowToList(RowData r) {
        return Arrays.asList(
                r.buildingName, r.unitName, r.roomCode,
                r.ownerName, r.billNo, r.billPeriod,
                fmt(r.totalAmount), fmt(r.paidAmount),
                fmt(r.arrearsAmount), fmt(r.lateFee),
                r.statusName, r.dueDate
        );
    }

    private RowData toRowData(BillEntity entity) {
        RowData r = new RowData();
        r.billNo = entity.getBillNo();
        r.billPeriod = entity.getBillPeriod();
        r.totalAmount = entity.getTotalAmount() != null ? entity.getTotalAmount() : BigDecimal.ZERO;
        r.paidAmount = entity.getPaidAmount() != null ? entity.getPaidAmount() : BigDecimal.ZERO;
        r.lateFee = entity.getLateFee() != null ? entity.getLateFee() : BigDecimal.ZERO;
        r.arrearsAmount = r.totalAmount.subtract(r.paidAmount);
        r.statusName = getStatusName(entity.getStatus());
        r.dueDate = entity.getDueDate() != null ? entity.getDueDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";

        if (entity.getRoomId() != null) {
            r.buildingName = roomDataService.getBuildingNameByRoomId(entity.getRoomId());
            r.unitName = roomDataService.getUnitNameByRoomId(entity.getRoomId());
            r.roomCode = roomDataService.getRoomCodeByRoomId(entity.getRoomId());
        }
        if (entity.getOwnerId() != null) {
            r.ownerName = ownerService.getOwnerNameById(entity.getOwnerId());
        }
        return r;
    }

    private static String fmt(BigDecimal v) {
        return v != null ? v.setScale(2, RoundingMode.HALF_UP).toString() : "0.00";
    }

    private String getStatusName(Integer status) {
        if (status == null || status < 0 || status >= STATUS_NAMES.length) return "未知";
        return STATUS_NAMES[status];
    }

    /** 内部行数据对象 */
    private static class RowData {
        private String buildingName;
        private String unitName;
        private String roomCode;
        private String ownerName;
        private String billNo;
        private String billPeriod;
        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private BigDecimal arrearsAmount;
        private BigDecimal lateFee;
        private String statusName;
        private String dueDate;
    }
}
