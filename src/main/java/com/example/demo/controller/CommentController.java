package com.example.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.Comment;
import com.example.demo.mapper.CommentMapper;
import com.example.demo.service.CommentService;
import com.example.demo.service.KeywordExtractionService;
import com.example.demo.service.SentimentAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/comments")
@CrossOrigin(originPatterns = "*", allowedHeaders = "*")
public class CommentController {
    private static final Logger log = LoggerFactory.getLogger(CommentController.class);
    private final CommentService commentService;
    private final CommentMapper commentMapper;
    private final KeywordExtractionService keywordExtractionService;
    private final SentimentAnalysisService sentimentAnalysisService;
    private final RestTemplate restTemplate;
    private final String kimiApiUrl;
    private final String kimiApiKey;

    // 构造函数注入所有依赖
    public CommentController(CommentService commentService,
                             CommentMapper commentMapper,
                             KeywordExtractionService keywordExtractionService,
                             SentimentAnalysisService sentimentAnalysisService,
                             RestTemplate restTemplate,
                             @Value("${kimi.api.url}") String kimiApiUrl,
                             @Value("${kimi.api.key}") String kimiApiKey) {
        this.commentService = commentService;
        this.commentMapper = commentMapper;
        this.keywordExtractionService = keywordExtractionService;
        this.sentimentAnalysisService = sentimentAnalysisService;
        this.restTemplate = restTemplate;
        this.kimiApiUrl = kimiApiUrl;
        this.kimiApiKey = kimiApiKey;
    }

    // 原有方法保持不变...
    @GetMapping("/fetch")
    public Result<List<Comment>> fetchComments(@RequestParam String url) {
        return commentService.fetchAndSaveComments(url);
    }

    @GetMapping("")
    public Result<Page<Comment>> getComments(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer minScore,
            @RequestParam(required = false) Integer maxScore) {
        return commentService.getCommentsFromDb(pageNum, pageSize, productId, startDate, endDate, minScore, maxScore);
    }

    // 关键词提取接口
    @GetMapping("/keywords1/{productId}")
    public Result<Map<String, Object>> getKeywordsForWordCloud(
            @PathVariable String productId,
            @RequestParam(defaultValue = "50") int topN) {

        List<Comment> comments = commentMapper.selectByProductId(productId);

        if (comments == null || comments.isEmpty()) {
            return Result.error("404", "该商品暂无评论");
        }

        Map<String, Integer> keywords = keywordExtractionService.extractKeywordsFromComments(comments, topN);

        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("totalComments", comments.size());
        result.put("keywords", keywords);

        return Result.success(result);
    }

    @GetMapping("/keywords2/{productId}")
    public Result<List<String>> getKeywordsByTfIdf(
            @PathVariable String productId,
            @RequestParam(defaultValue = "20") int topN) {

        List<Comment> comments = commentMapper.selectByProductId(productId);

        if (comments == null || comments.isEmpty()) {
            return Result.error("404", "该商品暂无评论");
        }

        // 使用方法1的实现
        List<String> keywords = keywordExtractionService.extractKeywordsFromAllComments(comments, topN);

        return Result.success(keywords);
    }
    /**
     * 根据评分范围查询评论
     * @param productId 商品ID
     * @param minScore 最低评分(1-5)
     * @param maxScore 最高评分(1-5)
     */
    @GetMapping("/score/{productId}")
    public Result<List<Comment>> getCommentsByScore(
            @PathVariable String productId,
            @RequestParam(defaultValue = "1") int minScore,
            @RequestParam(defaultValue = "5") int maxScore) {
        return commentService.getCommentsByScoreRange(productId, minScore, maxScore);
    }
    @GetMapping("/score")
    public Result<List<Comment>> getallCommentsByScore(
            @RequestParam(defaultValue = "1") int minScore,
            @RequestParam(defaultValue = "5") int maxScore) {
        return commentService.getCommentsByScoreRange(null, minScore, maxScore);
    }

