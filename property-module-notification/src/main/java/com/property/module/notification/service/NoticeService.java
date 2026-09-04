package com.property.module.notification.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.module.notification.dto.NoticeCreateRequest;
import com.property.module.notification.dto.NoticePageQuery;
import com.property.module.notification.dto.NoticeVO;

import java.util.List;

/**
 * 公告服务接口
 */
public interface NoticeService {

    /** 创建公告（草稿） */
    NoticeVO create(NoticeCreateRequest request, Long adminId);

    /** 发布公告 */
    void publish(Long id);

    /** 下线公告 */
    void offline(Long id);

    /** 分页查询（管理端） */
    IPage<NoticeVO> page(NoticePageQuery query);

    /** 查询最新 N 条已发布公告（业主端/AI 工具用） */
    List<NoticeVO> listLatest(int limit);
}