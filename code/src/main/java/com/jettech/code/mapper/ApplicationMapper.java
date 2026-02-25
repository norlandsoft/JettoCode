package com.jettech.code.mapper;

import com.jettech.code.entity.Application;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ApplicationMapper {
    List<Application> findAll();
    
    Application findById(Long id);
    
    int insert(Application application);
    
    int update(Application application);
    
    int deleteById(Long id);
}
