package com.jettech.code.controller;

import com.jettech.code.dto.ApiResponse;
import com.jettech.code.dto.QualityCheckConfigUpdateDTO;
import com.jettech.code.dto.QualityCheckTreeDTO;
import com.jettech.code.entity.QualityCheckConfig;
import com.jettech.code.entity.QualityCheckGroup;
import com.jettech.code.service.QualityCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quality-check")
@RequiredArgsConstructor
public class QualityCheckController {

    private final QualityCheckService qualityCheckService;

    /**
     * 获取完整的树形结构
     */
    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<QualityCheckTreeDTO>>> getTree() {
        List<QualityCheckTreeDTO> tree = qualityCheckService.getTree();
        return ResponseEntity.ok(ApiResponse.success(tree));
    }

    /**
     * 获取单个配置详情
     */
    @GetMapping("/configs/{id}")
    public ResponseEntity<ApiResponse<QualityCheckConfig>> getConfig(@PathVariable Long id) {
        QualityCheckConfig config = qualityCheckService.getConfigById(id);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    /**
     * 获取分组下的所有配置
     */
    @GetMapping("/groups/{groupKey}/configs")
    public ResponseEntity<ApiResponse<List<QualityCheckConfig>>> getConfigsByGroupKey(
            @PathVariable String groupKey) {
        List<QualityCheckConfig> configs = qualityCheckService.getConfigsByGroupKey(groupKey);
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    /**
     * 更新配置（主要是更新promptTemplate）
     */
    @PutMapping("/configs/{id}")
    public ResponseEntity<ApiResponse<QualityCheckConfig>> updateConfig(
            @PathVariable Long id,
            @RequestBody QualityCheckConfigUpdateDTO dto) {
        try {
            QualityCheckConfig config = qualityCheckService.updateConfig(id, dto);
            return ResponseEntity.ok(ApiResponse.success("更新成功", config));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 创建分组
     */
    @PostMapping("/groups")
    public ResponseEntity<ApiResponse<QualityCheckGroup>> createGroup(@RequestBody QualityCheckGroup group) {
        try {
            QualityCheckGroup created = qualityCheckService.createGroup(group);
            return ResponseEntity.ok(ApiResponse.success("创建成功", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 更新分组
     */
    @PutMapping("/groups/{id}")
    public ResponseEntity<ApiResponse<QualityCheckGroup>> updateGroup(
            @PathVariable Long id,
            @RequestBody QualityCheckGroup group) {
        try {
            QualityCheckGroup updated = qualityCheckService.updateGroup(id, group);
            return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 删除分组
     */
    @DeleteMapping("/groups/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(@PathVariable Long id) {
        try {
            qualityCheckService.deleteGroup(id);
            return ResponseEntity.ok(ApiResponse.success("删除成功", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 创建配置
     */
    @PostMapping("/configs")
    public ResponseEntity<ApiResponse<QualityCheckConfig>> createConfig(@RequestBody QualityCheckConfig config) {
        try {
            QualityCheckConfig created = qualityCheckService.createConfig(config);
            return ResponseEntity.ok(ApiResponse.success("创建成功", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/configs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(@PathVariable Long id) {
        try {
            qualityCheckService.deleteConfig(id);
            return ResponseEntity.ok(ApiResponse.success("删除成功", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
