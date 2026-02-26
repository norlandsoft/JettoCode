package com.jettech.code.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QualityCheckItem {
    private Long id;
    private String category;
    private String ruleId;
    private String ruleName;
    private String severity;
    private String description;
    private String promptTemplate;
    private Boolean enabled;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static final String CATEGORY_SECURITY = "SECURITY";
    public static final String CATEGORY_RELIABILITY = "RELIABILITY";
    public static final String CATEGORY_MAINTAINABILITY = "MAINTAINABILITY";
    public static final String CATEGORY_CODE_SMELL = "CODE_SMELL";
    public static final String CATEGORY_PERFORMANCE = "PERFORMANCE";

    public static final String SEVERITY_BLOCKER = "BLOCKER";
    public static final String SEVERITY_CRITICAL = "CRITICAL";
    public static final String SEVERITY_MAJOR = "MAJOR";
    public static final String SEVERITY_MINOR = "MINOR";
    public static final String SEVERITY_INFO = "INFO";
}
