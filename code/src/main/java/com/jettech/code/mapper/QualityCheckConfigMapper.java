package com.jettech.code.mapper;

import com.jettech.code.entity.QualityCheckConfig;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface QualityCheckConfigMapper {

    @Select("SELECT * FROM quality_check_config WHERE enabled = 1 ORDER BY sort_order")
    List<QualityCheckConfig> findAllEnabled();

    @Select("SELECT * FROM quality_check_config ORDER BY group_id, sort_order")
    List<QualityCheckConfig> findAll();

    @Select("SELECT * FROM quality_check_config WHERE id = #{id}")
    QualityCheckConfig findById(Long id);

    @Select("SELECT * FROM quality_check_config WHERE group_id = #{groupId} AND enabled = 1 ORDER BY sort_order")
    List<QualityCheckConfig> findByGroupId(Long groupId);

    @Select("SELECT * FROM quality_check_config WHERE group_id = #{groupId} ORDER BY sort_order")
    List<QualityCheckConfig> findAllByGroupId(Long groupId);

    @Insert("INSERT INTO quality_check_config (group_id, item_key, item_name, description, prompt_template, severity, sort_order, enabled, created_at, updated_at) " +
            "VALUES (#{groupId}, #{itemKey}, #{itemName}, #{description}, #{promptTemplate}, #{severity}, #{sortOrder}, #{enabled}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QualityCheckConfig config);

    @Update("UPDATE quality_check_config SET group_id = #{groupId}, item_key = #{itemKey}, item_name = #{itemName}, " +
            "description = #{description}, prompt_template = #{promptTemplate}, severity = #{severity}, " +
            "sort_order = #{sortOrder}, enabled = #{enabled}, updated_at = NOW() WHERE id = #{id}")
    int update(QualityCheckConfig config);

    @Update("UPDATE quality_check_config SET prompt_template = #{promptTemplate}, updated_at = NOW() WHERE id = #{id}")
    int updatePromptTemplate(@Param("id") Long id, @Param("promptTemplate") String promptTemplate);

    @Delete("DELETE FROM quality_check_config WHERE id = #{id}")
    int delete(Long id);

    @Delete("DELETE FROM quality_check_config WHERE group_id = #{groupId}")
    int deleteByGroupId(Long groupId);
}
