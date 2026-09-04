package com.property.adminapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.property.adminapi.dto.request.BuildingCreateRequest;
import com.property.adminapi.dto.request.BuildingPageQuery;
import com.property.adminapi.dto.request.BuildingUpdateRequest;
import com.property.adminapi.dto.response.BuildingVO;
import com.property.adminapi.service.BuildingService;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.framework.web.security.SecurityUtil;
import com.property.module.housing.entity.BuildingEntity;
import com.property.module.housing.repository.BuildingMapper;
import com.property.module.housing.entity.UnitEntity;
import com.property.module.housing.repository.UnitMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 楼栋服务实现
 */
@Service
@RequiredArgsConstructor
public class BuildingServiceImpl implements BuildingService {

    private final BuildingMapper buildingMapper;
    private final UnitMapper unitMapper;

    @Override
    public IPage<BuildingVO> page(BuildingPageQuery query) {
        Page<BuildingEntity> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<BuildingEntity> wrapper = new LambdaQueryWrapper<BuildingEntity>()
                .like(query.getBuildingCode() != null, BuildingEntity::getBuildingCode, query.getBuildingCode())
                .like(query.getBuildingName() != null, BuildingEntity::getBuildingName, query.getBuildingName())
                .eq(query.getStatus() != null, BuildingEntity::getStatus, query.getStatus())
                .orderByAsc(BuildingEntity::getSortOrder)
                .orderByDesc(BuildingEntity::getCreateTime);

        IPage<BuildingEntity> entityPage = buildingMapper.selectPage(page, wrapper);
        return entityPage.convert(this::toVO);
    }

    @Override
    public BuildingVO getDetail(Long id) {
        BuildingEntity entity = buildingMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "楼栋不存在");
        }
        return toVO(entity);
    }

    @Override
    public List<BuildingVO> listAll() {
        List<BuildingEntity> entities = buildingMapper.selectList(
                new LambdaQueryWrapper<BuildingEntity>()
                        .eq(BuildingEntity::getStatus, 1)
                        .orderByAsc(BuildingEntity::getSortOrder)
        );
        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(BuildingCreateRequest request) {
        // 校验楼栋编号唯一
        Long count = buildingMapper.selectCount(
                new LambdaQueryWrapper<BuildingEntity>()
                        .eq(BuildingEntity::getBuildingCode, request.getBuildingCode())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.DATA_EXISTS, "楼栋编号已存在");
        }

        BuildingEntity entity = new BuildingEntity();
        entity.setBuildingCode(request.getBuildingCode());
        entity.setBuildingName(request.getBuildingName());
        entity.setTotalUnits(request.getTotalUnits() != null ? request.getTotalUnits() : 0);
        entity.setTotalFloors(request.getTotalFloors() != null ? request.getTotalFloors() : 0);
        entity.setTotalRooms(request.getTotalRooms() != null ? request.getTotalRooms() : 0);
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        entity.setStatus(request.getStatus());
        entity.setRemark(request.getRemark());
        entity.setCreateBy(SecurityUtil.getUsername());

        buildingMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BuildingVO update(BuildingUpdateRequest request) {
        BuildingEntity entity = buildingMapper.selectById(request.getId());
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "楼栋不存在");
        }

        // 校验楼栋编号唯一（排除自身）
        if (!entity.getBuildingCode().equals(request.getBuildingCode())) {
            Long count = buildingMapper.selectCount(
                    new LambdaQueryWrapper<BuildingEntity>()
                            .eq(BuildingEntity::getBuildingCode, request.getBuildingCode())
                            .ne(BuildingEntity::getId, request.getId())
            );
            if (count > 0) {
                throw new BusinessException(ErrorCode.DATA_EXISTS, "楼栋编号已存在");
            }
        }

        entity.setBuildingCode(request.getBuildingCode());
        entity.setBuildingName(request.getBuildingName());
        entity.setTotalUnits(request.getTotalUnits() != null ? request.getTotalUnits() : 0);
        entity.setTotalFloors(request.getTotalFloors() != null ? request.getTotalFloors() : 0);
        entity.setTotalRooms(request.getTotalRooms() != null ? request.getTotalRooms() : 0);
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        entity.setStatus(request.getStatus());
        entity.setRemark(request.getRemark());
        entity.setUpdateBy(SecurityUtil.getUsername());

        buildingMapper.updateById(entity);
        return toVO(buildingMapper.selectById(entity.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        BuildingEntity entity = buildingMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "楼栋不存在");
        }

        // 检查是否有单元关联
        Long unitCount = unitMapper.selectCount(
                new LambdaQueryWrapper<UnitEntity>().eq(UnitEntity::getBuildingId, id)
        );
        if (unitCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "该楼栋下存在单元，无法删除");
        }

        buildingMapper.deleteById(id); // 逻辑删除（@TableLogic）
    }

    private BuildingVO toVO(BuildingEntity entity) {
        BuildingVO vo = new BuildingVO();
        vo.setId(entity.getId());
        vo.setBuildingCode(entity.getBuildingCode());
        vo.setBuildingName(entity.getBuildingName());
        vo.setTotalUnits(entity.getTotalUnits());
        vo.setTotalFloors(entity.getTotalFloors());
        vo.setTotalRooms(entity.getTotalRooms());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
