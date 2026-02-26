-- JettoCode 质量检查配置种子数据
-- 执行前请确保已运行 schema.sql 创建表结构

-- =====================================================
-- 1. 插入检查分组
-- =====================================================
INSERT INTO quality_check_group (group_key, group_name, description, sort_order, enabled) VALUES
('security', '代码安全检查', '检测代码中的安全漏洞和风险，包括注入、凭证泄露等问题', 1, 1),
('reliability', '可靠性检查', '检测可能导致程序崩溃或异常的问题', 2, 1),
('maintainability', '可维护性与代码异味', '检测影响代码可维护性的问题，包括重复代码、复杂度等', 3, 1),
('readability', '可读性与一致性', '检测代码风格、命名规范和可读性问题', 4, 1),
('performance', '性能与可扩展性', '检测性能瓶颈和可扩展性问题', 5, 1),
('testability', '可测试性', '检测影响代码测试的问题', 6, 1),
('operability', '可操作性', '检测运维相关的问题，包括日志、监控等', 7, 1),
('architecture', '架构与质量检查', '检测架构设计和代码质量问题', 8, 1);

-- =====================================================
-- 2. 插入检查配置 - 代码安全检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'security'),
'security-owasp', 'OWASP Top 10 / CWE Top 25 / OWASP Mobile/LLM',
'检查 OWASP Top 10、CWE Top 25、OWASP Mobile 和 LLM 应用安全问题',
'# OWASP Top 10 / CWE Top 25 / OWASP Mobile/LLM 安全检查

你是一位专业的代码安全审计专家。请根据以下安全标准对代码进行审查：

## 检查范围

### OWASP Top 10 (2021)
1. A01 - 访问控制失效 (Broken Access Control)
2. A02 - 加密失败 (Cryptographic Failures)
3. A03 - 注入 (Injection)
4. A04 - 不安全设计 (Insecure Design)
5. A05 - 安全配置错误 (Security Misconfiguration)
6. A06 - 易受攻击和过时的组件 (Vulnerable and Outdated Components)
7. A07 - 身份识别和身份验证失败 (Identification and Authentication Failures)
8. A08 - 软件和数据完整性失败 (Software and Data Integrity Failures)
9. A09 - 安全日志和监控失败 (Security Logging and Monitoring Failures)
10. A10 - 服务器端请求伪造 (Server-Side Request Forgery)

### CWE Top 25
- CWE-79: 跨站脚本 (XSS)
- CWE-89: SQL注入
- CWE-20: 输入验证不当
- CWE-78: 操作系统命令注入
- CWE-125: 越界读取

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
请以 JSON 格式输出检查结果：
```json
{
  "issues": [
    {
      "rule": "A03:2021-注入",
      "cwe": "CWE-89",
      "severity": "critical|high|medium|low|info",
      "line": 行号,
      "message": "问题描述",
      "suggestion": "修复建议"
    }
  ],
  "summary": "检查总结"
}
```

如果没有发现问题，返回 {"issues": [], "summary": "未发现安全问题"}',
'CRITICAL', 1),

((SELECT id FROM quality_check_group WHERE group_key = 'security'),
'security-injection', '注入（SQL/XSS/命令）、路径遍历',
'检测SQL注入、XSS、命令注入和路径遍历等注入类漏洞',
'# 注入漏洞安全检查

你是一位专业的代码安全审计专家，专注于检测各类注入漏洞。

## 检查类型

### SQL注入
- 动态SQL拼接
- 用户输入直接拼接到SQL
- ORM框架不安全用法

### XSS (跨站脚本)
- 反射型XSS
- 存储型XSS
- DOM型XSS
- innerHTML等危险DOM操作

### 命令注入
- 系统命令执行
- 代码执行
- 表达式语言注入

### 路径遍历
- 文件路径操作
- 目录遍历
- 用户控制的文件路径

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "type": "sql_injection|xss|command_injection|path_traversal",
      "severity": "critical|high|medium|low",
      "line": 行号,
      "vulnerable_code": "有问题的代码片段",
      "message": "问题描述",
      "attack_vector": "攻击向量说明",
      "suggestion": "修复建议，提供安全代码示例"
    }
  ]
}
```',
'CRITICAL', 2),

((SELECT id FROM quality_check_group WHERE group_key = 'security'),
'security-credentials', '硬编码凭证、不安全反序列化、弱加密',
'检测硬编码凭证、不安全的反序列化和弱加密算法',
'# 凭证与加密安全检查

你是一位专业的代码安全审计专家，专注于检测凭证泄露和加密问题。

## 检查类型

### 硬编码凭证
- 硬编码密码
- 硬编码API密钥
- 硬编码私钥
- 硬编码Token

### 不安全反序列化
- Java原生反序列化
- Python pickle反序列化
- YAML不安全加载
- JSONP远程调用

### 弱加密
- 弱哈希算法(MD5, SHA1用于安全场景)
- 弱加密算法(DES, RC4)
- 弱随机数生成器
- 硬编码盐值/IV
- 不安全的TLS版本

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "type": "hardcoded_credential|insecure_deserialization|weak_crypto",
      "severity": "critical|high|medium|low",
      "line": 行号,
      "vulnerable_code": "有问题的代码片段",
      "message": "问题描述",
      "suggestion": "修复建议"
    }
  ]
}
```',
'HIGH', 3),

