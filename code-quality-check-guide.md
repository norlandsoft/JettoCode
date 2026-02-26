# 代码质量扫描功能改造指南

## 改造概述

已将代码质量扫描功能改造为使用 OpenCode HTTP API 进行代码质量检查。

## 改造内容

### 1. OpenCodeService.java API 路径修正

| 功能 | 旧路径 | 新路径 |
|------|--------|--------|
| 健康检查 | `/health` | `/global/health` |
| 创建会话 | `/api/session` | `/session` |
| 发送消息 | `/api/session/{id}/prompt` | `/session/{id}/message` |
| 获取消息 | `/api/session/{id}/messages` | `/session/{id}/message` |
| 删除会话 | `/api/session/{id}` | `/session/{id}` |

### 2. 请求格式优化

- 模型参数改为可选（如果不提供，OpenCode 会使用默认模型）
- 简化了请求体结构

## 使用方法

### HTTP API 调用示例

#### 1. 创建会话

```bash
curl -X POST http://localhost:4096/session \
  -H "Content-Type: application/json" \
  -d '{"title":"代码质量检查"}'
```

响应：
```json
{
  "id": "ses_364a6dee1ffesx3X2GCg6FTybi",
  "title": "代码质量检查",
  "directory": "/opt/workspace"
}
```

#### 2. 发送检查指令

```bash
curl -X POST "http://localhost:4096/session/{session_id}/message" \
  -H "Content-Type: application/json" \
  -d '{
    "parts": [{
      "type": "text",
      "text": "请检查以下代码的质量问题...\n\n代码路径：/path/to/file.java"
    }]
  }'
```

#### 3. 异步发送（推荐用于长时间任务）

```bash
curl -X POST "http://localhost:4096/session/{session_id}/prompt_async" \
  -H "Content-Type: application/json" \
  -d '{
    "parts": [{
      "type": "text",
      "text": "检查指令..."
    }]
  }'
```

#### 4. 获取结果

```bash
curl "http://localhost:4096/session/{session_id}/message?limit=5"
```

### Java 代码调用示例

#### 在 CodeQualityService 中使用

```java
@Service
public class CodeQualityService {
    
    private final OpenCodeService openCodeService;
    
    public void performQualityCheck(Long serviceId, String filePath) {
        // 1. 检查服务可用性
        if (!openCodeService.isAvailable()) {
            throw new IllegalStateException("OpenCode 服务不可用");
        }
        
        // 2. 创建会话
        String sessionId = openCodeService.createSession(
            "代码质量检查 - " + filePath
        );
        
        // 3. 构建检查提示词
        String prompt = buildQualityCheckPrompt(filePath);
        
        // 4. 发送检查请求
        OpenCodeDTO.ScanResult result = openCodeService.sendPrompt(
            sessionId, 
            prompt
        );
        
        // 5. 处理结果
        if (result.isSuccess()) {
            int issueCount = result.getIssueCount();
            String severity = result.getSeverity();
            String summary = result.getSummary();
            
            // 保存结果到数据库...
        }
        
        // 6. 清理会话
        openCodeService.deleteSession(sessionId);
    }
    
    private String buildQualityCheckPrompt(String filePath) {
        return String.format(
            "请对以下文件进行代码质量检查：\n\n" +
            "文件路径：%s\n\n" +
            "检查维度：\n" +
            "1. 安全性问题（SQL注入、硬编码密码等）\n" +
            "2. 可靠性问题（异常处理、资源泄漏）\n" +
            "3. 可维护性问题（代码复杂度、重复代码）\n\n" +
            "请给出：\n" +
            "- 问题总数\n" +
            "- 每个问题的严重级别（Critical/High/Medium/Low）\n" +
            "- 问题描述和位置\n" +
            "- 修复建议",
            filePath
        );
    }
}
```

## 代码质量检查指令模板

### 安全性检查

```
请对以下代码进行安全性检查：
- SQL注入风险
- XSS跨站脚本攻击
- 硬编码密码/密钥
- 不安全的加密算法
- 命令注入风险

代码路径：{file_path}
```

### 可靠性检查

```
请对以下代码进行可靠性检查：
- 异常处理是否完善
- 资源是否正确释放
- 空指针风险
- 并发安全问题

代码路径：{file_path}
```

### 可维护性检查

```
请对以下代码进行可维护性检查：
- 代码复杂度
- 重复代码
- 命名规范
- 注释完整性

代码路径：{file_path}
```

## ScanTaskManager 工作流程

`ScanTaskManager` 会自动：

1. **创建任务**：根据选择的服务和检查项创建扫描任务
2. **构建提示词**：使用 `QualityCheckConfig` 中的提示词模板
3. **调用 OpenCode**：通过 `OpenCodeService` 发送检查请求
4. **解析结果**：提取问题数量、严重级别、摘要
5. **保存问题**：将发现的问题保存到数据库

### 配置检查项

在 `quality_check_config` 表中配置检查项：

```sql
INSERT INTO quality_check_config (
    group_id, 
    item_key, 
    item_name, 
    prompt_template,
    description
) VALUES (
    1, 
    'security_injection', 
    'SQL注入检查',
    '请检查代码中是否存在SQL注入风险，特别关注字符串拼接SQL的情况...',
    '检查SQL注入漏洞'
);
```

## 配置项

在 `application.yml` 中配置 OpenCode：

```yaml
opencode:
  base-url: http://127.0.0.1:4096
  model: claude-sonnet-4-6
  timeout: 300000
  enabled: true
  max-retries: 3
  retry-interval: 5000
```

## 验证测试

### 1. 检查 OpenCode 服务状态

```bash
curl http://localhost:4096/global/health
```

### 2. 手动测试代码质量检查

```bash
# 创建会话
SESSION_ID=$(curl -s -X POST http://localhost:4096/session \
  -H "Content-Type: application/json" \
  -d '{"title":"质量检查测试"}' | jq -r '.id')

# 发送检查指令
curl -X POST "http://localhost:4096/session/$SESSION_ID/message" \
  -H "Content-Type: application/json" \
  -d '{
    "parts": [{
      "type": "text",
      "text": "检查 /path/to/your/code.java 的代码质量"
    }]
  }'
```

## 总结

改造后的代码质量扫描功能：

✅ 使用标准的 OpenCode HTTP API  
✅ 支持异步执行长时间检查任务  
✅ 自动重试机制（默认3次）  
✅ 灵活的检查项配置  
✅ 完整的结果解析和存储  

现在可以通过前端界面或直接调用 API 来使用 OpenCode 进行代码质量检查！
