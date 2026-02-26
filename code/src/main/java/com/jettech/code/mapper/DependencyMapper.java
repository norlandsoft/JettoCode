package com.jettech.code.mapper;

import com.jettech.code.entity.Dependency;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DependencyMapper {
    List<Dependency> findByServiceId(Long serviceId);
    
    Dependency findById(Long id);
    
    List<Dependency> findByServiceIdAndLicenseStatus(@Param("serviceId") Long serviceId, @Param("licenseStatus") String licenseStatus);
    
    int insert(Dependency dependency);
    
    int batchInsert(List<Dependency> dependencies);
    
    int update(Dependency dependency);
    
    int deleteByServiceId(Long serviceId);
    
    int deleteById(Long id);
    
    List<Dependency> findByApplicationId(Long applicationId);
}
