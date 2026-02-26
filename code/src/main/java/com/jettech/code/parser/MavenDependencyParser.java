package com.jettech.code.parser;

import com.jettech.code.entity.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class MavenDependencyParser implements DependencyParser {
    
    private static final Logger logger = LoggerFactory.getLogger(MavenDependencyParser.class);

    @Override
    public boolean supports(File projectDir) {
        boolean supported = new File(projectDir, "pom.xml").exists();
        logger.debug("Maven parser supports {}: {}", projectDir.getPath(), supported);
        return supported;
    }

    @Override
    public List<Dependency> parse(Long serviceId, File projectDir) throws Exception {
        List<Dependency> dependencies = new ArrayList<>();
        Map<String, String> managedVersions = new HashMap<>();
        Map<String, String> properties = new HashMap<>();
        
        parsePomFile(serviceId, projectDir, new File(projectDir, "pom.xml"), 
            managedVersions, properties, dependencies, new HashSet<>());
        
        logger.info("Total parsed {} dependencies from project", dependencies.size());
        
        return dependencies;
    }
    
    private void parsePomFile(Long serviceId, File projectDir, File pomFile, 
                              Map<String, String> managedVersions,
                              Map<String, String> properties,
                              List<Dependency> dependencies,
                              Set<String> parsedModules) throws Exception {
        
        if (!pomFile.exists()) {
            logger.debug("pom.xml not found: {}", pomFile.getAbsolutePath());
            return;
        }
        
        logger.info("Parsing pom.xml: {}", pomFile.getAbsolutePath());
        
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader(pomFile));
        
        logger.info("Model groupId: {}, artifactId: {}, packaging: {}", 
            model.getGroupId(), model.getArtifactId(), model.getPackaging());
        
        if (model.getProperties() != null) {
            for (Map.Entry<Object, Object> entry : model.getProperties().entrySet()) {
                String key = String.valueOf(entry.getKey());
                String value = String.valueOf(entry.getValue());
                properties.put(key, value);
            }
            logger.debug("Loaded {} properties", model.getProperties().size());
        }
        
        if (model.getDependencyManagement() != null) {
            List<org.apache.maven.model.Dependency> managedDeps = model.getDependencyManagement().getDependencies();
            logger.debug("Found {} managed dependencies", managedDeps != null ? managedDeps.size() : 0);
            if (managedDeps != null) {
                for (org.apache.maven.model.Dependency dep : managedDeps) {
                    String groupId = resolveProperty(dep.getGroupId(), properties);
                    String artifactId = resolveProperty(dep.getArtifactId(), properties);
                    if (groupId != null && artifactId != null) {
                        String key = groupId + ":" + artifactId;
                        String version = resolveProperty(dep.getVersion(), properties);
                        if (version != null && !version.startsWith("${")) {
                            managedVersions.put(key, version);
                        }
                    }
                }
            }
        }
        
        List<org.apache.maven.model.Dependency> modelDeps = model.getDependencies();
        int depCount = modelDeps != null ? modelDeps.size() : 0;
        logger.info("Found {} direct dependencies in {}", depCount, pomFile.getName());
        
        if (modelDeps != null) {
            for (org.apache.maven.model.Dependency mavenDep : modelDeps) {
                Dependency dep = createDependency(serviceId, mavenDep, managedVersions, properties);
                if (dep != null) {
                    String depKey = dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion();
                    boolean exists = dependencies.stream()
                        .anyMatch(d -> (d.getGroupId() + ":" + d.getArtifactId() + ":" + d.getVersion()).equals(depKey));
                    if (!exists) {
                        dependencies.add(dep);
                    }
                }
            }
        }
        
        List<String> modules = model.getModules();
        if (modules != null && !modules.isEmpty()) {
            logger.info("Found {} sub-modules: {}", modules.size(), modules);
            for (String moduleName : modules) {
                if (parsedModules.contains(moduleName)) {
                    continue;
                }
                parsedModules.add(moduleName);
                
                File moduleDir = new File(pomFile.getParentFile(), moduleName);
                File modulePom = new File(moduleDir, "pom.xml");
                
                logger.info("Parsing sub-module: {}", moduleName);
                parsePomFile(serviceId, projectDir, modulePom, managedVersions, properties, dependencies, parsedModules);
            }
        }
    }
    
    private String resolveProperty(String value, Map<String, String> properties) {
        if (value == null) {
            return null;
        }
        
        if (value.startsWith("${") && value.endsWith("}")) {
            String key = value.substring(2, value.length() - 1);
            String resolved = properties.get(key);
            if (resolved != null) {
                return resolved;
            }
            return value;
        }
        
        return value;
    }

    private Dependency createDependency(Long serviceId, org.apache.maven.model.Dependency mavenDep, 
                                        Map<String, String> managedVersions,
                                        Map<String, String> properties) {
        String groupId = resolveProperty(mavenDep.getGroupId(), properties);
        String artifactId = resolveProperty(mavenDep.getArtifactId(), properties);
        String version = resolveProperty(mavenDep.getVersion(), properties);
        
        if (groupId == null || artifactId == null || groupId.startsWith("${") || artifactId.startsWith("${")) {
            return null;
        }
        
        if (version == null || version.startsWith("${")) {
            String key = groupId + ":" + artifactId;
            version = managedVersions.getOrDefault(key, "managed");
        }
        
        Dependency dep = new Dependency();
        dep.setServiceId(serviceId);
        dep.setGroupId(groupId);
        dep.setArtifactId(artifactId);
        dep.setVersion(version != null ? version : "unknown");
        dep.setName(groupId + ":" + artifactId);
        dep.setType("maven");
        dep.setScope(mavenDep.getScope());
        dep.setPurl("pkg:maven/" + groupId + "/" + artifactId + "@" + dep.getVersion());
        dep.setCreatedAt(LocalDateTime.now());
        
        return dep;
    }

    @Override
    public String getEcosystem() {
        return "Maven";
    }
}
