package com.jettech.code.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LicenseRule {
    private Long id;
    private String packagePattern;
    private String licenseType;
    private String ecosystem;
    private String source;
    private LocalDateTime createdAt;
}
