-- =====================================================
-- JettoCode 初始化数据
-- =====================================================

-- =====================================================
-- 1. 插入质量检查分组
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
-- 2. 插入质量检查配置项 - 文档检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'documentation'),
'documentation-api', 'API文档检查', '检测API文档的完整性',
'请检查 API 文档的完整性，包括：\n- 检查是否有缺失的 API 文档注释\n- 检查参数说明是否完整\n- 检查返回值说明是否清晰\n- 检查是否有示例代码', 'MAJOR', 1),
((SELECT id FROM quality_check_group WHERE group_key = 'documentation'),
'documentation-readme', 'README检查', '检测README文档的完整性',
'请检查 README 文档的完整性，包括：\n- 项目简介是否清晰\n- 安装说明是否完整\n- 使用说明是否详细\n- 是否有贡献指南', 'MINOR', 2),
((SELECT id FROM quality_check_group WHERE group_key = 'documentation'),
'documentation-changelog', '变更日志检查', '检测变更日志的维护情况',
'请检查变更日志的维护情况，包括：\n- 是否有 CHANGELOG 文件\n- 版本变更记录是否完整\n- 是否记录了重要变更', 'MINOR', 3);

-- =====================================================
-- 3. 插入质量检查配置项 - 依赖管理检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'dependency'),
'dependency-security', '依赖安全漏洞检查', '检测依赖包的已知安全漏洞',
'请检查依赖包的安全漏洞，包括：\n- 检查是否使用了已知有漏洞的依赖版本\n- 检查依赖是否需要更新\n- 检查是否有安全相关的依赖配置', 'CRITICAL', 1),
((SELECT id FROM quality_check_group WHERE group_key = 'dependency'),
'dependency-version', '依赖版本检查', '检测过时的依赖版本',
'请检查依赖版本是否过时，包括：\n- 检查是否有重大版本更新\n- 检查是否有次要版本更新\n- 建议升级到稳定版本', 'MAJOR', 2),
((SELECT id FROM quality_check_group WHERE group_key = 'dependency'),
'dependency-license', '许可证合规检查', '检测依赖许可证的合规性',
'请检查依赖许可证的合规性，包括：\n- 识别所有依赖的许可证类型\n- 检查是否存在许可证冲突\n- 检查是否使用了限制性许可证', 'HIGH', 3),
((SELECT id FROM quality_check_group WHERE group_key = 'dependency'),
'dependency-unused', '未使用依赖检查', '检测未被使用的依赖',
'请检查未使用的依赖，包括：\n- 识别声明但未使用的依赖\n- 检查是否有冗余的依赖\n- 建议移除未使用的依赖', 'MINOR', 4);

-- =====================================================
-- 4. 插入质量检查配置项 - 数据验证检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'data-validation'),
'data-validation-input', '输入验证检查', '检测用户输入的验证',
'请检查用户输入的验证，包括：\n- 检查是否有未验证的用户输入\n- 检查输入验证是否完整\n- 检查是否有 SQL 注入、XSS 等风险', 'CRITICAL', 1),
((SELECT id FROM quality_check_group WHERE group_key = 'data-validation'),
'data-validation-output', '输出编码检查', '检测输出数据的编码',
'请检查输出数据的编码，包括：\n- 检查是否有未编码的输出\n- 检查是否正确处理了特殊字符\n- 检查是否有 XSS 风险', 'HIGH', 2),
((SELECT id FROM quality_check_group WHERE group_key = 'data-validation'),
'data-validation-boundary', '边界检查', '检测数据边界验证',
'请检查数据边界验证，包括：\n- 检查数组边界检查\n- 检查数值范围验证\n- 检查字符串长度限制', 'HIGH', 3),
((SELECT id FROM quality_check_group WHERE group_key = 'data-validation'),
'data-validation-type', '类型安全检查', '检测数据类型验证',
'请检查数据类型验证，包括：\n- 检查类型转换是否安全\n- 检查是否有类型混淆风险\n- 检查空值处理', 'MAJOR', 4);

