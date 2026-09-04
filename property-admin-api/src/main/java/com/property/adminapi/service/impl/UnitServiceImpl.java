package com.property.adminapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.property.adminapi.dto.request.UnitCreateRequest;
import com.property.adminapi.dto.request.UnitPageQuery;
import com.property.adminapi.dto.request.UnitUpdateRequest;
import com.property.adminapi.dto.response.UnitVO;
import com.property.adminapi.service.UnitService;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 单元服务实现
 */
@Service
@RequiredArgsConstructor
public class UnitServiceImpl implements UnitService {

    private final UnitMapper unitMapper;
    private final BuildingMapper buildingMapper;
    private final RoomMapper roomMapper;

    @Override
    public IPage<UnitVO> page(UnitPageQuery query) {
        Page<UnitEntity> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<UnitEntity> wrapper = new LambdaQueryWrapper<UnitEntity>()
                .eq(query.getBuildingId() != null && query.getBuildingId() > 0, UnitEntity::getBuildingId, query.getBuildingId())
                .like(query.getUnitCode() != null && !query.getUnitCode().isEmpty(), UnitEntity::getUnitCode, query.getUnitCode())
                .like(query.getUnitName() != null && !query.getUnitName().isEmpty(), UnitEntity::getUnitName, query.getUnitName())
                .eq(query.getStatus() != null, UnitEntity::getStatus, query.getStatus())
                .orderByAsc(UnitEntity::getSortOrder)
                .orderByDesc(UnitEntity::getCreateTime);

        IPage<UnitEntity> entityPage = unitMapper.selectPage(page, wrapper);
        return entityPage.convert(this::toVO);
    }

    @Override
    public UnitVO getDetail(Long id) {
        UnitEntity entity = unitMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "单元不存在");
        }
        return toVO(entity);
    }

    @Override
    public List<UnitVO> listByBuildingId(Long buildingId) {
        List<UnitEntity> entities = unitMapper.selectList(
                new LambdaQueryWrapper<UnitEntity>()
                        .eq(UnitEntity::getBuildingId, buildingId)
                        .eq(UnitEntity::getStatus, 1)
                        .orderByAsc(UnitEntity::getSortOrder)
        );
        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(UnitCreateRequest request) {
        // 校验楼栋存在
        if (buildingMapper.selectById(request.getBuildingId()) == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "所属楼栋不存在");
        }

        // 校验同一楼栋下单元编号唯一
        Long count = unitMapper.selectCount(
                new LambdaQueryWrapper<UnitEntity>()
                        .eq(UnitEntity::getBuildingId, request.getBuildingId())
                        .eq(UnitEntity::getUnitCode, request.getUnitCode())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.DATA_EXISTS, "该楼栋下单元编号已存在");
        }

        UnitEntity entity = new UnitEntity();
        entity.setBuildingId(request.getBuildingId());
        entity.setUnitCode(request.getUnitCode());
        entity.setUnitName(request.getUnitName());
        entity.setTotalFloors(request.getTotalFloors() != null ? request.getTotalFloors() : 0);
        entity.setTotalRooms(request.getTotalRooms() != null ? request.getTotalRooms() : 0);
        entity.setElevatorCount(request.getElevatorCount() != null ? request.getElevatorCount() : 0);
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        entity.setStatus(request.getStatus());
        entity.setRemark(request.getRemark());

        unitMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UnitUpdateRequest request) {
        UnitEntity entity = unitMapper.selectById(request.getId());
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "单元不存在");
        }

        // 校验同一楼栋下单元编号唯一（排除自身）
        if (!entity.getUnitCode().equals(request.getUnitCode())
                || !entity.getBuildingId().equals(request.getBuildingId())) {
            Long count = unitMapper.selectCount(
                    new LambdaQueryWrapper<UnitEntity>()
                            .eq(UnitEntity::getBuildingId, request.getBuildingId())
                            .eq(UnitEntity::getUnitCode, request.getUnitCode())
                            .ne(UnitEntity::getId, request.getId())
            );
            if (count > 0) {
                throw new BusinessException(ErrorCode.DATA_EXISTS, "该楼栋下单元编号已存在");
            }
        }

        entity.setBuildingId(request.getBuildingId());
        entity.setUnitCode(request.getUnitCode());
        entity.setUnitName(request.getUnitName());
        entity.setTotalFloors(request.getTotalFloors() != null ? request.getTotalFloors() : 0);
        entity.setTotalRooms(request.getTotalRooms() != null ? request.getTotalRooms() : 0);
        entity.setElevatorCount(request.getElevatorCount() != null ? request.getElevatorCount() : 0);
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        entity.setStatus(request.getStatus());
        entity.setRemark(request.getRemark());

        unitMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        UnitEntity entity = unitMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "单元不存在");
        }

        // 检查是否有房屋关联
        Long roomCount = roomMapper.selectCount(
                new LambdaQueryWrapper<RoomEntity>().eq(RoomEntity::getUnitId, id)
        );
        if (roomCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "该单元下存在房屋，无法删除");
        }

        unitMapper.deleteById(id);
    }

    private UnitVO toVO(UnitEntity entity) {
        UnitVO vo = new UnitVO();
        vo.setId(entity.getId());
        vo.setBuildingId(entity.getBuildingId());
        vo.setUnitCode(entity.getUnitCode());
        vo.setUnitName(entity.getUnitName());
        vo.setTotalFloors(entity.getTotalFloors());
        vo.setTotalRooms(entity.getTotalRooms());
        vo.setElevatorCount(entity.getElevatorCount());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());

        // 填充楼栋名称
        BuildingEntity building = buildingMapper.selectById(entity.getBuildingId());
        if (building != null) {
            vo.setBuildingName(building.getBuildingName());
        }
        return vo;
    }
}
