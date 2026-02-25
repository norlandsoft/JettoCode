package com.jettech.code.controller;

import com.jettech.code.dto.ApiResponse;
import com.jettech.code.dto.CreateServiceRequest;
import com.jettech.code.entity.ServiceEntity;
import com.jettech.code.entity.ServiceVersion;
import com.jettech.code.service.GitService;
import com.jettech.code.service.ServiceEntityService;
import com.jettech.code.service.ServiceVersionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {
    private final ServiceEntityService serviceEntityService;
    private final ServiceVersionService serviceVersionService;
    private final GitService gitService;

    public ServiceController(ServiceEntityService serviceEntityService, 
                             ServiceVersionService serviceVersionService,
                             GitService gitService) {
        this.serviceEntityService = serviceEntityService;
        this.serviceVersionService = serviceVersionService;
        this.gitService = gitService;
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<ApiResponse<List<ServiceEntity>>> getByApplicationId(@PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success(serviceEntityService.findByApplicationId(applicationId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceEntity>> getById(@PathVariable Long id) {
        ServiceEntity service = serviceEntityService.getById(id);
        if (service == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(service));
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<ApiResponse<List<ServiceVersion>>> getVersions(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(serviceVersionService.findByServiceId(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ServiceEntity>> create(@RequestBody CreateServiceRequest request) {
        ServiceEntity service = new ServiceEntity();
        service.setApplicationId(request.getApplicationId());
        service.setName(request.getName());
        service.setGitUrl(request.getGitUrl());
        service.setDescription(request.getDescription());
        ServiceEntity created = serviceEntityService.create(service);
        return ResponseEntity.ok(ApiResponse.success("Service created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceEntity>> update(@PathVariable Long id, @RequestBody ServiceEntity service) {
        service.setId(id);
        ServiceEntity updated = serviceEntityService.update(service);
        return ResponseEntity.ok(ApiResponse.success("Service updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        serviceEntityService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Service deleted successfully", null));
    }

    @PostMapping("/{id}/pull")
    public ResponseEntity<ApiResponse<ServiceEntity>> pull(@PathVariable Long id) {
        try {
            ServiceEntity service = gitService.pullService(id);
            return ResponseEntity.ok(ApiResponse.success("代码拉取成功", service));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("代码拉取失败: " + e.getMessage()));
        }
    }
}
