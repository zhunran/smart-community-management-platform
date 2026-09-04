package com.property.adminapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.adminapi.dto.request.RoomCreateRequest;
import com.property.adminapi.dto.request.RoomPageQuery;
import com.property.adminapi.dto.request.RoomUpdateRequest;
import com.property.adminapi.dto.response.RoomVO;
import com.property.adminapi.service.RoomService;
import com.property.common.result.ApiResult;
import com.property.framework.web.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员端：房屋管理
 */
@Tag(name = "房屋管理", description = "房屋的增删改查")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/admin/room")
@RequiredArgsConstructor
public class AdminRoomController {

    private final RoomService roomService;

    @Operation(summary = "房屋分页查询", description = "支持楼栋ID、单元ID、房号、楼层、类型、入住状态过滤")
    @GetMapping("/page")
    public ApiResult<IPage<RoomVO>> page(RoomPageQuery query) {
        return ApiResult.success(roomService.page(query));
    }

    @Operation(summary = "房屋详情")
    @GetMapping("/{id}")
    public ApiResult<RoomVO> getDetail(@PathVariable Long id) {
        return ApiResult.success(roomService.getDetail(id));
    }

    @Operation(summary = "按单元查询房屋列表", description = "返回某单元下所有房屋，用于下拉选择框")
    @GetMapping("/list-by-unit")
    public ApiResult<List<RoomVO>> listByUnitId(@RequestParam Long unitId) {
        return ApiResult.success(roomService.listByUnitId(unitId));
    }

    @OperationLog(module = "房屋管理", action = "新增房屋")
    @Operation(summary = "新增房屋")
    @PostMapping
    public ApiResult<Long> create(@Valid @RequestBody RoomCreateRequest request) {
        return ApiResult.success("新增成功", roomService.create(request));
    }

    @OperationLog(module = "房屋管理", action = "修改房屋")
    @Operation(summary = "修改房屋")
    @PutMapping
    public ApiResult<?> update(@Valid @RequestBody RoomUpdateRequest request) {
        roomService.update(request);
        return ApiResult.success("修改成功");
    }

    @OperationLog(module = "房屋管理", action = "删除房屋")
    @Operation(summary = "删除房屋", description = "逻辑删除")
    @DeleteMapping("/{id}")
    public ApiResult<?> delete(@PathVariable Long id) {
        roomService.delete(id);
        return ApiResult.success("删除成功");
    }
}
