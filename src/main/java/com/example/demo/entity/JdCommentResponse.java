package com.example.demo.entity;

import com.example.demo.entity.Comment;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class JdCommentResponse {
    private List<Comment> comments;
    private int maxPage;
    private Map<String, Object> productCommentSummary;
}