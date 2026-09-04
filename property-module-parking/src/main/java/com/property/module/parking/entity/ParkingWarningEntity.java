package com.property.module.parking.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 车位预警实体（对应 t_parking_warning 表）
 *
 * 由双轨对账任务自动生成，支持手动处理/关闭的闭环流程。
 */
@Data
@TableName("t_parking_warning")
public class ParkingWarningEntity {

    @TableId
    private Long id;

    /** 关联车位ID */
    private Long spaceId;

    /** 预警类型：LEASE_EXPIRED / SPACE_IDLE_MISMATCH / PAYMENT_PENDING / OCCUPANCY_ANOMALY / LEASE_EXPIRING */
    private String warningType;

    /** 预警等级：LOW / MEDIUM / HIGH */
    private String warningLevel;

    /** 预警描述 */
    private String description;

    /** 处理状态：0-待处理 1-处理中 2-已处理 3-已关闭 */
    private Integer status;

    /** 处理人 */
    private String handler;

    /** 处理备注 */
    private String handleRemark;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 对账批次号（yyyyMMddHHmmss） */
    private String batchNo;

    private String remark;

    @TableLogic
    private Integer delFlag;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
