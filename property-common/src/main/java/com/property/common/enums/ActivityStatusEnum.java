package com.property.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * 社区活动状态枚举
 */
@Getter
@AllArgsConstructor
public enum ActivityStatusEnum {

    DRAFT(0, "草稿"),
    RECRUITING(1, "招募中"),
    FULL(2, "已满员"),
    IN_PROGRESS(3, "进行中"),
    FINISHED(4, "已结束"),
    CANCELED(5, "已取消");

    private final Integer value;
    private final String label;

    public static ActivityStatusEnum fromValue(Integer value) {
        if (value == null) return null;
        for (ActivityStatusEnum s : values()) {
            if (s.value.equals(value)) return s;
        }
        return null;
    }

    public static Optional<ActivityStatusEnum> fromValueSafe(Integer value) {
        return Optional.ofNullable(fromValue(value));
    }

    /** 是否可报名 */
    public boolean canSignup() {
        return this == RECRUITING;
    }

    /** 是否可取消（管理端） */
    public boolean canCancel() {
        return this == DRAFT || this == RECRUITING;
    }

    /** 是否可编辑 */
    public boolean canEdit() {
        return this == DRAFT || this == RECRUITING;
    }

    /** 是否可删除 */
    public boolean canDelete() {
        return this == DRAFT;
    }
}