-- =====================================================
-- 质量检查配置数据 - 12个分组及子项
-- =====================================================

-- =====================================================
-- 1. 插入检查分组
-- =====================================================
INSERT INTO quality_check_group (group_key, group_name, description, sort_order, enabled) VALUES
('documentation', '文档检查', '检测代码文档和注释的完整性与质量', 9, 1),
('dependency', '依赖管理检查', '检测依赖版本、安全漏洞和许可证问题', 10, 1),
('data-validation', '数据验证检查', '检测输入输出数据验证和边界检查', 11, 1),
('error-handling', '错误处理检查', '检测错误处理和恢复机制的完整性', 12, 1),
('accessibility', '无障碍访问检查', '检测代码的无障碍访问支持', 13, 1),
('internationalization', '国际化检查', '检测代码的国际化支持', 14, 1),
('api-design', 'API设计检查', '检测API设计规范和最佳实践', 15, 1),
('state-management', '状态管理检查', '检测状态管理和数据流问题', 16, 1),
('resource-usage', '资源使用检查', '检测资源使用效率和限制', 17, 1),
('backward-compatibility', '向后兼容性检查', '检测API和代码的向后兼容性', 18, 1),
('compliance', '合规性检查', '检测代码是否遵循行业规范和标准', 19, 1),
('best-practices', '最佳实践检查', '检测代码是否遵循语言和框架最佳实践', 20, 1);

-- =====================================================
-- 2. 插入检查配置 - 文档检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'documentation'),
'documentation-api', 'API文档检查', '检测API文档的完整性',
'略', 'MAJOR', 1),

((SELECT id FROM quality_check_group WHERE group_key = 'documentation'),
'documentation-readme', 'README检查', '检测README文档的完整性',
'略', 'MINOR', 2),

((SELECT id FROM quality_check_group WHERE group_key = 'documentation'),
'documentation-changelog', '变更日志检查', '检测变更日志的维护情况',
'略', 'MINOR', 3);

-- =====================================================
-- 3. 插入检查配置 - 依赖管理检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'dependency'),
'dependency-security', '依赖安全漏洞检查', '检测依赖包的已知安全漏洞',
'略', 'CRITICAL', 1),

((SELECT id FROM quality_check_group WHERE group_key = 'dependency'),
'dependency-version', '依赖版本检查', '检测过时的依赖版本',
'略', 'MAJOR', 2),

((SELECT id FROM quality_check_group WHERE group_key = 'dependency'),
'dependency-license', '许可证合规检查', '检测依赖许可证的合规性',
'略', 'HIGH', 3),

((SELECT id FROM quality_check_group WHERE group_key = 'dependency'),
'dependency-unused', '未使用依赖检查', '检测未被使用的依赖',
'略', 'MINOR', 4);

-- =====================================================
-- 4. 插入检查配置 - 数据验证检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'data-validation'),
'data-validation-input', '输入验证检查', '检测用户输入的验证',
'略', 'CRITICAL', 1),

((SELECT id FROM quality_check_group WHERE group_key = 'data-validation'),
'data-validation-output', '输出编码检查', '检测输出数据的编码',
'略', 'HIGH', 2),

((SELECT id FROM quality_check_group WHERE group_key = 'data-validation'),
'data-validation-boundary', '边界检查', '检测数据边界验证',
'略', 'HIGH', 3),

((SELECT id FROM quality_check_group WHERE group_key = 'data-validation'),
'data-validation-type', '类型安全检查', '检测数据类型验证',
'略', 'MAJOR', 4);

-- =====================================================
-- 5. 插入检查配置 - 错误处理检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'error-handling'),
'error-handling-coverage', '错误处理覆盖率', '检测错误处理的覆盖程度',
'略', 'MAJOR', 1),

((SELECT id FROM quality_check_group WHERE group_key = 'error-handling'),
'error-handling-recovery', '错误恢复机制', '检测错误恢复机制的完整性',
'略', 'HIGH', 2),

((SELECT id FROM quality_check_group WHERE group_key = 'error-handling'),
'error-handling-message', '错误信息质量', '检测错误信息的可用性',
'略', 'MINOR', 3);

-- =====================================================
-- 6. 插入检查配置 - 无障碍访问检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'accessibility'),
'accessibility-aria', 'ARIA属性检查', '检测ARIA属性的正确使用',
'略', 'MAJOR', 1),

((SELECT id FROM quality_check_group WHERE group_key = 'accessibility'),
'accessibility-semantic', '语义化HTML检查', '检测HTML语义化程度',
'略', 'MAJOR', 2),

((SELECT id FROM quality_check_group WHERE group_key = 'accessibility'),
'accessibility-keyboard', '键盘导航检查', '检测键盘导航支持',
'略', 'MAJOR', 3),

((SELECT id FROM quality_check_group WHERE group_key = 'accessibility'),
'accessibility-color', '颜色对比度检查', '检测颜色对比度是否达标',
'略', 'MINOR', 4);

-- =====================================================
-- 7. 插入检查配置 - 国际化检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'internationalization'),
'i18n-hardcoded', '硬编码字符串检查', '检测硬编码的文本字符串',
'略', 'MAJOR', 1),

