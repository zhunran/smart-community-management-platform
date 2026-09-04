package com.property.module.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告 VO
 */
@Data
public class NoticeVO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String title;

    private String content;

    private String type;

    private Integer status;

    private LocalDateTime publishTime;

    private LocalDateTime createTime;
}