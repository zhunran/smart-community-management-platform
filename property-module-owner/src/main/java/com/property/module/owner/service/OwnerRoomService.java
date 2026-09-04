package com.property.module.owner.service;

import com.property.module.owner.dto.request.OwnerRoomBindRequest;
import com.property.module.owner.dto.response.OwnerRoomVO;

import java.util.List;

/**
 * 业主-房屋关联服务接口
 */
public interface OwnerRoomService {

    /**
     * 绑定业主到房屋
     */
    Long bind(OwnerRoomBindRequest request);

    /**
     * 解绑（逻辑删除）
     */
    void unbind(Long id);

    /**
     * 查询业主名下的所有房屋关联（含房屋、楼栋信息）
     */
    List<OwnerRoomVO> listByOwnerId(Long ownerId);

    /**
     * 查询业主名下的有效房屋ID列表
     */
    List<Long> getRoomIdsByOwnerId(Long ownerId);

    /**
     * 查询房屋关联的所有业主（含业主信息）
     */
    List<OwnerRoomVO> listByRoomId(Long roomId);

    /**
     * 查询房屋的主要业主ID
     */
    Long getPrimaryOwnerIdByRoomId(Long roomId);
}
