package com.jettech.code.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QualityCheckConfig {
    private Long id;
    private Long groupId;
    private String itemKey;
    private String itemName;
    private String description;
    private String promptTemplate;
    private String severity;
    private Integer sortOrder;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
