package com.jettech.code.service;

import com.jettech.code.entity.Dependency;
import com.jettech.code.entity.SecurityScan;
import com.jettech.code.entity.Vulnerability;
import com.jettech.code.mapper.DependencyMapper;
import com.jettech.code.mapper.SecurityScanMapper;
import com.jettech.code.mapper.VulnerabilityMapper;
import com.jettech.code.mapper.ServiceMapper;
import com.jettech.code.parser.DependencyParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SupplyChainService {
    
    private static final Logger logger = LoggerFactory.getLogger(SupplyChainService.class);
    
    private final DependencyMapper dependencyMapper;
    private final VulnerabilityMapper vulnerabilityMapper;
    private final SecurityScanMapper securityScanMapper;
    private final ServiceMapper serviceMapper;
    private final List<DependencyParser> dependencyParsers;
    private final LicenseDetector licenseDetector;
    private final VulnerabilityChecker vulnerabilityChecker;

    public SupplyChainService(DependencyMapper dependencyMapper, 
                             VulnerabilityMapper vulnerabilityMapper,
                             SecurityScanMapper securityScanMapper,
                             ServiceMapper serviceMapper,
                             List<DependencyParser> dependencyParsers,
                             LicenseDetector licenseDetector,
                             VulnerabilityChecker vulnerabilityChecker) {
        this.dependencyMapper = dependencyMapper;
        this.vulnerabilityMapper = vulnerabilityMapper;
        this.securityScanMapper = securityScanMapper;
        this.serviceMapper = serviceMapper;
        this.dependencyParsers = dependencyParsers;
        this.licenseDetector = licenseDetector;
        this.vulnerabilityChecker = vulnerabilityChecker;
    }

    public List<Dependency> getDependencies(Long serviceId) {
        return dependencyMapper.findByServiceId(serviceId);
    }

    public Dependency getDependencyById(Long id) {
        return dependencyMapper.findById(id);
    }

    public List<SecurityScan> getScans(Long serviceId) {
        return securityScanMapper.findByServiceId(serviceId);
    }

    public SecurityScan getLatestScan(Long serviceId) {
        return securityScanMapper.findLatestByServiceId(serviceId);
    }

    public List<Dependency> getDependenciesByApplication(Long applicationId) {
        List<Dependency> dependencies = dependencyMapper.findByApplicationId(applicationId);
        for (Dependency dep : dependencies) {
            int count = vulnerabilityMapper.countByDependencyId(dep.getId());
            dep.setVulnerabilityCount(count);
        }
        return dependencies;
    }

    public SecurityScan getLatestScanByApplication(Long applicationId) {
        return securityScanMapper.findLatestByApplicationId(applicationId);
    }

    public List<Vulnerability> getVulnerabilities(Long dependencyId) {
        return vulnerabilityMapper.findByDependencyId(dependencyId);
    }

    @Transactional
    public List<Dependency> parseDependencies(Long serviceId) throws Exception {
        com.jettech.code.entity.ServiceEntity service = serviceMapper.findById(serviceId);
        if (service == null) {
            throw new IllegalArgumentException("Service not found");
        }

        String localPath = service.getLocalPath();
        if (localPath == null || localPath.isEmpty()) {
            throw new IllegalArgumentException("Service local path not configured");
        }

        logger.info("Parsing dependencies for service {} at {}", serviceId, localPath);
        
        List<Dependency> dependencies = generateSBOM(serviceId, localPath);
        
        logger.info("Parsed {} dependencies for service {}", dependencies.size(), serviceId);
        
        return dependencies;
    }

    @Transactional
    public SecurityScan performSecurityScan(Long serviceId) throws Exception {
        com.jettech.code.entity.ServiceEntity service = serviceMapper.findById(serviceId);
        if (service == null) {
            throw new IllegalArgumentException("Service not found");
        }

        String localPath = service.getLocalPath();
        if (localPath == null || localPath.isEmpty()) {
            throw new IllegalArgumentException("Service local path not configured");
        }

        SecurityScan scan = new SecurityScan();
        scan.setServiceId(serviceId);
        scan.setScanType("FULL");
        scan.setStatus("IN_PROGRESS");
        scan.setStartedAt(LocalDateTime.now());
        scan.setCreatedAt(LocalDateTime.now());
        securityScanMapper.insert(scan);

        try {
            List<Dependency> dependencies = generateSBOM(serviceId, localPath);
            scan.setTotalDependencies(dependencies.size());

            int vulnerableCount = 0;
            int criticalCount = 0;
            int highCount = 0;
            int mediumCount = 0;
            int lowCount = 0;
            int licenseViolationCount = 0;

            Map<Long, List<Vulnerability>> vulnResults = vulnerabilityChecker.batchCheckVulnerabilities(dependencies);

            for (Dependency dep : dependencies) {
                List<Vulnerability> vulns = vulnResults.getOrDefault(dep.getId(), List.of());
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

                if ("VIOLATION".equals(dep.getLicenseStatus())) {
                    licenseViolationCount++;
                }
            }

            scan.setVulnerableDependencies(vulnerableCount);
            scan.setCriticalCount(criticalCount);
            scan.setHighCount(highCount);
            scan.setMediumCount(mediumCount);
            scan.setLowCount(lowCount);
            scan.setLicenseViolationCount(licenseViolationCount);
            scan.setMalwareCount(0);
            scan.setStatus("COMPLETED");
            scan.setCompletedAt(LocalDateTime.now());
            securityScanMapper.update(scan);

            return scan;
        } catch (Exception e) {
            logger.error("Security scan failed for service {}", serviceId, e);
            scan.setStatus("FAILED");
            scan.setCompletedAt(LocalDateTime.now());
            securityScanMapper.update(scan);
            throw e;
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
        }

        return allDependencies;
    }
}
