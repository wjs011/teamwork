package com.example.demo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Comment {
    private Long id;            // 评论ID
    private String productId;   // 商品ID
    private String content;     // 评论内容
    private String nickname;    // 用户昵称
    private Integer score;      // 评分
    private LocalDateTime createTime; // 创建时间
    private Double sentimentScore;  // 情感评分
    private String sentimentLabel; // 情感标签
}