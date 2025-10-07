package com.ai.middle.platform.controller;

import com.ai.middle.platform.common.constant.ApiConstants;
import com.ai.middle.platform.common.result.Result;
import com.ai.middle.platform.dto.request.ModelBusinessRequest;
import com.ai.middle.platform.dto.response.ModelBusinessDTO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.MODEL_CONFIG_PATH + "/business")
@RequiredArgsConstructor
public class BusinessController {

    private final ModelConfigAdminService modelConfigAdminService;

    @GetMapping
    public Result<List<ModelBusinessDTO>> list() {
        List<ModelBusinessDTO> businesses = modelConfigAdminService.listBusinesses();
        return Result.success(businesses);
    }

    @PostMapping
    public Result<ModelBusinessDTO> create(@Validated @RequestBody ModelBusinessRequest request) {
        ModelBusinessDTO business = modelConfigAdminService.createBusiness(request);
        return Result.success(business);
    }

    @PutMapping("/{id}")
    public Result<ModelBusinessDTO> update(@PathVariable Long id,
                                           @Validated @RequestBody ModelBusinessRequest request) {
        ModelBusinessDTO business = modelConfigAdminService.updateBusiness(id, request);
        return Result.success(business);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        modelConfigAdminService.deleteBusiness(id);
        return Result.success();
    }
}
