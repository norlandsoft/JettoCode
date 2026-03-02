package com.jettech.code.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenCode 客户端工具类
 * 提供基础的对话和 session 状态检查方法
 *
 * 使用示例:
 * <pre>
 * // 创建客户端
 * OpenCodeClient client = new OpenCodeClient();
 *
 * // 检查服务状态
 * boolean available = client.isAvailable();
 *
 * // 创建会话并发送消息
 * String sessionId = client.createSession("My Session");
 * OpenCodeClient.MessageResult result = client.sendMessage(sessionId, "Hello");
 * System.out.println(result.getText());
 *
 * // 关闭会话
 * client.deleteSession(sessionId);
 * </pre>
 */
public class OpenCodeClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenCodeClient.class);

    // 默认配置
    private static final String DEFAULT_BASE_URL = "http://127.0.0.1:4096";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);

    // 实例配置
    private final String baseUrl;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * 使用默认配置创建客户端
     */
    public OpenCodeClient() {
        this(DEFAULT_BASE_URL, DEFAULT_TIMEOUT);
    }

    /**
     * 使用自定义配置创建客户端
     * @param baseUrl OpenCode 服务地址
     * @param timeout 请求超时时间
     */
    public OpenCodeClient(String baseUrl, Duration timeout) {
        this.baseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
        this.timeout = timeout != null ? timeout : DEFAULT_TIMEOUT;
        this.objectMapper = new ObjectMapper();

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        logger.info("OpenCodeClient initialized: baseUrl={}, timeout={}s", this.baseUrl, this.timeout.getSeconds());
    }

    /**
     * 使用自定义超时创建客户端（使用默认地址）
     * @param timeout 请求超时时间
     */
    public OpenCodeClient(Duration timeout) {
        this(DEFAULT_BASE_URL, timeout);
    }

    // ==================== 健康检查 ====================

    /**
     * 检查 OpenCode 服务是否可用
     * @return true 如果服务可用
     */
    public boolean isAvailable() {
        try {
            HealthStatus status = checkHealth();
            return status != null && status.isHealthy();
        } catch (Exception e) {
            logger.debug("OpenCode service not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取服务健康状态
     * @return 健康状态信息
     */
    public HealthStatus checkHealth() {
        try {
            String response = get("/global/health");
            JsonNode root = objectMapper.readTree(response);
            return new HealthStatus(
                    root.path("healthy").asBoolean(false),
                    root.path("version").asText(null)
            );
        } catch (Exception e) {
            logger.debug("Health check failed: {}", e.getMessage());
            return new HealthStatus(false, null);
        }
    }

    // ==================== 会话管理 ====================

    /**
     * 创建新的 OpenCode 会话
     * @param title 会话标题
     * @return 会话 ID
     * @throws OpenCodeException 如果创建失败
     */
    public String createSession(String title) throws OpenCodeException {
        return createSession(title, null);
    }

    /**
     * 创建新的 OpenCode 会话（带系统提示词）
     * @param title 会话标题
     * @param systemPrompt 系统提示词（可选）
     * @return 会话 ID
     * @throws OpenCodeException 如果创建失败
     */
    public String createSession(String title, String systemPrompt) throws OpenCodeException {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("title", title);
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                body.put("systemPrompt", systemPrompt);
            }

            String response = post("/session", body);
            JsonNode root = objectMapper.readTree(response);

            String sessionId = root.path("id").asText(null);
            if (sessionId == null || sessionId.isEmpty()) {
                throw new OpenCodeException("Failed to get session ID from response");
            }

            logger.info("Created OpenCode session: {} ({})", sessionId, title);
            return sessionId;

        } catch (OpenCodeException e) {
            throw e;
        } catch (Exception e) {
            throw new OpenCodeException("Failed to create session: " + e.getMessage(), e);
        }
    }

    /**
     * 获取会话状态
     * @param sessionId 会话 ID
     * @return 会话状态信息
     * @throws OpenCodeException 如果获取失败
     */
    public SessionStatus getSessionStatus(String sessionId) throws OpenCodeException {
        try {
            String response = get("/session/" + sessionId);
            JsonNode root = objectMapper.readTree(response);

            return new SessionStatus(
                    root.path("id").asText(),
                    root.path("slug").asText(),
                    root.path("title").asText(),
                    root.path("version").asText(),
                    root.path("projectID").asText(),
                    root.path("directory").asText()
            );
        } catch (Exception e) {
            throw new OpenCodeException("Failed to get session status: " + e.getMessage(), e);
        }
    }

    /**
     * 删除会话
     * @param sessionId 会话 ID
     * @return true 如果删除成功
     */
    public boolean deleteSession(String sessionId) {
        try {
            String response = delete("/session/" + sessionId);
            boolean success = "true".equalsIgnoreCase(response) || response.isEmpty();
            if (success) {
                logger.info("Deleted OpenCode session: {}", sessionId);
            }
            return success;
        } catch (Exception e) {
            logger.warn("Failed to delete session {}: {}", sessionId, e.getMessage());
            return false;
        }
    }

    /**
     * 检查会话是否存在
     * @param sessionId 会话 ID
     * @return true 如果会话存在
     */
    public boolean sessionExists(String sessionId) {
        try {
            getSessionStatus(sessionId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 消息发送 ====================

    /**
     * 发送消息并获取响应
     * @param sessionId 会话 ID
     * @param message 消息内容
     * @return 消息结果
     * @throws OpenCodeException 如果发送失败
     */
    public MessageResult sendMessage(String sessionId, String message) throws OpenCodeException {
        return sendMessage(sessionId, message, null);
    }

    /**
     * 发送消息并获取响应（带模型指定）
     * @param sessionId 会话 ID
     * @param message 消息内容
     * @param modelId 模型 ID（可选，格式：providerID/modelID）
     * @return 消息结果
     * @throws OpenCodeException 如果发送失败
     */
    public MessageResult sendMessage(String sessionId, String message, String modelId) throws OpenCodeException {
        try {
            Map<String, Object> body = new HashMap<>();

            // 添加模型配置（如果指定）
            if (modelId != null && !modelId.isEmpty()) {
                String[] parts = modelId.split("/");
                if (parts.length == 2) {
                    Map<String, String> model = new HashMap<>();
                    model.put("providerID", parts[0]);
                    model.put("modelID", parts[1]);
                    body.put("model", model);
                }
            }

            // 构建消息部分
            List<Map<String, String>> partsList = new ArrayList<>();
            Map<String, String> textPart = new HashMap<>();
            textPart.put("type", "text");
            textPart.put("text", message);
            partsList.add(textPart);
            body.put("parts", partsList);

            String response = post("/session/" + sessionId + "/message", body);
            return parseMessageResult(response);

        } catch (OpenCodeException e) {
            throw e;
        } catch (Exception e) {
            throw new OpenCodeException("Failed to send message: " + e.getMessage(), e);
        }
    }

    /**
     * 发送带文件的消息
     * @param sessionId 会话 ID
     * @param message 消息内容
     * @param filePaths 文件路径列表
     * @return 消息结果
     * @throws OpenCodeException 如果发送失败
     */
    public MessageResult sendMessageWithFiles(String sessionId, String message, List<String> filePaths)
            throws OpenCodeException {
        try {
            Map<String, Object> body = new HashMap<>();

            List<Map<String, String>> partsList = new ArrayList<>();

            // 添加文本部分
            Map<String, String> textPart = new HashMap<>();
            textPart.put("type", "text");
            textPart.put("text", message);
            partsList.add(textPart);

            // 添加文件部分
            if (filePaths != null) {
                for (String filePath : filePaths) {
                    Map<String, String> filePart = new HashMap<>();
                    filePart.put("type", "file");
                    filePart.put("path", filePath);
                    partsList.add(filePart);
                }
            }

            body.put("parts", partsList);

            String response = post("/session/" + sessionId + "/message", body);
            return parseMessageResult(response);

        } catch (OpenCodeException e) {
            throw e;
        } catch (Exception e) {
            throw new OpenCodeException("Failed to send message with files: " + e.getMessage(), e);
        }
    }

    /**
     * 获取会话的消息历史
     * @param sessionId 会话 ID
     * @return 消息列表
     * @throws OpenCodeException 如果获取失败
     */
    public List<MessageInfo> getMessageHistory(String sessionId) throws OpenCodeException {
        try {
            String response = get("/session/" + sessionId + "/message");
            JsonNode root = objectMapper.readTree(response);

            List<MessageInfo> messages = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode msg : root) {
                    JsonNode info = msg.path("info");
                    messages.add(new MessageInfo(
                            info.path("id").asText(),
                            info.path("role").asText(),
                            info.path("sessionID").asText(),
                            info.path("time").path("created").asLong(0)
                    ));
                }
            }
            return messages;
        } catch (Exception e) {
            throw new OpenCodeException("Failed to get message history: " + e.getMessage(), e);
        }
    }

    // ==================== 高级方法 ====================

    /**
     * 快速对话：创建会话、发送消息、删除会话（一次性对话）
     * @param message 消息内容
     * @return 响应结果
     * @throws OpenCodeException 如果失败
     */
    public MessageResult quickChat(String message) throws OpenCodeException {
        String sessionId = createSession("Quick Chat");
        try {
            return sendMessage(sessionId, message);
        } finally {
            deleteSession(sessionId);
        }
    }

    /**
     * 快速对话（带标题）
     * @param title 会话标题
     * @param message 消息内容
     * @return 响应结果
     * @throws OpenCodeException 如果失败
     */
    public MessageResult quickChat(String title, String message) throws OpenCodeException {
        String sessionId = createSession(title);
        try {
            return sendMessage(sessionId, message);
        } finally {
            deleteSession(sessionId);
        }
    }

    // ==================== HTTP 方法 ====================

    private String get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(timeout)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new IOException("HTTP error: " + response.statusCode());
        }

        return response.body();
    }

    private String post(String path, Map<String, Object> body) throws IOException, InterruptedException {
        String requestBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        logger.debug("POST {} - {} bytes", path, requestBody.length());

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new IOException("HTTP error: " + response.statusCode() + " - " + response.body());
        }

        return response.body();
    }

    private String delete(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(timeout)
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new IOException("HTTP error: " + response.statusCode());
        }

        return response.body();
    }

    // ==================== 结果解析 ====================

    private MessageResult parseMessageResult(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);

        // 提取消息信息
        JsonNode info = root.path("info");
        String messageId = info.path("id").asText();
        String role = info.path("role").asText();
        String sessionId = info.path("sessionID").asText();
        long createdTime = info.path("time").path("created").asLong(0);
        long completedTime = info.path("time").path("completed").asLong(0);

        // 提取响应文本
        StringBuilder textBuilder = new StringBuilder();
        List<MessagePart> parts = new ArrayList<>();

        JsonNode partsNode = root.path("parts");
        if (partsNode.isArray()) {
            for (JsonNode part : partsNode) {
                String type = part.path("type").asText();
                String text = part.path("text").asText("");

                parts.add(new MessagePart(type, text, part));

                if ("text".equals(type)) {
                    textBuilder.append(text);
                }
            }
        }

        // 提取 token 使用情况
        JsonNode tokens = info.path("tokens");
        int totalTokens = tokens.path("total").asInt(0);
        int inputTokens = tokens.path("input").asInt(0);
        int outputTokens = tokens.path("output").asInt(0);

        return new MessageResult(
                messageId,
                sessionId,
                role,
                textBuilder.toString(),
                parts,
                createdTime,
                completedTime,
                totalTokens,
                inputTokens,
                outputTokens,
                response
        );
    }

    // ==================== 数据类 ====================

    /**
     * 健康状态
     */
    public static class HealthStatus {
        private final boolean healthy;
        private final String version;

        public HealthStatus(boolean healthy, String version) {
            this.healthy = healthy;
            this.version = version;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public String getVersion() {
            return version;
        }

        @Override
        public String toString() {
            return "HealthStatus{healthy=" + healthy + ", version='" + version + "'}";
        }
    }

    /**
     * 会话状态
     */
    public static class SessionStatus {
        private final String id;
        private final String slug;
        private final String title;
        private final String version;
        private final String projectId;
        private final String directory;

        public SessionStatus(String id, String slug, String title, String version, String projectId, String directory) {
            this.id = id;
            this.slug = slug;
            this.title = title;
            this.version = version;
            this.projectId = projectId;
            this.directory = directory;
        }

        public String getId() {
            return id;
        }

        public String getSlug() {
            return slug;
        }

        public String getTitle() {
            return title;
        }

        public String getVersion() {
            return version;
        }

        public String getProjectId() {
            return projectId;
        }

        public String getDirectory() {
            return directory;
        }

        @Override
        public String toString() {
            return "SessionStatus{id='" + id + "', title='" + title + "'}";
        }
    }

    /**
     * 消息结果
     */
    public static class MessageResult {
        private final String messageId;
        private final String sessionId;
        private final String role;
        private final String text;
        private final List<MessagePart> parts;
        private final long createdTime;
        private final long completedTime;
        private final int totalTokens;
        private final int inputTokens;
        private final int outputTokens;
        private final String rawResponse;

        public MessageResult(String messageId, String sessionId, String role, String text,
                           List<MessagePart> parts, long createdTime, long completedTime,
                           int totalTokens, int inputTokens, int outputTokens, String rawResponse) {
            this.messageId = messageId;
            this.sessionId = sessionId;
            this.role = role;
            this.text = text;
            this.parts = parts;
            this.createdTime = createdTime;
            this.completedTime = completedTime;
            this.totalTokens = totalTokens;
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.rawResponse = rawResponse;
        }

        public String getMessageId() {
            return messageId;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getRole() {
            return role;
        }

        public String getText() {
            return text;
        }

        public List<MessagePart> getParts() {
            return parts;
        }

        public long getCreatedTime() {
            return createdTime;
        }

        public long getCompletedTime() {
            return completedTime;
        }

        public int getTotalTokens() {
            return totalTokens;
        }

        public int getInputTokens() {
            return inputTokens;
        }

        public int getOutputTokens() {
            return outputTokens;
        }

        public String getRawResponse() {
            return rawResponse;
        }

        @Override
        public String toString() {
            return "MessageResult{messageId='" + messageId + "', role='" + role +
                   "', text.length=" + (text != null ? text.length() : 0) + "}";
        }
    }

    /**
     * 消息部分
     */
    public static class MessagePart {
        private final String type;
        private final String text;
        private final JsonNode rawNode;

        public MessagePart(String type, String text, JsonNode rawNode) {
            this.type = type;
            this.text = text;
            this.rawNode = rawNode;
        }

        public String getType() {
            return type;
        }

        public String getText() {
            return text;
        }

        public JsonNode getRawNode() {
            return rawNode;
        }

        @Override
        public String toString() {
            return "MessagePart{type='" + type + "', text='" +
                   (text != null && text.length() > 50 ? text.substring(0, 50) + "..." : text) + "'}";
        }
    }

    /**
     * 消息信息
     */
    public static class MessageInfo {
        private final String id;
        private final String role;
        private final String sessionId;
        private final long createdTime;

        public MessageInfo(String id, String role, String sessionId, long createdTime) {
            this.id = id;
            this.role = role;
            this.sessionId = sessionId;
            this.createdTime = createdTime;
        }

        public String getId() {
            return id;
        }

        public String getRole() {
            return role;
        }

        public String getSessionId() {
            return sessionId;
        }

        public long getCreatedTime() {
            return createdTime;
        }
    }

    /**
     * OpenCode 异常
     */
    public static class OpenCodeException extends Exception {
        public OpenCodeException(String message) {
            super(message);
        }

        public OpenCodeException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // ==================== Builder ====================

    /**
     * 创建客户端构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String baseUrl = DEFAULT_BASE_URL;
        private Duration timeout = DEFAULT_TIMEOUT;

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder timeoutMinutes(int minutes) {
            this.timeout = Duration.ofMinutes(minutes);
            return this;
        }

        public Builder timeoutSeconds(int seconds) {
            this.timeout = Duration.ofSeconds(seconds);
            return this;
        }

        public OpenCodeClient build() {
            return new OpenCodeClient(baseUrl, timeout);
        }
    }
}
