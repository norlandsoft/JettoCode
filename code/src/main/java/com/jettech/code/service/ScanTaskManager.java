package com.jettech.code.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jettech.code.dto.OpenCodeDTO;
import com.jettech.code.entity.CodeQualityIssue;
import com.jettech.code.entity.CodeQualityScan;
import com.jettech.code.entity.CodeQualityTask;
import com.jettech.code.entity.QualityCheckConfig;
import com.jettech.code.entity.QualityCheckGroup;
import com.jettech.code.entity.ServiceEntity;
import com.jettech.code.mapper.CodeQualityIssueMapper;
import com.jettech.code.mapper.CodeQualityScanMapper;
import com.jettech.code.mapper.CodeQualityTaskMapper;
import com.jettech.code.mapper.QualityCheckConfigMapper;
import com.jettech.code.mapper.QualityCheckGroupMapper;
import com.jettech.code.mapper.ServiceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 扫描任务管理器
 * 负责创建和执行扫描任务
 * 支持并行执行、任务取消、代码内容读取
 */
@Service
public class ScanTaskManager {

    private static final Logger logger = LoggerFactory.getLogger(ScanTaskManager.class);

    // 代码文件扩展名
    private static final Set<String> CODE_EXTENSIONS = new HashSet<>(Arrays.asList(
        ".java", ".ts", ".tsx", ".js", ".jsx", ".py", ".go", ".vue", ".kt", ".scala", ".rs", ".c", ".cpp", ".h", ".cs"
    ));

    // 排除的目录
    private static final Set<String> EXCLUDED_DIRS = new HashSet<>(Arrays.asList(
        "target", "node_modules", ".git", "build", "dist", ".idea", ".vscode", "vendor", "__pycache__", ".mvn", "gradle"
    ));

    // 代码读取限制
    private static final int MAX_FILES = 30;
    private static final int MAX_SIZE_PER_FILE = 15000;
    private static final int MAX_TOTAL_SIZE = 150000;

    private final CodeQualityScanMapper scanMapper;
    private final CodeQualityTaskMapper taskMapper;
    private final CodeQualityIssueMapper issueMapper;
    private final ServiceMapper serviceMapper;
    private final QualityCheckConfigMapper checkConfigMapper;
    private final QualityCheckGroupMapper groupMapper;
    private final OpenCodeService openCodeService;
    private final ObjectMapper objectMapper;

    // 自定义线程池
    @Autowired
    @Qualifier("scanTaskExecutor")
    private Executor scanTaskExecutor;

    // 取消状态管理
    private final ConcurrentHashMap<Long, Boolean> cancelledScans = new ConcurrentHashMap<>();

    // 进度统计（用于并行执行时的线程安全计数）
    private final ConcurrentHashMap<Long, AtomicInteger> progressCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicInteger> totalIssuesMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicInteger> securityIssuesMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicInteger> reliabilityIssuesMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicInteger> maintainabilityIssuesMap = new ConcurrentHashMap<>();

    // WebSocket 进度服务（可选注入）
    @Autowired(required = false)
    private ScanProgressService progressService;

