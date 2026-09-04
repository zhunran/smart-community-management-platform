package com.property.module.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.property.common.enums.VoteStatusEnum;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.module.community.dto.request.VoteCastRequest;
import com.property.module.community.dto.request.VoteCreateRequest;
import com.property.module.community.dto.request.VoteQuery;
import com.property.module.community.dto.respose.VoteDetailVO;
import com.property.module.community.dto.respose.VoteOptionVO;
import com.property.module.community.dto.respose.VoteResultOptionVO;
import com.property.module.community.dto.respose.VoteResultVO;
import com.property.module.community.dto.respose.VoteVO;
import com.property.module.community.entity.CommunityVoteEntity;
import com.property.module.community.entity.VoteOptionEntity;
import com.property.module.community.entity.VoteRecordEntity;
import com.property.module.community.repository.CommunityVoteMapper;
import com.property.module.community.repository.VoteOptionMapper;
import com.property.module.community.repository.VoteRecordMapper;
import com.property.module.community.service.VoteService;
import com.property.module.community.service.impl.converter.VoteConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteServiceImpl extends ServiceImpl<CommunityVoteMapper, CommunityVoteEntity>
        implements VoteService {

    private static final int VOTE_TYPE_SINGLE = 1;
    private static final int VOTE_TYPE_MULTI = 2;
    private static final int ANONYMOUS = 1;

    private final VoteOptionMapper voteOptionMapper;
    private final VoteRecordMapper voteRecordMapper;
    private final VoteConverter voteConverter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createVote(VoteCreateRequest request) {
        validateCreate(request);

        CommunityVoteEntity entity = voteConverter.toEntity(request);
        entity.setStatus(resolveInitialStatus(request.getStartTime(), request.getEndTime()));
        this.save(entity);

        // 插入选项
        int sortOrder = 0;
        for (String option : request.getOptions()) {
            VoteOptionEntity optionEntity = new VoteOptionEntity();
            optionEntity.setVoteId(entity.getId());
            optionEntity.setContent(option);
            optionEntity.setVoteCount(0);
            optionEntity.setSortOrder(sortOrder++);
            voteOptionMapper.insert(optionEntity);
        }
    }

    @Override
    public IPage<VoteVO> adminPage(VoteQuery query) {
        LambdaQueryWrapper<CommunityVoteEntity> wrapper = buildQueryWrapper(query);
        Page<CommunityVoteEntity> page = new Page<>(query.getCurrent(), query.getSize());
        IPage<CommunityVoteEntity> entityPage = this.page(page, wrapper);
        IPage<VoteVO> voPage = entityPage.convert(voteConverter::toVO);
        voPage.getRecords().forEach(this::fillNames);
        return voPage;
    }

    @Override
    public VoteDetailVO getDetail(Long id) {
        return buildDetail(getByIdOrThrow(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void start(Long id) {
        CommunityVoteEntity entity = getByIdOrThrow(id);
        VoteStatusEnum statusEnum = VoteStatusEnum.fromValue(entity.getStatus());
        if (statusEnum == null || !statusEnum.canStart()) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "仅未开始的投票可开始 [status=" + entity.getStatus() + "]");
        }
        entity.setStatus(VoteStatusEnum.IN_PROGRESS.getValue());
        this.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void end(Long id) {
        CommunityVoteEntity entity = getByIdOrThrow(id);
        VoteStatusEnum statusEnum = VoteStatusEnum.fromValue(entity.getStatus());
        if (statusEnum == null || !statusEnum.canEnd()) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "仅进行中的投票可结束 [status=" + entity.getStatus() + "]");
        }
        entity.setStatus(VoteStatusEnum.ENDED.getValue());
        this.updateById(entity);
    }

    @Override
    public IPage<VoteVO> ownerPage(VoteQuery query) {
        LambdaQueryWrapper<CommunityVoteEntity> wrapper = buildQueryWrapper(query);
        wrapper.in(CommunityVoteEntity::getStatus,
                VoteStatusEnum.IN_PROGRESS.getValue(), VoteStatusEnum.ENDED.getValue());
        Page<CommunityVoteEntity> page = new Page<>(query.getCurrent(), query.getSize());
        IPage<CommunityVoteEntity> entityPage = this.page(page, wrapper);
        IPage<VoteVO> voPage = entityPage.convert(voteConverter::toVO);
        voPage.getRecords().forEach(this::fillNames);
        return voPage;
    }

    @Override
    public VoteDetailVO ownerGetDetail(Long id, Long ownerId) {
        CommunityVoteEntity entity = getByIdOrThrow(id);
        VoteDetailVO vo = buildDetail(entity);
        // 当前用户已投的选项
        List<Long> votedOptionIds = voteRecordMapper.selectList(
                        new LambdaQueryWrapper<VoteRecordEntity>()
                                .eq(VoteRecordEntity::getVoteId, id)
                                .eq(VoteRecordEntity::getOwnerId, ownerId))
                .stream()
                .map(VoteRecordEntity::getOptionId)
                .collect(Collectors.toList());
        vo.setMyVotedOptionIds(votedOptionIds);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void castVote(Long voteId, VoteCastRequest request, Long ownerId) {
        CommunityVoteEntity vote = getByIdOrThrow(voteId);

        VoteStatusEnum statusEnum = VoteStatusEnum.fromValue(vote.getStatus());
        if (statusEnum == null || !statusEnum.canVote()) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "当前投票不可投票 [status=" + vote.getStatus() + "]");
        }
        LocalDateTime now = LocalDateTime.now();
        if (vote.getStartTime() != null && now.isBefore(vote.getStartTime())) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "投票尚未开始");
        }
        if (vote.getEndTime() != null && now.isAfter(vote.getEndTime())) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "投票已结束");
        }

        List<Long> optionIds = request.getOptionIds().stream()
                .distinct()
                .collect(Collectors.toList());
        if (Integer.valueOf(VOTE_TYPE_SINGLE).equals(vote.getVoteType()) && optionIds.size() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "单选投票只能选择一个选项");
        }

        // 校验选项都属于该投票
        long validCount = voteOptionMapper.selectCount(
                new LambdaQueryWrapper<VoteOptionEntity>()
                        .eq(VoteOptionEntity::getVoteId, voteId)
                        .in(VoteOptionEntity::getId, optionIds));
        if (validCount != optionIds.size()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "存在无效的投票选项");
        }

        // 查重：同一投票同一用户
        List<VoteRecordEntity> existing = voteRecordMapper.selectList(
                new LambdaQueryWrapper<VoteRecordEntity>()
                        .eq(VoteRecordEntity::getVoteId, voteId)
                        .eq(VoteRecordEntity::getOwnerId, ownerId));
        Set<Long> votedOptionIds = existing.stream()
                .map(VoteRecordEntity::getOptionId)
                .collect(Collectors.toSet());

        List<Long> toInsert = optionIds.stream()
                .filter(oid -> !votedOptionIds.contains(oid))
                .collect(Collectors.toList());
        if (toInsert.isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "您已参与该投票");
        }

        for (Long optionId : toInsert) {
            VoteRecordEntity record = new VoteRecordEntity();
            record.setVoteId(voteId);
            record.setOptionId(optionId);
            record.setOwnerId(ownerId);
            voteRecordMapper.insert(record);
            voteOptionMapper.incrVoteCount(optionId);
        }
    }

    @Override
    public VoteResultVO getResult(Long id) {
        CommunityVoteEntity vote = getByIdOrThrow(id);

        List<VoteOptionEntity> options = voteOptionMapper.selectList(
                new LambdaQueryWrapper<VoteOptionEntity>()
                        .eq(VoteOptionEntity::getVoteId, id)
                        .orderByAsc(VoteOptionEntity::getSortOrder));

        VoteResultVO result = new VoteResultVO();
        result.setVoteId(vote.getId());
        result.setTitle(vote.getTitle());
        result.setVoteType(vote.getVoteType());
        result.setIsAnonymous(vote.getIsAnonymous());
        result.setTotalVotes(options.stream().mapToInt(o -> o.getVoteCount() == null ? 0 : o.getVoteCount()).sum());

        // 实名投票：一次性查询所有投票记录并按选项分组
        Map<Long, List<Long>> ownerByOption = Collections.emptyMap();
        if (vote.getIsAnonymous() != null && vote.getIsAnonymous() != ANONYMOUS) {
            ownerByOption = voteRecordMapper.selectList(
                            new LambdaQueryWrapper<VoteRecordEntity>()
                                    .eq(VoteRecordEntity::getVoteId, id))
                    .stream()
                    .collect(Collectors.groupingBy(VoteRecordEntity::getOptionId,
                            Collectors.mapping(VoteRecordEntity::getOwnerId, Collectors.toList())));
        }

        List<VoteResultOptionVO> resultOptions = new ArrayList<>();
        for (VoteOptionEntity option : options) {
            VoteResultOptionVO ro = new VoteResultOptionVO();
            ro.setOptionId(option.getId());
            ro.setContent(option.getContent());
            ro.setVoteCount(option.getVoteCount());
            ro.setOwnerIds(ownerByOption.getOrDefault(option.getId(), Collections.emptyList()));
            resultOptions.add(ro);
        }
        result.setOptions(resultOptions);
        return result;
    }

    private void validateCreate(VoteCreateRequest request) {
        if (request.getVoteType() == null || (request.getVoteType() != VOTE_TYPE_SINGLE && request.getVoteType() != VOTE_TYPE_MULTI)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "投票类型仅支持：1单选 2多选");
        }
        if (request.getIsAnonymous() == null || (request.getIsAnonymous() != 0 && request.getIsAnonymous() != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "是否匿名仅支持：1匿名 0实名");
        }
        if (request.getEndTime() != null && request.getStartTime() != null
                && !request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束时间必须晚于开始时间");
        }
        if (request.getOptions() == null || request.getOptions().size() < 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "投票选项至少需要两项");
        }
    }

    private int resolveInitialStatus(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (startTime != null && now.isBefore(startTime)) {
            return VoteStatusEnum.NOT_STARTED.getValue();
        }
        if (endTime != null && now.isAfter(endTime)) {
            return VoteStatusEnum.ENDED.getValue();
        }
        return VoteStatusEnum.IN_PROGRESS.getValue();
    }

    private LambdaQueryWrapper<CommunityVoteEntity> buildQueryWrapper(VoteQuery query) {
        return new LambdaQueryWrapper<CommunityVoteEntity>()
                .eq(query.getStatus() != null, CommunityVoteEntity::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getTitle()), CommunityVoteEntity::getTitle, query.getTitle())
                .orderByDesc(CommunityVoteEntity::getCreateTime);
    }

    private VoteDetailVO buildDetail(CommunityVoteEntity entity) {
        VoteDetailVO vo = voteConverter.toDetailVO(entity);
        fillNames(vo);

        List<VoteOptionEntity> options = voteOptionMapper.selectList(
                new LambdaQueryWrapper<VoteOptionEntity>()
                        .eq(VoteOptionEntity::getVoteId, entity.getId())
                        .orderByAsc(VoteOptionEntity::getSortOrder));
        vo.setOptions(options.stream().map(voteConverter::toOptionVO).collect(Collectors.toList()));
        vo.setTotalVotes(options.stream().mapToInt(o -> o.getVoteCount() == null ? 0 : o.getVoteCount()).sum());
        return vo;
    }

    private void fillNames(VoteVO vo) {
        vo.setVoteTypeName(voteTypeName(vo.getVoteType()));
        vo.setStatusName(Optional.ofNullable(vo.getStatus())
                .map(VoteStatusEnum::fromValue)
                .map(VoteStatusEnum::getLabel)
                .orElse(null));
    }

    private void fillNames(VoteDetailVO vo) {
        vo.setVoteTypeName(voteTypeName(vo.getVoteType()));
        vo.setStatusName(Optional.ofNullable(vo.getStatus())
                .map(VoteStatusEnum::fromValue)
                .map(VoteStatusEnum::getLabel)
                .orElse(null));
    }

    private String voteTypeName(Integer type) {
        return type != null && type == VOTE_TYPE_MULTI ? "多选" : "单选";
    }

    private CommunityVoteEntity getByIdOrThrow(Long id) {
        CommunityVoteEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "投票不存在");
        }
        return entity;
    }
}
