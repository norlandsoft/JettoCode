package com.jettech.code.parser;

import com.jettech.code.entity.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class MavenDependencyParser implements DependencyParser {

    @Override
    public boolean supports(File projectDir) {
        return new File(projectDir, "pom.xml").exists();
    }

    @Override
    public List<Dependency> parse(Long serviceId, File projectDir) throws Exception {
        List<Dependency> dependencies = new ArrayList<>();
        File pomFile = new File(projectDir, "pom.xml");
        
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader(pomFile));
        
        Map<String, String> managedVersions = new HashMap<>();
        if (model.getDependencyManagement() != null) {
            for (org.apache.maven.model.Dependency dep : model.getDependencyManagement().getDependencies()) {
                String key = dep.getGroupId() + ":" + dep.getArtifactId();
                if (dep.getVersion() != null) {
                    managedVersions.put(key, dep.getVersion());
                }
            }
        }
        
        if (model.getDependencies() != null) {
            for (org.apache.maven.model.Dependency mavenDep : model.getDependencies()) {
                Dependency dep = createDependency(serviceId, mavenDep, managedVersions);
                if (dep != null) {
                    dependencies.add(dep);
                }
            }
        }
        
        return dependencies;
    }

    private Dependency createDependency(Long serviceId, org.apache.maven.model.Dependency mavenDep, 
                                        Map<String, String> managedVersions) {
        String groupId = mavenDep.getGroupId();
        String artifactId = mavenDep.getArtifactId();
        String version = mavenDep.getVersion();
        
        if (version == null) {
            String key = groupId + ":" + artifactId;
            version = managedVersions.getOrDefault(key, "managed");
        }
        
        if (groupId == null || artifactId == null) {
            return null;
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
