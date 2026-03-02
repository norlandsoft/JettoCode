package com.jettech.code.service;

import com.jettech.code.dto.OpenCodeDTO;
import com.jettech.code.util.OpenCodeClient;
import com.jettech.code.util.OpenCodeClient.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OpenCode 服务
 * 负责与 OpenCode API 进行通信
 * 使用 OpenCodeClient 工具类实现
 */
@Service
public class OpenCodeService {

    private static final Logger logger = LoggerFactory.getLogger(OpenCodeService.class);

    @Value("${opencode.base-url:http://127.0.0.1:4096}")
    private String baseUrl;

    @Value("${opencode.model:claude-sonnet-4-6}")
    private String defaultModel;

    @Value("${opencode.timeout:300000}")
    private int timeout;

    @Value("${opencode.enabled:true}")
    private boolean enabled;

    @Value("${opencode.max-retries:3}")
    private int maxRetries;

    @Value("${opencode.retry-interval:5000}")
    private int retryInterval;

    private OpenCodeClient client;

    @PostConstruct
    public void init() {
        Duration timeoutDuration = Duration.ofMillis(timeout);
        this.client = OpenCodeClient.builder()
                .baseUrl(baseUrl)
                .timeout(timeoutDuration)
                .build();

        logger.info("[OpenCode] 服务初始化完成 - baseUrl: {}, timeout: {}ms, enabled: {}", baseUrl, timeout, enabled);
    }

