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
