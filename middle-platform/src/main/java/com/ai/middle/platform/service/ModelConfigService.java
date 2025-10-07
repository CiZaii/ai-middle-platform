package com.ai.middle.platform.service;

import com.ai.middle.platform.entity.po.ModelApiKey;
import com.ai.middle.platform.entity.po.ModelBusiness;
import com.ai.middle.platform.entity.po.ModelEndpoint;
import com.ai.middle.platform.entity.po.ModelInfo;

import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service that resolves model configuration from the database for dynamic provider management.
 */
public interface ModelConfigService {

    /**
     * Resolve the model configuration (base URL, key, provider, model name) for a business.
     *
     * @param businessCode business identifier such as "ocr", "vectorization"
     * @return ordered array [baseUrl, apiKey, provider, modelName]
     */
    String[] getModelConfig(String businessCode);

    /**
     * Resolve the full runtime configuration for a business without excluding any keys.
     */
    default ModelRuntimeConfig getRuntimeConfig(String businessCode) {
        return getRuntimeConfig(businessCode, Collections.emptySet());
    }

    /**
     * Resolve the runtime configuration for a business while avoiding specific key IDs.
     */
    ModelRuntimeConfig getRuntimeConfig(String businessCode, Set<String> excludedKeyIds);

    /**
     * Locate an available API key for the endpoint.
     */
    default ModelApiKey getAvailableApiKey(Long endpointId) {
        return getAvailableApiKey(endpointId, Collections.emptySet());
    }

    /**
     * Locate an available API key for the endpoint while excluding specific keys.
     */
    ModelApiKey getAvailableApiKey(Long endpointId, Set<String> excludedKeyIds);

    /**
     * Update usage statistics for the API key.
     */
    void recordApiKeyUsage(String keyId, boolean success, String error);

    /**
     * Immutable runtime configuration resolved from the database.
     */
    record ModelRuntimeConfig(ModelBusiness business,
                              ModelEndpoint endpoint,
                              ModelApiKey apiKey,
                              ModelInfo modelInfo) {

        private static final String DEFAULT_CHAT_MODEL = "gpt-4o";
        private static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-3-small";
        private static final String DEFAULT_BASE_URL = "https://api.openai.com";
        private static final Pattern SCHEME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$");

        public String provider() {
            return endpoint != null ? endpoint.getProvider() : null;
        }

        public String baseUrl() {
            String explicit = endpoint != null ? endpoint.getBaseUrl() : null;
            String candidate = (explicit != null && !explicit.isBlank()) ? explicit.trim() : DEFAULT_BASE_URL;
            return normalizeBaseUrl(candidate);
        }

        public String modelNameOrDefault(String fallback) {
            if (modelInfo != null && modelInfo.getName() != null && !modelInfo.getName().isBlank()) {
                return modelInfo.getName();
            }
            return fallback;
        }

        public String chatModelName() {
            return modelNameOrDefault(DEFAULT_CHAT_MODEL);
        }

        public String embeddingModelName() {
            return modelNameOrDefault(DEFAULT_EMBEDDING_MODEL);
        }

        private String normalizeBaseUrl(String url) {
            String value = url != null ? url.trim() : "";
            if (value.isEmpty()) {
                return DEFAULT_BASE_URL;
            }
            if (value.startsWith("//")) {
                return "https:" + value;
            }
            if (!SCHEME_PATTERN.matcher(value).matches()) {
                return "https://" + value;
            }
            return value;
        }
    }
}
