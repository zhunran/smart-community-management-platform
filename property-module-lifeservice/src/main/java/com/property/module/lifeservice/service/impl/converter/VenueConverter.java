package com.property.module.lifeservice.service.impl.converter;

import com.property.module.lifeservice.dto.venue.request.VenueCreateRequest;
import com.property.module.lifeservice.dto.venue.request.VenueUpdateRequest;
import com.property.module.lifeservice.dto.venue.response.VenueBookingVO;
import com.property.module.lifeservice.dto.venue.response.VenueVO;
import com.property.module.lifeservice.entity.VenueBookingEntity;
import com.property.module.lifeservice.entity.VenueEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * 场地实体与 DTO 转换器（MapStruct）
 */
@Mapper(componentModel = "spring")
public interface VenueConverter {

    /**
     * CreateRequest → Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "delFlag", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    VenueEntity toEntity(VenueCreateRequest request);

    /**
     * UpdateRequest → Entity（合并更新，null 字段不覆盖）
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "delFlag", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    void updateEntity(VenueUpdateRequest request, @MappingTarget VenueEntity entity);

    /**
     * Entity → VO（venueTypeName/statusName 由 service 层填充）
     */
    @Mapping(target = "venueTypeName", ignore = true)
    @Mapping(target = "statusName", ignore = true)
    VenueVO toVO(VenueEntity entity);

    /**
     * BookingEntity → BookingVO（venueName/statusName 由 service 层填充）
     */
    @Mapping(target = "venueName", ignore = true)
    @Mapping(target = "statusName", ignore = true)
    VenueBookingVO toBookingVO(VenueBookingEntity entity);
}
