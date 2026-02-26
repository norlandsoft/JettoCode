package com.jettech.code.controller;

import com.jettech.code.dto.ApiResponse;
import com.jettech.code.entity.CodeQualityTask;
import com.jettech.code.service.OpenCodeService;
import com.jettech.code.service.ScanTaskManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代码质量调试控制器
 * 用于排查问题
 */
@RestController
@RequestMapping("/api/debug/code-quality")
public class CodeQualityDebugController {

    private final OpenCodeService openCodeService;
    private final ScanTaskManager scanTaskManager;

    public CodeQualityDebugController(OpenCodeService openCodeService, 
                                     ScanTaskManager scanTaskManager) {
        this.openCodeService = openCodeService;
        this.scanTaskManager = scanTaskManager;
    }

    /**
     * 检查 OpenCode 服务状态
     */
    @GetMapping("/opencode/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkOpenCodeStatus() {
        Map<String, Object> status = new HashMap<>();
        
        boolean available = openCodeService.isAvailable();
        status.put("available", available);
        status.put("message", available ? "OpenCode 服务可用" : "OpenCode 服务不可用");
        
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * 查看任务的详细信息（包括 prompt 和 response）
     */
    @GetMapping("/tasks/{taskId}/detail")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTaskDetail(@PathVariable Long taskId) {
        CodeQualityTask task = scanTaskManager.getTask(taskId);
        
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", task.getId());
        detail.put("scanId", task.getScanId());
        detail.put("serviceId", task.getServiceId());
        detail.put("serviceName", task.getServiceName());
        detail.put("checkItemKey", task.getCheckItemKey());
        detail.put("checkItemName", task.getCheckItemName());
        detail.put("status", task.getStatus());
        detail.put("opencodeSessionId", task.getOpencodeSessionId());
        detail.put("issueCount", task.getIssueCount());
        detail.put("severity", task.getSeverity());
        detail.put("resultSummary", task.getResultSummary());
        detail.put("errorMessage", task.getErrorMessage());
        detail.put("retryCount", task.getRetryCount());
        detail.put("startedAt", task.getStartedAt());
        detail.put("completedAt", task.getCompletedAt());
        
        // 提示词和响应（可能很长）
        detail.put("promptText", task.getPromptText());
        detail.put("promptLength", task.getPromptText() != null ? task.getPromptText().length() : 0);
        detail.put("responseText", task.getResponseText());
        detail.put("responseLength", task.getResponseText() != null ? task.getResponseText().length() : 0);
        
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    /**
     * 查看扫描的所有任务状态
     */
    @GetMapping("/scans/{scanId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getScanStatus(@PathVariable Long scanId) {
        List<CodeQualityTask> tasks = scanTaskManager.getScanTasks(scanId);
        
        Map<String, Object> status = new HashMap<>();
        status.put("totalTasks", tasks.size());
        
        // 统计各状态任务数
        long pending = tasks.stream().filter(t -> "PENDING".equals(t.getStatus())).count();
        long running = tasks.stream().filter(t -> "RUNNING".equals(t.getStatus())).count();
        long completed = tasks.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
        long failed = tasks.stream().filter(t -> "FAILED".equals(t.getStatus())).count();
        
        status.put("pendingCount", pending);
        status.put("runningCount", running);
        status.put("completedCount", completed);
        status.put("failedCount", failed);
        
        // 检查是否有 prompt 和 response
        long withPrompt = tasks.stream().filter(t -> t.getPromptText() != null && !t.getPromptText().isEmpty()).count();
        long withResponse = tasks.stream().filter(t -> t.getResponseText() != null && !t.getResponseText().isEmpty()).count();
        long withError = tasks.stream().filter(t -> t.getErrorMessage() != null && !t.getErrorMessage().isEmpty()).count();
        
        status.put("tasksWithPrompt", withPrompt);
        status.put("tasksWithResponse", withResponse);
        status.put("tasksWithError", withError);
        
        // 任务概览
        status.put("tasks", tasks.stream().map(t -> {
            Map<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("id", t.getId());
            taskInfo.put("service", t.getServiceName());
            taskInfo.put("checkItem", t.getCheckItemName());
            taskInfo.put("status", t.getStatus());
            taskInfo.put("hasPrompt", t.getPromptText() != null && !t.getPromptText().isEmpty());
            taskInfo.put("hasResponse", t.getResponseText() != null && !t.getResponseText().isEmpty());
            taskInfo.put("issueCount", t.getIssueCount());
            taskInfo.put("errorMessage", t.getErrorMessage());
            return taskInfo;
        }).toList());
        
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * 测试 OpenCode 连接
     */
    @PostMapping("/test/opencode")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testOpenCodeConnection(
            @RequestBody(required = false) Map<String, String> request) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 检查服务可用性
            boolean available = openCodeService.isAvailable();
            result.put("serviceAvailable", available);
            
            if (!available) {
                result.put("success", false);
                result.put("message", "OpenCode 服务不可用，请检查服务是否运行在 http://127.0.0.1:4096");
                return ResponseEntity.ok(ApiResponse.success(result));
            }
            
            // 创建测试会话
            String sessionId = openCodeService.createSession("调试测试会话");
            result.put("sessionId", sessionId);
            result.put("sessionCreated", true);
            
            // 发送测试消息
            String testPrompt = "请简单回复：测试成功";
            if (request != null && request.containsKey("prompt")) {
                testPrompt = request.get("prompt");
            }
            
            var response = openCodeService.sendPrompt(sessionId, testPrompt);
            result.put("promptSent", testPrompt);
            result.put("responseReceived", response.getFullResponse());
            result.put("responseSuccess", response.isSuccess());
            
            // 清理会话
            openCodeService.deleteSession(sessionId);
            result.put("sessionDeleted", true);
            
            result.put("success", true);
            result.put("message", "OpenCode 连接测试成功");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
        }
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
