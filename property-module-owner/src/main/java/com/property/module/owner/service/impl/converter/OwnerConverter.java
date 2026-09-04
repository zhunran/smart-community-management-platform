package com.property.module.owner.service.impl.converter;

import com.property.module.owner.dto.request.OwnerCreateRequest;
import com.property.module.owner.dto.request.OwnerProfileUpdateRequest;
import com.property.module.owner.dto.request.OwnerUpdateRequest;
import com.property.module.owner.dto.response.OwnerDetailVO;
import com.property.module.owner.dto.response.OwnerVO;
import com.property.module.owner.entity.OwnerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * 业主实体与 DTO 转换器（MapStruct）
 */
@Mapper(componentModel = "spring")
public interface OwnerConverter {

    /**
     * CreateRequest → Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "registerTime", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "lastLoginTime", ignore = true)
    @Mapping(target = "delFlag", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    OwnerEntity toEntity(OwnerCreateRequest request);

    /**
     * UpdateRequest → Entity（合并更新）
     */
    @Mapping(target = "registerTime", ignore = true)
    @Mapping(target = "lastLoginTime", ignore = true)
    @Mapping(target = "delFlag", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    void updateEntity(OwnerUpdateRequest request, @MappingTarget OwnerEntity entity);

    /**
     * ProfileUpdateRequest → Entity（业主端个人信息合并更新，仅更新可编辑字段）
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "idCardType", ignore = true)
    @Mapping(target = "idCardNo", ignore = true)
    @Mapping(target = "ownerType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "registerTime", ignore = true)
    @Mapping(target = "lastLoginTime", ignore = true)
    @Mapping(target = "remark", ignore = true)
    @Mapping(target = "delFlag", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    void updateProfileEntity(OwnerProfileUpdateRequest request, @MappingTarget OwnerEntity entity);

    /**
     * Entity → VO（列表用）
     */
    OwnerVO toVO(OwnerEntity entity);

    /**
     * Entity → DetailVO（详情用）
     */
    OwnerDetailVO toDetailVO(OwnerEntity entity);
}
