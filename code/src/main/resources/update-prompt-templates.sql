-- 更新 promptTemplate 内容，移除 '略' 占位符，添加有意义的检查要求

-- 文档检查
UPDATE quality_check_config SET prompt_template = '请检查 API 文档的完整性，包括：
- 检查是否有缺失的 API 文档注释
- 检查参数说明是否完整
- 检查返回值说明是否清晰
- 检查是否有示例代码' WHERE item_key = 'documentation-api';

UPDATE quality_check_config SET prompt_template = '请检查 README 文档的完整性，包括：
- 项目简介是否清晰
- 安装说明是否完整
- 使用说明是否详细
- 是否有贡献指南' WHERE item_key = 'documentation-readme';

UPDATE quality_check_config SET prompt_template = '请检查变更日志的维护情况，包括：
- 是否有 CHANGELOG 文件
- 版本变更记录是否完整
- 是否记录了重要变更' WHERE item_key = 'documentation-changelog';

-- 依赖管理检查
UPDATE quality_check_config SET prompt_template = '请检查依赖包的安全漏洞，包括：
- 检查是否使用了已知有漏洞的依赖版本
- 检查依赖是否需要更新
- 检查是否有安全相关的依赖配置' WHERE item_key = 'dependency-security';

UPDATE quality_check_config SET prompt_template = '请检查依赖版本是否过时，包括：
- 检查是否有重大版本更新
- 检查是否有次要版本更新
- 建议升级到稳定版本' WHERE item_key = 'dependency-version';

UPDATE quality_check_config SET prompt_template = '请检查依赖许可证的合规性，包括：
- 识别所有依赖的许可证类型
- 检查是否存在许可证冲突
- 检查是否使用了限制性许可证' WHERE item_key = 'dependency-license';

UPDATE quality_check_config SET prompt_template = '请检查未使用的依赖，包括：
- 识别声明但未使用的依赖
- 检查是否有冗余的依赖
- 建议移除未使用的依赖' WHERE item_key = 'dependency-unused';

-- 数据验证检查
UPDATE quality_check_config SET prompt_template = '请检查用户输入的验证，包括：
- 检查是否有未验证的用户输入
- 检查输入验证是否完整
- 检查是否有 SQL 注入、XSS 等风险' WHERE item_key = 'data-validation-input';

UPDATE quality_check_config SET prompt_template = '请检查输出数据的编码，包括：
- 检查是否有未编码的输出
- 检查是否正确处理了特殊字符
- 检查是否有 XSS 风险' WHERE item_key = 'data-validation-output';

UPDATE quality_check_config SET prompt_template = '请检查数据边界验证，包括：
- 检查数组边界检查
- 检查数值范围验证
- 检查字符串长度限制' WHERE item_key = 'data-validation-boundary';

UPDATE quality_check_config SET prompt_template = '请检查数据类型验证，包括：
- 检查类型转换是否安全
- 检查是否有类型混淆风险
- 检查空值处理' WHERE item_key = 'data-validation-type';

-- 错误处理检查
UPDATE quality_check_config SET prompt_template = '请检查错误处理的覆盖程度，包括：
- 检查是否有未捕获的异常
- 检查 catch 块是否为空
- 检查错误是否被正确记录
- 检查是否有适当的错误恢复机制' WHERE item_key = 'error-handling-coverage';

UPDATE quality_check_config SET prompt_template = '请检查错误恢复机制的完整性，包括：
- 检查是否有重试机制
- 检查是否有降级处理
- 检查是否有事务回滚' WHERE item_key = 'error-handling-recovery';

UPDATE quality_check_config SET prompt_template = '请检查错误信息的质量，包括：
- 检查错误信息是否清晰
- 检查错误信息是否包含足够的上下文
- 检查是否泄露了敏感信息' WHERE item_key = 'error-handling-message';

