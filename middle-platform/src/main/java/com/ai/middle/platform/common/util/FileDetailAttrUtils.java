package com.ai.middle.platform.common.util;

import com.ai.middle.platform.entity.po.FileDetailAttributes;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

/**
 * 工具类，用于序列化和反序列化 file_detail.attr 字段。
 */
@UtilityClass
public class FileDetailAttrUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public FileDetailAttributes parse(String json) {
        if (!StringUtils.hasText(json)) {
            return new FileDetailAttributes();
        }
        try {
            return MAPPER.readValue(json, FileDetailAttributes.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse file detail attributes", ex);
        }
    }

    public String toJson(FileDetailAttributes attributes) {
        if (attributes == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attributes);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize file detail attributes", ex);
        }
    }
}
