package com.example.demo.service;

import com.example.demo.entity.Comment;
import com.example.demo.entity.CommentCategory;
import com.example.demo.mapper.CommentCategoryMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentCategoryService {
    private final CommentCategoryMapper categoryMapper;
    private final ObjectMapper objectMapper;

    public CommentCategoryService(CommentCategoryMapper categoryMapper, ObjectMapper objectMapper) {
        this.categoryMapper = categoryMapper;
        this.objectMapper = objectMapper;
    }

    // 获取所有分类
    public List<CommentCategory> getAllCategories() {
        return categoryMapper.selectAllCategories();
    }

    // 获取单个分类
    public CommentCategory getCategoryById(Long id) {
        return categoryMapper.selectById(id);
    }

    // 创建分类
    @Transactional
    public CommentCategory createCategory(CommentCategory category) {
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.insert(category);
        return category;
    }

    // 更新分类
    @Transactional
    public CommentCategory updateCategory(CommentCategory category) {
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.update(category);
        return category;
    }

    // 删除分类
    @Transactional
    public void deleteCategory(Long id) {
        categoryMapper.deleteById(id);
    }

    // 对评论进行分类
    public Map<Long, List<String>> categorizeComments(List<Comment> comments) {
        List<CommentCategory> categories = categoryMapper.selectAllCategories();
        Map<Long, List<String>> commentCategories = new HashMap<>();

        for (Comment comment : comments) {
            List<String> matchedCategories = new ArrayList<>();
            String content = comment.getContent();

            if (content != null) {
                for (CommentCategory category : categories) {
                    try {
                        List<String> keywords = objectMapper.readValue(
                            category.getKeywords(),
                            new TypeReference<List<String>>() {}
                        );

                        // 检查评论内容是否包含分类的关键词
                        boolean matches = keywords.stream()
                            .anyMatch(keyword -> content.contains(keyword));

                        if (matches) {
                            matchedCategories.add(category.getName());
                        }
                    } catch (Exception e) {
                        // 处理JSON解析错误
                        e.printStackTrace();
                    }
                }
            }

            commentCategories.put(comment.getId(), matchedCategories);
        }

        return commentCategories;
    }

    // 获取评论的分类统计
    public Map<String, Integer> getCategoryStatistics(List<Comment> comments) {
        Map<Long, List<String>> categorizedComments = categorizeComments(comments);
        Map<String, Integer> statistics = new HashMap<>();

        // 统计每个分类的评论数量
        for (List<String> categories : categorizedComments.values()) {
            for (String category : categories) {
                statistics.merge(category, 1, Integer::sum);
            }
        }

        return statistics;
    }

    // 获取评论的分类趋势
    public Map<String, Map<String, Integer>> getCategoryTrends(
            List<Comment> comments,
            String timeRange) {
        Map<String, Map<String, Integer>> trends = new HashMap<>();
        List<CommentCategory> categories = categoryMapper.selectAllCategories();

        // 按时间范围分组评论
        Map<String, List<Comment>> commentsByDate = comments.stream()
            .collect(Collectors.groupingBy(comment -> {
                LocalDateTime createTime = comment.getCreateTime();
                if (createTime == null) return "unknown";
                
                if ("month".equals(timeRange)) {
                    return createTime.toLocalDate().toString();
                } else {
                    return createTime.getYear() + "-" + 
                           String.format("%02d", createTime.getMonthValue());
                }
            }));

        // 对每个时间段的评论进行分类统计
        for (Map.Entry<String, List<Comment>> entry : commentsByDate.entrySet()) {
            String date = entry.getKey();
            List<Comment> dateComments = entry.getValue();
            
            Map<String, Integer> dateStats = new HashMap<>();
            for (CommentCategory category : categories) {
                try {
                    List<String> keywords = objectMapper.readValue(
                        category.getKeywords(),
                        new TypeReference<List<String>>() {}
                    );

                    int count = (int) dateComments.stream()
                        .filter(comment -> comment.getContent() != null &&
                            keywords.stream().anyMatch(keyword -> 
                                comment.getContent().contains(keyword)))
                        .count();

                    dateStats.put(category.getName(), count);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            trends.put(date, dateStats);
        }

        return trends;
    }
} 