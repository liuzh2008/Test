package com.example.medaiassistant.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InputSetNormalizer单元测试类
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-23
 */
class InputSetNormalizerTest {

    private final InputSetNormalizer normalizer = new InputSetNormalizer();

    @Test
    void normalizeIdSet_WithDuplicateIds_ReturnsDistinctSorted() {
        // 准备测试数据 - 包含重复元素和乱序
        List<String> inputIds = Arrays.asList("102", "101", "102", "103", "101");
        
        // 执行测试
        String[] result = normalizer.normalizeIdSet(inputIds);
        
        // 验证结果 - 应该去重并排序
        assertArrayEquals(new String[]{"101", "102", "103"}, result, 
                "应该返回去重并排序后的ID数组");
    }

    @Test
    void normalizeIdSet_WithEmptySet_ReturnsEmptyArray() {
        // 准备测试数据 - 空集合
        List<String> inputIds = Arrays.asList();
        
        // 执行测试
        String[] result = normalizer.normalizeIdSet(inputIds);
        
        // 验证结果 - 应该返回空数组
        assertArrayEquals(new String[]{}, result, 
                "空集合应该返回空数组");
    }

    @Test
    void generateSetHash_WithSameContent_ReturnsSameHash() {
        // 准备测试数据 - 相同内容但顺序不同
        List<String> ids1 = Arrays.asList("101", "102", "103");
        List<String> ids2 = Arrays.asList("103", "101", "102");
        
        // 执行测试
        String hash1 = normalizer.generateSetHash(ids1);
        String hash2 = normalizer.generateSetHash(ids2);
        
        // 验证结果 - 相同内容应该生成相同的哈希
        assertEquals(hash1, hash2, 
                "相同内容但顺序不同的集合应该生成相同的哈希");
    }

    @Test
    void generateSetHash_WithDifferentOrder_ReturnsSameHash() {
        // 准备测试数据 - 不同顺序的相同集合
        List<String> ids1 = Arrays.asList("201", "202", "203");
        List<String> ids2 = Arrays.asList("203", "201", "202");
        
        // 执行测试
        String hash1 = normalizer.generateSetHash(ids1);
        String hash2 = normalizer.generateSetHash(ids2);
        
        // 验证结果 - 不同顺序的相同集合应该生成相同的哈希
        assertEquals(hash1, hash2, 
                "不同顺序的相同集合应该生成相同的哈希");
    }

    @Test
    void areSetsEqual_WithSameContent_ReturnsTrue() {
        // 准备测试数据 - 相同内容但顺序不同
        List<String> ids1 = Arrays.asList("301", "302", "303");
        List<String> ids2 = Arrays.asList("303", "301", "302");
        
        // 执行测试
        boolean result = normalizer.areSetsEqual(ids1, ids2);
        
        // 验证结果 - 相同内容应该返回true
        assertTrue(result, "相同内容的集合应该返回true");
    }

    @Test
    void areSetsEqual_WithDifferentContent_ReturnsFalse() {
        // 准备测试数据 - 不同内容
        List<String> ids1 = Arrays.asList("401", "402");
        List<String> ids2 = Arrays.asList("401", "403");
        
        // 执行测试
        boolean result = normalizer.areSetsEqual(ids1, ids2);
        
        // 验证结果 - 不同内容应该返回false
        assertFalse(result, "不同内容的集合应该返回false");
    }

    @Test
    void areSetsEqual_WithDifferentSizes_ReturnsFalse() {
        // 准备测试数据 - 不同大小
        List<String> ids1 = Arrays.asList("501", "502");
        List<String> ids2 = Arrays.asList("501", "502", "503");
        
        // 执行测试
        boolean result = normalizer.areSetsEqual(ids1, ids2);
        
        // 验证结果 - 不同大小应该返回false
        assertFalse(result, "不同大小的集合应该返回false");
    }

    @Test
    void areSetsEqual_WithNullInput_ReturnsFalse() {
        // 准备测试数据 - 包含null
        List<String> ids1 = Arrays.asList("601", "602");
        List<String> ids2 = null;
        
        // 执行测试
        boolean result = normalizer.areSetsEqual(ids1, ids2);
        
        // 验证结果 - 包含null应该返回false
        assertFalse(result, "包含null的集合应该返回false");
    }

    @Test
    void areSetsEqual_WithSameReference_ReturnsTrue() {
        // 准备测试数据 - 相同引用
        List<String> ids1 = Arrays.asList("701", "702");
        
        // 执行测试
        boolean result = normalizer.areSetsEqual(ids1, ids1);
        
        // 验证结果 - 相同引用应该返回true
        assertTrue(result, "相同引用的集合应该返回true");
    }
}
