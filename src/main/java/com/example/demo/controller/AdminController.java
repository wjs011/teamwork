package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.Admin;
import com.example.demo.mapper.AdminMapper;
import com.example.demo.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private AdminMapper adminMapper;

    @Value("${admin.register.code}")
    private String adminRegisterCode; // 注入配置的认证码

    // 管理员注册
    @PostMapping("/register")
    public Result<?> register(@RequestBody Admin admin) {
        if(!adminRegisterCode.equals(admin.getAuthCode())) {
            return Result.error("-1", "认证码错误");
        }
        Admin res = adminMapper.selectOne(Wrappers.<Admin>lambdaQuery()
                .eq(Admin::getUsername, admin.getUsername()));
        if(res != null) {
            return Result.error("-1", "用户名已存在");
        }
        adminMapper.insert(admin);
        return Result.success();
    }

    // 管理员登录
    @PostMapping("/login")
    public Result<?> login(@RequestBody Admin admin) {
        Admin res = adminMapper.selectOne(Wrappers.<Admin>lambdaQuery()
                .eq(Admin::getUsername, admin.getUsername())
                .eq(Admin::getPassword, admin.getPassword()));
        if(res == null) {
            return Result.error("-1", "用户名或密码错误");
        }

        // 使该用户所有旧Token失效
        res.setToken(null);
        res.setTokenExpireTime(null);
        adminMapper.updateById(res);

        // 生成新token并设置过期时间
        String token = TokenUtils.generateToken();
        res.setToken(token);
        res.setTokenExpireTime(LocalDateTime.now().plusHours(24)); // 24小时后过期
        adminMapper.updateById(res);
        return Result.success(res);
    }

    // 检查token有效性
    private boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        Admin admin = adminMapper.selectOne(Wrappers.<Admin>lambdaQuery()
                .eq(Admin::getToken, token));

        return admin != null &&
                admin.getTokenExpireTime() != null &&
                admin.getTokenExpireTime().isAfter(LocalDateTime.now());
    }

    // 获取当前管理员信息
    @GetMapping("/info")
    public Result<Admin> getAdminInfo(@RequestHeader("Authorization") String token) {
        if (!validateToken(token)) {
            return Result.error("401", "无效或过期的token");
        }

        Admin admin = adminMapper.selectOne(Wrappers.<Admin>lambdaQuery()
                .eq(Admin::getToken, token));

        return Result.success(admin);
    }

    // 获取管理员列表
    @GetMapping("/all")
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String search) {
        LambdaQueryWrapper<Admin> query = Wrappers.<Admin>lambdaQuery();
        if(!search.isEmpty()) {
            query.like(Admin::getUsername, search)
                    .or()
                    .like(Admin::getNickName, search);
        }
        Page<Admin> page = adminMapper.selectPage(new Page<>(pageNum, pageSize), query);
        return Result.success(page);
    }

    // 更新管理员信息
    @PutMapping("/update")
    public Result<?> update(@RequestBody Admin admin) {
        adminMapper.updateById(admin);
        return Result.success();
    }

    // 删除管理员，只有总管理员才可以删除其他管理员
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Integer id, @RequestHeader("Authorization") String token) {
        if (token == null || token.isEmpty()) {
            return Result.error("-1", "无权限操作") ;
        }
        Admin admin = adminMapper.selectOne(Wrappers.<Admin>lambdaQuery()
                .eq(Admin::getToken, token));
        if(admin == null || admin.getId() != 1) {
            return Result.error("-1", "无权限操作") ;
        }
        adminMapper.deleteById(id);
        return Result.success();
    }
}