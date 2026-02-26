package com.jettech.code.controller;

import com.jettech.code.dto.ApiResponse;
import com.jettech.code.entity.Dependency;
import com.jettech.code.entity.SecurityScan;
import com.jettech.code.entity.Vulnerability;
import com.jettech.code.service.SupplyChainService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/supply-chain")
public class SupplyChainController {
    private final SupplyChainService supplyChainService;

    public SupplyChainController(SupplyChainService supplyChainService) {
        this.supplyChainService = supplyChainService;
    }

    @GetMapping("/services/{serviceId}/dependencies")
    public ResponseEntity<ApiResponse<List<Dependency>>> getDependencies(@PathVariable Long serviceId) {
        List<Dependency> dependencies = supplyChainService.getDependencies(serviceId);
        return ResponseEntity.ok(ApiResponse.success(dependencies));
    }

    @GetMapping("/services/{serviceId}/parse")
    public ResponseEntity<ApiResponse<List<Dependency>>> parseDependencies(@PathVariable Long serviceId) {
        try {
            List<Dependency> dependencies = supplyChainService.parseDependencies(serviceId);
            return ResponseEntity.ok(ApiResponse.success("依赖解析完成", dependencies));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("依赖解析失败: " + e.getMessage()));
        }
    }

    @GetMapping("/dependencies/{id}")
    public ResponseEntity<ApiResponse<Dependency>> getDependency(@PathVariable Long id) {
        Dependency dependency = supplyChainService.getDependencyById(id);
        if (dependency == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(dependency));
    }

    @GetMapping("/services/{serviceId}/scans")
    public ResponseEntity<ApiResponse<List<SecurityScan>>> getScans(@PathVariable Long serviceId) {
        List<SecurityScan> scans = supplyChainService.getScans(serviceId);
        return ResponseEntity.ok(ApiResponse.success(scans));
    }

    @GetMapping("/services/{serviceId}/scans/latest")
    public ResponseEntity<ApiResponse<SecurityScan>> getLatestScan(@PathVariable Long serviceId) {
        SecurityScan scan = supplyChainService.getLatestScan(serviceId);
        if (scan == null) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }
        return ResponseEntity.ok(ApiResponse.success(scan));
    }
    
    @GetMapping("/scans/{scanId}")
    public ResponseEntity<ApiResponse<SecurityScan>> getScanProgress(@PathVariable Long scanId) {
        SecurityScan scan = supplyChainService.getScanById(scanId);
        if (scan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(scan));
    }

    @PostMapping("/services/{serviceId}/scan")
    public ResponseEntity<ApiResponse<SecurityScan>> startScan(@PathVariable Long serviceId) {
        try {
            SecurityScan scan = supplyChainService.startSecurityScan(serviceId);
            return ResponseEntity.ok(ApiResponse.success("扫描已启动", scan));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("启动扫描失败: " + e.getMessage()));
        }
    }

    @GetMapping("/dependencies/{dependencyId}/vulnerabilities")
    public ResponseEntity<ApiResponse<List<Vulnerability>>> getVulnerabilities(@PathVariable Long dependencyId) {
        List<Vulnerability> vulnerabilities = supplyChainService.getVulnerabilities(dependencyId);
        return ResponseEntity.ok(ApiResponse.success(vulnerabilities));
    }

    @GetMapping("/applications/{applicationId}/dependencies")
    public ResponseEntity<ApiResponse<List<Dependency>>> getDependenciesByApplication(@PathVariable Long applicationId) {
        List<Dependency> dependencies = supplyChainService.getDependenciesByApplication(applicationId);
        return ResponseEntity.ok(ApiResponse.success(dependencies));
    }

    @GetMapping("/applications/{applicationId}/scans/latest")
    public ResponseEntity<ApiResponse<SecurityScan>> getLatestScanByApplication(@PathVariable Long applicationId) {
        SecurityScan scan = supplyChainService.getLatestScanByApplication(applicationId);
        if (scan == null) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }
        return ResponseEntity.ok(ApiResponse.success(scan));
    }
}
