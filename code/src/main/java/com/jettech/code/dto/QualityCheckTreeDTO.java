package com.jettech.code.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 质量检查树形结构DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QualityCheckTreeDTO {
    private Long groupId;
    private String groupKey;
    private String groupName;
    private String description;
    private Integer sortOrder;
    private List<CheckItemDTO> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckItemDTO {
        private Long id;
        private String itemKey;
        private String itemName;
        private String description;
        private String promptTemplate;
        private String severity;
        private Integer sortOrder;
        private Boolean enabled;
    }
}
