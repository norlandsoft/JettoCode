package com.jettech.code.mapper;

import com.jettech.code.entity.QualityCheckItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QualityCheckItemMapper {
    List<QualityCheckItem> findAll();
    
    List<QualityCheckItem> findByCategory(@Param("category") String category);
    
    List<QualityCheckItem> findByEnabled(@Param("enabled") Boolean enabled);
    
    QualityCheckItem findById(@Param("id") Long id);
    
    QualityCheckItem findByRuleId(@Param("ruleId") String ruleId);
    
    int insert(QualityCheckItem item);
    
    int update(QualityCheckItem item);
    
    int deleteById(@Param("id") Long id);
    
    int updateEnabled(@Param("id") Long id, @Param("enabled") Boolean enabled);
}
