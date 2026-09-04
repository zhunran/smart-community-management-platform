package com.property.module.bill.service;

import com.property.module.bill.dto.response.DashboardVO;
import com.property.module.bill.dto.response.FeeItemStatVO;

import java.util.List;

/**
 * 仪表盘服务接口
 */
public interface DashboardService {

    /**
     * 获取首页仪表盘统计数据
     * @param period 统计月份（如 2026-06），为空则取当前月
     * @return 仪表盘统计 VO
     */
    DashboardVO getDashboard(String period);

    /**
     * 按费用项目维度统计指定账期的应收/实收/户数/收缴率
     * @param period 统计月份（如 2026-06），为空则取当前月
     * @return 各费用项统计列表
     */
    List<FeeItemStatVO> getFeeItemStats(String period);
}
