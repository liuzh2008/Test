package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.model.SqlTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * JSON模板解析器服务
 * 负责解析JSON格式的SQL模板文件
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-03
 */
@Slf4j
@Service
public class JsonTemplateParser {
    
    private final ObjectMapper objectMapper;
    
    public JsonTemplateParser() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 从JSON字符串解析SQL模板
     * 
     * @param json JSON字符串
     * @return 解析后的SqlTemplate对象
     * @throws IOException 如果JSON解析失败
     */
    public SqlTemplate parseFromJson(String json) throws IOException {
        try {
            SqlTemplate template = objectMapper.readValue(json, SqlTemplate.class);
            log.debug("成功解析SQL模板: {}", template.getQueryName());
            return template;
        } catch (IOException e) {
            log.error("解析JSON模板失败: {}", e.getMessage(), e);
            throw new IOException("解析JSON模板失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从文件解析SQL模板
     * 
     * @param filePath 文件路径
     * @return 解析后的SqlTemplate对象
     * @throws IOException 如果文件读取或解析失败
     */
    public SqlTemplate parseFromFile(Path filePath) throws IOException {
        try {
            String json = Files.readString(filePath);
            return parseFromJson(json);
        } catch (IOException e) {
            log.error("从文件解析SQL模板失败: {}", filePath, e);
            throw new IOException("从文件解析SQL模板失败: " + filePath + " - " + e.getMessage(), e);
        }
    }
    
    /**
     * 从类路径资源解析SQL模板
     * 
     * @param resourcePath 资源路径
     * @return 解析后的SqlTemplate对象
     * @throws IOException 如果资源读取或解析失败
     */
    public SqlTemplate parseFromResource(String resourcePath) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("资源未找到: " + resourcePath);
            }
            SqlTemplate template = objectMapper.readValue(is, SqlTemplate.class);
            log.debug("成功从资源解析SQL模板: {}", template.getQueryName());
            return template;
        } catch (IOException e) {
            log.error("从资源解析SQL模板失败: {}", resourcePath, e);
            throw new IOException("从资源解析SQL模板失败: " + resourcePath + " - " + e.getMessage(), e);
        }
    }
    
    /**
     * 批量解析JSON字符串为SQL模板列表
     * 
     * @param jsonList JSON字符串列表
     * @return SQL模板列表
     * @throws IOException 如果任何JSON解析失败
     */
    public List<SqlTemplate> parseBatchFromJson(List<String> jsonList) throws IOException {
        return jsonList.stream()
                .map(json -> {
                    try {
                        return parseFromJson(json);
                    } catch (IOException e) {
                        throw new RuntimeException("批量解析失败: " + e.getMessage(), e);
                    }
                })
                .toList();
    }
    
    /**
     * 验证SQL模板的完整性（简化版本）
     * 
     * @param template SQL模板
     * @return 验证结果，true表示有效
     */
    public boolean validateTemplate(SqlTemplate template) {
        if (template == null) {
            log.warn("模板为null");
            return false;
        }
        
        if (template.getQueryName() == null || template.getQueryName().trim().isEmpty()) {
            log.warn("模板缺少查询名称");
            return false;
        }
        
        // 检查是否有有效的SQL语句（优先使用sql字段，其次使用template字段）
        String effectiveSql = template.getEffectiveSql();
        if (effectiveSql == null || effectiveSql.trim().isEmpty()) {
            log.warn("模板缺少SQL语句");
            return false;
        }
        
        // 验证参数定义
        if (template.getParameters() != null) {
            for (SqlTemplate.Parameter param : template.getParameters()) {
                if (param.getName() == null || param.getName().trim().isEmpty()) {
                    log.warn("参数缺少名称");
                    return false;
                }
                if (param.getType() == null || param.getType().trim().isEmpty()) {
                    log.warn("参数{}缺少类型", param.getName());
                    return false;
                }
            }
        }
        
        log.debug("模板验证通过: {}", template.getQueryName());
        return true;
    }
    
    /**
     * 替换模板中的变量（简化版本）
     * 注意：简化版本中，我们不再进行变量替换，因为SQL语句已经是完整的
     * 变量替换由SqlExecutionService处理命名参数
     * 
     * @param template SQL模板
     * @param variables 变量映射
     * @return 替换后的SQL字符串
     */
    public String replaceVariables(SqlTemplate template, Map<String, String> variables) {
        if (template == null) {
            return "";
        }
        
        // 获取有效的SQL语句（优先使用sql字段）
        String result = template.getEffectiveSql();
        if (result == null) {
            return "";
        }
        
        // 简化版本：不再进行变量替换，因为SQL语句已经是完整的
        // 命名参数（如:DEPT_NAME）由SqlExecutionService处理
        log.debug("使用完整的SQL语句，不进行变量替换: {}", template.getQueryName());
        return result;
    }
    
    /**
     * 生成完整的SQL语句（简化版本）
     * 对于简化版本，直接返回SQL语句，不进行变量替换
     * 
     * @param template SQL模板
     * @param variables 变量映射
     * @return 完整的SQL语句
     */
    public String generateSql(SqlTemplate template, Map<String, String> variables) {
        return replaceVariables(template, variables);
    }
}