-- =====================================================
-- 5. 插入质量检查配置项 - 错误处理检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'error-handling'),
'error-handling-coverage', '错误处理覆盖率', '检测错误处理的覆盖程度',
'请检查错误处理的覆盖程度，包括：\n- 检查是否有未捕获的异常\n- 检查 catch 块是否为空\n- 检查错误是否被正确记录\n- 检查是否有适当的错误恢复机制', 'MAJOR', 1),
((SELECT id FROM quality_check_group WHERE group_key = 'error-handling'),
'error-handling-recovery', '错误恢复机制', '检测错误恢复机制的完整性',
'请检查错误恢复机制的完整性，包括：\n- 检查是否有重试机制\n- 检查是否有降级处理\n- 检查是否有事务回滚', 'HIGH', 2),
((SELECT id FROM quality_check_group WHERE group_key = 'error-handling'),
'error-handling-message', '错误信息质量', '检测错误信息的可用性',
'请检查错误信息的质量，包括：\n- 检查错误信息是否清晰\n- 检查错误信息是否包含足够的上下文\n- 检查是否泄露了敏感信息', 'MINOR', 3);

-- =====================================================
-- 6. 插入质量检查配置项 - 无障碍访问检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'accessibility'),
'accessibility-aria', 'ARIA属性检查', '检测ARIA属性的正确使用',
'请检查 ARIA 属性的正确使用，包括：\n- 检查 ARIA 属性是否正确\n- 检查是否有缺失的 ARIA 属性\n- 检查 ARIA 属性是否与角色匹配', 'MAJOR', 1),
((SELECT id FROM quality_check_group WHERE group_key = 'accessibility'),
'accessibility-semantic', '语义化HTML检查', '检测HTML语义化程度',
'请检查 HTML 语义化程度，包括：\n- 检查是否使用了语义化标签\n- 检查标题层级是否正确\n- 检查是否有正确的 landmark', 'MAJOR', 2),
((SELECT id FROM quality_check_group WHERE group_key = 'accessibility'),
'accessibility-keyboard', '键盘导航检查', '检测键盘导航支持',
'请检查键盘导航支持，包括：\n- 检查所有交互元素是否可聚焦\n- 检查 tabindex 使用是否正确\n- 检查键盘快捷键是否合理', 'MAJOR', 3),
((SELECT id FROM quality_check_group WHERE group_key = 'accessibility'),
'accessibility-color', '颜色对比度检查', '检测颜色对比度是否达标',
'请检查颜色对比度是否达标，包括：\n- 检查文本与背景对比度\n- 检查是否仅依赖颜色传达信息\n- 检查是否支持高对比度模式', 'MINOR', 4);

-- =====================================================
-- 7. 插入质量检查配置项 - 国际化检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'internationalization'),
'i18n-hardcoded', '硬编码字符串检查', '检测硬编码的文本字符串',
'请检查硬编码的文本字符串，包括：\n- 检查是否有硬编码的用户界面文本\n- 检查是否有硬编码的日期/数字格式\n- 建议使用 i18n 资源文件', 'MAJOR', 1),
((SELECT id FROM quality_check_group WHERE group_key = 'internationalization'),
'i18n-datetime', '日期时间格式检查', '检测日期时间格式化',
'请检查日期时间格式化，包括：\n- 检查是否使用了 locale-aware 的日期格式\n- 检查时区处理是否正确\n- 建议使用标准化的日期格式', 'MINOR', 2),
((SELECT id FROM quality_check_group WHERE group_key = 'internationalization'),
'i18n-number', '数字格式检查', '检测数字和货币格式化',
'请检查数字和货币格式化，包括：\n- 检查是否使用了 locale-aware 的数字格式\n- 检查货币符号处理是否正确\n- 检查小数点和千位分隔符', 'MINOR', 3),
((SELECT id FROM quality_check_group WHERE group_key = 'internationalization'),
'i18n-rtl', 'RTL支持检查', '检测从右到左语言支持',
'请检查从右到左语言支持，包括：\n- 检查布局是否支持 RTL\n- 检查文本方向设置\n- 检查镜像布局支持', 'MINOR', 4);

