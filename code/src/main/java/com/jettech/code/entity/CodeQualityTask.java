package com.jettech.code.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 代码质量扫描任务实体
 * 每个任务对应一个服务和一个检查项的组合
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeQualityTask {

    // 任务状态常量
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    // 严重级别常量
    public static final String SEVERITY_NONE = "NONE";
    public static final String SEVERITY_LOW = "LOW";
    public static final String SEVERITY_MEDIUM = "MEDIUM";
    public static final String SEVERITY_HIGH = "HIGH";
    public static final String SEVERITY_CRITICAL = "CRITICAL";

    private Long id;
    private Long scanId;
    private Long serviceId;
    private String serviceName;
    private Long checkItemId;
    private String checkItemKey;
    private String checkItemName;

    // 任务状态
    private String status;
    private Integer priority;

    // OpenCode 会话信息
    private String opencodeSessionId;

    // 提示词和响应
    private String promptText;
    private String responseText;

    // 结果摘要
    private Integer issueCount;
    private String severity;
    private String resultSummary;

    // 时间戳
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 错误处理
    private String errorMessage;
    private Integer retryCount;
}
