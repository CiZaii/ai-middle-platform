package com.ai.middle.platform.common.enums;

import lombok.Getter;

@Getter
public enum UserRole {
    ADMIN("admin", "管理员"),
    USER("user", "普通用户");

    private final String code;
    private final String desc;

    UserRole(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
