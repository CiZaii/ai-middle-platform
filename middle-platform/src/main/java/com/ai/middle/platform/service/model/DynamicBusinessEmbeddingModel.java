package com.ai.middle.platform.service.model;

import com.ai.middle.platform.common.exception.BusinessException;
import com.ai.middle.platform.service.AIModelFactory;
import com.ai.middle.platform.service.ModelConfigService;
import com.ai.middle.platform.service.ModelConfigService.ModelRuntimeConfig;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Embedding model that fetches configuration dynamically for a specific business and
 * performs simple failover across API keys.
 */
@Slf4j
public class DynamicBusinessEmbeddingModel implements EmbeddingModel {

    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    private final AIModelFactory aiModelFactory;
    private final ModelConfigService modelConfigService;
    private final String businessCode;
    private final int maxAttempts;

    public DynamicBusinessEmbeddingModel(AIModelFactory aiModelFactory,
                                         ModelConfigService modelConfigService,
                                         String businessCode) {
        this(aiModelFactory, modelConfigService, businessCode, DEFAULT_MAX_ATTEMPTS);
    }

    public DynamicBusinessEmbeddingModel(AIModelFactory aiModelFactory,
                                         ModelConfigService modelConfigService,
                                         String businessCode,
                                         int maxAttempts) {
        this.aiModelFactory = aiModelFactory;
        this.modelConfigService = modelConfigService;
        this.businessCode = businessCode;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
        return executeWithFailover(model -> model.embedAll(segments));
    }

    private <T> T executeWithFailover(EmbeddingInvocation<T> invocation) {
        Set<String> attempted = new LinkedHashSet<>();
        RuntimeException lastError = null;

        while (attempted.size() < maxAttempts) {
            ModelRuntimeConfig runtimeConfig;
            try {
                runtimeConfig = modelConfigService.getRuntimeConfig(businessCode, attempted);
            } catch (BusinessException ex) {
                if (lastError != null) {
                    throw lastError;
                }
                throw ex;
            }

            AIModelFactory.EmbeddingModelContext context = aiModelFactory.createEmbeddingModelContext(runtimeConfig);
            try {
                T result = invocation.invoke(context.embeddingModel());
                modelConfigService.recordApiKeyUsage(runtimeConfig.apiKey().getKeyId(), true, null);
                return result;
            } catch (RuntimeException ex) {
                modelConfigService.recordApiKeyUsage(runtimeConfig.apiKey().getKeyId(), false, ex.getMessage());
                attempted.add(runtimeConfig.apiKey().getKeyId());
                lastError = ex;
                log.warn("Embedding request failed for provider {} key {}, attempting failover", runtimeConfig.provider(), runtimeConfig.apiKey().getDisplayKey());
            }
        }

        if (lastError != null) {
            throw lastError;
        }
        throw new BusinessException("No available API key for business: " + businessCode);
    }

    @FunctionalInterface
    private interface EmbeddingInvocation<T> {
        T invoke(EmbeddingModel embeddingModel);
    }
}
