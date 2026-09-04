package com.property.module.bill.service.impl;

import com.property.module.bill.dto.response.DashboardVO;
import com.property.module.bill.dto.response.FeeItemStatVO;
import com.property.module.bill.repository.BillItemMapper;
import com.property.module.bill.repository.BillMapper;
import com.property.module.bill.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 仪表盘服务实现
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BillMapper billMapper;
    private final BillItemMapper billItemMapper;

    @Override
    public DashboardVO getDashboard(String period) {
        // 默认取当前月
        if (period == null || period.isBlank()) {
            period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        BigDecimal receivable = billMapper.sumReceivableByPeriod(period);
        BigDecimal received = billMapper.sumReceivedByPeriod(period);
        BigDecimal arrears = billMapper.sumArrears();

        // 收缴率 = 实收 / 应收 * 100，保留两位小数
        BigDecimal collectionRate = BigDecimal.ZERO;
        if (receivable.compareTo(BigDecimal.ZERO) > 0) {
            collectionRate = received.multiply(BigDecimal.valueOf(100))
                    .divide(receivable, 2, RoundingMode.HALF_UP);
        }

        DashboardVO vo = new DashboardVO();
        vo.setStatisticsMonth(period);
        vo.setCurrentMonthReceivable(receivable);
        vo.setCurrentMonthReceived(received);
        vo.setTotalArrears(arrears);
        vo.setCollectionRate(collectionRate);
        return vo;
    }

    @Override
    public List<FeeItemStatVO> getFeeItemStats(String period) {
        // 默认取当前月
        if (period == null || period.isBlank()) {
            period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        // 1. 查询各费用项的应收/实收/户数
        List<Map<String, Object>> statsRows = billItemMapper.selectFeeItemStatsByPeriod(period);

        // 2. 查询各费用项的已缴户数
        List<Map<String, Object>> paidRows = billItemMapper.selectPaidBillCountByPeriod(period);
        Map<Object, Object> paidCountMap = paidRows.stream()
                .collect(Collectors.toMap(
                        r -> r.get("fee_item_id"),
                        r -> r.get("paid_count")
                ));

        // 3. 组装结果
        List<FeeItemStatVO> result = new ArrayList<>();
        for (Map<String, Object> row : statsRows) {
            FeeItemStatVO vo = new FeeItemStatVO();
            vo.setFeeItemId(toLong(row.get("fee_item_id")));
            vo.setFeeItemName((String) row.get("fee_item_name"));
            vo.setReceivable(toBigDecimal(row.get("receivable")));
            vo.setReceived(toBigDecimal(row.get("received")));
            vo.setBillCount(toInt(row.get("bill_count")));

            Object paidObj = paidCountMap.get(row.get("fee_item_id"));
            int paidCount = paidObj != null ? toInt(paidObj) : 0;
            vo.setPaidCount(paidCount);

            // 收缴率
            BigDecimal receivable = vo.getReceivable();
            BigDecimal received = vo.getReceived();
            if (receivable != null && receivable.compareTo(BigDecimal.ZERO) > 0 && received != null) {
                BigDecimal rate = received.multiply(BigDecimal.valueOf(100))
                        .divide(receivable, 2, RoundingMode.HALF_UP);
                vo.setCollectionRate(rate);
            } else {
                vo.setCollectionRate(BigDecimal.ZERO);
            }

            result.add(vo);
        }

        return result;
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        return Long.valueOf(obj.toString());
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        if (obj instanceof Number) return BigDecimal.valueOf(((Number) obj).doubleValue());
        return new BigDecimal(obj.toString());
    }

    private int toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        return Integer.parseInt(obj.toString());
    }
}
