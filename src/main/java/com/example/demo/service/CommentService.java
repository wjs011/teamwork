package com.example.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.Comment;
import com.example.demo.entity.JdCommentResponse;
import com.example.demo.entity.Product;
import com.example.demo.mapper.CommentMapper;
import com.example.demo.mapper.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

@Service
@Slf4j
public class CommentService {
    private final RestTemplate restTemplate;
    private final JdUrlParserService urlParserService;
    private final CommentMapper commentMapper;
    private final ProductMapper productMapper;

    public CommentService(RestTemplate restTemplate,
                          JdUrlParserService urlParserService,
                          CommentMapper commentMapper,
                          ProductMapper productMapper
                         ) {
        this.restTemplate = restTemplate;
        this.urlParserService = urlParserService;
        this.commentMapper = commentMapper;
        this.productMapper = productMapper;

    }

    @Transactional
    public Result<List<Comment>> fetchAndSaveComments(String url) {
        String productId = urlParserService.getProductIdFromUrl(url);
        if (productId == null) {
            return Result.error("400", "无法从URL中解析商品ID");
        }

        // 检查或创建商品记录
        Product product = productMapper.selectById(productId);
        if (product == null) {
            product = new Product();
            product.setId(productId);
            product.setUrl(url);
            product.setName("京东商品-" + productId);
            productMapper.insert(product);
        }

        // 创建商品评论表（如果不存在）
        commentMapper.createCommentTableForProduct(productId);

        // 检查数据库中是否已有评论数据
        int existingCommentsCount = commentMapper.countByProductId(productId);
        if (existingCommentsCount > 0) {
            // 如果数据库已有评论，直接返回第一页数据
            List<Comment> comments = commentMapper.selectByProductIdWithLimit(productId, 0, 10);
            if (comments != null && !comments.isEmpty()) {
                return Result.success(comments);
            }
            // 如果查询结果为空，继续执行爬取逻辑
        }

        // 如果数据库没有评论，则开始爬取
        List<Comment> allComments = new ArrayList<>();
        int maxPages = 0;
        boolean hasMore = true;

        // 首先获取第一页数据，从中提取最大页数
        try {
            Optional<JdCommentResponse> firstPageResponse = fetchCommentsFromJd(productId, 0);
            if (!firstPageResponse.isPresent()) {
                return Result.error("404", "未获取到商品评论数据");
            }

            JdCommentResponse response = firstPageResponse.get();
            maxPages = response.getMaxPage(); // 获取最大页数
            if (response.getComments() != null && !response.getComments().isEmpty()) {
                response.getComments().forEach(c -> c.setProductId(productId));
                commentMapper.batchInsertForProduct(productId, response.getComments());
                allComments.addAll(response.getComments());
            }

            // 从第二页开始爬取剩余页数
            for (int page = 1; page < maxPages && hasMore; page++) {
                try {
                    Optional<JdCommentResponse> pageResponse = fetchCommentsFromJd(productId, page);
                    if (pageResponse.isPresent() && !pageResponse.get().getComments().isEmpty()) {
                        List<Comment> comments = pageResponse.get().getComments();
                        comments.forEach(c -> c.setProductId(productId));
                        commentMapper.batchInsertForProduct(productId, comments);
                        allComments.addAll(comments);
                    } else {
                        hasMore = false;
                    }
                    Thread.sleep(2000); // 防止请求过于频繁
                } catch (Exception e) {
                    log.error("获取评论失败(页{}): {}", page, e.getMessage());
                    // 继续尝试下一页
                }
            }

            if (allComments.isEmpty()) {
                return Result.error("404", "未获取到商品评论数据");
            }
            return Result.success(allComments);
        } catch (Exception e) {
            log.error("获取评论失败: {}", e.getMessage());
            return Result.error("500", "获取评论失败: " + e.getMessage());
        }
    }

