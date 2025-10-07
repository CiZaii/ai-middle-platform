package com.ai.middle.platform.controller;

import com.ai.middle.platform.common.constant.ApiConstants;
import com.ai.middle.platform.common.result.Result;
import com.ai.middle.platform.dto.request.ModelApiKeyRequest;
import com.ai.middle.platform.dto.response.ModelApiKeyDTO;
import com.ai.middle.platform.service.ModelConfigAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.MODEL_CONFIG_PATH + "/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ModelConfigAdminService modelConfigAdminService;

    @GetMapping
    public Result<List<ModelApiKeyDTO>> list(@RequestParam(value = "endpointId", required = false) Long endpointId) {
        List<ModelApiKeyDTO> keys = modelConfigAdminService.listApiKeys(endpointId);
        return Result.success(keys);
    }

    @PostMapping
    public Result<ModelApiKeyDTO> create(@Validated @RequestBody ModelApiKeyRequest request) {
        ModelApiKeyDTO apiKey = modelConfigAdminService.createApiKey(request);
        return Result.success(apiKey);
    }

    @PutMapping("/{id}")
    public Result<ModelApiKeyDTO> update(@PathVariable Long id,
                                         @Validated @RequestBody ModelApiKeyRequest request) {
        ModelApiKeyDTO apiKey = modelConfigAdminService.updateApiKey(id, request);
        return Result.success(apiKey);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        modelConfigAdminService.deleteApiKey(id);
        return Result.success();
    }
}
