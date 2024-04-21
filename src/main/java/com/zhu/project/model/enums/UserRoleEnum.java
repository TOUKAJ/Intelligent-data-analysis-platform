package com.zhu.project.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;

/**
 * 用户角色枚举
 */

public enum UserRoleEnum {

    USER("用户", "user"),
    ADMIN("管理员", "admin");


    private final String text;

    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     * @param value
     * @return
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum anEnum : UserRoleEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
