package com.jettech.code.service;

import com.jettech.code.dto.VulnerabilityInfo;
import com.jettech.code.entity.Dependency;
import com.jettech.code.entity.SecurityScan;
import com.jettech.code.entity.Vulnerability;
import com.jettech.code.mapper.DependencyMapper;
import com.jettech.code.mapper.SecurityScanMapper;
import com.jettech.code.mapper.VulnerabilityMapper;
import com.jettech.code.parser.DependencyParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AsyncScanService {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncScanService.class);
    
    private final DependencyMapper dependencyMapper;
    private final VulnerabilityMapper vulnerabilityMapper;
    private final SecurityScanMapper securityScanMapper;
    private final List<DependencyParser> dependencyParsers;
    private final LicenseDetector licenseDetector;
    private final MultiSourceVulnerabilityService multiSourceService;
    
    private static final Map<String, String> ECOSYSTEM_MAPPING = Map.of(
        "maven", "Maven",
        "npm", "npm",
        "pypi", "PyPI",
        "golang", "Go"
    );

    public AsyncScanService(DependencyMapper dependencyMapper,
                           VulnerabilityMapper vulnerabilityMapper,
                           SecurityScanMapper securityScanMapper,
                           List<DependencyParser> dependencyParsers,
                           LicenseDetector licenseDetector,
                           MultiSourceVulnerabilityService multiSourceService) {
        this.dependencyMapper = dependencyMapper;
        this.vulnerabilityMapper = vulnerabilityMapper;
        this.securityScanMapper = securityScanMapper;
        this.dependencyParsers = dependencyParsers;
        this.licenseDetector = licenseDetector;
        this.multiSourceService = multiSourceService;
    }
    
    @Async
    public void executeScanAsync(Long scanId, Long serviceId, String localPath) {
        SecurityScan scan = securityScanMapper.findById(scanId);
        if (scan == null) {
            logger.error("Scan not found: {}", scanId);
            return;
        }
        
        try {
            updatePhase(scan, "正在解析依赖...");
            List<Dependency> dependencies = generateSBOM(serviceId, localPath);
            scan.setTotalDependencies(dependencies.size());
            securityScanMapper.update(scan);
            
            if (dependencies.isEmpty()) {
                completeScan(scan, 0, 0, 0, 0, 0, 0);
                return;
            }
            
            updatePhase(scan, "正在检查漏洞...");
            
            int vulnerableCount = 0;
            int criticalCount = 0;
            int highCount = 0;
            int mediumCount = 0;
            int lowCount = 0;
            int licenseViolationCount = 0;
            int checkedCount = 0;
            
            for (Dependency dep : dependencies) {
                try {
                    updateProgress(scan, checkedCount, dependencies.size(), dep.getName());
                    
                    vulnerabilityMapper.deleteByDependencyId(dep.getId());
                    
                    String ecosystem = mapEcosystem(dep.getType());
                    if (ecosystem != null && isValidVersion(dep.getVersion())) {
                        List<Vulnerability> vulns = checkSingleDependency(dep, ecosystem);
                        
                        if (!vulns.isEmpty()) {
                            vulnerableCount++;
                            for (Vulnerability v : vulns) {
                                switch (v.getSeverity()) {
                                    case "CRITICAL": criticalCount++; break;
                                    case "HIGH": highCount++; break;
                                    case "MEDIUM": mediumCount++; break;
                                    case "LOW": lowCount++; break;
                                }
                            }
                        }
                    }
                    
                    if ("VIOLATION".equals(dep.getLicenseStatus())) {
                        licenseViolationCount++;
                    }
                    
                    checkedCount++;
                    scan.setCheckedCount(checkedCount);
                    scan.setProgress(calculateProgress(checkedCount, dependencies.size()));
                    securityScanMapper.update(scan);
                    
                    TimeUnit.MILLISECONDS.sleep(100);
                    
                } catch (Exception e) {
                    logger.error("Failed to check dependency {}@{}: {}", 
                        dep.getName(), dep.getVersion(), e.getMessage());
                    checkedCount++;
                    scan.setCheckedCount(checkedCount);
                    scan.setProgress(calculateProgress(checkedCount, dependencies.size()));
                    securityScanMapper.update(scan);
                }
            }
            
            completeScan(scan, vulnerableCount, criticalCount, highCount, mediumCount, lowCount, licenseViolationCount);
            
        } catch (Exception e) {
            logger.error("Scan failed for service {}: {}", serviceId, e.getMessage(), e);
            failScan(scan, e.getMessage());
        }
    }
    
    private List<Dependency> generateSBOM(Long serviceId, String localPath) throws Exception {
        dependencyMapper.deleteByServiceId(serviceId);
        List<Dependency> allDependencies = new ArrayList<>();
        
        File projectDir = new File(localPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            throw new IllegalArgumentException("Project directory not found: " + localPath);
        }
        
        for (DependencyParser parser : dependencyParsers) {
            if (parser.supports(projectDir)) {
                try {
                    logger.info("Parsing dependencies with {} parser", parser.getEcosystem());
                    List<Dependency> dependencies = parser.parse(serviceId, projectDir);
                    
                    for (Dependency dep : dependencies) {
                        licenseDetector.detectLicense(dep, localPath);
                    }
                    
                    allDependencies.addAll(dependencies);
                    logger.info("Found {} dependencies with {} parser", dependencies.size(), parser.getEcosystem());
                } catch (Exception e) {
                    logger.error("Failed to parse dependencies with {} parser: {}", 
                                parser.getEcosystem(), e.getMessage());
                }
            }
        }
        
        if (!allDependencies.isEmpty()) {
            dependencyMapper.batchInsert(allDependencies);
            List<Dependency> savedDeps = dependencyMapper.findByServiceId(serviceId);
            logger.info("Saved {} dependencies with IDs", savedDeps.size());
            return savedDeps;
        }
        
        return allDependencies;
    }
    
    private List<Vulnerability> checkSingleDependency(Dependency dep, String ecosystem) {
        try {
            List<VulnerabilityInfo> vulnInfos = multiSourceService.queryAllSources(
                dep.getName(), ecosystem, dep.getVersion()
            );
            
            List<Vulnerability> vulnerabilities = new ArrayList<>();
            for (VulnerabilityInfo info : vulnInfos) {
                Vulnerability vuln = convertToVulnerability(dep.getId(), info);
                if (vuln != null) {
                    vulnerabilities.add(vuln);
                }
            }
            
            if (!vulnerabilities.isEmpty()) {
                vulnerabilityMapper.batchInsert(vulnerabilities);
            }
            
            return vulnerabilities;
        } catch (Exception e) {
            logger.error("Failed to check vulnerabilities for {}@{}: {}", 
                dep.getName(), dep.getVersion(), e.getMessage());
            return List.of();
        }
    }
    
    private Vulnerability convertToVulnerability(Long dependencyId, VulnerabilityInfo info) {
        if (info == null || info.getCveId() == null) {
            return null;
        }
        
        Vulnerability vuln = new Vulnerability();
        vuln.setDependencyId(dependencyId);
        vuln.setCveId(info.getCveId());
        vuln.setTitle(info.getTitle() != null ? info.getTitle() : info.getCveId());
        vuln.setDescription(info.getDescription());
        vuln.setSeverity(info.getSeverity() != null ? info.getSeverity() : "UNKNOWN");
        vuln.setCvssScore(info.getCvssScore() != null ? info.getCvssScore() : estimateCvssScore(info.getSeverity()));
        vuln.setAffectedVersion(info.getAffectedVersion() != null ? info.getAffectedVersion() : "unknown");
        vuln.setFixedVersion(info.getFixedVersion() != null ? info.getFixedVersion() : "unknown");
        vuln.setReferences(info.getReferences());
        vuln.setStatus("OPEN");
        vuln.setCreatedAt(LocalDateTime.now());
        
        return vuln;
    }
    
    private void updatePhase(SecurityScan scan, String phase) {
        scan.setCurrentPhase(phase);
        scan.setProgress(0);
        scan.setCheckedCount(0);
        securityScanMapper.update(scan);
    }
    
    private void updateProgress(SecurityScan scan, int checked, int total, String currentDep) {
        scan.setCheckedCount(checked);
        scan.setCurrentDependency(currentDep);
        scan.setProgress(calculateProgress(checked, total));
        securityScanMapper.update(scan);
    }
    
    private int calculateProgress(int checked, int total) {
        if (total == 0) return 100;
        return (int) ((checked * 100.0) / total);
    }
    
    private void completeScan(SecurityScan scan, int vulnerableCount, int criticalCount, 
                             int highCount, int mediumCount, int lowCount, int licenseViolationCount) {
        scan.setVulnerableDependencies(vulnerableCount);
        scan.setCriticalCount(criticalCount);
        scan.setHighCount(highCount);
        scan.setMediumCount(mediumCount);
        scan.setLowCount(lowCount);
        scan.setLicenseViolationCount(licenseViolationCount);
        scan.setMalwareCount(0);
        scan.setStatus("COMPLETED");
        scan.setProgress(100);
        scan.setCompletedAt(LocalDateTime.now());
        scan.setCurrentPhase("扫描完成");
        scan.setCurrentDependency(null);
        securityScanMapper.update(scan);
        logger.info("Scan completed: {}", scan.getId());
    }
    
    private void failScan(SecurityScan scan, String errorMessage) {
        scan.setStatus("FAILED");
        scan.setCompletedAt(LocalDateTime.now());
        scan.setCurrentPhase("扫描失败: " + errorMessage);
        securityScanMapper.update(scan);
    }
    
    private String mapEcosystem(String type) {
        if (type == null) return null;
        return ECOSYSTEM_MAPPING.get(type.toLowerCase());
    }
    
    private boolean isValidVersion(String version) {
        if (version == null) return false;
        String lower = version.toLowerCase();
        return !lower.equals("managed") && 
               !lower.equals("unknown") && 
               !lower.equals("latest") &&
               !lower.isEmpty();
    }
    
    private double estimateCvssScore(String severity) {
        if (severity == null) return 5.0;
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> 9.5;
            case "HIGH" -> 7.5;
            case "MEDIUM" -> 5.0;
            case "LOW" -> 2.5;
            default -> 5.0;
        };
    }
}
