package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.model.SqlTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 模板变量替换引擎
 * 负责替换SQL模板中的变量占位符
 * 变量格式：${variableName}
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-03
 */
@Slf4j
@Service
public class TemplateVariableReplacer {
    
    /**
     * 替换模板中的变量
     * 
     * @param template SQL模板
     * @param variables 变量映射
     * @return 替换后的SQL字符串
     */
    public String replaceVariables(SqlTemplate template, Map<String, String> variables) {
        // 处理null模板
        if (template == null || template.getTemplate() == null) {
            log.debug("模板为null，返回空字符串");
            return "";
        }
        
        String result = template.getTemplate();
        
        // 处理空模板字符串
        if (result.isEmpty()) {
            log.debug("模板字符串为空，返回空字符串");
            return "";
        }
        
        // 记录原始模板用于调试
        log.debug("开始替换模板变量，原始模板: {}", result);
        
        // 处理变量替换
        if (variables != null && !variables.isEmpty()) {
            log.debug("提供的变量数量: {}", variables.size());
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String placeholder = "${" + entry.getKey() + "}";
                String value = entry.getValue() != null ? entry.getValue() : "";
                
                // 检查占位符是否存在
                if (result.contains(placeholder)) {
                    // 使用replace替换所有出现的占位符
                    result = result.replace(placeholder, value);
                    log.debug("替换变量 {} -> {}", entry.getKey(), value);
                } else {
                    log.debug("变量 {} 的占位符 {} 在模板中未找到", entry.getKey(), placeholder);
                }
            }
        } else {
            log.debug("变量映射为空或为null，跳过变量替换");
        }
        
        // 检查是否还有未替换的变量
        checkForUnreplacedVariables(result, template);
        
        log.debug("变量替换完成，结果: {}", result);
        return result;
    }
    
    /**
     * 检查未替换的变量并记录警告
     * 
     * @param result 替换后的字符串
     * @param template 原始模板
     */
    private void checkForUnreplacedVariables(String result, SqlTemplate template) {
        // 使用正则表达式检查是否还有 ${...} 格式的变量
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}");
        java.util.regex.Matcher matcher = pattern.matcher(result);
        
        if (matcher.find()) {
            java.util.Set<String> unreplacedVars = new java.util.HashSet<>();
            matcher.reset();
            while (matcher.find()) {
                unreplacedVars.add(matcher.group(1));
            }
            log.warn("模板 {} 仍有未替换的变量: {}", 
                template.getQueryName() != null ? template.getQueryName() : "未知", 
                unreplacedVars);
        }
    }
}
