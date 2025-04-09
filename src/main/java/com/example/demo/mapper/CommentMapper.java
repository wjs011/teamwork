package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Comment;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface CommentMapper extends BaseMapper<Comment> {
    @Select("SELECT * FROM comment_${productId} ORDER BY id DESC")
    List<Comment> selectByProductId(@Param("productId") String productId);

    // ✅ 使用 XML 中的 SQL，声明方法，不加注解
    List<Comment> selectByProductIdWithLimit(
            @Param("productId") String productId,
            @Param("offset") long offset,
            @Param("pageSize") int pageSize);

    @Update("CREATE TABLE IF NOT EXISTS comment_${productId} (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "product_id VARCHAR(64) NOT NULL, " +
            "content TEXT, " +
            "nickname VARCHAR(64), " +
            "score INT, " +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
            ")")
    void createCommentTableForProduct(@Param("productId") String productId);

    @Insert("<script>" +
            "INSERT INTO comment_${productId} (product_id, content, nickname, score, create_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.productId}, #{item.content}, #{item.nickname}, #{item.score}, #{item.createTime})" +
            "</foreach>" +
            "</script>")
    int batchInsertForProduct(@Param("productId") String productId, @Param("list") List<Comment> comments);

    @Select("SELECT COUNT(*) FROM comment_${productId}")
    int countByProductId(@Param("productId") String productId);
}