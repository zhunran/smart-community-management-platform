package com.property.module.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 公告创建请求
 */
@Data
public class NoticeCreateRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private String type = "NOTICE";
}