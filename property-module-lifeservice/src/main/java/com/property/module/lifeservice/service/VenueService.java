package com.property.module.lifeservice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.property.common.dto.PageQuery;
import com.property.module.lifeservice.dto.venue.request.VenueBookingRequest;
import com.property.module.lifeservice.dto.venue.request.VenueCreateRequest;
import com.property.module.lifeservice.dto.venue.request.VenueQuery;
import com.property.module.lifeservice.dto.venue.request.VenueUpdateRequest;
import com.property.module.lifeservice.dto.venue.response.VenueBookingVO;
import com.property.module.lifeservice.dto.venue.response.VenueSlotVO;
import com.property.module.lifeservice.dto.venue.response.VenueVO;
import com.property.module.lifeservice.entity.VenueEntity;

import java.time.LocalDate;
import java.util.List;

public interface VenueService extends IService<VenueEntity> {

    /** 管理端创建场地 */
    void createVenue(VenueCreateRequest request);

    /** 管理端修改场地 */
    void updateVenue(VenueUpdateRequest request);

    /** 管理端删除场地（逻辑删除） */
    void deleteVenue(Long id);

    /** 管理端场地分页 */
    IPage<VenueVO> adminPage(VenueQuery query);

    /** 场地详情 */
    VenueVO getDetail(Long id);

    /** 业主端场地列表（仅启用） */
    List<VenueVO> ownerList(VenueQuery query);

    /** 业主端查询场地某日已占用时段 */
    VenueSlotVO getSlots(Long venueId, LocalDate date);

    /** 业主端预约场地 */
    VenueBookingVO book(Long venueId, VenueBookingRequest request, Long ownerId);

    /** 业主端取消预约（开始前2小时） */
    void cancelBooking(Long bookingId, Long ownerId);

    /** 业主端我的预约 */
    IPage<VenueBookingVO> myBookings(PageQuery query, Long ownerId);

    /** 管理端场地预约记录（可按场地筛选） */
    IPage<VenueBookingVO> adminBookings(Long venueId, PageQuery query);
}
