package com.jettech.code.mapper;

import com.jettech.code.entity.CodeQualityTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CodeQualityTaskMapper {

    void insert(CodeQualityTask task);

    void update(CodeQualityTask task);

    void deleteById(Long id);

    void deleteByScanId(@Param("scanId") Long scanId);

    CodeQualityTask findById(@Param("id") Long id);

    List<CodeQualityTask> findByScanId(@Param("scanId") Long scanId);

    List<CodeQualityTask> findByServiceId(@Param("serviceId") Long serviceId);

    List<CodeQualityTask> findByStatus(@Param("status") String status);

    List<CodeQualityTask> findPendingTasks(@Param("scanId") Long scanId);

    CodeQualityTask findNextPendingTask(@Param("scanId") Long scanId);

    void batchInsert(@Param("tasks") List<CodeQualityTask> tasks);

    void updateStatus(@Param("id") Long id, @Param("status") String status);

    int countByScanIdAndStatus(@Param("scanId") Long scanId, @Param("status") String status);

    int countCompletedByScanId(@Param("scanId") Long scanId);

    int countTotalByScanId(@Param("scanId") Long scanId);
}
