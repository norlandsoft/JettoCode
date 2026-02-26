package com.jettech.code.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LicenseRule {
    private Long id;
    private String packagePattern;
    private String licenseType;
    private String ecosystem;
    private String source;
    private LocalDateTime createdAt;
}