((SELECT id FROM quality_check_group WHERE group_key = 'security'),
'security-secrets', '秘密凭证检测（450+ 模式，熵 + 语义 + 正则）',
'使用多种方法检测代码中的秘密凭证泄露',
'# 秘密凭证检测

你是一位专业的安全审计专家，专注于检测代码中的秘密凭证泄露。

## 检测方法

### 熵值检测
- 高熵字符串检测(阈值: 4.5)
- 随机性分析

### 语义分析
- 变量名分析(password, secret, key, token等)
- 上下文分析
- 赋值语句分析

### 正则模式匹配
支持450+种凭证模式，包括：
- AWS凭证 (AKIA..., ASIA...)
- Azure凭证
- GCP凭证
- GitHub Token (ghp_, gho_, ghu_, ghs_, ghr_)
- GitLab Token
- Slack Token (xoxb-, xoxa-, xoxp-)
- JWT Secret
- 数据库连接字符串
- 私钥文件 (-----BEGIN ... PRIVATE KEY-----)

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "secrets_found": [
    {
      "type": "aws_key|github_token|jwt_secret|db_connection|private_key|...",
      "line": 行号,
      "matched_pattern": "匹配的模式或正则",
      "entropy": 熵值,
      "confidence": "high|medium|low",
      "preview": "凭证预览(部分隐藏)",
      "suggestion": "修复建议"
    }
  ]
}
```',
'CRITICAL', 4),

((SELECT id FROM quality_check_group WHERE group_key = 'security'),
'security-hotspots', '安全热点（潜在风险需人工复核）',
'检测需要人工复核的潜在安全风险',
'# 安全热点检测

你是一位专业的代码安全审计专家，专注于识别需要人工复核的潜在安全风险。

## 热点类别

### 敏感数据暴露
- 个人信息处理
- 敏感数据传输
- 数据存储安全

### 不安全配置
- 调试模式开启
- 默认凭据使用
- 不安全的跨域配置

### 权限问题
- 权限过大
- 缺少访问控制
- 不安全的默认值

### 其他热点
- 调试信息泄露
- 不安全的重定向
- 文件上传风险
- SSRF风险点

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "hotspots": [
    {
      "category": "sensitive_data|insecure_config|permission|other",
      "severity": "high|medium|low",
      "line": 行号,
      "code_snippet": "相关代码片段",
      "risk_description": "风险描述",
      "review_question": "需要人工确认的问题",
      "recommendation": "建议操作"
    }
  ],
  "requires_review": true/false
}
```',
'MEDIUM', 5);

-- =====================================================
-- 3. 插入检查配置 - 可靠性检查
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'reliability'),
'reliability-null', '空指针与资源泄漏',
'检测空指针引用和资源泄漏问题',
'# 空指针与资源泄漏检查

你是一位专业的代码质量分析师，专注于检测可靠性问题。

## 检查类型

### 空指针问题
- 空指针解引用
- 空检查后解引用
- 可能为空的变量使用

### 资源泄漏
- 未关闭的文件流
- 未关闭的数据库连接
- 未关闭的网络连接
- 未释放的内存

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "type": "null_pointer|resource_leak",
      "severity": "critical|high|medium|low",
      "line": 行号,
      "variable": "相关变量名",
      "message": "问题描述",
      "suggestion": "修复建议"
    }
  ]
}
```',
'HIGH', 1),

((SELECT id FROM quality_check_group WHERE group_key = 'reliability'),
'reliability-exception', '异常处理与错误处理',
'检测异常处理和错误处理中的问题',
'# 异常与错误处理检查

你是一位专业的代码质量分析师，专注于检测异常处理问题。

## 检查类型

### 异常处理
- 空catch块
- 过于宽泛的异常捕获
- 异常信息丢失

### 错误处理
- 被吞掉的异常
- 未记录的错误
- 错误处理不当

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "type": "empty_catch|broad_catch|swallowed_exception|unlogged_error",
      "severity": "high|medium|low",
      "line": 行号,
      "message": "问题描述",
      "suggestion": "修复建议"
    }
  ]
}
```',
'HIGH', 2),

