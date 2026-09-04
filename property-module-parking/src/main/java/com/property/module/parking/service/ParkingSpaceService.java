package com.property.module.parking.service;

import com.property.module.parking.dto.request.ParkingBindRequest;
import com.property.module.parking.dto.request.ParkingChangeRequest;
import com.property.module.parking.dto.response.ParkingSpaceVO;

import java.util.List;

/**
 * 车位服务接口
 */
public interface ParkingSpaceService {

    /**
     * 绑定车位给业主
     */
    ParkingSpaceVO bind(ParkingBindRequest request);

    /**
     * 变更车位绑定（换业主/换房屋）
     */
    ParkingSpaceVO change(ParkingChangeRequest request);

    /**
     * 退租/解绑车位
     */
    ParkingSpaceVO unbind(Long spaceId, String remark);

    /**
     * 查询全部车位
     */
    List<ParkingSpaceVO> listAll();

    /**
     * 查询车位详情
     */
    ParkingSpaceVO getDetail(Long id);
}
