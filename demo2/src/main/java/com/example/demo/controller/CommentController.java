package com.example.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.Comment;
import com.example.demo.service.CommentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 从京东获取评论并保存
     * @param url 京东商品URL
     * @param maxPages 最大爬取页数
     */
    @GetMapping("/fetch")
    public Result<List<Comment>> fetchComments(
            @RequestParam String url,
            @RequestParam(defaultValue = "5") int maxPages) {
        return commentService.fetchAndSaveComments(url, maxPages);
    }

    /**
     * 从数据库查询评论(分页)
     * @param pageNum 页码
     * @param pageSize 每页大小
     */
    @GetMapping("")
    public Result<Page<Comment>> getComments(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return commentService.getCommentsFromDb(pageNum, pageSize);
    }
}