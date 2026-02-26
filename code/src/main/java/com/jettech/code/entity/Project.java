package com.jettech.code.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
