package com.ai.middle.platform.common.enums;

import lombok.Getter;

@Getter
public enum ProcessingStatus {
    PENDING("pending", "待处理"),
    PROCESSING("processing", "处理中"),
    COMPLETED("completed", "已完成"),
    FAILED("failed", "失败");

    private final String code;
    private final String desc;

    ProcessingStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
