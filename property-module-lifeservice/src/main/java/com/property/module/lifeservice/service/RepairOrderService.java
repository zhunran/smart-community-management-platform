package com.property.module.lifeservice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.property.module.lifeservice.dto.repair.request.RepairOrderAssignRequest;
import com.property.module.lifeservice.dto.repair.request.RepairOrderAuditRequest;
import com.property.module.lifeservice.dto.repair.request.RepairOrderCompleteRequest;
import com.property.module.lifeservice.dto.repair.request.RepairOrderCreateRequest;
import com.property.module.lifeservice.dto.repair.request.RepairOrderQuery;
import com.property.module.lifeservice.dto.repair.request.RepairOrderRateRequest;
import com.property.module.lifeservice.dto.repair.response.RepairOrderVO;
import com.property.module.lifeservice.dto.repair.response.RepairStatisticsVO;
import com.property.module.lifeservice.entity.RepairOrderEntity;

public interface RepairOrderService extends IService<RepairOrderEntity> {

    /** 业主端提交报修（状态0待审核） */
    RepairOrderVO create(RepairOrderCreateRequest request, Long ownerId);

    /** 业主端我的工单 */
    IPage<RepairOrderVO> myPage(RepairOrderQuery query, Long ownerId);

    /** 业主端工单详情（校验归属） */
    RepairOrderVO getById(Long id, Long ownerId);

    /** 业主端取消工单（仅0/1态可取消） */
    void cancel(Long id, Long ownerId);

    /** 业主端评价（仅4态可评） */
    void rate(Long id, RepairOrderRateRequest request, Long ownerId);

    /** 管理端工单分页 */
    IPage<RepairOrderVO> adminPage(RepairOrderQuery query);

    /** 管理端工单详情 */
    RepairOrderVO adminGetDetail(Long id);

    /** 管理端审核（行级锁）：通过→待派单 / 驳回→已驳回+原因 */
    void audit(Long id, RepairOrderAuditRequest request);

    /** 管理端派单：指派维修员 */
    void assign(Long id, RepairOrderAssignRequest request);

    /** 维修员接单（校验本人，已派单→维修中） */
    void accept(Long id, Long handlerId);

    /** 维修员完工（校验本人，维修中→已完成+处理说明） */
    void complete(Long id, RepairOrderCompleteRequest request, Long handlerId);

    /** 管理端工单统计 */
    RepairStatisticsVO statistics();
}
