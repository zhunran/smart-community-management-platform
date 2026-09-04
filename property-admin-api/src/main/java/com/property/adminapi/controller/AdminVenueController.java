package com.property.adminapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.common.dto.PageQuery;
import com.property.common.result.ApiResult;
import com.property.framework.web.annotation.OperationLog;
import com.property.module.lifeservice.dto.venue.request.VenueCreateRequest;
import com.property.module.lifeservice.dto.venue.request.VenueQuery;
import com.property.module.lifeservice.dto.venue.request.VenueUpdateRequest;
import com.property.module.lifeservice.dto.venue.response.VenueBookingVO;
import com.property.module.lifeservice.dto.venue.response.VenueVO;
import com.property.module.lifeservice.service.VenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "场地管理", description = "场地的增删改查")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/admin/service/venue")
@RequiredArgsConstructor
public class AdminVenueController {

    private final VenueService venueService;

    @Operation(summary = "场地分页查询", description = "支持名称、类型、状态过滤")
    @GetMapping("/page")
    public ApiResult<IPage<VenueVO>> page(VenueQuery query) {
        return ApiResult.success(venueService.adminPage(query));
    }

    @Operation(summary = "场地详情")
    @GetMapping("/{id}")
    public ApiResult<VenueVO> getDetail(@PathVariable Long id) {
        return ApiResult.success(venueService.getDetail(id));
    }

    @OperationLog(module = "场地管理", action = "新增场地")
    @Operation(summary = "新增场地")
    @PostMapping
    public ApiResult<?> create(@Valid @RequestBody VenueCreateRequest request) {
        venueService.createVenue(request);
        return ApiResult.success("创建成功");
    }

    @OperationLog(module = "场地管理", action = "修改场地")
    @Operation(summary = "修改场地")
    @PutMapping
    public ApiResult<?> update(@Valid @RequestBody VenueUpdateRequest request) {
        venueService.updateVenue(request);
        return ApiResult.success("修改成功");
    }

    @OperationLog(module = "场地管理", action = "删除场地")
    @Operation(summary = "删除场地")
    @DeleteMapping("/{id}")
    public ApiResult<?> delete(@PathVariable Long id) {
        venueService.deleteVenue(id);
        return ApiResult.success("删除成功");
    }

    @Operation(summary = "场地预约记录", description = "可按场地筛选")
    @GetMapping("/booking/page")
    public ApiResult<IPage<VenueBookingVO>> bookingPage(@RequestParam(required = false) Long venueId,
                                                        PageQuery query) {
        return ApiResult.success(venueService.adminBookings(venueId, query));
    }
}
