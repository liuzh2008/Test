package com.example.medaiassistant.util;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Component;

/**
 * 字符串相似度计算工具类
 * 使用Apache Commons Text的Levenshtein距离算法
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-16
 */
@Component
public class LevenshteinUtil {
    
    private final LevenshteinDistance levenshteinDistance;
    
    /**
     * 默认构造函数
     * 使用默认的Levenshtein距离计算器（无阈值限制）
     */
    public LevenshteinUtil() {
        this.levenshteinDistance = LevenshteinDistance.getDefaultInstance();
    }
    
    /**
     * 带阈值限制的构造函数
     * 
     * @param threshold 最大距离阈值，超过此值返回-1
     */
    public LevenshteinUtil(Integer threshold) {
        this.levenshteinDistance = LevenshteinDistance.getDefaultInstance();
    }
    
    /**
     * 计算两个字符串的Levenshtein距离
     * 
     * @param s1 字符串1
     * @param s2 字符串2
     * @return Levenshtein距离，如果任一字符串为null则返回-1
     */
    public int calculateDistance(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return -1;
        }
        return levenshteinDistance.apply(s1, s2);
    }
    
    /**
     * 计算两个字符串的相似度
     * 相似度 = 1 - (距离 / max(len1, len2))
     * 
     * @param s1 字符串1
     * @param s2 字符串2
     * @return 相似度（0.0到1.0之间），如果任一字符串为null则返回0.0
     */
    public double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        
        if (s1.isEmpty() && s2.isEmpty()) {
            return 1.0;
        }
        
        int distance = calculateDistance(s1, s2);
        if (distance == -1) {
            return 0.0;
        }
        
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) {
            return 1.0;
        }
        
        return 1.0 - ((double) distance / maxLength);
    }
    
    /**
     * 计算两个字符串的相似度百分比
     * 
     * @param s1 字符串1
     * @param s2 字符串2
     * @return 相似度百分比（0到100），如果任一字符串为null则返回0
     */
    public int calculateSimilarityPercentage(String s1, String s2) {
        double similarity = calculateSimilarity(s1, s2);
        return (int) Math.round(similarity * 100);
    }
    
    /**
     * 判断两个字符串是否相似（基于阈值）
     * 
     * @param s1 字符串1
     * @param s2 字符串2
     * @param threshold 相似度阈值（0.0到1.0）
     * @return 如果相似度大于等于阈值则返回true
     */
    public boolean isSimilar(String s1, String s2, double threshold) {
        double similarity = calculateSimilarity(s1, s2);
        return similarity >= threshold;
    }
    
    /**
     * 判断两个字符串是否高度相似（阈值0.8）
     * 
     * @param s1 字符串1
     * @param s2 字符串2
     * @return 如果相似度大于等于0.8则返回true
     */
    public boolean isHighlySimilar(String s1, String s2) {
        return isSimilar(s1, s2, 0.8);
    }
    
    /**
     * 判断两个字符串是否中等相似（阈值0.5）
     * 
     * @param s1 字符串1
     * @param s2 字符串2
     * @return 如果相似度大于等于0.5则返回true
     */
    public boolean isModeratelySimilar(String s1, String s2) {
        return isSimilar(s1, s2, 0.5);
    }
    
    /**
     * 判断两个字符串是否轻微相似（阈值0.3）
     * 
     * @param s1 字符串1
     * @param s2 字符串2
     * @return 如果相似度大于等于0.3则返回true
     */
    public boolean isSlightlySimilar(String s1, String s2) {
        return isSimilar(s1, s2, 0.3);
    }
    
    /**
     * 获取相似度描述
     * 
     * @param similarity 相似度值
     * @return 相似度描述
     */
    public String getSimilarityDescription(double similarity) {
        if (similarity >= 0.9) {
            return "极高相似度";
        } else if (similarity >= 0.8) {
            return "高度相似";
        } else if (similarity >= 0.7) {
            return "较高相似度";
        } else if (similarity >= 0.6) {
            return "中等相似度";
        } else if (similarity >= 0.5) {
            return "一般相似度";
        } else if (similarity >= 0.3) {
            return "轻微相似度";
        } else {
            return "不相似";
        }
    }
    
    /**
     * 获取两个字符串的相似度描述
     * 
     * @param s1 字符串1
     * @param s2 字符串2
     * @return 相似度描述
     */
    public String getSimilarityDescription(String s1, String s2) {
        double similarity = calculateSimilarity(s1, s2);
        return getSimilarityDescription(similarity);
    }
    
    /**
     * 计算标准化后的字符串相似度
     * 先对两个字符串进行标准化处理，再计算相似度
     * 
     * @param s1 字符串1
     * @param s2 字符串2
     * @param textNormalizer 文本标准化器
     * @return 标准化后的相似度
     */
    public double calculateNormalizedSimilarity(String s1, String s2, TextNormalizer textNormalizer) {
        if (textNormalizer == null) {
            return calculateSimilarity(s1, s2);
        }
        
        String normalizedS1 = textNormalizer.normalize(s1);
        String normalizedS2 = textNormalizer.normalize(s2);
        
        return calculateSimilarity(normalizedS1, normalizedS2);
    }
    
    /**
     * 计算标准化后的字符串相似度百分比
     * 
     * @param s1 字符串1
     * @param s2 字符串2
     * @param textNormalizer 文本标准化器
     * @return 标准化后的相似度百分比
     */
    public int calculateNormalizedSimilarityPercentage(String s1, String s2, TextNormalizer textNormalizer) {
        double similarity = calculateNormalizedSimilarity(s1, s2, textNormalizer);
        return (int) Math.round(similarity * 100);
    }
    
    /**
     * 判断标准化后的字符串是否相似
     * 
     * @param s1 字符串1
     * @param s2 字符串2
     * @param threshold 相似度阈值
     * @param textNormalizer 文本标准化器
     * @return 如果标准化后的相似度大于等于阈值则返回true
     */
    public boolean isNormalizedSimilar(String s1, String s2, double threshold, TextNormalizer textNormalizer) {
        double similarity = calculateNormalizedSimilarity(s1, s2, textNormalizer);
        return similarity >= threshold;
    }
    
    /**
     * 获取最佳匹配字符串
     * 
     * @param target 目标字符串
     * @param candidates 候选字符串列表
     * @return 最佳匹配的字符串和相似度，如果没有匹配则返回null
     */
    public MatchResult findBestMatch(String target, java.util.List<String> candidates) {
        if (target == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        
        String bestMatch = null;
        double bestSimilarity = -1.0;
        
        for (String candidate : candidates) {
            double similarity = calculateSimilarity(target, candidate);
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestMatch = candidate;
            }
        }
        
        if (bestMatch != null) {
            return new MatchResult(bestMatch, bestSimilarity);
        }
        
        return null;
    }
    
    /**
     * 获取最佳匹配字符串（使用标准化）
     * 
     * @param target 目标字符串
     * @param candidates 候选字符串列表
     * @param textNormalizer 文本标准化器
     * @return 最佳匹配的字符串和相似度，如果没有匹配则返回null
     */
    public MatchResult findBestNormalizedMatch(String target, java.util.List<String> candidates, TextNormalizer textNormalizer) {
        if (target == null || candidates == null || candidates.isEmpty() || textNormalizer == null) {
            return null;
        }
        
        String normalizedTarget = textNormalizer.normalize(target);
        String bestMatch = null;
        double bestSimilarity = -1.0;
        
        for (String candidate : candidates) {
            String normalizedCandidate = textNormalizer.normalize(candidate);
            double similarity = calculateSimilarity(normalizedTarget, normalizedCandidate);
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestMatch = candidate;
            }
        }
        
        if (bestMatch != null) {
            return new MatchResult(bestMatch, bestSimilarity);
        }
        
        return null;
    }
    
    /**
     * 匹配结果类
     */
    public static class MatchResult {
        private final String matchedString;
        private final double similarity;
        
        public MatchResult(String matchedString, double similarity) {
            this.matchedString = matchedString;
            this.similarity = similarity;
        }
        
        public String getMatchedString() {
            return matchedString;
        }
        
        public double getSimilarity() {
            return similarity;
        }
        
        public int getSimilarityPercentage() {
            return (int) Math.round(similarity * 100);
        }
        
        public String getSimilarityDescription() {
            LevenshteinUtil util = new LevenshteinUtil();
            return util.getSimilarityDescription(similarity);
        }
        
        @Override
        public String toString() {
            return String.format("MatchResult{matchedString='%s', similarity=%.2f}", matchedString, similarity);
        }
    }
    
    /**
     * 获取Levenshtein距离计算器实例
     * 
     * @return Levenshtein距离计算器
     */
    public LevenshteinDistance getLevenshteinDistance() {
        return levenshteinDistance;
    }
}
