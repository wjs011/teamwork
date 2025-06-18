package com.example.demo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentCategory {
    private Long id;            // 分类ID
    private String name;        // 分类名称
    private String description; // 分类描述
    private String keywords;    // 分类关键词（JSON格式存储）
    private Integer priority;   // 优先级
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
} 