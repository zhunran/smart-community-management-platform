package com.property.module.bill.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.bill.entity.PaymentOrderEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付记录 Mapper
 */
@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrderEntity> {
}
