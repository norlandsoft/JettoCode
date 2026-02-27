package com.jettech.code.controller;

import com.jettech.code.dto.ApiResponse;
import com.jettech.code.dto.ScanRequestDTO;
import com.jettech.code.entity.CodeQualityIssue;
import com.jettech.code.entity.CodeQualityScan;
import com.jettech.code.entity.CodeQualityTask;
import com.jettech.code.service.CodeQualityService;
import com.jettech.code.service.ScanTaskManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/code-quality")
public class CodeQualityController {
    private final CodeQualityService codeQualityService;
    private final ScanTaskManager scanTaskManager;

    public CodeQualityController(CodeQualityService codeQualityService, ScanTaskManager scanTaskManager) {
        this.codeQualityService = codeQualityService;
        this.scanTaskManager = scanTaskManager;
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

    @GetMapping("/applications/{applicationId}/scans")
    public ResponseEntity<ApiResponse<List<CodeQualityScan>>> getScansByApplication(@PathVariable Long applicationId) {
        List<CodeQualityScan> scans = codeQualityService.getScansByApplication(applicationId);
        return ResponseEntity.ok(ApiResponse.success(scans));
    }

    /**
     * 启动扫描 - 新版本，支持指定检查项
     */
    @PostMapping("/services/{serviceId}/scan")
    public ResponseEntity<ApiResponse<CodeQualityScan>> startScan(
            @PathVariable Long serviceId,
            @RequestBody(required = false) ScanRequestDTO request) {
        try {
            // 获取检查项ID列表
            List<Long> checkItemIds = Collections.emptyList();
            if (request != null && request.getCheckItemIds() != null && !request.getCheckItemIds().isEmpty()) {
                checkItemIds = request.getCheckItemIds();
            } else if (request != null && request.getCheckItems() != null && !request.getCheckItems().isEmpty()) {
                // 如果传的是 checkItems (key列表)，需要转换为ID
                // 这里暂时不做转换，由前端直接传ID
                checkItemIds = Collections.emptyList();
            }

            // 如果没有指定检查项，使用默认行为
            if (checkItemIds.isEmpty()) {
                CodeQualityScan scan = codeQualityService.startScan(serviceId);
                return ResponseEntity.ok(ApiResponse.success("扫描已启动（传统模式）", scan));
            }

            // 使用新的扫描流程
            CodeQualityScan scan = codeQualityService.startScanWithCheckItems(
                Collections.singletonList(serviceId),
                checkItemIds
            );
            return ResponseEntity.ok(ApiResponse.success("扫描已启动", scan));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("启动扫描失败: " + e.getMessage()));
        }
    }

    /**
     * 批量启动扫描 - 支持多个服务和多个检查项
     */
    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<CodeQualityScan>> startBatchScan(
            @RequestParam List<Long> serviceIds,
            @RequestParam List<Long> checkItemIds) {
        try {
            CodeQualityScan scan = codeQualityService.startScanWithCheckItems(serviceIds, checkItemIds);
            return ResponseEntity.ok(ApiResponse.success("批量扫描已启动", scan));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("启动批量扫描失败: " + e.getMessage()));
        }
    }

    /**
     * 获取扫描任务列表
     */
    @GetMapping("/scans/{scanId}/tasks")
    public ResponseEntity<ApiResponse<List<CodeQualityTask>>> getScanTasks(@PathVariable Long scanId) {
        List<CodeQualityTask> tasks = scanTaskManager.getScanTasks(scanId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    /**
     * 获取单个扫描任务
     */
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<CodeQualityTask>> getTask(@PathVariable Long taskId) {
        CodeQualityTask task = scanTaskManager.getTask(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(task));
    }

    /**
     * 获取扫描进度
     */
    @GetMapping("/scans/{scanId}/progress")
    public ResponseEntity<ApiResponse<Integer>> getScanProgress(@PathVariable Long scanId) {
        int progress = scanTaskManager.getScanProgress(scanId);
        return ResponseEntity.ok(ApiResponse.success(progress));
    }

    /**
     * 取消扫描
     */
    @PostMapping("/scans/{scanId}/cancel")
    public ResponseEntity<ApiResponse<Boolean>> cancelScan(@PathVariable Long scanId) {
        boolean cancelled = scanTaskManager.cancelScan(scanId);
        if (cancelled) {
            return ResponseEntity.ok(ApiResponse.success("扫描已取消", true));
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("无法取消扫描，可能扫描已完成或不存在"));
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

    /**
     * 删除扫描记录
     */
    @DeleteMapping("/scans/{scanId}")
    public ResponseEntity<ApiResponse<Boolean>> deleteScan(@PathVariable Long scanId) {
        try {
            boolean deleted = codeQualityService.deleteScan(scanId);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success("扫描记录已删除", true));
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("删除失败: " + e.getMessage()));
        }
    }
}
