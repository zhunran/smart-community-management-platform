package com.property.ownerapi.controller;

import com.property.common.result.ApiResult;
import com.property.framework.web.security.SecurityUtil;
import com.property.module.owner.dto.request.OwnerProfileUpdateRequest;
import com.property.module.owner.dto.response.OwnerDetailVO;
import com.property.module.owner.dto.response.OwnerRoomVO;
import com.property.module.owner.service.OwnerRoomService;
import com.property.module.owner.service.OwnerService;
import com.property.ownerapi.service.OwnerRoomInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 业主端：个人信息 + 我的房屋
 */
@Tag(name = "业主信息", description = "个人信息与房屋查询")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/owner/profile")
@RequiredArgsConstructor
public class OwnerProfileController {

    private final OwnerService ownerService;
    private final OwnerRoomService ownerRoomService;
    private final OwnerRoomInfoService ownerRoomInfoService;

    @Operation(summary = "会话校验", description = "校验当前登录会话是否有效（Token 有效且业主存在）")
    @GetMapping("/session")
    public ApiResult<Void> checkSession() {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        // 业主不存在时 getDetail 会抛出业务异常（code=1006），以此判定会话失效
        ownerService.getDetail(ownerId);
        return ApiResult.success("ok", null);
    }

    @Operation(summary = "个人信息", description = "获取当前登录业主的详细信息")
    @GetMapping
    public ApiResult<OwnerDetailVO> getProfile() {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        return ApiResult.success(ownerService.getDetail(ownerId));
    }
    @Operation(summary = "更新个人信息", description = "更改当前登录业主的详细信息")
    @PostMapping("/owner")
    public ApiResult<?> updateOwnerProfile(@Valid @RequestBody OwnerProfileUpdateRequest request) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        ownerService.updateProfile(ownerId, request);
        return ApiResult.success("更新成功");
    }

    @Operation(summary = "我的房屋", description = "获取当前登录业主名下的所有房屋列表，含房屋编码、楼栋名称、单元名称")
    @GetMapping("/rooms")
    public ApiResult<List<OwnerRoomVO>> getMyRooms() {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        List<OwnerRoomVO> rooms = ownerRoomService.listByOwnerId(ownerId);
        ownerRoomInfoService.fillRoomInfo(rooms);
        return ApiResult.success(rooms);
    }
}
