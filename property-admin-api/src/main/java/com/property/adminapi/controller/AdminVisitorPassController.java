package com.property.adminapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.common.result.ApiResult;
import com.property.framework.web.annotation.OperationLog;
import com.property.module.lifeservice.dto.visitor.request.VisitorPassQuery;
import com.property.module.lifeservice.dto.visitor.request.VisitorPassVerifyRequest;
import com.property.module.lifeservice.dto.visitor.response.VisitorPassVO;
import com.property.module.lifeservice.dto.visitor.response.VisitorPassVerifyVO;
import com.property.module.lifeservice.service.VisitorPassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "访客通行码管理", description = "管理端访客通行码列表与核销")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/admin/service/visitor-pass")
@RequiredArgsConstructor
public class AdminVisitorPassController {

    private final VisitorPassService visitorPassService;

    @Operation(summary = "通行码分页", description = "按状态筛选")
    @GetMapping("/page")
    public ApiResult<IPage<VisitorPassVO>> page(VisitorPassQuery query) {
        return ApiResult.success(visitorPassService.adminPage(query));
    }

    @OperationLog(module = "访客通行码", action = "核销通行码")
    @Operation(summary = "核销通行码", description = "校验状态/有效期/次数")
    @PostMapping("/verify")
    public ApiResult<VisitorPassVerifyVO> verify(@Valid @RequestBody VisitorPassVerifyRequest request) {
        return ApiResult.success(visitorPassService.verify(request));
    }
}