    /**
     * 检查 OpenCode 服务是否可用
     */
    public boolean isAvailable() {
        logger.debug("[OpenCode] 检查服务可用性...");

        if (!enabled) {
            logger.warn("[OpenCode] 服务已禁用");
            return false;
        }

        try {
            HealthStatus status = client.checkHealth();
            boolean available = status.isHealthy();

            if (available) {
                logger.info("[OpenCode] 服务可用 - version: {}", status.getVersion());
            } else {
                logger.warn("[OpenCode] 服务不可用");
            }

            return available;
        } catch (Exception e) {
            logger.error("[OpenCode] 服务检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 创建 OpenCode 会话
     */
    public String createSession(String title) throws Exception {
        return createSession(title, null);
    }

    /**
     * 创建 OpenCode 会话（带系统提示词）
     */
    public String createSession(String title, String systemPrompt) throws Exception {
        logger.info("[OpenCode] 创建会话 - title: {}", title);

        if (!enabled) {
            throw new IllegalStateException("[OpenCode] 服务已禁用");
        }

        long startTime = System.currentTimeMillis();

        try {
            String sessionId = client.createSession(title, systemPrompt);
            long elapsed = System.currentTimeMillis() - startTime;

            logger.info("[OpenCode] 会话创建成功 - sessionId: {}, 耗时: {}ms", sessionId, elapsed);
            return sessionId;

        } catch (OpenCodeClient.OpenCodeException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            logger.error("[OpenCode] 会话创建失败 - 耗时: {}ms, 错误: {}", elapsed, e.getMessage());
            throw new Exception("创建会话失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送提示词并获取响应
     */
    public OpenCodeDTO.ScanResult sendPrompt(String sessionId, String prompt) throws Exception {
        return sendPrompt(sessionId, prompt, defaultModel);
    }

    /**
     * 发送提示词并获取响应（指定模型）
     * 注意：OpenCode API 要求 model 参数格式为 {"modelID": "xxx", "providerID": "xxx"}
     * 如果不传递 model 参数，API 会使用默认模型
     */
    public OpenCodeDTO.ScanResult sendPrompt(String sessionId, String prompt, String model) throws Exception {
        logger.info("[OpenCode] 发送消息 - sessionId: {}, prompt长度: {} 字符", sessionId, prompt.length());
        logger.debug("[OpenCode] 消息内容预览: {}", truncate(prompt, 200));

        if (!enabled) {
            throw new IllegalStateException("[OpenCode] 服务已禁用");
        }

        long startTime = System.currentTimeMillis();
        int retries = 0;
        Exception lastException = null;

        while (retries < maxRetries) {
            try {
                MessageResult result = client.sendMessage(sessionId, prompt, null);

                long elapsed = System.currentTimeMillis() - startTime;
                logger.info("[OpenCode] 消息响应成功 - 耗时: {}ms, tokens: {}/{}/{}",
                        elapsed, result.getTotalTokens(), result.getInputTokens(), result.getOutputTokens());
                logger.debug("[OpenCode] 响应内容预览: {}", truncate(result.getText(), 500));

                return convertToScanResult(result);

            } catch (OpenCodeClient.OpenCodeException e) {
                lastException = e;
                retries++;

                logger.warn("[OpenCode] 消息发送失败 (尝试 {}/{}) - 错误: {}",
                        retries, maxRetries, e.getMessage());

                if (retries < maxRetries) {
                    logger.info("[OpenCode] 等待 {}ms 后重试...", retryInterval);
                    Thread.sleep(retryInterval);
                }
            }
        }

        // 所有重试都失败
        long elapsed = System.currentTimeMillis() - startTime;
        logger.error("[OpenCode] 消息发送最终失败 - 总耗时: {}ms, 重试次数: {}", elapsed, maxRetries);

        OpenCodeDTO.ScanResult result = new OpenCodeDTO.ScanResult();
        result.setSuccess(false);
        result.setErrorMessage(lastException != null ? lastException.getMessage() : "Unknown error");
        return result;
    }

    /**
     * 发送带文件上下文的提示词
     */
    public OpenCodeDTO.ScanResult sendPromptWithFiles(String sessionId, String prompt, List<String> filePaths) throws Exception {
        int fileCount = filePaths != null ? filePaths.size() : 0;
        logger.info("[OpenCode] 发送带文件的消息 - sessionId: {}, 文件数: {}, prompt长度: {}",
                sessionId, fileCount, prompt.length());

        if (!enabled) {
            throw new IllegalStateException("[OpenCode] 服务已禁用");
        }

        long startTime = System.currentTimeMillis();

        try {
            MessageResult result = client.sendMessageWithFiles(sessionId, prompt, filePaths);

            long elapsed = System.currentTimeMillis() - startTime;
            logger.info("[OpenCode] 带文件消息响应成功 - 耗时: {}ms, tokens: {}",
                    elapsed, result.getTotalTokens());

            return convertToScanResult(result);

        } catch (OpenCodeClient.OpenCodeException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            logger.error("[OpenCode] 带文件消息发送失败 - 耗时: {}ms, 错误: {}", elapsed, e.getMessage());

            OpenCodeDTO.ScanResult scanResult = new OpenCodeDTO.ScanResult();
            scanResult.setSuccess(false);
            scanResult.setErrorMessage("发送失败: " + e.getMessage());
            return scanResult;
        }
    }

    /**
     * 获取会话消息历史
     */
    public List<Map<String, Object>> getSessionMessages(String sessionId) throws Exception {
        logger.debug("[OpenCode] 获取消息历史 - sessionId: {}", sessionId);

        try {
            List<MessageInfo> messages = client.getMessageHistory(sessionId);

            logger.debug("[OpenCode] 消息历史数量: {}", messages.size());

            return messages.stream().map(msg -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", msg.getId());
                map.put("role", msg.getRole());
                return map;
            }).collect(Collectors.toList());

        } catch (OpenCodeClient.OpenCodeException e) {
            logger.error("[OpenCode] 获取消息历史失败: {}", e.getMessage());
            throw new Exception("获取消息历史失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除会话
     */
    public boolean deleteSession(String sessionId) {
        logger.info("[OpenCode] 删除会话 - sessionId: {}", sessionId);

        boolean deleted = client.deleteSession(sessionId);

        if (deleted) {
            logger.info("[OpenCode] 会话删除成功 - sessionId: {}", sessionId);
        } else {
            logger.warn("[OpenCode] 会话删除失败 - sessionId: {}", sessionId);
        }

        return deleted;
    }

    // ==================== 私有方法 ====================

    /**
     * 将 MessageResult 转换为 ScanResult
     */
    private OpenCodeDTO.ScanResult convertToScanResult(MessageResult messageResult) {
        OpenCodeDTO.ScanResult result = new OpenCodeDTO.ScanResult();
        result.setSuccess(true);

        String responseText = messageResult.getText();
        result.setFullResponse(responseText);

        // 分析结果提取问题数量和严重级别
        int issueCount = extractIssueCount(responseText);
        String severity = extractSeverity(responseText);
        String summary = extractSummary(responseText);

        result.setIssueCount(issueCount);
        result.setSeverity(severity);
        result.setSummary(summary);

        logger.debug("[OpenCode] 结果解析 - 问题数: {}, 严重级别: {}", issueCount, severity);

        return result;
    }

    /**
     * 从响应中提取问题数量
     */
    private int extractIssueCount(String response) {
        if (response == null) return 0;

        // 尝试匹配数字
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "(?i)(?:found|detected|identified|发现|检测到|识别到)\\s*(\\d+)\\s*(?:issues?|problems?|问题|缺陷)",
            java.util.regex.Pattern.MULTILINE
        );
        java.util.regex.Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return 0;
    }

    /**
     * 从响应中提取严重级别
     */
    private String extractSeverity(String response) {
        if (response == null) return OpenCodeDTO.ScanResult.SEVERITY_NONE;

        String lower = response.toLowerCase();

        if (lower.contains("critical") || lower.contains("严重") || lower.contains("高危")) {
            return OpenCodeDTO.ScanResult.SEVERITY_CRITICAL;
        }
        if (lower.contains("high") || lower.contains("高")) {
            return OpenCodeDTO.ScanResult.SEVERITY_HIGH;
        }
        if (lower.contains("medium") || lower.contains("中")) {
            return OpenCodeDTO.ScanResult.SEVERITY_MEDIUM;
        }
        if (lower.contains("low") || lower.contains("低")) {
            return OpenCodeDTO.ScanResult.SEVERITY_LOW;
        }

        return OpenCodeDTO.ScanResult.SEVERITY_NONE;
    }

    /**
     * 从响应中提取摘要
     */
    private String extractSummary(String response) {
        if (response == null || response.isEmpty()) {
            return "No response";
        }

        // 取前500个字符作为摘要
        if (response.length() <= 500) {
            return response;
        }

        // 尝试在句号处截断
        int dotIndex = response.lastIndexOf('。', 500);
        int periodIndex = response.lastIndexOf('.', 500);
        int cutPoint = Math.max(dotIndex, periodIndex);

        if (cutPoint > 200) {
            return response.substring(0, cutPoint + 1) + "...";
        }

        return response.substring(0, 500) + "...";
    }

    /**
     * 截断文本用于日志
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "null";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "... (共" + text.length() + "字符)";
    }
}
