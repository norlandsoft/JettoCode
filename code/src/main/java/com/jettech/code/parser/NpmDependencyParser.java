package com.jettech.code.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jettech.code.entity.Dependency;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class NpmDependencyParser implements DependencyParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(File projectDir) {
        return new File(projectDir, "package.json").exists();
    }

    @Override
    public List<Dependency> parse(Long serviceId, File projectDir) throws Exception {
        List<Dependency> dependencies = new ArrayList<>();
        
        File packageLockFile = new File(projectDir, "package-lock.json");
        if (packageLockFile.exists()) {
            dependencies.addAll(parsePackageLock(serviceId, packageLockFile));
        } else {
            File packageJsonFile = new File(projectDir, "package.json");
            dependencies.addAll(parsePackageJson(serviceId, packageJsonFile));
        }
        
        return dependencies;
    }

    private List<Dependency> parsePackageLock(Long serviceId, File lockFile) throws Exception {
        List<Dependency> dependencies = new ArrayList<>();
        
        JsonNode root = objectMapper.readTree(lockFile);
        JsonNode packages = root.get("packages");
        
        if (packages != null) {
            Iterator<Map.Entry<String, JsonNode>> fields = packages.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String packagePath = entry.getKey();
                
                if (packagePath.startsWith("node_modules/")) {
                    String name = packagePath.substring("node_modules/".length());
                    JsonNode pkgNode = entry.getValue();
                    
                    String version = pkgNode.has("version") ? pkgNode.get("version").asText() : "unknown";
                    boolean dev = pkgNode.has("dev") && pkgNode.get("dev").asBoolean();
                    
                    Dependency dep = new Dependency();
                    dep.setServiceId(serviceId);
                    dep.setName(name);
                    dep.setVersion(version);
                    dep.setType("npm");
                    dep.setScope(dev ? "dev" : "runtime");
                    dep.setPurl("pkg:npm/" + name + "@" + version);
                    dep.setCreatedAt(LocalDateTime.now());
                    
                    if (pkgNode.has("license")) {
                        JsonNode licenseNode = pkgNode.get("license");
                        String license = licenseNode.isTextual() ? licenseNode.asText() : 
                                        licenseNode.isArray() ? licenseNode.get(0).asText() : "Unknown";
                        dep.setLicense(license);
                    }
                    
                    dependencies.add(dep);
                }
            }
        }
        
        return dependencies;
    }

    private List<Dependency> parsePackageJson(Long serviceId, File packageJsonFile) throws Exception {
        List<Dependency> dependencies = new ArrayList<>();
        
        JsonNode root = objectMapper.readTree(packageJsonFile);
        
        parseDependencySection(root, "dependencies", serviceId, "runtime", dependencies);
        parseDependencySection(root, "devDependencies", serviceId, "dev", dependencies);
        
        return dependencies;
    }

    private void parseDependencySection(JsonNode root, String section, Long serviceId, 
                                        String scope, List<Dependency> dependencies) {
        JsonNode sectionNode = root.get(section);
        if (sectionNode == null) return;
        
        Iterator<Map.Entry<String, JsonNode>> fields = sectionNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String name = entry.getKey();
            String version = entry.getValue().asText();
            
            Dependency dep = new Dependency();
            dep.setServiceId(serviceId);
            dep.setName(name);
            dep.setVersion(version);
            dep.setType("npm");
            dep.setScope(scope);
            dep.setPurl("pkg:npm/" + name + "@" + version);
            dep.setCreatedAt(LocalDateTime.now());
            
            dependencies.add(dep);
        }
    }

    @Override
    public String getEcosystem() {
        return "npm";
    }
}
