package com.ai.middle.platform.common.enums;

import lombok.Getter;

@Getter
public enum FileType {
    PDF("pdf", "PDF文档"),
    WORD("word", "Word文档"),
    IMAGE("image", "图片");

    private final String code;
    private final String desc;

    FileType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
