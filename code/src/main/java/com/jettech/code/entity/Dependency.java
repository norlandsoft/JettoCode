package com.jettech.code.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dependency {
    private Long id;
    private Long serviceId;
    private String name;
    private String version;
    private String groupId;
    private String artifactId;
    private String type;
    private String scope;
    private String license;
    private String licenseStatus;
    private String purl;
    private String filePath;
    private String checksum;
    private LocalDateTime createdAt;

    /**
     * 非持久化字段，用于临时存储漏洞数量
     * transient 关键字防止Java序列化，但Jackson会正常序列化此字段
     */
    @Setter
    private transient Integer vulnerabilityCount;
}
