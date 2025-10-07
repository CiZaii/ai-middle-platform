package com.ai.middle.platform.service.impl;

import com.ai.middle.platform.common.context.UserContextHolder;
import com.ai.middle.platform.common.exception.BusinessException;
import com.ai.middle.platform.common.util.IdGenerator;
import com.ai.middle.platform.dto.request.ModelApiKeyRequest;
import com.ai.middle.platform.dto.request.ModelBusinessRequest;
import com.ai.middle.platform.dto.request.ModelEndpointRequest;
import com.ai.middle.platform.dto.response.ModelApiKeyDTO;
import com.ai.middle.platform.dto.response.ModelApiKeyDTO.EndpointSummary;
import com.ai.middle.platform.dto.response.ModelBusinessDTO;
import com.ai.middle.platform.dto.response.ModelEndpointDTO;
import com.ai.middle.platform.dto.response.ModelEndpointDTO.EndpointStatsDTO;
import com.ai.middle.platform.dto.response.ModelEndpointDTO.ModelInfoDTO;
import com.ai.middle.platform.dto.response.UserDTO;
import com.ai.middle.platform.entity.po.ModelApiKey;
import com.ai.middle.platform.entity.po.ModelBusiness;
import com.ai.middle.platform.entity.po.ModelEndpoint;
import com.ai.middle.platform.entity.po.ModelEndpointBusiness;
import com.ai.middle.platform.entity.po.ModelInfo;
import com.ai.middle.platform.entity.po.SysUser;
import com.ai.middle.platform.repository.mapper.ModelApiKeyMapper;
import com.ai.middle.platform.repository.mapper.ModelBusinessMapper;
import com.ai.middle.platform.repository.mapper.ModelEndpointBusinessMapper;
import com.ai.middle.platform.repository.mapper.ModelEndpointMapper;
import com.ai.middle.platform.repository.mapper.ModelInfoMapper;
import com.ai.middle.platform.repository.mapper.SysUserMapper;
import com.ai.middle.platform.service.ModelConfigAdminService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 模型配置管理服务实现
 */
@Service
@RequiredArgsConstructor
public class ModelConfigAdminServiceImpl implements ModelConfigAdminService {

    private final ModelEndpointMapper endpointMapper;
    private final ModelInfoMapper modelInfoMapper;
    private final ModelEndpointBusinessMapper endpointBusinessMapper;
    private final ModelBusinessMapper businessMapper;
    private final ModelApiKeyMapper apiKeyMapper;
    private final SysUserMapper userMapper;

