package com.jettech.code.mapper;

import com.jettech.code.entity.SecurityScan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SecurityScanMapper {
    List<SecurityScan> findByServiceId(Long serviceId);
    
    SecurityScan findById(Long id);
    
    SecurityScan findLatestByServiceId(Long serviceId);
    
    List<SecurityScan> findByServiceIdAndStatus(@Param("serviceId") Long serviceId, @Param("status") String status);
    
    int insert(SecurityScan scan);
    
    int update(SecurityScan scan);
    
    int deleteByServiceId(Long serviceId);
    
    int deleteById(Long id);
    
    List<SecurityScan> findByApplicationId(Long applicationId);
    
    SecurityScan findLatestByApplicationId(Long applicationId);
}
