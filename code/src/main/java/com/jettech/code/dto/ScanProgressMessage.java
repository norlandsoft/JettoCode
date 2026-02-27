package com.jettech.code.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 扫描进度消息
 * 用于 WebSocket 推送
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanProgressMessage {

    // 消息类型
    public static final String TYPE_PHASE = "PHASE";
    public static final String TYPE_PROGRESS = "PROGRESS";
    public static final String TYPE_TASK_START = "TASK_START";
    public static final String TYPE_TASK_COMPLETE = "TASK_COMPLETE";
    public static final String TYPE_COMPLETED = "COMPLETED";
    public static final String TYPE_CANCELLED = "CANCELLED";
    public static final String TYPE_ERROR = "ERROR";

    private String type;
    private Long scanId;
    private Long taskId;
    private String serviceName;
    private String checkItemName;
    private String status;
    private Integer progress;
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer issueCount;
    private String severity;
    private String message;
    private LocalDateTime timestamp;

    /**
     * 创建阶段消息
     */
    public static ScanProgressMessage phase(Long scanId, String message) {
        return ScanProgressMessage.builder()
                .type(TYPE_PHASE)
                .scanId(scanId)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 创建进度消息
     */
    public static ScanProgressMessage progress(Long scanId, int progress, int completed, int total) {
        return ScanProgressMessage.builder()
                .type(TYPE_PROGRESS)
                .scanId(scanId)
                .progress(progress)
                .completedTasks(completed)
                .totalTasks(total)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 创建任务开始消息
     */
    public static ScanProgressMessage taskStart(Long scanId, Long taskId, String serviceName, String checkItemName) {
        return ScanProgressMessage.builder()
                .type(TYPE_TASK_START)
                .scanId(scanId)
                .taskId(taskId)
                .serviceName(serviceName)
                .checkItemName(checkItemName)
                .status("RUNNING")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 创建任务完成消息
     */
    public static ScanProgressMessage taskComplete(Long scanId, Long taskId, String serviceName,
                                                    String checkItemName, int issueCount, String status) {
        return ScanProgressMessage.builder()
                .type(TYPE_TASK_COMPLETE)
                .scanId(scanId)
                .taskId(taskId)
                .serviceName(serviceName)
                .checkItemName(checkItemName)
                .issueCount(issueCount)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 创建扫描完成消息
     */
    public static ScanProgressMessage completed(Long scanId, int totalIssues, int progress) {
        return ScanProgressMessage.builder()
                .type(TYPE_COMPLETED)
                .scanId(scanId)
                .issueCount(totalIssues)
                .progress(progress)
                .status("COMPLETED")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 创建扫描取消消息
     */
    public static ScanProgressMessage cancelled(Long scanId, String message) {
        return ScanProgressMessage.builder()
                .type(TYPE_CANCELLED)
                .scanId(scanId)
                .message(message)
                .status("CANCELLED")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 创建错误消息
     */
    public static ScanProgressMessage error(Long scanId, String message) {
        return ScanProgressMessage.builder()
                .type(TYPE_ERROR)
                .scanId(scanId)
                .message(message)
                .status("ERROR")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
