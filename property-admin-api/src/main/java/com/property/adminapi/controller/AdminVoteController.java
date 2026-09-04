package com.property.adminapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.common.result.ApiResult;
import com.property.framework.web.annotation.OperationLog;
import com.property.module.community.dto.request.VoteCreateRequest;
import com.property.module.community.dto.request.VoteQuery;
import com.property.module.community.dto.respose.VoteDetailVO;
import com.property.module.community.dto.respose.VoteVO;
import com.property.module.community.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "社区投票管理", description = "社区投票的创建与开始/结束管理")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/admin/community/vote")
@RequiredArgsConstructor
public class AdminVoteController {

    private final VoteService voteService;

    @Operation(summary = "投票分页查询", description = "支持标题、状态过滤")
    @GetMapping("/page")
    public ApiResult<IPage<VoteVO>> page(VoteQuery query) {
        return ApiResult.success(voteService.adminPage(query));
    }

    @Operation(summary = "投票详情", description = "含选项实时票数")
    @GetMapping("/{id}")
    public ApiResult<VoteDetailVO> getDetail(@PathVariable Long id) {
        return ApiResult.success(voteService.getDetail(id));
    }

    @OperationLog(module = "社区投票", action = "创建投票")
    @Operation(summary = "创建投票", description = "创建投票并保存选项")
    @PostMapping
    public ApiResult<?> create(@Valid @RequestBody VoteCreateRequest request) {
        voteService.createVote(request);
        return ApiResult.success("创建成功");
    }

    @OperationLog(module = "社区投票", action = "开始投票")
    @Operation(summary = "开始投票", description = "未开始 → 进行中")
    @PostMapping("/{id}/start")
    public ApiResult<?> start(@PathVariable Long id) {
        voteService.start(id);
        return ApiResult.success("已开始");
    }

    @OperationLog(module = "社区投票", action = "结束投票")
    @Operation(summary = "结束投票", description = "进行中 → 已结束")
    @PostMapping("/{id}/end")
    public ApiResult<?> end(@PathVariable Long id) {
        voteService.end(id);
        return ApiResult.success("已结束");
    }
}