((SELECT id FROM quality_check_group WHERE group_key = 'reliability'),
'reliability-concurrency', '并发与线程安全',
'检测并发和线程安全问题',
'# 并发与线程安全检查

你是一位专业的代码质量分析师，专注于检测并发问题。

## 检查类型

### 并发问题
- 竞态条件
- 死锁风险
- 数据竞争
- 不安全的发布

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "type": "race_condition|deadlock|data_race|unsafe_publication",
      "severity": "critical|high|medium",
      "line": 行号,
      "message": "问题描述",
      "suggestion": "修复建议"
    }
  ]
}
```',
'HIGH', 3);

-- =====================================================
-- 4. 插入检查配置 - 可维护性
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'maintainability'),
'maintainability-duplication', '代码重复检测',
'检测重复代码和复制粘贴问题',
'# 代码重复检测

你是一位专业的代码质量分析师，专注于检测重复代码。

## 检查类型
- 完全相同的代码块
- 相似的代码块(相似度>80%)
- 复制粘贴的代码

## 阈值
- 最小重复行数: 5行
- 相似度阈值: 80%

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "duplications": [
    {
      "type": "exact|similar",
      "start_line": 起始行,
      "end_line": 结束行,
      "line_count": 行数,
      "similar_to": "相似代码位置(如果存在)",
      "suggestion": "重构建议"
    }
  ]
}
```',
'MAJOR', 1),

((SELECT id FROM quality_check_group WHERE group_key = 'maintainability'),
'maintainability-complexity', '复杂度与代码异味',
'检测代码复杂度和代码异味问题',
'# 复杂度与代码异味检查

你是一位专业的代码质量分析师，专注于检测复杂度和代码异味。

## 检查类型

### 复杂度
- 圈复杂度(阈值: 15)
- 认知复杂度(阈值: 20)

### 代码异味
- 过长的方法
- 过大的类
- 过深的嵌套
- 过多的参数
- 依恋情结
- 发散式变化

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "type": "high_complexity|long_method|large_class|deep_nesting|...",
      "severity": "major|minor",
      "line": 行号,
      "metric_value": 指标值,
      "threshold": 阈值,
      "message": "问题描述",
      "suggestion": "重构建议"
    }
  ]
}
```',
'MAJOR', 2),

((SELECT id FROM quality_check_group WHERE group_key = 'maintainability'),
'maintainability-size', '文件与方法大小',
'检测过大的文件和方法',
'# 文件与方法大小检查

你是一位专业的代码质量分析师，专注于检测代码大小问题。

## 阈值
- 最大文件行数: 500
- 最大方法行数: 50
- 最大参数数量: 7

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "type": "large_file|long_method|too_many_params",
      "line": 行号,
      "current_size": 当前大小,
      "threshold": 阈值,
      "message": "问题描述",
      "suggestion": "拆分建议"
    }
  ]
}
```',
'MINOR', 3);

-- =====================================================
-- 5. 插入检查配置 - 可读性
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'readability'),
'readability-naming', '命名规范检查',
'检查命名规范和命名质量问题',
'# 命名规范检查

你是一位专业的代码质量分析师，专注于命名规范。

## 检查类型
- 类名规范(PascalCase)
- 方法名规范(camelCase)
- 变量名规范(camelCase)
- 常量名规范(UPPER_SNAKE_CASE)
- 有意义的命名

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "type": "class_naming|method_naming|variable_naming|meaningless_name",
      "line": 行号,
      "name": "有问题的名称",
      "expected_pattern": "期望的命名模式",
      "message": "问题描述",
      "suggestion": "建议的名称"
    }
  ]
}
```',
'MINOR', 1),

((SELECT id FROM quality_check_group WHERE group_key = 'readability'),
'readability-comment', '注释质量检查',
'检查代码注释的质量和完整性',
'# 注释质量检查

你是一位专业的代码质量分析师，专注于注释质量。

## 检查类型
- 注释覆盖率(最低: 10%)
- TODO/FIXME标记
- 过时的注释
- 无用的注释
- 缺少文档的公共API

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "type": "missing_comment|outdated_comment|todo_fixme|useless_comment",
      "line": 行号,
      "message": "问题描述",
      "suggestion": "改进建议"
    }
  ],
  "metrics": {
    "total_lines": 总行数,
    "comment_lines": 注释行数,
    "comment_ratio": 注释比例
  }
}
```',
'INFO', 2),

