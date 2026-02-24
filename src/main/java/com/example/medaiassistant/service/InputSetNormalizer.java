package com.example.medaiassistant.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * 输入集合规范化器
 * 负责对诊断和手术ID集合进行规范化处理，包括去重、排序和哈希生成
 * 为快照比对提供标准化的集合处理能力，支持集合大小预筛选优化
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-23
 */
@Component
public class InputSetNormalizer {

    /**
     * 规范化ID集合，进行去重和排序处理
     * 确保相同内容但顺序不同的集合生成相同的规范化结果
     * 
     * @param ids 输入的ID集合，可以为null或空集合
     * @return 规范化后的ID数组，如果输入为null或空集合则返回空数组
     * @example
     * 输入: ["102", "101", "102", "103", "101"]
     * 输出: ["101", "102", "103"]
     */
    public String[] normalizeIdSet(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return new String[0];
        }
        
        // 去重并排序
        return ids.stream()
                .distinct()
                .sorted()
                .toArray(String[]::new);
    }

    /**
     * 生成集合的规范化哈希
     * 使用SHA-256算法确保哈希的唯一性和安全性
     * 相同内容但顺序不同的集合会生成相同的哈希值
     * 
     * @param ids 输入的ID集合
     * @return 集合的SHA-256哈希值，如果SHA-256不可用则回退到简单哈希
     * @example
     * 输入: ["101", "102", "103"] 和 ["103", "101", "102"]
     * 输出: 相同的哈希值
     */
    public String generateSetHash(List<String> ids) {
        String[] normalizedIds = normalizeIdSet(ids);
        
        // 使用规范化后的ID数组生成哈希字符串
        String normalizedString = String.join(",", normalizedIds);
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(normalizedString.getBytes(StandardCharsets.UTF_8));
            
            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // 如果SHA-256不可用，回退到简单的字符串哈希
            return String.valueOf(normalizedString.hashCode());
        }
    }
    
    /**
     * 快速比较两个集合是否相等
     * 支持集合大小预筛选优化，减少不必要的哈希计算
     * 使用哈希值进行快速比对，避免完整的集合比较
     * 
     * @param ids1 第一个ID集合
     * @param ids2 第二个ID集合
     * @return 如果两个集合包含相同的元素（不考虑顺序）则返回true，否则返回false
     * @example
     * 输入: ["101", "102"] 和 ["102", "101"]
     * 输出: true
     * 输入: ["101", "102"] 和 ["101", "103"]
     * 输出: false
     */
    public boolean areSetsEqual(List<String> ids1, List<String> ids2) {
        if (ids1 == ids2) {
            return true;
        }
        if (ids1 == null || ids2 == null) {
            return false;
        }
        
        // 快速预筛选：检查集合大小
        if (ids1.size() != ids2.size()) {
            return false;
        }
        
        // 使用哈希进行快速比对
        String hash1 = generateSetHash(ids1);
        String hash2 = generateSetHash(ids2);
        
        return hash1.equals(hash2);
    }
}
