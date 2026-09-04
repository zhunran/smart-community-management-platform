package com.property.module.bill.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 账单手动触发生成请求 DTO
 */
@Data
public class BillGenerateRequest {

    @Schema(description = "账期（如 2024-01）", example = "2024-01", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "账期不能为空")
    @Pattern(regexp = "\\d{4}-\\d{1,2}", message = "账期格式应为 yyyy-MM，如 2026-08")
    private String billPeriod;

    @Schema(description = "业主ID（指定后自动解析该业主名下的房屋，与roomIds互斥，优先使用ownerId）")
    private Long ownerId;

    @Schema(description = "房屋ID列表（为空则生成全部有效房屋）")
    private List<Long> roomIds;

    @Schema(description = "缴费截止日期（为空默认当月最后一天）", example = "2024-01-31")
    private String dueDate;
}
