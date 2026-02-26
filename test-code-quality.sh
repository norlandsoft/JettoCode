#!/bin/bash

# OpenCode 代码质量检查测试脚本
# 用于验证提示词生成和响应保存功能

BASE_URL="http://localhost:4096"
API_URL="http://localhost:9990/api/code-quality"

echo "=== OpenCode 代码质量检查测试 ==="
echo ""

# 1. 检查 OpenCode 服务状态
echo "1. 检查 OpenCode 服务状态..."
HEALTH=$(curl -s $BASE_URL/global/health)
echo "   状态: $HEALTH"
echo ""

# 2. 创建 OpenCode 会话并测试
echo "2. 创建测试会话..."
SESSION_RESPONSE=$(curl -s -X POST $BASE_URL/session \
  -H "Content-Type: application/json" \
  -d '{"title":"代码质量检查测试-手动"}')
echo "   响应: $SESSION_RESPONSE"
SESSION_ID=$(echo $SESSION_RESPONSE | jq -r '.id')
echo "   会话ID: $SESSION_ID"
echo ""

# 3. 构建测试提示词
echo "3. 构建测试提示词..."
PROMPT="# 代码质量检查任务

## 服务信息
- 服务名称: OpenCodeService
- 代码路径: /opt/Projects/JettoCode/code/src/main/java/com/jettech/code/service/OpenCodeService.java
- 描述: OpenCode HTTP 客户端服务

## 检查项信息
- 检查项: 代码安全性检查
- 维度: security

## 检查要求
请使用 glob 和 read 工具检查以下安全性问题：
1. SQL 注入风险
2. 硬编码密码
3. 不安全的加密算法
4. 命令执行风险

## 输出要求
请以结构化的方式输出检查结果，包括:
1. 发现的问题数量
2. 每个问题的严重级别 (Critical/High/Medium/Low/Info)
3. 问题描述和位置
4. 修复建议

开始执行检查..."

echo "   提示词长度: ${#PROMPT} 字符"
echo ""

# 4. 发送消息（同步）
echo "4. 发送检查请求（同步方式）..."
RESPONSE=$(curl -s -X POST "$BASE_URL/session/$SESSION_ID/message" \
  -H "Content-Type: application/json" \
  -d "{\"parts\":[{\"type\":\"text\",\"text\":\"$PROMPT\"}]}")

echo "   响应长度: ${#RESPONSE} 字符"
echo ""

# 5. 解析响应
echo "5. 解析响应..."
RESPONSE_TEXT=$(echo $RESPONSE | jq -r '.parts[] | select(.type == "text") | .text' | head -c 500)
echo "   响应预览:"
echo "   ${RESPONSE_TEXT:0:300}..."
echo ""

# 6. 提取问题数量
ISSUE_COUNT=$(echo $RESPONSE_TEXT | grep -oiE '[0-9]+个?问题|found [0-9]+ issue|发现 [0-9]+ 个' | head -1 | grep -oE '[0-9]+')
if [ -z "$ISSUE_COUNT" ]; then
  ISSUE_COUNT=0
fi
echo "   检测到的问题数: $ISSUE_COUNT"
echo ""

# 7. 清理会话
echo "7. 清理测试会话..."
DELETE_RESULT=$(curl -s -X DELETE "$BASE_URL/session/$SESSION_ID")
echo "   删除结果: $DELETE_RESULT"
echo ""

# 8. 测试后端 API（如果应用正在运行）
echo "8. 测试后端 API..."
API_TEST=$(curl -s -o /dev/null -w "%{http_code}" $API_URL/scans/1/tasks 2>/dev/null)
if [ "$API_TEST" = "200" ] || [ "$API_TEST" = "404" ]; then
  echo "   后端 API 可访问 (HTTP $API_TEST)"
  
  # 获取任务列表
  echo "   正在获取任务列表..."
  TASKS=$(curl -s $API_URL/scans/1/tasks 2>/dev/null)
  if [ ! -z "$TASKS" ]; then
    echo "   任务数据:"
    echo "$TASKS" | jq -r '.data[] | "   - 任务ID: \(.id), 状态: \(.status), 问题数: \(.issueCount // 0)"' 2>/dev/null || echo "   无法解析任务数据"
  fi
else
  echo "   后端 API 不可访问 (HTTP $API_TEST)"
  echo "   请确保应用正在运行: mvn spring-boot:run"
fi
echo ""

echo "=== 测试完成 ==="
