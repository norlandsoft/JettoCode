package com.jettech.code.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeQualityScan {
    private Long id;
    private Long serviceId;
    private String status;
    private Integer totalFiles;
    private Integer totalIssues;
    private Integer securityIssues;
    private Integer reliabilityIssues;
    private Integer maintainabilityIssues;
    private Integer codeSmellIssues;
    private Integer blockerCount;
    private Integer criticalCount;
    private Integer majorCount;
    private Integer minorCount;
    private Integer infoCount;
    private Double qualityScore;
    private Double securityScore;
    private Double reliabilityScore;
    private Double maintainabilityScore;
    private String reportPath;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    private Integer checkedCount;
    private String currentPhase;
    private String currentFile;
    private Integer progress;
}
