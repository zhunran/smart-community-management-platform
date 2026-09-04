package com.property.module.community.service.impl.converter;

import com.property.module.community.dto.request.ForumPostCreateRequest;
import com.property.module.community.dto.respose.ForumCommentVO;
import com.property.module.community.dto.respose.ForumPostDetailVO;
import com.property.module.community.dto.respose.ForumPostVO;
import com.property.module.community.entity.ForumCommentEntity;
import com.property.module.community.entity.ForumPostEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 论坛实体与 DTO 转换器（MapStruct）
 */
@Mapper(componentModel = "spring")
public interface ForumConverter {

    /**
     * CreateRequest → Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "viewCount", constant = "0")
    @Mapping(target = "likeCount", constant = "0")
    @Mapping(target = "commentCount", constant = "0")
    @Mapping(target = "isPinned", constant = "0")
    @Mapping(target = "isEssence", constant = "0")
    @Mapping(target = "rejectReason", ignore = true)
    @Mapping(target = "sensitiveWords", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "delFlag", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    ForumPostEntity toEntity(ForumPostCreateRequest request);

    /**
     * Entity → VO（列表用）
     */
    ForumPostVO toVO(ForumPostEntity entity);

    /**
     * Entity → DetailVO（详情用）
     */
    ForumPostDetailVO toDetailVO(ForumPostEntity entity);

    /**
     * CommentEntity → CommentVO
     */
    ForumCommentVO toCommentVO(ForumCommentEntity entity);
}