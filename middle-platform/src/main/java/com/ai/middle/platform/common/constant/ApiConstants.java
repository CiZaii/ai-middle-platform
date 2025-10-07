package com.ai.middle.platform.common.constant;

/**
 * API常量类
 */
public final class ApiConstants {

    private ApiConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final String API_PREFIX = "/api";
    public static final String AUTH_PATH = API_PREFIX + "/auth";
    public static final String KB_PATH = API_PREFIX + "/knowledge-bases";
    public static final String FILE_PATH = API_PREFIX + "/files";
    public static final String MODEL_CONFIG_PATH = "/api/model";
    public static final String JWT_HEADER = "Authorization";
    public static final String JWT_PREFIX = "Bearer ";
}
