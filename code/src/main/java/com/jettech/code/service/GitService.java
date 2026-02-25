package com.jettech.code.service;

import com.jettech.code.config.WorkspaceConfig;
import com.jettech.code.dto.CloneRequest;
import com.jettech.code.entity.Project;
import com.jettech.code.entity.ServiceEntity;
import com.jettech.code.mapper.ProjectMapper;
import com.jettech.code.mapper.ServiceMapper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class GitService {
    private static final Logger log = LoggerFactory.getLogger(GitService.class);
    
    private final ProjectMapper projectMapper;
    private final ServiceMapper serviceMapper;
    private final WorkspaceConfig workspaceConfig;

    @Value("${jettech.git.timeout:300000}")
    private int timeout;

    @Value("${jettech.git.retry-times:3}")
    private int retryTimes;

    @Value("${jettech.git.retry-interval:5000}")
    private int retryInterval;

    @Value("${jettech.git.shallow-depth:1}")
    private int shallowDepth;

    public GitService(ProjectMapper projectMapper, ServiceMapper serviceMapper, WorkspaceConfig workspaceConfig) {
        this.projectMapper = projectMapper;
        this.serviceMapper = serviceMapper;
        this.workspaceConfig = workspaceConfig;
    }

    @Transactional
    public Project cloneRepository(CloneRequest request) throws Exception {
        if (projectMapper.existsByGitUrl(request.getGitUrl())) {
            throw new IllegalArgumentException("Project already exists with this Git URL");
        }

        String projectName = request.getName();
        if (projectName == null || projectName.isEmpty()) {
            projectName = extractProjectName(request.getGitUrl());
        }

        Path reposPath = workspaceConfig.getReposPath();
        Path projectPath = reposPath.resolve(projectName);
        Files.createDirectories(projectPath.getParent());

        Git git = executeWithRetry(() -> {
            return Git.cloneRepository()
                    .setURI(request.getGitUrl())
                    .setDirectory(projectPath.toFile())
                    .setCloneAllBranches(true)
                    .setTimeout(timeout)
                    .setProgressMonitor(new TextProgressMonitor(new PrintWriter(System.out)))
                    .call();
        }, "clone repository " + projectName);

        String branch = request.getBranch();
        if (branch != null && !branch.isEmpty()) {
            git.checkout().setName(branch).call();
        } else {
            Ref head = git.getRepository().exactRef("HEAD");
            branch = head.getTarget().getName().replace("refs/heads/", "");
        }

        String lastCommit = git.log().setMaxCount(1).call().iterator().next().getName();

        Project project = new Project();
        project.setName(projectName);
        project.setGitUrl(request.getGitUrl());
        project.setLocalPath(projectPath.toString());
        project.setCurrentBranch(branch);
        project.setLastCommit(lastCommit);
        project.setCreatedAt(LocalDateTime.now());

        git.close();

        projectMapper.insert(project);
        return project;
    }

    public List<Map<String, String>> getBranches(Long projectId) throws Exception {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new IllegalArgumentException("Project not found");
        }

        Git git = Git.open(new File(project.getLocalPath()));
        Collection<Ref> refs = git.branchList().call();

        List<Map<String, String>> branches = refs.stream()
                .map(ref -> {
                    Map<String, String> branch = new HashMap<>();
                    branch.put("name", ref.getName().replace("refs/heads/", ""));
                    branch.put("objectId", ref.getObjectId().getName());
                    return branch;
                })
                .toList();

        git.close();
        return branches;
    }

    public void checkoutBranch(Long projectId, String branchName) throws Exception {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new IllegalArgumentException("Project not found");
        }

        Git git = Git.open(new File(project.getLocalPath()));
        git.checkout().setName(branchName).call();

        String lastCommit = git.log().setMaxCount(1).call().iterator().next().getName();
        project.setCurrentBranch(branchName);
        project.setLastCommit(lastCommit);

        git.close();
        projectMapper.update(project);
    }

    public void pull(Long projectId) throws Exception {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new IllegalArgumentException("Project not found");
        }

        executeWithRetry(() -> {
            Git git = Git.open(new File(project.getLocalPath()));
            git.pull()
                    .setTimeout(timeout)
                    .setProgressMonitor(new TextProgressMonitor(new PrintWriter(System.out)))
                    .call();

            String lastCommit = git.log().setMaxCount(1).call().iterator().next().getName();
            project.setLastCommit(lastCommit);

            git.close();
            projectMapper.update(project);
            return null;
        }, "pull project " + project.getName());
    }

    @Transactional
    public ServiceEntity pullService(Long serviceId) throws Exception {
        ServiceEntity service = serviceMapper.findById(serviceId);
        if (service == null) {
            throw new IllegalArgumentException("Service not found");
        }

        String localPath = service.getLocalPath();
        File repoDir = (localPath != null && !localPath.isEmpty()) ? new File(localPath) : null;
        
        boolean needClone = repoDir == null || !repoDir.exists() || !new File(repoDir, ".git").exists();
        
        if (needClone) {
            Path reposPath = workspaceConfig.getReposPath();
            String repoName = extractProjectName(service.getGitUrl());
            localPath = reposPath.resolve(repoName).toString();
            repoDir = new File(localPath);
            
            if (repoDir.exists()) {
                deleteDirectory(repoDir);
            }
            
            Git cloneGit = Git.cloneRepository()
                    .setURI(service.getGitUrl())
                    .setDirectory(repoDir)
                    .setDepth(shallowDepth)
                    .setTimeout(timeout)
                    .setProgressMonitor(new TextProgressMonitor(new PrintWriter(System.out)))
                    .call();
            cloneGit.close();
            
            service.setLocalPath(localPath);
        }

        Git git = Git.open(repoDir);
        git.pull()
                .setTimeout(timeout)
                .setProgressMonitor(new TextProgressMonitor(new PrintWriter(System.out)))
                .call();

        String lastCommit = git.log().setMaxCount(1).call().iterator().next().getName();
        String currentBranch = git.getRepository().getBranch();
        service.setLastCommit(lastCommit);
        service.setCurrentBranch(currentBranch);

        git.close();
        serviceMapper.update(service);
        return service;
    }
    
    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    private <T> T executeWithRetry(GitOperation<T> operation, String operationName) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= retryTimes; attempt++) {
            try {
                log.info("Executing {} (attempt {}/{})", operationName, attempt, retryTimes);
                T result = operation.execute();
                log.info("{} completed successfully", operationName);
                return result;
            } catch (GitAPIException e) {
                lastException = e;
                log.warn("{} failed (attempt {}/{}): {}", operationName, attempt, retryTimes, e.getMessage());
                
                if (attempt < retryTimes) {
                    log.info("Retrying in {} ms...", retryInterval);
                    TimeUnit.MILLISECONDS.sleep(retryInterval);
                }
            } catch (Exception e) {
                lastException = e;
                log.error("{} failed with unexpected error: {}", operationName, e.getMessage());
                throw e;
            }
        }
        
        throw new Exception("Failed to " + operationName + " after " + retryTimes + " attempts", lastException);
    }

    private String extractProjectName(String gitUrl) {
        String[] parts = gitUrl.split("/");
        String lastPart = parts[parts.length - 1];
        return lastPart.endsWith(".git") ? lastPart.substring(0, lastPart.length() - 4) : lastPart;
    }

    public List<Map<String, Object>> getFileTree(Long serviceId, String path) throws Exception {
        ServiceEntity service = serviceMapper.findById(serviceId);
        if (service == null) {
            throw new IllegalArgumentException("Service not found");
        }

        String localPath = service.getLocalPath();
        if (localPath == null || localPath.isEmpty()) {
            throw new IllegalArgumentException("Service local path not configured");
        }

        File baseDir = new File(localPath);
        File targetDir = path == null || path.isEmpty() ? baseDir : new File(baseDir, path);

        if (!targetDir.exists() || !targetDir.isDirectory()) {
            throw new IllegalArgumentException("Directory not found: " + path);
        }

        File[] files = targetDir.listFiles();
        if (files == null) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> fileTree = new ArrayList<>();
        for (File file : files) {
            String name = file.getName();
            if (name.equals(".git")) {
                continue;
            }

            Map<String, Object> node = new HashMap<>();
            node.put("name", name);
            String relativePath = path == null || path.isEmpty() ? name : path + "/" + name;
            node.put("path", relativePath);
            node.put("type", file.isDirectory() ? "directory" : "file");

            if (file.isDirectory()) {
                node.put("children", getFileTree(serviceId, relativePath));
            }

            fileTree.add(node);
        }

        fileTree.sort((a, b) -> {
            boolean aIsDir = a.get("type").equals("directory");
            boolean bIsDir = b.get("type").equals("directory");
            if (aIsDir && !bIsDir) return -1;
            if (!aIsDir && bIsDir) return 1;
            return ((String) a.get("name")).compareToIgnoreCase((String) b.get("name"));
        });

        return fileTree;
    }

    public Map<String, Object> getFileContent(Long serviceId, String path) throws Exception {
        ServiceEntity service = serviceMapper.findById(serviceId);
        if (service == null) {
            throw new IllegalArgumentException("Service not found");
        }

        String localPath = service.getLocalPath();
        if (localPath == null || localPath.isEmpty()) {
            throw new IllegalArgumentException("Service local path not configured");
        }

        File file = new File(localPath, path);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File not found: " + path);
        }

        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        String language = detectLanguage(file.getName());

        Map<String, Object> result = new HashMap<>();
        result.put("path", path);
        result.put("content", content);
        result.put("language", language);
        return result;
    }

    public List<Map<String, String>> getServiceBranches(Long serviceId) throws Exception {
        ServiceEntity service = serviceMapper.findById(serviceId);
        if (service == null) {
            throw new IllegalArgumentException("Service not found");
        }

        String localPath = service.getLocalPath();
        if (localPath == null || localPath.isEmpty()) {
            throw new IllegalArgumentException("Service local path not configured");
        }

        Git git = Git.open(new File(localPath));
        Collection<Ref> refs = git.branchList().call();

        List<Map<String, String>> branches = refs.stream()
                .map(ref -> {
                    Map<String, String> branch = new HashMap<>();
                    branch.put("name", ref.getName().replace("refs/heads/", ""));
                    branch.put("objectId", ref.getObjectId().getName());
                    return branch;
                })
                .collect(Collectors.toList());

        git.close();
        return branches;
    }

    public void checkoutServiceBranch(Long serviceId, String branchName) throws Exception {
        ServiceEntity service = serviceMapper.findById(serviceId);
        if (service == null) {
            throw new IllegalArgumentException("Service not found");
        }

        String localPath = service.getLocalPath();
        if (localPath == null || localPath.isEmpty()) {
            throw new IllegalArgumentException("Service local path not configured");
        }

        Git git = Git.open(new File(localPath));
        git.checkout().setName(branchName).call();

        String lastCommit = git.log().setMaxCount(1).call().iterator().next().getName();
        service.setCurrentBranch(branchName);
        service.setLastCommit(lastCommit);

        git.close();
        serviceMapper.update(service);
    }

    private String detectLanguage(String fileName) {
        if (fileName.endsWith(".java")) return "java";
        if (fileName.endsWith(".ts") || fileName.endsWith(".tsx")) return "typescript";
        if (fileName.endsWith(".js") || fileName.endsWith(".jsx")) return "javascript";
        if (fileName.endsWith(".vue")) return "vue";
        if (fileName.endsWith(".py")) return "python";
        if (fileName.endsWith(".go")) return "go";
        if (fileName.endsWith(".rs")) return "rust";
        if (fileName.endsWith(".c")) return "c";
        if (fileName.endsWith(".cpp") || fileName.endsWith(".cc")) return "cpp";
        if (fileName.endsWith(".h")) return "c";
        if (fileName.endsWith(".json")) return "json";
        if (fileName.endsWith(".xml")) return "xml";
        if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) return "yaml";
        if (fileName.endsWith(".md")) return "markdown";
        if (fileName.endsWith(".html")) return "html";
        if (fileName.endsWith(".css")) return "css";
        if (fileName.endsWith(".scss") || fileName.endsWith(".sass")) return "scss";
        if (fileName.endsWith(".sql")) return "sql";
        if (fileName.endsWith(".sh")) return "shell";
        if (fileName.endsWith(".properties")) return "properties";
        return "text";
    }

    @FunctionalInterface
    private interface GitOperation<T> {
        T execute() throws Exception;
    }
}
