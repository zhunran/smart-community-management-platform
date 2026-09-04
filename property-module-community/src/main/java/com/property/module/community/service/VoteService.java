package com.property.module.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.property.module.community.dto.request.VoteCastRequest;
import com.property.module.community.dto.request.VoteCreateRequest;
import com.property.module.community.dto.request.VoteQuery;
import com.property.module.community.dto.respose.VoteDetailVO;
import com.property.module.community.dto.respose.VoteResultVO;
import com.property.module.community.dto.respose.VoteVO;
import com.property.module.community.entity.CommunityVoteEntity;

public interface VoteService extends IService<CommunityVoteEntity> {

    /** 管理端创建投票（含选项） */
    void createVote(VoteCreateRequest request);

    /** 管理端分页查询 */
    IPage<VoteVO> adminPage(VoteQuery query);

    /** 管理端/业主端详情（含选项实时票数） */
    VoteDetailVO getDetail(Long id);

    /** 管理端开始投票：未开始 → 进行中 */
    void start(Long id);

    /** 管理端结束投票：进行中 → 已结束 */
    void end(Long id);

    /** 业主端投票列表（进行中/已结束） */
    IPage<VoteVO> ownerPage(VoteQuery query);

    /** 业主端详情（含我是否已投） */
    VoteDetailVO ownerGetDetail(Long id, Long ownerId);

    /** 业主端投票 */
    void castVote(Long voteId, VoteCastRequest request, Long ownerId);

    /** 投票结果（匿名只返回计数，实名返回明细） */
    VoteResultVO getResult(Long id);
}
