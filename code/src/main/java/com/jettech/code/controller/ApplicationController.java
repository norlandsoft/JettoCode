package com.jettech.code.controller;

import com.jettech.code.dto.ApiResponse;
import com.jettech.code.entity.Application;
import com.jettech.code.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService applicationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Application>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(applicationService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Application>> getById(@PathVariable Long id) {
        Application app = applicationService.getById(id);
        if (app == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(app));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Application>> create(@RequestBody Application application) {
        Application created = applicationService.create(application);
        return ResponseEntity.ok(ApiResponse.success("Application created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Application>> update(@PathVariable Long id, @RequestBody Application application) {
        application.setId(id);
        Application updated = applicationService.update(application);
        return ResponseEntity.ok(ApiResponse.success("Application updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        applicationService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Application deleted successfully", null));
    }
}
