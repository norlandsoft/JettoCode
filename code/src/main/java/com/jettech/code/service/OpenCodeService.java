package com.jettech.code.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jettech.code.dto.OpenCodeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenCode 服务
 * 负责与 OpenCode API 进行通信
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

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenCodeService(@Qualifier("openCodeRestTemplate") RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 检查 OpenCode 服务是否可用
     */
    public boolean isAvailable() {
        if (!enabled) {
            return false;
        }
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/global/health", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.debug("OpenCode service not available: {}", e.getMessage());
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
        if (!enabled) {
            throw new IllegalStateException("OpenCode service is disabled");
        }

        String url = baseUrl + "/session";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("title", title);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        logger.info("Creating OpenCode session: {}", title);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode idNode = root.path("id");
            if (!idNode.isMissingNode()) {
                String sessionId = idNode.asText();
                logger.info("Created OpenCode session: {}", sessionId);
                return sessionId;
            }
        }

        throw new RuntimeException("Failed to create OpenCode session: " + response.getStatusCode());
    }

    /**
     * 发送提示词并获取响应
     */
    public OpenCodeDTO.ScanResult sendPrompt(String sessionId, String prompt) throws Exception {
        return sendPrompt(sessionId, prompt, defaultModel);
    }

    /**
     * 发送提示词并获取响应（指定模型）
     */
    public OpenCodeDTO.ScanResult sendPrompt(String sessionId, String prompt, String model) throws Exception {
        if (!enabled) {
            throw new IllegalStateException("OpenCode service is disabled");
        }

        String url = baseUrl + "/session/" + sessionId + "/message";

        Map<String, Object> requestBody = new HashMap<>();

        // model 参数应该是对象格式
        if (model != null && !model.isEmpty()) {
            Map<String, String> modelObj = new HashMap<>();
            modelObj.put("id", model);
            requestBody.put("model", modelObj);
        }

        List<Map<String, String>> parts = new ArrayList<>();
        Map<String, String> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", prompt);
        parts.add(textPart);
        requestBody.put("parts", parts);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        logger.info("Sending prompt to session {}: {} chars", sessionId, prompt.length());

        int retries = 0;
        Exception lastException = null;

        while (retries < maxRetries) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return parseScanResult(response.getBody());
                }

                throw new RuntimeException("Unexpected response status: " + response.getStatusCode());
            } catch (Exception e) {
                lastException = e;
                retries++;
                logger.warn("OpenCode prompt failed (attempt {}/{}): {}", retries, maxRetries, e.getMessage());

                if (retries < maxRetries) {
                    Thread.sleep(retryInterval);
                }
            }
        }

        OpenCodeDTO.ScanResult result = new OpenCodeDTO.ScanResult();
        result.setSuccess(false);
        result.setErrorMessage(lastException != null ? lastException.getMessage() : "Unknown error");
        return result;
    }

    /**
     * 发送带文件上下文的提示词
     */
    public OpenCodeDTO.ScanResult sendPromptWithFiles(String sessionId, String prompt, List<String> filePaths) throws Exception {
        if (!enabled) {
            throw new IllegalStateException("OpenCode service is disabled");
        }

        String url = baseUrl + "/session/" + sessionId + "/message";

        Map<String, Object> requestBody = new HashMap<>();
        
        List<Map<String, String>> parts = new ArrayList<>();

        // 添加文本提示
        Map<String, String> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", prompt);
        parts.add(textPart);

        requestBody.put("parts", parts);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        logger.info("Sending prompt with {} files to session {}", filePaths != null ? filePaths.size() : 0, sessionId);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return parseScanResult(response.getBody());
        }

        OpenCodeDTO.ScanResult result = new OpenCodeDTO.ScanResult();
        result.setSuccess(false);
        result.setErrorMessage("Failed to get response: " + response.getStatusCode());
        return result;
    }

    /**
     * 解析扫描结果
     */
    private OpenCodeDTO.ScanResult parseScanResult(String responseBody) throws Exception {
        OpenCodeDTO.ScanResult result = new OpenCodeDTO.ScanResult();
        result.setSuccess(true);

        JsonNode root = objectMapper.readTree(responseBody);

        // 提取响应文本
        StringBuilder fullText = new StringBuilder();
        JsonNode partsNode = root.path("parts");
        if (partsNode.isArray()) {
            for (JsonNode part : partsNode) {
                String type = part.path("type").asText();
                if ("text".equals(type)) {
                    fullText.append(part.path("text").asText());
                }
            }
        }

        String responseText = fullText.toString();
        result.setFullResponse(responseText);

        // 分析结果提取问题数量和严重级别
        int issueCount = extractIssueCount(responseText);
        String severity = extractSeverity(responseText);
        String summary = extractSummary(responseText);

        result.setIssueCount(issueCount);
        result.setSeverity(severity);
        result.setSummary(summary);

        logger.info("Scan result: {} issues, severity: {}", issueCount, severity);

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
     * 获取会话消息历史
     */
    public List<Map<String, Object>> getSessionMessages(String sessionId) throws Exception {
        String url = baseUrl + "/session/" + sessionId + "/message";

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            List<Map<String, Object>> messages = new ArrayList<>();

            if (root.isArray()) {
                for (JsonNode message : root) {
                    Map<String, Object> msg = new HashMap<>();
                    msg.put("id", message.path("info").path("id").asText());
                    msg.put("role", message.path("info").path("role").asText());
                    messages.add(msg);
                }
            }

            return messages;
        }

        return new ArrayList<>();
    }

    /**
     * 删除会话
     */
    public boolean deleteSession(String sessionId) {
        try {
            String url = baseUrl + "/session/" + sessionId;
            restTemplate.delete(url);
            return true;
        } catch (Exception e) {
            logger.warn("Failed to delete session {}: {}", sessionId, e.getMessage());
            return false;
        }
    }
}
