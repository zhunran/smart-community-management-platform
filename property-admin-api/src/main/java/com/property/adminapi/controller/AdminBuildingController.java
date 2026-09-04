package com.property.adminapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.adminapi.dto.request.BuildingCreateRequest;
import com.property.adminapi.dto.request.BuildingPageQuery;
import com.property.adminapi.dto.request.BuildingUpdateRequest;
import com.property.adminapi.dto.response.BuildingVO;
import com.property.adminapi.service.BuildingService;
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
 * 管理员端：楼栋管理
 */
@Tag(name = "楼栋管理", description = "楼栋的增删改查")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/admin/building")
@RequiredArgsConstructor
public class AdminBuildingController {

    private final BuildingService buildingService;

    @Operation(summary = "楼栋分页查询", description = "支持楼栋编号、名称、状态过滤")
    @GetMapping("/page")
    public ApiResult<IPage<BuildingVO>> page(BuildingPageQuery query) {
        return ApiResult.success(buildingService.page(query));
    }

    @Operation(summary = "楼栋详情")
    @GetMapping("/{id}")
    public ApiResult<BuildingVO> getDetail(@PathVariable Long id) {
        return ApiResult.success(buildingService.getDetail(id));
    }

    @Operation(summary = "全部启用楼栋列表", description = "返回所有启用状态的楼栋，用于下拉选择框")
    @GetMapping("/list")
    public ApiResult<List<BuildingVO>> listAll() {
        return ApiResult.success(buildingService.listAll());
    }

    @OperationLog(module = "楼栋管理", action = "新增楼栋")
    @Operation(summary = "新增楼栋")
    @PostMapping
    public ApiResult<Long> create(@Valid @RequestBody BuildingCreateRequest request) {
        return ApiResult.success("新增成功", buildingService.create(request));
    }

    @OperationLog(module = "楼栋管理", action = "修改楼栋")
    @Operation(summary = "修改楼栋")
    @PutMapping
    public ApiResult<BuildingVO> update(@Valid @RequestBody BuildingUpdateRequest request) {
        return ApiResult.success("修改成功", buildingService.update(request));
    }

    @OperationLog(module = "楼栋管理", action = "删除楼栋")
    @Operation(summary = "删除楼栋", description = "逻辑删除，楼栋下有单元则禁止删除")
    @DeleteMapping("/{id}")
    public ApiResult<?> delete(@PathVariable Long id) {
        buildingService.delete(id);
        return ApiResult.success("删除成功");
    }
}
