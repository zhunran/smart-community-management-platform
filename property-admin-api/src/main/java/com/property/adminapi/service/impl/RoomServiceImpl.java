package com.property.adminapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.property.adminapi.dto.request.RoomCreateRequest;
import com.property.adminapi.dto.request.RoomPageQuery;
import com.property.adminapi.dto.request.RoomUpdateRequest;
import com.property.adminapi.dto.response.RoomVO;
import com.property.adminapi.service.RoomService;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.module.housing.entity.BuildingEntity;
import com.property.module.housing.repository.BuildingMapper;
import com.property.module.housing.entity.RoomEntity;
import com.property.module.housing.entity.UnitEntity;
import com.property.module.housing.repository.RoomMapper;
import com.property.module.housing.repository.UnitMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 房屋服务实现
 */
@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;
    private final UnitMapper unitMapper;

    @Override
    public IPage<RoomVO> page(RoomPageQuery query) {
        Page<RoomEntity> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<RoomEntity> wrapper = new LambdaQueryWrapper<RoomEntity>()
                .eq(query.getBuildingId() != null, RoomEntity::getBuildingId, query.getBuildingId())
                .eq(query.getUnitId() != null, RoomEntity::getUnitId, query.getUnitId())
                .like(query.getRoomCode() != null, RoomEntity::getRoomCode, query.getRoomCode())
                .like(query.getRoomName() != null, RoomEntity::getRoomName, query.getRoomName())
                .eq(query.getFloor() != null, RoomEntity::getFloor, query.getFloor())
                .eq(query.getRoomType() != null, RoomEntity::getRoomType, query.getRoomType())
                .eq(query.getOccupancyStatus() != null, RoomEntity::getOccupancyStatus, query.getOccupancyStatus())
                .eq(query.getStatus() != null, RoomEntity::getStatus, query.getStatus())
                .orderByAsc(RoomEntity::getBuildingId, RoomEntity::getUnitId, RoomEntity::getFloor, RoomEntity::getRoomCode);

        IPage<RoomEntity> entityPage = roomMapper.selectPage(page, wrapper);
        return entityPage.convert(this::toVO);
    }

    @Override
    public RoomVO getDetail(Long id) {
        RoomEntity entity = roomMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "房屋不存在");
        }
        return toVO(entity);
    }

    @Override
    public List<RoomVO> listByUnitId(Long unitId) {
        List<RoomEntity> entities = roomMapper.selectList(
                new LambdaQueryWrapper<RoomEntity>()
                        .eq(RoomEntity::getUnitId, unitId)
                        .eq(RoomEntity::getStatus, 1)
                        .orderByAsc(RoomEntity::getFloor, RoomEntity::getRoomCode)
        );
        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RoomCreateRequest request) {
        // 校验楼栋和单元存在
        if (buildingMapper.selectById(request.getBuildingId()) == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "所属楼栋不存在");
        }
        if (unitMapper.selectById(request.getUnitId()) == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "所属单元不存在");
        }

        // 校验同一楼栋+单元下房号唯一
        Long count = roomMapper.selectCount(
                new LambdaQueryWrapper<RoomEntity>()
                        .eq(RoomEntity::getBuildingId, request.getBuildingId())
                        .eq(RoomEntity::getUnitId, request.getUnitId())
                        .eq(RoomEntity::getRoomCode, request.getRoomCode())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.DATA_EXISTS, "该楼栋单元下房号已存在");
        }

        RoomEntity entity = new RoomEntity();
        entity.setBuildingId(request.getBuildingId());
        entity.setUnitId(request.getUnitId());
        entity.setRoomCode(request.getRoomCode());
        entity.setRoomName(request.getRoomName());
        entity.setFloor(request.getFloor());
        entity.setRoomType(request.getRoomType() != null ? request.getRoomType() : 1);
        entity.setArea(request.getArea());
        entity.setUsableArea(request.getUsableArea());
        entity.setOrientation(request.getOrientation());
        entity.setDecorationStatus(request.getDecorationStatus() != null ? request.getDecorationStatus() : 0);
        entity.setOccupancyStatus(request.getOccupancyStatus() != null ? request.getOccupancyStatus() : 0);
        entity.setPropertyFeeRate(request.getPropertyFeeRate() != null ? request.getPropertyFeeRate() : java.math.BigDecimal.ZERO);
        entity.setStatus(request.getStatus());
        entity.setRemark(request.getRemark());

        roomMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(RoomUpdateRequest request) {
        RoomEntity entity = roomMapper.selectById(request.getId());
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "房屋不存在");
        }

        // 校验房号唯一（排除自身）
        if (!entity.getRoomCode().equals(request.getRoomCode())
                || !entity.getBuildingId().equals(request.getBuildingId())
                || !entity.getUnitId().equals(request.getUnitId())) {
            Long count = roomMapper.selectCount(
                    new LambdaQueryWrapper<RoomEntity>()
                            .eq(RoomEntity::getBuildingId, request.getBuildingId())
                            .eq(RoomEntity::getUnitId, request.getUnitId())
                            .eq(RoomEntity::getRoomCode, request.getRoomCode())
                            .ne(RoomEntity::getId, request.getId())
            );
            if (count > 0) {
                throw new BusinessException(ErrorCode.DATA_EXISTS, "该楼栋单元下房号已存在");
            }
        }

        entity.setBuildingId(request.getBuildingId());
        entity.setUnitId(request.getUnitId());
        entity.setRoomCode(request.getRoomCode());
        entity.setRoomName(request.getRoomName());
        entity.setFloor(request.getFloor());
        entity.setRoomType(request.getRoomType() != null ? request.getRoomType() : 1);
        entity.setArea(request.getArea());
        entity.setUsableArea(request.getUsableArea());
        entity.setOrientation(request.getOrientation());
        entity.setDecorationStatus(request.getDecorationStatus() != null ? request.getDecorationStatus() : 0);
        entity.setOccupancyStatus(request.getOccupancyStatus() != null ? request.getOccupancyStatus() : 0);
        entity.setPropertyFeeRate(request.getPropertyFeeRate() != null ? request.getPropertyFeeRate() : java.math.BigDecimal.ZERO);
        entity.setStatus(request.getStatus());
        entity.setRemark(request.getRemark());

        roomMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        RoomEntity entity = roomMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "房屋不存在");
        }
        roomMapper.deleteById(id);
    }

    private RoomVO toVO(RoomEntity entity) {
        RoomVO vo = new RoomVO();
        vo.setId(entity.getId());
        vo.setBuildingId(entity.getBuildingId());
        vo.setUnitId(entity.getUnitId());
        vo.setRoomCode(entity.getRoomCode());
        vo.setRoomName(entity.getRoomName());
        vo.setFloor(entity.getFloor());
        vo.setRoomType(entity.getRoomType());
        vo.setArea(entity.getArea());
        vo.setUsableArea(entity.getUsableArea());
        vo.setOrientation(entity.getOrientation());
        vo.setDecorationStatus(entity.getDecorationStatus());
        vo.setOccupancyStatus(entity.getOccupancyStatus());
        vo.setPropertyFeeRate(entity.getPropertyFeeRate());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());

        // 填充关联名称
        BuildingEntity building = buildingMapper.selectById(entity.getBuildingId());
        if (building != null) {
            vo.setBuildingName(building.getBuildingName());
        }
        UnitEntity unit = unitMapper.selectById(entity.getUnitId());
        if (unit != null) {
            vo.setUnitName(unit.getUnitName());
        }
        return vo;
    }
}
