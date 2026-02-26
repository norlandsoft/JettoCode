package com.jettech.code.service;

import com.jettech.code.dto.QualityCheckConfigUpdateDTO;
import com.jettech.code.dto.QualityCheckTreeDTO;
import com.jettech.code.entity.QualityCheckConfig;
import com.jettech.code.entity.QualityCheckGroup;
import com.jettech.code.mapper.QualityCheckConfigMapper;
import com.jettech.code.mapper.QualityCheckGroupMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QualityCheckService {

    private final QualityCheckGroupMapper groupMapper;
    private final QualityCheckConfigMapper configMapper;

    /**
     * 获取完整的树形结构
     */
    public List<QualityCheckTreeDTO> getTree() {
        List<QualityCheckGroup> groups = groupMapper.findAllEnabled();
        List<QualityCheckConfig> configs = configMapper.findAllEnabled();

        Map<Long, List<QualityCheckConfig>> configByGroup = configs.stream()
                .collect(Collectors.groupingBy(QualityCheckConfig::getGroupId));

        List<QualityCheckTreeDTO> result = new ArrayList<>();
        for (QualityCheckGroup group : groups) {
            QualityCheckTreeDTO dto = new QualityCheckTreeDTO();
            dto.setGroupId(group.getId());
            dto.setGroupKey(group.getGroupKey());
            dto.setGroupName(group.getGroupName());
            dto.setDescription(group.getDescription());
            dto.setSortOrder(group.getSortOrder());

            List<QualityCheckConfig> groupConfigs = configByGroup.getOrDefault(group.getId(), new ArrayList<>());
            List<QualityCheckTreeDTO.CheckItemDTO> items = groupConfigs.stream()
                    .map(this::convertToItemDTO)
                    .collect(Collectors.toList());
            dto.setItems(items);

            result.add(dto);
        }

        return result;
    }

    /**
     * 获取单个配置详情
     */
    public QualityCheckConfig getConfigById(Long id) {
        return configMapper.findById(id);
    }

    /**
     * 获取分组下的所有配置
     */
    public List<QualityCheckConfig> getConfigsByGroupKey(String groupKey) {
        QualityCheckGroup group = groupMapper.findByKey(groupKey);
        if (group == null) {
            return new ArrayList<>();
        }
        return configMapper.findByGroupId(group.getId());
    }

    /**
     * 更新配置（主要是更新promptTemplate）
     */
    @Transactional
    public QualityCheckConfig updateConfig(Long id, QualityCheckConfigUpdateDTO dto) {
        QualityCheckConfig config = configMapper.findById(id);
        if (config == null) {
            throw new IllegalArgumentException("配置不存在: " + id);
        }

        if (dto.getPromptTemplate() != null) {
            configMapper.updatePromptTemplate(id, dto.getPromptTemplate());
        }

        return configMapper.findById(id);
    }

    /**
     * 创建分组
     */
    @Transactional
    public QualityCheckGroup createGroup(QualityCheckGroup group) {
        if (groupMapper.findByKey(group.getGroupKey()) != null) {
            throw new IllegalArgumentException("分组Key已存在: " + group.getGroupKey());
        }
        groupMapper.insert(group);
        return group;
    }

    /**
     * 更新分组
     */
    @Transactional
    public QualityCheckGroup updateGroup(Long id, QualityCheckGroup group) {
        QualityCheckGroup existing = groupMapper.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("分组不存在: " + id);
        }
        group.setId(id);
        groupMapper.update(group);
        return groupMapper.findById(id);
    }

    /**
     * 删除分组
     */
    @Transactional
    public void deleteGroup(Long id) {
        configMapper.deleteByGroupId(id);
        groupMapper.delete(id);
    }

    /**
     * 创建配置
     */
    @Transactional
    public QualityCheckConfig createConfig(QualityCheckConfig config) {
        QualityCheckGroup group = groupMapper.findById(config.getGroupId());
        if (group == null) {
            throw new IllegalArgumentException("分组不存在: " + config.getGroupId());
        }
        configMapper.insert(config);
        return config;
    }

    /**
     * 删除配置
     */
    @Transactional
    public void deleteConfig(Long id) {
        configMapper.delete(id);
    }

    private QualityCheckTreeDTO.CheckItemDTO convertToItemDTO(QualityCheckConfig config) {
        QualityCheckTreeDTO.CheckItemDTO dto = new QualityCheckTreeDTO.CheckItemDTO();
        dto.setId(config.getId());
        dto.setItemKey(config.getItemKey());
        dto.setItemName(config.getItemName());
        dto.setDescription(config.getDescription());
        dto.setPromptTemplate(config.getPromptTemplate());
        dto.setSeverity(config.getSeverity());
        dto.setSortOrder(config.getSortOrder());
        return dto;
    }
}
