package com.jettech.code.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 质量检查配置更新DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QualityCheckConfigUpdateDTO {
    private String promptTemplate;
    private String itemName;
    private String description;
    private String severity;
    private Boolean enabled;
}
