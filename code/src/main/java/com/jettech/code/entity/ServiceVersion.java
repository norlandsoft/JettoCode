package com.jettech.code.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ServiceVersion {
    private Long id;
    private Long serviceId;
    private String version;
    private String commitId;
    private String description;
    private LocalDateTime createdAt;
}
