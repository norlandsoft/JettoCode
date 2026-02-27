package com.jettech.code.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务执行日志 DTO
 * 用于展示扫描任务的详细执行信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionLog {

    private Long taskId;
    private Long scanId;
    private String serviceName;
    private String checkItemKey;
    private String checkItemName;
    private String status;

    // 输入信息
    private String promptText;
    private Integer promptLength;
    private LocalDateTime startedAt;

    // 输出信息
    private String responseText;
    private Integer responseLength;
    private LocalDateTime completedAt;

    // 结果
    private Integer issueCount;
    private String severity;
    private String resultSummary;

    // 错误信息
    private String errorMessage;
    private Integer retryCount;

    // OpenCode 会话信息
    private String opencodeSessionId;

    // 执行时长（毫秒）
    private Long duration;

    /**
     * 获取格式化的执行时长
     */
    public String getFormattedDuration() {
        if (duration == null) return "-";

        if (duration < 1000) {
            return duration + "ms";
        } else if (duration < 60000) {
            return String.format("%.1fs", duration / 1000.0);
        } else {
            long minutes = duration / 60000;
            long seconds = (duration % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }

    /**
     * 获取状态显示文本
     */
    public String getStatusText() {
        if (status == null) return "未知";

        switch (status) {
            case "PENDING":
                return "等待中";
            case "RUNNING":
                return "执行中";
            case "COMPLETED":
                return "已完成";
            case "FAILED":
                return "失败";
            case "CANCELLED":
                return "已取消";
            default:
                return status;
        }
    }
}
