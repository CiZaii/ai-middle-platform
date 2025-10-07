package com.ai.middle.platform.common.enums;

import lombok.Getter;

@Getter
public enum MemberRole {
    OWNER("owner", "所有者"),
    EDITOR("editor", "编辑者"),
    VIEWER("viewer", "查看者");

    private final String code;
    private final String desc;

    MemberRole(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
