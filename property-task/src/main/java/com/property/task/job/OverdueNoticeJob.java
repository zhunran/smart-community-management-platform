package com.property.task.job;

import com.property.module.bill.entity.BillEntity;
import com.property.module.bill.repository.BillMapper;
import com.property.module.notification.service.MailService;
import com.property.module.owner.entity.OwnerEntity;
import com.property.module.owner.repository.OwnerMapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 逾期账单邮件通知任务
 *
 * 每日 09:00 执行，将逾期未缴费的账单通过邮件通知业主。
 * 同一业主有多笔逾期账单时合并为一封邮件发送。
 *
 * 依赖 OverdueScanJob 先执行（每日 08:00），确保罚息已计算。
 *
 * 幂等性：每次重新查询逾期账单并发送，重复执行只发当前逾期数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueNoticeJob {

    private final BillMapper billMapper;
    private final OwnerMapper ownerMapper;
    private final MailService mailService;

    /**
     * 逾期邮件通知任务
     *
     * cron: 0 0 9 * * ?  每日 09:00（需确保 OverdueScanJob 08:00 已先执行）
     */
    @XxlJob("overdueNoticeJob")
    public void sendOverdueNotices() {
        XxlJobHelper.log("逾期邮件通知任务开始");

        try {
            // 1. 查询逾期未缴费的账单
            LocalDate today = LocalDate.now();
            List<BillEntity> overdueBills = billMapper.selectOverdueBills(today);

            if (overdueBills.isEmpty()) {
                XxlJobHelper.log("无逾期账单，跳过邮件通知");
                log.info("逾期邮件通知：无逾期账单");
                return;
            }

            XxlJobHelper.log("查询到 {} 笔逾期账单", overdueBills.size());

            // 2. 按 ownerId 分组
            Map<Long, List<BillEntity>> billsByOwner = overdueBills.stream()
                    .filter(b -> b.getOwnerId() != null)
                    .collect(Collectors.groupingBy(BillEntity::getOwnerId));

            XxlJobHelper.log("涉及 {} 位业主", billsByOwner.size());

            // 3. 逐业主发送邮件
            int sentCount = 0;
            int failCount = 0;

            for (Map.Entry<Long, List<BillEntity>> entry : billsByOwner.entrySet()) {
                try {
                    boolean sent = sendNoticeToOwner(entry.getKey(), entry.getValue());
                    if (sent) {
                        sentCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    XxlJobHelper.log("业主[{}]邮件发送异常: {}", entry.getKey(), e.getMessage());
                    log.warn("业主[{}]邮件发送异常", entry.getKey(), e);
                    failCount++;
                }
            }

            String msg = String.format("逾期邮件通知完成，发送成功 %d 人，失败 %d 人", sentCount, failCount);
            XxlJobHelper.log(msg);
            log.info(msg);

        } catch (Exception e) {
            XxlJobHelper.log("逾期邮件通知异常: " + e.getMessage());
            log.error("逾期邮件通知异常", e);
            throw new RuntimeException("逾期邮件通知任务失败", e);
        }
    }

    /**
     * 向单个业主发送逾期通知邮件
     */
    private boolean sendNoticeToOwner(Long ownerId, List<BillEntity> bills) {
        // 1. 查询业主信息（含邮箱）
        OwnerEntity owner = ownerMapper.selectById(ownerId);
        if (owner == null || owner.getEmail() == null || owner.getEmail().isBlank()) {
            XxlJobHelper.log("业主[{}] 无邮箱信息，跳过", ownerId);
            log.info("业主[{}] 无邮箱信息，跳过邮件通知", ownerId);
            return false;
        }

        // 2. 计算逾期总金额（未缴部分）
        BigDecimal totalOverdueAmount = bills.stream()
                .map(b -> b.getTotalAmount().subtract(
                        b.getPaidAmount() != null ? b.getPaidAmount() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. 准备模板参数
        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
        Map<String, Object> variables = new HashMap<>();
        variables.put("ownerName", owner.getOwnerName());
        variables.put("overdueCount", bills.size());
        variables.put("totalOverdueAmount", totalOverdueAmount);
        variables.put("bills", bills);
        variables.put("todayStr", todayStr);
        variables.put("propertyName", "物业管理处");
        variables.put("contactPhone", "请咨询物业管理处");
        variables.put("portalUrl", "#");

        // 4. 发送邮件
        String subject = String.format("物业费逾期提醒——您有 %d 笔账单已逾期", bills.size());
        boolean sent = mailService.sendHtml(owner.getEmail(), subject, "mail/overdue-notice", variables);

        if (sent) {
            XxlJobHelper.log("业主[{}] {} 逾期邮件已发送至 {}", ownerId, owner.getOwnerName(), owner.getEmail());
            log.info("逾期邮件已发送 [ownerId={}, owner={}, email={}, bills={}]",
                    ownerId, owner.getOwnerName(), owner.getEmail(), bills.size());
        }
        return sent;
    }
}
