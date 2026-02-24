package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.model.SqlTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SQL模板解析器测试
 * 按照TDD红-绿-重构流程实现的最终测试
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-03
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SQL模板解析器测试")
class SqlTemplateParserTest {
    
    private ObjectMapper objectMapper;
    private JsonTemplateParser jsonTemplateParser;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        jsonTemplateParser = new JsonTemplateParser();
    }
    
    /**
     * 测试1：SQLTemplate实体结构测试
     * 验证实体类的基本字段和方法
     */
    @Test
    @DisplayName("测试SQLTemplate实体结构 - 应正确创建实体")
    void testSqlTemplateEntityStructure() {
        SqlTemplate template = new SqlTemplate();
        template.setQueryName("getPatientBasicInfo");
        template.setDescription("根据患者ID和就诊ID查询患者基本信息");
        template.setTemplate("SELECT * FROM ${tablePrefix}.his_in_pati_reg WHERE PATI_ID = :PATI_ID AND VISIT_ID = :VISIT_ID");
        
        assertNotNull(template, "SqlTemplate对象不应为null");
        assertEquals("getPatientBasicInfo", template.getQueryName(), "查询名称应匹配");
        assertEquals("根据患者ID和就诊ID查询患者基本信息", template.getDescription(), "描述应匹配");
        assertTrue(template.containsVariables(), "模板应包含变量");
        
        List<String> variables = template.extractVariableNames();
        assertNotNull(variables, "提取的变量列表不应为null");
        assertEquals(1, variables.size(), "应包含1个变量");
        assertTrue(variables.contains("tablePrefix"), "应包含tablePrefix变量");
    }
    
    /**
     * 测试2：JSON模板文件解析测试
     * 验证JSON字符串能正确解析为SqlTemplate对象
     */
    @Test
    @DisplayName("测试JSON模板文件解析 - 应正确解析JSON")
    void testJsonTemplateParsing() throws Exception {
        String jsonTemplate = """
            {
                "queryName": "getPatientBasicInfo",
                "description": "根据患者ID和就诊ID查询患者基本信息",
                "template": "SELECT * FROM ${tablePrefix}.his_in_pati_reg WHERE PATI_ID = :PATI_ID AND VISIT_ID = :VISIT_ID",
                "parameters": [
                    {
                        "name": "PATI_ID",
                        "type": "String",
                        "required": true,
                        "description": "患者ID"
                    },
                    {
                        "name": "VISIT_ID",
                        "type": "String",
                        "required": true,
                        "description": "就诊ID"
                    }
                ],
                "metadata": {
                    "category": "patient",
                    "version": "1.0",
                    "lastModified": "2025-12-03",
                    "author": "系统管理员"
                }
            }
            """;
        
        // 使用ObjectMapper直接解析，测试实体结构
        SqlTemplate template = objectMapper.readValue(jsonTemplate, SqlTemplate.class);
        
        assertNotNull(template, "解析后的SqlTemplate不应为null");
        assertEquals("getPatientBasicInfo", template.getQueryName(), "查询名称应匹配");
        assertEquals("patient", template.getMetadata().getCategory(), "分类应匹配");
        assertNotNull(template.getParameters(), "参数列表不应为null");
        assertEquals(2, template.getParameters().size(), "应有2个参数");
        
        // 验证参数
        SqlTemplate.Parameter param1 = template.getParameters().get(0);
        assertEquals("PATI_ID", param1.getName(), "第一个参数名称应为PATI_ID");
        assertTrue(param1.getRequired(), "PATI_ID参数应为必需");
        
        // 验证模板变量提取
        List<String> variables = template.extractVariableNames();
        assertNotNull(variables, "提取的变量列表不应为null");
        assertEquals(1, variables.size(), "应包含1个变量");
        assertTrue(variables.contains("tablePrefix"), "应包含tablePrefix变量");
    }
    
    /**
     * 测试3：模板变量提取测试
     * 验证能从模板字符串中正确提取变量名
     */
    @Test
    @DisplayName("测试模板变量提取 - 应正确提取变量名")
    void testTemplateVariableExtraction() {
        SqlTemplate template = new SqlTemplate();
        template.setTemplate("SELECT * FROM ${tablePrefix}.his_in_pati_reg WHERE DEPT_NAME = :deptName AND ${statusField} = :status");
        
        List<String> variables = template.extractVariableNames();
        
        assertNotNull(variables, "提取的变量列表不应为null");
        assertEquals(2, variables.size(), "应提取2个变量");
        assertTrue(variables.contains("tablePrefix"), "应包含tablePrefix变量");
        assertTrue(variables.contains("statusField"), "应包含statusField变量");
        assertFalse(variables.contains("deptName"), "不应包含命名参数deptName");
        assertFalse(variables.contains("status"), "不应包含命名参数status");
    }
    
    /**
     * 测试4：参数验证测试
     * 验证参数验证逻辑是否正确
     */
    @Test
    @DisplayName("测试参数验证 - 应正确验证输入参数")
    void testParameterValidation() {
        SqlTemplate template = new SqlTemplate();
        
        // 创建参数列表
        SqlTemplate.Parameter requiredParam = new SqlTemplate.Parameter();
        requiredParam.setName("patientId");
        requiredParam.setType("String");
        requiredParam.setRequired(true);
        requiredParam.setDescription("患者ID");
        
        SqlTemplate.Parameter optionalParam = new SqlTemplate.Parameter();
        optionalParam.setName("department");
        optionalParam.setType("String");
        optionalParam.setRequired(false);
        optionalParam.setDescription("科室名称");
        
        template.setParameters(List.of(requiredParam, optionalParam));
        
        // 测试1：缺少必需参数
        Map<String, Object> missingParams = Map.of("department", "内科");
        boolean result1 = template.validateParameters(missingParams);
        assertFalse(result1, "缺少必需参数时应验证失败");
        
        // 测试2：包含必需参数
        Map<String, Object> validParams = Map.of("patientId", "P001", "department", "内科");
        boolean result2 = template.validateParameters(validParams);
        assertTrue(result2, "包含必需参数时应验证成功");
        
        // 测试3：包含所有参数（包括可选参数）
        Map<String, Object> allParams = Map.of("patientId", "P001", "department", "内科", "extra", "额外参数");
        boolean result3 = template.validateParameters(allParams);
        assertTrue(result3, "包含额外参数时应验证成功");
    }
    
    /**
     * 测试5：复杂模板解析测试
     * 验证包含多个变量和复杂结构的模板
     */
    @Test
    @DisplayName("测试复杂模板解析 - 应正确处理复杂模板")
    void testComplexTemplateParsing() throws Exception {
        String complexJson = """
            {
                "queryName": "getPatientWithConditions",
                "description": "根据多个条件查询患者信息",
                "template": "SELECT p.* FROM ${tablePrefix}.his_in_pati_reg p WHERE p.DEPT_NAME = :deptName AND p.${statusField} = :status AND p.ADMISSION_DATE >= :startDate",
                "parameters": [
                    {
                        "name": "deptName",
                        "type": "String",
                        "required": true,
                        "description": "科室名称"
                    },
                    {
                        "name": "status",
                        "type": "String",
                        "required": true,
                        "description": "状态代码"
                    },
                    {
                        "name": "startDate",
                        "type": "Date",
                        "required": false,
                        "description": "开始日期",
                        "defaultValue": "2025-01-01"
                    }
                ],
                "metadata": {
                    "category": "patient",
                    "version": "1.1",
                    "lastModified": "2025-12-03",
                    "author": "系统管理员",
                    "tags": ["complex", "multi-condition"],
                    "extensions": {
                        "maxRows": 1000,
                        "timeout": 30
                    }
                }
            }
            """;
        
        SqlTemplate template = objectMapper.readValue(complexJson, SqlTemplate.class);
        
        assertNotNull(template, "复杂模板解析不应为null");
        assertEquals("getPatientWithConditions", template.getQueryName(), "查询名称应匹配");
        assertEquals(3, template.getParameters().size(), "应有3个参数");
        
        // 验证元数据
        assertNotNull(template.getMetadata(), "元数据不应为null");
        assertEquals("1.1", template.getMetadata().getVersion(), "版本号应匹配");
        assertNotNull(template.getMetadata().getTags(), "标签列表不应为null");
        assertEquals(2, template.getMetadata().getTags().size(), "应有2个标签");
        assertNotNull(template.getMetadata().getExtensions(), "扩展属性不应为null");
        assertEquals(1000, template.getMetadata().getExtensions().get("maxRows"), "maxRows扩展属性应匹配");
        
        // 验证变量提取
        List<String> variables = template.extractVariableNames();
        assertNotNull(variables, "提取的变量列表不应为null");
        assertEquals(2, variables.size(), "应提取2个变量");
        assertTrue(variables.contains("tablePrefix"), "应包含tablePrefix变量");
        assertTrue(variables.contains("statusField"), "应包含statusField变量");
    }
    
    /**
     * 测试6：JsonTemplateParser服务测试
     * 验证JsonTemplateParser能正确解析JSON
     */
    @Test
    @DisplayName("测试JsonTemplateParser服务 - 应正确解析JSON模板")
    void testJsonTemplateParserService() throws Exception {
        String jsonTemplate = """
            {
                "queryName": "testQuery",
                "description": "测试查询",
                "template": "SELECT * FROM ${schema}.test_table WHERE id = :id",
                "parameters": [
                    {
                        "name": "id",
                        "type": "Integer",
                        "required": true,
                        "description": "ID"
                    }
                ],
                "metadata": {
                    "category": "test",
                    "version": "1.0"
                }
            }
            """;
        
        // 使用JsonTemplateParser解析
        SqlTemplate template = jsonTemplateParser.parseFromJson(jsonTemplate);
        
        assertNotNull(template, "解析后的SqlTemplate不应为null");
        assertEquals("testQuery", template.getQueryName(), "查询名称应匹配");
        assertEquals("test", template.getMetadata().getCategory(), "分类应匹配");
        
        // 验证模板验证
        boolean isValid = jsonTemplateParser.validateTemplate(template);
        assertTrue(isValid, "模板应验证通过");
        
        // 验证变量替换
        Map<String, String> variables = Map.of("schema", "test_schema");
        String replacedSql = jsonTemplateParser.replaceVariables(template, variables);
        assertNotNull(replacedSql, "替换后的SQL不应为null");
        assertTrue(replacedSql.contains("test_schema.test_table"), "应替换schema变量");
        assertFalse(replacedSql.contains("${schema}"), "不应包含未替换的变量占位符");
        
        // 验证生成SQL
        String generatedSql = jsonTemplateParser.generateSql(template, variables);
        assertEquals(replacedSql, generatedSql, "生成的SQL应与替换后的SQL相同");
    }
    
    /**
     * 测试7：JsonTemplateParser验证测试
     * 验证模板验证逻辑
     */
    @Test
    @DisplayName("测试JsonTemplateParser验证 - 应正确验证模板完整性")
    void testJsonTemplateParserValidation() {
        // 测试无效模板：缺少查询名称
        SqlTemplate invalidTemplate1 = new SqlTemplate();
        invalidTemplate1.setTemplate("SELECT * FROM table");
        boolean result1 = jsonTemplateParser.validateTemplate(invalidTemplate1);
        assertFalse(result1, "缺少查询名称的模板应验证失败");
        
        // 测试无效模板：缺少模板字符串
        SqlTemplate invalidTemplate2 = new SqlTemplate();
        invalidTemplate2.setQueryName("testQuery");
        boolean result2 = jsonTemplateParser.validateTemplate(invalidTemplate2);
        assertFalse(result2, "缺少模板字符串的模板应验证失败");
        
        // 测试无效模板：参数缺少名称
        SqlTemplate invalidTemplate3 = new SqlTemplate();
        invalidTemplate3.setQueryName("testQuery");
        invalidTemplate3.setTemplate("SELECT * FROM table");
        
        SqlTemplate.Parameter invalidParam = new SqlTemplate.Parameter();
        invalidParam.setType("String");
        invalidParam.setRequired(true);
        invalidTemplate3.setParameters(List.of(invalidParam));
        
        boolean result3 = jsonTemplateParser.validateTemplate(invalidTemplate3);
        assertFalse(result3, "参数缺少名称的模板应验证失败");
        
        // 测试有效模板
        SqlTemplate validTemplate = new SqlTemplate();
        validTemplate.setQueryName("validQuery");
        validTemplate.setTemplate("SELECT * FROM table WHERE id = :id");
        
        SqlTemplate.Parameter validParam = new SqlTemplate.Parameter();
        validParam.setName("id");
        validParam.setType("Integer");
        validParam.setRequired(true);
        validTemplate.setParameters(List.of(validParam));
        
        boolean result4 = jsonTemplateParser.validateTemplate(validTemplate);
        assertTrue(result4, "有效模板应验证通过");
    }
    
    /**
     * 测试8：变量替换测试
     * 验证变量替换功能
     */
    @Test
    @DisplayName("测试变量替换 - 应正确替换模板变量")
    void testVariableReplacement() {
        SqlTemplate template = new SqlTemplate();
        template.setQueryName("testQuery");
        template.setTemplate("SELECT * FROM ${schema}.${tableName} WHERE ${conditionField} = :value");
        
        Map<String, String> variables = Map.of(
            "schema", "test_schema",
            "tableName", "test_table",
            "conditionField", "status"
        );
        
        String replacedSql = jsonTemplateParser.replaceVariables(template, variables);
        
        assertNotNull(replacedSql, "替换后的SQL不应为null");
        assertEquals("SELECT * FROM test_schema.test_table WHERE status = :value", replacedSql, "应正确替换所有变量");
        
        // 测试部分变量替换
        Map<String, String> partialVariables = Map.of("schema", "partial_schema");
        String partialReplacedSql = jsonTemplateParser.replaceVariables(template, partialVariables);
        assertTrue(partialReplacedSql.contains("partial_schema"), "应替换schema变量");
        assertTrue(partialReplacedSql.contains("${tableName}"), "应保留未替换的tableName变量");
        assertTrue(partialReplacedSql.contains("${conditionField}"), "应保留未替换的conditionField变量");
    }
    
    /**
     * 测试9：边界条件测试 - 空模板
     */
    @Test
    @DisplayName("测试边界条件 - 空模板处理")
    void testEmptyTemplate() {
        SqlTemplate template = new SqlTemplate();
        template.setTemplate("");
        
        List<String> variables = template.extractVariableNames();
        assertNotNull(variables, "空模板的变量列表不应为null");
        assertTrue(variables.isEmpty(), "空模板应返回空变量列表");
        
        assertFalse(template.containsVariables(), "空模板不应包含变量");
    }
    
    /**
     * 测试10：边界条件测试 - 无变量模板
     */
    @Test
    @DisplayName("测试边界条件 - 无变量模板处理")
    void testTemplateWithoutVariables() {
        SqlTemplate template = new SqlTemplate();
        template.setTemplate("SELECT * FROM patients WHERE id = :id");
        
        List<String> variables = template.extractVariableNames();
        assertNotNull(variables, "无变量模板的变量列表不应为null");
        assertTrue(variables.isEmpty(), "无变量模板应返回空变量列表");
        
        assertFalse(template.containsVariables(), "无变量模板不应包含变量");
    }
    
    /**
     * 测试11：边界条件测试 - 重复变量
     */
    @Test
    @DisplayName("测试边界条件 - 重复变量处理")
    void testDuplicateVariables() {
        SqlTemplate template = new SqlTemplate();
        template.setTemplate("SELECT * FROM ${schema}.${table} WHERE ${schema} = :value AND ${table} = :tableValue");
        
        List<String> variables = template.extractVariableNames();
        assertNotNull(variables, "重复变量模板的变量列表不应为null");
        assertEquals(2, variables.size(), "应提取2个唯一变量");
        assertTrue(variables.contains("schema"), "应包含schema变量");
        assertTrue(variables.contains("table"), "应包含table变量");
    }
}
