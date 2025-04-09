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
}