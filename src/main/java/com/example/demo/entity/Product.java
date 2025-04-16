package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    private String id;          // 京东商品ID
    private String name;        // 商品名称
    private String url;         // 商品URL
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
}