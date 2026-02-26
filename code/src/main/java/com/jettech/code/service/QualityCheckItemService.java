package com.jettech.code.service;

import com.jettech.code.entity.QualityCheckItem;
import com.jettech.code.mapper.QualityCheckItemMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class QualityCheckItemService {
    
    private final QualityCheckItemMapper mapper;

    public QualityCheckItemService(QualityCheckItemMapper mapper) {
        this.mapper = mapper;
    }

    public List<QualityCheckItem> getAll() {
        return mapper.findAll();
    }

    public List<QualityCheckItem> getByCategory(String category) {
        return mapper.findByCategory(category);
    }

    public List<QualityCheckItem> getByEnabled(Boolean enabled) {
        return mapper.findByEnabled(enabled);
    }

    public QualityCheckItem getById(Long id) {
        return mapper.findById(id);
    }

    public QualityCheckItem create(QualityCheckItem item) {
        if (item.getRuleId() != null) {
            QualityCheckItem existing = mapper.findByRuleId(item.getRuleId());
            if (existing != null) {
                throw new IllegalArgumentException("规则ID已存在: " + item.getRuleId());
            }
        }
        
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        if (item.getEnabled() == null) {
            item.setEnabled(true);
        }
        if (item.getSortOrder() == null) {
            item.setSortOrder(0);
        }
        
        mapper.insert(item);
        return item;
    }

    public QualityCheckItem update(Long id, QualityCheckItem item) {
        QualityCheckItem existing = mapper.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("检查项不存在: " + id);
        }
        
        if (item.getRuleId() != null && !item.getRuleId().equals(existing.getRuleId())) {
            QualityCheckItem duplicate = mapper.findByRuleId(item.getRuleId());
            if (duplicate != null) {
                throw new IllegalArgumentException("规则ID已存在: " + item.getRuleId());
            }
        }
        
        item.setId(id);
        item.setUpdatedAt(LocalDateTime.now());
        mapper.update(item);
        return mapper.findById(id);
    }

    public void delete(Long id) {
        QualityCheckItem existing = mapper.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("检查项不存在: " + id);
        }
        mapper.deleteById(id);
    }

    public void updateEnabled(Long id, Boolean enabled) {
        QualityCheckItem existing = mapper.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("检查项不存在: " + id);
        }
        mapper.updateEnabled(id, enabled);
    }
}
