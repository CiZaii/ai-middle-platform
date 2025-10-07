package com.ai.middle.platform.service.model;

import com.ai.middle.platform.client.EmbeddingHttpClient;
import com.ai.middle.platform.common.exception.BusinessException;
import com.ai.middle.platform.dto.embedding.EmbeddingReqDTO;
import com.ai.middle.platform.dto.embedding.EmbeddingRespDTO;
import com.ai.middle.platform.entity.po.ModelApiKey;
import com.ai.middle.platform.service.ModelConfigService;
import com.ai.middle.platform.service.ModelConfigService.ModelRuntimeConfig;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Embedding model implementation that dynamically resolves provider configuration
 * from the database and calls embedding endpoints through the Forest HTTP client.
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class DynamicEmbeddingModelHandler implements EmbeddingModel {

    private static final String DEFAULT_BUSINESS_CODE = "vectorization";
    private static final int MAX_KEY_ATTEMPTS = 5;

    private final EmbeddingHttpClient embeddingHttpClient;
    private final ModelConfigService modelConfigService;

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
        if (CollectionUtils.isEmpty(segments)) {
            return Response.from(Collections.emptyList());
        }

        List<String> inputs = segments.stream()
                .map(TextSegment::text)
                .map(text -> StringUtils.hasText(text) ? text : "")
                .toList();

        List<Embedding> embeddings = executeWithFailover(runtimeConfig ->
                createEmbeddings(inputs, runtimeConfig));
        return Response.from(embeddings);
    }

    private List<Embedding> createEmbeddings(List<String> instructions, ModelRuntimeConfig runtimeConfig) {
        EmbeddingInvocationContext invocationContext = resolveInvocationContext(runtimeConfig);
        List<Embedding> embeddings = new ArrayList<>();

        for (String instruction : instructions) {
            if (!StringUtils.hasText(instruction)) {
                embeddings.add(Embedding.from(new float[0]));
                continue;
            }

            EmbeddingReqDTO request = new EmbeddingReqDTO();
            request.setInput(instruction);
            request.setModel(invocationContext.model());

            EmbeddingRespDTO response = invokeEmbeddingApi(invocationContext, request);
            if (CollectionUtils.isEmpty(response.getData())) {
                throw new RuntimeException("No embedding data returned by provider " + invocationContext.provider());
            }

            for (EmbeddingRespDTO.EmbeddingData data : response.getData()) {
                float[] vector = convertToFloatArray(data.getEmbedding());
                embeddings.add(Embedding.from(vector));
            }
        }

        log.info("Created {} embeddings via provider {} model {}", embeddings.size(),
                invocationContext.provider(), invocationContext.model());
        return embeddings;
    }

    private EmbeddingRespDTO invokeEmbeddingApi(EmbeddingInvocationContext invocationContext,
                                                EmbeddingReqDTO request) {
        try {
            return embeddingHttpClient.createEmbedding(
                    invocationContext.url(),
                    invocationContext.authorization(),
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE,
                    request
            );
        } catch (Exception ex) {
            throw new RuntimeException("Embedding API call failed: " + ex.getMessage(), ex);
        }
    }

    private EmbeddingInvocationContext resolveInvocationContext(ModelRuntimeConfig runtimeConfig) {
        String url = buildEmbeddingUrl(runtimeConfig.baseUrl(), runtimeConfig.provider());
        String authorization = buildAuthorizationHeader(runtimeConfig);
        String model = runtimeConfig.embeddingModelName();
        String provider = runtimeConfig.provider();
        log.debug("Resolved embedding context: provider={}, model={}, url={}", provider, model, url);
        return new EmbeddingInvocationContext(url, authorization, model, provider);
    }

    private String buildEmbeddingUrl(String baseUrl, String provider) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new BusinessException("Embedding base URL is not configured for business: " + DEFAULT_BUSINESS_CODE);
        }
        String normalized = baseUrl.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.contains("/embeddings")) {
            return normalized;
        }
        return normalized + "/v1/embeddings";
    }

    private String buildAuthorizationHeader(ModelRuntimeConfig runtimeConfig) {
        ModelApiKey apiKey = runtimeConfig.apiKey();
        if (apiKey == null || !StringUtils.hasText(apiKey.getApiKey())) {
            throw new BusinessException("No API key available for business: " + DEFAULT_BUSINESS_CODE);
        }
        return "Bearer " + apiKey.getApiKey().trim();
    }

    private float[] convertToFloatArray(List<Double> values) {
        if (CollectionUtils.isEmpty(values)) {
            return new float[0];
        }
        float[] array = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Double value = values.get(i);
            array[i] = value != null ? value.floatValue() : 0F;
        }
        return array;
    }

    private <T> T executeWithFailover(Function<ModelRuntimeConfig, T> operation) {
        Set<String> attemptedKeys = new LinkedHashSet<>();
        RuntimeException lastError = null;

        for (int attempt = 0; attempt < MAX_KEY_ATTEMPTS; attempt++) {
            ModelRuntimeConfig runtimeConfig;
            try {
                runtimeConfig = modelConfigService.getRuntimeConfig(DEFAULT_BUSINESS_CODE, attemptedKeys);
            } catch (BusinessException ex) {
                if (lastError != null) {
                    throw lastError;
                }
                throw ex;
            }

            ModelApiKey apiKey = runtimeConfig.apiKey();
            String keyId = apiKey != null ? apiKey.getKeyId() : null;

            try {
                T result = operation.apply(runtimeConfig);
                if (StringUtils.hasText(keyId)) {
                    modelConfigService.recordApiKeyUsage(keyId, true, null);
                }
                return result;
            } catch (RuntimeException ex) {
                if (StringUtils.hasText(keyId)) {
                    modelConfigService.recordApiKeyUsage(keyId, false, ex.getMessage());
                    attemptedKeys.add(keyId);
                }
                lastError = ex;
                log.warn("Embedding request failed for provider {} key {}: {}",
                        runtimeConfig.provider(),
                        apiKey != null ? apiKey.getDisplayKey() : "n/a",
                        ex.getMessage());
            }
        }

        if (lastError != null) {
            throw lastError;
        }
        throw new BusinessException("No available API key for business: " + DEFAULT_BUSINESS_CODE);
    }

    private record EmbeddingInvocationContext(String url, String authorization, String model, String provider) {
    }
}
