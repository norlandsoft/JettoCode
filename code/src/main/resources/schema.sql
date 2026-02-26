-- JettoCode Database Schema
-- Three-tier structure: Application -> Service -> ServiceVersion

DROP TABLE IF EXISTS application;
CREATE TABLE IF NOT EXISTS application (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS service;
CREATE TABLE IF NOT EXISTS service (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    git_url VARCHAR(500),
    local_path VARCHAR(500),
    current_branch VARCHAR(255),
    last_commit VARCHAR(255),
    description TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (application_id) REFERENCES application(id) ON DELETE CASCADE,
    INDEX idx_application_id (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS service_version;
CREATE TABLE IF NOT EXISTS service_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_id BIGINT NOT NULL,
    version VARCHAR(100) NOT NULL,
    commit_id VARCHAR(255),
    description TEXT,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (service_id) REFERENCES service(id) ON DELETE CASCADE,
    INDEX idx_service_id (service_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS dependency;
CREATE TABLE IF NOT EXISTS dependency (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    version VARCHAR(100),
    group_id VARCHAR(255),
    artifact_id VARCHAR(255),
    type VARCHAR(50),
    scope VARCHAR(50),
    license VARCHAR(255),
    license_status VARCHAR(50),
    purl VARCHAR(500),
    file_path VARCHAR(500),
    checksum VARCHAR(255),
    created_at DATETIME NOT NULL,
    FOREIGN KEY (service_id) REFERENCES service(id) ON DELETE CASCADE,
    INDEX idx_service_id (service_id),
    INDEX idx_name (name),
    INDEX idx_license_status (license_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS vulnerability;
CREATE TABLE IF NOT EXISTS vulnerability (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dependency_id BIGINT NOT NULL,
    cve_id VARCHAR(100),
    cwe_id VARCHAR(100),
    severity VARCHAR(50),
    cvss_score DOUBLE,
    title VARCHAR(500),
    description TEXT,
    affected_version VARCHAR(255),
    fixed_version VARCHAR(255),
    `references` TEXT,
    status VARCHAR(50),
    created_at DATETIME NOT NULL,
    FOREIGN KEY (dependency_id) REFERENCES dependency(id) ON DELETE CASCADE,
    INDEX idx_dependency_id (dependency_id),
    INDEX idx_severity (severity),
    INDEX idx_cve_id (cve_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS security_scan;
CREATE TABLE IF NOT EXISTS security_scan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_id BIGINT NOT NULL,
    scan_type VARCHAR(50),
    status VARCHAR(50),
    total_dependencies INT,
    vulnerable_dependencies INT,
    critical_count INT,
    high_count INT,
    medium_count INT,
    low_count INT,
    license_violation_count INT,
    malware_count INT,
    report_path VARCHAR(500),
    started_at DATETIME,
    completed_at DATETIME,
    created_at DATETIME NOT NULL,
    checked_count INT DEFAULT 0,
    current_phase VARCHAR(100),
    current_dependency VARCHAR(255),
    progress INT DEFAULT 0,
    FOREIGN KEY (service_id) REFERENCES service(id) ON DELETE CASCADE,
    INDEX idx_service_id (service_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS license_rule;
CREATE TABLE IF NOT EXISTS license_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_pattern VARCHAR(255) NOT NULL,
    license_type VARCHAR(100) NOT NULL,
    ecosystem VARCHAR(50),
    source VARCHAR(50),
    created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS vulnerability_cache;
CREATE TABLE IF NOT EXISTS vulnerability_cache (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_pattern VARCHAR(255) NOT NULL,
    ecosystem VARCHAR(50),
    source VARCHAR(50),
    priority INT DEFAULT 0,
    created_at DATETIME NOT NULL,
    expires_at DATETIME,
    raw_data LONGTEXT,
    INDEX idx_package_pattern (package_pattern),
    INDEX idx_ecosystem (ecosystem),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS code_quality_scan;
CREATE TABLE IF NOT EXISTS code_quality_scan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_id BIGINT NOT NULL,
    status VARCHAR(50),
    total_files INT DEFAULT 0,
    total_issues INT DEFAULT 0,
    security_issues INT DEFAULT 0,
    reliability_issues INT DEFAULT 0,
    maintainability_issues INT DEFAULT 0,
    code_smell_issues INT DEFAULT 0,
    blocker_count INT DEFAULT 0,
    critical_count INT DEFAULT 0,
    major_count INT DEFAULT 0,
    minor_count INT DEFAULT 0,
    info_count INT DEFAULT 0,
    quality_score DOUBLE DEFAULT 0,
    security_score DOUBLE DEFAULT 0,
    reliability_score DOUBLE DEFAULT 0,
    maintainability_score DOUBLE DEFAULT 0,
    report_path VARCHAR(500),
    started_at DATETIME,
    completed_at DATETIME,
    created_at DATETIME NOT NULL,
    checked_count INT DEFAULT 0,
    current_phase VARCHAR(100),
    current_file VARCHAR(500),
    progress INT DEFAULT 0,
    FOREIGN KEY (service_id) REFERENCES service(id) ON DELETE CASCADE,
    INDEX idx_service_id (service_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS code_quality_issue;
CREATE TABLE IF NOT EXISTS code_quality_issue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scan_id BIGINT NOT NULL,
    file_path VARCHAR(500),
    line INT,
    `column` INT,
    category VARCHAR(50),
    severity VARCHAR(50),
    rule_id VARCHAR(100),
    rule_name VARCHAR(255),
    message TEXT,
    suggestion TEXT,
    code_snippet LONGTEXT,
    status VARCHAR(50),
    created_at DATETIME NOT NULL,
    FOREIGN KEY (scan_id) REFERENCES code_quality_scan(id) ON DELETE CASCADE,
    INDEX idx_scan_id (scan_id),
    INDEX idx_category (category),
    INDEX idx_severity (severity),
    INDEX idx_file_path (file_path(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 质量检查分组表
DROP TABLE IF EXISTS quality_check_group;
CREATE TABLE IF NOT EXISTS quality_check_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_key VARCHAR(50) NOT NULL UNIQUE COMMENT '分组标识',
    group_name VARCHAR(255) NOT NULL COMMENT '分组名称',
    description TEXT COMMENT '分组描述',
    sort_order INT DEFAULT 0 COMMENT '排序',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_enabled_sort (enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='质量检查分组';

-- 质量检查配置表
DROP TABLE IF EXISTS quality_check_config;
CREATE TABLE IF NOT EXISTS quality_check_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL COMMENT '所属分组ID',
    item_key VARCHAR(100) NOT NULL COMMENT '检查项标识',
    item_name VARCHAR(255) NOT NULL COMMENT '检查项名称',
    description TEXT COMMENT '检查项描述',
    prompt_template LONGTEXT NOT NULL COMMENT 'AI提示词模板',
    severity VARCHAR(50) DEFAULT 'MAJOR' COMMENT '默认严重程度',
    sort_order INT DEFAULT 0 COMMENT '排序',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES quality_check_group(id) ON DELETE CASCADE,
    UNIQUE KEY uk_group_item (group_id, item_key),
    INDEX idx_enabled (enabled),
    INDEX idx_group_sort (group_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='质量检查配置';

-- 代码质量扫描任务表
DROP TABLE IF EXISTS code_quality_task;
CREATE TABLE IF NOT EXISTS code_quality_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scan_id BIGINT NOT NULL COMMENT '父扫描ID',
    service_id BIGINT NOT NULL COMMENT '要扫描的服务ID',
    service_name VARCHAR(255) COMMENT '服务名称（用于显示）',
    check_item_id BIGINT NOT NULL COMMENT '检查项ID',
    check_item_key VARCHAR(100) NOT NULL COMMENT '检查项标识',
    check_item_name VARCHAR(255) COMMENT '检查项名称（用于显示）',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态: PENDING, RUNNING, COMPLETED, FAILED',
    priority INT DEFAULT 0 COMMENT '执行优先级',
    opencode_session_id VARCHAR(100) COMMENT 'OpenCode会话ID',
    prompt_text TEXT COMMENT '发送给OpenCode的完整提示词',
    response_text LONGTEXT COMMENT 'OpenCode的响应',
    issue_count INT DEFAULT 0 COMMENT '发现的问题数量',
    severity VARCHAR(50) COMMENT '整体严重级别: NONE, LOW, MEDIUM, HIGH, CRITICAL',
    result_summary TEXT COMMENT '扫描结果摘要',
    started_at DATETIME COMMENT '任务开始时间',
    completed_at DATETIME COMMENT '任务完成时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    error_message TEXT COMMENT '错误信息',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    FOREIGN KEY (scan_id) REFERENCES code_quality_scan(id) ON DELETE CASCADE,
    FOREIGN KEY (service_id) REFERENCES service(id) ON DELETE CASCADE,
    FOREIGN KEY (check_item_id) REFERENCES quality_check_config(id) ON DELETE CASCADE,
    INDEX idx_scan_id (scan_id),
    INDEX idx_service_id (service_id),
    INDEX idx_check_item_id (check_item_id),
    INDEX idx_status (status),
    INDEX idx_opencode_session (opencode_session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码质量扫描任务表';
