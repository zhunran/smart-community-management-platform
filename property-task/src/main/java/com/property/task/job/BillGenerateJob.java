package com.property.task.job;

import com.property.module.bill.dto.request.BillGenerateRequest;
import com.property.module.bill.service.BillService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 账单自动生成定时任务
 *
 * 每月 1 号 00:00 执行，生成上一个月账期的账单（如 3 月 1 日生成 2 月份账单）。
 * 自动计算账期、设置缴费截止日期为当月最后一天。
 *
 * 幂等性：BillService.generate() 内部已做幂等检查，同一房屋同一账期不会重复生成。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillGenerateJob {

    private final BillService billService;

    /**
     * 账单生成任务
     *
     * cron: 0 0 0 1 * ?  每月 1 日凌晨 0 点
     */
    @XxlJob("billGenerateJob")
    public void generate() {
        XxlJobHelper.log("账单自动生成任务开始");

        try {
            // 计算账期：当前月减 1 个月即为上个月
            LocalDate now = LocalDate.now();
            LocalDate lastMonth = now.minusMonths(1);
            String period = lastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));

            // 生成请求（不指定房屋ID，默认所有有效房屋）
            BillGenerateRequest request = new BillGenerateRequest();
            request.setBillPeriod(period);

            XxlJobHelper.log("账期: {}, 开始生成账单", period);

            int count = billService.generate(request);

            String msg = String.format("账单生成完成, 账期: %s, 共生成 %d 笔", period, count);
            XxlJobHelper.log(msg);
            log.info(msg);

        } catch (Exception e) {
            XxlJobHelper.log("账单生成异常: " + e.getMessage());
            log.error("账单生成异常", e);
            throw new RuntimeException("账单生成任务失败", e);
        }
    }
}
