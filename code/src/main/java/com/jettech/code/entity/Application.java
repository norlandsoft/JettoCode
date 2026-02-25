package com.jettech.code.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Application {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
