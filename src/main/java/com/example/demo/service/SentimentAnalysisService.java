package com.example.demo.service;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.seg.NShort.NShortSegment;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SentimentAnalysisService {
    
    private final NShortSegment segmenter;
    private final Map<String, Double> sentimentCache;
    
    // 使用ConcurrentHashMap提高并发性能
    private static final Map<String, Double> SENTIMENT_WORDS = new ConcurrentHashMap<>();
    private static final Map<String, Double> DEGREE_WORDS = new ConcurrentHashMap<>();
    private static final Set<String> NEGATION_WORDS = ConcurrentHashMap.newKeySet();
    
    static {
        // 初始化情感词典
        initializeSentimentWords();
        // 初始化程度副词
        initializeDegreeWords();
        // 初始化否定词
        initializeNegationWords();
    }
    
    public SentimentAnalysisService() {
        // 使用轻量级分词模型
        NShortSegment nShortSegment = new NShortSegment();
        nShortSegment.enableCustomDictionary(false);
        this.segmenter = nShortSegment;
        // 初始化缓存，设置最大容量为1000
        this.sentimentCache = new ConcurrentHashMap<>(1000);
    }
    
    private static void initializeSentimentWords() {
        // 积极词
        SENTIMENT_WORDS.put("好", 1.0);
        SENTIMENT_WORDS.put("棒", 1.0);
        SENTIMENT_WORDS.put("赞", 1.0);
        SENTIMENT_WORDS.put("优秀", 1.0);
        SENTIMENT_WORDS.put("满意", 1.0);
        SENTIMENT_WORDS.put("喜欢", 1.0);
        SENTIMENT_WORDS.put("推荐", 1.0);
        SENTIMENT_WORDS.put("不错", 1.0);
        SENTIMENT_WORDS.put("完美", 1.0);
        SENTIMENT_WORDS.put("超值", 1.0);
        
        // 消极词
        SENTIMENT_WORDS.put("差", -1.0);
        SENTIMENT_WORDS.put("烂", -1.0);
        SENTIMENT_WORDS.put("糟", -1.0);
        SENTIMENT_WORDS.put("失望", -1.0);
        SENTIMENT_WORDS.put("后悔", -1.0);
        SENTIMENT_WORDS.put("不推荐", -1.0);
        SENTIMENT_WORDS.put("不行", -1.0);
        SENTIMENT_WORDS.put("不好", -1.0);
        SENTIMENT_WORDS.put("差劲", -1.0);
        SENTIMENT_WORDS.put("垃圾", -1.0);
    }
    
    private static void initializeDegreeWords() {
        DEGREE_WORDS.put("非常", 1.5);
        DEGREE_WORDS.put("很", 1.3);
        DEGREE_WORDS.put("太", 1.3);
        DEGREE_WORDS.put("特别", 1.2);
        DEGREE_WORDS.put("比较", 0.8);
        DEGREE_WORDS.put("有点", 0.7);
        DEGREE_WORDS.put("稍微", 0.6);
        DEGREE_WORDS.put("一般", 0.5);
    }
    
    private static void initializeNegationWords() {
        NEGATION_WORDS.add("不");
        NEGATION_WORDS.add("没");
        NEGATION_WORDS.add("无");
        NEGATION_WORDS.add("非");
        NEGATION_WORDS.add("否");
        NEGATION_WORDS.add("别");
        NEGATION_WORDS.add("莫");
        NEGATION_WORDS.add("勿");
        NEGATION_WORDS.add("未");
    }

    /**
     * 分析评论情感
     * @param text 评论内容
     * @return 情感得分 (0-1之间，0表示最消极，1表示最积极)
     */
    public double analyzeSentiment(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.5;
        }
        
        // 检查缓存
        Double cachedScore = sentimentCache.get(text);
        if (cachedScore != null) {
            return cachedScore;
        }

        // 使用轻量级分词
        List<Term> terms = segmenter.seg(text);
        
        double positiveScore = 0;
        double negativeScore = 0;
        double degreeMultiplier = 1.0;
        boolean isNegated = false;
        int wordCount = 0;

        for (Term term : terms) {
            String word = term.word;
            if (word.length() < 2) continue;

            // 检查程度副词
            Double degree = DEGREE_WORDS.get(word);
            if (degree != null) {
                degreeMultiplier = degree;
                continue;
            }

            // 检查否定词
            if (NEGATION_WORDS.contains(word)) {
                isNegated = !isNegated;
                continue;
            }

            // 检查情感词
            Double sentiment = SENTIMENT_WORDS.get(word);
            if (sentiment != null) {
                wordCount++;
                double score = isNegated ? -sentiment : sentiment;
                score *= degreeMultiplier;
                
                if (score > 0) {
                    positiveScore += score;
                } else {
                    negativeScore += Math.abs(score);
                }
                
                isNegated = false;
                degreeMultiplier = 1.0;
            }
        }

        double finalScore;
        if (wordCount == 0) {
            finalScore = 0.5;
        } else {
            double totalScore = positiveScore + negativeScore;
            finalScore = totalScore == 0 ? 0.5 : positiveScore / totalScore;
            finalScore = Math.max(0.0, Math.min(1.0, finalScore));
        }

        // 存入缓存
        sentimentCache.put(text, finalScore);
        
        // 如果缓存过大，清理一半的缓存
        if (sentimentCache.size() > 1000) {
            sentimentCache.clear();
        }

        return finalScore;
    }

    /**
     * 批量分析评论情感
     * @param comments 评论列表
     * @return 评论ID到情感得分的映射
     */
    public Map<Long, Double> analyzeCommentsSentiment(List<Map<String, Object>> comments) {
        Map<Long, Double> results = new HashMap<>();
        
        for (Map<String, Object> comment : comments) {
            Long id = ((Number) comment.get("id")).longValue();
            String content = (String) comment.get("content");
            if (content != null && !content.trim().isEmpty()) {
                results.put(id, analyzeSentiment(content));
            }
        }
        
        return results;
    }
} 