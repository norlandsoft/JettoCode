package com.jettech.code.mapper;

import com.jettech.code.entity.QualityCheckGroup;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface QualityCheckGroupMapper {

    @Select("SELECT * FROM quality_check_group WHERE enabled = 1 ORDER BY sort_order")
    List<QualityCheckGroup> findAllEnabled();

    @Select("SELECT * FROM quality_check_group ORDER BY sort_order")
    List<QualityCheckGroup> findAll();

    @Select("SELECT * FROM quality_check_group WHERE id = #{id}")
    QualityCheckGroup findById(Long id);

    @Select("SELECT * FROM quality_check_group WHERE group_key = #{groupKey}")
    QualityCheckGroup findByKey(String groupKey);

    @Insert("INSERT INTO quality_check_group (group_key, group_name, description, sort_order, enabled, created_at, updated_at) " +
            "VALUES (#{groupKey}, #{groupName}, #{description}, #{sortOrder}, #{enabled}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QualityCheckGroup group);

    @Update("UPDATE quality_check_group SET group_key = #{groupKey}, group_name = #{groupName}, " +
            "description = #{description}, sort_order = #{sortOrder}, enabled = #{enabled}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int update(QualityCheckGroup group);

    @Delete("DELETE FROM quality_check_group WHERE id = #{id}")
    int delete(Long id);
}
