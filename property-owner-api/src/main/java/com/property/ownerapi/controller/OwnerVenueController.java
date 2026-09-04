package com.property.ownerapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.common.dto.PageQuery;
import com.property.common.result.ApiResult;
import com.property.framework.web.security.SecurityUtil;
import com.property.module.lifeservice.dto.venue.request.VenueBookingRequest;
import com.property.module.lifeservice.dto.venue.request.VenueQuery;
import com.property.module.lifeservice.dto.venue.response.VenueBookingVO;
import com.property.module.lifeservice.dto.venue.response.VenueSlotVO;
import com.property.module.lifeservice.dto.venue.response.VenueVO;
import com.property.module.lifeservice.service.VenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "场地预约", description = "业主端场地列表、时段查看、预约、取消")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/owner/service/venue")
@RequiredArgsConstructor
public class OwnerVenueController {

    private final VenueService venueService;

    @Operation(summary = "场地列表", description = "仅展示启用场地")
    @GetMapping("/list")
    public ApiResult<List<VenueVO>> list(VenueQuery query) {
        return ApiResult.success(venueService.ownerList(query));
    }

    @Operation(summary = "场地时段占用", description = "返回某日已占用的时间段")
    @GetMapping("/{id}/slots")
    public ApiResult<VenueSlotVO> slots(@PathVariable Long id,
                                        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResult.success(venueService.getSlots(id, date));
    }

    @Operation(summary = "预约场地", description = "校验开放时间/粒度/月度上限/冲突后插入")
    @PostMapping("/{id}/book")
    public ApiResult<VenueBookingVO> book(@PathVariable Long id,
                                          @Valid @RequestBody VenueBookingRequest request) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        return ApiResult.success(venueService.book(id, request, ownerId));
    }

    @Operation(summary = "取消预约", description = "开始前2小时可取消")
    @DeleteMapping("/booking/{id}")
    public ApiResult<?> cancel(@PathVariable Long id) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        venueService.cancelBooking(id, ownerId);
        return ApiResult.success("取消成功");
    }

    @Operation(summary = "我的预约")
    @GetMapping("/booking/mine")
    public ApiResult<IPage<VenueBookingVO>> mine(PageQuery query) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        return ApiResult.success(venueService.myBookings(query, ownerId));
    }
}
