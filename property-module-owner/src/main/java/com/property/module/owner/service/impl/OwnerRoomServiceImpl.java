package com.property.module.owner.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.module.owner.dto.request.OwnerRoomBindRequest;
import com.property.module.owner.dto.response.OwnerRoomVO;
import com.property.module.owner.entity.OwnerEntity;
import com.property.module.owner.entity.OwnerRoomEntity;
import com.property.module.owner.repository.OwnerMapper;
import com.property.module.owner.repository.OwnerRoomMapper;
import com.property.module.owner.service.OwnerRoomService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 业主-房屋关联服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OwnerRoomServiceImpl implements OwnerRoomService {

    private final OwnerRoomMapper ownerRoomMapper;
    private final OwnerMapper ownerMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long bind(OwnerRoomBindRequest request) {
        log.info("绑定业主房屋，ownerId={}, roomId={}, type={}", request.getOwnerId(), request.getRoomId(), request.getOwnerId().getClass().getSimpleName());
        // 1. 校验业主存在
        OwnerEntity owner = ownerMapper.selectById(request.getOwnerId());
        if (owner == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "业主不存在");
        }

        // 2. 校验是否已存在相同绑定关系
        Integer relationType = request.getRelationType() != null ? request.getRelationType() : 1;
        Long count = ownerRoomMapper.selectCount(
                new LambdaQueryWrapper<OwnerRoomEntity>()
                        .eq(OwnerRoomEntity::getOwnerId, request.getOwnerId())
                        .eq(OwnerRoomEntity::getRoomId, request.getRoomId())
                        .eq(OwnerRoomEntity::getRelationType, relationType)
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.DATA_EXISTS, "该业主已与该房屋存在相同类型的关联");
        }

        // 3. 如果是主要业主，先取消该房屋下其他主要业主标记
        Integer isPrimary = request.getIsPrimary() != null ? request.getIsPrimary() : 0;
        if (isPrimary == 1) {
            ownerRoomMapper.update(
                    new LambdaUpdateWrapper<OwnerRoomEntity>()
                            .eq(OwnerRoomEntity::getRoomId, request.getRoomId())
                            .eq(OwnerRoomEntity::getIsPrimary, 1)
                            .set(OwnerRoomEntity::getIsPrimary, 0)
            );
        }

        // 4. 创建绑定
        OwnerRoomEntity entity = new OwnerRoomEntity();
        entity.setOwnerId(request.getOwnerId());
        entity.setRoomId(request.getRoomId());
        entity.setRelationType(relationType);
        entity.setIsPrimary(isPrimary);
        entity.setMoveInTime(request.getMoveInTime() != null ? request.getMoveInTime() : LocalDate.now());
        entity.setStatus(1);

        ownerRoomMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind(Long id) {
        OwnerRoomEntity entity = ownerRoomMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "关联关系不存在");
        }
        ownerRoomMapper.deleteById(id);
    }

    @Override
    public List<OwnerRoomVO> listByOwnerId(Long ownerId) {
        // 校验业主存在
        if (ownerMapper.selectById(ownerId) == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "业主不存在");
        }

        List<OwnerRoomEntity> entities = ownerRoomMapper.selectList(
                new LambdaQueryWrapper<OwnerRoomEntity>()
                        .eq(OwnerRoomEntity::getOwnerId, ownerId)
                        .eq(OwnerRoomEntity::getStatus, 1)
                        .orderByDesc(OwnerRoomEntity::getCreateTime)
        );

        return entities.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getRoomIdsByOwnerId(Long ownerId) {
        return ownerRoomMapper.selectList(
                new LambdaQueryWrapper<OwnerRoomEntity>()
                        .eq(OwnerRoomEntity::getOwnerId, ownerId)
                        .eq(OwnerRoomEntity::getStatus, 1)
        ).stream().map(OwnerRoomEntity::getRoomId).collect(Collectors.toList());
    }

    @Override
    public List<OwnerRoomVO> listByRoomId(Long roomId) {
        List<OwnerRoomEntity> entities = ownerRoomMapper.selectList(
                new LambdaQueryWrapper<OwnerRoomEntity>()
                        .eq(OwnerRoomEntity::getRoomId, roomId)
                        .eq(OwnerRoomEntity::getStatus, 1)
                        .orderByDesc(OwnerRoomEntity::getIsPrimary)
                        .orderByDesc(OwnerRoomEntity::getCreateTime)
        );

        return entities.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public Long getPrimaryOwnerIdByRoomId(Long roomId) {
        OwnerRoomEntity entity = ownerRoomMapper.selectOne(
                new LambdaQueryWrapper<OwnerRoomEntity>()
                        .eq(OwnerRoomEntity::getRoomId, roomId)
                        .eq(OwnerRoomEntity::getIsPrimary, 1)
                        .eq(OwnerRoomEntity::getStatus, 1)
                        .last("LIMIT 1")
        );
        return entity != null ? entity.getOwnerId() : null;
    }

    private OwnerRoomVO toVO(OwnerRoomEntity entity) {
        OwnerRoomVO vo = new OwnerRoomVO();
        vo.setId(entity.getId());
        vo.setOwnerId(entity.getOwnerId());
        vo.setRoomId(entity.getRoomId());
        vo.setRelationType(entity.getRelationType());
        vo.setIsPrimary(entity.getIsPrimary());
        vo.setMoveInTime(entity.getMoveInTime());
        vo.setMoveOutTime(entity.getMoveOutTime());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());

        // 填充业主信息
        OwnerEntity owner = ownerMapper.selectById(entity.getOwnerId());
        if (owner != null) {
            vo.setOwnerName(owner.getOwnerName());
            vo.setOwnerPhone(owner.getPhone());
        }

        return vo;
    }
}
