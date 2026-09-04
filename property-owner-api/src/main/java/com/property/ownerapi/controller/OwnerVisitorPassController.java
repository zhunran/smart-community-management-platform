package com.property.ownerapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.common.dto.PageQuery;
import com.property.common.result.ApiResult;
import com.property.framework.web.security.SecurityUtil;
import com.property.module.lifeservice.dto.visitor.request.VisitorPassCreateRequest;
import com.property.module.lifeservice.dto.visitor.response.VisitorPassVO;
import com.property.module.lifeservice.service.VisitorPassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "访客通行码", description = "业主端生成、查看、撤销访客通行码")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/owner/service/visitor-pass")
@RequiredArgsConstructor
public class OwnerVisitorPassController {

    private final VisitorPassService visitorPassService;

    @Operation(summary = "生成访客通行码", description = "SecureRandom 6位数字码")
    @PostMapping
    public ApiResult<VisitorPassVO> create(@Valid @RequestBody VisitorPassCreateRequest request) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        return ApiResult.success(visitorPassService.create(request, ownerId));
    }

    @Operation(summary = "我的通行码")
    @GetMapping("/mine")
    public ApiResult<IPage<VisitorPassVO>> mine(PageQuery query) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        return ApiResult.success(visitorPassService.myPage(query, ownerId));
    }

    @Operation(summary = "撤销通行码")
    @DeleteMapping("/{id}")
    public ApiResult<?> revoke(@PathVariable Long id) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        visitorPassService.revoke(id, ownerId);
        return ApiResult.success("已撤销");
    }
}
