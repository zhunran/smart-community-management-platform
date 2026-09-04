package com.property.module.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.property.common.dto.PageQuery;
import com.property.module.community.dto.respose.ActivitySignupVO;
import com.property.module.community.entity.ActivitySignupEntity;

public interface ActivitySignupService extends IService<ActivitySignupEntity> {

    /** 报名（乐观锁并发控制） */
    void signup(Long activityId, Long ownerId);

    /** 取消报名 */
    void cancelSignup(Long activityId, Long ownerId);

    /** 我的报名记录 */
    IPage<ActivitySignupVO> mySignups(Long ownerId, PageQuery query);
}