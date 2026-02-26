package com.jettech.code.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SecurityScan {
    private Long id;
    private Long serviceId;
    private String scanType;
    private String status;
    private Integer totalDependencies;
    private Integer vulnerableDependencies;
    private Integer criticalCount;
    private Integer highCount;
    private Integer mediumCount;
    private Integer lowCount;
    private Integer licenseViolationCount;
    private Integer malwareCount;
    private String reportPath;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    
    private Integer checkedCount;
    private String currentPhase;
    private String currentDependency;
    private Integer progress;
}
