package com.ai.middle.platform.service.processing;

import java.util.List;

public interface TagProcessor {
    List<String> generate(String fileId, String fileName, String content);
}

