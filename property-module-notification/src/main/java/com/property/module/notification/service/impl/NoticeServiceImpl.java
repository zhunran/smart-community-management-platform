package com.property.module.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.property.module.notification.dto.NoticeCreateRequest;
import com.property.module.notification.dto.NoticePageQuery;
import com.property.module.notification.dto.NoticeVO;
import com.property.module.notification.entity.NoticeEntity;
import com.property.module.notification.repository.NoticeMapper;
import com.property.module.notification.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 公告服务实现
 */
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeMapper noticeMapper;

    @Override
    @Transactional
    public NoticeVO create(NoticeCreateRequest request, Long adminId) {
        NoticeEntity entity = new NoticeEntity();
        entity.setTitle(request.getTitle());
        entity.setContent(request.getContent());
        entity.setType(request.getType() != null ? request.getType() : "NOTICE");
        entity.setStatus(0); // 草稿
        entity.setCreateBy(adminId);
        noticeMapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public void publish(Long id) {
        NoticeEntity entity = noticeMapper.selectById(id);
        if (entity == null) return;
        entity.setStatus(1);
        entity.setPublishTime(LocalDateTime.now());
        noticeMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void offline(Long id) {
        NoticeEntity entity = noticeMapper.selectById(id);
        if (entity == null) return;
        entity.setStatus(2);
        noticeMapper.updateById(entity);
    }

    @Override
    public IPage<NoticeVO> page(NoticePageQuery query) {
        LambdaQueryWrapper<NoticeEntity> wrapper = new LambdaQueryWrapper<NoticeEntity>()
                .eq(query.getType() != null, NoticeEntity::getType, query.getType())
                .eq(query.getStatus() != null, NoticeEntity::getStatus, query.getStatus())
                .orderByDesc(NoticeEntity::getCreateTime);
        Page<NoticeEntity> page = new Page<>(query.getCurrent(), query.getSize());
        IPage<NoticeEntity> result = noticeMapper.selectPage(page, wrapper);
        return result.convert(this::toVO);
    }

    @Override
    public List<NoticeVO> listLatest(int limit) {
        LambdaQueryWrapper<NoticeEntity> wrapper = new LambdaQueryWrapper<NoticeEntity>()
                .eq(NoticeEntity::getStatus, 1) // 已发布
                .orderByDesc(NoticeEntity::getPublishTime)
                .last("LIMIT " + limit);
        return noticeMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private NoticeVO toVO(NoticeEntity entity) {
        NoticeVO vo = new NoticeVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}