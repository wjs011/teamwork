package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.CommentCategory;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommentCategoryMapper extends BaseMapper<CommentCategory> {
    
    @Select("SELECT * FROM comment_category ORDER BY priority DESC")
    List<CommentCategory> selectAllCategories();
    
    @Select("SELECT * FROM comment_category WHERE id = #{id}")
    CommentCategory selectById(@Param("id") Long id);
    
    @Insert("INSERT INTO comment_category (name, description, keywords, priority, create_time, update_time) " +
            "VALUES (#{name}, #{description}, #{keywords}, #{priority}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CommentCategory category);
    
    @Update("UPDATE comment_category SET name = #{name}, description = #{description}, " +
            "keywords = #{keywords}, priority = #{priority}, update_time = #{updateTime} " +
            "WHERE id = #{id}")
    int update(CommentCategory category);
    
    @Delete("DELETE FROM comment_category WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
} 