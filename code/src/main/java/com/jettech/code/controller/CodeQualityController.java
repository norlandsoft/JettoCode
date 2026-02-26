package com.jettech.code.controller;

import com.jettech.code.dto.ApiResponse;
import com.jettech.code.entity.CodeQualityIssue;
import com.jettech.code.entity.CodeQualityScan;
import com.jettech.code.service.CodeQualityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/code-quality")
public class CodeQualityController {
    private final CodeQualityService codeQualityService;

    public CodeQualityController(CodeQualityService codeQualityService) {
        this.codeQualityService = codeQualityService;
    }

    @GetMapping("/services/{serviceId}/scans")
    public ResponseEntity<ApiResponse<List<CodeQualityScan>>> getScans(@PathVariable Long serviceId) {
        List<CodeQualityScan> scans = codeQualityService.getScans(serviceId);
        return ResponseEntity.ok(ApiResponse.success(scans));
    }

    @GetMapping("/scans/{scanId}")
    public ResponseEntity<ApiResponse<CodeQualityScan>> getScan(@PathVariable Long scanId) {
        CodeQualityScan scan = codeQualityService.getScanById(scanId);
        if (scan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(scan));
    }

    @GetMapping("/services/{serviceId}/scans/latest")
    public ResponseEntity<ApiResponse<CodeQualityScan>> getLatestScan(@PathVariable Long serviceId) {
        CodeQualityScan scan = codeQualityService.getLatestScan(serviceId);
        return ResponseEntity.ok(ApiResponse.success(scan));
    }

    @GetMapping("/applications/{applicationId}/scans/latest")
    public ResponseEntity<ApiResponse<CodeQualityScan>> getLatestScanByApplication(@PathVariable Long applicationId) {
        CodeQualityScan scan = codeQualityService.getLatestScanByApplication(applicationId);
        return ResponseEntity.ok(ApiResponse.success(scan));
    }

    @PostMapping("/services/{serviceId}/scan")
    public ResponseEntity<ApiResponse<CodeQualityScan>> startScan(@PathVariable Long serviceId) {
        try {
            CodeQualityScan scan = codeQualityService.startScan(serviceId);
            return ResponseEntity.ok(ApiResponse.success("扫描已启动", scan));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("启动扫描失败: " + e.getMessage()));
        }
    }

    @GetMapping("/scans/{scanId}/issues")
    public ResponseEntity<ApiResponse<List<CodeQualityIssue>>> getIssues(
            @PathVariable Long scanId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity) {
        
        List<CodeQualityIssue> issues;
        if (category != null && !category.isEmpty()) {
            issues = codeQualityService.getIssuesByCategory(scanId, category);
        } else if (severity != null && !severity.isEmpty()) {
            issues = codeQualityService.getIssuesBySeverity(scanId, severity);
        } else {
            issues = codeQualityService.getIssues(scanId);
        }
        return ResponseEntity.ok(ApiResponse.success(issues));
    }

    @GetMapping("/issues/{issueId}")
    public ResponseEntity<ApiResponse<CodeQualityIssue>> getIssue(@PathVariable Long issueId) {
        CodeQualityIssue issue = codeQualityService.getIssueById(issueId);
        if (issue == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(issue));
    }
}
