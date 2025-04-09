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
                          ProductMapper productMapper) {
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
                        pageResponse.get().getComments().forEach(c -> c.setProductId(productId));
                        commentMapper.batchInsertForProduct(productId, pageResponse.get().getComments());
                        allComments.addAll(pageResponse.get().getComments());
                    } else {
                        hasMore = false;
                    }
                    Thread.sleep(2000); // 防止请求过于频繁
                } catch (Exception e) {
                    log.error("获取评论失败(页{}): {}", page, e.getMessage());
                    // 继续尝试下一页
                }
            }
        } catch (Exception e) {
            log.error("获取评论失败: {}", e.getMessage());
            return Result.error("500", "获取评论失败: " + e.getMessage());
        }

        return Result.success(allComments);
    }

    /**
     * 从京东API获取评论
     */
    private Optional<JdCommentResponse> fetchCommentsFromJd(String productId, int page) {
        String url = String.format(
                "https://club.jd.com/comment/productPageComments.action?productId=%s&score=0&sortType=5&page=%d&pageSize=10",
                productId, page);

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return parseComments(response.getBody());
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
            if (productId != null && !productId.isEmpty()) {
                // 查询特定商品评论表
                int total = commentMapper.countByProductId(productId);
                Page<Comment> page = new Page<>(pageNum, pageSize, total);

                // 计算偏移量
                long offset = (pageNum - 1) * pageSize;
                List<Comment> records = commentMapper.selectByProductIdWithLimit(productId, offset, pageSize);

                page.setRecords(records);
                return Result.success(page);
            } else {
                // 查询所有商品评论（从各个表联合查询，这里简化为只查询主表）
                Page<Comment> page = new Page<>(pageNum, pageSize);
                return Result.success(page);
            }
        } catch (Exception e) {
            log.error("查询评论失败: {}", e.getMessage());
            return Result.error("500", "查询评论失败: " + e.getMessage());
        }
    }


}