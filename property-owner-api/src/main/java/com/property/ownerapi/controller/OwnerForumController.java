package com.property.ownerapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.common.result.ApiResult;
import com.property.framework.web.security.SecurityUtil;
import com.property.module.community.dto.request.ForumCommentCreateRequest;
import com.property.module.community.dto.request.ForumLikeRequest;
import com.property.module.community.dto.request.ForumPostCreateRequest;
import com.property.module.community.dto.request.ForumPostQuery;
import com.property.module.community.dto.respose.ForumCommentVO;
import com.property.module.community.dto.respose.ForumPostDetailVO;
import com.property.module.community.dto.respose.ForumPostVO;
import com.property.module.community.service.ForumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "社区论坛", description = "业主端论坛发帖、评论、点赞")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/owner/community/forum")
@RequiredArgsConstructor
public class OwnerForumController {

    private final ForumService forumService;

    @Operation(summary = "发帖", description = "敏感词过滤：无命中直接发布，命中进入待审核")
    @PostMapping("/post")
    public ApiResult<?> createPost(@Valid @RequestBody ForumPostCreateRequest request) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        forumService.createPost(request, ownerId);
        return ApiResult.success("发帖成功");
    }

    @Operation(summary = "帖子列表", description = "仅已发布帖子，置顶优先，按时间倒序")
    @GetMapping("/post/page")
    public ApiResult<IPage<ForumPostVO>> page(ForumPostQuery query) {
        return ApiResult.success(forumService.ownerPage(query));
    }

    @Operation(summary = "我的帖子", description = "当前用户的所有帖子")
    @GetMapping("/post/mine")
    public ApiResult<IPage<ForumPostVO>> myPosts(ForumPostQuery query) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        return ApiResult.success(forumService.myPosts(query, ownerId));
    }

    @Operation(summary = "帖子详情", description = "浏览量+1，含评论列表和点赞状态")
    @GetMapping("/post/{id}")
    public ApiResult<ForumPostDetailVO> getDetail(@PathVariable Long id) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        return ApiResult.success(forumService.getDetail(id, ownerId));
    }

    @Operation(summary = "发表评论", description = "一级评论 parentId=0，二级评论需指定 parentId 和 replyTo")
    @PostMapping("/post/{postId}/comment")
    public ApiResult<?> createComment(@PathVariable Long postId,
                                      @Valid @RequestBody ForumCommentCreateRequest request) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        request.setPostId(postId);
        forumService.createComment(request, ownerId);
        return ApiResult.success("评论成功");
    }

    @Operation(summary = "评论列表", description = "两级嵌套展示：一级评论含子评论列表")
    @GetMapping("/post/{postId}/comment")
    public ApiResult<List<ForumCommentVO>> getComments(@PathVariable Long postId) {
        return ApiResult.success(forumService.getComments(postId));
    }

    @Operation(summary = "点赞/取消点赞", description = "toggle 模式，幂等")
    @PostMapping("/like")
    public ApiResult<?> like(@Valid @RequestBody ForumLikeRequest request) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        forumService.like(request, ownerId);
        return ApiResult.success("ok");
    }
}