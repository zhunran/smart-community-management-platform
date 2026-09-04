package com.property.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * 帖子分类枚举
 */
@Getter
@AllArgsConstructor
public enum PostCategoryEnum {

    SECOND_HAND(1, "二手转让"),
    LOST_FOUND(2, "失物招领"),
    DECORATION(3, "装修推荐"),
    NEIGHBOR_HELP(4, "邻里互助"),
    OTHER(5, "其他");

    private final Integer value;
    private final String label;

    public static PostCategoryEnum fromValue(Integer value) {
        if (value == null) return null;
        for (PostCategoryEnum s : values()) {
            if (s.value.equals(value)) return s;
        }
        return null;
    }

    public static Optional<PostCategoryEnum> fromValueSafe(Integer value) {
        return Optional.ofNullable(fromValue(value));
    }
}