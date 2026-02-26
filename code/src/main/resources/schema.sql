-- JettoCode Database Schema
-- Three-tier structure: Application -> Service -> ServiceVersion

CREATE TABLE IF NOT EXISTS application (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    FOREIGN KEY (service_id) REFERENCES service(id) ON DELETE CASCADE,
    INDEX idx_service_id (service_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS license_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_pattern VARCHAR(255) NOT NULL,
    license_type VARCHAR(100) NOT NULL,
    ecosystem VARCHAR(50),
    source VARCHAR(50),
    created_at DATETIME NOT NULL,
    INDEX idx_package_pattern (package_pattern),
    INDEX idx_ecosystem (ecosystem)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
