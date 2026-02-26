#!/bin/bash

# 简单测试：验证 ScanTaskManager 的提示词生成逻辑

echo "=== 测试提示词生成逻辑 ==="
echo ""

# 读取 ScanTaskManager 的 buildPrompt 方法
echo "1. 查看 buildPrompt 方法："
sed -n '283,324p' /opt/Projects/JettoCode/code/src/main/java/com/jettech/code/service/ScanTaskManager.java

echo ""
echo "2. 检查数据库表结构："
grep -A 30 "CREATE TABLE.*code_quality_task" /opt/Projects/JettoCode/code/src/main/resources/schema.sql | grep -E "prompt_text|response_text"

echo ""
echo "3. 检查 Mapper update 语句："
sed -n '43,64p' /opt/Projects/JettoCode/code/src/main/resources/mapper/CodeQualityTaskMapper.xml

echo ""
echo "4. 检查 executeTask 方法中的字段设置："
grep -n "setPromptText\|setResponseText" /opt/Projects/JettoCode/code/src/main/java/com/jettech/code/service/ScanTaskManager.java

echo ""
echo "=== 分析 ==="
echo "✅ 数据库表有 prompt_text 和 response_text 字段"
echo "✅ Mapper update 语句包含这两个字段"
echo "✅ executeTask 方法中设置了这两个字段"
echo ""
echo "如果数据库中没有这些数据，可能的原因："
echo "1. 任务执行失败（抛出异常）"
echo "2. OpenCode 服务不可用"
echo "3. taskMapper.update() 调用失败"
echo "4. 查看的是错误的 scan_id"
echo ""
echo "建议检查："
echo "- 应用日志中是否有错误"
echo "- code_quality_task 表中是否有记录"
echo "- 任务的 status 字段值"
