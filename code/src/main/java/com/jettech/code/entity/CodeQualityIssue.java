package com.jettech.code.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeQualityIssue {
    private Long id;
    private Long scanId;
    private String filePath;
    private Integer line;
    private Integer column;
    private String category;
    private String severity;
    private String ruleId;
    private String ruleName;
    private String message;
    private String suggestion;
    private String codeSnippet;
    private String status;
    private LocalDateTime createdAt;

    public static final String CATEGORY_SECURITY = "SECURITY";
    public static final String CATEGORY_RELIABILITY = "RELIABILITY";
    public static final String CATEGORY_MAINTAINABILITY = "MAINTAINABILITY";
    public static final String CATEGORY_CODE_SMELL = "CODE_SMELL";

    public static final String SEVERITY_BLOCKER = "BLOCKER";
    public static final String SEVERITY_CRITICAL = "CRITICAL";
    public static final String SEVERITY_MAJOR = "MAJOR";
    public static final String SEVERITY_MINOR = "MINOR";
    public static final String SEVERITY_INFO = "INFO";
}
