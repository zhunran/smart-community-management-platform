package com.property.module.community.service.impl.converter;

import com.property.module.community.dto.request.VoteCreateRequest;
import com.property.module.community.dto.respose.VoteDetailVO;
import com.property.module.community.dto.respose.VoteOptionVO;
import com.property.module.community.dto.respose.VoteVO;
import com.property.module.community.entity.CommunityVoteEntity;
import com.property.module.community.entity.VoteOptionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 社区投票实体与 DTO 转换器（MapStruct）
 */
@Mapper(componentModel = "spring")
public interface VoteConverter {

    /**
     * CreateRequest → Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "delFlag", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    CommunityVoteEntity toEntity(VoteCreateRequest request);

    /**
     * Entity → VO（列表用）
     */
    @Mapping(target = "voteTypeName", ignore = true)
    @Mapping(target = "statusName", ignore = true)
    VoteVO toVO(CommunityVoteEntity entity);

    /**
     * Entity → DetailVO（详情用）
     */
    @Mapping(target = "voteTypeName", ignore = true)
    @Mapping(target = "statusName", ignore = true)
    @Mapping(target = "totalVotes", ignore = true)
    @Mapping(target = "options", ignore = true)
    @Mapping(target = "myVotedOptionIds", ignore = true)
    VoteDetailVO toDetailVO(CommunityVoteEntity entity);

    /**
     * VoteOptionEntity → VoteOptionVO
     */
    VoteOptionVO toOptionVO(VoteOptionEntity entity);
}
