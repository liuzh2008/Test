package com.example.medaiassistant.hospital.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL模板实体类
 * <p>对应JSON模板文件结构，用于内存中表示SQL模板</p>
 * <p>不是JPA实体，不存储到数据库</p>
 * 
 * <p><strong>字段说明</strong>：</p>
 * <ul>
 *   <li>{@code queryName} - 查询名称，唯一标识符</li>
 *   <li>{@code description} - 查询描述</li>
 *   <li>{@code template} - SQL模板字符串（旧格式，支持${}变量替换）</li>
 *   <li>{@code sql} - 完整SQL语句（新格式，支持:名称参数）</li>
 *   <li>{@code databaseType} - 数据库类型（his/lis），用于路由数据库连接</li>
 *   <li>{@code parameters} - 参数定义列表</li>
 *   <li>{@code metadata} - 元数据信息</li>
 * </ul>
 * 
 * @author TDD
 * @version 1.1
 * @since 2026-01-12 添加databaseType字段支持
 * @see com.example.medaiassistant.hospital.service.SqlExecutionService
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SqlTemplate {
    
    /**
     * 查询名称，唯一标识符
     */
    private String queryName;
    
    /**
     * 查询描述
     */
    private String description;
    
    /**
     * SQL模板字符串，支持变量替换（旧格式）
     * 变量格式：${variableName}
     * 注意：新模板使用完整的SQL语句和命名参数
     */
    private String template;
    
    /**
     * 完整的SQL语句（新格式）
     * 包含命名参数，如 :DEPT_NAME
     */
    private String sql;
    
    /**
     * 数据库类型
     * <p>用于指定查询应该在哪个数据库连接上执行</p>
     * 
     * <p><strong>支持的值</strong>：</p>
     * <ul>
     *   <li>{@code his} - HIS数据库连接（默认）</li>
     *   <li>{@code lis} - LIS数据库连接</li>
     * </ul>
     * 
     * @since 1.1
     */
    private String databaseType;
    
    /**
     * 模板参数定义
     */
    private List<Parameter> parameters;
    
    /**
     * 元数据信息（简化版本中可选）
     */
    private Metadata metadata;
    
    /**
     * 模板参数类
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Parameter {
        /**
         * 参数名称
         */
        private String name;
        
        /**
         * 参数类型：String, Integer, Date等
         */
        private String type;
        
        /**
         * 是否必需
         */
        private Boolean required;
        
        /**
         * 参数描述
         */
        private String description;
        
        /**
         * 默认值（可选）
         */
        private String defaultValue;
    }
    
    /**
     * 元数据类
     */
    @Data
    public static class Metadata {
        /**
         * 分类：patient, lab, order等
         */
        private String category;
        
        /**
         * 版本号
         */
        private String version;
        
        /**
         * 最后修改时间
         */
        private String lastModified;
        
        /**
         * 作者
         */
        private String author;
        
        /**
         * 标签列表
         */
        private List<String> tags;
        
        /**
         * 扩展属性
         */
        private Map<String, Object> extensions;
    }
    
    /**
     * 获取有效的SQL语句
     * 优先使用sql字段，如果不存在则使用template字段
     */
    public String getEffectiveSql() {
        if (sql != null && !sql.trim().isEmpty()) {
            return sql;
        }
        return template;
    }
    
    /**
     * 便捷方法：检查SQL是否包含命名参数
     */
    public boolean containsNamedParameters() {
        String effectiveSql = getEffectiveSql();
        return effectiveSql != null && effectiveSql.contains(":");
    }
    
    /**
     * 便捷方法：提取SQL中的所有命名参数
     * 提取格式为:paramName的参数
     * 返回唯一的参数名列表
     */
    public List<String> extractNamedParameters() {
        String effectiveSql = getEffectiveSql();
        if (effectiveSql == null || effectiveSql.isEmpty()) {
            return List.of();
        }
        
        Set<String> paramSet = new LinkedHashSet<>();
        Pattern pattern = Pattern.compile(":([a-zA-Z_][a-zA-Z0-9_]*)");
        Matcher matcher = pattern.matcher(effectiveSql);
        
        while (matcher.find()) {
            paramSet.add(matcher.group(1));
        }
        
        return new ArrayList<>(paramSet);
    }
    
    /**
     * 便捷方法：检查参数是否有效
     */
    public boolean validateParameters(Map<String, Object> inputParams) {
        if (parameters == null || parameters.isEmpty()) {
            return true;
        }
        
        for (Parameter param : parameters) {
            if (Boolean.TRUE.equals(param.getRequired())) {
                if (!inputParams.containsKey(param.getName())) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * 便捷方法：检查模板是否包含变量（旧格式）
     */
    public boolean containsVariables() {
        return template != null && template.contains("${");
    }
    
    /**
     * 便捷方法：提取模板中的所有变量名（旧格式）
     * 提取格式为${variableName}的变量
     * 返回唯一的变量名列表
     */
    public List<String> extractVariableNames() {
        if (template == null || template.isEmpty()) {
            return List.of();
        }
        
        Set<String> variableSet = new LinkedHashSet<>();
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(template);
        
        while (matcher.find()) {
            variableSet.add(matcher.group(1));
        }
        
        return new ArrayList<>(variableSet);
    }
    
    @Override
    public String toString() {
        return "SqlTemplate{" +
                "queryName='" + queryName + '\'' +
                ", description='" + description + '\'' +
                ", sqlLength=" + (sql != null ? sql.length() : 0) +
                ", templateLength=" + (template != null ? template.length() : 0) +
                ", parameters=" + (parameters != null ? parameters.size() : 0) +
                '}';
    }
}
