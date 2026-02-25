package com.jettech.code.controller;

import com.jettech.code.dto.ApiResponse;
import com.jettech.code.dto.CreateVersionRequest;
import com.jettech.code.entity.ServiceVersion;
import com.jettech.code.service.ServiceVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/versions")
@RequiredArgsConstructor
public class ServiceVersionController {
    private final ServiceVersionService serviceVersionService;

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<ApiResponse<List<ServiceVersion>>> getByServiceId(@PathVariable Long serviceId) {
        return ResponseEntity.ok(ApiResponse.success(serviceVersionService.findByServiceId(serviceId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceVersion>> getById(@PathVariable Long id) {
        ServiceVersion version = serviceVersionService.getById(id);
        if (version == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(version));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ServiceVersion>> create(@RequestBody CreateVersionRequest request) {
        ServiceVersion version = new ServiceVersion();
        version.setServiceId(request.getServiceId());
        version.setVersion(request.getVersion());
        version.setCommitId(request.getCommitId());
        version.setDescription(request.getDescription());
        ServiceVersion created = serviceVersionService.create(version);
        return ResponseEntity.ok(ApiResponse.success("Version created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceVersion>> update(@PathVariable Long id, @RequestBody ServiceVersion version) {
        version.setId(id);
        ServiceVersion updated = serviceVersionService.update(version);
        return ResponseEntity.ok(ApiResponse.success("Version updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        serviceVersionService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Version deleted successfully", null));
    }
}
