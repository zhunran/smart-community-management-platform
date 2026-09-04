package com.property.module.bill.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.module.bill.dto.request.PaymentOrderPageQuery;
import com.property.module.bill.dto.response.PaymentOrderVO;

/**
 * 支付记录服务接口
 */
public interface PaymentOrderService {

    /**
     * 支付记录分页查询
     */
    IPage<PaymentOrderVO> page(PaymentOrderPageQuery query);

    /**
     * 支付记录详情
     */
    PaymentOrderVO getDetail(Long id);
}