((SELECT id FROM quality_check_group WHERE group_key = 'readability'),
'readability-format', '格式化与风格一致性',
'检查代码格式化和风格一致性问题',
'# 格式化与风格一致性检查

你是一位专业的代码质量分析师，专注于代码格式化。

## 检查类型
- 行长度(最大: 120)
- 缩进一致性
- 空白符使用
- 括号风格
- 导入排序

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "type": "line_too_long|inconsistent_indent|trailing_whitespace|...",
      "line": 行号,
      "message": "问题描述",
      "suggestion": "修复建议"
    }
  ]
}
```',
'INFO', 3);

-- =====================================================
-- 6. 插入检查配置 - 性能
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'performance'),
'performance-query', 'N+1 查询与内存泄漏',
'检测N+1查询和内存泄漏问题',
'# N+1查询与内存泄漏检查

你是一位专业的性能优化专家。

## 检查类型

### N+1查询
- 循环中的数据库查询
- 延迟加载问题
- 缺少预加载

### 内存泄漏
- 大对象分配
- 未释放的引用
- 缓存无限制增长

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "type": "n_plus_one|memory_leak|large_allocation",
      "severity": "high|medium",
      "line": 行号,
      "message": "问题描述",
      "impact": "性能影响说明",
      "suggestion": "优化建议"
    }
  ]
}
```',
'HIGH', 1),

((SELECT id FROM quality_check_group WHERE group_key = 'performance'),
'performance-loop', '低效循环与阻塞 IO',
'检测低效循环和阻塞IO问题',
'# 低效循环与阻塞IO检查

你是一位专业的性能优化专家。

## 检查类型

### 低效循环
- 嵌套循环(O(n²)或更高)
- 循环中的重复计算
- 循环中的IO操作

### 阻塞IO
- 同步IO在异步上下文中
- 阻塞的网络调用
- 阻塞的文件操作

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "type": "nested_loop|redundant_computation|blocking_io",
      "severity": "high|medium|low",
      "line": 行号,
      "complexity": "时间复杂度",
      "message": "问题描述",
      "suggestion": "优化建议"
    }
  ]
}
```',
'MEDIUM', 2),

((SELECT id FROM quality_check_group WHERE group_key = 'performance'),
'performance-cache', '缓存策略检查',
'检测缓存使用和策略问题',
'# 缓存策略检查

你是一位专业的性能优化专家。

## 检查类型
- 缓存失效问题
- 缓存键冲突
- 缓存穿透风险
- 缓存雪崩风险
- 缺少缓存的热点数据

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "type": "cache_invalidation|key_conflict|cache_miss|...",
      "severity": "medium|low",
      "line": 行号,
      "message": "问题描述",
      "suggestion": "优化建议"
    }
  ]
}
```',
'MINOR', 3);

-- =====================================================
-- 7. 插入检查配置 - 可测试性
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'testability'),
'testability-coverage', '单元测试覆盖率',
'检测单元测试覆盖率问题',
'# 单元测试覆盖率检查

你是一位专业的测试专家。

## 检查类型
- 测试覆盖率(最低: 80%)
- 缺少测试的公共方法
- 边界条件覆盖
- 异常路径覆盖

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "type": "low_coverage|missing_test|uncovered_branch",
      "line": 行号,
      "message": "问题描述",
      "suggestion": "测试建议"
    }
  ]
}
```',
'MINOR', 1),

((SELECT id FROM quality_check_group WHERE group_key = 'testability'),
'testability-mock', '可模拟性与依赖注入',
'检测代码的可测试性和依赖注入问题',
'# 可模拟性与依赖注入检查

你是一位专业的测试专家。

## 检查类型
- 硬编码依赖
- 静态方法调用
- 单例模式滥用
- 构造函数注入
- 接口抽象

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "type": "hardcoded_dependency|static_call|singleton_abuse",
      "line": 行号,
      "message": "问题描述",
      "suggestion": "重构建议"
    }
  ]
}
```',
'MINOR', 2),

