package com.jettech.code.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Project {
    private Long id;
    private String name;
    private String gitUrl;
    private String localPath;
    private String currentBranch;
    private String lastCommit;
    private LocalDateTime lastAnalyzedAt;
    private LocalDateTime createdAt;
}
