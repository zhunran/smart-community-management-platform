package com.property.module.bill.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.module.bill.dto.request.BillGenerateRequest;
import com.property.module.bill.dto.request.BillPageQuery;
import com.property.module.bill.dto.request.ItemizedPaymentRequest;
import com.property.module.bill.dto.request.ManualPaymentRequest;
import com.property.module.bill.dto.response.BillDetailVO;
import com.property.module.bill.dto.response.BillVO;

/**
 * 账单服务接口
 */
public interface BillService {

    /**
     * 手动触发生成账单
     * @return 成功生成的账单数
     */
    int generate(BillGenerateRequest request);

    /**
     * 账单分页查询
     */
    IPage<BillVO> page(BillPageQuery query);

    /**
     * 账单详情（含明细列表）
     */
    BillDetailVO getDetail(Long id);

    /**
     * 账单详情（含归属校验）
     * @param id 账单ID
     * @param ownerId 业主ID，用于校验账单归属
     * @throws com.property.common.exception.BusinessException 账单不属于该业主时抛出
     */
    BillDetailVO getDetail(Long id, Long ownerId);

    /**
     * 管理员手动标记缴费（整单缴清）
     * @param request 手动缴费请求
     * @return 支付记录ID
     */
    Long manualPayment(ManualPaymentRequest request);

    /**
     * 管理员分项缴费（支持部分缴费）
     * @param request 分项缴费请求
     * @return 支付记录ID
     */
    Long itemizedPayment(ItemizedPaymentRequest request);
}
