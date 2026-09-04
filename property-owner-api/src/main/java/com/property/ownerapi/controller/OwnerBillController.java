package com.property.ownerapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.property.common.result.ApiResult;
import com.property.framework.web.security.SecurityUtil;
import com.property.module.bill.dto.request.BillPageQuery;
import com.property.module.bill.dto.response.BillDetailVO;
import com.property.module.bill.dto.response.BillVO;
import com.property.module.bill.service.BillService;
import com.property.module.owner.service.OwnerRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 业主端：账单查询
 */
@Tag(name = "业主账单", description = "业主账单查看、分项明细")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/owner/bills")
@RequiredArgsConstructor
public class OwnerBillController {

    private final BillService billService;
    private final OwnerRoomService ownerRoomService;

    @Operation(summary = "我的账单（分页）", description = "获取当前业主名下所有房屋的账单列表，支持按账期、状态筛选")
    @GetMapping("/page")
    public ApiResult<IPage<BillVO>> page(BillPageQuery query) {
        Long ownerId = SecurityUtil.requireUser().getUserId();

        // 查询业主名下所有有效房屋ID
        List<Long> roomIds = ownerRoomService.getRoomIdsByOwnerId(ownerId);

        // 如果业主没有关联房屋，返回空结果
        if (roomIds.isEmpty()) {
            Page<BillVO> emptyPage = new Page<>(query.getCurrent(), query.getSize());
            emptyPage.setRecords(List.of());
            emptyPage.setTotal(0);
            return ApiResult.success(emptyPage);
        }

        query.setOwnerId(ownerId);
        return ApiResult.success(billService.page(query));
    }

    @Operation(summary = "账单明细", description = "根据账单ID查询账单详情，含各费用项的明细列表")
    @GetMapping("/{id}")
    public ApiResult<BillDetailVO> getDetail(@PathVariable Long id) {
        Long ownerId = SecurityUtil.requireUser().getUserId();
        return ApiResult.success(billService.getDetail(id, ownerId));
    }
}
