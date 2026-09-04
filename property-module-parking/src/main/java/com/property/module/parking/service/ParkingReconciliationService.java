package com.property.module.parking.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.module.parking.dto.request.WarningHandleRequest;
import com.property.module.parking.dto.response.ParkingWarningVO;

/**
 * 车位对账与预警服务
 */
public interface ParkingReconciliationService {

    /**
     * 执行双轨对账
     * 扫描车位状态、租赁合同、使用记录，自动生成预警
     * @return 本次生成的预警数量
     */
    int reconcile();

    /**
     * 处理预警
     */
    void handle(Long id, WarningHandleRequest request);

    /**
     * 关闭预警
     */
    void close(Long id, String remark);

    /**
     * 分页查询预警
     */
    IPage<ParkingWarningVO> page(int current, int size, String warningType, Integer status);
}