-- 无障碍访问检查
UPDATE quality_check_config SET prompt_template = '请检查 ARIA 属性的正确使用，包括：
- 检查 ARIA 属性是否正确
- 检查是否有缺失的 ARIA 属性
- 检查 ARIA 属性是否与角色匹配' WHERE item_key = 'accessibility-aria';

UPDATE quality_check_config SET prompt_template = '请检查 HTML 语义化程度，包括：
- 检查是否使用了语义化标签
- 检查标题层级是否正确
- 检查是否有正确的 landmark' WHERE item_key = 'accessibility-semantic';

UPDATE quality_check_config SET prompt_template = '请检查键盘导航支持，包括：
- 检查所有交互元素是否可聚焦
- 检查 tabindex 使用是否正确
- 检查键盘快捷键是否合理' WHERE item_key = 'accessibility-keyboard';

UPDATE quality_check_config SET prompt_template = '请检查颜色对比度是否达标，包括：
- 检查文本与背景对比度
- 检查是否仅依赖颜色传达信息
- 检查是否支持高对比度模式' WHERE item_key = 'accessibility-color';

-- 国际化检查
UPDATE quality_check_config SET prompt_template = '请检查硬编码的文本字符串，包括：
- 检查是否有硬编码的用户界面文本
- 检查是否有硬编码的日期/数字格式
- 建议使用 i18n 资源文件' WHERE item_key = 'i18n-hardcoded';

UPDATE quality_check_config SET prompt_template = '请检查日期时间格式化，包括：
- 检查是否使用了 locale-aware 的日期格式
- 检查时区处理是否正确
- 建议使用标准化的日期格式' WHERE item_key = 'i18n-datetime';

UPDATE quality_check_config SET prompt_template = '请检查数字和货币格式化，包括：
- 检查是否使用了 locale-aware 的数字格式
- 检查货币符号处理是否正确
- 检查小数点和千位分隔符' WHERE item_key = 'i18n-number';

UPDATE quality_check_config SET prompt_template = '请检查从右到左语言支持，包括：
- 检查布局是否支持 RTL
- 检查文本方向设置
- 检查镜像布局支持' WHERE item_key = 'i18n-rtl';

-- API 设计检查
UPDATE quality_check_config SET prompt_template = '请检查 RESTful API 设计规范，包括：
- 检查 HTTP 方法使用是否正确
- 检查 URL 命名是否规范
- 检查状态码使用是否正确
- 检查资源命名是否一致' WHERE item_key = 'api-design-restful';

UPDATE quality_check_config SET prompt_template = '请检查 API 版本控制策略，包括：
- 检查是否有版本控制机制
- 检查版本号格式是否规范
- 检查版本兼容性处理' WHERE item_key = 'api-design-versioning';

UPDATE quality_check_config SET prompt_template = '请检查 API 分页设计，包括：
- 检查是否有分页支持
- 检查分页参数是否规范
- 检查分页元数据是否完整' WHERE item_key = 'api-design-pagination';

UPDATE quality_check_config SET prompt_template = '请检查 API 错误响应格式，包括：
- 检查错误响应格式是否统一
- 检查错误码是否规范
- 检查错误信息是否清晰' WHERE item_key = 'api-design-error-response';

-- 状态管理检查
UPDATE quality_check_config SET prompt_template = '请检查不可预期的状态变更，包括：
- 检查是否有直接的状态修改
- 检查状态变更是否可追踪
- 检查是否有状态同步问题' WHERE item_key = 'state-mutation';

UPDATE quality_check_config SET prompt_template = '请检查状态同步问题，包括：
- 检查多组件状态一致性
- 检查异步状态更新
- 检查状态竞态条件' WHERE item_key = 'state-synchronization';

UPDATE quality_check_config SET prompt_template = '请检查状态持久化策略，包括：
- 检查是否有必要的状态持久化
- 检查持久化时机是否合适
- 检查状态恢复机制' WHERE item_key = 'state-persistence';

