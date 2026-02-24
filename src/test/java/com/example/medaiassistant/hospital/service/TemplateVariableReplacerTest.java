package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.model.SqlTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模板变量替换引擎测试
 * 按照TDD红-绿-重构流程实现
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-03
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("模板变量替换引擎测试")
class TemplateVariableReplacerTest {
    
    private TemplateVariableReplacer templateVariableReplacer;
    
    @BeforeEach
    void setUp() {
        // 红阶段：TemplateVariableReplacer还不存在，测试会编译失败
        templateVariableReplacer = new TemplateVariableReplacer();
    }
    
    /**
     * 测试1：简单变量替换测试
     * 验证简单变量替换功能
     * 红阶段：TemplateVariableReplacer类不存在，测试会编译失败
     */
    @Test
    @DisplayName("测试简单变量替换 - 应正确替换单个变量")
    void testSimpleVariableReplacement() {
        // 准备测试数据
        SqlTemplate template = new SqlTemplate();
        template.setTemplate("SELECT * FROM ${tablePrefix}.patients WHERE id = :id");
        
        Map<String, String> variables = Map.of("tablePrefix", "hospital");
        
        // 执行变量替换
        String result = templateVariableReplacer.replaceVariables(template, variables);
        
        // 验证结果
        assertNotNull(result, "替换结果不应为null");
        assertEquals("SELECT * FROM hospital.patients WHERE id = :id", result, 
            "应正确替换tablePrefix变量");
        assertFalse(result.contains("${tablePrefix}"), "不应包含未替换的变量占位符");
    }
    
    /**
     * 测试2：多个变量替换测试
     * 验证多个变量同时替换功能
     */
    @Test
    @DisplayName("测试多个变量替换 - 应正确替换多个变量")
    void testMultipleVariableReplacement() {
        // 准备测试数据
        SqlTemplate template = new SqlTemplate();
        template.setTemplate("SELECT * FROM ${schema}.${table} WHERE ${conditionField} = :value");
        
        Map<String, String> variables = Map.of(
            "schema", "hospital_schema",
            "table", "patient_records",
            "conditionField", "status"
        );
        
        // 执行变量替换
        String result = templateVariableReplacer.replaceVariables(template, variables);
        
        // 验证结果
        assertNotNull(result, "替换结果不应为null");
        assertEquals("SELECT * FROM hospital_schema.patient_records WHERE status = :value", 
            result, "应正确替换所有变量");
        assertFalse(result.contains("${"), "不应包含任何未替换的变量占位符");
    }
    
    /**
     * 测试3：嵌套变量替换测试
     * 验证嵌套变量替换功能
     */
    @Test
    @DisplayName("测试嵌套变量替换 - 应正确处理嵌套变量场景")
    void testNestedVariableReplacement() {
        // 准备测试数据
        SqlTemplate template = new SqlTemplate();
        template.setTemplate("SELECT * FROM ${schema}.${tablePrefix}_${tableSuffix} WHERE id = :id");
        
        Map<String, String> variables = Map.of(
            "schema", "hospital",
            "tablePrefix", "patient",
            "tableSuffix", "records"
        );
        
        // 执行变量替换
        String result = templateVariableReplacer.replaceVariables(template, variables);
        
        // 验证结果
        assertNotNull(result, "替换结果不应为null");
        assertEquals("SELECT * FROM hospital.patient_records WHERE id = :id", 
            result, "应正确替换嵌套变量");
        assertFalse(result.contains("${"), "不应包含任何未替换的变量占位符");
    }
    
