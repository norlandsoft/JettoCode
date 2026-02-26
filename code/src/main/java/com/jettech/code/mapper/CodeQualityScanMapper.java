package com.jettech.code.mapper;

import com.jettech.code.entity.CodeQualityScan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CodeQualityScanMapper {
    List<CodeQualityScan> findByServiceId(Long serviceId);
    
    CodeQualityScan findById(Long id);
    
    CodeQualityScan findLatestByServiceId(Long serviceId);
    
    CodeQualityScan findLatestByApplicationId(Long applicationId);
    
    int insert(CodeQualityScan scan);
    
    int update(CodeQualityScan scan);
    
    int deleteByServiceId(Long serviceId);
    
    int deleteById(Long id);
    
    int countByServiceIdAndStatus(@Param("serviceId") Long serviceId, @Param("status") String status);
}