-- =====================================================
-- 8. 插入质量检查配置项 - API设计检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'api-design'),
'api-design-restful', 'RESTful规范检查', '检测RESTful API设计规范',
'请检查 RESTful API 设计规范，包括：\n- 检查 HTTP 方法使用是否正确\n- 检查 URL 命名是否规范\n- 检查状态码使用是否正确\n- 检查资源命名是否一致', 'MAJOR', 1),
((SELECT id FROM quality_check_group WHERE group_key = 'api-design'),
'api-design-versioning', 'API版本控制检查', '检测API版本控制策略',
'请检查 API 版本控制策略，包括：\n- 检查是否有版本控制机制\n- 检查版本号格式是否规范\n- 检查版本兼容性处理', 'MAJOR', 2),
((SELECT id FROM quality_check_group WHERE group_key = 'api-design'),
'api-design-pagination', '分页设计检查', '检测API分页设计',
'请检查 API 分页设计，包括：\n- 检查是否有分页支持\n- 检查分页参数是否规范\n- 检查分页元数据是否完整', 'MINOR', 3),
((SELECT id FROM quality_check_group WHERE group_key = 'api-design'),
'api-design-error-response', '错误响应格式检查', '检测API错误响应格式',
'请检查 API 错误响应格式，包括：\n- 检查错误响应格式是否统一\n- 检查错误码是否规范\n- 检查错误信息是否清晰', 'MAJOR', 4);

-- =====================================================
-- 9. 插入质量检查配置项 - 状态管理检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'state-management'),
'state-mutation', '状态变更检查', '检测不可预期的状态变更',
'请检查不可预期的状态变更，包括：\n- 检查是否有直接的状态修改\n- 检查状态变更是否可追踪\n- 检查是否有状态同步问题', 'HIGH', 1),
((SELECT id FROM quality_check_group WHERE group_key = 'state-management'),
'state-synchronization', '状态同步检查', '检测状态同步问题',
'请检查状态同步问题，包括：\n- 检查多组件状态一致性\n- 检查异步状态更新\n- 检查状态竞态条件', 'MAJOR', 2),
((SELECT id FROM quality_check_group WHERE group_key = 'state-management'),
'state-persistence', '状态持久化检查', '检测状态持久化策略',
'请检查状态持久化策略，包括：\n- 检查是否有必要的状态持久化\n- 检查持久化时机是否合适\n- 检查状态恢复机制', 'MINOR', 3);

-- =====================================================
-- 10. 插入质量检查配置项 - 资源使用检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'resource-usage'),
'resource-memory', '内存使用检查', '检测内存使用效率',
'请检查内存使用效率，包括：\n- 检查是否有内存泄漏风险\n- 检查大对象处理是否合理\n- 检查缓存使用是否合理', 'HIGH', 1),
((SELECT id FROM quality_check_group WHERE group_key = 'resource-usage'),
'resource-cpu', 'CPU使用检查', '检测CPU使用效率',
'请检查 CPU 使用效率，包括：\n- 检查是否有 CPU 密集型操作\n- 检查循环和递归是否高效\n- 检查是否有不必要的计算', 'MEDIUM', 2),
((SELECT id FROM quality_check_group WHERE group_key = 'resource-usage'),
'resource-network', '网络资源检查', '检测网络资源使用',
'请检查网络资源使用，包括：\n- 检查是否有不必要的网络请求\n- 检查请求是否可以合并\n- 检查是否有缓存策略', 'MEDIUM', 3),
((SELECT id FROM quality_check_group WHERE group_key = 'resource-usage'),
'resource-disk', '磁盘使用检查', '检测磁盘I/O效率',
'请检查磁盘 I/O 效率，包括：\n- 检查文件读写是否高效\n- 检查是否有不必要的磁盘操作\n- 检查缓冲区使用是否合理', 'MEDIUM', 4);

