package com.property.module.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.property.module.community.dto.request.CommunityActivityCreateRequest;
import com.property.module.community.dto.request.CommunityActivityQuery;
import com.property.module.community.dto.request.CommunityActivityUpdateRequest;
import com.property.module.community.dto.respose.CommunityActivityDetailVO;
import com.property.module.community.dto.respose.CommunityActivityVO;
import com.property.module.community.entity.CommunityActivityEntity;


public interface CommunityActivityService extends IService<CommunityActivityEntity> {

    /** 管理端分页查询 */
    IPage<CommunityActivityVO> adminPage(CommunityActivityQuery query);

    /** 管理端详情 */
    CommunityActivityDetailVO getDetail(Long id);

    /** 创建活动（草稿） */
    void create(CommunityActivityCreateRequest request);

    /** 创建并发布活动 */
    void publish(CommunityActivityCreateRequest request);

    /** 修改活动（仅草稿/招募中） */
    CommunityActivityVO updateActivity(CommunityActivityUpdateRequest request);

    /** 删除活动（仅草稿） */
    void delete(Long id);

    /** 发布活动：草稿 → 招募中 */
    void publishActivity(Long id);

    /** 取消活动：校验 startTime > now，取消后通知已报名业主 */
    void cancelActivity(Long id);

    /** 业主端分页查询（仅可见：招募中/已满员/进行中/已结束） */
    IPage<CommunityActivityVO> ownerPage(CommunityActivityQuery query);

    /** 业主端详情（含是否已报名） */
    CommunityActivityDetailVO ownerGetDetail(Long id, Long ownerId);
}