package com.property.module.lifeservice.service.impl.converter;

import com.property.module.lifeservice.dto.visitor.request.VisitorPassCreateRequest;
import com.property.module.lifeservice.dto.visitor.response.VisitorPassVO;
import com.property.module.lifeservice.entity.VisitorPassEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 访客通行码实体与 DTO 转换器（MapStruct）
 */
@Mapper(componentModel = "spring")
public interface VisitorConverter {

    /**
     * CreateRequest → Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passCode", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "usedCount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "delFlag", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    VisitorPassEntity toEntity(VisitorPassCreateRequest request);

    /**
     * Entity → VO（statusName 由 service 层填充）
     */
    @Mapping(target = "statusName", ignore = true)
    VisitorPassVO toVO(VisitorPassEntity entity);
}
