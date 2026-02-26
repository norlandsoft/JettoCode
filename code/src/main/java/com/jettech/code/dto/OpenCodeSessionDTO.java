package com.jettech.code.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenCode API 相关 DTO
 */
public class OpenCodeDTO {

    /**
     * 创建会话请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateSessionRequest {
        private String title;
        private String systemPrompt;
    }

    /**
     * 创建会话响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateSessionResponse {
        private String id;
        private String title;
        private String status;
    }

    /**
     * 发送消息请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromptRequest {
        private String model;
        private List<PromptPart> parts;
    }

    /**
     * 消息部分
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromptPart {
        private String type;  // "text", "file"
        private String text;
        private String filePath;
    }

    /**
     * 发送消息响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromptResponse {
        private String id;
        private String role;
        private List<MessagePart> parts;
    }

    /**
     * 消息部分（响应）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessagePart {
        private String type;
        private String text;
    }

    /**
     * 扫描结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScanResult {
        private boolean success;
        private int issueCount;
        private String severity;
        private String summary;
        private String fullResponse;
        private String errorMessage;
    }
}
