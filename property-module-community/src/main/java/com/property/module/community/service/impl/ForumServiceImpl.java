package com.property.module.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.property.common.dto.PageQuery;
import com.property.common.enums.CommentStatusEnum;
import com.property.common.enums.PostCategoryEnum;
import com.property.common.enums.PostStatusEnum;
import com.property.common.enums.TargetTypeEnum;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.framework.util.SensitiveWordFilter;
import com.property.module.community.dto.request.*;
import com.property.module.community.dto.respose.ForumCommentVO;
import com.property.module.community.dto.respose.ForumPostDetailVO;
import com.property.module.community.dto.respose.ForumPostVO;
import com.property.module.community.entity.ForumCommentEntity;
import com.property.module.community.entity.ForumLikeEntity;
import com.property.module.community.entity.ForumPostEntity;
import com.property.module.community.repository.ForumCommentMapper;
import com.property.module.community.repository.ForumLikeMapper;
import com.property.module.community.repository.ForumPostMapper;
import com.property.module.community.service.ForumService;
import com.property.module.community.service.impl.converter.ForumConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForumServiceImpl extends ServiceImpl<ForumPostMapper, ForumPostEntity>
        implements ForumService {

    private final ForumPostMapper forumPostMapper;
    private final ForumCommentMapper forumCommentMapper;
    private final ForumLikeMapper forumLikeMapper;
    private final ForumConverter forumConverter;
    private final SensitiveWordFilter sensitiveWordFilter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPost(ForumPostCreateRequest request, Long ownerId) {
        ForumPostEntity entity = forumConverter.toEntity(request);
        entity.setOwnerId(ownerId);

        // 敏感词过滤
        Set<String> allHits = new HashSet<>();
        allHits.addAll(sensitiveWordFilter.filter(request.getTitle()));
        allHits.addAll(sensitiveWordFilter.filter(request.getContent()));

        if (allHits.isEmpty()) {
            // 无敏感词，直接发布
            entity.setStatus(PostStatusEnum.PUBLISHED.getValue());
        } else {
            // 命中敏感词，进入待审核
            entity.setStatus(PostStatusEnum.PENDING_AUDIT.getValue());
            entity.setSensitiveWords(String.join(",", allHits));
            log.info("帖子上报敏感词，进入待审核 [ownerId={}, words={}]", ownerId, allHits);
        }

        this.save(entity);
    }

    @Override
    public IPage<ForumPostVO> ownerPage(ForumPostQuery query) {
        LambdaQueryWrapper<ForumPostEntity> wrapper = new LambdaQueryWrapper<ForumPostEntity>()
                .eq(ForumPostEntity::getStatus, PostStatusEnum.PUBLISHED.getValue())
                .eq(query.getCategory() != null, ForumPostEntity::getCategory, query.getCategory());
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(ForumPostEntity::getTitle, query.getKeyword())
                    .or().like(ForumPostEntity::getContent, query.getKeyword()));
        }
        wrapper.orderByDesc(ForumPostEntity::getIsPinned)
                .orderByDesc(ForumPostEntity::getCreateTime);
        Page<ForumPostEntity> page = new Page<>(query.getCurrent(), query.getSize());
        IPage<ForumPostEntity> entityPage = this.page(page, wrapper);
        return entityPage.convert(forumConverter::toVO);
    }

    @Override
    public IPage<ForumPostVO> myPosts(ForumPostQuery query, Long ownerId) {
        LambdaQueryWrapper<ForumPostEntity> wrapper = new LambdaQueryWrapper<ForumPostEntity>()
                .eq(ForumPostEntity::getOwnerId, ownerId)
                .eq(query.getCategory() != null, ForumPostEntity::getCategory, query.getCategory())
                .eq(query.getStatus() != null, ForumPostEntity::getStatus, query.getStatus())
                .ne(ForumPostEntity::getStatus, PostStatusEnum.DELETED.getValue())
                .orderByDesc(ForumPostEntity::getCreateTime);
        Page<ForumPostEntity> page = new Page<>(query.getCurrent(), query.getSize());
        IPage<ForumPostEntity> entityPage = this.page(page, wrapper);
        return entityPage.convert(forumConverter::toVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ForumPostDetailVO getDetail(Long postId, Long ownerId) {
        ForumPostEntity post = getByIdOrThrow(postId);

        // 浏览量+1
        post.setViewCount(post.getViewCount() + 1);
        this.updateById(post);

        ForumPostDetailVO vo = forumConverter.toDetailVO(post);

        // 填充评论列表
        List<ForumCommentVO> comments = getComments(postId);
        vo.setComments(comments);

        // 判断当前用户是否已点赞
        if (ownerId != null) {
            boolean liked = forumLikeMapper.selectCount(
                    new LambdaQueryWrapper<ForumLikeEntity>()
                            .eq(ForumLikeEntity::getTargetId, postId)
                            .eq(ForumLikeEntity::getTargetType, TargetTypeEnum.POST.getValue())
                            .eq(ForumLikeEntity::getOwnerId, ownerId)
            ) > 0;
            vo.setIsLiked(liked);
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createComment(ForumCommentCreateRequest request, Long ownerId) {
        // 校验帖子存在
        ForumPostEntity post = getByIdOrThrow(request.getPostId());
        if (!PostStatusEnum.PUBLISHED.getValue().equals(post.getStatus())) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "帖子不可评论");
        }

        ForumCommentEntity comment = new ForumCommentEntity();
        comment.setPostId(request.getPostId());
        comment.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        comment.setReplyTo(request.getReplyTo());
        comment.setOwnerId(ownerId);
        comment.setContent(request.getContent());
        comment.setLikeCount(0);

        // 敏感词过滤
        List<String> hits = sensitiveWordFilter.filter(request.getContent());
        comment.setStatus(hits.isEmpty() ? CommentStatusEnum.NORMAL.getValue()
                : CommentStatusEnum.PENDING_AUDIT.getValue());

        forumCommentMapper.insert(comment);

        // 评论数+1
        post.setCommentCount(post.getCommentCount() + 1);
        this.updateById(post);
    }

    @Override
    public List<ForumCommentVO> getComments(Long postId) {
        // 一次查出帖子全部正常评论
        List<ForumCommentEntity> allComments = forumCommentMapper.selectList(
                new LambdaQueryWrapper<ForumCommentEntity>()
                        .eq(ForumCommentEntity::getPostId, postId)
                        .eq(ForumCommentEntity::getStatus, CommentStatusEnum.NORMAL.getValue())
                        .orderByAsc(ForumCommentEntity::getCreateTime)
        );

        List<ForumCommentVO> voList = allComments.stream()
                .map(forumConverter::toCommentVO)
                .toList();

        // 两级组装：parentId=0 为一级，其余挂到对应一级下
        List<ForumCommentVO> roots = new ArrayList<>();
        Map<Long, List<ForumCommentVO>> childrenMap = new HashMap<>();

        for (ForumCommentVO vo : voList) {
            if (vo.getParentId() == null || vo.getParentId() == 0) {
                roots.add(vo);
            } else {
                childrenMap.computeIfAbsent(vo.getParentId(), k -> new ArrayList<>()).add(vo);
            }
        }
        for (ForumCommentVO root : roots) {
            root.setChildren(childrenMap.getOrDefault(root.getId(), Collections.emptyList()));
        }

        return roots;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void like(ForumLikeRequest request, Long ownerId) {
        // 查唯一索引（targetId + targetType + ownerId）
        ForumLikeEntity existing = forumLikeMapper.selectOne(
                new LambdaQueryWrapper<ForumLikeEntity>()
                        .eq(ForumLikeEntity::getTargetId, request.getTargetId())
                        .eq(ForumLikeEntity::getTargetType, request.getTargetType())
                        .eq(ForumLikeEntity::getOwnerId, ownerId)
        );

        if (existing != null) {
            // 已点赞 → 取消点赞（物理删除）
            forumLikeMapper.deleteById(existing.getId());
            if (TargetTypeEnum.POST.getValue().equals(request.getTargetType())) {
                decrPostLikeCount(request.getTargetId());
            } else {
                decrCommentLikeCount(request.getTargetId());
            }
        } else {
            // 未点赞 → 点赞
            ForumLikeEntity like = new ForumLikeEntity();
            like.setTargetId(request.getTargetId());
            like.setTargetType(request.getTargetType());
            like.setOwnerId(ownerId);
            forumLikeMapper.insert(like);
            if (TargetTypeEnum.POST.getValue().equals(request.getTargetType())) {
                incrPostLikeCount(request.getTargetId());
            } else {
                incrCommentLikeCount(request.getTargetId());
            }
        }
    }

    @Override
    public IPage<ForumPostVO> adminPage(ForumPostQuery query) {
        LambdaQueryWrapper<ForumPostEntity> wrapper = new LambdaQueryWrapper<ForumPostEntity>()
                .eq(query.getCategory() != null, ForumPostEntity::getCategory, query.getCategory())
                .eq(query.getStatus() != null, ForumPostEntity::getStatus, query.getStatus());
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(ForumPostEntity::getTitle, query.getKeyword())
                    .or().like(ForumPostEntity::getContent, query.getKeyword()));
        }
        wrapper.orderByDesc(ForumPostEntity::getCreateTime);
        Page<ForumPostEntity> page = new Page<>(query.getCurrent(), query.getSize());
        IPage<ForumPostEntity> entityPage = this.page(page, wrapper);
        return entityPage.convert(forumConverter::toVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long postId, ForumPostAuditRequest request) {
        ForumPostEntity post = getByIdOrThrow(postId);
        if (!PostStatusEnum.PENDING_AUDIT.getValue().equals(post.getStatus())) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "仅待审核状态可审核");
        }
        if (PostStatusEnum.PUBLISHED.getValue().equals(request.getStatus())) {
            post.setStatus(PostStatusEnum.PUBLISHED.getValue());
            post.setRejectReason(null);
            post.setSensitiveWords(null);
        } else if (PostStatusEnum.REJECTED.getValue().equals(request.getStatus())) {
            post.setStatus(PostStatusEnum.REJECTED.getValue());
            post.setRejectReason(request.getRejectReason());
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "审核结果仅支持：1-通过 2-驳回");
        }
        this.updateById(post);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void togglePin(Long postId) {
        ForumPostEntity post = getByIdOrThrow(postId);
        post.setIsPinned(post.getIsPinned() == 1 ? 0 : 1);
        this.updateById(post);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleEssence(Long postId) {
        ForumPostEntity post = getByIdOrThrow(postId);
        post.setIsEssence(post.getIsEssence() == 1 ? 0 : 1);
        this.updateById(post);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long postId) {
        ForumPostEntity post = getByIdOrThrow(postId);
        post.setStatus(PostStatusEnum.DELETED.getValue());
        this.updateById(post);
    }

    @Override
    public IPage<ForumCommentVO> adminCommentPage(Long postId, PageQuery query) {
        Page<ForumCommentEntity> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<ForumCommentEntity> wrapper = new LambdaQueryWrapper<ForumCommentEntity>()
                .eq(ForumCommentEntity::getPostId, postId)
                .orderByAsc(ForumCommentEntity::getCreateTime);
        IPage<ForumCommentEntity> entityPage = forumCommentMapper.selectPage(page, wrapper);
        return entityPage.convert(forumConverter::toCommentVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId) {
        ForumCommentEntity comment = forumCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "评论不存在");
        }
        comment.setStatus(CommentStatusEnum.DELETED.getValue());
        forumCommentMapper.updateById(comment);
    }

    private void incrPostLikeCount(Long postId) {
        ForumPostEntity post = forumPostMapper.selectById(postId);
        if (post != null) {
            post.setLikeCount(post.getLikeCount() + 1);
            forumPostMapper.updateById(post);
        }
    }

    private void decrPostLikeCount(Long postId) {
        ForumPostEntity post = forumPostMapper.selectById(postId);
        if (post != null && post.getLikeCount() > 0) {
            post.setLikeCount(post.getLikeCount() - 1);
            forumPostMapper.updateById(post);
        }
    }

    private void incrCommentLikeCount(Long commentId) {
        ForumCommentEntity comment = forumCommentMapper.selectById(commentId);
        if (comment != null) {
            comment.setLikeCount(comment.getLikeCount() + 1);
            forumCommentMapper.updateById(comment);
        }
    }

    private void decrCommentLikeCount(Long commentId) {
        ForumCommentEntity comment = forumCommentMapper.selectById(commentId);
        if (comment != null && comment.getLikeCount() > 0) {
            comment.setLikeCount(comment.getLikeCount() - 1);
            forumCommentMapper.updateById(comment);
        }
    }

    private ForumPostEntity getByIdOrThrow(Long id) {
        ForumPostEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "帖子不存在");
        }
        return entity;
    }
}