package com.jettech.code.mapper;

import com.jettech.code.entity.CodeQualityIssue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CodeQualityIssueMapper {
    List<CodeQualityIssue> findByScanId(Long scanId);
    
    CodeQualityIssue findById(Long id);
    
    List<CodeQualityIssue> findByScanIdAndCategory(@Param("scanId") Long scanId, @Param("category") String category);
    
    List<CodeQualityIssue> findByScanIdAndSeverity(@Param("scanId") Long scanId, @Param("severity") String severity);
    
    int countByScanId(Long scanId);
    
    int countByScanIdAndCategory(@Param("scanId") Long scanId, @Param("category") String category);
    
    int countByScanIdAndSeverity(@Param("scanId") Long scanId, @Param("severity") String severity);
    
    int insert(CodeQualityIssue issue);
    
    int batchInsert(List<CodeQualityIssue> issues);
    
    int update(CodeQualityIssue issue);
    
    int deleteByScanId(Long scanId);
    
    int deleteById(Long id);
}
