-- Code Quality Scan Task Table
-- Stores individual scan tasks for service x check item combinations

CREATE TABLE IF NOT EXISTS code_quality_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scan_id BIGINT NOT NULL COMMENT 'Parent scan ID',
    service_id BIGINT NOT NULL COMMENT 'Service to scan',
    service_name VARCHAR(255) COMMENT 'Service name for display',
    check_item_id BIGINT NOT NULL COMMENT 'Check item ID',
    check_item_key VARCHAR(100) NOT NULL COMMENT 'Check item key',
    check_item_name VARCHAR(255) COMMENT 'Check item name for display',

    -- Task status
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT 'Task status: PENDING, RUNNING, COMPLETED, FAILED',
    priority INT DEFAULT 0 COMMENT 'Execution priority',

    -- OpenCode session info
    opencode_session_id VARCHAR(100) COMMENT 'OpenCode session ID',

    -- Prompt and response
    prompt_text TEXT COMMENT 'Full prompt sent to OpenCode',
    response_text LONGTEXT COMMENT 'Response from OpenCode',

    -- Result summary
    issue_count INT DEFAULT 0 COMMENT 'Number of issues found',
    severity VARCHAR(50) COMMENT 'Overall severity: NONE, LOW, MEDIUM, HIGH, CRITICAL',
    result_summary TEXT COMMENT 'Brief summary of scan result',

    -- Timestamps
    started_at DATETIME COMMENT 'When task started',
    completed_at DATETIME COMMENT 'When task completed',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Error handling
    error_message TEXT COMMENT 'Error message if failed',
    retry_count INT DEFAULT 0 COMMENT 'Number of retries',

    INDEX idx_scan_id (scan_id),
    INDEX idx_service_id (service_id),
    INDEX idx_check_item_id (check_item_id),
    INDEX idx_status (status),
    INDEX idx_opencode_session (opencode_session_id),

    FOREIGN KEY (scan_id) REFERENCES code_quality_scan(id) ON DELETE CASCADE,
    FOREIGN KEY (service_id) REFERENCES service(id) ON DELETE CASCADE,
    FOREIGN KEY (check_item_id) REFERENCES quality_check_item(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Code quality scan task table';
