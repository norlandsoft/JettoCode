package com.jettech.code.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreateServiceRequest {
    private Long applicationId;
    private String name;
    private String gitUrl;
    private String description;
}

