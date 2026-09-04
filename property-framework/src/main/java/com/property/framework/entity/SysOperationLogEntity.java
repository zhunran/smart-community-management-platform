package com.property.framework.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作审计日志实体（对应 t_sys_operation_log 表）
 */
@Data
@TableName("t_sys_operation_log")
public class SysOperationLogEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("trace_id")
    private String traceId;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("real_name")
    private String realName;

    @TableField("module")
    private String module;

    @TableField("action")
    private String action;

    @TableField("request_method")
    private String requestMethod;

    @TableField("request_url")
    private String requestUrl;

    @TableField("request_params")
    private String requestParams;

    @TableField("response_data")
    private String responseData;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("user_agent")
    private String userAgent;

    @TableField("cost_time")
    private Long costTime;

    @TableField("result_code")
    private Integer resultCode;

    @TableField("result_msg")
    private String resultMsg;

    @TableField("status")
    private Integer status;   // 1成功 0失败

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
