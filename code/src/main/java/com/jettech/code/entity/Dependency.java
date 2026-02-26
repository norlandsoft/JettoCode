package com.jettech.code.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Dependency {
    private Long id;
    private Long serviceId;
    private String name;
    private String version;
    private String groupId;
    private String artifactId;
    private String type;
    private String scope;
    private String license;
    private String licenseStatus;
    private String purl;
    private String filePath;
    private String checksum;
    private LocalDateTime createdAt;
    
    private transient Integer vulnerabilityCount;
}
