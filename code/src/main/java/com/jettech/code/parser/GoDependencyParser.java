package com.jettech.code.parser;

import com.jettech.code.entity.Dependency;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GoDependencyParser implements DependencyParser {

    @Override
    public boolean supports(File projectDir) {
        return new File(projectDir, "go.mod").exists();
    }

    @Override
    public List<Dependency> parse(Long serviceId, File projectDir) throws Exception {
        List<Dependency> dependencies = new ArrayList<>();
        
        File goSumFile = new File(projectDir, "go.sum");
        if (goSumFile.exists()) {
            dependencies.addAll(parseGoSum(serviceId, goSumFile));
        } else {
            File goModFile = new File(projectDir, "go.mod");
            dependencies.addAll(parseGoMod(serviceId, goModFile));
        }
        
        return dependencies;
    }

    private List<Dependency> parseGoSum(Long serviceId, File goSumFile) throws Exception {
        List<Dependency> dependencies = new ArrayList<>();
        Set<String> seenPackages = new HashSet<>();
        List<String> lines = Files.readAllLines(goSumFile.toPath());

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\s+");
            if (parts.length >= 2) {
                String moduleName = parts[0];
                String version = parts[1];
                
                if (version.endsWith("/go.mod")) {
                    version = version.substring(0, version.length() - 7);
                }
                
                if (version.startsWith("v") || Character.isDigit(version.charAt(0))) {
                    String key = moduleName + "@" + version;
                    if (seenPackages.contains(key)) {
                        continue;
                    }
                    seenPackages.add(key);
                    
                    Dependency dep = new Dependency();
                    dep.setServiceId(serviceId);
                    dep.setName(moduleName);
                    dep.setVersion(version);
                    dep.setType("golang");
                    dep.setPurl("pkg:golang/" + moduleName + "@" + version);
                    dep.setCreatedAt(LocalDateTime.now());
                    
                    dependencies.add(dep);
                }
            }
        }

        return dependencies;
    }

    private List<Dependency> parseGoMod(Long serviceId, File goModFile) throws Exception {
        List<Dependency> dependencies = new ArrayList<>();
        List<String> lines = Files.readAllLines(goModFile.toPath());
        
        boolean inRequireBlock = false;

        for (String line : lines) {
            line = line.trim();
            
            if (line.startsWith("require (")) {
                inRequireBlock = true;
                continue;
            }
            
            if (inRequireBlock && line.equals(")")) {
                inRequireBlock = false;
                continue;
            }
            
            if (inRequireBlock || line.startsWith("require ")) {
                if (line.startsWith("require ")) {
                    line = line.substring(8).trim();
                }
                
                String[] parts = line.split("\\s+");
                if (parts.length >= 2 && !line.startsWith("module") && !line.startsWith("go ")) {
                    String moduleName = parts[0];
                    String version = parts[1];
                    
                    Dependency dep = new Dependency();
                    dep.setServiceId(serviceId);
                    dep.setName(moduleName);
                    dep.setVersion(version);
                    dep.setType("golang");
                    dep.setPurl("pkg:golang/" + moduleName + "@" + version);
                    dep.setCreatedAt(LocalDateTime.now());
                    
                    dependencies.add(dep);
                }
            }
        }

        return dependencies;
    }

    @Override
    public String getEcosystem() {
        return "Go";
    }
}
