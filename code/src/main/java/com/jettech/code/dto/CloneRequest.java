package com.jettech.code.dto;

import lombok.Data;

@Data
public class CloneRequest {
    private String gitUrl;
    private String name;
    private String branch;
}
