package com.jettech.code.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceVersion {
    private Long id;
    private Long serviceId;
    private String version;
    private String commitId;
    private String description;
    private LocalDateTime createdAt;
}
