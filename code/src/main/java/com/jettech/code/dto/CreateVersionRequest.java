package com.jettech.code.dto;

import lombok.Data;

@Data
public class CreateVersionRequest {
    private Long serviceId;
    private String version;
    private String commitId;
    private String description;
}
