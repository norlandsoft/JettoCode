package com.jettech.code.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 扫描请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScanRequestDTO {

    /**
     * 选中的检查项ID列表
     */
    private List<Long> checkItemIds;

    /**
     * 选中的检查项Key列表（用于前端传值）
     */
    private List<String> checkItems;
}