((SELECT id FROM quality_check_group WHERE group_key = 'internationalization'),
'i18n-datetime', '日期时间格式检查', '检测日期时间格式化',
'略', 'MINOR', 2),

((SELECT id FROM quality_check_group WHERE group_key = 'internationalization'),
'i18n-number', '数字格式检查', '检测数字和货币格式化',
'略', 'MINOR', 3),

((SELECT id FROM quality_check_group WHERE group_key = 'internationalization'),
'i18n-rtl', 'RTL支持检查', '检测从右到左语言支持',
'略', 'MINOR', 4);

-- =====================================================
-- 8. 插入检查配置 - API设计检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'api-design'),
'api-design-restful', 'RESTful规范检查', '检测RESTful API设计规范',
'略', 'MAJOR', 1),

((SELECT id FROM quality_check_group WHERE group_key = 'api-design'),
'api-design-versioning', 'API版本控制检查', '检测API版本控制策略',
'略', 'MAJOR', 2),

((SELECT id FROM quality_check_group WHERE group_key = 'api-design'),
'api-design-pagination', '分页设计检查', '检测API分页设计',
'略', 'MINOR', 3),

((SELECT id FROM quality_check_group WHERE group_key = 'api-design'),
'api-design-error-response', '错误响应格式检查', '检测API错误响应格式',
'略', 'MAJOR', 4);

-- =====================================================
-- 9. 插入检查配置 - 状态管理检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'state-management'),
'state-mutation', '状态变更检查', '检测不可预期的状态变更',
'略', 'HIGH', 1),

((SELECT id FROM quality_check_group WHERE group_key = 'state-management'),
'state-synchronization', '状态同步检查', '检测状态同步问题',
'略', 'MAJOR', 2),

((SELECT id FROM quality_check_group WHERE group_key = 'state-management'),
'state-persistence', '状态持久化检查', '检测状态持久化策略',
'略', 'MINOR', 3);

-- =====================================================
-- 10. 插入检查配置 - 资源使用检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'resource-usage'),
'resource-memory', '内存使用检查', '检测内存使用效率',
'略', 'HIGH', 1),

((SELECT id FROM quality_check_group WHERE group_key = 'resource-usage'),
'resource-cpu', 'CPU使用检查', '检测CPU使用效率',
'略', 'MEDIUM', 2),

((SELECT id FROM quality_check_group WHERE group_key = 'resource-usage'),
'resource-network', '网络资源检查', '检测网络资源使用',
'略', 'MEDIUM', 3),

((SELECT id FROM quality_check_group WHERE group_key = 'resource-usage'),
'resource-disk', '磁盘使用检查', '检测磁盘I/O效率',
'略', 'MEDIUM', 4);

-- =====================================================
-- 11. 插入检查配置 - 向后兼容性检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'backward-compatibility'),
'compatibility-api', 'API兼容性检查', '检测API变更的向后兼容性',
'略', 'HIGH', 1),

((SELECT id FROM quality_check_group WHERE group_key = 'backward-compatibility'),
'compatibility-database', '数据库兼容性检查', '检测数据库变更的向后兼容性',
'略', 'HIGH', 2),

((SELECT id FROM quality_check_group WHERE group_key = 'backward-compatibility'),
'compatibility-config', '配置兼容性检查', '检测配置变更的向后兼容性',
'略', 'MAJOR', 3);

-- =====================================================
-- 12. 插入检查配置 - 合规性检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'compliance'),
'compliance-gdpr', 'GDPR合规检查', '检测GDPR数据保护合规',
'略', 'CRITICAL', 1),

((SELECT id FROM quality_check_group WHERE group_key = 'compliance'),
'compliance-pci', 'PCI DSS合规检查', '检测支付卡行业数据安全标准',
'略', 'CRITICAL', 2),

((SELECT id FROM quality_check_group WHERE group_key = 'compliance'),
'compliance-hipaa', 'HIPAA合规检查', '检测医疗数据保护合规',
'略', 'CRITICAL', 3),

((SELECT id FROM quality_check_group WHERE group_key = 'compliance'),
'compliance-logging', '审计日志检查', '检测审计日志的完整性',
'略', 'HIGH', 4);

-- =====================================================
-- 13. 插入检查配置 - 最佳实践检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'best-practices'),
'best-practices-patterns', '设计模式检查', '检测设计模式的使用',
'略', 'MAJOR', 1),

((SELECT id FROM quality_check_group WHERE group_key = 'best-practices'),
'best-practices-idiomatic', '惯用代码检查', '检测代码是否符合语言惯用法',
'略', 'MINOR', 2),

((SELECT id FROM quality_check_group WHERE group_key = 'best-practices'),
'best-practices-antipatterns', '反模式检查', '检测常见的代码反模式',
'略', 'MAJOR', 3),

((SELECT id FROM quality_check_group WHERE group_key = 'best-practices'),
'best-practices-dry', 'DRY原则检查', '检测代码重复违反DRY原则',
'略', 'MAJOR', 4);
