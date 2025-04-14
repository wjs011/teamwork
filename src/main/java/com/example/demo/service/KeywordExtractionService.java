package com.example.demo.service;


import com.example.demo.entity.Comment;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class KeywordExtractionService {

    public List<String> extractKeywords(String text, int topN) {
        // 使用HanLP提取关键词
        return HanLP.extractKeyword(text, topN);
    }

    public List<String> extractKeywordsFromAllComments(List<Comment> comments, int topN) {
        // 合并所有评论内容
        String combinedText = comments.stream()
                .map(Comment::getContent)
                .collect(Collectors.joining(" "));

        // 使用原有的extractKeywords方法分析
        return extractKeywords(combinedText, topN);
    }

    public List<String> extractTopNouns(String text, int topN) {
        // 分词并过滤出名词
        List<Term> termList = StandardTokenizer.segment(text);
        List<String> nouns = termList.stream()
                .filter(term -> term.nature.toString().startsWith("n")) // 名词
                .map(term -> term.word)
                .filter(word -> word.length() > 1) // 过滤单字
                .collect(Collectors.toList());

        // 统计词频
        Map<String, Long> frequencyMap = nouns.stream()
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

        // 按词频排序取前N个
        return frequencyMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }


    public Map<String, Integer> extractKeywordsFromComments(List<Comment> comments, int topN) {
//        // 合并所有评论内容
////        String allText = comments.stream()
////                .map(Comment::getContent)
////                .collect(Collectors.joining(" "));
////
////        // 分词并统计
////        List<Term> termList = StandardTokenizer.segment(allText);
////        Map<String, Integer> wordFrequency = new HashMap<>();
////
////        for (Term term : termList) {
////            String word = term.word;
////            String nature = term.nature.toString();
////
////            // 更严格的过滤条件
////            if (word.length() > 1 &&
////                    !isStopWord(word) &&
////                    isMeaningfulWord(nature, word) &&
////                    !isLowValueWord(word)) {
////                wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
////            }
////        }
////
////        // 按频率排序并取前N个
////        return wordFrequency.entrySet().stream()
////                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
////                .limit(topN)
////                .collect(Collectors.toMap(
////                        Map.Entry::getKey,
////                        Map.Entry::getValue,
////                        (e1, e2) -> e1,
////                        LinkedHashMap::new
////                ));
        // 存储名词-形容词组合及其频率
        Map<String, Integer> pairFrequency = new HashMap<>();

        // 定义名词和形容词的词性标记
        Set<String> nounTags = Set.of("n", "nb", "nba", "nbc", "nz"); // 名词类词性
        Set<String> adjTags = Set.of("a", "ad", "ag", "an"); // 形容词类词性

        for (Comment comment : comments) {
            List<Term> terms = StandardTokenizer.segment(comment.getContent());

            // 滑动窗口检查名词+形容词组合
            for (int i = 0; i < terms.size() - 1; i++) {
                Term current = terms.get(i);
                Term next = terms.get(i + 1);

                // 名词+形容词组合 (如"颜色漂亮")
                if (nounTags.contains(current.nature.toString()) &&
                        adjTags.contains(next.nature.toString())) {
                    String pair = current.word + next.word;
                    pairFrequency.put(pair, pairFrequency.getOrDefault(pair, 0) + 1);
                }

                // 形容词+名词组合 (如"漂亮颜色")
                if (adjTags.contains(current.nature.toString()) &&
                        nounTags.contains(next.nature.toString())) {
                    String pair = next.word + current.word; // 统一为名词在前
                    pairFrequency.put(pair, pairFrequency.getOrDefault(pair, 0) + 1);
                }
            }
        }

        // 过滤低频组合并按频率排序
        return pairFrequency.entrySet().stream()
                .filter(entry -> entry.getValue() > 3) // 过滤出现少于3次的组合
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(topN)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private boolean isLowValueWord(String word) {
        // 添加特定领域无意义词
        Set<String> lowValueWords = Set.of("觉得", "东西", "商品", "收到", "购买");
        return lowValueWords.contains(word);
    }

    private boolean isStopWord(String word) {
        // 简单停用词列表，实际项目中可以从文件加载更全面的列表
        Set<String> stopWords = Set.of(
                "的", "了", "和", "是", "在", "我", "有", "就", "不", "也", "这",
                "真的", "特别", "非常", "真是", "太", "款", "起来", "变得", "感觉",
                "使用", "可以", "一个", "没有", "什么", "这样", "还是", "就是", "这个", "时候"
        );
        return stopWords.contains(word);
    }

    private boolean isMeaningfulWord(String nature, String word) {
        // 只保留特定类型的有意义词汇
        return (nature.startsWith("n") || // 名词
                nature.startsWith("a") || // 形容词
                (nature.equals("l") && word.length() > 1)); // 习用语(成语等)
    }
}
