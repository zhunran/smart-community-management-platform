package com.property.adminapi.controller;

import com.property.common.result.ApiResult;
import com.property.module.parking.dto.request.ParkingBindRequest;
import com.property.module.parking.dto.request.ParkingChangeRequest;
import com.property.module.parking.dto.response.ParkingSpaceVO;
import com.property.module.parking.service.ParkingSpaceService;
import com.property.framework.web.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员端：车位管理
 */
@Tag(name = "车位管理", description = "车位信息、绑定/变更/退租")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/admin/parking")
@RequiredArgsConstructor
public class AdminParkingController {

    private final ParkingSpaceService parkingSpaceService;

    @Operation(summary = "车位列表", description = "所有车位，按楼层/区域/编号排序")
    @GetMapping("/list")
    public ApiResult<List<ParkingSpaceVO>> list() {
        return ApiResult.success(parkingSpaceService.listAll());
    }

    @Operation(summary = "车位详情")
    @GetMapping("/{id}")
    public ApiResult<ParkingSpaceVO> getDetail(@PathVariable Long id) {
        return ApiResult.success(parkingSpaceService.getDetail(id));
    }

    @OperationLog(module = "车位管理", action = "绑定车位")
    @Operation(summary = "绑定车位", description = "将空闲车位绑定给业主（自有→已售，租赁→已租）")
    @PostMapping("/bind")
    public ApiResult<ParkingSpaceVO> bind(@Valid @RequestBody ParkingBindRequest request) {
        return ApiResult.success("车位绑定成功", parkingSpaceService.bind(request));
    }

    @OperationLog(module = "车位管理", action = "变更车位绑定")
    @Operation(summary = "变更绑定", description = "变更车位的绑定业主（含乐观锁防并发）")
    @PutMapping("/change")
    public ApiResult<ParkingSpaceVO> change(@Valid @RequestBody ParkingChangeRequest request) {
        return ApiResult.success("车位变更成功", parkingSpaceService.change(request));
    }

    @OperationLog(module = "车位管理", action = "退租解绑车位")
    @Operation(summary = "退租解绑", description = "解绑车位，恢复空闲状态（含乐观锁防并发）")
    @PostMapping("/unbind/{id}")
    public ApiResult<ParkingSpaceVO> unbind(@PathVariable Long id, @RequestParam(required = false) String remark) {
        return ApiResult.success("车位退租成功", parkingSpaceService.unbind(id, remark));
    }
}
