package com.example.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.Comment;
import com.example.demo.mapper.CommentMapper;
import com.example.demo.service.CommentService;
import com.example.demo.service.KeywordExtractionService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;
    private final CommentMapper commentMapper;
    private final KeywordExtractionService keywordExtractionService;

    // 构造函数注入所有依赖
    public CommentController(CommentService commentService,
                             CommentMapper commentMapper,
                             KeywordExtractionService keywordExtractionService) {
        this.commentService = commentService;
        this.commentMapper = commentMapper;
        this.keywordExtractionService = keywordExtractionService;
    }

    // 原有方法保持不变...
    @GetMapping("/fetch")
    public Result<List<Comment>> fetchComments(@RequestParam String url) {
        return commentService.fetchAndSaveComments(url);
    }

    @GetMapping("")
    public Result<Page<Comment>> getComments(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String productId) {
        return commentService.getCommentsFromDb(pageNum, pageSize, productId);
    }

    // 关键词提取接口
    @GetMapping("/keywords1/{productId}")
    public Result<Map<String, Object>> getKeywordsForWordCloud(
            @PathVariable String productId,
            @RequestParam(defaultValue = "50") int topN) {

        List<Comment> comments = commentMapper.selectByProductId(productId);

        if (comments == null || comments.isEmpty()) {
            return Result.error("404", "该商品暂无评论");
        }

        Map<String, Integer> keywords = keywordExtractionService.extractKeywordsFromComments(comments, topN);

        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("totalComments", comments.size());
        result.put("keywords", keywords);

        return Result.success(result);
    }

    @GetMapping("/keywords2/{productId}")
    public Result<List<String>> getKeywordsByTfIdf(
            @PathVariable String productId,
            @RequestParam(defaultValue = "20") int topN) {

        List<Comment> comments = commentMapper.selectByProductId(productId);

        if (comments == null || comments.isEmpty()) {
            return Result.error("404", "该商品暂无评论");
        }

        // 使用方法1的实现
        List<String> keywords = keywordExtractionService.extractKeywordsFromAllComments(comments, topN);

        return Result.success(keywords);
    }


}