-- 资源使用检查
UPDATE quality_check_config SET prompt_template = '请检查内存使用效率，包括：
- 检查是否有内存泄漏风险
- 检查大对象处理是否合理
- 检查缓存使用是否合理' WHERE item_key = 'resource-memory';

UPDATE quality_check_config SET prompt_template = '请检查 CPU 使用效率，包括：
- 检查是否有 CPU 密集型操作
- 检查循环和递归是否高效
- 检查是否有不必要的计算' WHERE item_key = 'resource-cpu';

UPDATE quality_check_config SET prompt_template = '请检查网络资源使用，包括：
- 检查是否有不必要的网络请求
- 检查请求是否可以合并
- 检查是否有缓存策略' WHERE item_key = 'resource-network';

UPDATE quality_check_config SET prompt_template = '请检查磁盘 I/O 效率，包括：
- 检查文件读写是否高效
- 检查是否有不必要的磁盘操作
- 检查缓冲区使用是否合理' WHERE item_key = 'resource-disk';

-- 向后兼容性检查
UPDATE quality_check_config SET prompt_template = '请检查 API 变更的向后兼容性，包括：
- 检查是否有破坏性变更
- 检查是否有废弃的 API
- 检查版本迁移路径' WHERE item_key = 'compatibility-api';

UPDATE quality_check_config SET prompt_template = '请检查数据库变更的向后兼容性，包括：
- 检查 schema 变更是否兼容
- 检查数据迁移脚本
- 检查是否有回滚方案' WHERE item_key = 'compatibility-database';

UPDATE quality_check_config SET prompt_template = '请检查配置变更的向后兼容性，包括：
- 检查配置项变更是否兼容
- 检查默认值变更
- 检查配置迁移指南' WHERE item_key = 'compatibility-config';

-- 合规性检查
UPDATE quality_check_config SET prompt_template = '请检查 GDPR 数据保护合规，包括：
- 检查个人数据收集是否合规
- 检查数据存储和处理
- 检查用户同意机制' WHERE item_key = 'compliance-gdpr';

UPDATE quality_check_config SET prompt_template = '请检查 PCI DSS 支付卡行业数据安全标准，包括：
- 检查支付数据处理
- 检查卡号存储是否安全
- 检查日志是否包含敏感信息' WHERE item_key = 'compliance-pci';

UPDATE quality_check_config SET prompt_template = '请检查 HIPAA 医疗数据保护合规，包括：
- 检查医疗数据处理
- 检查访问控制
- 检查审计日志' WHERE item_key = 'compliance-hipaa';

UPDATE quality_check_config SET prompt_template = '请检查审计日志的完整性，包括：
- 检查关键操作是否记录
- 检查日志格式是否规范
- 检查日志存储是否安全' WHERE item_key = 'compliance-logging';

-- 最佳实践检查
UPDATE quality_check_config SET prompt_template = '请检查设计模式的使用，包括：
- 检查是否正确使用了设计模式
- 检查是否有过度设计
- 检查是否有更合适的设计模式' WHERE item_key = 'best-practices-patterns';

UPDATE quality_check_config SET prompt_template = '请检查代码是否符合语言惯用法，包括：
- 检查是否使用了语言特性
- 检查命名规范
- 检查代码风格一致性' WHERE item_key = 'best-practices-idiomatic';

UPDATE quality_check_config SET prompt_template = '请检查常见的代码反模式，包括：
- 检查是否有 God Object
- 检查是否有 Spaghetti Code
- 检查是否有 Copy-Paste 编程' WHERE item_key = 'best-practices-antipatterns';

UPDATE quality_check_config SET prompt_template = '请检查代码重复违反 DRY 原则，包括：
- 检查是否有重复的代码块
- 检查是否有相似的业务逻辑
- 建议提取公共方法或组件' WHERE item_key = 'best-practices-dry';
