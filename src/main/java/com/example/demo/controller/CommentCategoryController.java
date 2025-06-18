package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.Comment;
import com.example.demo.entity.CommentCategory;
import com.example.demo.mapper.CommentMapper;
import com.example.demo.service.CommentCategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(originPatterns = "*", allowedHeaders = "*")
public class CommentCategoryController {
    private final CommentCategoryService categoryService;
    private final CommentMapper commentMapper;

    public CommentCategoryController(CommentCategoryService categoryService, CommentMapper commentMapper) {
        this.categoryService = categoryService;
        this.commentMapper = commentMapper;
    }

    // 获取所有分类
    @GetMapping
    public Result<List<CommentCategory>> getAllCategories() {
        return Result.success(categoryService.getAllCategories());
    }

    // 获取单个分类
    @GetMapping("/{id}")
    public Result<CommentCategory> getCategory(@PathVariable Long id) {
        return Result.success(categoryService.getCategoryById(id));
    }

    // 创建分类
    @PostMapping
    public Result<CommentCategory> createCategory(@RequestBody CommentCategory category) {
        return Result.success(categoryService.createCategory(category));
    }

    // 更新分类
    @PutMapping("/{id}")
    public Result<CommentCategory> updateCategory(
            @PathVariable Long id,
            @RequestBody CommentCategory category) {
        category.setId(id);
        return Result.success(categoryService.updateCategory(category));
    }

    // 删除分类
    @DeleteMapping("/{id}")
    public Result<?> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }

    // 获取评论的分类
    @GetMapping("/comments/{productId}")
    public Result<Map<Long, List<String>>> getCommentCategories(@PathVariable String productId) {
        List<Comment> comments = commentMapper.selectByProductId(productId);
        return Result.success(categoryService.categorizeComments(comments));
    }

    // 获取评论分类统计
    @GetMapping("/statistics/{productId}")
    public Result<?> getCategoryStatistics(@PathVariable String productId) {
        List<Comment> comments = commentMapper.selectByProductId(productId);
        Map<String, Integer> statistics = categoryService.getCategoryStatistics(comments);
        return Result.success(statistics);
    }

    // 获取评论分类趋势
    @GetMapping("/trends/{productId}")
    public Result<Map<String, Map<String, Integer>>> getCategoryTrends(
            @PathVariable String productId,
            @RequestParam(defaultValue = "month") String timeRange) {
        List<Comment> comments = commentMapper.selectByProductId(productId);
        return Result.success(categoryService.getCategoryTrends(comments, timeRange));
    }

    // 获取指定分类的评论列表
    @GetMapping("/category-comments/{productId}")
    public Result<?> getCategoryComments(@PathVariable String productId, @RequestParam String category) {
        List<Comment> comments = commentMapper.selectByProductId(productId);
        List<Comment> categorizedComments = comments.stream()
            .filter(comment -> {
                List<String> commentCategories = categoryService.categorizeComments(Collections.singletonList(comment))
                    .get(comment.getId());
                return commentCategories != null && commentCategories.contains(category);
            })
            .collect(Collectors.toList());
        return Result.success(categorizedComments);
    }
} 