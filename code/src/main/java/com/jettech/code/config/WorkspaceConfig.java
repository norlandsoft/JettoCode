package com.jettech.code.config;

import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WorkspaceConfig {
    
    private static final String DEFAULT_WORKSPACE_PATH = ".jetto-code/workspace";
    
    private final Path workspacePath;
    private final Path reposPath;
    
    public WorkspaceConfig() {
        String envWorkspace = System.getenv("JETTO_CODE_WORKSPACE");
        String workspaceDir;
        if (envWorkspace != null && !envWorkspace.isEmpty()) {
            workspaceDir = envWorkspace;
        } else {
            workspaceDir = System.getProperty("user.home") + "/" + DEFAULT_WORKSPACE_PATH;
        }
        
        this.workspacePath = Paths.get(workspaceDir).toAbsolutePath().normalize();
        this.reposPath = this.workspacePath.resolve("repos");
        
        ensureDirectoriesExist();
    }
    
    private void ensureDirectoriesExist() {
        try {
            java.nio.file.Files.createDirectories(workspacePath);
            java.nio.file.Files.createDirectories(reposPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create workspace directories: " + e.getMessage(), e);
        }
    }
    
    public Path getWorkspacePath() {
        return workspacePath;
    }
    
    public Path getReposPath() {
        return reposPath;
    }
    
    public String getReposPathString() {
        return reposPath.toString();
    }
}
