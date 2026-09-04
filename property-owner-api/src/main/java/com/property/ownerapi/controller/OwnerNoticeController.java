package com.property.ownerapi.controller;

import com.property.common.result.ApiResult;
import com.property.module.notification.dto.NoticeVO;
import com.property.module.notification.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 业主端：社区公告查询
 */
@Tag(name = "业主公告", description = "业主查看小区已发布公告")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/owner/notices")
@RequiredArgsConstructor
public class OwnerNoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "已发布公告列表", description = "按发布时间倒序返回最新已发布公告")
    @GetMapping
    public ApiResult<List<NoticeVO>> list() {
        return ApiResult.success(noticeService.listLatest(50));
    }
}