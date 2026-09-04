package com.property.task.job;

import com.property.framework.service.SysConfigService;
import com.property.module.bill.entity.BillEntity;
import com.property.module.bill.repository.BillMapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 逾期账单扫描任务
 *
 * 每日 08:00 执行，扫描已过缴费截止日但未缴清的账单，计算逾期天数和罚息并写入。
 *
 * 罚息规则（从 sys_config 表读取）：
 * - late.fee.rate（配置项 4）：日罚息利率，默认 0.001（0.1%）
 * - late.fee.days（配置项 5）：超过截止日多少天后开始计算罚息，默认 30 天
 * - 计息基数：未缴金额（total_amount - paid_amount）
 * - 罚息上限：不超过未缴金额的 100%
 *
 * 幂等性：每次执行重新计算并覆盖 late_fee，支持重复执行。
 *
 * 如需实时修改罚息规则，直接更新 t_sys_config 表对应配置项即可，下次任务执行自动生效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueScanJob {

    /** 配置键：日罚息利率 */
    private static final String CONFIG_KEY_RATE = "late.fee.rate";

    /** 配置键：逾期起算天数（超过截止日多少天后开始计算罚息） */
    private static final String CONFIG_KEY_OVERDUE_DAYS = "late.fee.days";

    /** 罚息上限倍数（相对未缴金额，固定为 100%） */
    private static final BigDecimal MAX_RATE = new BigDecimal("1.00");

    private final BillMapper billMapper;
    private final SysConfigService sysConfigService;

    /**
     * 逾期扫描 + 罚息计算任务
     *
     * cron: 0 0 8 * * ?  每日 08:00
     */
    @XxlJob("overdueScanJob")
    @Transactional(rollbackFor = Exception.class)
    public void scan() {
        XxlJobHelper.log("逾期账单扫描任务开始");

        try {
            // 1. 读取参数化配置
            BigDecimal dailyRate = getDailyRate();
            int overdueThreshold = getOverdueThreshold();

            XxlJobHelper.log("罚息配置: 日利率={}, 逾期起算天数={}", dailyRate, overdueThreshold);

            // 2. 查询逾期未缴费的账单（已超过 due_date 的）
            LocalDate today = LocalDate.now();
            List<BillEntity> overdueBills = billMapper.selectOverdueBills(today);

            if (overdueBills.isEmpty()) {
                XxlJobHelper.log("无逾期账单");
                log.info("逾期扫描：无逾期账单");
                return;
            }

            XxlJobHelper.log("发现 {} 笔逾期账单", overdueBills.size());
            int updateCount = 0;

            for (BillEntity bill : overdueBills) {
                try {
                    boolean updated = processOverdueBill(bill, today, dailyRate, overdueThreshold);
                    if (updated) updateCount++;
                } catch (Exception e) {
                    XxlJobHelper.log("处理逾期账单异常 [billId={}]: {}", bill.getId(), e.getMessage());
                    log.warn("逾期账单处理异常 [billId={}]", bill.getId(), e);
                }
            }

            String msg = String.format("逾期扫描完成，处理 %d / %d 笔", updateCount, overdueBills.size());
            XxlJobHelper.log(msg);
            log.info(msg);

        } catch (Exception e) {
            XxlJobHelper.log("逾期扫描异常: " + e.getMessage());
            log.error("逾期扫描异常", e);
            throw new RuntimeException("逾期扫描任务失败", e);
        }
    }

    /**
     * 处理单笔逾期账单的罚息计算
     */
    private boolean processOverdueBill(BillEntity bill, LocalDate today,
                                       BigDecimal dailyRate, int overdueThreshold) {
        // 计算已逾期天数（从截止日次日起算）
        long overdueDays = ChronoUnit.DAYS.between(bill.getDueDate(), today);
        if (overdueDays <= 0) {
            return false;
        }

        // 计算未缴金额
        BigDecimal unpaid = bill.getTotalAmount().subtract(
                bill.getPaidAmount() != null ? bill.getPaidAmount() : BigDecimal.ZERO);
        if (unpaid.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        // 超过起算天数后才计算罚息并标记逾期
        long penaltyDays = overdueDays - overdueThreshold;
        if (penaltyDays <= 0) {
            return false;
        }

        // 计算罚息 = 未缴金额 * 日利率 * （逾期天数 - 起算天数）
        BigDecimal lateFee = unpaid.multiply(dailyRate)
                .multiply(BigDecimal.valueOf(penaltyDays))
                .setScale(2, RoundingMode.HALF_UP);

        // 罚息上限：不超过未缴金额
        BigDecimal maxFee = unpaid.multiply(MAX_RATE).setScale(2, RoundingMode.HALF_UP);
        if (lateFee.compareTo(maxFee) > 0) {
            lateFee = maxFee;
        }

        // 判断当前状态是否需要标记为逾期（只有 UNPAID/PARTIAL 需要标记，已逾期的只更新罚息）
        boolean needMarkOverdue = bill.getStatus() != null
                && (bill.getStatus() == 0 || bill.getStatus() == 1);

        int updated;
        if (needMarkOverdue) {
            // 更新状态为 OVERDUE(5) + 同时写入罚息
            updated = billMapper.markAsOverdue(bill.getId(), lateFee);
            if (updated > 0) {
                XxlJobHelper.log("账单[{}] 标记为逾期, 逾期{}天, 计息{}天, 未缴{}, 罚息{}",
                        bill.getId(), overdueDays, penaltyDays, unpaid, lateFee);
                log.info("逾期标记并更新罚息 [billId={}, overdueDays={}, penaltyDays={}, unpaid={}, lateFee={}]",
                        bill.getId(), overdueDays, penaltyDays, unpaid, lateFee);
                return true;
            }
        } else {
            // 已是 OVERDUE 状态，只更新罚息
            updated = billMapper.updateLateFee(bill.getId(), lateFee);
            if (updated > 0) {
                XxlJobHelper.log("账单[{}] 逾期{}天, 计息{}天, 未缴{}, 罚息{}",
                        bill.getId(), overdueDays, penaltyDays, unpaid, lateFee);
                log.info("逾期罚息已更新 [billId={}, overdueDays={}, penaltyDays={}, unpaid={}, lateFee={}]",
                        bill.getId(), overdueDays, penaltyDays, unpaid, lateFee);
                return true;
            }
        }
        return false;
    }

    /**
     * 读取日罚息利率，取不到则使用默认值 0.001（0.1%）
     */
    private BigDecimal getDailyRate() {
        return sysConfigService.getBigDecimal(CONFIG_KEY_RATE, new BigDecimal("0.001"));
    }

    /**
     * 读取逾期起算天数，取不到则使用默认值 30 天
     */
    private int getOverdueThreshold() {
        return sysConfigService.getInt(CONFIG_KEY_OVERDUE_DAYS, 30);
    }
}
