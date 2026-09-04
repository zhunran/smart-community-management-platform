package com.property.adminapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.common.dto.PageQuery;
import com.property.common.result.ApiResult;
import com.property.framework.web.annotation.OperationLog;
import com.property.module.community.dto.request.ForumPostAuditRequest;
import com.property.module.community.dto.request.ForumPostQuery;
import com.property.module.community.dto.respose.ForumCommentVO;
import com.property.module.community.dto.respose.ForumPostVO;
import com.property.module.community.service.ForumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "论坛管理", description = "管理端帖子审核、置顶、加精、删除")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/admin/community/forum")
@RequiredArgsConstructor
public class AdminForumController {

    private final ForumService forumService;

    @Operation(summary = "帖子列表", description = "按状态、分类、关键词过滤")
    @GetMapping("/post/page")
    public ApiResult<IPage<ForumPostVO>> page(ForumPostQuery query) {
        return ApiResult.success(forumService.adminPage(query));
    }

    @OperationLog(module = "论坛管理", action = "审核帖子")
    @Operation(summary = "审核帖子", description = "通过 status=1，驳回 status=2 需填写驳回原因")
    @PostMapping("/post/{id}/audit")
    public ApiResult<?> audit(@PathVariable Long id, @Valid @RequestBody ForumPostAuditRequest request) {
        forumService.audit(id, request);
        return ApiResult.success("审核完成");
    }

    @OperationLog(module = "论坛管理", action = "置顶/取消置顶")
    @Operation(summary = "置顶/取消置顶")
    @PostMapping("/post/{id}/pin")
    public ApiResult<?> togglePin(@PathVariable Long id) {
        forumService.togglePin(id);
        return ApiResult.success("操作成功");
    }

    @OperationLog(module = "论坛管理", action = "加精/取消加精")
    @Operation(summary = "加精/取消加精")
    @PostMapping("/post/{id}/essence")
    public ApiResult<?> toggleEssence(@PathVariable Long id) {
        forumService.toggleEssence(id);
        return ApiResult.success("操作成功");
    }

    @OperationLog(module = "论坛管理", action = "删除帖子")
    @Operation(summary = "删除帖子", description = "状态改为已删除")
    @DeleteMapping("/post/{id}")
    public ApiResult<?> deletePost(@PathVariable Long id) {
        forumService.deletePost(id);
        return ApiResult.success("删除成功");
    }

    @Operation(summary = "评论列表", description = "查看指定帖子的评论")
    @GetMapping("/comment/page")
    public ApiResult<IPage<ForumCommentVO>> commentPage(@RequestParam Long postId, PageQuery query) {
        return ApiResult.success(forumService.adminCommentPage(postId, query));
    }

    @OperationLog(module = "论坛管理", action = "删除评论")
    @Operation(summary = "删除评论", description = "状态改为已删除")
    @DeleteMapping("/comment/{id}")
    public ApiResult<?> deleteComment(@PathVariable Long id) {
        forumService.deleteComment(id);
        return ApiResult.success("删除成功");
    }
}