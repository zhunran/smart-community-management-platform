package com.property.module.lifeservice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.property.common.dto.PageQuery;
import com.property.module.lifeservice.dto.visitor.request.VisitorPassCreateRequest;
import com.property.module.lifeservice.dto.visitor.request.VisitorPassQuery;
import com.property.module.lifeservice.dto.visitor.request.VisitorPassVerifyRequest;
import com.property.module.lifeservice.dto.visitor.response.VisitorPassVO;
import com.property.module.lifeservice.dto.visitor.response.VisitorPassVerifyVO;
import com.property.module.lifeservice.entity.VisitorPassEntity;

public interface VisitorPassService extends IService<VisitorPassEntity> {

    /** 业主端生成访客通行码 */
    VisitorPassVO create(VisitorPassCreateRequest request, Long ownerId);

    /** 业主端我的通行码 */
    IPage<VisitorPassVO> myPage(PageQuery query, Long ownerId);

    /** 业主端撤销通行码 */
    void revoke(Long id, Long ownerId);

    /** 管理端通行码分页 */
    IPage<VisitorPassVO> adminPage(VisitorPassQuery query);

    /** 管理端核销通行码 */
    VisitorPassVerifyVO verify(VisitorPassVerifyRequest request);
}
