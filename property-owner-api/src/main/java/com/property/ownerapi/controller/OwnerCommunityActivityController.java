package com.property.ownerapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.common.dto.PageQuery;
import com.property.common.result.ApiResult;
import com.property.framework.web.security.SecurityUtil;
import com.property.module.community.dto.request.CommunityActivityQuery;
import com.property.module.community.dto.respose.ActivitySignupVO;
import com.property.module.community.dto.respose.CommunityActivityDetailVO;
import com.property.module.community.dto.respose.CommunityActivityVO;
import com.property.module.community.service.ActivitySignupService;
import com.property.module.community.service.CommunityActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "社区活动", description = "业主端社区活动浏览与报名")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/owner/community/activity")
@RequiredArgsConstructor
public class OwnerCommunityActivityController {

    private final CommunityActivityService communityActivityService;
    private final ActivitySignupService activitySignupService;

    @Operation(summary = "活动列表", description = "展示招募中/已满员/进行中/已结束的活动")
    @GetMapping("/page")
    public ApiResult<IPage<CommunityActivityVO>> page(CommunityActivityQuery query) {
        return ApiResult.success(communityActivityService.ownerPage(query));
    }

    @Operation(summary = "活动详情", description = "含当前用户是否已报名标识")
    @GetMapping("/{id}")
    public ApiResult<CommunityActivityDetailVO> getDetail(@PathVariable Long id) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        return ApiResult.success(communityActivityService.ownerGetDetail(id, ownerId));
    }

    @Operation(summary = "报名活动", description = "校验活动状态→查重→报名→人数+1→满员自动流转")
    @PostMapping("/{id}/signup")
    public ApiResult<?> signup(@PathVariable Long id) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        activitySignupService.signup(id, ownerId);
        return ApiResult.success("报名成功");
    }

    @Operation(summary = "取消报名", description = "取消报名后报名人数-1")
    @DeleteMapping("/{id}/signup")
    public ApiResult<?> cancelSignup(@PathVariable Long id) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        activitySignupService.cancelSignup(id, ownerId);
        return ApiResult.success("已取消报名");
    }

    @Operation(summary = "我的报名", description = "查看当前用户的所有有效报名记录")
    @GetMapping("/mine")
    public ApiResult<IPage<ActivitySignupVO>> mySignups(PageQuery query) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        return ApiResult.success(activitySignupService.mySignups(ownerId, query));
    }
}