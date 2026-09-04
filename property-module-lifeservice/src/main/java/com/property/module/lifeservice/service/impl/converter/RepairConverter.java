package com.property.module.lifeservice.service.impl.converter;

import com.property.module.lifeservice.dto.repair.request.RepairOrderCreateRequest;
import com.property.module.lifeservice.dto.repair.response.RepairOrderVO;
import com.property.module.lifeservice.entity.RepairOrderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 报修工单实体与 DTO 转换器（MapStruct）
 */
@Mapper(componentModel = "spring")
public interface RepairConverter {

    /**
     * CreateRequest → Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderNo", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "handlerId", ignore = true)
    @Mapping(target = "handleNote", ignore = true)
    @Mapping(target = "rejectReason", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "ratingComment", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "timeoutFlag", ignore = true)
    @Mapping(target = "delFlag", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    RepairOrderEntity toEntity(RepairOrderCreateRequest request);

    /**
     * Entity → VO（categoryName/statusName/urgencyName 由 service 层填充）
     */
    RepairOrderVO toVO(RepairOrderEntity entity);
}
