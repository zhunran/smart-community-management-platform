package com.property.module.housing.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 房屋数据服务——供其他模块跨模块访问房屋/楼栋/单元数据
 */
public interface RoomDataService {

    /** 查询所有有效房屋ID */
    List<Long> getAllActiveRoomIds();

    /** 查询指定房屋的面积 */
    BigDecimal getAreaByRoomId(Long roomId);

    /** 查询指定楼栋下的房屋ID列表 */
    List<Long> getRoomIdsByBuildingId(Long buildingId);

    /** 查询指定房屋的楼栋名称 */
    String getBuildingNameByRoomId(Long roomId);

    /** 查询指定房屋的房号 */
    String getRoomCodeByRoomId(Long roomId);

    /** 查询指定房屋的名称 */
    String getRoomNameByRoomId(Long roomId);

    /** 查询指定房屋的单元名称 */
    String getUnitNameByRoomId(Long roomId);

    /** 批量查询房屋楼栋信息 */
    List<Map<String, Object>> getRoomBuildingInfoBatch(List<Long> roomIds);

    /** 批量查询房屋房号和名称 */
    List<Map<String, Object>> getRoomCodeNameBatch(List<Long> roomIds);
}