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
    private final AsyncScanService asyncScanService;

    public SupplyChainService(DependencyMapper dependencyMapper, 
                             VulnerabilityMapper vulnerabilityMapper,
                             SecurityScanMapper securityScanMapper,
                             ServiceMapper serviceMapper,
                             List<DependencyParser> dependencyParsers,
                             LicenseDetector licenseDetector,
                             VulnerabilityChecker vulnerabilityChecker,
                             AsyncScanService asyncScanService) {
        this.dependencyMapper = dependencyMapper;
        this.vulnerabilityMapper = vulnerabilityMapper;
        this.securityScanMapper = securityScanMapper;
        this.serviceMapper = serviceMapper;
        this.dependencyParsers = dependencyParsers;
        this.licenseDetector = licenseDetector;
        this.vulnerabilityChecker = vulnerabilityChecker;
        this.asyncScanService = asyncScanService;
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
    
    public SecurityScan getScanById(Long scanId) {
        return securityScanMapper.findById(scanId);
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

    public SecurityScan startSecurityScan(Long serviceId) throws Exception {
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
        scan.setCheckedCount(0);
        scan.setProgress(0);
        scan.setCurrentPhase("正在初始化...");
        securityScanMapper.insert(scan);

        logger.info("Starting async security scan {} for service {}", scan.getId(), serviceId);
        
        asyncScanService.executeScanAsync(scan.getId(), serviceId, localPath);

        return scan;
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
}
