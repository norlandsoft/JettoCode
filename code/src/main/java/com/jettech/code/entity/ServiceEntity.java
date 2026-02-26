package com.jettech.code.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
