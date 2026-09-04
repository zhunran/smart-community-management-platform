package com.property.adminapi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.adminapi.dto.request.RoomCreateRequest;
import com.property.adminapi.dto.request.RoomPageQuery;
import com.property.adminapi.dto.request.RoomUpdateRequest;
import com.property.adminapi.dto.response.RoomVO;

import java.util.List;

/**
 * 房屋服务接口
 */
public interface RoomService {

    /**
     * 分页查询房屋
     */
    IPage<RoomVO> page(RoomPageQuery query);

    /**
     * 查询房屋详情
     */
    RoomVO getDetail(Long id);

    /**
     * 查询某单元下的全部房屋
     */
    List<RoomVO> listByUnitId(Long unitId);

    /**
     * 新增房屋
     */
    Long create(RoomCreateRequest request);

    /**
     * 修改房屋
     */
    void update(RoomUpdateRequest request);

    /**
     * 删除房屋（逻辑删除）
     */
    void delete(Long id);
}
