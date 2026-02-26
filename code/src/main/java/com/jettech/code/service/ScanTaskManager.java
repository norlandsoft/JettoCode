package com.jettech.code.service;

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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 扫描任务管理器
 * 负责创建和执行扫描任务
 */
@Service
public class ScanTaskManager {

    private static final Logger logger = LoggerFactory.getLogger(ScanTaskManager.class);

    private final CodeQualityScanMapper scanMapper;
    private final CodeQualityTaskMapper taskMapper;
    private final CodeQualityIssueMapper issueMapper;
    private final ServiceMapper serviceMapper;
    private final QualityCheckConfigMapper checkConfigMapper;
    private final QualityCheckGroupMapper groupMapper;
    private final OpenCodeService openCodeService;

    public ScanTaskManager(CodeQualityScanMapper scanMapper,
                          CodeQualityTaskMapper taskMapper,
                          CodeQualityIssueMapper issueMapper,
                          ServiceMapper serviceMapper,
                          QualityCheckConfigMapper checkConfigMapper,
                          QualityCheckGroupMapper groupMapper,
                          OpenCodeService openCodeService) {
        this.scanMapper = scanMapper;
        this.taskMapper = taskMapper;
        this.issueMapper = issueMapper;
        this.serviceMapper = serviceMapper;
        this.checkConfigMapper = checkConfigMapper;
        this.groupMapper = groupMapper;
        this.openCodeService = openCodeService;
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
                QualityCheckGroup group = groupMap.get(item.getGroupId());

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
     * 异步执行所有扫描任务
     */
    @Async
    public void executeScanAsync(Long scanId, List<Long> serviceIds, List<Long> checkItemIds) {
        CodeQualityScan scan = scanMapper.findById(scanId);
        if (scan == null) {
            logger.error("Scan not found: {}", scanId);
            return;
        }

        try {
            // 更新扫描状态
            updateScanPhase(scan, "正在创建扫描任务...");

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

            // 逐个执行任务
            int completedTasks = 0;
            int totalIssues = 0;
            int securityIssues = 0;
            int reliabilityIssues = 0;
            int maintainabilityIssues = 0;

            for (CodeQualityTask task : tasks) {
                try {
                    updateTaskProgress(scan, completedTasks, tasks.size(), task);

                    // 执行单个任务
                    executeTask(task);

                    // 统计结果
                    if (task.getIssueCount() != null && task.getIssueCount() > 0) {
                        totalIssues += task.getIssueCount();

                        // 根据检查项类别分类统计
                        String category = getCategoryFromCheckItemKey(task.getCheckItemKey());
                        switch (category) {
                            case CodeQualityIssue.CATEGORY_SECURITY:
                                securityIssues += task.getIssueCount();
                                break;
                            case CodeQualityIssue.CATEGORY_RELIABILITY:
                                reliabilityIssues += task.getIssueCount();
                                break;
                            case CodeQualityIssue.CATEGORY_MAINTAINABILITY:
                                maintainabilityIssues += task.getIssueCount();
                                break;
                        }

                        // 从任务结果创建问题记录
                        createIssuesFromTaskResult(task);
                    }

                } catch (Exception e) {
                    logger.error("Task execution failed: {} - {}", task.getId(), e.getMessage());
                    failTask(task, e.getMessage());
                }

                completedTasks++;
            }

            completeScan(scan, totalIssues, securityIssues, reliabilityIssues, maintainabilityIssues);

        } catch (Exception e) {
            logger.error("Scan execution failed: {}", e.getMessage(), e);
            failScan(scan, e.getMessage());
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

        // 构建提示词
        String prompt = buildPrompt(service, checkItem, groupKey, localPath);
        task.setPromptText(prompt.length() > 10000 ? prompt.substring(0, 10000) + "..." : prompt);

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

        // 保存结果
        task.setResponseText(result.getFullResponse());
        task.setIssueCount(result.getIssueCount());
        task.setSeverity(result.getSeverity());
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
     * 构建扫描提示词
     */
    private String buildPrompt(ServiceEntity service, QualityCheckConfig checkItem, String groupKey, String localPath) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("# 代码质量检查任务\n\n");
        prompt.append("## 服务信息\n");
        prompt.append("- 服务名称: ").append(service.getName()).append("\n");
        prompt.append("- 代码路径: ").append(localPath).append("\n");
        if (service.getDescription() != null && !service.getDescription().isEmpty()) {
            prompt.append("- 描述: ").append(service.getDescription()).append("\n");
        }
        prompt.append("\n");

        prompt.append("## 检查项信息\n");
        prompt.append("- 检查项: ").append(checkItem.getItemName()).append("\n");
        prompt.append("- 维度: ").append(groupKey).append("\n");
        if (checkItem.getDescription() != null && !checkItem.getDescription().isEmpty()) {
            prompt.append("- 描述: ").append(checkItem.getDescription()).append("\n");
        }
        prompt.append("\n");

        // 添加检查项的提示词模板
        if (checkItem.getPromptTemplate() != null && !checkItem.getPromptTemplate().isEmpty()) {
            prompt.append("## 检查要求\n");
            prompt.append(checkItem.getPromptTemplate()).append("\n\n");
        }

        // 添加代码结构信息
        prompt.append("## 代码结构\n");
        prompt.append("请扫描以下路径中的代码文件:\n");
        prompt.append("```\n").append(localPath).append("\n```\n\n");

        prompt.append("## 输出要求\n");
        prompt.append("请以结构化的方式输出检查结果，包括:\n");
        prompt.append("1. 发现的问题数量\n");
        prompt.append("2. 每个问题的严重级别 (Critical/High/Medium/Low/Info)\n");
        prompt.append("3. 问题描述和位置\n");
        prompt.append("4. 修复建议\n\n");

        prompt.append("开始执行检查...\n");

        return prompt.toString();
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
     * 从任务结果创建问题记录
     */
    private void createIssuesFromTaskResult(CodeQualityTask task) {
        if (task.getResponseText() == null || task.getResponseText().isEmpty()) {
            return;
        }

        // 简单解析响应文本，提取问题
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
                return CodeQualityIssue.SEVERITY_CRITICAL;
            case "HIGH":
                return CodeQualityIssue.SEVERITY_MAJOR;
            case "MEDIUM":
                return CodeQualityIssue.SEVERITY_MINOR;
            case "LOW":
                return CodeQualityIssue.SEVERITY_INFO;
            default:
                return CodeQualityIssue.SEVERITY_INFO;
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
     * 更新任务进度
     */
    private void updateTaskProgress(CodeQualityScan scan, int completed, int total, CodeQualityTask currentTask) {
        scan.setCheckedCount(completed);
        scan.setCurrentFile(String.format("%s - %s", currentTask.getServiceName(), currentTask.getCheckItemName()));
        scan.setProgress(calculateProgress(completed, total));
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
