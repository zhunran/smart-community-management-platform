package com.property.ownerapi.service;

import com.property.module.housing.service.RoomDataService;
import com.property.module.owner.dto.response.OwnerRoomVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 业主房屋信息服务（封装跨模块 Mapper 调用，批量填充房屋信息）
 */
@Service
@RequiredArgsConstructor
public class OwnerRoomInfoService {

    private final RoomDataService roomDataService;

    /**
     * 批量填充房屋信息（楼栋名称、房号、房屋名称）
     */
    public void fillRoomInfo(List<OwnerRoomVO> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return;
        }
        List<Long> roomIds = rooms.stream()
                .map(OwnerRoomVO::getRoomId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (roomIds.isEmpty()) {
            return;
        }

        // 批量查询楼栋信息
        List<Map<String, Object>> buildingInfos = roomDataService.getRoomBuildingInfoBatch(roomIds);
        Map<Long, String> buildingNameMap = new HashMap<>();
        for (Map<String, Object> info : buildingInfos) {
            Long roomId = ((Number) info.get("roomId")).longValue();
            buildingNameMap.put(roomId, (String) info.get("buildingName"));
        }

        // 批量查询房号和名称
        List<Map<String, Object>> roomInfos = roomDataService.getRoomCodeNameBatch(roomIds);
        Map<Long, String> roomCodeMap = new HashMap<>();
        Map<Long, String> roomNameMap = new HashMap<>();
        for (Map<String, Object> info : roomInfos) {
            Long roomId = ((Number) info.get("roomId")).longValue();
            roomCodeMap.put(roomId, (String) info.get("roomCode"));
            roomNameMap.put(roomId, (String) info.get("roomName"));
        }

        for (OwnerRoomVO room : rooms) {
            Long roomId = room.getRoomId();
            if (roomId != null) {
                room.setBuildingName(buildingNameMap.get(roomId));
                room.setRoomCode(roomCodeMap.get(roomId));
                room.setRoomName(roomNameMap.get(roomId));
            }
        }
    }
}