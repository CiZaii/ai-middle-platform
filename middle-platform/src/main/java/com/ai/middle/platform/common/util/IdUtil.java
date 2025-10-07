package com.ai.middle.platform.common.util;

/**
 * ID生成工具类
 */
public final class IdUtil {

    private IdUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 生成UUID（去掉横线）
     */
    public static String simpleUUID() {
        return cn.hutool.core.util.IdUtil.simpleUUID();
    }

    /**
     * 生成雪花ID
     */
    public static String snowflakeId() {
        return String.valueOf(cn.hutool.core.util.IdUtil.getSnowflakeNextId());
    }
}
