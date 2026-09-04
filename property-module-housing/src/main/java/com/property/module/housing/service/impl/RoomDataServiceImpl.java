package com.property.module.housing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.property.module.housing.entity.BuildingEntity;
import com.property.module.housing.entity.RoomEntity;
import com.property.module.housing.entity.UnitEntity;
import com.property.module.housing.repository.BuildingMapper;
import com.property.module.housing.repository.RoomMapper;
import com.property.module.housing.repository.UnitMapper;
import com.property.module.housing.service.RoomDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 房屋数据服务实现
 */
@Service
@RequiredArgsConstructor
public class RoomDataServiceImpl implements RoomDataService {

    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;
    private final UnitMapper unitMapper;

    @Override
    public List<Long> getAllActiveRoomIds() {
        return roomMapper.selectList(
                new LambdaQueryWrapper<RoomEntity>()
                        .eq(RoomEntity::getStatus, 1)
                        .eq(RoomEntity::getDelFlag, 0)
        ).stream().map(RoomEntity::getId).collect(Collectors.toList());
    }

    @Override
    public BigDecimal getAreaByRoomId(Long roomId) {
        RoomEntity room = roomMapper.selectById(roomId);
        return room != null ? room.getArea() : null;
    }

    @Override
    public List<Long> getRoomIdsByBuildingId(Long buildingId) {
        return roomMapper.selectList(
                new LambdaQueryWrapper<RoomEntity>()
                        .eq(RoomEntity::getBuildingId, buildingId)
                        .eq(RoomEntity::getDelFlag, 0)
        ).stream().map(RoomEntity::getId).collect(Collectors.toList());
    }

    @Override
    public String getBuildingNameByRoomId(Long roomId) {
        RoomEntity room = roomMapper.selectById(roomId);
        if (room == null) return null;
        BuildingEntity building = buildingMapper.selectById(room.getBuildingId());
        return building != null ? building.getBuildingName() : null;
    }

    @Override
    public String getRoomCodeByRoomId(Long roomId) {
        RoomEntity room = roomMapper.selectById(roomId);
        return room != null ? room.getRoomCode() : null;
    }

    @Override
    public String getRoomNameByRoomId(Long roomId) {
        RoomEntity room = roomMapper.selectById(roomId);
        return room != null ? room.getRoomName() : null;
    }

    @Override
    public String getUnitNameByRoomId(Long roomId) {
        RoomEntity room = roomMapper.selectById(roomId);
        if (room == null) return null;
        UnitEntity unit = unitMapper.selectById(room.getUnitId());
        return unit != null ? unit.getUnitName() : null;
    }

    @Override
    public List<Map<String, Object>> getRoomBuildingInfoBatch(List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) return Collections.emptyList();
        List<RoomEntity> rooms = roomMapper.selectBatchIds(roomIds);
        List<Map<String, Object>> result = new ArrayList<>();
        for (RoomEntity room : rooms) {
            BuildingEntity building = buildingMapper.selectById(room.getBuildingId());
            Map<String, Object> map = new HashMap<>();
            map.put("roomId", room.getId());
            map.put("buildingId", building != null ? building.getId() : null);
            map.put("buildingName", building != null ? building.getBuildingName() : null);
            result.add(map);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getRoomCodeNameBatch(List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) return Collections.emptyList();
        List<RoomEntity> rooms = roomMapper.selectBatchIds(roomIds);
        return rooms.stream().map(room -> {
            Map<String, Object> map = new HashMap<>();
            map.put("roomId", room.getId());
            map.put("roomCode", room.getRoomCode());
            map.put("roomName", room.getRoomName());
            return map;
        }).collect(Collectors.toList());
    }
}