package com.ai.middle.platform.service;

import com.ai.middle.platform.dto.request.ModelApiKeyRequest;
import com.ai.middle.platform.dto.request.ModelBusinessRequest;
import com.ai.middle.platform.dto.request.ModelEndpointRequest;
import com.ai.middle.platform.dto.response.ModelApiKeyDTO;
import com.ai.middle.platform.dto.response.ModelBusinessDTO;
import com.ai.middle.platform.dto.response.ModelEndpointDTO;

import java.util.List;

/**
 * 后台模型配置管理服务
 */
public interface ModelConfigAdminService {

    List<ModelEndpointDTO> listEndpoints();

    ModelEndpointDTO createEndpoint(ModelEndpointRequest request);

    ModelEndpointDTO updateEndpoint(Long id, ModelEndpointRequest request);

    void deleteEndpoint(Long id);

    List<ModelBusinessDTO> listBusinesses();

    ModelBusinessDTO createBusiness(ModelBusinessRequest request);

    ModelBusinessDTO updateBusiness(Long id, ModelBusinessRequest request);

    void deleteBusiness(Long id);

    List<ModelApiKeyDTO> listApiKeys(Long endpointId);

    ModelApiKeyDTO createApiKey(ModelApiKeyRequest request);

    ModelApiKeyDTO updateApiKey(Long id, ModelApiKeyRequest request);

    void deleteApiKey(Long id);
}
