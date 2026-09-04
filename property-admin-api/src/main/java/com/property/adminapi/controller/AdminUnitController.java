package com.property.adminapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.adminapi.dto.request.UnitCreateRequest;
import com.property.adminapi.dto.request.UnitPageQuery;
import com.property.adminapi.dto.request.UnitUpdateRequest;
import com.property.adminapi.dto.response.UnitVO;
import com.property.adminapi.service.UnitService;
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
 * 管理员端：单元管理
 */
@Tag(name = "单元管理", description = "单元的增删改查")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/admin/unit")
@RequiredArgsConstructor
public class AdminUnitController {

    private final UnitService unitService;

    @Operation(summary = "单元分页查询", description = "支持楼栋ID、单元编号、名称、状态过滤")
    @GetMapping("/page")
    public ApiResult<IPage<UnitVO>> page(UnitPageQuery query) {
        return ApiResult.success(unitService.page(query));
    }

    @Operation(summary = "单元详情")
    @GetMapping("/{id}")
    public ApiResult<UnitVO> getDetail(@PathVariable Long id) {
        return ApiResult.success(unitService.getDetail(id));
    }

    @Operation(summary = "按楼栋查询单元列表", description = "返回某楼栋下所有启用单元，用于下拉选择框")
    @GetMapping("/list-by-building")
    public ApiResult<List<UnitVO>> listByBuildingId(@RequestParam Long buildingId) {
        return ApiResult.success(unitService.listByBuildingId(buildingId));
    }

    @OperationLog(module = "单元管理", action = "新增单元")
    @Operation(summary = "新增单元")
    @PostMapping
    public ApiResult<Long> create(@Valid @RequestBody UnitCreateRequest request) {
        return ApiResult.success("新增成功", unitService.create(request));
    }

    @OperationLog(module = "单元管理", action = "修改单元")
    @Operation(summary = "修改单元")
    @PutMapping
    public ApiResult<?> update(@Valid @RequestBody UnitUpdateRequest request) {
        unitService.update(request);
        return ApiResult.success("修改成功");
    }

    @OperationLog(module = "单元管理", action = "删除单元")
    @Operation(summary = "删除单元", description = "逻辑删除，单元下有房屋则禁止删除")
    @DeleteMapping("/{id}")
    public ApiResult<?> delete(@PathVariable Long id) {
        unitService.delete(id);
        return ApiResult.success("删除成功");
    }
}