    @Override
    public List<ModelEndpointDTO> listEndpoints() {
        List<ModelEndpoint> endpoints = endpointMapper.selectList(null);
        if (endpoints.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> endpointIds = endpoints.stream()
                .map(ModelEndpoint::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, List<ModelInfo>> modelsByEndpoint = loadModels(endpointIds);
        Map<Long, List<ModelBusiness>> businessesByEndpoint = loadEndpointBusinesses(endpointIds);
        Map<Long, EndpointStats> statsByEndpoint = loadEndpointStats(endpointIds);

        Set<Long> userIds = new LinkedHashSet<>();
        userIds.addAll(endpoints.stream()
                .map(ModelEndpoint::getCreatedBy)
                .filter(Objects::nonNull)
                .toList());
        businessesByEndpoint.values().forEach(list -> list.stream()
                .map(ModelBusiness::getCreatedBy)
                .filter(Objects::nonNull)
                .forEach(userIds::add));

        Map<Long, UserDTO> userMap = loadUsers(userIds);

        return endpoints.stream()
                .sorted(Comparator.comparing(ModelEndpoint::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ModelEndpoint::getId, Comparator.nullsLast(Long::compareTo)))
                .map(endpoint -> buildEndpointDTO(endpoint,
                        modelsByEndpoint.getOrDefault(endpoint.getId(), Collections.emptyList()),
                        businessesByEndpoint.getOrDefault(endpoint.getId(), Collections.emptyList()),
                        statsByEndpoint.getOrDefault(endpoint.getId(), EndpointStats.empty()),
                        userMap))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelEndpointDTO createEndpoint(ModelEndpointRequest request) {
        Long currentUserId = requireCurrentUserId();
        ModelEndpoint endpoint = new ModelEndpoint();
        endpoint.setEndpointId(IdGenerator.simpleUUID());
        endpoint.setName(request.getName().trim());
        endpoint.setBaseUrl(request.getBaseUrl().trim());
        endpoint.setProvider(request.getProvider().trim());
        endpoint.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        endpoint.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        endpoint.setCreatedBy(currentUserId);

        endpointMapper.insert(endpoint);

        replaceEndpointModels(endpoint.getId(), request.getModelNames(), endpoint.getProvider());
        replaceEndpointBusinesses(endpoint.getId(), request.getBusinessIds());

        return getEndpointDTO(endpoint.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelEndpointDTO updateEndpoint(Long id, ModelEndpointRequest request) {
        ModelEndpoint endpoint = endpointMapper.selectById(id);
        if (endpoint == null) {
            throw new BusinessException("端点不存在");
        }

        endpoint.setName(request.getName().trim());
        endpoint.setBaseUrl(request.getBaseUrl().trim());
        endpoint.setProvider(request.getProvider().trim());
        endpoint.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        endpoint.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        endpointMapper.updateById(endpoint);

        replaceEndpointModels(endpoint.getId(), request.getModelNames(), endpoint.getProvider());
        replaceEndpointBusinesses(endpoint.getId(), request.getBusinessIds());

        return getEndpointDTO(endpoint.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEndpoint(Long id) {
        if (id == null) {
            return;
        }
        endpointMapper.deleteById(id);
        modelInfoMapper.delete(new LambdaQueryWrapper<ModelInfo>().eq(ModelInfo::getEndpointId, id));
        endpointBusinessMapper.delete(new LambdaQueryWrapper<ModelEndpointBusiness>().eq(ModelEndpointBusiness::getEndpointId, id));
        apiKeyMapper.delete(new LambdaQueryWrapper<ModelApiKey>().eq(ModelApiKey::getEndpointId, id));
    }

    @Override
    public List<ModelBusinessDTO> listBusinesses() {
        List<ModelBusiness> businesses = businessMapper.selectList(null);
        if (businesses.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = businesses.stream()
                .map(ModelBusiness::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, UserDTO> userMap = loadUsers(userIds);
        return businesses.stream()
                .sorted(Comparator.comparing(ModelBusiness::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ModelBusiness::getId, Comparator.nullsLast(Long::compareTo)))
                .map(business -> toBusinessDTO(business, userMap.get(business.getCreatedBy())))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelBusinessDTO createBusiness(ModelBusinessRequest request) {
        Long currentUserId = requireCurrentUserId();
        ModelBusiness business = new ModelBusiness();
        business.setBusinessId(IdGenerator.simpleUUID());
        business.setName(request.getName().trim());
        business.setCode(request.getCode().trim());
        business.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        business.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        business.setCreatedBy(currentUserId);
        businessMapper.insert(business);
        return getBusinessDTO(business.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelBusinessDTO updateBusiness(Long id, ModelBusinessRequest request) {
        ModelBusiness business = businessMapper.selectById(id);
        if (business == null) {
            throw new BusinessException("业务不存在");
        }
        business.setName(request.getName().trim());
        business.setCode(request.getCode().trim());
        business.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        business.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        businessMapper.updateById(business);
        return getBusinessDTO(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBusiness(Long id) {
        if (id == null) {
            return;
        }
        endpointBusinessMapper.delete(new LambdaQueryWrapper<ModelEndpointBusiness>().eq(ModelEndpointBusiness::getBusinessId, id));
        businessMapper.deleteById(id);
    }

    @Override
    public List<ModelApiKeyDTO> listApiKeys(Long endpointId) {
        LambdaQueryWrapper<ModelApiKey> wrapper = new LambdaQueryWrapper<>();
        if (endpointId != null) {
            wrapper.eq(ModelApiKey::getEndpointId, endpointId);
        }
        List<ModelApiKey> apiKeys = apiKeyMapper.selectList(wrapper);
        if (apiKeys.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> endpointIds = apiKeys.stream()
                .map(ModelApiKey::getEndpointId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, ModelEndpoint> endpointMap = endpointIds.isEmpty()
                ? Collections.emptyMap()
                : endpointMapper.selectBatchIds(endpointIds).stream()
                .collect(Collectors.toMap(ModelEndpoint::getId, Function.identity()));

        Set<Long> userIds = apiKeys.stream()
                .map(ModelApiKey::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, UserDTO> userMap = loadUsers(userIds);

        return apiKeys.stream()
                .sorted(Comparator.comparing(ModelApiKey::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ModelApiKey::getId, Comparator.nullsLast(Long::compareTo)))
                .map(apiKey -> toApiKeyDTO(apiKey, endpointMap.get(apiKey.getEndpointId()), userMap.get(apiKey.getCreatedBy())))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelApiKeyDTO createApiKey(ModelApiKeyRequest request) {
        Long currentUserId = requireCurrentUserId();
        ModelEndpoint endpoint = endpointMapper.selectById(request.getEndpointId());
        if (endpoint == null) {
            throw new BusinessException("关联端点不存在");
        }

        ModelApiKey apiKey = new ModelApiKey();
        apiKey.setKeyId(IdGenerator.simpleUUID());
        apiKey.setEndpointId(request.getEndpointId());
        apiKey.setName(request.getName().trim());
        if (StringUtils.hasText(request.getApiKey())) {
            String secret = request.getApiKey().trim();
            apiKey.setApiKey(secret);
            apiKey.setDisplayKey(maskApiKey(secret));
        }
        apiKey.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        apiKey.setRateLimitPerMinute(request.getRequestsPerMinute());
        apiKey.setRateLimitPerDay(request.getRequestsPerDay());
        apiKey.setExpiresAt(toLocalDateTime(request.getExpiresAt()));
        apiKey.setCreatedBy(currentUserId);

        apiKeyMapper.insert(apiKey);
        return getApiKeyDTO(apiKey.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelApiKeyDTO updateApiKey(Long id, ModelApiKeyRequest request) {
        ModelApiKey apiKey = apiKeyMapper.selectById(id);
        if (apiKey == null) {
            throw new BusinessException("API Key不存在");
        }
        if (!Objects.equals(apiKey.getEndpointId(), request.getEndpointId())) {
            ModelEndpoint endpoint = endpointMapper.selectById(request.getEndpointId());
            if (endpoint == null) {
                throw new BusinessException("关联端点不存在");
            }
            apiKey.setEndpointId(request.getEndpointId());
        }
        apiKey.setName(request.getName().trim());
        if (StringUtils.hasText(request.getApiKey())) {
            String secret = request.getApiKey().trim();
            apiKey.setApiKey(secret);
            apiKey.setDisplayKey(maskApiKey(secret));
        }
        apiKey.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        apiKey.setRateLimitPerMinute(request.getRequestsPerMinute());
        apiKey.setRateLimitPerDay(request.getRequestsPerDay());
        apiKey.setExpiresAt(toLocalDateTime(request.getExpiresAt()));
        apiKeyMapper.updateById(apiKey);
        return getApiKeyDTO(apiKey.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteApiKey(Long id) {
        if (id == null) {
            return;
        }
        apiKeyMapper.deleteById(id);
    }

    private Map<Long, List<ModelInfo>> loadModels(List<Long> endpointIds) {
        if (CollectionUtils.isEmpty(endpointIds)) {
            return Collections.emptyMap();
        }
        List<ModelInfo> models = modelInfoMapper.selectList(new LambdaQueryWrapper<ModelInfo>()
                .in(ModelInfo::getEndpointId, endpointIds));
        return models.stream()
                .collect(Collectors.groupingBy(ModelInfo::getEndpointId));
    }

    private Map<Long, List<ModelBusiness>> loadEndpointBusinesses(List<Long> endpointIds) {
        if (CollectionUtils.isEmpty(endpointIds)) {
            return Collections.emptyMap();
        }
        List<ModelEndpointBusiness> relations = endpointBusinessMapper.selectList(new LambdaQueryWrapper<ModelEndpointBusiness>()
                .in(ModelEndpointBusiness::getEndpointId, endpointIds));
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> businessIds = relations.stream()
                .map(ModelEndpointBusiness::getBusinessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (businessIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, ModelBusiness> businessMap = businessMapper.selectBatchIds(businessIds).stream()
                .collect(Collectors.toMap(ModelBusiness::getId, Function.identity()));

        Map<Long, List<ModelBusiness>> grouped = relations.stream()
                .collect(Collectors.groupingBy(ModelEndpointBusiness::getEndpointId,
                        Collectors.mapping(rel -> businessMap.get(rel.getBusinessId()), Collectors.toList())));

        grouped.replaceAll((key, list) -> list.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new)));
        return grouped;
    }

    private Map<Long, EndpointStats> loadEndpointStats(List<Long> endpointIds) {
        if (CollectionUtils.isEmpty(endpointIds)) {
            return Collections.emptyMap();
        }
        List<ModelApiKey> apiKeys = apiKeyMapper.selectList(new LambdaQueryWrapper<ModelApiKey>()
                .in(ModelApiKey::getEndpointId, endpointIds));
        if (apiKeys.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<ModelApiKey>> grouped = apiKeys.stream()
                .collect(Collectors.groupingBy(ModelApiKey::getEndpointId));
        return grouped.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> EndpointStats.from(entry.getValue())));
    }

    private Map<Long, UserDTO> loadUsers(Set<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<Long> filteredIds = userIds.stream()
                .filter(Objects::nonNull)
                .toList();
        if (filteredIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(filteredIds).stream()
                .collect(Collectors.toMap(SysUser::getId, this::toUserDTO));
    }

    private ModelEndpointDTO buildEndpointDTO(ModelEndpoint endpoint,
                                              List<ModelInfo> models,
                                              List<ModelBusiness> businesses,
                                              EndpointStats stats,
                                              Map<Long, UserDTO> userMap) {
        List<ModelInfoDTO> modelDTOs = models.stream()
                .map(this::toModelInfoDTO)
                .toList();

        Map<Long, UserDTO> businessUserMap = businesses.stream()
                .map(ModelBusiness::getCreatedBy)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(Function.identity(), userMap::get, (a, b) -> a));

        List<ModelBusinessDTO> businessDTOs = businesses.stream()
                .map(business -> toBusinessDTO(business, businessUserMap.get(business.getCreatedBy())))
                .toList();

        EndpointStatsDTO statsDTO = EndpointStatsDTO.builder()
                .totalApiKeys(stats.totalKeys())
                .activeApiKeys(stats.activeKeys())
                .totalRequests(stats.totalRequests())
                .successRate(stats.successRate())
                .build();

        return ModelEndpointDTO.builder()
                .id(endpoint.getId())
                .endpointId(endpoint.getEndpointId())
                .name(endpoint.getName())
                .baseUrl(endpoint.getBaseUrl())
                .provider(endpoint.getProvider())
                .description(endpoint.getDescription())
                .enabled(endpoint.getEnabled())
                .createdAt(endpoint.getCreatedAt())
                .updatedAt(endpoint.getUpdatedAt())
                .createdBy(userMap.get(endpoint.getCreatedBy()))
                .models(modelDTOs)
                .businesses(businessDTOs)
                .stats(statsDTO)
                .build();
    }

    private ModelEndpointDTO getEndpointDTO(Long endpointId) {
        ModelEndpoint endpoint = endpointMapper.selectById(endpointId);
        if (endpoint == null) {
            throw new BusinessException("端点不存在");
        }
        Map<Long, List<ModelInfo>> models = loadModels(List.of(endpointId));
        Map<Long, List<ModelBusiness>> businesses = loadEndpointBusinesses(List.of(endpointId));
        Map<Long, EndpointStats> stats = loadEndpointStats(List.of(endpointId));
        Set<Long> userIds = new LinkedHashSet<>();
        if (endpoint.getCreatedBy() != null) {
            userIds.add(endpoint.getCreatedBy());
        }
        businesses.values().stream()
                .flatMap(List::stream)
                .map(ModelBusiness::getCreatedBy)
                .filter(Objects::nonNull)
                .forEach(userIds::add);
        Map<Long, UserDTO> userMap = loadUsers(userIds);
        return buildEndpointDTO(endpoint,
                models.getOrDefault(endpointId, Collections.emptyList()),
                businesses.getOrDefault(endpointId, Collections.emptyList()),
                stats.getOrDefault(endpointId, EndpointStats.empty()),
                userMap);
    }

    private ModelBusinessDTO getBusinessDTO(Long id) {
        ModelBusiness business = businessMapper.selectById(id);
        if (business == null) {
            throw new BusinessException("业务不存在");
        }
        UserDTO createdBy = business.getCreatedBy() != null ? loadUsers(Set.of(business.getCreatedBy())).get(business.getCreatedBy()) : null;
        return toBusinessDTO(business, createdBy);
    }

    private ModelApiKeyDTO getApiKeyDTO(Long id) {
        ModelApiKey apiKey = apiKeyMapper.selectById(id);
        if (apiKey == null) {
            throw new BusinessException("API Key不存在");
        }
        ModelEndpoint endpoint = apiKey.getEndpointId() != null ? endpointMapper.selectById(apiKey.getEndpointId()) : null;
        UserDTO createdBy = apiKey.getCreatedBy() != null ? loadUsers(Set.of(apiKey.getCreatedBy())).get(apiKey.getCreatedBy()) : null;
        return toApiKeyDTO(apiKey, endpoint, createdBy);
    }

    private void replaceEndpointModels(Long endpointId, List<String> modelNames, String provider) {
        modelInfoMapper.delete(new LambdaQueryWrapper<ModelInfo>().eq(ModelInfo::getEndpointId, endpointId));
        if (CollectionUtils.isEmpty(modelNames)) {
            return;
        }
        modelNames.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .map(name -> ModelInfo.builder()
                        .modelId(IdGenerator.simpleUUID())
                        .endpointId(endpointId)
                        .name(name)
                        .provider(provider)
                        .build())
                .forEach(modelInfoMapper::insert);
    }

    private void replaceEndpointBusinesses(Long endpointId, List<Long> businessIds) {
        endpointBusinessMapper.delete(new LambdaQueryWrapper<ModelEndpointBusiness>().eq(ModelEndpointBusiness::getEndpointId, endpointId));
        if (CollectionUtils.isEmpty(businessIds)) {
            return;
        }
        Set<Long> uniqueIds = businessIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (uniqueIds.isEmpty()) {
            return;
        }
        // 校验业务是否存在
        Map<Long, ModelBusiness> businessMap = businessMapper.selectBatchIds(uniqueIds).stream()
                .collect(Collectors.toMap(ModelBusiness::getId, Function.identity()));
        for (Long businessId : uniqueIds) {
            if (!businessMap.containsKey(businessId)) {
                throw new BusinessException("业务不存在: " + businessId);
            }
            ModelEndpointBusiness relation = new ModelEndpointBusiness();
            relation.setEndpointId(endpointId);
            relation.setBusinessId(businessId);
            endpointBusinessMapper.insert(relation);
        }
    }

    private ModelBusinessDTO toBusinessDTO(ModelBusiness business, UserDTO createdBy) {
        return ModelBusinessDTO.builder()
                .id(business.getId())
                .businessId(business.getBusinessId())
                .name(business.getName())
                .code(business.getCode())
                .description(business.getDescription())
                .enabled(business.getEnabled())
                .createdAt(business.getCreatedAt())
                .updatedAt(business.getUpdatedAt())
                .createdBy(createdBy)
                .build();
    }

    private ModelInfoDTO toModelInfoDTO(ModelInfo model) {
        return ModelInfoDTO.builder()
                .id(model.getModelId())
                .name(model.getName())
                .provider(model.getProvider())
                .description(model.getDescription())
                .build();
    }

    private ModelApiKeyDTO toApiKeyDTO(ModelApiKey apiKey, ModelEndpoint endpoint, UserDTO createdBy) {
        EndpointSummary summary = endpoint != null
                ? EndpointSummary.builder()
                .id(endpoint.getId())
                .name(endpoint.getName())
                .provider(endpoint.getProvider())
                .baseUrl(endpoint.getBaseUrl())
                .build()
                : null;

        return ModelApiKeyDTO.builder()
                .id(apiKey.getId())
                .keyId(apiKey.getKeyId())
                .endpointId(apiKey.getEndpointId())
                .name(apiKey.getName())
                .apiKey(apiKey.getApiKey())
                .displayKey(resolveDisplayKey(apiKey))
                .enabled(apiKey.getEnabled())
                .rateLimitPerMinute(apiKey.getRateLimitPerMinute())
                .rateLimitPerDay(apiKey.getRateLimitPerDay())
                .totalRequests(Optional.ofNullable(apiKey.getTotalRequests()).orElse(0L))
                .successRequests(Optional.ofNullable(apiKey.getSuccessRequests()).orElse(0L))
                .failedRequests(Optional.ofNullable(apiKey.getFailedRequests()).orElse(0L))
                .lastError(apiKey.getLastError())
                .createdAt(apiKey.getCreatedAt())
                .updatedAt(apiKey.getUpdatedAt())
                .expiresAt(apiKey.getExpiresAt())
                .lastUsedAt(apiKey.getLastUsedAt())
                .createdBy(createdBy)
                .endpoint(summary)
                .build();
    }

    private UserDTO toUserDTO(SysUser user) {
        if (user == null) {
            return null;
        }
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .build();
    }

    private String maskApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }
        String value = apiKey.trim();
        if (value.length() <= 4) {
            return value.charAt(0) + "***" + value.charAt(value.length() - 1);
        }
        if (value.length() <= 8) {
            return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
        }
        return value.substring(0, 4) + "***" + value.substring(value.length() - 4);
    }

    private String resolveDisplayKey(ModelApiKey apiKey) {
        if (StringUtils.hasText(apiKey.getDisplayKey())) {
            return apiKey.getDisplayKey();
        }
        if (StringUtils.hasText(apiKey.getApiKey())) {
            return maskApiKey(apiKey.getApiKey());
        }
        return null;
    }

    private LocalDateTime toLocalDateTime(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    private record EndpointStats(long totalKeys, long activeKeys, long totalRequests, long successRequests,
                                 long failedRequests) {

        static EndpointStats from(List<ModelApiKey> keys) {
            long total = keys.size();
            long active = keys.stream().filter(key -> Boolean.TRUE.equals(key.getEnabled())).count();
            long totalRequests = keys.stream()
                    .map(ModelApiKey::getTotalRequests)
                    .filter(Objects::nonNull)
                    .mapToLong(Long::longValue)
                    .sum();
            long success = keys.stream()
                    .map(ModelApiKey::getSuccessRequests)
                    .filter(Objects::nonNull)
                    .mapToLong(Long::longValue)
                    .sum();
            long failed = keys.stream()
                    .map(ModelApiKey::getFailedRequests)
                    .filter(Objects::nonNull)
                    .mapToLong(Long::longValue)
                    .sum();
            return new EndpointStats(total, active, totalRequests, success, failed);
        }

        static EndpointStats empty() {
            return new EndpointStats(0, 0, 0, 0, 0);
        }

        double successRate() {
            if (totalRequests == 0) {
                return 0.0;
            }
            return (double) successRequests / totalRequests * 100;
        }
    }

    private Long requireCurrentUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录或登录已过期");
        }
        return userId;
    }
}
