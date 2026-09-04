package com.property.module.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.property.common.dto.PageQuery;
import com.property.common.enums.ActivityStatusEnum;
import com.property.common.enums.SignupStatusEnum;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.module.community.dto.respose.ActivitySignupVO;
import com.property.module.community.entity.ActivitySignupEntity;
import com.property.module.community.entity.CommunityActivityEntity;
import com.property.module.community.repository.ActivitySignupMapper;
import com.property.module.community.repository.CommunityActivityMapper;
import com.property.module.community.service.ActivitySignupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivitySignupServiceImpl extends ServiceImpl<ActivitySignupMapper, ActivitySignupEntity>
        implements ActivitySignupService {

    private final CommunityActivityMapper communityActivityMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void signup(Long activityId, Long ownerId) {
        // 1. 校验活动存在
        CommunityActivityEntity activity = communityActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "活动不存在");
        }

        // 2. 校验活动状态可报名
        ActivityStatusEnum statusEnum = ActivityStatusEnum.fromValue(activity.getStatus());
        if (statusEnum == null || !statusEnum.canSignup()) {
            throw new BusinessException(ErrorCode.STATUS_ERROR,
                    "当前活动状态不允许报名 [status=" + activity.getStatus() + "]");
        }

        // 3. 查重（排除已取消的报名记录）
        boolean exists = this.lambdaQuery()
                .eq(ActivitySignupEntity::getActivityId, activityId)
                .eq(ActivitySignupEntity::getOwnerId, ownerId)
                .ne(ActivitySignupEntity::getStatus, SignupStatusEnum.CANCELED.getValue())
                .exists();
        if (exists) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "您已报名该活动");
        }

        // 4. 插入报名记录
        ActivitySignupEntity signup = new ActivitySignupEntity();
        signup.setActivityId(activityId);
        signup.setOwnerId(ownerId);
        signup.setParticipants(1);
        signup.setStatus(SignupStatusEnum.SIGNED_UP.getValue());
        signup.setSignupTime(LocalDateTime.now());
        this.save(signup);

        // 5. 乐观锁更新报名人数
        int updated = communityActivityMapper.incrSignupCount(activityId);
        if (updated <= 0) {
            // 报名人数已满，回滚报名记录
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "报名人数已满，报名失败");
        }
        
        // 6. 满员自动流转
        CommunityActivityEntity updatedActivity = communityActivityMapper.selectById(activityId);
        if (updatedActivity.getSignupCount() >= updatedActivity.getMaxParticipants()) {
            communityActivityMapper.updateStatus(activityId, ActivityStatusEnum.FULL.getValue());
            log.info("活动已满员 [id={}]", activityId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelSignup(Long activityId, Long ownerId) {
        // 1. 查找有效报名记录
        ActivitySignupEntity signup = this.lambdaQuery()
                .eq(ActivitySignupEntity::getActivityId, activityId)
                .eq(ActivitySignupEntity::getOwnerId, ownerId)
                .eq(ActivitySignupEntity::getStatus, SignupStatusEnum.SIGNED_UP.getValue())
                .one();
        if (signup == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "未找到有效报名记录");
        }

        // 2. 更新报名状态为已取消
        signup.setStatus(SignupStatusEnum.CANCELED.getValue());
        this.updateById(signup);

        // 3. 报名人数 -1
        communityActivityMapper.decrSignupCount(activityId);
    }

    @Override
    public IPage<ActivitySignupVO> mySignups(Long ownerId, PageQuery query) {
        LambdaQueryWrapper<ActivitySignupEntity> wrapper = new LambdaQueryWrapper<ActivitySignupEntity>()
                .eq(ActivitySignupEntity::getOwnerId, ownerId)
                .ne(ActivitySignupEntity::getStatus, SignupStatusEnum.CANCELED.getValue())
                .orderByDesc(ActivitySignupEntity::getSignupTime);
        Page<ActivitySignupEntity> page = new Page<>(query.getCurrent(), query.getSize());
        IPage<ActivitySignupEntity> entityPage = this.page(page, wrapper);
        return entityPage.convert(this::toVO);
    }

    private ActivitySignupVO toVO(ActivitySignupEntity entity) {
        ActivitySignupVO vo = new ActivitySignupVO();
        vo.setId(entity.getId());
        vo.setActivityId(entity.getActivityId());
        vo.setParticipants(entity.getParticipants());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(Optional.ofNullable(entity.getStatus())
                .map(SignupStatusEnum::fromValue)
                .map(SignupStatusEnum::getLabel)
                .orElse(null));
        vo.setSignupTime(entity.getSignupTime());
        vo.setCheckinTime(entity.getCheckinTime());

        // 填充关联活动信息
        CommunityActivityEntity activity = communityActivityMapper.selectById(entity.getActivityId());
        if (activity != null) {
            vo.setTitle(activity.getTitle());
            vo.setActivityStartTime(activity.getStartTime());
            vo.setActivityEndTime(activity.getEndTime());
            vo.setLocation(activity.getLocation());
        }
        return vo;
    }
}