    /**
     * 测试4：变量缺失处理测试
     * 验证当变量缺失时的处理逻辑
     */
    @Test
    @DisplayName("测试变量缺失处理 - 应保留未替换的变量占位符")
    void testMissingVariableHandling() {
        // 准备测试数据
        SqlTemplate template = new SqlTemplate();
        template.setTemplate("SELECT * FROM ${schema}.${table} WHERE status = :status");
        
        // 只提供部分变量
        Map<String, String> variables = Map.of("schema", "hospital");
        
        // 执行变量替换
        String result = templateVariableReplacer.replaceVariables(template, variables);
        
        // 验证结果
        assertNotNull(result, "替换结果不应为null");
        assertTrue(result.contains("hospital"), "应替换schema变量");
        assertTrue(result.contains("${table}"), "应保留未替换的table变量占位符");
        assertFalse(result.contains("${schema}"), "不应包含已替换的schema变量占位符");
    }
    
    /**
     * 测试5：空变量映射测试
     * 验证当变量映射为空时的处理逻辑
     */
    @Test
    @DisplayName("测试空变量映射 - 应保留所有变量占位符")
    void testEmptyVariableMap() {
        // 准备测试数据
        SqlTemplate template = new SqlTemplate();
        template.setTemplate("SELECT * FROM ${schema}.${table} WHERE id = :id");
        
        // 空变量映射
        Map<String, String> variables = Map.of();
        
        // 执行变量替换
        String result = templateVariableReplacer.replaceVariables(template, variables);
        
        // 验证结果
        assertNotNull(result, "替换结果不应为null");
        assertEquals(template.getTemplate(), result, "空变量映射时应返回原模板");
        assertTrue(result.contains("${schema}"), "应保留schema变量占位符");
        assertTrue(result.contains("${table}"), "应保留table变量占位符");
    }
    
    /**
     * 测试6：null模板处理测试
     * 验证当模板为null时的处理逻辑
     */
    @Test
    @DisplayName("测试null模板处理 - 应返回空字符串")
    void testNullTemplateHandling() {
        // null模板
        SqlTemplate template = null;
        Map<String, String> variables = Map.of("key", "value");
        
        // 执行变量替换
        String result = templateVariableReplacer.replaceVariables(template, variables);
        
        // 验证结果
        assertNotNull(result, "结果不应为null");
        assertEquals("", result, "null模板时应返回空字符串");
    }
    
    /**
     * 测试7：null模板字符串处理测试
     * 验证当模板字符串为null时的处理逻辑
     */
    @Test
    @DisplayName("测试null模板字符串处理 - 应返回空字符串")
    void testNullTemplateStringHandling() {
        // 模板字符串为null
        SqlTemplate template = new SqlTemplate();
        template.setTemplate(null);
        Map<String, String> variables = Map.of("key", "value");
        
        // 执行变量替换
        String result = templateVariableReplacer.replaceVariables(template, variables);
        
        // 验证结果
        assertNotNull(result, "结果不应为null");
        assertEquals("", result, "null模板字符串时应返回空字符串");
    }
    
    /**
     * 测试8：空模板字符串处理测试
     * 验证当模板字符串为空时的处理逻辑
     */
    @Test
    @DisplayName("测试空模板字符串处理 - 应返回空字符串")
    void testEmptyTemplateStringHandling() {
        // 空模板字符串
        SqlTemplate template = new SqlTemplate();
        template.setTemplate("");
        Map<String, String> variables = Map.of("key", "value");
        
        // 执行变量替换
        String result = templateVariableReplacer.replaceVariables(template, variables);
        
        // 验证结果
        assertNotNull(result, "结果不应为null");
        assertEquals("", result, "空模板字符串时应返回空字符串");
    }
    
