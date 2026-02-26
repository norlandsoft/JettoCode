package com.jettech.code.controller;

import com.jettech.code.dto.ApiResponse;
import com.jettech.code.entity.QualityCheckItem;
import com.jettech.code.service.QualityCheckItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quality-check-items")
public class QualityCheckItemController {
    
    private final QualityCheckItemService service;

    public QualityCheckItemController(QualityCheckItemService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<QualityCheckItem>>> getAll(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean enabled) {
        List<QualityCheckItem> items;
        
        if (category != null) {
            items = service.getByCategory(category);
        } else if (enabled != null) {
            items = service.getByEnabled(enabled);
        } else {
            items = service.getAll();
        }
        
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QualityCheckItem>> getById(@PathVariable Long id) {
        QualityCheckItem item = service.getById(id);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(item));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<QualityCheckItem>> create(@RequestBody QualityCheckItem item) {
        try {
            QualityCheckItem created = service.create(item);
            return ResponseEntity.ok(ApiResponse.success("创建成功", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<QualityCheckItem>> update(@PathVariable Long id, @RequestBody QualityCheckItem item) {
        try {
            QualityCheckItem updated = service.update(id, item);
            return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.success("删除成功", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/enabled")
    public ResponseEntity<ApiResponse<Void>> updateEnabled(@PathVariable Long id, @RequestParam Boolean enabled) {
        try {
            service.updateEnabled(id, enabled);
            return ResponseEntity.ok(ApiResponse.success(enabled ? "已启用" : "已禁用", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
