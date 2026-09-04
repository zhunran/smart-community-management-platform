package com.property.adminapi.controller;

import com.property.common.result.ApiResult;
import com.property.framework.web.security.SecurityUtil;
import com.property.module.notification.dto.NoticeCreateRequest;
import com.property.module.notification.dto.NoticePageQuery;
import com.property.module.notification.dto.NoticeVO;
import com.property.module.notification.service.NoticeService;
import com.property.framework.web.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公告管理 Controller（管理端）
 */
@Tag(name = "公告管理", description = "小区公告发布与管理")
@RestController
@RequestMapping("/api/admin/notice")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;

    @OperationLog(module = "公告管理", action = "创建公告")
    @Operation(summary = "创建公告（草稿）")
    @PostMapping
    public ApiResult<NoticeVO> create(@Valid @RequestBody NoticeCreateRequest request) {
        Long adminId = SecurityUtil.requireUser().getUserId();
        return ApiResult.success(noticeService.create(request, adminId));
    }

    @OperationLog(module = "公告管理", action = "发布公告")
    @Operation(summary = "发布公告")
    @PutMapping("/{id}/publish")
    public ApiResult<Void> publish(@PathVariable Long id) {
        noticeService.publish(id);
        return ApiResult.success();
    }

    @OperationLog(module = "公告管理", action = "下线公告")
    @Operation(summary = "下线公告")
    @PutMapping("/{id}/offline")
    public ApiResult<Void> offline(@PathVariable Long id) {
        noticeService.offline(id);
        return ApiResult.success();
    }

    @Operation(summary = "分页查询公告")
    @GetMapping("/page")
    public ApiResult<Object> page(NoticePageQuery query) {
        return ApiResult.success(noticeService.page(query));
    }
}