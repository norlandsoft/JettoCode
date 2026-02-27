package com.jettech.code.controller;

import com.jettech.code.dto.ApiResponse;
import com.jettech.code.dto.TaskExecutionLog;
import com.jettech.code.entity.CodeQualityTask;
import com.jettech.code.service.ScanTaskManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 扫描日志控制器
 * 提供扫描任务的详细执行日志查看功能
 */
@RestController
@RequestMapping("/api/code-quality/logs")
public class ScanLogController {

    private final ScanTaskManager scanTaskManager;

    public ScanLogController(ScanTaskManager scanTaskManager) {
        this.scanTaskManager = scanTaskManager;
    }

    /**
     * 获取扫描的执行日志列表
     */
    @GetMapping("/scans/{scanId}")
    public ResponseEntity<ApiResponse<List<TaskExecutionLog>>> getScanLogs(@PathVariable Long scanId) {
        List<CodeQualityTask> tasks = scanTaskManager.getScanTasks(scanId);

        List<TaskExecutionLog> logs = tasks.stream()
                .map(this::convertToLog)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * 获取单个任务的执行日志详情
     */
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<TaskExecutionLog>> getTaskLog(@PathVariable Long taskId) {
        CodeQualityTask task = scanTaskManager.getTask(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ApiResponse.success(convertToLog(task)));
    }

    /**
     * 获取任务的原始提示词
     */
    @GetMapping("/tasks/{taskId}/prompt")
    public ResponseEntity<ApiResponse<String>> getTaskPrompt(@PathVariable Long taskId) {
        CodeQualityTask task = scanTaskManager.getTask(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ApiResponse.success(task.getPromptText()));
    }

    /**
     * 获取任务的原始响应
     */
    @GetMapping("/tasks/{taskId}/response")
    public ResponseEntity<ApiResponse<String>> getTaskResponse(@PathVariable Long taskId) {
        CodeQualityTask task = scanTaskManager.getTask(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ApiResponse.success(task.getResponseText()));
    }

    /**
     * 转换任务为日志 DTO
     */
    private TaskExecutionLog convertToLog(CodeQualityTask task) {
        Long duration = null;
        if (task.getStartedAt() != null && task.getCompletedAt() != null) {
            duration = Duration.between(task.getStartedAt(), task.getCompletedAt()).toMillis();
        }

        return TaskExecutionLog.builder()
                .taskId(task.getId())
                .scanId(task.getScanId())
                .serviceName(task.getServiceName())
                .checkItemKey(task.getCheckItemKey())
                .checkItemName(task.getCheckItemName())
                .status(task.getStatus())
                .promptText(task.getPromptText())
                .promptLength(task.getPromptText() != null ? task.getPromptText().length() : 0)
                .startedAt(task.getStartedAt())
                .responseText(task.getResponseText())
                .responseLength(task.getResponseText() != null ? task.getResponseText().length() : 0)
                .completedAt(task.getCompletedAt())
                .issueCount(task.getIssueCount())
                .severity(task.getSeverity())
                .resultSummary(task.getResultSummary())
                .errorMessage(task.getErrorMessage())
                .retryCount(task.getRetryCount())
                .opencodeSessionId(task.getOpencodeSessionId())
                .duration(duration)
                .build();
    }
}
