package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Comment;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

public interface CommentMapper extends BaseMapper<Comment> {
    @Select("SELECT * FROM comment_${productId} ORDER BY id DESC")
    List<Comment> selectByProductId(@Param("productId") String productId);
    @Select("SELECT * FROM comment_${productId} WHERE score BETWEEN #{minScore} AND #{maxScore} ORDER BY id DESC")
    List<Comment> selectByProductIdAndScoreRange(
            @Param("productId") String productId,
            @Param("minScore") int minScore,
            @Param("maxScore") int maxScore);

    // 根据商品ID和关键词查询评论
    @Select("SELECT * FROM comment_${productId} WHERE content LIKE CONCAT('%',#{keyword},'%') ORDER BY id DESC")
    List<Comment> selectByProductIdAndKeyword(
            @Param("productId") String productId,
            @Param("keyword") String keyword);

    // 删除评论
    @Delete("DELETE FROM comment_${productId} WHERE id = #{commentId}")
    int deleteComment(
            @Param("productId") String productId,
            @Param("commentId") Long commentId);
    // ✅ 使用 XML 中的 SQL，声明方法，不加注解
    List<Comment> selectByProductIdWithLimit(
            @Param("productId") String productId,
            @Param("offset") long offset,
            @Param("pageSize") int pageSize);

    @Insert("CREATE TABLE IF NOT EXISTS comment_${productId} (" +
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

    @Select("SELECT DISTINCT product_id as id, product_name as name FROM comment_${productId} WHERE product_id IS NOT NULL")
    List<Map<String, Object>> selectDistinctProducts(@Param("productId") String productId);

    @Select("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME LIKE 'comment_%'")
    List<Map<String, Object>> selectAllCommentTables();

    @Select("SELECT name FROM product WHERE id = #{productId}")
    String selectProductName(@Param("productId") String productId);
}