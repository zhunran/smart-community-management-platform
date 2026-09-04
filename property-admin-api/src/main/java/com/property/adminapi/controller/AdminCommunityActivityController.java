package com.property.adminapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.common.result.ApiResult;
import com.property.framework.web.annotation.OperationLog;
import com.property.module.community.dto.request.CommunityActivityCreateRequest;
import com.property.module.community.dto.request.CommunityActivityQuery;
import com.property.module.community.dto.request.CommunityActivityUpdateRequest;
import com.property.module.community.dto.respose.CommunityActivityDetailVO;
import com.property.module.community.dto.respose.CommunityActivityVO;
import com.property.module.community.service.CommunityActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "社区活动管理", description = "社区活动的增删改查与发布管理")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/admin/community/activity")
@RequiredArgsConstructor
public class AdminCommunityActivityController {

    private final CommunityActivityService communityActivityService;

    @Operation(summary = "活动分页查询", description = "支持标题、类型、状态、地点、时间范围过滤")
    @GetMapping("/page")
    public ApiResult<IPage<CommunityActivityVO>> page(CommunityActivityQuery query) {
        return ApiResult.success(communityActivityService.adminPage(query));
    }

    @Operation(summary = "活动详情")
    @GetMapping("/{id}")
    public ApiResult<CommunityActivityDetailVO> getDetail(@PathVariable Long id) {
        return ApiResult.success(communityActivityService.getDetail(id));
    }

    @OperationLog(module = "社区活动", action = "新增活动（草稿）")
    @Operation(summary = "新增活动（草稿）", description = "创建活动并保存为草稿状态")
    @PostMapping
    public ApiResult<?> create(@Valid @RequestBody CommunityActivityCreateRequest request) {
        communityActivityService.create(request);
        return ApiResult.success("创建成功");
    }

    @OperationLog(module = "社区活动", action = "新增并发布活动")
    @Operation(summary = "新增并发布活动", description = "创建活动并直接发布为招募中状态")
    @PostMapping("/publish")
    public ApiResult<?> createAndPublish(@Valid @RequestBody CommunityActivityCreateRequest request) {
        communityActivityService.publish(request);
        return ApiResult.success("发布成功");
    }

    @OperationLog(module = "社区活动", action = "修改活动")
    @Operation(summary = "修改活动", description = "仅草稿/招募中状态可修改")
    @PutMapping
    public ApiResult<CommunityActivityVO> update(@Valid @RequestBody CommunityActivityUpdateRequest request) {
        return ApiResult.success("修改成功", communityActivityService.updateActivity(request));
    }

    @OperationLog(module = "社区活动", action = "删除活动")
    @Operation(summary = "删除活动", description = "仅草稿状态可删除")
    @DeleteMapping("/{id}")
    public ApiResult<?> delete(@PathVariable Long id) {
        communityActivityService.delete(id);
        return ApiResult.success("删除成功");
    }

    @OperationLog(module = "社区活动", action = "发布活动")
    @Operation(summary = "发布活动", description = "草稿 → 招募中")
    @PostMapping("/{id}/publish")
    public ApiResult<?> publish(@PathVariable Long id) {
        communityActivityService.publishActivity(id);
        return ApiResult.success("发布成功");
    }

    @OperationLog(module = "社区活动", action = "取消活动")
    @Operation(summary = "取消活动", description = "仅草稿/招募中状态可取消，活动开始后不可取消")
    @PostMapping("/{id}/cancel")
    public ApiResult<?> cancel(@PathVariable Long id) {
        communityActivityService.cancelActivity(id);
        return ApiResult.success("已取消");
    }
}