package com.jettech.code.mapper;

import com.jettech.code.entity.ServiceEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ServiceMapper {
    List<ServiceEntity> findByApplicationId(Long applicationId);
    
    ServiceEntity findById(Long id);
    
    int insert(ServiceEntity service);
    
    int update(ServiceEntity service);
    
    int deleteById(Long id);
    
    int deleteByApplicationId(Long applicationId);
}
