package com.property.task.job;

import com.property.framework.service.SysConfigService;
import com.property.module.lifeservice.entity.RepairOrderEntity;
import com.property.module.lifeservice.repository.RepairOrderMapper;
import com.property.module.notification.service.MailService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报修工单超时扫描任务
 *
 * 每小时执行一次，扫描长时间未流转的工单并标记超时、通知管理员：
 * - 待审核(0)/待派单(1)：超过 24 小时未处理
 * - 已派单(2)：超过 48 小时未被接单
 *
 * 管理员邮箱从 sys_config(key=repair.timeout.admin.email，逗号分隔多个) 读取，
 * 未配置时仅标记超时，跳过邮件通知。
 *
 * cron: 0 0 * * * ?  每小时
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RepairTimeoutJob {

    /** 配置键：超时通知管理员邮箱（逗号分隔多个） */
    private static final String CONFIG_KEY_ADMIN_EMAIL = "repair.timeout.admin.email";

    /** 待审核/待派单超时阈值（小时） */
    private static final int AUDIT_TIMEOUT_HOURS = 24;

    /** 已派单未接单超时阈值（小时） */
    private static final int ACCEPT_TIMEOUT_HOURS = 48;

    private final RepairOrderMapper repairOrderMapper;
    private final MailService mailService;
    private final SysConfigService sysConfigService;

    @XxlJob("repairTimeoutJob")
    public void scan() {
        XxlJobHelper.log("报修工单超时扫描任务开始");

        try {
            LocalDateTime auditDeadline = LocalDateTime.now().minusHours(AUDIT_TIMEOUT_HOURS);
            LocalDateTime acceptDeadline = LocalDateTime.now().minusHours(ACCEPT_TIMEOUT_HOURS);

            // 1. 扫描待审核/待派单超时
            List<RepairOrderEntity> pendingList = repairOrderMapper.selectTimeoutPendingAudit(auditDeadline);
            // 2. 扫描已派单未接单超时
            List<RepairOrderEntity> unacceptedList = repairOrderMapper.selectTimeoutUnaccepted(acceptDeadline);

            int marked = 0;
            for (RepairOrderEntity order : pendingList) {
                order.setTimeoutFlag(1);
                repairOrderMapper.updateById(order);
                marked++;
            }
            for (RepairOrderEntity order : unacceptedList) {
                order.setTimeoutFlag(1);
                repairOrderMapper.updateById(order);
                marked++;
            }

            XxlJobHelper.log("超时扫描完成，标记 {} 单（待审核/待派单超时 {} 单，已派单未接单超时 {} 单）",
                    marked, pendingList.size(), unacceptedList.size());
            log.info("报修工单超时扫描：标记 {} 单（pending={}, unaccepted={}）",
                    marked, pendingList.size(), unacceptedList.size());

            // 3. 有超时单时通知管理员
            if (marked > 0) {
                sendTimeoutNotice(marked, pendingList.size(), unacceptedList.size());
            }

        } catch (Exception e) {
            XxlJobHelper.log("报修工单超时扫描异常: " + e.getMessage());
            log.error("报修工单超时扫描异常", e);
            throw new RuntimeException("报修工单超时扫描任务失败", e);
        }
    }

    /**
     * 发送超时通知邮件给管理员
     */
    private void sendTimeoutNotice(int total, int pendingCount, int unacceptedCount) {
        String adminEmails = sysConfigService.getString(CONFIG_KEY_ADMIN_EMAIL, "");
        if (adminEmails == null || adminEmails.isBlank()) {
            XxlJobHelper.log("未配置管理员邮箱(key={})，跳过超时邮件通知", CONFIG_KEY_ADMIN_EMAIL);
            log.warn("未配置管理员邮箱，跳过超时邮件通知 [key={}]", CONFIG_KEY_ADMIN_EMAIL);
            return;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("totalCount", total);
        variables.put("pendingCount", pendingCount);
        variables.put("unacceptedCount", unacceptedCount);
        variables.put("scanTime", LocalDateTime.now());

        for (String email : adminEmails.split(",")) {
            String to = email.trim();
            if (to.isEmpty()) continue;
            boolean sent = mailService.sendHtml(to, "报修工单超时提醒", "mail/repair-timeout", variables);
            XxlJobHelper.log("超时通知邮件发送{} [to={}]", sent ? "成功" : "失败", to);
            log.info("超时通知邮件发送{} [to={}]", sent ? "成功" : "失败", to);
        }
    }
}
