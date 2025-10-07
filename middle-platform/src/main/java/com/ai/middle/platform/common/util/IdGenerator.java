package com.ai.middle.platform.common.util;

/**
 * ID生成工具（兼容Hutool工具类调用）
 */
public final class IdGenerator {

    private IdGenerator() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 生成简单UUID（去掉横线）
     */
    public static String simpleUUID() {
        return cn.hutool.core.util.IdUtil.simpleUUID();
    }

    /**
     * 生成雪花ID字符串
     */
    public static String snowflakeId() {
        return String.valueOf(cn.hutool.core.util.IdUtil.getSnowflakeNextId());
    }
}
