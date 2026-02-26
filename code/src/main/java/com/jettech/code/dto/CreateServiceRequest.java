package com.jettech.code.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateServiceRequest {
    private Long applicationId;
    private String name;
    private String gitUrl;
    private String description;
}

