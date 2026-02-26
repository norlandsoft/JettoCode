package com.jettech.code.service;

import com.jettech.code.entity.CodeQualityIssue;
import com.jettech.code.entity.CodeQualityScan;
import com.jettech.code.mapper.CodeQualityIssueMapper;
import com.jettech.code.mapper.CodeQualityScanMapper;
import com.jettech.code.mapper.ServiceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CodeQualityService {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeQualityService.class);
    
    private final CodeQualityScanMapper scanMapper;
    private final CodeQualityIssueMapper issueMapper;
    private final ServiceMapper serviceMapper;

    public CodeQualityService(CodeQualityScanMapper scanMapper,
                             CodeQualityIssueMapper issueMapper,
                             ServiceMapper serviceMapper) {
        this.scanMapper = scanMapper;
        this.issueMapper = issueMapper;
        this.serviceMapper = serviceMapper;
    }

    public List<CodeQualityScan> getScans(Long serviceId) {
        return scanMapper.findByServiceId(serviceId);
    }

    public CodeQualityScan getScanById(Long scanId) {
        return scanMapper.findById(scanId);
    }

    public CodeQualityScan getLatestScan(Long serviceId) {
        return scanMapper.findLatestByServiceId(serviceId);
    }

    public CodeQualityScan getLatestScanByApplication(Long applicationId) {
        return scanMapper.findLatestByApplicationId(applicationId);
    }

    public List<CodeQualityScan> getScansByApplication(Long applicationId) {
        return scanMapper.findByApplicationId(applicationId);
    }

    public List<CodeQualityIssue> getIssues(Long scanId) {
        return issueMapper.findByScanId(scanId);
    }

    public List<CodeQualityIssue> getIssuesByCategory(Long scanId, String category) {
        return issueMapper.findByScanIdAndCategory(scanId, category);
    }

    public List<CodeQualityIssue> getIssuesBySeverity(Long scanId, String severity) {
        return issueMapper.findByScanIdAndSeverity(scanId, severity);
    }

    public CodeQualityIssue getIssueById(Long issueId) {
        return issueMapper.findById(issueId);
    }

    public CodeQualityScan startScan(Long serviceId) throws Exception {
        com.jettech.code.entity.ServiceEntity service = serviceMapper.findById(serviceId);
        if (service == null) {
            throw new IllegalArgumentException("Service not found");
        }

        String localPath = service.getLocalPath();
        if (localPath == null || localPath.isEmpty()) {
            throw new IllegalArgumentException("Service local path not configured");
        }

        CodeQualityScan scan = new CodeQualityScan();
        scan.setServiceId(serviceId);
        scan.setStatus("IN_PROGRESS");
        scan.setStartedAt(LocalDateTime.now());
        scan.setCreatedAt(LocalDateTime.now());
        scan.setCheckedCount(0);
        scan.setProgress(0);
        scan.setCurrentPhase("正在初始化...");
        scanMapper.insert(scan);

        logger.info("Starting async code quality scan {} for service {}", scan.getId(), serviceId);

        executeScanAsync(scan.getId(), serviceId, localPath);

        return scan;
    }

    @Async
    public void executeScanAsync(Long scanId, Long serviceId, String localPath) {
        CodeQualityScan scan = scanMapper.findById(scanId);
        if (scan == null) {
            logger.error("Scan not found: {}", scanId);
            return;
        }

        try {
            issueMapper.deleteByScanId(scanId);

            File projectDir = new File(localPath);
            if (!projectDir.exists() || !projectDir.isDirectory()) {
                throw new IllegalArgumentException("Project directory not found: " + localPath);
            }

            updatePhase(scan, "正在扫描文件...");

            List<File> sourceFiles = scanSourceFiles(projectDir);
            scan.setTotalFiles(sourceFiles.size());
            scanMapper.update(scan);

            if (sourceFiles.isEmpty()) {
                completeScan(scan, new ArrayList<>());
                return;
            }

            updatePhase(scan, "正在分析代码...");

            List<CodeQualityIssue> allIssues = new ArrayList<>();
            int checkedCount = 0;

            for (File file : sourceFiles) {
                try {
                    updateProgress(scan, checkedCount, sourceFiles.size(), file.getPath());

                    List<CodeQualityIssue> fileIssues = analyzeFile(scanId, file);
                    allIssues.addAll(fileIssues);

                    checkedCount++;
                    scan.setCheckedCount(checkedCount);
                    scan.setProgress(calculateProgress(checkedCount, sourceFiles.size()));
                    scanMapper.update(scan);

                    TimeUnit.MILLISECONDS.sleep(50);

                } catch (Exception e) {
                    logger.error("Failed to analyze file {}: {}", file.getPath(), e.getMessage());
                    checkedCount++;
                    scan.setCheckedCount(checkedCount);
                    scan.setProgress(calculateProgress(checkedCount, sourceFiles.size()));
                    scanMapper.update(scan);
                }
            }

            if (!allIssues.isEmpty()) {
                issueMapper.batchInsert(allIssues);
            }

            completeScan(scan, allIssues);

        } catch (Exception e) {
            logger.error("Code quality scan failed for service {}: {}", serviceId, e.getMessage(), e);
            failScan(scan, e.getMessage());
        }
    }

    private List<File> scanSourceFiles(File dir) {
        List<File> files = new ArrayList<>();
        scanSourceFilesRecursive(dir, files);
        return files;
    }

    private void scanSourceFilesRecursive(File dir, List<File> files) {
        File[] children = dir.listFiles();
        if (children == null) return;

        for (File child : children) {
            if (child.isDirectory()) {
                String name = child.getName();
                if (!name.equals("target") && !name.equals("node_modules") && 
                    !name.equals(".git") && !name.equals("build") &&
                    !name.equals("dist") && !name.startsWith(".")) {
                    scanSourceFilesRecursive(child, files);
                }
            } else if (child.isFile()) {
                String name = child.getName().toLowerCase();
                if (name.endsWith(".java") || name.endsWith(".ts") || name.endsWith(".tsx") ||
                    name.endsWith(".js") || name.endsWith(".jsx") || name.endsWith(".py") ||
                    name.endsWith(".go") || name.endsWith(".vue") || name.endsWith(".kt")) {
                    files.add(child);
                }
            }
        }
    }

    private List<CodeQualityIssue> analyzeFile(Long scanId, File file) {
        List<CodeQualityIssue> issues = new ArrayList<>();

        try {
            String content = Files.readString(file.toPath());
            String[] lines = content.split("\n");
            String filePath = file.getPath();

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                int lineNum = i + 1;

                checkSecurityIssues(scanId, filePath, line, lineNum, issues);
                checkReliabilityIssues(scanId, filePath, line, lineNum, issues);
                checkMaintainabilityIssues(scanId, filePath, line, lineNum, issues);
                checkCodeSmellIssues(scanId, filePath, line, lineNum, issues);
            }

        } catch (Exception e) {
            logger.debug("Failed to analyze file {}: {}", file.getPath(), e.getMessage());
        }

        return issues;
    }

    private void checkSecurityIssues(Long scanId, String filePath, String line, int lineNum, List<CodeQualityIssue> issues) {
        String trimmed = line.trim().toLowerCase();

        if (trimmed.contains("password") && (trimmed.contains("=") || trimmed.contains(":"))) {
            if (!trimmed.contains("//") && !trimmed.contains("*")) {
                issues.add(createIssue(scanId, filePath, lineNum, 
                    CodeQualityIssue.CATEGORY_SECURITY, CodeQualityIssue.SEVERITY_CRITICAL,
                    "SEC001", "硬编码密码", 
                    "检测到可能的硬编码密码，建议使用环境变量或配置文件管理敏感信息",
                    line.trim()));
            }
        }

        if (trimmed.contains("sql") && trimmed.contains("+") && !trimmed.contains("//")) {
            issues.add(createIssue(scanId, filePath, lineNum,
                CodeQualityIssue.CATEGORY_SECURITY, CodeQualityIssue.SEVERITY_CRITICAL,
                "SEC002", "SQL注入风险",
                "检测到字符串拼接SQL，建议使用参数化查询",
                line.trim()));
        }

        if (trimmed.contains("exec(") || trimmed.contains("eval(") || trimmed.contains("runtime.exec")) {
            issues.add(createIssue(scanId, filePath, lineNum,
                CodeQualityIssue.CATEGORY_SECURITY, CodeQualityIssue.SEVERITY_MAJOR,
                "SEC003", "命令执行风险",
                "检测到动态命令执行，请确保输入经过严格验证",
                line.trim()));
        }

        if (trimmed.contains("md5") || trimmed.contains("sha1") || trimmed.contains("des")) {
            issues.add(createIssue(scanId, filePath, lineNum,
                CodeQualityIssue.CATEGORY_SECURITY, CodeQualityIssue.SEVERITY_MAJOR,
                "SEC004", "弱加密算法",
                "建议使用更安全的加密算法如SHA-256、AES等",
                line.trim()));
        }
    }

    private void checkReliabilityIssues(Long scanId, String filePath, String line, int lineNum, List<CodeQualityIssue> issues) {
        String trimmed = line.trim().toLowerCase();

        if (trimmed.contains("catch") && trimmed.contains("exception") && !trimmed.contains("//")) {
            if (!linesAroundContain(filePath, lineNum, "log", 3)) {
                issues.add(createIssue(scanId, filePath, lineNum,
                    CodeQualityIssue.CATEGORY_RELIABILITY, CodeQualityIssue.SEVERITY_MAJOR,
                    "REL001", "空catch块",
                    "捕获异常后应进行适当的处理或日志记录",
                    line.trim()));
            }
        }

        if (trimmed.contains(".printstacktrace()")) {
            issues.add(createIssue(scanId, filePath, lineNum,
                CodeQualityIssue.CATEGORY_RELIABILITY, CodeQualityIssue.SEVERITY_MINOR,
                "REL002", "使用printStackTrace",
                "建议使用日志框架替代printStackTrace",
                line.trim()));
        }

        if (trimmed.contains("thread.sleep") && !trimmed.contains("//")) {
            issues.add(createIssue(scanId, filePath, lineNum,
                CodeQualityIssue.CATEGORY_RELIABILITY, CodeQualityIssue.SEVERITY_MINOR,
                "REL003", "线程休眠",
                "注意Thread.sleep可能影响系统响应性",
                line.trim()));
        }
    }

    private void checkMaintainabilityIssues(Long scanId, String filePath, String line, int lineNum, List<CodeQualityIssue> issues) {
        String trimmed = line.trim();

        if (trimmed.length() > 120) {
            issues.add(createIssue(scanId, filePath, lineNum,
                CodeQualityIssue.CATEGORY_MAINTAINABILITY, CodeQualityIssue.SEVERITY_MINOR,
                "MAIN001", "行过长",
                "代码行超过120字符，建议适当换行提高可读性",
                trimmed.substring(0, Math.min(100, trimmed.length())) + "..."));
        }

        if (trimmed.contains("todo") || trimmed.contains("fixme") || trimmed.contains("hack")) {
            issues.add(createIssue(scanId, filePath, lineNum,
                CodeQualityIssue.CATEGORY_MAINTAINABILITY, CodeQualityIssue.SEVERITY_INFO,
                "MAIN002", "待办事项",
                "检测到TODO/FIXME注释",
                line.trim()));
        }

        if (trimmed.contains("@deprecated")) {
            issues.add(createIssue(scanId, filePath, lineNum,
                CodeQualityIssue.CATEGORY_MAINTAINABILITY, CodeQualityIssue.SEVERITY_INFO,
                "MAIN003", "已废弃代码",
                "检测到废弃的代码",
                line.trim()));
        }
    }

    private void checkCodeSmellIssues(Long scanId, String filePath, String line, int lineNum, List<CodeQualityIssue> issues) {
        String trimmed = line.trim();

        if (trimmed.equals("} else {") || trimmed.equals("}else{")) {
            issues.add(createIssue(scanId, filePath, lineNum,
                CodeQualityIssue.CATEGORY_CODE_SMELL, CodeQualityIssue.SEVERITY_INFO,
                "SMELL001", "else语句",
                "考虑使用提前返回来减少else分支",
                line.trim()));
        }

        if (trimmed.contains("system.out.print") || trimmed.contains("console.log")) {
            issues.add(createIssue(scanId, filePath, lineNum,
                CodeQualityIssue.CATEGORY_CODE_SMELL, CodeQualityIssue.SEVERITY_MINOR,
                "SMELL002", "控制台输出",
                "生产代码应使用日志框架而非控制台输出",
                line.trim()));
        }
    }

    private boolean linesAroundContain(String filePath, int lineNum, String keyword, int range) {
        return false;
    }

    private CodeQualityIssue createIssue(Long scanId, String filePath, int line, 
                                         String category, String severity,
                                         String ruleId, String ruleName,
                                         String message, String codeSnippet) {
        CodeQualityIssue issue = new CodeQualityIssue();
        issue.setScanId(scanId);
        issue.setFilePath(filePath);
        issue.setLine(line);
        issue.setColumn(1);
        issue.setCategory(category);
        issue.setSeverity(severity);
        issue.setRuleId(ruleId);
        issue.setRuleName(ruleName);
        issue.setMessage(message);
        issue.setSuggestion(message);
        issue.setCodeSnippet(codeSnippet);
        issue.setStatus("OPEN");
        issue.setCreatedAt(LocalDateTime.now());
        return issue;
    }

    private void updatePhase(CodeQualityScan scan, String phase) {
        scan.setCurrentPhase(phase);
        scan.setProgress(0);
        scan.setCheckedCount(0);
        scanMapper.update(scan);
    }

    private void updateProgress(CodeQualityScan scan, int checked, int total, String currentFile) {
        scan.setCheckedCount(checked);
        scan.setCurrentFile(currentFile.length() > 100 ? "..." + currentFile.substring(currentFile.length() - 97) : currentFile);
        scan.setProgress(calculateProgress(checked, total));
        scanMapper.update(scan);
    }

    private int calculateProgress(int checked, int total) {
        if (total == 0) return 100;
        return (int) ((checked * 100.0) / total);
    }

    private void completeScan(CodeQualityScan scan, List<CodeQualityIssue> issues) {
        int totalIssues = issues.size();
        int securityIssues = 0;
        int reliabilityIssues = 0;
        int maintainabilityIssues = 0;
        int codeSmellIssues = 0;
        int blockerCount = 0;
        int criticalCount = 0;
        int majorCount = 0;
        int minorCount = 0;
        int infoCount = 0;

        for (CodeQualityIssue issue : issues) {
            switch (issue.getCategory()) {
                case CodeQualityIssue.CATEGORY_SECURITY: securityIssues++; break;
                case CodeQualityIssue.CATEGORY_RELIABILITY: reliabilityIssues++; break;
                case CodeQualityIssue.CATEGORY_MAINTAINABILITY: maintainabilityIssues++; break;
                case CodeQualityIssue.CATEGORY_CODE_SMELL: codeSmellIssues++; break;
            }
            switch (issue.getSeverity()) {
                case CodeQualityIssue.SEVERITY_BLOCKER: blockerCount++; break;
                case CodeQualityIssue.SEVERITY_CRITICAL: criticalCount++; break;
                case CodeQualityIssue.SEVERITY_MAJOR: majorCount++; break;
                case CodeQualityIssue.SEVERITY_MINOR: minorCount++; break;
                case CodeQualityIssue.SEVERITY_INFO: infoCount++; break;
            }
        }

        scan.setTotalIssues(totalIssues);
        scan.setSecurityIssues(securityIssues);
        scan.setReliabilityIssues(reliabilityIssues);
        scan.setMaintainabilityIssues(maintainabilityIssues);
        scan.setCodeSmellIssues(codeSmellIssues);
        scan.setBlockerCount(blockerCount);
        scan.setCriticalCount(criticalCount);
        scan.setMajorCount(majorCount);
        scan.setMinorCount(minorCount);
        scan.setInfoCount(infoCount);

        double qualityScore = calculateScore(totalIssues, blockerCount, criticalCount, majorCount, minorCount);
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

        logger.info("Code quality scan completed: {} issues found", totalIssues);
    }

    private double calculateScore(int total, int blocker, int critical, int major, int minor) {
        if (total == 0) return 100;
        double penalty = blocker * 20 + critical * 10 + major * 5 + minor * 1;
        return Math.max(0, 100 - penalty);
    }

    private void failScan(CodeQualityScan scan, String errorMessage) {
        scan.setStatus("FAILED");
        scan.setCompletedAt(LocalDateTime.now());
        scan.setCurrentPhase("扫描失败: " + errorMessage);
        scanMapper.update(scan);
    }
}