-- =====================================================
-- 11. 插入质量检查配置项 - 向后兼容性检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'backward-compatibility'),
'compatibility-api', 'API兼容性检查', '检测API变更的向后兼容性',
'请检查 API 变更的向后兼容性，包括：\n- 检查是否有破坏性变更\n- 检查是否有废弃的 API\n- 检查版本迁移路径', 'HIGH', 1),
((SELECT id FROM quality_check_group WHERE group_key = 'backward-compatibility'),
'compatibility-database', '数据库兼容性检查', '检测数据库变更的向后兼容性',
'请检查数据库变更的向后兼容性，包括：\n- 检查 schema 变更是否兼容\n- 检查数据迁移脚本\n- 检查是否有回滚方案', 'HIGH', 2),
((SELECT id FROM quality_check_group WHERE group_key = 'backward-compatibility'),
'compatibility-config', '配置兼容性检查', '检测配置变更的向后兼容性',
'请检查配置变更的向后兼容性，包括：\n- 检查配置项变更是否兼容\n- 检查默认值变更\n- 检查配置迁移指南', 'MAJOR', 3);

-- =====================================================
-- 12. 插入质量检查配置项 - 合规性检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'compliance'),
'compliance-gdpr', 'GDPR合规检查', '检测GDPR数据保护合规',
'请检查 GDPR 数据保护合规，包括：\n- 检查个人数据收集是否合规\n- 检查数据存储和处理\n- 检查用户同意机制', 'CRITICAL', 1),
((SELECT id FROM quality_check_group WHERE group_key = 'compliance'),
'compliance-pci', 'PCI DSS合规检查', '检测支付卡行业数据安全标准',
'请检查 PCI DSS 支付卡行业数据安全标准，包括：\n- 检查支付数据处理\n- 检查卡号存储是否安全\n- 检查日志是否包含敏感信息', 'CRITICAL', 2),
((SELECT id FROM quality_check_group WHERE group_key = 'compliance'),
'compliance-hipaa', 'HIPAA合规检查', '检测医疗数据保护合规',
'请检查 HIPAA 医疗数据保护合规，包括：\n- 检查医疗数据处理\n- 检查访问控制\n- 检查审计日志', 'CRITICAL', 3),
((SELECT id FROM quality_check_group WHERE group_key = 'compliance'),
'compliance-logging', '审计日志检查', '检测审计日志的完整性',
'请检查审计日志的完整性，包括：\n- 检查关键操作是否记录\n- 检查日志格式是否规范\n- 检查日志存储是否安全', 'HIGH', 4);

-- =====================================================
-- 13. 插入质量检查配置项 - 最佳实践检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'best-practices'),
'best-practices-patterns', '设计模式检查', '检测设计模式的使用',
'请检查设计模式的使用，包括：\n- 检查是否正确使用了设计模式\n- 检查是否有过度设计\n- 检查是否有更合适的设计模式', 'MAJOR', 1),
((SELECT id FROM quality_check_group WHERE group_key = 'best-practices'),
'best-practices-idiomatic', '惯用代码检查', '检测代码是否符合语言惯用法',
'请检查代码是否符合语言惯用法，包括：\n- 检查是否使用了语言特性\n- 检查命名规范\n- 检查代码风格一致性', 'MINOR', 2),
((SELECT id FROM quality_check_group WHERE group_key = 'best-practices'),
'best-practices-antipatterns', '反模式检查', '检测常见的代码反模式',
'请检查常见的代码反模式，包括：\n- 检查是否有 God Object\n- 检查是否有 Spaghetti Code\n- 检查是否有 Copy-Paste 编程', 'MAJOR', 3),
((SELECT id FROM quality_check_group WHERE group_key = 'best-practices'),
'best-practices-dry', 'DRY原则检查', '检测代码重复违反DRY原则',
'请检查代码重复违反 DRY 原则，包括：\n- 检查是否有重复的代码块\n- 检查是否有相似的业务逻辑\n- 建议提取公共方法或组件', 'MAJOR', 4);
