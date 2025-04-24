package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin")
public class Admin {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String username;
    private String password;
    private String nickName;
    private Integer age;
    private String gender;
    private String address;
    private String avatar; // 新增头像字段
    private String token; // 存储token
    private LocalDateTime tokenExpireTime; // token过期时间
    private String authCode; // 认证码字段
    private String permissions; // 权限列表，可以用JSON格式存储
}