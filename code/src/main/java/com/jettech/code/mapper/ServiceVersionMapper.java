package com.jettech.code.mapper;

import com.jettech.code.entity.ServiceVersion;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ServiceVersionMapper {
    List<ServiceVersion> findByServiceId(Long serviceId);
    
    ServiceVersion findById(Long id);
    
    int insert(ServiceVersion serviceVersion);
    
    int update(ServiceVersion serviceVersion);
    
    int deleteById(Long id);
    
    int deleteByServiceId(Long serviceId);
}
