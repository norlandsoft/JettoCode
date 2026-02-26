package com.jettech.code.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jettech.code.entity.Dependency;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PythonDependencyParser implements DependencyParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(File projectDir) {
        return new File(projectDir, "requirements.txt").exists() ||
               new File(projectDir, "pyproject.toml").exists() ||
               new File(projectDir, "Pipfile").exists();
    }

    @Override
    public List<Dependency> parse(Long serviceId, File projectDir) throws Exception {
        List<Dependency> dependencies = new ArrayList<>();
        
        File poetryLock = new File(projectDir, "poetry.lock");
        if (poetryLock.exists()) {
            dependencies.addAll(parsePoetryLock(serviceId, poetryLock));
        }
        
        File pipfileLock = new File(projectDir, "Pipfile.lock");
        if (pipfileLock.exists()) {
            dependencies.addAll(parsePipfileLock(serviceId, pipfileLock));
        }
        
        if (dependencies.isEmpty()) {
            File requirementsFile = new File(projectDir, "requirements.txt");
            if (requirementsFile.exists()) {
                dependencies.addAll(parseRequirementsTxt(serviceId, requirementsFile));
            }
        }
        
        return dependencies;
    }

    private List<Dependency> parsePoetryLock(Long serviceId, File lockFile) throws Exception {
        List<Dependency> dependencies = new ArrayList<>();
        
        String content = Files.readString(lockFile.toPath());
        Pattern pattern = Pattern.compile(
            "\\[package\\]\\s*name\\s*=\\s*\"([^\"]+)\"\\s*version\\s*=\\s*\"([^\"]+)\""
        );
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String name = matcher.group(1);
            String version = matcher.group(2);
            
            Dependency dep = new Dependency();
            dep.setServiceId(serviceId);
            dep.setName(name);
            dep.setVersion(version);
            dep.setType("pypi");
            dep.setPurl("pkg:pypi/" + name + "@" + version);
            dep.setCreatedAt(LocalDateTime.now());
            
            dependencies.add(dep);
        }
        
        return dependencies;
    }

    private List<Dependency> parsePipfileLock(Long serviceId, File lockFile) throws Exception {
        List<Dependency> dependencies = new ArrayList<>();
        
        JsonNode root = objectMapper.readTree(lockFile);
        
        parsePipfileSection(root, "default", serviceId, dependencies);
        parsePipfileSection(root, "develop", serviceId, dependencies);
        
        return dependencies;
    }

    private void parsePipfileSection(JsonNode root, String section, Long serviceId, 
                                     List<Dependency> dependencies) {
        JsonNode sectionNode = root.get(section);
        if (sectionNode == null) return;
        
        sectionNode.fields().forEachRemaining(entry -> {
            String name = entry.getKey();
            JsonNode versionNode = entry.getValue();
            String version = "unknown";
            
            if (versionNode.isTextual()) {
                String versionStr = versionNode.asText();
                Pattern p = Pattern.compile("\\d+\\.\\d+(\\.\\d+)?");
                Matcher m = p.matcher(versionStr);
                if (m.find()) {
                    version = m.group();
                }
            } else if (versionNode.isObject() && versionNode.has("version")) {
                version = versionNode.get("version").asText().replace("=", "").trim();
            }
            
            Dependency dep = new Dependency();
            dep.setServiceId(serviceId);
            dep.setName(name);
            dep.setVersion(version);
            dep.setType("pypi");
            dep.setScope("default".equals(section) ? "runtime" : "dev");
            dep.setPurl("pkg:pypi/" + name + "@" + version);
            dep.setCreatedAt(LocalDateTime.now());
            
            dependencies.add(dep);
        });
    }

    private List<Dependency> parseRequirementsTxt(Long serviceId, File requirementsFile) throws Exception {
        List<Dependency> dependencies = new ArrayList<>();
        List<String> lines = Files.readAllLines(requirementsFile.toPath());

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("-")) {
                continue;
            }

            String[] parts = line.split("==|>=|<=|>|<|~=|!=");
            if (parts.length >= 1) {
                String name = parts[0].trim();
                String version = parts.length > 1 ? parts[1].trim().split(",")[0] : "latest";
                
                name = name.replaceAll("\\[.*\\]", "");
                
                Dependency dep = new Dependency();
                dep.setServiceId(serviceId);
                dep.setName(name);
                dep.setVersion(version);
                dep.setType("pypi");
                dep.setPurl("pkg:pypi/" + name + "@" + version);
                dep.setCreatedAt(LocalDateTime.now());
                
                dependencies.add(dep);
            }
        }

        return dependencies;
    }

    @Override
    public String getEcosystem() {
        return "PyPI";
    }
}
