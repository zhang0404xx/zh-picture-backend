package com.zh.zhpicturebackend.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

@Getter
public enum UserRoleEnum {

    USER("user","用户"),
    ADMIN("admin","管理员");

    private final String text;

    private final String value;

    UserRoleEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值
     * @return 对应的枚举，未匹配返回 null
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum roleEnum : UserRoleEnum.values()) {
            if (roleEnum.getValue().equals(value)) {
                return roleEnum;
            }
        }
        return null;
    }

    /**
     * 根据 text 获取枚举
     *
     * @param text 枚举描述
     * @return 对应的枚举，未匹配返回 null
     */
    public static UserRoleEnum getEnumByText(String text) {
        if (ObjectUtils.isEmpty(text)) {
            return null;
        }
        for (UserRoleEnum roleEnum : UserRoleEnum.values()) {
            if (roleEnum.getText().equals(text)) {
                return roleEnum;
            }
        }
        return null;
    }
}