    /**
     * 从京东API获取评论
     */
    private Optional<JdCommentResponse> fetchCommentsFromJd(String productId, int page) {
        String url = String.format(
                "https://club.jd.com/comment/productPageComments.action?productId=%s&score=0&sortType=5&page=%d&pageSize=10",
                productId, page);

        HttpHeaders headers = new HttpHeaders();
        // 添加更多请求头，模拟真实浏览器
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        headers.set("Accept", "application/json, text/javascript, */*; q=0.01");
        headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.set("Accept-Encoding", "gzip, deflate, br");
        headers.set("Connection", "keep-alive");
        headers.set("Referer", "https://item.jd.com/" + productId + ".html");
        headers.set("Origin", "https://item.jd.com");
        headers.set("X-Requested-With", "XMLHttpRequest");
        headers.set("sec-ch-ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"96\", \"Google Chrome\";v=\"96\"");
        headers.set("sec-ch-ua-mobile", "?0");
        headers.set("sec-ch-ua-platform", "\"Windows\"");
        headers.set("Sec-Fetch-Dest", "empty");
        headers.set("Sec-Fetch-Mode", "cors");
        headers.set("Sec-Fetch-Site", "same-site");

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String responseBody = response.getBody();
                
                // 检查是否是错误响应
                if (responseBody.contains("系统繁忙") || responseBody.contains("访问受限")) {
                    log.warn("京东API返回错误: {}", responseBody);
                    // 等待一段时间后重试
                    Thread.sleep(5000);
                    return fetchCommentsFromJd(productId, page);
                }
                
                // 使用 Jackson 手动解析 JSON 字符串
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> jsonData = mapper.readValue(responseBody, Map.class);
                return parseComments(jsonData);
            }
        } catch (Exception e) {
            log.error("调用京东API失败: {}", e.getMessage());
            // 如果是解析错误，可能是系统繁忙，等待后重试
            if (e.getMessage().contains("系统繁忙")) {
                try {
                    Thread.sleep(5000);
                    return fetchCommentsFromJd(productId, page);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private Optional<JdCommentResponse> parseComments(Map<String, Object> jsonData) {
        if (jsonData.containsKey("comments")) {
            List<Map<String, Object>> rawComments = (List<Map<String, Object>>) jsonData.get("comments");
            List<Comment> comments = new ArrayList<>();

            for (Map<String, Object> rawComment : rawComments) {
                Comment comment = new Comment();
                comment.setContent((String) rawComment.get("content"));
                comment.setNickname((String) rawComment.get("nickname"));
                comment.setScore((Integer) rawComment.get("score"));
                comments.add(comment);
            }

            JdCommentResponse response = new JdCommentResponse();
            response.setComments(comments);
            response.setMaxPage((Integer) jsonData.getOrDefault("maxPage", 0));
            response.setProductCommentSummary((Map<String, Object>) jsonData.getOrDefault("productCommentSummary", new HashMap<>()));

            return Optional.of(response);
        }
        return Optional.empty();
    }

    public Result<Page<Comment>> getCommentsFromDb(int pageNum, int pageSize, String productId) {
        try {
            if (productId == null || productId.trim().isEmpty()) {
                // 查询所有商品ID
                List<String> productIds = productMapper.selectList(null)
                        .stream().map(Product::getId).toList();
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
                // 按时间倒序排序
                allComments.sort((a, b) -> {
                    if (a.getCreateTime() == null || b.getCreateTime() == null) return 0;
                    return b.getCreateTime().compareTo(a.getCreateTime());
                });
                int total = allComments.size();
                int fromIndex = Math.max(0, (pageNum - 1) * pageSize);
                int toIndex = Math.min(fromIndex + pageSize, total);
                List<Comment> pageRecords = fromIndex < toIndex ? allComments.subList(fromIndex, toIndex) : new ArrayList<>();
                Page<Comment> page = new Page<>(pageNum, pageSize, total);
                page.setRecords(pageRecords);
                return Result.success(page);
            } else {
                // 查询特定商品评论表
                int total = commentMapper.countByProductId(productId);
                Page<Comment> page = new Page<>(pageNum, pageSize, total);
                // 计算偏移量
                long offset = (pageNum - 1) * pageSize;
                List<Comment> records = commentMapper.selectByProductIdWithLimit(productId, offset, pageSize);
                page.setRecords(records);
                return Result.success(page);
            }
        } catch (Exception e) {
            log.error("查询评论失败: {}", e.getMessage());
            return Result.error("500", "查询评论失败: " + e.getMessage());
        }
    }

    /**
     * 根据评分范围查询评论
     */
    public Result<List<Comment>> getCommentsByScoreRange(String productId, int minScore, int maxScore) {
        try {
            if (productId == null || productId.trim().isEmpty()) {
                // 查询所有商品ID
                List<String> productIds = productMapper.selectList(null)
                        .stream().map(Product::getId).toList();
                List<Comment> allComments = new ArrayList<>();
                for (String pid : productIds) {
                    try {
                        List<Comment> comments = commentMapper.selectByProductIdAndScoreRange(pid, minScore, maxScore);
                        if (comments != null) {
                            allComments.addAll(comments);
                        }
                    } catch (Exception e) {
                        // 某个商品表不存在时跳过
                    }
                }
                // 按时间倒序排序
                allComments.sort((a, b) -> {
                    if (a.getCreateTime() == null || b.getCreateTime() == null) return 0;
                    return b.getCreateTime().compareTo(a.getCreateTime());
                });
                return Result.success(allComments);
            } else {
                List<Comment> comments = commentMapper.selectByProductIdAndScoreRange(productId, minScore, maxScore);
                return Result.success(comments);
            }
        } catch (Exception e) {
            log.error("根据评分查询评论失败: {}", e.getMessage());
            return Result.error("500", "根据评分查询评论失败: " + e.getMessage());
        }
    }

    /**
     * 根据关键词查询评论
     */
    public Result<List<Comment>> searchCommentsByKeyword(String productId, String keyword) {
        try {
            if (productId == null || productId.trim().isEmpty()) {
                // 查询所有商品ID
                List<String> productIds = productMapper.selectList(null)
                        .stream().map(Product::getId).toList();
                List<Comment> allComments = new ArrayList<>();
                for (String pid : productIds) {
                    try {
                        List<Comment> comments = commentMapper.selectByProductIdAndKeyword(pid, keyword);
                        if (comments != null) {
                            allComments.addAll(comments);
                        }
                    } catch (Exception e) {
                        // 某个商品表不存在时跳过
                    }
                }
                // 按时间倒序排序
                allComments.sort((a, b) -> {
                    if (a.getCreateTime() == null || b.getCreateTime() == null) return 0;
                    return b.getCreateTime().compareTo(a.getCreateTime());
                });
                return Result.success(allComments);
            } else {
                List<Comment> comments = commentMapper.selectByProductIdAndKeyword(productId, keyword);
                return Result.success(comments);
            }
        } catch (Exception e) {
            log.error("根据关键词查询评论失败: {}", e.getMessage());
            return Result.error("500", "根据关键词查询评论失败: " + e.getMessage());
        }
    }

    /**
     * 删除评论
     */
    @Transactional
    public Result<String> deleteComment(String productId, Long commentId) {
        try {
            int affectedRows = commentMapper.deleteComment(productId, commentId);
            if (affectedRows > 0) {
                return Result.success("评论删除成功");
            } else {
                return Result.error("404", "未找到指定评论");
            }
        } catch (Exception e) {
            log.error("删除评论失败: {}", e.getMessage());
            return Result.error("500", "删除评论失败: " + e.getMessage());
        }
    }
}

