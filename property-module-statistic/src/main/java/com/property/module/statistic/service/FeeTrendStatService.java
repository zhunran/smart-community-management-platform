package com.property.module.statistic.service;

import com.property.module.statistic.repository.FeeTrendMapper;
import com.property.module.statistic.vo.FeeTrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeTrendStatService {
    private final FeeTrendMapper feeTrendMapper;
    public List<FeeTrendPointVO> getDailyFeeTrend(LocalDate start,LocalDate end)
    {
        // 默认值兜底：都为空 → 近30天；只传一个 → 以它为锚补满30天；都传 → 直接用
        if (start != null && end == null) {
            end = start.plusDays(29);
        } else if (start == null && end != null) {
            start = end.minusDays(29);
        } else if (start == null) {
            end = LocalDate.now();
            start = end.minusDays(29);
        }

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("选取时间不合理");
        }
        if (ChronoUnit.DAYS.between(start, end) > 365) {
            throw new IllegalArgumentException("时间区间不能超过1年");
        }
        return feeTrendMapper.selectDailyFeeTrend(start,end);
    }
}
