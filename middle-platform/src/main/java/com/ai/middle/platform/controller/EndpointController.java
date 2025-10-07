package com.ai.middle.platform.controller;

import com.ai.middle.platform.common.constant.ApiConstants;
import com.ai.middle.platform.common.result.Result;
import com.ai.middle.platform.dto.request.ModelEndpointRequest;
import com.ai.middle.platform.dto.response.ModelEndpointDTO;
import com.ai.middle.platform.service.ModelConfigAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.MODEL_CONFIG_PATH + "/endpoints")
@RequiredArgsConstructor
public class EndpointController {

    private final ModelConfigAdminService modelConfigAdminService;

    @GetMapping
    public Result<List<ModelEndpointDTO>> list() {
        List<ModelEndpointDTO> endpoints = modelConfigAdminService.listEndpoints();
        return Result.success(endpoints);
    }

    @PostMapping
    public Result<ModelEndpointDTO> create(@Validated @RequestBody ModelEndpointRequest request) {
        ModelEndpointDTO endpointDTO = modelConfigAdminService.createEndpoint(request);
        return Result.success(endpointDTO);
    }

    @PutMapping("/{id}")
    public Result<ModelEndpointDTO> update(@PathVariable Long id,
                                          @Validated @RequestBody ModelEndpointRequest request) {
        ModelEndpointDTO endpointDTO = modelConfigAdminService.updateEndpoint(id, request);
        return Result.success(endpointDTO);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        modelConfigAdminService.deleteEndpoint(id);
        return Result.success();
    }
}
