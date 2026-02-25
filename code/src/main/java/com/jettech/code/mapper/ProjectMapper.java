package com.jettech.code.mapper;

import com.jettech.code.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProjectMapper {
    
    int insert(Project project);
    
    int update(Project project);
    
    int deleteById(@Param("id") Long id);
    
    Project selectById(@Param("id") Long id);
    
    List<Project> selectAll();
    
    Project selectByGitUrl(@Param("gitUrl") String gitUrl);
    
    boolean existsByGitUrl(@Param("gitUrl") String gitUrl);
}
