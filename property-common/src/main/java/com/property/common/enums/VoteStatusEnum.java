package com.property.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * 社区投票状态枚举
 */
@Getter
@AllArgsConstructor
public enum VoteStatusEnum {

    NOT_STARTED(0, "未开始"),
    IN_PROGRESS(1, "进行中"),
    ENDED(2, "已结束");

    private final Integer value;
    private final String label;

    public static VoteStatusEnum fromValue(Integer value) {
        if (value == null) return null;
        for (VoteStatusEnum s : values()) {
            if (s.value.equals(value)) return s;
        }
        return null;
    }

    public static Optional<VoteStatusEnum> fromValueSafe(Integer value) {
        return Optional.ofNullable(fromValue(value));
    }

    /** 是否可投票 */
    public boolean canVote() {
        return this == IN_PROGRESS;
    }

    /** 是否可开始 */
    public boolean canStart() {
        return this == NOT_STARTED;
    }

    /** 是否可结束 */
    public boolean canEnd() {
        return this == IN_PROGRESS;
    }
}
