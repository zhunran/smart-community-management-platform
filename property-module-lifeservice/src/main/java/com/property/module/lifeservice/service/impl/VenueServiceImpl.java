package com.property.module.lifeservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.property.common.dto.PageQuery;
import com.property.common.enums.VenueBookingStatusEnum;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.common.exception.ForbiddenException;
import com.property.module.lifeservice.dto.venue.request.VenueBookingRequest;
import com.property.module.lifeservice.dto.venue.request.VenueCreateRequest;
import com.property.module.lifeservice.dto.venue.request.VenueQuery;
import com.property.module.lifeservice.dto.venue.request.VenueUpdateRequest;
import com.property.module.lifeservice.dto.venue.response.VenueBookingVO;
import com.property.module.lifeservice.dto.venue.response.VenueOccupiedSlotVO;
import com.property.module.lifeservice.dto.venue.response.VenueSlotVO;
import com.property.module.lifeservice.dto.venue.response.VenueVO;
import com.property.module.lifeservice.entity.VenueBookingEntity;
import com.property.module.lifeservice.entity.VenueEntity;
import com.property.module.lifeservice.repository.VenueBookingMapper;
import com.property.module.lifeservice.repository.VenueMapper;
import com.property.module.lifeservice.service.VenueService;
import com.property.module.lifeservice.service.impl.converter.VenueConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VenueServiceImpl extends ServiceImpl<VenueMapper, VenueEntity>
        implements VenueService {

    private static final String[] VENUE_TYPE_NAMES = {"", "健身房", "棋牌室", "会议室", "游泳池", "其他"};
    private static final int VENUE_ENABLED = 1;

    private final VenueBookingMapper venueBookingMapper;
    private final VenueConverter venueConverter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createVenue(VenueCreateRequest request) {
        validateVenue(request.getName(), request.getVenueType(), request.getOpenTime(),
                request.getCloseTime(), request.getSlotMinutes());
        VenueEntity entity = venueConverter.toEntity(request);
        if (entity.getCapacity() == null) entity.setCapacity(1);
        if (entity.getMonthlyLimit() == null) entity.setMonthlyLimit(0);
        if (entity.getPrice() == null) entity.setPrice(java.math.BigDecimal.ZERO);
        if (entity.getStatus() == null) entity.setStatus(VENUE_ENABLED);
        this.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVenue(VenueUpdateRequest request) {
        VenueEntity entity = getByIdOrThrow(request.getId());
        if ((request.getOpenTime() != null || request.getCloseTime() != null || request.getSlotMinutes() != null)) {
            LocalTime open = request.getOpenTime() != null ? request.getOpenTime() : entity.getOpenTime();
            LocalTime close = request.getCloseTime() != null ? request.getCloseTime() : entity.getCloseTime();
            Integer slot = request.getSlotMinutes() != null ? request.getSlotMinutes() : entity.getSlotMinutes();
            validateVenue(entity.getName(), entity.getVenueType(), open, close, slot);
        }
        venueConverter.updateEntity(request, entity);
        this.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVenue(Long id) {
        getByIdOrThrow(id);
        this.removeById(id);
    }

    @Override
    public IPage<VenueVO> adminPage(VenueQuery query) {
        LambdaQueryWrapper<VenueEntity> wrapper = new LambdaQueryWrapper<VenueEntity>()
                .like(StringUtils.hasText(query.getName()), VenueEntity::getName, query.getName())
                .eq(query.getVenueType() != null, VenueEntity::getVenueType, query.getVenueType())
                .eq(query.getStatus() != null, VenueEntity::getStatus, query.getStatus())
                .orderByDesc(VenueEntity::getCreateTime);
        Page<VenueEntity> page = new Page<>(query.getCurrent(), query.getSize());
        IPage<VenueEntity> entityPage = this.page(page, wrapper);
        return entityPage.convert(e -> fillNames(venueConverter.toVO(e)));
    }

    @Override
    public VenueVO getDetail(Long id) {
        return fillNames(venueConverter.toVO(getByIdOrThrow(id)));
    }

    @Override
    public List<VenueVO> ownerList(VenueQuery query) {
        LambdaQueryWrapper<VenueEntity> wrapper = new LambdaQueryWrapper<VenueEntity>()
                .eq(VenueEntity::getStatus, VENUE_ENABLED)
                .like(StringUtils.hasText(query.getName()), VenueEntity::getName, query.getName())
                .eq(query.getVenueType() != null, VenueEntity::getVenueType, query.getVenueType())
                .orderByAsc(VenueEntity::getId);
        return this.list(wrapper).stream()
                .map(e -> fillNames(venueConverter.toVO(e)))
                .collect(Collectors.toList());
    }

    @Override
    public VenueSlotVO getSlots(Long venueId, LocalDate date) {
        VenueEntity venue = getByIdOrThrow(venueId);
        List<VenueBookingEntity> bookings = venueBookingMapper.selectList(
                new LambdaQueryWrapper<VenueBookingEntity>()
                        .eq(VenueBookingEntity::getVenueId, venueId)
                        .eq(VenueBookingEntity::getBookingDate, date)
                        .in(VenueBookingEntity::getStatus,
                                VenueBookingStatusEnum.BOOKED.getValue(),
                                VenueBookingStatusEnum.USED.getValue())
                        .orderByAsc(VenueBookingEntity::getStartTime));

        VenueSlotVO vo = new VenueSlotVO();
        vo.setVenueId(venue.getId());
        vo.setDate(date);
        vo.setOpenTime(venue.getOpenTime());
        vo.setCloseTime(venue.getCloseTime());
        vo.setSlotMinutes(venue.getSlotMinutes());
        vo.setOccupied(bookings.stream().map(b -> {
            VenueOccupiedSlotVO o = new VenueOccupiedSlotVO();
            o.setStartTime(b.getStartTime());
            o.setEndTime(b.getEndTime());
            return o;
        }).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VenueBookingVO book(Long venueId, VenueBookingRequest request, Long ownerId) {
        VenueEntity venue = getByIdOrThrow(venueId);
        if (venue.getStatus() == null || venue.getStatus() != VENUE_ENABLED) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "该场地已停用");
        }

        LocalDate bookingDate = request.getBookingDate();
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = request.getEndTime();
        if (bookingDate.isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能预约过去的日期");
        }

        checkOpenWindow(venue, startTime, endTime);
        checkSlotAligned(venue, startTime, endTime);
        checkMonthlyLimit(venue, ownerId, bookingDate);
        checkConflict(venueId, bookingDate, startTime, endTime);

        VenueBookingEntity booking = new VenueBookingEntity();
        booking.setVenueId(venueId);
        booking.setOwnerId(ownerId);
        booking.setBookingDate(bookingDate);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setStatus(VenueBookingStatusEnum.BOOKED.getValue());
        venueBookingMapper.insert(booking);

        log.info("业主预约场地 [venueId={}, ownerId={}, date={}, {}~{}]",
                venueId, ownerId, bookingDate, startTime, endTime);
        return fillBookingNames(venueConverter.toBookingVO(booking));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelBooking(Long bookingId, Long ownerId) {
        VenueBookingEntity booking = getBookingOrThrow(bookingId);
        checkBookingOwner(booking, ownerId);

        VenueBookingStatusEnum statusEnum = VenueBookingStatusEnum.fromValue(booking.getStatus());
        if (statusEnum != VenueBookingStatusEnum.BOOKED) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "仅已预约状态可取消");
        }

        LocalDateTime bookingStart = booking.getBookingDate().atTime(booking.getStartTime());
        if (LocalDateTime.now().isAfter(bookingStart.minusHours(2))) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "预约开始前2小时内不可取消");
        }

        booking.setStatus(VenueBookingStatusEnum.CANCELED.getValue());
        venueBookingMapper.updateById(booking);
        log.info("业主取消场地预约 [bookingId={}, ownerId={}]", bookingId, ownerId);
    }

    @Override
    public IPage<VenueBookingVO> myBookings(PageQuery query, Long ownerId) {
        Page<VenueBookingEntity> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<VenueBookingEntity> wrapper = new LambdaQueryWrapper<VenueBookingEntity>()
                .eq(VenueBookingEntity::getOwnerId, ownerId)
                .orderByDesc(VenueBookingEntity::getCreateTime);
        IPage<VenueBookingEntity> entityPage = venueBookingMapper.selectPage(page, wrapper);
        return entityPage.convert(e -> fillBookingNames(venueConverter.toBookingVO(e)));
    }

    @Override
    public IPage<VenueBookingVO> adminBookings(Long venueId, PageQuery query) {
        Page<VenueBookingEntity> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<VenueBookingEntity> wrapper = new LambdaQueryWrapper<VenueBookingEntity>()
                .eq(venueId != null, VenueBookingEntity::getVenueId, venueId)
                .orderByDesc(VenueBookingEntity::getCreateTime);
        IPage<VenueBookingEntity> entityPage = venueBookingMapper.selectPage(page, wrapper);
        return entityPage.convert(e -> fillBookingNames(venueConverter.toBookingVO(e)));
    }

    private void validateVenue(String name, Integer venueType, LocalTime openTime, LocalTime closeTime, Integer slotMinutes) {
        if (venueType == null || venueType < 1 || venueType > 5) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "场地类型仅支持1-5");
        }
        if (openTime != null && closeTime != null && !closeTime.isAfter(openTime)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "关闭时间必须晚于开放时间");
        }
        if (slotMinutes == null || slotMinutes <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "预约粒度必须大于0");
        }
    }

    private void checkOpenWindow(VenueEntity venue, LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束时间必须晚于开始时间");
        }
        if (venue.getOpenTime() != null && startTime.isBefore(venue.getOpenTime())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "预约时间早于场地开放时间");
        }
        if (venue.getCloseTime() != null && endTime.isAfter(venue.getCloseTime())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "预约时间晚于场地关闭时间");
        }
    }

    private void checkSlotAligned(VenueEntity venue, LocalTime startTime, LocalTime endTime) {
        long openMinutes = venue.getOpenTime() == null ? 0 : venue.getOpenTime().toSecondOfDay() / 60;
        long startMinutes = startTime.toSecondOfDay() / 60;
        long endMinutes = endTime.toSecondOfDay() / 60;
        int slot = venue.getSlotMinutes() == null ? 60 : venue.getSlotMinutes();
        if ((startMinutes - openMinutes) % slot != 0 || (endMinutes - openMinutes) % slot != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "预约时段需按场地粒度对齐");
        }
    }

    private void checkMonthlyLimit(VenueEntity venue, Long ownerId, LocalDate bookingDate) {
        if (venue.getMonthlyLimit() == null || venue.getMonthlyLimit() <= 0) {
            return;
        }
        LocalDate firstOfMonth = bookingDate.withDayOfMonth(1);
        LocalDate lastOfMonth = bookingDate.withDayOfMonth(bookingDate.lengthOfMonth());
        long count = venueBookingMapper.selectCount(
                new LambdaQueryWrapper<VenueBookingEntity>()
                        .eq(VenueBookingEntity::getVenueId, venue.getId())
                        .eq(VenueBookingEntity::getOwnerId, ownerId)
                        .between(VenueBookingEntity::getBookingDate, firstOfMonth, lastOfMonth)
                        .in(VenueBookingEntity::getStatus,
                                VenueBookingStatusEnum.BOOKED.getValue(),
                                VenueBookingStatusEnum.USED.getValue()));
        if (count >= venue.getMonthlyLimit()) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "本月该场地预约次数已达上限");
        }
    }

    private void checkConflict(Long venueId, LocalDate bookingDate, LocalTime startTime, LocalTime endTime) {
        // 冲突检测：已有预约与目标时段重叠即冲突（相邻不冲突）
        long conflict = venueBookingMapper.selectCount(
                new LambdaQueryWrapper<VenueBookingEntity>()
                        .eq(VenueBookingEntity::getVenueId, venueId)
                        .eq(VenueBookingEntity::getBookingDate, bookingDate)
                        .in(VenueBookingEntity::getStatus,
                                VenueBookingStatusEnum.BOOKED.getValue(),
                                VenueBookingStatusEnum.USED.getValue())
                        .gt(VenueBookingEntity::getEndTime, startTime)
                        .lt(VenueBookingEntity::getStartTime, endTime));
        if (conflict > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该时段已被预约");
        }
    }

    private VenueVO fillNames(VenueVO vo) {
        vo.setVenueTypeName(venueTypeName(vo.getVenueType()));
        vo.setStatusName(vo.getStatus() != null && vo.getStatus() == VENUE_ENABLED ? "启用" : "停用");
        return vo;
    }

    private VenueBookingVO fillBookingNames(VenueBookingVO vo) {
        vo.setStatusName(Optional.ofNullable(vo.getStatus())
                .map(VenueBookingStatusEnum::fromValue)
                .map(VenueBookingStatusEnum::getLabel)
                .orElse(null));
        if (vo.getVenueId() != null) {
            VenueEntity venue = this.getById(vo.getVenueId());
            if (venue != null) {
                vo.setVenueName(venue.getName());
            }
        }
        return vo;
    }

    private String venueTypeName(Integer type) {
        if (type != null && type >= 1 && type < VENUE_TYPE_NAMES.length) {
            return VENUE_TYPE_NAMES[type];
        }
        return null;
    }

    private void checkBookingOwner(VenueBookingEntity booking, Long ownerId) {
        if (!Objects.equals(booking.getOwnerId(), ownerId)) {
            throw new ForbiddenException("无权操作他人预约");
        }
    }

    private VenueEntity getByIdOrThrow(Long id) {
        VenueEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "场地不存在");
        }
        return entity;
    }

    private VenueBookingEntity getBookingOrThrow(Long id) {
        VenueBookingEntity entity = venueBookingMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "预约记录不存在");
        }
        return entity;
    }
}
