package com.property.module.community.service.impl.converter;

import com.property.module.community.dto.request.CommunityActivityCreateRequest;
import com.property.module.community.dto.request.CommunityActivityUpdateRequest;
import com.property.module.community.dto.respose.CommunityActivityDetailVO;
import com.property.module.community.dto.respose.CommunityActivityVO;
import com.property.module.community.entity.CommunityActivityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * 社区活动实体与 DTO 转换器（MapStruct）
 */
@Mapper(componentModel = "spring")
public interface ActivityConverter {

    /**
     * CreateRequest → Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "signupCount", constant = "0")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "delFlag", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    CommunityActivityEntity toEntity(CommunityActivityCreateRequest request);

    /**
     * UpdateRequest → Entity（合并更新，null 字段不覆盖）
     */
    @Mapping(target = "signupCount", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "delFlag", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    void updateEntity(CommunityActivityUpdateRequest request, @MappingTarget CommunityActivityEntity entity);

    /**
     * Entity → VO（列表用）
     */
    CommunityActivityVO toVO(CommunityActivityEntity entity);

    /**
     * Entity → DetailVO（详情用）
     */
    CommunityActivityDetailVO toDetailVO(CommunityActivityEntity entity);
}