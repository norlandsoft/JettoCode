package com.jettech.code.mapper;

import com.jettech.code.entity.LicenseRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LicenseRuleMapper {
    List<LicenseRule> findAll();
    
    List<LicenseRule> findByEcosystem(@Param("ecosystem") String ecosystem);
    
    LicenseRule findByPackagePattern(@Param("packagePattern") String packagePattern);
    
    int insert(LicenseRule rule);
    
    int batchInsert(List<LicenseRule> rules);
    
    int deleteById(Long id);
}
