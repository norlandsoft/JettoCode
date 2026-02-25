package com.jettech.code.controller;

import com.jettech.code.dto.ApiResponse;
import com.jettech.code.dto.CloneRequest;
import com.jettech.code.entity.Project;
import com.jettech.code.mapper.ProjectMapper;
import com.jettech.code.service.GitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class GitController {
    private final GitService gitService;
    private final ProjectMapper projectMapper;

    public GitController(GitService gitService, ProjectMapper projectMapper) {
        this.gitService = gitService;
        this.projectMapper = projectMapper;
    }

    @PostMapping("/clone")
    public ResponseEntity<ApiResponse<Project>> cloneRepository(@RequestBody CloneRequest request) {
        try {
            Project project = gitService.cloneRepository(request);
            return ResponseEntity.ok(ApiResponse.success("Repository cloned successfully", project));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to clone repository: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Project>>> getAllProjects() {
        List<Project> projects = projectMapper.selectAll();
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Project>> getProject(@PathVariable Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(project));
    }

    @GetMapping("/{id}/branches")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getBranches(@PathVariable Long id) {
        try {
            List<Map<String, String>> branches = gitService.getBranches(id);
            return ResponseEntity.ok(ApiResponse.success(branches));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get branches: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<ApiResponse<Void>> checkoutBranch(
            @PathVariable Long id,
            @RequestParam String branch) {
        try {
            gitService.checkoutBranch(id, branch);
            return ResponseEntity.ok(ApiResponse.success("Branch checked out successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to checkout branch: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/pull")
    public ResponseEntity<ApiResponse<Void>> pull(@PathVariable Long id) {
        try {
            gitService.pull(id);
            return ResponseEntity.ok(ApiResponse.success("Repository pulled successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to pull repository: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable Long id) {
        Project project = projectMapper.selectById(id);
        if (project != null) {
            projectMapper.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success("Project deleted successfully", null));
        }
        return ResponseEntity.notFound().build();
    }
}
