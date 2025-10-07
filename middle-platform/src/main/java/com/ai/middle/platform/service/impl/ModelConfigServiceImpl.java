package com.ai.middle.platform.service.impl;

import com.ai.middle.platform.common.exception.BusinessException;
import com.ai.middle.platform.entity.po.ModelApiKey;
import com.ai.middle.platform.entity.po.ModelBusiness;
import com.ai.middle.platform.entity.po.ModelEndpoint;
import com.ai.middle.platform.entity.po.ModelEndpointBusiness;
import com.ai.middle.platform.entity.po.ModelInfo;
import com.ai.middle.platform.repository.mapper.ModelApiKeyMapper;
import com.ai.middle.platform.repository.mapper.ModelBusinessMapper;
import com.ai.middle.platform.repository.mapper.ModelEndpointBusinessMapper;
import com.ai.middle.platform.repository.mapper.ModelEndpointMapper;
import com.ai.middle.platform.repository.mapper.ModelInfoMapper;
import com.ai.middle.platform.service.ModelConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Reads model configuration from the database and provides load-balanced access to API keys.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelConfigServiceImpl implements ModelConfigService {

    private static final Comparator<ModelApiKey> KEY_PRIORITY_COMPARATOR = Comparator
            .comparing((ModelApiKey key) -> Optional.ofNullable(key.getTotalRequests()).orElse(0L))
            .thenComparing(key -> Optional.ofNullable(key.getLastUsedAt()).orElse(LocalDateTime.MIN));

    private final ModelBusinessMapper businessMapper;
    private final ModelEndpointBusinessMapper endpointBusinessMapper;
    private final ModelEndpointMapper endpointMapper;
    private final ModelApiKeyMapper apiKeyMapper;
    private final ModelInfoMapper modelInfoMapper;

    @Override
    public String[] getModelConfig(String businessCode) {
        ModelRuntimeConfig config = getRuntimeConfig(businessCode);
        String baseUrl = config.baseUrl();
        String apiKey = config.apiKey() != null ? config.apiKey().getApiKey() : null;
        String provider = config.provider();
        String modelName = config.embeddingModelName();
        return new String[]{baseUrl, apiKey, provider, modelName};
    }

    @Override
    public ModelRuntimeConfig getRuntimeConfig(String businessCode, Set<String> excludedKeyIds) {
        if (!StringUtils.hasText(businessCode)) {
            throw new BusinessException("Business code must not be blank");
        }

        ModelBusiness business = businessMapper.selectOne(
                new LambdaQueryWrapper<ModelBusiness>()
                        .eq(ModelBusiness::getCode, businessCode)
                        .last("LIMIT 1"));
        if (business == null || Boolean.FALSE.equals(business.getEnabled())) {
            throw new BusinessException("Business not available: " + businessCode);
        }

        List<ModelEndpointBusiness> relations = endpointBusinessMapper.selectList(
                new LambdaQueryWrapper<ModelEndpointBusiness>()
                        .eq(ModelEndpointBusiness::getBusinessId, business.getId()));
        if (relations.isEmpty()) {
            throw new BusinessException("No endpoint configured for business: " + businessCode);
        }

        Set<String> exclusions = excludedKeyIds != null ? new LinkedHashSet<>(excludedKeyIds) : Collections.emptySet();

        for (ModelEndpointBusiness relation : relations) {
            ModelEndpoint endpoint = endpointMapper.selectById(relation.getEndpointId());
            if (endpoint == null || Boolean.FALSE.equals(endpoint.getEnabled())) {
                continue;
            }
            ModelApiKey apiKey = getAvailableApiKey(endpoint.getId(), exclusions);
            if (apiKey == null) {
                continue;
            }
            ModelInfo modelInfo = findModelInfo(endpoint.getId());
            return new ModelRuntimeConfig(business, endpoint, apiKey, modelInfo);
        }

        throw new BusinessException("No available API key for business: " + businessCode);
    }

    @Override
    public ModelApiKey getAvailableApiKey(Long endpointId, Set<String> excludedKeyIds) {
        if (endpointId == null) {
            return null;
        }

        List<ModelApiKey> candidates = apiKeyMapper.selectList(
                new LambdaQueryWrapper<ModelApiKey>()
                        .eq(ModelApiKey::getEndpointId, endpointId)
                        .eq(ModelApiKey::getEnabled, true));
        if (candidates.isEmpty()) {
            return null;
        }

        Set<String> exclusions = excludedKeyIds != null ? excludedKeyIds : Collections.emptySet();
        LocalDateTime now = LocalDateTime.now();

        return candidates.stream()
                .filter(key -> key.getApiKey() != null && !key.getApiKey().isBlank())
                .filter(key -> key.getKeyId() == null || !exclusions.contains(key.getKeyId()))
                .filter(key -> key.getExpiresAt() == null || key.getExpiresAt().isAfter(now))
                .sorted(KEY_PRIORITY_COMPARATOR)
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordApiKeyUsage(String keyId, boolean success, String error) {
        if (!StringUtils.hasText(keyId)) {
            return;
        }

        /*ModelApiKey key = apiKeyMapper.selectOne(
                new LambdaQueryWrapper<ModelApiKey>()
                        .eq(ModelApiKey::getKeyId, keyId)
                        .last("LIMIT 1"));
        if (key == null) {
            log.warn("Attempted to record usage for missing API key: {}", keyId);
            return;
        }

        key.setTotalRequests(Optional.ofNullable(key.getTotalRequests()).orElse(0L) + 1);
        if (success) {
            key.setSuccessRequests(Optional.ofNullable(key.getSuccessRequests()).orElse(0L) + 1);
            key.setLastError(null);
        } else {
            key.setFailedRequests(Optional.ofNullable(key.getFailedRequests()).orElse(0L) + 1);
            key.setLastError(error);
            evaluateKeyHealth(key, error);
        }
        key.setLastUsedAt(LocalDateTime.now());

        apiKeyMapper.updateById(key);*/
    }

    private void evaluateKeyHealth(ModelApiKey key, String error) {
        long failed = Optional.ofNullable(key.getFailedRequests()).orElse(0L);
        long success = Optional.ofNullable(key.getSuccessRequests()).orElse(0L);

        if (error != null && error.toLowerCase(Locale.ROOT).contains("unauthorized")) {
            key.setEnabled(false);
            log.warn("Disabling API key {} due to unauthorized error", key.getKeyId());
            return;
        }

        if (failed >= 5 && failed > success) {
            key.setEnabled(false);
            log.warn("Disabling API key {} after repeated failures (failed={}, success={})", key.getKeyId(), failed, success);
        }
    }

    private ModelInfo findModelInfo(Long endpointId) {
        return modelInfoMapper.selectOne(
                new LambdaQueryWrapper<ModelInfo>()
                        .eq(ModelInfo::getEndpointId, endpointId)
                        .orderByDesc(ModelInfo::getCreatedAt)
                        .last("LIMIT 1"));
    }
}
