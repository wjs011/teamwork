package com.example.demo.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.Admin;
import com.example.demo.entity.Comment;
import com.example.demo.mapper.AdminMapper;
import com.example.demo.mapper.CommentMapper;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.utils.TokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private AdminMapper adminMapper;

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private ProductMapper productMapper;

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

        // 生成token并设置过期时间
        String token = TokenUtils.generateToken();
        res.setToken(token);
        res.setTokenExpireTime(LocalDateTime.now().plusHours(24)); // 24小时后过期
        adminMapper.updateById(res);
        return Result.success(res);
    }

    // 获取最新评论
    @GetMapping("/recent-comments")
    public Result<?> getRecentComments() {
        try {
            // 查询所有商品ID
            List<String> productIds = productMapper.selectList(null)
                    .stream().map(p -> p.getId()).toList();
            List<Comment> allComments = new ArrayList<>();
            for (String pid : productIds) {
                try {
                    List<Comment> comments = commentMapper.selectByProductId(pid);
                    if (comments != null) {
                        allComments.addAll(comments);
                    }
                } catch (Exception e) {
                    // 某个商品表不存在时跳过
                }
            }
            // 按时间倒序排序，取前10条
            allComments.sort((a, b) -> {
                if (a.getCreateTime() == null || b.getCreateTime() == null) return 0;
                return b.getCreateTime().compareTo(a.getCreateTime());
            });
            List<Comment> latestComments = allComments.size() > 10 ? allComments.subList(0, 10) : allComments;
            return Result.success(latestComments);
        } catch (Exception e) {
            return Result.error("500", "获取最新评论失败: " + e.getMessage());
        }
    }

    // 获取关键词分析
    @GetMapping("/keyword-analysis")
    public Result<?> getKeywordAnalysis(@RequestParam(required = false) String productId) {
        try {
            if (productId == null || productId.trim().isEmpty()) {
                return Result.error("400", "商品ID不能为空");
            }

            // 确保评论表存在
            commentMapper.createCommentTableForProduct(productId);

            // 获取该商品的所有评论
            List<Comment> comments = commentMapper.selectByProductId(productId);
            if (comments.isEmpty()) {
                return Result.error("404", "该商品暂无评论数据");
            }

            // 统计关键词频率
            Map<String, Integer> keywordFrequency = new HashMap<>();
            for (Comment comment : comments) {
                if (comment.getContent() != null) {
                    // 这里可以接入分词服务，暂时使用简单的分词方法
                    String[] words = comment.getContent().split("[\\s,，.。!！?？;；:：]");
                    for (String word : words) {
                        if (word.length() >= 2) { // 只统计长度大于等于2的词
                            keywordFrequency.merge(word, 1, Integer::sum);
                        }
                    }
                }
            }

            // 按频率排序，取前5个关键词
            List<Map.Entry<String, Integer>> sortedKeywords = keywordFrequency.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .collect(Collectors.toList());

            // 构建返回数据
            List<String> keywords = new ArrayList<>();
            List<Integer> frequencies = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : sortedKeywords) {
                keywords.add(entry.getKey());
                frequencies.add(entry.getValue());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("keywords", keywords);
            data.put("frequencies", frequencies);
            return Result.success(data);
        } catch (Exception e) {
            log.error("获取关键词分析失败", e);
            return Result.error("500", "获取关键词分析失败: " + e.getMessage());
        }
    }

    // 获取关键词趋势
    @GetMapping("/keyword-trend")
    public Result<?> getKeywordTrend(
            @RequestParam String timeRange,
            @RequestParam(required = false) String productId) {
        try {
            if (productId == null || productId.trim().isEmpty()) {
                return Result.error("400", "商品ID不能为空");
            }

            // 确保评论表存在
            commentMapper.createCommentTableForProduct(productId);

            // 获取该商品的所有评论
            List<Comment> comments = commentMapper.selectByProductId(productId);
            if (comments.isEmpty()) {
                return Result.error("404", "该商品暂无评论数据");
            }

            // 获取前5个高频关键词
            Map<String, Integer> keywordFrequency = new HashMap<>();
            for (Comment comment : comments) {
                if (comment.getContent() != null) {
                    String[] words = comment.getContent().split("[\\s,，.。!！?？;；:：]");
                    for (String word : words) {
                        if (word.length() >= 2) {
                            keywordFrequency.merge(word, 1, Integer::sum);
                        }
                    }
                }
            }

            List<String> topKeywords = keywordFrequency.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            // 根据时间范围确定日期列表
            List<String> dates;
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime;
            if ("week".equals(timeRange)) {
                startTime = now.minusWeeks(1);
                dates = IntStream.range(0, 7)
                        .mapToObj(i -> startTime.plusDays(i).toLocalDate().toString())
                        .collect(Collectors.toList());
            } else if ("month".equals(timeRange)) {
                startTime = now.minusMonths(1);
                dates = IntStream.range(0, 30)
                        .mapToObj(i -> startTime.plusDays(i).toLocalDate().toString())
                        .collect(Collectors.toList());
            } else {
                startTime = now.minusYears(1);
                dates = IntStream.range(0, 12)
                        .mapToObj(i -> startTime.plusMonths(i).toLocalDate().toString())
                        .collect(Collectors.toList());
            }

            // 统计每个关键词在不同时间段的出现频率
            Map<String, List<Integer>> trends = new HashMap<>();
            for (String keyword : topKeywords) {
                List<Integer> frequencies = new ArrayList<>();
                for (String date : dates) {
                    int count = 0;
                    for (Comment comment : comments) {
                        if (comment.getCreateTime() != null &&
                            comment.getCreateTime().toLocalDate().toString().equals(date) &&
                            comment.getContent() != null &&
                            comment.getContent().contains(keyword)) {
                            count++;
                        }
                    }
                    frequencies.add(count);
                }
                trends.put(keyword, frequencies);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("dates", dates);
            data.put("keywords", topKeywords);
            data.put("trends", trends);
            return Result.success(data);
        } catch (Exception e) {
            log.error("获取关键词趋势失败", e);
            return Result.error("500", "获取关键词趋势失败: " + e.getMessage());
        }
    }

    // 获取评论质量分析
    @GetMapping("/quality-analysis")
    public Result<?> getQualityAnalysis(
            @RequestParam String metric,
            @RequestParam(required = false) String productId) {
        try {
            if (productId == null || productId.trim().isEmpty()) {
                return Result.error("400", "商品ID不能为空");
            }

            // 确保评论表存在
            commentMapper.createCommentTableForProduct(productId);

            // 获取该商品的所有评论
            List<Comment> comments = commentMapper.selectByProductId(productId);
            if (comments.isEmpty()) {
                return Result.error("404", "该商品暂无评论数据");
            }

            List<Map<String, Object>> data = new ArrayList<>();
            switch (metric) {
                case "length":
                    // 评论长度分布
                    int shortCount = 0;  // 0-50字
                    int mediumCount = 0; // 51-200字
                    int longCount = 0;   // 200字以上
                    for (Comment comment : comments) {
                        if (comment.getContent() != null) {
                            int length = comment.getContent().length();
                            if (length <= 50) shortCount++;
                            else if (length <= 200) mediumCount++;
                            else longCount++;
                        }
                    }
                    data.add(Map.of("name", "短评", "value", shortCount));
                    data.add(Map.of("name", "中评", "value", mediumCount));
                    data.add(Map.of("name", "长评", "value", longCount));
                    break;

                case "sentiment":
                    // 评论情感分布（基于评分）
                    int positiveCount = 0; // 4-5分
                    int neutralCount = 0;  // 3分
                    int negativeCount = 0; // 1-2分
                    for (Comment comment : comments) {
                        if (comment.getScore() != null) {
                            int score = comment.getScore();
                            if (score >= 4) positiveCount++;
                            else if (score == 3) neutralCount++;
                            else negativeCount++;
                        }
                    }
                    data.add(Map.of("name", "正面", "value", positiveCount));
                    data.add(Map.of("name", "中性", "value", neutralCount));
                    data.add(Map.of("name", "负面", "value", negativeCount));
                    break;

                case "timeliness":
                    // 评论时效性（基于创建时间）
                    LocalDateTime now = LocalDateTime.now();
                    int timelyCount = 0;    // 24小时内
                    int normalCount = 0;    // 24小时-7天
                    int delayedCount = 0;   // 7天以上
                    for (Comment comment : comments) {
                        if (comment.getCreateTime() != null) {
                            long hours = java.time.Duration.between(comment.getCreateTime(), now).toHours();
                            if (hours <= 24) timelyCount++;
                            else if (hours <= 168) normalCount++; // 7天 = 168小时
                            else delayedCount++;
                        }
                    }
                    data.add(Map.of("name", "及时", "value", timelyCount));
                    data.add(Map.of("name", "一般", "value", normalCount));
                    data.add(Map.of("name", "延迟", "value", delayedCount));
                    break;

                default:
                    return Result.error("400", "无效的分析指标");
            }

            return Result.success(data);
        } catch (Exception e) {
            log.error("获取评论质量分析失败", e);
            return Result.error("500", "获取评论质量分析失败: " + e.getMessage());
        }
    }

    // 获取管理员列表
    @GetMapping("/all")
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String search) {
        try {
            LambdaQueryWrapper<Admin> queryWrapper = Wrappers.<Admin>lambdaQuery();
            if(StrUtil.isNotBlank(search)) {
                queryWrapper.like(Admin::getUsername, search)
                        .or()
                        .like(Admin::getNickName, search);
            }
            Page<Admin> adminPage = adminMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
            return Result.success(adminPage);
        } catch (Exception e) {
            return Result.error("500", "获取管理员列表失败: " + e.getMessage());
        }
    }

    // 更新管理员信息
    @PutMapping("/update")
    public Result<?> update(@RequestBody Admin admin) {
        try {
            adminMapper.updateById(admin);
            return Result.success();
        } catch (Exception e) {
            return Result.error("500", "更新管理员信息失败: " + e.getMessage());
        }
    }

    // 删除管理员，只有总管理员才可以删除其他管理员
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Integer id, @RequestHeader("Authorization") String token) {
        try {
            if (token == null || token.isEmpty()) {
                return Result.error("-1", "无权限操作");
            }
            Admin admin = adminMapper.selectOne(Wrappers.<Admin>lambdaQuery()
                    .eq(Admin::getToken, token));
            if(admin == null || admin.getId() != 1) {
                return Result.error("-1", "无权限操作");
            }
            adminMapper.deleteById(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error("500", "删除管理员失败: " + e.getMessage());
        }
    }

    // 全局异常处理
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        return Result.error("500", "Internal Server Error: " + e.getMessage());
    }
}