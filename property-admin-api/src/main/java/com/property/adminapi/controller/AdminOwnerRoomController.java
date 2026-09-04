package com.property.adminapi.controller;

import com.property.common.result.ApiResult;
import com.property.module.owner.dto.request.OwnerRoomBindRequest;
import com.property.module.owner.dto.response.OwnerRoomVO;
import com.property.module.owner.service.OwnerRoomService;
import com.property.framework.web.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员端：业主-房屋关联管理
 */
@Tag(name = "业主房屋关联", description = "业主与房屋的绑定/解绑管理")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/admin/owner-room")
@RequiredArgsConstructor
public class AdminOwnerRoomController {

    private final OwnerRoomService ownerRoomService;

    @OperationLog(module = "业主房屋", action = "绑定业主到房屋")
    @Operation(summary = "绑定业主到房屋")
    @PostMapping("/bind")
    public ApiResult<Long> bind(@Valid @RequestBody OwnerRoomBindRequest request) {
        return ApiResult.success("绑定成功", ownerRoomService.bind(request));
    }

    @OperationLog(module = "业主房屋", action = "解绑业主房屋")
    @Operation(summary = "解绑（逻辑删除）")
    @DeleteMapping("/{id}")
    public ApiResult<?> unbind(@PathVariable Long id) {
        ownerRoomService.unbind(id);
        return ApiResult.success("解绑成功");
    }

    @Operation(summary = "查询业主名下的房屋列表")
    @GetMapping("/list-by-owner")
    public ApiResult<List<OwnerRoomVO>> listByOwnerId(@RequestParam Long ownerId) {
        return ApiResult.success(ownerRoomService.listByOwnerId(ownerId));
    }

    @Operation(summary = "查询房屋关联的业主列表")
    @GetMapping("/list-by-room")
    public ApiResult<List<OwnerRoomVO>> listByRoomId(@RequestParam Long roomId) {
        return ApiResult.success(ownerRoomService.listByRoomId(roomId));
    }
}
