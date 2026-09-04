package com.property.ownerapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.common.result.ApiResult;
import com.property.framework.web.security.SecurityUtil;
import com.property.module.community.dto.request.VoteCastRequest;
import com.property.module.community.dto.request.VoteQuery;
import com.property.module.community.dto.respose.VoteDetailVO;
import com.property.module.community.dto.respose.VoteResultVO;
import com.property.module.community.dto.respose.VoteVO;
import com.property.module.community.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "社区投票", description = "业主端社区投票与结果查看")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/owner/community/vote")
@RequiredArgsConstructor
public class OwnerVoteController {

    private final VoteService voteService;

    @Operation(summary = "投票列表", description = "展示进行中/已结束的投票")
    @GetMapping("/page")
    public ApiResult<IPage<VoteVO>> page(VoteQuery query) {
        return ApiResult.success(voteService.ownerPage(query));
    }

    @Operation(summary = "投票详情", description = "含选项与当前用户已投选项")
    @GetMapping("/{id}")
    public ApiResult<VoteDetailVO> getDetail(@PathVariable Long id) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        return ApiResult.success(voteService.ownerGetDetail(id, ownerId));
    }

    @Operation(summary = "投票", description = "单选/多选，唯一索引防重复投票")
    @PostMapping("/{id}/cast")
    public ApiResult<?> cast(@PathVariable Long id, @Valid @RequestBody VoteCastRequest request) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        voteService.castVote(id, request, ownerId);
        return ApiResult.success("投票成功");
    }

    @Operation(summary = "投票结果", description = "匿名只返回计数，实名返回明细")
    @GetMapping("/{id}/result")
    public ApiResult<VoteResultVO> result(@PathVariable Long id) {
        return ApiResult.success(voteService.getResult(id));
    }
}