    /**
     * 根据关键词搜索评论
     * @param productId 商品ID
     * @param keyword 搜索关键词
     */
    @GetMapping("/search/{productId}")
    public Result<List<Comment>> searchComments(
            @PathVariable String productId,
            @RequestParam String keyword) {
        return commentService.searchCommentsByKeyword(productId, keyword);
    }
    @GetMapping("/search")
    public Result<List<Comment>> searchallComments(

            @RequestParam String keyword) {
        return commentService.searchCommentsByKeyword(null, keyword);
    }
    /**
     * 删除评论
     * @param productId 商品ID
     * @param commentId 评论ID
     */
    @DeleteMapping("/{productId}/{commentId}")
    public Result<String> deleteComment(
            @PathVariable String productId,
            @PathVariable Long commentId) {
        return commentService.deleteComment(productId, commentId);
    }

    /**
     * 情感分析接口
     * @param productId 商品ID
     * @param request 包含评论列表的请求体
     * @return 评论ID到情感得分的映射
     */
    @PostMapping("/sentiment/{productId}")
    public Result<Map<Long, Double>> analyzeSentiment(
            @PathVariable String productId,
            @RequestBody Map<String, List<Map<String, Object>>> request) {
        try {
            List<Map<String, Object>> comments = request.get("comments");
            if (comments == null || comments.isEmpty()) {
                return Result.error("400", "评论列表不能为空");
            }
            
            Map<Long, Double> sentimentResults = new HashMap<>();
            for (Map<String, Object> comment : comments) {
                Long id = ((Number) comment.get("id")).longValue();
                String content = (String) comment.get("content");
                if (content != null && !content.trim().isEmpty()) {
                    double score = sentimentAnalysisService.analyzeSentiment(content);
                    sentimentResults.put(id, score);
                }
            }
            
            return Result.success(sentimentResults);
        } catch (Exception e) {
            return Result.error("500", "情感分析失败: " + e.getMessage());
        }
    }

    @GetMapping("/summary/{productId}")
    public Result<?> getCommentSummary(
            @PathVariable String productId,
            @RequestParam(defaultValue = "all") String timeRange,
            @RequestParam(defaultValue = "0") String scoreFilter,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        List<Comment> comments = commentMapper.selectByProductId(productId);
        
        // 根据时间范围过滤
        if (!timeRange.equals("all")) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime;
            switch (timeRange) {
                case "week":
                    startTime = now.minusWeeks(1);
                    break;
                case "month":
                    startTime = now.minusMonths(1);
                    break;
                case "quarter":
                    startTime = now.minusMonths(3);
                    break;
                default:
                    startTime = now.minusYears(100);
            }
            comments = comments.stream()
                .filter(comment -> comment.getCreateTime() != null && comment.getCreateTime().isAfter(startTime))
                .collect(Collectors.toList());
        }

        // 根据评分过滤
        if (!scoreFilter.equals("0")) {
            int minScore = Integer.parseInt(scoreFilter);
            comments = comments.stream()
                .filter(comment -> comment.getScore() != null && comment.getScore() >= minScore)
                .collect(Collectors.toList());
        }

        // 如果没有评论数据，返回空结果
        if (comments.isEmpty()) {
            Map<String, Object> emptySummary = new HashMap<>();
            emptySummary.put("totalComments", 0);
            emptySummary.put("averageScore", 0.0);
            emptySummary.put("positiveRate", 0.0);
            emptySummary.put("negativeRate", 0.0);
            emptySummary.put("keyPoints", new ArrayList<>());
            emptySummary.put("total", 0);
            emptySummary.put("current", pageNum);
            emptySummary.put("size", pageSize);
            return Result.success(emptySummary);
        }

        // 计算统计信息
        int totalComments = comments.size();
        double averageScore = comments.stream()
            .filter(comment -> comment.getScore() != null)
            .mapToInt(Comment::getScore)
            .average()
            .orElse(0.0);

        // 计算情感分布
        long positiveCount = comments.stream()
            .filter(comment -> comment.getScore() != null && comment.getScore() >= 4)
            .count();
        long negativeCount = comments.stream()
            .filter(comment -> comment.getScore() != null && comment.getScore() <= 2)
            .count();
        double positiveRate = totalComments > 0 ? (double) positiveCount / totalComments * 100 : 0;
        double negativeRate = totalComments > 0 ? (double) negativeCount / totalComments * 100 : 0;

        // 提取核心观点
        List<Map<String, Object>> allKeyPoints = extractKeyPoints(comments);
        
