package com.property.module.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.property.common.dto.PageQuery;
import com.property.module.community.dto.request.*;
import com.property.module.community.dto.respose.ForumCommentVO;
import com.property.module.community.dto.respose.ForumPostDetailVO;
import com.property.module.community.dto.respose.ForumPostVO;
import com.property.module.community.entity.ForumCommentEntity;
import com.property.module.community.entity.ForumPostEntity;

import java.util.List;

public interface ForumService extends IService<ForumPostEntity> {

    /** 业主端发帖 */
    void createPost(ForumPostCreateRequest request, Long ownerId);

    /** 业主端帖子列表（仅已发布，置顶优先） */
    IPage<ForumPostVO> ownerPage(ForumPostQuery query);

    /** 业主端我的帖子 */
    IPage<ForumPostVO> myPosts(ForumPostQuery query, Long ownerId);

    /** 帖子详情（浏览量+1，含评论和点赞状态） */
    ForumPostDetailVO getDetail(Long postId, Long ownerId);

    /** 发表评论 */
    void createComment(ForumCommentCreateRequest request, Long ownerId);

    /** 获取帖子评论列表（两级嵌套） */
    List<ForumCommentVO> getComments(Long postId);

    /** 点赞/取消点赞（toggle，幂等） */
    void like(ForumLikeRequest request, Long ownerId);

    /** 管理端：帖子分页 */
    IPage<ForumPostVO> adminPage(ForumPostQuery query);

    /** 管理端：审核帖子 */
    void audit(Long postId, ForumPostAuditRequest request);

    /** 管理端：置顶 toggle */
    void togglePin(Long postId);

    /** 管理端：加精 toggle */
    void toggleEssence(Long postId);

    /** 管理端：删除帖子 */
    void deletePost(Long postId);

    /** 管理端：评论分页 */
    IPage<ForumCommentVO> adminCommentPage(Long postId, PageQuery query);

    /** 管理端：删除评论 */
    void deleteComment(Long commentId);
}