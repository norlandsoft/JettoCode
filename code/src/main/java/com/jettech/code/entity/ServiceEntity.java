package com.jettech.code.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ServiceEntity {
    private Long id;
    private Long applicationId;
    private String name;
    private String gitUrl;
    private String localPath;
    private String currentBranch;
    private String lastCommit;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
