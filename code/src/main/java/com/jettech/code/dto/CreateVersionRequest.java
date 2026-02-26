package com.jettech.code.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVersionRequest {
    private Long serviceId;
    private String version;
    private String commitId;
    private String description;
}
