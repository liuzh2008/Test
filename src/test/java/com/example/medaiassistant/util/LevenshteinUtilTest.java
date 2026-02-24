package com.example.medaiassistant.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LevenshteinUtil单元测试
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-16
 */
@DisplayName("字符串相似度计算工具类测试")
class LevenshteinUtilTest {
    
    private LevenshteinUtil levenshteinUtil;
    
    @BeforeEach
    void setUp() {
        levenshteinUtil = new LevenshteinUtil();
    }
    
    @Test
    @DisplayName("测试计算Levenshtein距离")
    void testCalculateDistance() {
        // 测试相同字符串
        String s1 = "hello";
        String s2 = "hello";
        int distance = levenshteinUtil.calculateDistance(s1, s2);
        assertEquals(0, distance, "相同字符串的距离应为0");
        
        // 测试不同字符串
        s1 = "kitten";
        s2 = "sitting";
        distance = levenshteinUtil.calculateDistance(s1, s2);
        assertEquals(3, distance, "kitten和sitting的距离应为3");
        
        // 测试空字符串
        s1 = "";
        s2 = "test";
        distance = levenshteinUtil.calculateDistance(s1, s2);
        assertEquals(4, distance, "空字符串和test的距离应为4");
        
        // 测试null值
        s1 = null;
        s2 = "test";
        distance = levenshteinUtil.calculateDistance(s1, s2);
        assertEquals(-1, distance, "null字符串应返回-1");
    }
    
    @ParameterizedTest
    @DisplayName("参数化测试字符串相似度")
    @CsvSource({
        "hello, hello, 1.0",
        "kitten, sitting, 0.571",
        "test, '', 0.0",
        "'', '', 1.0",
        "高血压, 高血压病, 0.75"
    })
    void testCalculateSimilarity(String s1, String s2, double expectedSimilarity) {
        double similarity = levenshteinUtil.calculateSimilarity(s1, s2);
        assertEquals(expectedSimilarity, similarity, 0.01, 
            "字符串相似度计算不正确: " + s1 + " vs " + s2);
    }
    
    @Test
    @DisplayName("测试计算相似度百分比")
    void testCalculateSimilarityPercentage() {
        // 测试相同字符串
        String s1 = "hello";
        String s2 = "hello";
        int percentage = levenshteinUtil.calculateSimilarityPercentage(s1, s2);
        assertEquals(100, percentage, "相同字符串的相似度应为100%");
        
        // 测试不同字符串
        s1 = "kitten";
        s2 = "sitting";
        percentage = levenshteinUtil.calculateSimilarityPercentage(s1, s2);
        assertEquals(57, percentage, "kitten和sitting的相似度应为57%");
        
        // 测试空字符串
        s1 = "";
        s2 = "test";
        percentage = levenshteinUtil.calculateSimilarityPercentage(s1, s2);
        assertEquals(0, percentage, "空字符串和test的相似度应为0%");
    }
    
    @Test
    @DisplayName("测试判断字符串相似度")
    void testIsSimilar() {
        // 测试高度相似
        String s1 = "高血压";
        String s2 = "高血压病";
        boolean isSimilar = levenshteinUtil.isSimilar(s1, s2, 0.7);
        assertTrue(isSimilar, "高血压和高血压病应被认为是相似的");
        
        // 测试不相似
        s1 = "感冒";
        s2 = "肺炎";
        isSimilar = levenshteinUtil.isSimilar(s1, s2, 0.7);
        assertFalse(isSimilar, "感冒和肺炎不应被认为是相似的");
    }
    
    @Test
    @DisplayName("测试高度相似判断")
    void testIsHighlySimilar() {
        // 测试高度相似
        String s1 = "糖尿病";
        String s2 = "糖尿病";
        boolean isHighlySimilar = levenshteinUtil.isHighlySimilar(s1, s2);
        assertTrue(isHighlySimilar, "相同字符串应被认为是高度相似的");
        
        // 测试不高度相似
        s1 = "糖尿病";
        s2 = "糖尿病酮症酸中毒";
        isHighlySimilar = levenshteinUtil.isHighlySimilar(s1, s2);
        assertFalse(isHighlySimilar, "糖尿病和糖尿病酮症酸中毒不应被认为是高度相似的");
    }
    
    @Test
    @DisplayName("测试中等相似判断")
    void testIsModeratelySimilar() {
        // 测试中等相似
        String s1 = "高血压";
        String s2 = "高血压病";
        boolean isModeratelySimilar = levenshteinUtil.isModeratelySimilar(s1, s2);
        assertTrue(isModeratelySimilar, "高血压和高血压病应被认为是中等相似的");
        
        // 测试不中等相似
        s1 = "感冒";
        s2 = "重症肺炎";
        isModeratelySimilar = levenshteinUtil.isModeratelySimilar(s1, s2);
        assertFalse(isModeratelySimilar, "感冒和重症肺炎不应被认为是中等相似的");
    }
    
