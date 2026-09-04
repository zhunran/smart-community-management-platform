package com.property.module.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.property.common.enums.ActivityStatusEnum;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.module.community.dto.request.CommunityActivityCreateRequest;
import com.property.module.community.dto.request.CommunityActivityQuery;
import com.property.module.community.dto.request.CommunityActivityUpdateRequest;
import com.property.module.community.dto.respose.CommunityActivityDetailVO;
import com.property.module.community.dto.respose.CommunityActivityVO;
import com.property.module.community.entity.ActivitySignupEntity;
import com.property.module.community.entity.CommunityActivityEntity;
import com.property.module.community.repository.CommunityActivityMapper;
import com.property.module.community.service.CommunityActivityService;
import com.property.module.community.service.impl.converter.ActivityConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityActivityServiceImpl extends ServiceImpl<CommunityActivityMapper, CommunityActivityEntity>
        implements CommunityActivityService {

    private static final List<Integer> OWNER_VISIBLE_STATUSES = Arrays.asList(
            ActivityStatusEnum.RECRUITING.getValue(),
            ActivityStatusEnum.FULL.getValue(),
            ActivityStatusEnum.IN_PROGRESS.getValue(),
            ActivityStatusEnum.FINISHED.getValue()
    );

    private final ActivityConverter activityConverter;
    private final ActivitySignupServiceImpl activitySignupService;

    @Override
    public IPage<CommunityActivityVO> adminPage(CommunityActivityQuery query) {
        LambdaQueryWrapper<CommunityActivityEntity> wrapper = buildQueryWrapper(query);
        Page<CommunityActivityEntity> page = new Page<>(query.getCurrent(), query.getSize());
        IPage<CommunityActivityEntity> entityPage = this.page(page, wrapper);
        return entityPage.convert(activityConverter::toVO);
    }

    @Override
    public CommunityActivityDetailVO getDetail(Long id) {
        CommunityActivityEntity entity = getByIdOrThrow(id);
        return activityConverter.toDetailVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(CommunityActivityCreateRequest request) {
        CommunityActivityEntity entity = activityConverter.toEntity(request);
        entity.setStatus(ActivityStatusEnum.DRAFT.getValue());
        this.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(CommunityActivityCreateRequest request) {
        CommunityActivityEntity entity = activityConverter.toEntity(request);
        entity.setStatus(ActivityStatusEnum.RECRUITING.getValue());
        this.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommunityActivityVO updateActivity(CommunityActivityUpdateRequest request) {
        CommunityActivityEntity entity = getByIdOrThrow(request.getId());
        ActivityStatusEnum currentStatus = ActivityStatusEnum.fromValue(entity.getStatus());
        if (currentStatus == null || !currentStatus.canEdit()) {
            throw new BusinessException(ErrorCode.STATUS_ERROR,
                    "当前状态不允许修改 [status=" + entity.getStatus() + "]");
        }
        activityConverter.updateEntity(request, entity);
        this.updateById(entity);
        return activityConverter.toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CommunityActivityEntity entity = getByIdOrThrow(id);
        ActivityStatusEnum currentStatus = ActivityStatusEnum.fromValue(entity.getStatus());
        if (currentStatus == null || !currentStatus.canDelete()) {
            throw new BusinessException(ErrorCode.STATUS_ERROR,
                    "仅草稿状态可删除 [status=" + entity.getStatus() + "]");
        }
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishActivity(Long id) {
        CommunityActivityEntity entity = getByIdOrThrow(id);
        ActivityStatusEnum currentStatus = ActivityStatusEnum.fromValue(entity.getStatus());
        if (currentStatus != ActivityStatusEnum.DRAFT) {
            throw new BusinessException(ErrorCode.STATUS_ERROR,
                    "仅草稿状态可发布 [status=" + entity.getStatus() + "]");
        }
        entity.setStatus(ActivityStatusEnum.RECRUITING.getValue());
        this.updateById(entity);
        log.info("活动发布成功 [id={}]", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelActivity(Long id) {
        CommunityActivityEntity entity = getByIdOrThrow(id);
        ActivityStatusEnum currentStatus = ActivityStatusEnum.fromValue(entity.getStatus());
        if (currentStatus == null || !currentStatus.canCancel()) {
            throw new BusinessException(ErrorCode.STATUS_ERROR,
                    "仅草稿/招募中状态可取消 [status=" + entity.getStatus() + "]");
        }
        // 校验活动尚未开始
        if (entity.getStartTime() != null && entity.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "活动已开始，无法取消");
        }
        entity.setStatus(ActivityStatusEnum.CANCELED.getValue());
        this.updateById(entity);
        log.info("活动已取消 [id={}]", id);
        // TODO: 通知已报名业主（邮件）
    }

    @Override
    public IPage<CommunityActivityVO> ownerPage(CommunityActivityQuery query) {
        LambdaQueryWrapper<CommunityActivityEntity> wrapper = buildQueryWrapper(query);
        wrapper.in(CommunityActivityEntity::getStatus, OWNER_VISIBLE_STATUSES);
        Page<CommunityActivityEntity> page = new Page<>(query.getCurrent(), query.getSize());
        IPage<CommunityActivityEntity> entityPage = this.page(page, wrapper);
        return entityPage.convert(activityConverter::toVO);
    }

    @Override
    public CommunityActivityDetailVO ownerGetDetail(Long id, Long ownerId) {
        CommunityActivityEntity entity = getByIdOrThrow(id);
        CommunityActivityDetailVO vo = activityConverter.toDetailVO(entity);
        // 判断当前业主是否已报名（排除已取消）
        boolean signedUp = activitySignupService.lambdaQuery()
                .eq(ActivitySignupEntity::getActivityId, id)
                .eq(ActivitySignupEntity::getOwnerId, ownerId)
                .ne(ActivitySignupEntity::getStatus, 2) // 排除已取消
                .exists();
        vo.setIsSignedUp(signedUp);
        return vo;
    }

    private LambdaQueryWrapper<CommunityActivityEntity> buildQueryWrapper(CommunityActivityQuery query) {
        return new LambdaQueryWrapper<CommunityActivityEntity>()
                .eq(query.getActivityType() != null, CommunityActivityEntity::getActivityType, query.getActivityType())
                .eq(query.getStatus() != null, CommunityActivityEntity::getStatus, query.getStatus())
                .between(query.getStartTimeBegin() != null && query.getStartTimeEnd() != null,
                        CommunityActivityEntity::getStartTime, query.getStartTimeBegin(), query.getStartTimeEnd())
                .between(query.getEndTimeBegin() != null && query.getEndTimeEnd() != null,
                        CommunityActivityEntity::getEndTime, query.getEndTimeBegin(), query.getEndTimeEnd())
                .like(query.getTitle() != null, CommunityActivityEntity::getTitle, query.getTitle())
                .like(query.getLocation() != null, CommunityActivityEntity::getLocation, query.getLocation())
                .orderByAsc(CommunityActivityEntity::getStartTime);
    }

    private CommunityActivityEntity getByIdOrThrow(Long id) {
        CommunityActivityEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "活动不存在");
        }
        return entity;
    }
}