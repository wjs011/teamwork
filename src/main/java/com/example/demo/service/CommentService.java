package com.example.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.Comment;
import com.example.demo.mapper.CommentMapper;
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

    public CommentService(RestTemplate restTemplate,
                          JdUrlParserService urlParserService,
                          CommentMapper commentMapper) {
        this.restTemplate = restTemplate;
        this.urlParserService = urlParserService;
        this.commentMapper = commentMapper;
    }

    /**
     * 从京东获取评论并保存到数据库
     */
    @Transactional
    public Result<List<Comment>> fetchAndSaveComments(String url, int maxPages) {
        String productId = urlParserService.getProductIdFromUrl(url);
        if (productId == null) {
            return Result.error("400", "无法从URL中解析商品ID");
        }

        List<Comment> allComments = new ArrayList<>();

        for (int page = 0; page <= maxPages; page++) {
            try {
                Optional<List<Comment>> comments = fetchCommentsFromJd(productId, page);
                if (comments.isPresent() && !comments.get().isEmpty()) {
                    // 使用我们自定义的批量插入方法
                    commentMapper.batchInsert(comments.get());
                    allComments.addAll(comments.get());
                } else {
                    break;
                }

                Thread.sleep(2000);
            } catch (Exception e) {
                log.error("获取评论失败: {}", e.getMessage());
                return Result.error("500", "获取评论失败: " + e.getMessage());
            }
        }

        return Result.success(allComments);
    }

    /**
     * 从数据库分页查询评论
     */
    public Result<Page<Comment>> getCommentsFromDb(int pageNum, int pageSize) {
        try {
            Page<Comment> page = new Page<>(pageNum, pageSize);
            Page<Comment> result = commentMapper.findPage(page);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询评论失败: {}", e.getMessage());
            return Result.error("500", "查询评论失败: " + e.getMessage());
        }
    }

    /**
     * 从京东API获取评论
     */
    private Optional fetchCommentsFromJd(String productId, int page) {
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
    private Optional<List<Comment>> parseComments(Map<String, Object> jsonData) {
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
            return Optional.of(comments);
        }
        return Optional.empty();
    }
}
