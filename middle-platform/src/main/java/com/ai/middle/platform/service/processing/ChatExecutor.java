package com.ai.middle.platform.service.processing;

import com.ai.middle.platform.common.exception.BusinessException;
import com.ai.middle.platform.service.AIModelFactory;
import com.ai.middle.platform.service.ModelConfigService;
import com.ai.middle.platform.service.ModelConfigService.ModelRuntimeConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.LinkedHashSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatExecutor {

    private static final int MAX_KEY_ATTEMPTS = 5;

    private final AIModelFactory aiModelFactory;
    private final ModelConfigService modelConfigService;

    @FunctionalInterface
    public interface ChatOperation<T> {
        T execute(ChatLanguageModel chatModel, ModelRuntimeConfig runtimeConfig);
    }

    public <T> T execute(String businessCode, ChatOperation<T> operation) {
        LinkedHashSet<String> attemptedKeys = new LinkedHashSet<>();
        RuntimeException lastError = null;

        for (int attempt = 0; attempt < MAX_KEY_ATTEMPTS; attempt++) {
            ModelRuntimeConfig runtimeConfig;
            try {
                runtimeConfig = modelConfigService.getRuntimeConfig(businessCode, attemptedKeys);
            } catch (BusinessException ex) {
                if (lastError != null) {
                    throw lastError;
                }
                throw ex;
            }

            AIModelFactory.ChatModelContext context = aiModelFactory.createChatModelContext(runtimeConfig);
            String keyId = runtimeConfig.apiKey() != null ? runtimeConfig.apiKey().getKeyId() : null;

            try {
                T result = operation.execute(context.chatModel(), runtimeConfig);
                recordApiKeyUsage(runtimeConfig, true, null);
                return result;
            } catch (RuntimeException ex) {
                recordApiKeyUsage(runtimeConfig, false, ex.getMessage());
                if (keyId != null) {
                    attemptedKeys.add(keyId);
                }
                lastError = ex;
                log.warn("Chat operation failed for provider {} key {}: {}", runtimeConfig.provider(),
                        runtimeConfig.apiKey() != null ? runtimeConfig.apiKey().getDisplayKey() : "n/a",
                        ex.getMessage());
            }
        }

        throw lastError;
    }

    private void recordApiKeyUsage(ModelRuntimeConfig runtimeConfig, boolean success, String error) {
        if (runtimeConfig.apiKey() == null || !StringUtils.hasText(runtimeConfig.apiKey().getKeyId())) {
            return;
        }
        modelConfigService.recordApiKeyUsage(runtimeConfig.apiKey().getKeyId(), success, error);
    }
}

