package com.property.module.parking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.framework.web.security.SecurityUtil;
import com.property.module.parking.dto.request.ParkingBindRequest;
import com.property.module.parking.dto.request.ParkingChangeRequest;
import com.property.module.parking.dto.response.ParkingSpaceVO;
import com.property.module.parking.entity.ParkingChangeLogEntity;
import com.property.module.parking.entity.ParkingSpaceEntity;
import com.property.module.parking.repository.ParkingChangeLogMapper;
import com.property.module.parking.repository.ParkingSpaceMapper;
import com.property.module.parking.service.ParkingSpaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 车位服务实现
 *
 * 所有变更操作均使用 @Version 乐观锁防止并发冲突，
 * 每次变更自动写入 t_parking_change_log 变更日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingSpaceServiceImpl implements ParkingSpaceService {

    private final ParkingSpaceMapper parkingSpaceMapper;
    private final ParkingChangeLogMapper changeLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParkingSpaceVO bind(ParkingBindRequest request) {
        // 1. 查询车位（带乐观锁 version）
        ParkingSpaceEntity space = parkingSpaceMapper.selectById(request.getSpaceId());
        if (space == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "车位不存在");
        }
        // 仅允许空闲车位绑定
        if (space.getStatus() != 0) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "车位当前状态不允许绑定 [status=" + space.getStatus() + "]");
        }

        // 2. 记录变更前的快照
        Long oldOwnerId = space.getOwnerId();
        Long oldRoomId = space.getRoomId();
        int oldStatus = space.getStatus();

        // 3. 更新车位信息
        space.setOwnerId(request.getOwnerId());
        space.setRoomId(request.getRoomId());
        space.setRentalType(request.getRentalType());
        space.setRemark(request.getRemark());
        // 自有→已售(1)，租赁→已租(2)
        space.setStatus(request.getRentalType() == 1 ? 1 : 2);

        // 4. 乐观锁更新（version 自增，并发时 version 不匹配则更新 0 行）
        int updated = parkingSpaceMapper.updateById(space);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "车位已被其他操作修改，请刷新后重试");
        }

        // 5. 写入变更日志
        writeLog(space, "BIND", oldOwnerId, request.getOwnerId(), oldRoomId, request.getRoomId(),
                oldStatus, space.getStatus(), request.getRemark());

        log.info("车位绑定成功 [spaceId={}, ownerId={}, rentalType={}]", request.getSpaceId(), request.getOwnerId(), request.getRentalType());
        return toVO(space);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParkingSpaceVO change(ParkingChangeRequest request) {
        // 1. 查询车位
        ParkingSpaceEntity space = parkingSpaceMapper.selectById(request.getSpaceId());
        if (space == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "车位不存在");
        }
        // 仅已售或已租的车位可以变更
        if (space.getStatus() != 1 && space.getStatus() != 2) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "车位当前状态不允许变更 [status=" + space.getStatus() + "]");
        }

        // 2. 记录变更前快照
        Long oldOwnerId = space.getOwnerId();
        Long oldRoomId = space.getRoomId();
        int oldStatus = space.getStatus();

        // 3. 更新
        space.setOwnerId(request.getNewOwnerId());
        space.setRoomId(request.getNewRoomId());
        if (request.getRemark() != null) {
            space.setRemark(request.getRemark());
        }

        int updated = parkingSpaceMapper.updateById(space);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "车位已被其他操作修改，请刷新后重试");
        }

        // 4. 写入变更日志
        writeLog(space, "CHANGE", oldOwnerId, request.getNewOwnerId(), oldRoomId, request.getNewRoomId(),
                oldStatus, space.getStatus(), request.getRemark());

        log.info("车位变更成功 [spaceId={}, oldOwnerId={}, newOwnerId={}]", request.getSpaceId(), oldOwnerId, request.getNewOwnerId());
        return toVO(space);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParkingSpaceVO unbind(Long spaceId, String remark) {
        // 1. 查询车位
        ParkingSpaceEntity space = parkingSpaceMapper.selectById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "车位不存在");
        }
        if (space.getStatus() == 0) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "车位当前已是空闲状态");
        }

        // 2. 记录快照
        Long oldOwnerId = space.getOwnerId();
        Long oldRoomId = space.getRoomId();
        int oldStatus = space.getStatus();

        // 3. 清空绑定，置为空闲
        space.setOwnerId(null);
        space.setRoomId(null);
        space.setStatus(0);          // 空闲
        space.setRemark(remark);

        int updated = parkingSpaceMapper.updateById(space);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "车位已被其他操作修改，请刷新后重试");
        }

        // 4. 写入变更日志
        writeLog(space, "UNBIND", oldOwnerId, null, oldRoomId, null,
                oldStatus, 0, remark);

        log.info("车位退租成功 [spaceId={}, oldOwnerId={}]", spaceId, oldOwnerId);
        return toVO(space);
    }

    @Override
    public List<ParkingSpaceVO> listAll() {
        List<ParkingSpaceEntity> entities = parkingSpaceMapper.selectList(
                new LambdaQueryWrapper<ParkingSpaceEntity>()
                        .orderByAsc(ParkingSpaceEntity::getFloor, ParkingSpaceEntity::getZone, ParkingSpaceEntity::getSpaceCode)
        );
        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ParkingSpaceVO getDetail(Long id) {
        ParkingSpaceEntity entity = parkingSpaceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "车位不存在");
        }
        return toVO(entity);
    }

    private void writeLog(ParkingSpaceEntity space, String changeType,
                          Long oldOwnerId, Long newOwnerId,
                          Long oldRoomId, Long newRoomId,
                          int oldStatus, int newStatus,
                          String remark) {
        ParkingChangeLogEntity logEntity = new ParkingChangeLogEntity();
        logEntity.setSpaceId(space.getId());
        logEntity.setSpaceCode(space.getSpaceCode());
        logEntity.setChangeType(changeType);
        logEntity.setOldOwnerId(oldOwnerId);
        logEntity.setNewOwnerId(newOwnerId);
        logEntity.setOldRoomId(oldRoomId);
        logEntity.setNewRoomId(newRoomId);
        logEntity.setOldStatus(oldStatus);
        logEntity.setNewStatus(newStatus);
        logEntity.setRemark(remark);
        logEntity.setOperator(SecurityUtil.getUsername() != null ? SecurityUtil.getUsername() : "system");
        changeLogMapper.insert(logEntity);
    }

    private ParkingSpaceVO toVO(ParkingSpaceEntity entity) {
        ParkingSpaceVO vo = new ParkingSpaceVO();
        vo.setId(entity.getId());
        vo.setSpaceCode(entity.getSpaceCode());
        vo.setSpaceName(entity.getSpaceName());
        vo.setSpaceType(entity.getSpaceType());
        vo.setArea(entity.getArea());
        vo.setFloor(entity.getFloor());
        vo.setZone(entity.getZone());
        vo.setOwnerId(entity.getOwnerId());
        vo.setRoomId(entity.getRoomId());
        vo.setRentalType(entity.getRentalType());
        vo.setMonthlyFee(entity.getMonthlyFee());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