((SELECT id FROM quality_check_group WHERE group_key = 'testability'),
'testability-isolation', '测试隔离性',
'检测测试隔离性问题',
'# 测试隔离性检查

你是一位专业的测试专家。

## 检查类型
- 共享状态
- 全局可变状态
- 测试间依赖
- 外部依赖

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "type": "shared_state|global_mutable|test_dependency",
      "line": 行号,
      "message": "问题描述",
      "suggestion": "隔离建议"
    }
  ]
}
```',
'MINOR', 3);

-- =====================================================
-- 8. 插入检查配置 - 可操作性
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'operability'),
'operability-logging', '日志与监控',
'检测日志和监控相关问题',
'# 日志与监控检查

你是一位专业的运维专家。

## 检查类型
- 敏感数据日志
- 日志级别不当
- 缺少关键日志
- 指标导出问题

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "type": "sensitive_logging|wrong_level|missing_log|metrics_issue",
      "line": 行号,
      "message": "问题描述",
      "suggestion": "改进建议"
    }
  ]
}
```',
'MINOR', 1),

((SELECT id FROM quality_check_group WHERE group_key = 'operability'),
'operability-health', '健康检查与优雅关闭',
'检测健康检查和优雅关闭问题',
'# 健康检查与优雅关闭检查

你是一位专业的运维专家。

## 检查类型
- 健康检查端点
- 就绪检查
- 优雅关闭钩子
- 资源清理

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "type": "missing_health_check|missing_shutdown_hook|resource_leak",
      "line": 行号,
      "message": "问题描述",
      "suggestion": "改进建议"
    }
  ]
}
```',
'MINOR', 2),

((SELECT id FROM quality_check_group WHERE group_key = 'operability'),
'operability-config', '配置管理检查',
'检测配置管理相关问题',
'# 配置管理检查

你是一位专业的运维专家。

## 检查类型
- 硬编码配置
- 环境变量使用
- 配置加密
- 敏感配置管理

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "type": "hardcoded_config|missing_env|unencrypted_secret",
      "line": 行号,
      "message": "问题描述",
      "suggestion": "改进建议"
    }
  ]
}
```',
'MINOR', 3);

-- =====================================================
-- 9. 插入检查配置 - 架构
-- =====================================================
INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order) VALUES
((SELECT id FROM quality_check_group WHERE group_key = 'architecture'),
'architecture-layer', '分层违规与循环依赖',
'检测分层违规和循环依赖问题',
'# 分层违规与循环依赖检查

你是一位专业的软件架构师。

## 检查类型

### 分层违规
- 表现层直接访问数据层
- 业务逻辑泄漏到表现层
- 跨层调用

### 循环依赖
- 类之间的循环依赖
- 模块之间的循环依赖
- 包之间的循环依赖

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "type": "layer_violation|circular_dependency",
      "severity": "major|minor",
      "line": 行号,
      "layers": "涉及的层",
      "message": "问题描述",
      "suggestion": "重构建议"
    }
  ]
}
```',
'MAJOR', 1),

((SELECT id FROM quality_check_group WHERE group_key = 'architecture'),
'architecture-principle', '设计原则检查（SOLID）',
'检查SOLID设计原则遵循情况',
'# SOLID设计原则检查

你是一位专业的软件架构师。

## 检查类型

### S - 单一职责原则 (Single Responsibility)
- 类是否只有一个变更原因
- 方法是否只做一件事

### O - 开闭原则 (Open/Closed)
- 是否对扩展开放
- 是否对修改关闭

### L - 里氏替换原则 (Liskov Substitution)
- 子类是否能替换父类
- 契约是否被遵守

### I - 接口隔离原则 (Interface Segregation)
- 接口是否足够小
- 客户端是否依赖不需要的方法

### D - 依赖倒置原则 (Dependency Inversion)
- 是否依赖抽象而非具体
- 高层模块是否独立于低层模块

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "principle": "S|O|L|I|D",
      "severity": "major|minor",
      "line": 行号,
      "message": "问题描述",
      "suggestion": "重构建议"
    }
  ]
}
```',
'MAJOR', 2),

((SELECT id FROM quality_check_group WHERE group_key = 'architecture'),
'architecture-api', 'API 设计规范',
'检查API设计规范遵循情况',
'# API设计规范检查

你是一位专业的API设计师。

## 检查类型

### RESTful规范
- HTTP方法使用
- 资源命名
- 状态码使用
- 版本控制

### API一致性
- 响应格式一致性
- 错误处理一致性
- 分页规范
- 命名规范

## 待检查代码
文件: {{file_path}}
语言: {{language}}

```{{language}}
{{code}}
```

## 输出要求
```json
{
  "issues": [
    {
      "type": "http_method|naming|status_code|versioning|consistency",
      "severity": "minor|info",
      "line": 行号,
      "endpoint": "相关端点",
      "message": "问题描述",
      "suggestion": "改进建议"
    }
  ]
}
```',
'INFO', 3);