    @Test
    @DisplayName("测试轻微相似判断")
    void testIsSlightlySimilar() {
        // 测试轻微相似
        String s1 = "心绞痛";
        String s2 = "心梗";
        boolean isSlightlySimilar = levenshteinUtil.isSlightlySimilar(s1, s2);
        assertTrue(isSlightlySimilar, "心绞痛和心梗应被认为是轻微相似的");
    }
    
    @Test
    @DisplayName("测试获取相似度描述")
    void testGetSimilarityDescription() {
        // 测试极高相似度
        String description = levenshteinUtil.getSimilarityDescription(0.95);
        assertEquals("极高相似度", description, "0.95相似度应描述为极高相似度");
        
        // 测试高度相似
        description = levenshteinUtil.getSimilarityDescription(0.85);
        assertEquals("高度相似", description, "0.85相似度应描述为高度相似");
        
        // 测试较高相似度
        description = levenshteinUtil.getSimilarityDescription(0.75);
        assertEquals("较高相似度", description, "0.75相似度应描述为较高相似度");
        
        // 测试中等相似度
        description = levenshteinUtil.getSimilarityDescription(0.65);
        assertEquals("中等相似度", description, "0.65相似度应描述为中等相似度");
        
        // 测试一般相似度
        description = levenshteinUtil.getSimilarityDescription(0.55);
        assertEquals("一般相似度", description, "0.55相似度应描述为一般相似度");
        
        // 测试轻微相似度
        description = levenshteinUtil.getSimilarityDescription(0.35);
        assertEquals("轻微相似度", description, "0.35相似度应描述为轻微相似度");
        
        // 测试不相似
        description = levenshteinUtil.getSimilarityDescription(0.1);
        assertEquals("不相似", description, "0.1相似度应描述为不相似");
    }
    
    @Test
    @DisplayName("测试获取两个字符串的相似度描述")
    void testGetSimilarityDescriptionForStrings() {
        String s1 = "高血压";
        String s2 = "高血压病";
        String description = levenshteinUtil.getSimilarityDescription(s1, s2);
        assertNotNull(description, "相似度描述不应为null");
        assertTrue(description.contains("相似"), "相似度描述应包含'相似'字样");
    }
    
    @Test
    @DisplayName("测试查找最佳匹配")
    void testFindBestMatch() {
        String target = "高血压";
        List<String> candidates = Arrays.asList(
            "低血压", "高血压病", "血压异常", "高血压危象"
        );
        
        LevenshteinUtil.MatchResult result = levenshteinUtil.findBestMatch(target, candidates);
        
        assertNotNull(result, "应找到最佳匹配");
        assertEquals("高血压病", result.getMatchedString(), "最佳匹配应为高血压病");
        assertTrue(result.getSimilarity() > 0.7, "相似度应较高");
    }
    
    @Test
    @DisplayName("测试空值和边界情况")
    void testNullAndEdgeCases() {
        // 测试null目标字符串
        List<String> candidates = Arrays.asList("test1", "test2");
        LevenshteinUtil.MatchResult result = levenshteinUtil.findBestMatch(null, candidates);
        assertNull(result, "null目标字符串应返回null");
        
        // 测试空候选列表
        result = levenshteinUtil.findBestMatch("test", null);
        assertNull(result, "空候选列表应返回null");
        
        // 测试空候选列表
        result = levenshteinUtil.findBestMatch("test", Arrays.asList());
        assertNull(result, "空候选列表应返回null");
        
    }
    
    @Test
    @DisplayName("测试MatchResult类")
    void testMatchResultClass() {
        LevenshteinUtil.MatchResult result = new LevenshteinUtil.MatchResult("高血压", 0.85);
        
        assertEquals("高血压", result.getMatchedString(), "匹配字符串应正确");
        assertEquals(0.85, result.getSimilarity(), 0.001, "相似度应正确");
        assertEquals(85, result.getSimilarityPercentage(), "相似度百分比应正确");
        assertNotNull(result.getSimilarityDescription(), "相似度描述不应为null");
        assertNotNull(result.toString(), "toString方法不应返回null");
    }
    
    @Test
    @DisplayName("测试医疗诊断名称相似度")
    void testMedicalDiagnosisSimilarity() {
        // 医疗诊断名称相似度测试用例
        String[][] testCases = {
            {"高血压", "高血压病", "0.75"},
            {"糖尿病", "糖尿病2型", "0.67"}, 
            {"肺炎", "肺部感染", "0.33"},
            {"冠心病", "冠状动脉粥样硬化性心脏病", "0.25"},
            {"感冒", "流行性感冒", "0.5"}
        };
        
        for (String[] testCase : testCases) {
            String s1 = testCase[0];
            String s2 = testCase[1];
            double expectedSimilarity = Double.parseDouble(testCase[2]);
            double actualSimilarity = levenshteinUtil.calculateSimilarity(s1, s2);
            
            assertEquals(expectedSimilarity, actualSimilarity, 0.1, 
                "医疗诊断名称相似度测试失败: " + s1 + " vs " + s2);
        }
    }
    
    @Test
    @DisplayName("测试Levenshtein距离计算器实例")
    void testGetLevenshteinDistance() {
        assertNotNull(levenshteinUtil.getLevenshteinDistance(), 
            "Levenshtein距离计算器实例不应为null");
    }
}