    public ScanTaskManager(CodeQualityScanMapper scanMapper,
                          CodeQualityTaskMapper taskMapper,
                          CodeQualityIssueMapper issueMapper,
                          ServiceMapper serviceMapper,
                          QualityCheckConfigMapper checkConfigMapper,
                          QualityCheckGroupMapper groupMapper,
                          OpenCodeService openCodeService,
                          ObjectMapper objectMapper) {
        this.scanMapper = scanMapper;
        this.taskMapper = taskMapper;
        this.issueMapper = issueMapper;
        this.serviceMapper = serviceMapper;
        this.checkConfigMapper = checkConfigMapper;
        this.groupMapper = groupMapper;
        this.openCodeService = openCodeService;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建扫描任务
     * 交叉匹配服务列表和检查项列表
     */
    public List<CodeQualityTask> createScanTasks(Long scanId, List<Long> serviceIds, List<Long> checkItemIds) {
        List<CodeQualityTask> tasks = new ArrayList<>();

        // 获取服务信息
        List<ServiceEntity> services = new ArrayList<>();
        for (Long serviceId : serviceIds) {
            ServiceEntity service = serviceMapper.findById(serviceId);
            if (service != null) {
                services.add(service);
            }
        }

        // 获取检查项信息和对应的分组信息
        List<QualityCheckConfig> checkItems = new ArrayList<>();
        Map<Long, QualityCheckGroup> groupMap = new HashMap<>();

        for (Long itemId : checkItemIds) {
            QualityCheckConfig item = checkConfigMapper.findById(itemId);
            if (item != null) {
                checkItems.add(item);

                // 获取分组信息
                if (!groupMap.containsKey(item.getGroupId())) {
                    QualityCheckGroup group = groupMapper.findById(item.getGroupId());
                    if (group != null) {
                        groupMap.put(item.getGroupId(), group);
                    }
                }
            }
        }

        // 交叉匹配创建任务
        int priority = 0;
        for (ServiceEntity service : services) {
            for (QualityCheckConfig item : checkItems) {
                CodeQualityTask task = new CodeQualityTask();
                task.setScanId(scanId);
                task.setServiceId(service.getId());
                task.setServiceName(service.getName());
                task.setCheckItemId(item.getId());
                task.setCheckItemKey(item.getItemKey());
                task.setCheckItemName(item.getItemName());
                task.setStatus(CodeQualityTask.STATUS_PENDING);
                task.setPriority(priority++);
                task.setRetryCount(0);
                task.setIssueCount(0);
                task.setSeverity(CodeQualityTask.SEVERITY_NONE);

                tasks.add(task);
            }
        }

        if (!tasks.isEmpty()) {
            taskMapper.batchInsert(tasks);
            logger.info("Created {} scan tasks for scan {}", tasks.size(), scanId);
        }

        return tasks;
    }

    /**
     * 异步执行所有扫描任务（并行）
     */
    @Async("scanTaskExecutor")
    public void executeScanAsync(Long scanId, List<Long> serviceIds, List<Long> checkItemIds) {
        CodeQualityScan scan = scanMapper.findById(scanId);
        if (scan == null) {
            logger.error("Scan not found: {}", scanId);
            return;
        }

        // 清除之前的取消状态
        cancelledScans.remove(scanId);
        progressCounters.put(scanId, new AtomicInteger(0));
        totalIssuesMap.put(scanId, new AtomicInteger(0));
        securityIssuesMap.put(scanId, new AtomicInteger(0));
        reliabilityIssuesMap.put(scanId, new AtomicInteger(0));
        maintainabilityIssuesMap.put(scanId, new AtomicInteger(0));

        try {
            // 更新扫描状态
            updateScanPhase(scan, "正在创建扫描任务...");
            notifyProgress(scanId, "PHASE", "正在创建扫描任务...", 0, 0, 0);

            // 创建任务
            List<CodeQualityTask> tasks = createScanTasks(scanId, serviceIds, checkItemIds);

            if (tasks.isEmpty()) {
                completeScan(scan, 0, 0, 0, 0);
                return;
            }

            // 更新总文件数（用任务数代替）
            scan.setTotalFiles(tasks.size());
            scanMapper.update(scan);

            updateScanPhase(scan, "正在执行扫描任务...");
            notifyProgress(scanId, "PHASE", "正在执行扫描任务...", 0, 0, tasks.size());

            // 并行执行任务
            int concurrency = Math.min(tasks.size(), 5); // 最大并发 5
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (CodeQualityTask task : tasks) {
                // 检查是否被取消
                if (isScanCancelled(scanId)) {
                    logger.info("Scan {} cancelled during task creation", scanId);
                    cancelScanInternal(scan);
                    return;
                }

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    executeTaskWithStats(task, scanId, tasks.size());
                }, scanTaskExecutor);

                futures.add(future);
            }

            // 等待所有任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // 检查是否被取消
            if (isScanCancelled(scanId)) {
                cancelScanInternal(scan);
                return;
            }

            // 获取统计结果
            int totalIssues = totalIssuesMap.get(scanId).get();
            int securityIssues = securityIssuesMap.get(scanId).get();
            int reliabilityIssues = reliabilityIssuesMap.get(scanId).get();
            int maintainabilityIssues = maintainabilityIssuesMap.get(scanId).get();

            completeScan(scan, totalIssues, securityIssues, reliabilityIssues, maintainabilityIssues);

        } catch (Exception e) {
            logger.error("Scan execution failed: {}", e.getMessage(), e);
            failScan(scan, e.getMessage());
            notifyProgress(scanId, "ERROR", e.getMessage(), 0, 0, 0);
        } finally {
            // 清理计数器
            progressCounters.remove(scanId);
            totalIssuesMap.remove(scanId);
            securityIssuesMap.remove(scanId);
            reliabilityIssuesMap.remove(scanId);
            maintainabilityIssuesMap.remove(scanId);
        }
    }

    /**
     * 执行单个任务并更新统计
     */
    private void executeTaskWithStats(CodeQualityTask task, Long scanId, int totalTasks) {
        if (isScanCancelled(scanId)) {
            task.setStatus(CodeQualityTask.STATUS_CANCELLED);
            taskMapper.update(task);
            return;
        }

        try {
            executeTask(task);

            // 更新进度
            int completed = progressCounters.get(scanId).incrementAndGet();
            int progress = calculateProgress(completed, totalTasks);

            // 更新扫描记录
            CodeQualityScan scan = scanMapper.findById(scanId);
            if (scan != null) {
                scan.setCheckedCount(completed);
                scan.setProgress(progress);
                scan.setCurrentFile(task.getServiceName() + " - " + task.getCheckItemName());
                scanMapper.update(scan);
            }

            // 通知进度
            notifyProgress(scanId, "PROGRESS", null, progress, completed, totalTasks);

            // 统计结果
            if (task.getIssueCount() != null && task.getIssueCount() > 0) {
                totalIssuesMap.get(scanId).addAndGet(task.getIssueCount());

                String category = getCategoryFromCheckItemKey(task.getCheckItemKey());
                switch (category) {
                    case CodeQualityIssue.CATEGORY_SECURITY:
                        securityIssuesMap.get(scanId).addAndGet(task.getIssueCount());
                        break;
                    case CodeQualityIssue.CATEGORY_RELIABILITY:
                        reliabilityIssuesMap.get(scanId).addAndGet(task.getIssueCount());
                        break;
                    case CodeQualityIssue.CATEGORY_MAINTAINABILITY:
                        maintainabilityIssuesMap.get(scanId).addAndGet(task.getIssueCount());
                        break;
                }

                // 创建问题记录
                createIssuesFromTaskResult(task);
            }

            // 通知任务完成
            notifyTaskComplete(scanId, task);

        } catch (Exception e) {
            logger.error("Task execution failed: {} - {}", task.getId(), e.getMessage());
            failTask(task, e.getMessage());
        }
    }

    /**
     * 执行单个扫描任务
     */
    private void executeTask(CodeQualityTask task) throws Exception {
        logger.info("Executing task {} for service {} check item {}",
            task.getId(), task.getServiceId(), task.getCheckItemKey());

        // 更新任务状态为运行中
        task.setStatus(CodeQualityTask.STATUS_RUNNING);
        task.setStartedAt(LocalDateTime.now());
        taskMapper.update(task);

        // 获取服务信息
        ServiceEntity service = serviceMapper.findById(task.getServiceId());
        if (service == null) {
            throw new IllegalArgumentException("Service not found: " + task.getServiceId());
        }

        String localPath = service.getLocalPath();
        if (localPath == null || localPath.isEmpty()) {
            throw new IllegalArgumentException("Service local path not configured");
        }

        // 获取检查项信息
        QualityCheckConfig checkItem = checkConfigMapper.findById(task.getCheckItemId());
        if (checkItem == null) {
            throw new IllegalArgumentException("Check item not found: " + task.getCheckItemId());
        }

        // 获取分组信息
        QualityCheckGroup group = groupMapper.findById(checkItem.getGroupId());
        String groupKey = group != null ? group.getGroupKey() : "unknown";

        // 构建提示词（包含代码内容）
        String prompt = buildPrompt(service, checkItem, groupKey, localPath);
        task.setPromptText(prompt);

        // 检查 OpenCode 服务是否可用
        if (!openCodeService.isAvailable()) {
            throw new IllegalStateException("OpenCode service is not available");
        }

        // 创建 OpenCode 会话
        String sessionTitle = "Code Quality Scan - " + service.getName() + " - " + checkItem.getItemName();
        String sessionId = openCodeService.createSession(sessionTitle);
        task.setOpencodeSessionId(sessionId);
        taskMapper.update(task);

        // 发送提示词并获取结果
        OpenCodeDTO.ScanResult result = openCodeService.sendPrompt(sessionId, prompt);

        // 尝试解析结构化结果
        parseStructuredResult(task, result);

        // 保存原始响应
        task.setResponseText(result.getFullResponse());
        task.setResultSummary(result.getSummary());

        if (result.isSuccess()) {
            task.setStatus(CodeQualityTask.STATUS_COMPLETED);
        } else {
            task.setStatus(CodeQualityTask.STATUS_FAILED);
            task.setErrorMessage(result.getErrorMessage());
        }

        task.setCompletedAt(LocalDateTime.now());
        taskMapper.update(task);

        // 清理会话
        openCodeService.deleteSession(sessionId);

        logger.info("Task {} completed: {} issues found", task.getId(), task.getIssueCount());
    }

    /**
     * 解析结构化 JSON 结果
     */
    private void parseStructuredResult(CodeQualityTask task, OpenCodeDTO.ScanResult result) {
        String responseText = result.getFullResponse();
        if (responseText == null || responseText.isEmpty()) {
            task.setIssueCount(0);
            task.setSeverity(CodeQualityTask.SEVERITY_NONE);
            return;
        }

        try {
            // 尝试提取 JSON 块
            String jsonContent = extractJsonFromResponse(responseText);
            if (jsonContent != null) {
                JsonNode root = objectMapper.readTree(jsonContent);

                // 解析 issues 数组
                JsonNode issuesNode = root.path("issues");
                if (issuesNode.isArray()) {
                    int issueCount = issuesNode.size();
                    task.setIssueCount(issueCount);

                    // 确定最高严重级别
                    String maxSeverity = determineMaxSeverity(issuesNode);
                    task.setSeverity(maxSeverity);
                }

                // 解析 summary
                JsonNode summaryNode = root.path("summary");
                if (!summaryNode.isMissingNode()) {
                    String briefSummary = summaryNode.path("briefSummary").asText(null);
                    if (briefSummary != null) {
                        task.setResultSummary(briefSummary);
                    }
                }

                logger.debug("Parsed structured result: {} issues, severity: {}",
                    task.getIssueCount(), task.getSeverity());
                return;
            }
        } catch (Exception e) {
            logger.debug("Failed to parse structured result, falling back to simple parsing: {}", e.getMessage());
        }

        // 回退到简单解析
        task.setIssueCount(result.getIssueCount());
        task.setSeverity(result.getSeverity());
    }

    /**
     * 从响应中提取 JSON 内容
     */
    private String extractJsonFromResponse(String response) {
        if (response == null) return null;

        // 查找 ```json ... ``` 块
        int jsonStart = response.indexOf("```json");
        if (jsonStart >= 0) {
            int contentStart = response.indexOf('\n', jsonStart);
            if (contentStart > jsonStart) {
                int jsonEnd = response.indexOf("```", contentStart);
                if (jsonEnd > contentStart) {
                    return response.substring(contentStart + 1, jsonEnd).trim();
                }
            }
        }

        // 尝试直接解析整个响应
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }

        return null;
    }

    /**
     * 确定最高严重级别
     */
    private String determineMaxSeverity(JsonNode issuesNode) {
        int critical = 0, high = 0, medium = 0, low = 0;

        for (JsonNode issue : issuesNode) {
            String severity = issue.path("severity").asText("").toUpperCase();
            switch (severity) {
                case "CRITICAL":
                    critical++;
                    break;
                case "HIGH":
                    high++;
                    break;
                case "MEDIUM":
                    medium++;
                    break;
                case "LOW":
                    low++;
                    break;
            }
        }

        if (critical > 0) return "CRITICAL";
        if (high > 0) return "HIGH";
        if (medium > 0) return "MEDIUM";
        if (low > 0) return "LOW";
        return "NONE";
    }

    /**
     * 构建扫描提示词（包含代码内容，要求 JSON 输出）
     */
    private String buildPrompt(ServiceEntity service, QualityCheckConfig checkItem, String groupKey, String localPath) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("# 代码质量检查任务\n\n");

        // 服务信息
        prompt.append("## 服务信息\n");
        prompt.append("- 服务名称: ").append(service.getName()).append("\n");
        prompt.append("- 代码路径: ").append(localPath).append("\n");
        if (service.getDescription() != null && !service.getDescription().isEmpty()) {
            prompt.append("- 描述: ").append(service.getDescription()).append("\n");
        }
        prompt.append("\n");

        // 检查项信息
        prompt.append("## 检查项信息\n");
        prompt.append("- 检查项: ").append(checkItem.getItemName()).append("\n");
        prompt.append("- 维度: ").append(groupKey).append("\n");
        if (checkItem.getDescription() != null && !checkItem.getDescription().isEmpty()) {
            prompt.append("- 描述: ").append(checkItem.getDescription()).append("\n");
        }
        prompt.append("\n");

        // 检查项的提示词模板（清理掉可能包含的代码结构/代码内容部分）
        String promptTemplate = checkItem.getPromptTemplate();
        if (promptTemplate != null && !promptTemplate.isEmpty() && !promptTemplate.equals("略")) {
            // 清理 promptTemplate 中可能包含的代码结构/代码内容部分
            String cleanedTemplate = cleanPromptTemplate(promptTemplate);
            if (!cleanedTemplate.isEmpty()) {
                prompt.append("## 检查要求\n");
                prompt.append(cleanedTemplate).append("\n\n");
            }
        }

        // 读取代码内容
        prompt.append("## 代码内容\n\n");
        String codeContent = readCodeContent(localPath);
        prompt.append(codeContent);
        prompt.append("\n");

        logger.info("Built prompt with {} chars, code content: {} chars",
            prompt.length(), codeContent.length());

        // JSON 输出格式要求
        prompt.append("## 输出要求\n");
        prompt.append("请严格按照以下 JSON 格式输出检查结果，不要输出其他内容：\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"issues\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"filePath\": \"相对文件路径\",\n");
        prompt.append("      \"line\": 行号(数字),\n");
        prompt.append("      \"column\": 列号(数字),\n");
        prompt.append("      \"category\": \"SECURITY|RELIABILITY|MAINTAINABILITY|CODE_SMELL\",\n");
        prompt.append("      \"severity\": \"CRITICAL|HIGH|MEDIUM|LOW|INFO\",\n");
        prompt.append("      \"ruleId\": \"规则ID\",\n");
        prompt.append("      \"ruleName\": \"规则名称\",\n");
        prompt.append("      \"message\": \"问题描述\",\n");
        prompt.append("      \"suggestion\": \"修复建议\",\n");
        prompt.append("      \"codeSnippet\": \"相关代码片段\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"summary\": {\n");
        prompt.append("    \"totalIssues\": 问题总数,\n");
        prompt.append("    \"securityIssues\": 安全问题数,\n");
        prompt.append("    \"reliabilityIssues\": 可靠性问题数,\n");
        prompt.append("    \"maintainabilityIssues\": 可维护性问题数,\n");
        prompt.append("    \"overallSeverity\": \"总体严重级别\",\n");
        prompt.append("    \"briefSummary\": \"简要总结\"\n");
        prompt.append("  }\n");
        prompt.append("}\n");
        prompt.append("```\n\n");

        prompt.append("请仔细分析代码，找出所有相关问题，并按照上述 JSON 格式输出结果。\n");

        return prompt.toString();
    }

    /**
     * 清理提示词模板，移除可能包含的代码结构/代码内容/输出要求部分
     * 只保留检查要求的核心内容
     */
    private String cleanPromptTemplate(String template) {
        if (template == null || template.isEmpty()) {
            return "";
        }

        // 需要移除的部分的标记
        String[] sectionsToRemove = {
            "## 代码结构", "##代码结构",
            "## 代码内容", "##代码内容",
            "## 输出要求", "##输出要求",
            "## 输出格式", "##输出格式"
        };

        String result = template;
        for (String section : sectionsToRemove) {
            int index = result.indexOf(section);
            if (index >= 0) {
                result = result.substring(0, index);
            }
        }

        return result.trim();
    }

    /**
     * 读取代码内容
     */
    private String readCodeContent(String localPath) {
        StringBuilder content = new StringBuilder();
        File rootDir = new File(localPath);

        logger.info("Reading code content from path: {}", localPath);

        if (!rootDir.exists()) {
            logger.error("Code path does not exist: {}", localPath);
            return "错误：代码路径不存在: " + localPath + "\n";
        }

        if (!rootDir.isDirectory()) {
            logger.error("Code path is not a directory: {}", localPath);
            return "错误：代码路径不是目录: " + localPath + "\n";
        }

        List<File> sourceFiles = new ArrayList<>();
        scanSourceFiles(rootDir, sourceFiles, rootDir.getAbsolutePath());

        logger.info("Found {} source files in {}", sourceFiles.size(), localPath);

        if (sourceFiles.isEmpty()) {
            logger.warn("No source code files found in: {}", localPath);
            return "未找到源代码文件，路径: " + localPath + "\n";
        }

        long totalSize = 0;
        int fileCount = 0;

        for (File file : sourceFiles) {
            if (fileCount >= MAX_FILES || totalSize >= MAX_TOTAL_SIZE) {
                break;
            }

            try {
                String fileContent = Files.readString(file.toPath());
                String relativePath = file.getAbsolutePath();
                if (relativePath.startsWith(localPath)) {
                    relativePath = relativePath.substring(localPath.length());
                    if (relativePath.startsWith("/")) {
                        relativePath = relativePath.substring(1);
                    }
                }

                // 截断过长的文件
                if (fileContent.length() > MAX_SIZE_PER_FILE) {
                    fileContent = fileContent.substring(0, MAX_SIZE_PER_FILE) + "\n... (文件过长，已截断)";
                }

                content.append("### 文件: ").append(relativePath).append("\n");
                content.append("```\n").append(fileContent).append("\n```\n\n");

                totalSize += fileContent.length();
                fileCount++;

            } catch (Exception e) {
                logger.warn("Failed to read file {}: {}", file.getPath(), e.getMessage());
            }
        }

        if (sourceFiles.size() > fileCount) {
            content.append("... 还有 ").append(sourceFiles.size() - fileCount).append(" 个文件未显示\n");
        }

        content.append("共读取 ").append(fileCount).append(" 个文件，约 ")
               .append(totalSize / 1024).append(" KB\n");

        return content.toString();
    }

    /**
     * 递归扫描源代码文件
     */
    private void scanSourceFiles(File dir, List<File> files, String rootPath) {
        File[] children = dir.listFiles();
        if (children == null) return;

        for (File child : children) {
            if (child.isDirectory()) {
                // 跳过排除的目录
                if (!EXCLUDED_DIRS.contains(child.getName())) {
                    scanSourceFiles(child, files, rootPath);
                }
            } else if (child.isFile()) {
                String name = child.getName().toLowerCase();
                for (String ext : CODE_EXTENSIONS) {
                    if (name.endsWith(ext)) {
                        files.add(child);
                        break;
                    }
                }
            }
        }
    }

    /**
     * 从检查项 key 获取类别
     */
    private String getCategoryFromCheckItemKey(String key) {
        if (key == null) return CodeQualityIssue.CATEGORY_CODE_SMELL;

        String groupKey = key.contains("_") ? key.substring(0, key.indexOf("_")) : key;

        switch (groupKey.toLowerCase()) {
            case "security":
            case "injection":
            case "auth":
                return CodeQualityIssue.CATEGORY_SECURITY;
            case "reliability":
            case "error":
            case "exception":
                return CodeQualityIssue.CATEGORY_RELIABILITY;
            case "maintainability":
            case "complexity":
            case "duplication":
                return CodeQualityIssue.CATEGORY_MAINTAINABILITY;
            default:
                return CodeQualityIssue.CATEGORY_CODE_SMELL;
        }
    }

    /**
     * 从任务结果创建问题记录（支持结构化 JSON）
     */
    private void createIssuesFromTaskResult(CodeQualityTask task) {
        if (task.getResponseText() == null || task.getResponseText().isEmpty()) {
            return;
        }

        // 尝试解析结构化 JSON
        String jsonContent = extractJsonFromResponse(task.getResponseText());
        if (jsonContent != null) {
            try {
                JsonNode root = objectMapper.readTree(jsonContent);
                JsonNode issuesNode = root.path("issues");

                if (issuesNode.isArray()) {
                    for (JsonNode issueNode : issuesNode) {
                        CodeQualityIssue issue = new CodeQualityIssue();
                        issue.setScanId(task.getScanId());
                        issue.setFilePath(issueNode.path("filePath").asText("OpenCode分析结果"));
                        issue.setLine(issueNode.path("line").asInt(1));
                        issue.setColumn(issueNode.path("column").asInt(1));

                        String category = issueNode.path("category").asText(null);
                        if (category == null || category.isEmpty()) {
                            category = getCategoryFromCheckItemKey(task.getCheckItemKey());
                        }
                        issue.setCategory(category);

                        String severity = issueNode.path("severity").asText("INFO");
                        issue.setSeverity(mapSeverity(severity));

                        issue.setRuleId(issueNode.path("ruleId").asText(task.getCheckItemKey()));
                        issue.setRuleName(issueNode.path("ruleName").asText(task.getCheckItemName()));

                        String message = issueNode.path("message").asText("");
                        issue.setMessage(message.length() > 500 ? message.substring(0, 500) : message);

                        String suggestion = issueNode.path("suggestion").asText("请参考OpenCode分析结果进行修复");
                        issue.setSuggestion(suggestion.length() > 1000 ? suggestion.substring(0, 1000) : suggestion);

                        String codeSnippet = issueNode.path("codeSnippet").asText("");
                        issue.setCodeSnippet(codeSnippet.length() > 1000 ? codeSnippet.substring(0, 1000) : codeSnippet);

                        issue.setStatus("OPEN");
                        issue.setCreatedAt(LocalDateTime.now());

                        issueMapper.insert(issue);
                    }
                    return;
                }
            } catch (Exception e) {
                logger.debug("Failed to parse structured issues, falling back to simple parsing: {}", e.getMessage());
            }
        }

        // 回退到简单解析
        createIssuesFromTaskResultSimple(task);
    }

    /**
     * 从任务结果创建问题记录（简单解析，回退方案）
     */
    private void createIssuesFromTaskResultSimple(CodeQualityTask task) {
        String[] lines = task.getResponseText().split("\n");
        int lineNumber = 1;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 检测可能的问题行
            if (line.contains("问题") || line.contains("issue") ||
                line.contains("错误") || line.contains("error") || line.contains("warning")) {

                CodeQualityIssue issue = new CodeQualityIssue();
                issue.setScanId(task.getScanId());
                issue.setFilePath("OpenCode分析结果");
                issue.setLine(lineNumber);
                issue.setColumn(1);
                issue.setCategory(getCategoryFromCheckItemKey(task.getCheckItemKey()));
                issue.setSeverity(mapSeverity(task.getSeverity()));
                issue.setRuleId(task.getCheckItemKey());
                issue.setRuleName(task.getCheckItemName());
                issue.setMessage(line.length() > 500 ? line.substring(0, 500) : line);
                issue.setSuggestion("请参考OpenCode分析结果进行修复");
                issue.setCodeSnippet(line.length() > 1000 ? line.substring(0, 1000) : line);
                issue.setStatus("OPEN");
                issue.setCreatedAt(LocalDateTime.now());

                issueMapper.insert(issue);
            }

            lineNumber++;
        }
    }

    /**
     * 映射严重级别
     */
    private String mapSeverity(String severity) {
        if (severity == null) return CodeQualityIssue.SEVERITY_INFO;

        switch (severity.toUpperCase()) {
            case "CRITICAL":
            case "BLOCKER":
                return CodeQualityIssue.SEVERITY_CRITICAL;
            case "HIGH":
            case "MAJOR":
                return CodeQualityIssue.SEVERITY_MAJOR;
            case "MEDIUM":
                return CodeQualityIssue.SEVERITY_MINOR;
            case "LOW":
            case "INFO":
            default:
                return CodeQualityIssue.SEVERITY_INFO;
        }
    }

    /**
     * 取消扫描
     */
    public boolean cancelScan(Long scanId) {
        cancelledScans.put(scanId, true);

        CodeQualityScan scan = scanMapper.findById(scanId);
        if (scan != null && "IN_PROGRESS".equals(scan.getStatus())) {
            scan.setStatus(CodeQualityScan.STATUS_CANCELLED);
            scan.setCurrentPhase("扫描已取消");
            scan.setCompletedAt(LocalDateTime.now());
            scanMapper.update(scan);

            // 取消所有待执行的任务
            List<CodeQualityTask> pendingTasks = taskMapper.findPendingTasks(scanId);
            for (CodeQualityTask task : pendingTasks) {
                task.setStatus(CodeQualityTask.STATUS_CANCELLED);
                taskMapper.update(task);
            }

            // 通知前端
            notifyProgress(scanId, "CANCELLED", "扫描已取消", 0, 0, 0);

            logger.info("Scan {} cancelled", scanId);
            return true;
        }

        return false;
    }

    /**
     * 检查扫描是否被取消
     */
    public boolean isScanCancelled(Long scanId) {
        return Boolean.TRUE.equals(cancelledScans.get(scanId));
    }

    /**
     * 内部取消处理
     */
    private void cancelScanInternal(CodeQualityScan scan) {
        scan.setStatus(CodeQualityScan.STATUS_CANCELLED);
        scan.setCurrentPhase("扫描已取消");
        scan.setCompletedAt(LocalDateTime.now());
        scanMapper.update(scan);
        notifyProgress(scan.getId(), "CANCELLED", "扫描已取消", 0, 0, 0);
    }

    /**
     * 通知进度更新（WebSocket）
     */
    private void notifyProgress(Long scanId, String type, String message, int progress, int completed, int total) {
        if (progressService != null) {
            progressService.notifyProgress(scanId, type, message, progress, completed, total);
        }
    }

    /**
     * 通知任务完成
     */
    private void notifyTaskComplete(Long scanId, CodeQualityTask task) {
        if (progressService != null) {
            progressService.notifyTaskComplete(scanId, task.getId(), task.getServiceName(),
                task.getCheckItemName(), task.getIssueCount(), task.getStatus());
        }
    }

    /**
     * 更新扫描阶段
     */
    private void updateScanPhase(CodeQualityScan scan, String phase) {
        scan.setCurrentPhase(phase);
        scan.setProgress(0);
        scan.setCheckedCount(0);
        scanMapper.update(scan);
    }

    /**
     * 计算进度
     */
    private int calculateProgress(int completed, int total) {
        if (total == 0) return 100;
        return (int) ((completed * 100.0) / total);
    }

    /**
     * 完成任务
     */
    private void failTask(CodeQualityTask task, String errorMessage) {
        task.setStatus(CodeQualityTask.STATUS_FAILED);
        task.setErrorMessage(errorMessage);
        task.setCompletedAt(LocalDateTime.now());
        taskMapper.update(task);
    }

    /**
     * 完成扫描
     */
    private void completeScan(CodeQualityScan scan, int totalIssues, int securityIssues,
                             int reliabilityIssues, int maintainabilityIssues) {
        scan.setTotalIssues(totalIssues);
        scan.setSecurityIssues(securityIssues);
        scan.setReliabilityIssues(reliabilityIssues);
        scan.setMaintainabilityIssues(maintainabilityIssues);
        scan.setCodeSmellIssues(totalIssues - securityIssues - reliabilityIssues - maintainabilityIssues);

        // 计算分数
        double qualityScore = calculateQualityScore(totalIssues, securityIssues, reliabilityIssues, maintainabilityIssues);
        double securityScore = securityIssues == 0 ? 100 : Math.max(0, 100 - securityIssues * 10);
        double reliabilityScore = reliabilityIssues == 0 ? 100 : Math.max(0, 100 - reliabilityIssues * 5);
        double maintainabilityScore = maintainabilityIssues == 0 ? 100 : Math.max(0, 100 - maintainabilityIssues * 3);

        scan.setQualityScore(qualityScore);
        scan.setSecurityScore(securityScore);
        scan.setReliabilityScore(reliabilityScore);
        scan.setMaintainabilityScore(maintainabilityScore);

        scan.setStatus("COMPLETED");
        scan.setProgress(100);
        scan.setCompletedAt(LocalDateTime.now());
        scan.setCurrentPhase("扫描完成");
        scan.setCurrentFile(null);
        scanMapper.update(scan);

        // 通知完成
        notifyProgress(scan.getId(), "COMPLETED", "扫描完成", 100, scan.getTotalFiles(), scan.getTotalFiles());

        logger.info("Scan {} completed: {} issues found", scan.getId(), totalIssues);
    }

    /**
     * 计算质量分数
     */
    private double calculateQualityScore(int total, int security, int reliability, int maintainability) {
        if (total == 0) return 100;
        double penalty = security * 15 + reliability * 8 + maintainability * 3;
        return Math.max(0, 100 - penalty);
    }

    /**
     * 扫描失败
     */
    private void failScan(CodeQualityScan scan, String errorMessage) {
        scan.setStatus("FAILED");
        scan.setCompletedAt(LocalDateTime.now());
        scan.setCurrentPhase("扫描失败: " + errorMessage);
        scanMapper.update(scan);
    }

    /**
     * 获取扫描任务列表
     */
    public List<CodeQualityTask> getScanTasks(Long scanId) {
        return taskMapper.findByScanId(scanId);
    }

    /**
     * 获取单个任务
     */
    public CodeQualityTask getTask(Long taskId) {
        return taskMapper.findById(taskId);
    }

    /**
     * 获取扫描进度
     */
    public int getScanProgress(Long scanId) {
        int total = taskMapper.countTotalByScanId(scanId);
        int completed = taskMapper.countCompletedByScanId(scanId);
        return calculateProgress(completed, total);
    }
}
