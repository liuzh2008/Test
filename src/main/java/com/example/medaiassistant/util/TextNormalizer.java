package com.example.medaiassistant.util;

import com.example.medaiassistant.config.TextNormalizerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 文本预处理和标准化工具类
 * 
 * 提供医疗文本的标准化处理功能，包括：
 * - 全角半角字符转换
 * - 多余空格去除
 * - 修饰词过滤（保留DRGs主要诊断关键词）
 * - 数字格式统一
 * - 方括号内容处理
 * 
 * 特别注意：本工具会保留DRGs主要诊断中的关键词，如"急性"、"亚急性"、"稳定性"等，
 * 确保DRGs匹配时不会因为过度标准化导致匹配失败。
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-16
 * @version 2.0
 */
@Component
public class TextNormalizer {
    
    private final TextNormalizerConfig config;
    
    // 运行时配置数据
    private Set<String> modifiers;
    private Map<String, String> numberConversions;
    private char[] fullWidthChars;
    private char[] halfWidthChars;
    
    // 正则表达式模式
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");
    private static final Pattern BRACKET_CONTENT = Pattern.compile("\\[.*?\\]");
    
    @Autowired
    public TextNormalizer(TextNormalizerConfig config) {
        this.config = config;
        initializeConfig();
    }
    
    /**
     * 初始化配置数据
     */
    private void initializeConfig() {
        // 初始化修饰词集合
        this.modifiers = new HashSet<>(config.getModifiers());
        
        // 初始化数字转换映射
        this.numberConversions = new HashMap<>();
        for (String conversion : config.getNumberConversions()) {
            String[] parts = conversion.split(":", 2);
            if (parts.length == 2) {
                numberConversions.put(parts[0], parts[1]);
            }
        }
        
        // 初始化全角半角字符映射
        this.fullWidthChars = config.getFullWidthChars().toCharArray();
        this.halfWidthChars = config.getHalfWidthChars().toCharArray();
    }
    
    /**
     * 完整文本预处理流程
     * 
     * @param text 原始文本
     * @return 标准化后的文本
     */
    public String normalize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        
        String result = text;
        
        // 1. 全角→半角转换
        result = fullWidthToHalfWidth(result);
        
        // 2. 去除多余空格
        result = removeExtraSpaces(result);
        
        // 3. 去除常见修饰词
        result = removeModifiers(result);
        
        // 4. 统一数字格式
        result = normalizeNumbers(result);
        
        // 5. 去除方括号内容（别名）
        result = removeBracketContent(result);
        
        return result.trim();
    }
    
    /**
     * 全角字符转换为半角字符
     * @return 转换后的文本
     */
    public String fullWidthToHalfWidth(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            for (int j = 0; j < fullWidthChars.length; j++) {
                if (chars[i] == fullWidthChars[j]) {
                    chars[i] = halfWidthChars[j];
                    break;
                }
            }
        }
        
        return new String(chars);
    }
    
    /**
     * 去除多余空格
     * 
     * @param text 原始文本
     * @return 去除多余空格后的文本
     */
    public String removeExtraSpaces(String text) {
        if (text == null) {
            return "";
        }
        return MULTIPLE_SPACES.matcher(text.trim()).replaceAll(" ");
    }
    
    /**
     * 去除常见修饰词
     * 
     * @param text 原始文本
     * @return 去除修饰词后的文本
     */
    public String removeModifiers(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String result = text;
        for (String modifier : modifiers) {
            // 中文文本没有单词边界，直接替换
            result = result.replace(modifier, "");
        }
        
        return removeExtraSpaces(result);
    }
    
    /**
     * 统一数字格式
     * 
     * @param text 原始文本
     * @return 统一数字格式后的文本
     */
    public String normalizeNumbers(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String result = text;
        for (Map.Entry<String, String> conversion : numberConversions.entrySet()) {
            result = result.replace(conversion.getKey(), conversion.getValue());
        }
        
        return result;
    }
    
    /**
     * 去除方括号内容（别名）
     * 
     * @param text 原始文本
     * @return 去除方括号内容后的文本
     */
    public String removeBracketContent(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return BRACKET_CONTENT.matcher(text).replaceAll("").trim();
    }
    
    /**
     * 提取方括号内容（别名）
     * 
     * @param text 原始文本
     * @return 方括号中的内容，如果没有则返回空字符串
     */
    public String extractBracketContent(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        java.util.regex.Matcher matcher = BRACKET_CONTENT.matcher(text);
        if (matcher.find()) {
            String content = matcher.group();
            // 去除方括号
            return content.substring(1, content.length() - 1);
        }
        
        return "";
    }
    
    /**
     * 判断文本是否包含特定修饰词
     * 
     * @param text 文本
     * @param modifier 修饰词
     * @return 是否包含
     */
    public boolean containsModifier(String text, String modifier) {
        if (text == null || modifier == null) {
            return false;
        }
        // 中文文本没有单词边界，直接判断是否包含
        return text.contains(modifier);
    }
    
    /**
     * 获取文本中的主要名称（去除方括号内容）
     * 
     * @param text 原始文本
     * @return 主要名称
     */
    public String getMainName(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return removeBracketContent(text).trim();
    }
    
    /**
     * 获取文本中的别名（方括号内容）
     * 
     * @param text 原始文本
     * @return 别名，如果没有则返回空字符串
     */
    public String getAlias(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return extractBracketContent(text);
    }
    
    /**
     * 判断文本是否包含方括号内容
     * 
     * @param text 文本
     * @return 是否包含方括号内容
     */
    public boolean hasBracketContent(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return BRACKET_CONTENT.matcher(text).find();
    }
    
    /**
     * 获取修饰词集合（只读）
     * 
     * @return 修饰词集合
     */
    public Set<String> getModifiers() {
        return new HashSet<>(modifiers);
    }
    
    /**
     * 添加自定义修饰词
     * 
     * @param modifier 修饰词
     */
    public void addModifier(String modifier) {
        if (modifier != null && !modifier.trim().isEmpty()) {
            modifiers.add(modifier.trim());
        }
    }
    
    /**
     * 移除自定义修饰词
     * 
     * @param modifier 修饰词
     */
    public void removeModifier(String modifier) {
        if (modifier != null) {
            modifiers.remove(modifier);
        }
    }
    
    /**
     * 重新加载配置
     */
    public void reloadConfig() {
        initializeConfig();
    }
    
    /**
     * 获取当前配置的快照
     * 
     * @return 配置快照
     */
    public Map<String, Object> getConfigSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("modifiers", new ArrayList<>(modifiers));
        snapshot.put("numberConversions", new HashMap<>(numberConversions));
        snapshot.put("fullWidthChars", String.valueOf(fullWidthChars));
        snapshot.put("halfWidthChars", String.valueOf(halfWidthChars));
        return snapshot;
    }
}
