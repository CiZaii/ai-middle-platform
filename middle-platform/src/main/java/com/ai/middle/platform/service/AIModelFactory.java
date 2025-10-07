package com.ai.middle.platform.service;

import com.ai.middle.platform.common.exception.BusinessException;
import com.ai.middle.platform.entity.po.ModelApiKey;
import com.ai.middle.platform.service.ModelConfigService.ModelRuntimeConfig;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Locale;

/**
 * Factory that builds LangChain4j model clients dynamically based on database configuration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AIModelFactory {

    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final String DEFAULT_AZURE_API_VERSION = "2024-02-15-preview";

    private final ModelConfigService modelConfigService;

    public ChatModelContext createChatModelContext(String businessCode) {
        ModelRuntimeConfig runtimeConfig = modelConfigService.getRuntimeConfig(businessCode);
        return createChatModelContext(runtimeConfig);
    }

    public ChatModelContext createChatModelContext(ModelRuntimeConfig runtimeConfig) {
        ChatLanguageModel chatModel = buildChatModel(runtimeConfig);
        return new ChatModelContext(chatModel, runtimeConfig);
    }

    public EmbeddingModelContext createEmbeddingModelContext(String businessCode) {
        ModelRuntimeConfig runtimeConfig = modelConfigService.getRuntimeConfig(businessCode);
        return createEmbeddingModelContext(runtimeConfig);
    }

    public EmbeddingModelContext createEmbeddingModelContext(ModelRuntimeConfig runtimeConfig) {
        EmbeddingModel embeddingModel = buildEmbeddingModel(runtimeConfig);
        return new EmbeddingModelContext(embeddingModel, runtimeConfig);
    }

    private ChatLanguageModel buildChatModel(ModelRuntimeConfig runtimeConfig) {
        String provider = normalizeProvider(runtimeConfig.provider());
        return switch (provider) {
            case "azure", "azure-openai" -> buildAzureChatModel(runtimeConfig);
            default -> buildOpenAiChatModel(runtimeConfig);
        };
    }

    private EmbeddingModel buildEmbeddingModel(ModelRuntimeConfig runtimeConfig) {
        String provider = normalizeProvider(runtimeConfig.provider());
        return switch (provider) {
            case "azure", "azure-openai" -> buildAzureEmbeddingModel(runtimeConfig);
            default -> buildOpenAiEmbeddingModel(runtimeConfig);
        };
    }

    private ChatLanguageModel buildOpenAiChatModel(ModelRuntimeConfig runtimeConfig) {
        ModelApiKey apiKey = requireApiKey(runtimeConfig);
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(apiKey.getApiKey().trim())
                .timeout(Duration.ofHours(1))
                .modelName(runtimeConfig.chatModelName())
                .temperature(DEFAULT_TEMPERATURE);

        String baseUrl = ensureOpenAiBaseUrl(runtimeConfig.baseUrl());
        if (StringUtils.hasText(baseUrl)) {
            builder.baseUrl(baseUrl);
        }
        return builder.build();
    }

    private ChatLanguageModel buildAzureChatModel(ModelRuntimeConfig runtimeConfig) {
        ModelApiKey apiKey = requireApiKey(runtimeConfig);
        AzureEndpoint endpoint = resolveAzureEndpoint(runtimeConfig.baseUrl());

        AzureOpenAiChatModel.Builder builder = AzureOpenAiChatModel.builder()
                .endpoint(endpoint.url())
                .deploymentName(runtimeConfig.chatModelName())
                .apiKey(apiKey.getApiKey().trim())
                .temperature(DEFAULT_TEMPERATURE);

        String apiVersion = endpoint.apiVersion();
        if (StringUtils.hasText(apiVersion)) {
            builder.serviceVersion(apiVersion);
        }
        return builder.build();
    }

    private EmbeddingModel buildOpenAiEmbeddingModel(ModelRuntimeConfig runtimeConfig) {
        ModelApiKey apiKey = requireApiKey(runtimeConfig);
        OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .apiKey(apiKey.getApiKey().trim())
                .modelName(runtimeConfig.embeddingModelName());

        String baseUrl = ensureOpenAiBaseUrl(runtimeConfig.baseUrl());
        if (StringUtils.hasText(baseUrl)) {
            builder.baseUrl(baseUrl);
        }
        return builder.build();
    }

    private EmbeddingModel buildAzureEmbeddingModel(ModelRuntimeConfig runtimeConfig) {
        ModelApiKey apiKey = requireApiKey(runtimeConfig);
        AzureEndpoint endpoint = resolveAzureEndpoint(runtimeConfig.baseUrl());

        AzureOpenAiEmbeddingModel.Builder builder = AzureOpenAiEmbeddingModel.builder()
                .endpoint(endpoint.url())
                .deploymentName(runtimeConfig.embeddingModelName())
                .apiKey(apiKey.getApiKey().trim());

        String apiVersion = endpoint.apiVersion();
        if (StringUtils.hasText(apiVersion)) {
            builder.serviceVersion(apiVersion);
        }
        return builder.build();
    }

    private ModelApiKey requireApiKey(ModelRuntimeConfig runtimeConfig) {
        ModelApiKey apiKey = runtimeConfig.apiKey();
        if (apiKey == null || !StringUtils.hasText(apiKey.getApiKey())) {
            throw new BusinessException("No API key available for provider: " + runtimeConfig.provider());
        }
        return apiKey;
    }

    private AzureEndpoint resolveAzureEndpoint(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new BusinessException("Azure endpoint base URL is not configured");
        }
        try {
            UriComponents components = UriComponentsBuilder.fromUriString(baseUrl.trim()).build();

            UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                    .scheme(components.getScheme())
                    .host(components.getHost());
            if (components.getPort() != -1) {
                builder.port(components.getPort());
            }
            if (StringUtils.hasText(components.getPath())) {
                builder.path(components.getPath());
            }
            String endpoint = trimTrailingSlash(builder.build().toUriString());

            String apiVersion = components.getQueryParams().getFirst("api-version");
            if (!StringUtils.hasText(apiVersion)) {
                apiVersion = DEFAULT_AZURE_API_VERSION;
            }
            return new AzureEndpoint(endpoint, apiVersion);
        } catch (Exception ex) {
            log.warn("Failed to parse Azure endpoint URL '{}': {}", baseUrl, ex.getMessage());
            return new AzureEndpoint(baseUrl.trim(), DEFAULT_AZURE_API_VERSION);
        }
    }

    private String normalizeProvider(String provider) {
        return StringUtils.hasText(provider) ? provider.trim().toLowerCase(Locale.ROOT) : "openai";
    }

    private String ensureOpenAiBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return baseUrl;
        }
        String trimmed = baseUrl.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        try {
            UriComponents components = UriComponentsBuilder.fromUriString(trimmed).build();
            List<String> segments = components.getPathSegments();
            if (segments.stream().anyMatch(segment -> "v1".equalsIgnoreCase(segment))) {
                return trimmed;
            }
            return trimmed + "/v1";
        } catch (Exception ex) {
            log.debug("Failed to parse baseUrl '{}', defaulting with /v1 suffix: {}", baseUrl, ex.getMessage());
            return trimmed.endsWith("/v1") ? trimmed : trimmed + "/v1";
        }
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public record ChatModelContext(ChatLanguageModel chatModel, ModelRuntimeConfig runtimeConfig) {
    }

    public record EmbeddingModelContext(EmbeddingModel embeddingModel, ModelRuntimeConfig runtimeConfig) {
    }

    private record AzureEndpoint(String url, String apiVersion) {
    }
}