    /**
     * 测试9：重复变量替换测试
     * 验证同一变量多次出现的替换
     */
    @Test
    @DisplayName("测试重复变量替换 - 应替换所有出现的变量")
    void testDuplicateVariableReplacement() {
        // 准备测试数据 - 同一变量多次出现
        SqlTemplate template = new SqlTemplate();
        template.setTemplate("SELECT * FROM ${schema}.table1 t1 JOIN ${schema}.table2 t2 ON t1.id = t2.id WHERE t1.${statusField} = :status1 AND t2.${statusField} = :status2");
        
        Map<String, String> variables = Map.of(
            "schema", "hospital",
            "statusField", "patient_status"
        );
        
        // 执行变量替换
        String result = templateVariableReplacer.replaceVariables(template, variables);
        
        // 验证结果
        assertNotNull(result, "替换结果不应为null");
        assertTrue(result.contains("hospital.table1"), "应替换第一个schema变量");
        assertTrue(result.contains("hospital.table2"), "应替换第二个schema变量");
        assertTrue(result.contains("t1.patient_status"), "应替换第一个statusField变量");
        assertTrue(result.contains("t2.patient_status"), "应替换第二个statusField变量");
        assertFalse(result.contains("${schema}"), "不应包含未替换的schema变量占位符");
        assertFalse(result.contains("${statusField}"), "不应包含未替换的statusField变量占位符");
    }
    
    /**
     * 测试10：特殊字符变量名测试
     * 验证包含特殊字符的变量名替换
     */
    @Test
    @DisplayName("测试特殊字符变量名 - 应正确处理特殊字符")
    void testSpecialCharacterVariableNames() {
        // 准备测试数据 - 包含特殊字符的变量名
        SqlTemplate template = new SqlTemplate();
        template.setTemplate("SELECT * FROM ${table-prefix}.${table_name} WHERE ${column.name} = :value");
        
        Map<String, String> variables = Map.of(
            "table-prefix", "hospital",
            "table_name", "patient_records",
            "column.name", "patient_status"
        );
        
        // 执行变量替换
        String result = templateVariableReplacer.replaceVariables(template, variables);
        
        // 验证结果
        assertNotNull(result, "替换结果不应为null");
        assertEquals("SELECT * FROM hospital.patient_records WHERE patient_status = :value", 
            result, "应正确替换包含特殊字符的变量");
        assertFalse(result.contains("${"), "不应包含任何未替换的变量占位符");
    }
    
    /**
     * 测试11：变量值包含特殊字符测试
     * 验证变量值包含特殊字符时的替换
     */
    @Test
    @DisplayName("测试变量值包含特殊字符 - 应正确处理特殊字符值")
    void testSpecialCharacterVariableValues() {
        // 准备测试数据 - 变量值包含特殊字符
        SqlTemplate template = new SqlTemplate();
        template.setTemplate("SELECT * FROM ${schema}.${table} WHERE name = :name");
        
        Map<String, String> variables = Map.of(
            "schema", "hospital-db",
            "table", "patient's_records"
        );
        
        // 执行变量替换
        String result = templateVariableReplacer.replaceVariables(template, variables);
        
        // 验证结果
        assertNotNull(result, "替换结果不应为null");
        assertEquals("SELECT * FROM hospital-db.patient's_records WHERE name = :name", 
            result, "应正确替换包含特殊字符的变量值");
        assertFalse(result.contains("${"), "不应包含任何未替换的变量占位符");
    }
    
    /**
     * 测试12：null变量值处理测试
     * 验证当变量值为null时的处理逻辑
     */
    @Test
    @DisplayName("测试null变量值处理 - 应将null值替换为空字符串")
    void testNullVariableValueHandling() {
        // 准备测试数据
        SqlTemplate template = new SqlTemplate();
        template.setTemplate("SELECT * FROM ${schema}.${table} WHERE id = :id");
        
        // 变量值为null - 使用HashMap允许null值
        Map<String, String> variables = new java.util.HashMap<>();
        variables.put("schema", "hospital");
        variables.put("table", null);
        
        // 执行变量替换
        String result = templateVariableReplacer.replaceVariables(template, variables);
        
        // 验证结果
        assertNotNull(result, "替换结果不应为null");
        assertTrue(result.contains("hospital"), "应替换schema变量");
        assertTrue(result.contains(". WHERE"), "null变量值应替换为空字符串");
        assertFalse(result.contains("${schema}"), "不应包含已替换的schema变量占位符");
    }
}
