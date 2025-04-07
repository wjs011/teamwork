package com.example.demo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Comment {
    private String content;       // 评论内容
    private String nickname;      // 用户昵称
    private Integer score;        // 评分
}