        // 计算分页
        int total = allKeyPoints.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        
        // 获取当前页的核心观点
        List<Map<String, Object>> pagedKeyPoints = fromIndex < total ? 
            allKeyPoints.subList(fromIndex, toIndex) : new ArrayList<>();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalComments", totalComments);
        summary.put("averageScore", averageScore);
        summary.put("positiveRate", positiveRate);
        summary.put("negativeRate", negativeRate);
        summary.put("keyPoints", pagedKeyPoints);
        summary.put("total", total);
        summary.put("current", pageNum);
        summary.put("size", pageSize);

        return Result.success(summary);
    }

    private List<Map<String, Object>> extractKeyPoints(List<Comment> comments) {
        Map<String, List<Comment>> keywordGroups = new HashMap<>();
        
        // 扩展关键词列表
        String[] keywords = {
            "质量", "价格", "物流", "服务", "外观", "功能", "性价比", "做工", "材质",
            "包装", "尺寸", "颜色", "味道", "口感", "效果", "使用", "体验", "售后",
            "发货", "速度", "客服", "态度", "问题", "建议", "推荐", "满意", "失望"
        };
        
        for (Comment comment : comments) {
            if (comment.getContent() == null) continue;
            String content = comment.getContent();
            for (String keyword : keywords) {
                if (content.contains(keyword)) {
                    keywordGroups.computeIfAbsent(keyword, k -> new ArrayList<>()).add(comment);
                }
            }
        }

        List<Map<String, Object>> keyPoints = new ArrayList<>();
        for (Map.Entry<String, List<Comment>> entry : keywordGroups.entrySet()) {
            List<Comment> groupComments = entry.getValue();
            if (groupComments.size() >= 2) { // 降低阈值到2，以显示更多观点
                Map<String, Object> point = new HashMap<>();
                point.put("title", entry.getKey());
                point.put("content", generateSummary(groupComments));
                point.put("count", groupComments.size());
                point.put("score", groupComments.stream()
                    .filter(comment -> comment.getScore() != null)
                    .mapToInt(Comment::getScore)
                    .average()
                    .orElse(0.0));
                keyPoints.add(point);
            }
        }

        // 按提及次数排序
        keyPoints.sort((a, b) -> ((Integer) b.get("count")).compareTo((Integer) a.get("count")));
        return keyPoints;
    }

    private String generateSummary(List<Comment> comments) {
        if (comments.isEmpty()) {
            return "";
        }

        // 选择评分最高的评论作为摘要
        return comments.stream()
            .filter(comment -> comment.getScore() != null)
            .max(Comparator.comparingInt(Comment::getScore))
            .map(Comment::getContent)
            .orElse("");
    }

    @PostMapping("/compare")
    public Result<?> compareComments(@RequestBody Map<String, List<String>> request) {
        try {
            List<String> productIds = request.get("productIds");
            if (productIds == null || productIds.isEmpty()) {
                return Result.error("400", "请选择要对比的商品");
            }

            System.out.println("开始对比商品: " + productIds);

            List<Map<String, Object>> compareData = new ArrayList<>();
            for (String productId : productIds) {
                System.out.println("处理商品: " + productId);
                List<Comment> comments = commentMapper.selectByProductId(productId);
                if (comments == null || comments.isEmpty()) {
                    System.out.println("商品 " + productId + " 没有评论数据");
                    continue;
                }

                System.out.println("商品 " + productId + " 评论数量: " + comments.size());

                // 计算基础统计数据
                int totalComments = comments.size();
                double averageScore = comments.stream()
                    .filter(comment -> comment.getScore() != null)
                    .mapToInt(Comment::getScore)
                    .average()
                    .orElse(0.0);

                // 计算情感分布
                long positiveCount = comments.stream()
                    .filter(comment -> comment.getScore() != null && comment.getScore() >= 4)
                    .count();
                long negativeCount = comments.stream()
                    .filter(comment -> comment.getScore() != null && comment.getScore() <= 2)
                    .count();
                double positiveRate = totalComments > 0 ? (double) positiveCount / totalComments * 100 : 0;
                double negativeRate = totalComments > 0 ? (double) negativeCount / totalComments * 100 : 0;

                // 提取关键词
                Map<String, Integer> keywords = keywordExtractionService.extractKeywordsFromComments(comments, 10);
                System.out.println("商品 " + productId + " 关键词: " + keywords);

                // 将关键词出现次数转换为百分比
                Map<String, Double> keywordPercentages = new HashMap<>();
                for (Map.Entry<String, Integer> entry : keywords.entrySet()) {
                    double percentage = (double) entry.getValue() / totalComments * 100;
                    keywordPercentages.put(entry.getKey(), percentage);
                }

                // 获取商品名称
                String productName = commentMapper.selectProductName(productId);
                if (productName == null) {
                    productName = "商品" + productId;
                }

                Map<String, Object> productData = new HashMap<>();
                productData.put("id", productId);
                productData.put("name", productName);
                productData.put("totalComments", totalComments);
                productData.put("averageScore", averageScore);
                productData.put("positiveRate", positiveRate);
                productData.put("negativeRate", negativeRate);
                productData.put("keywords", keywordPercentages);

                compareData.add(productData);
                System.out.println("商品 " + productId + " 数据处理完成");
            }

            System.out.println("对比数据生成完成: " + compareData);
            return Result.success(compareData);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("500", "生成对比数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/products")
    public Result<?> getProducts() {
        try {
            // 获取所有商品表名
            List<String> productIds = new ArrayList<>();
            List<Map<String, Object>> tables = commentMapper.selectAllCommentTables();
            
            // 从表名中提取商品ID
            for (Map<String, Object> table : tables) {
                String tableName = (String) table.get("TABLE_NAME");
                String productId = tableName.substring("comment_".length());
                productIds.add(productId);
            }
            
            // 获取每个商品的名称
            List<Map<String, Object>> products = new ArrayList<>();
            for (String productId : productIds) {
                Map<String, Object> product = new HashMap<>();
                product.put("id", productId);
                // 从product表获取商品名称
                String productName = commentMapper.selectProductName(productId);
                product.put("name", productName != null ? productName : "商品" + productId);
                products.add(product);
            }
            
            return Result.success(products);
        } catch (Exception e) {
            return Result.error("500", "获取商品列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/ai-analysis")
    public Result aiAnalysis(@RequestBody Map<String, Object> request) {
        try {
            List<Map<String, Object>> products = (List<Map<String, Object>>) request.get("products");
            if (products == null || products.isEmpty()) {
                return Result.error("400", "请选择要分析的商品");
            }

            // 构建提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append("请根据以下商品评论数据生成一份竞品分析报告，使用纯文本格式，不要使用表格或特殊格式。报告应包含以下三个部分：\n\n");
            prompt.append("1. 总体评价对比：比较各商品的评论数量、平均评分、正面评价率等基础数据。\n");
            prompt.append("2. 优势分析：分析各商品的主要优势，包括关键词分布和用户反馈。\n");
            prompt.append("3. 改进建议：针对各商品的不足提出具体的改进建议。\n\n");
            prompt.append("商品数据如下：\n");

            for (Map<String, Object> product : products) {
                prompt.append(String.format("商品%s（%s）：\n", product.get("id"), product.get("name")));
                prompt.append(String.format("- 评论总数：%d\n", product.get("totalComments")));
                prompt.append(String.format("- 平均评分：%.1f\n", product.get("averageScore")));
                prompt.append(String.format("- 正面评价率：%.1f%%\n", product.get("positiveRate")));
                prompt.append(String.format("- 负面评价率：%.1f%%\n", product.get("negativeRate")));
                
                Map<String, Double> keywords = (Map<String, Double>) product.get("keywords");
                if (keywords != null && !keywords.isEmpty()) {
                    prompt.append("- 关键词分布：\n");
                    keywords.forEach((keyword, percentage) -> 
                        prompt.append(String.format("  * %s: %.1f%%\n", keyword, percentage))
                    );
                }
                prompt.append("\n");
            }

            // 调用Kimi接口
            String apiUrl = kimiApiUrl;
            String apiKey = kimiApiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "moonshot-v1-8k");
            requestBody.put("messages", Collections.singletonList(Map.of(
                "role", "user",
                "content", prompt.toString()
            )));
            requestBody.put("stream", false);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    String content = (String) message.get("content");
                    
                    List<Map<String, Object>> result = new ArrayList<>();
                    result.add(Map.of("content", content));
                    return Result.success(result);
                }
            }
            
            return Result.error("500", "AI分析失败");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("500", "AI分析失败: " + e.getMessage());
        }
    }
}