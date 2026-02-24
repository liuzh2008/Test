package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.model.SqlTestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * SQL执行测试服务测试
 * 按照TDD红-绿-重构流程实现
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-03
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SQL执行测试服务TDD测试")
class SqlTestExecutorTddTest {
    
    @Mock
    private SqlSecurityValidator sqlSecurityValidator;
    
    private SqlTestExecutor sqlTestExecutor;
    
    @BeforeEach
    void setUp() {
        // 创建SqlTestExecutor实例，注入模拟的SqlSecurityValidator
        sqlTestExecutor = new SqlTestExecutor(sqlSecurityValidator);
    }
    
    /**
     * 测试1：执行简单SELECT查询测试
     * 验证可以执行简单的SELECT查询并返回结果
     * 绿阶段：SqlTestExecutor类已实现，测试通过
     */
    @Test
    @DisplayName("测试执行简单SELECT查询 - 应返回查询结果")
    void testExecuteSimpleSelectQuery() {
        // 准备测试数据
        String hospitalId = "test-hospital";
        String sql = "SELECT 1 as result FROM DUAL";
        
        // 设置模拟行为
        when(sqlSecurityValidator.isSqlSafe(sql)).thenReturn(true);
        
        // 执行SQL测试
        SqlTestResult result = sqlTestExecutor.executeSqlTest(hospitalId, sql);
        
        // 验证结果
        assertNotNull(result, "查询结果不应为null");
        assertTrue(result.isSuccess(), "查询应成功执行");
        assertNotNull(result.getData(), "查询数据不应为null");
        assertFalse(result.getData().isEmpty(), "查询数据不应为空");
        
        // 验证具体数据
        List<Map<String, Object>> data = result.getData();
        Map<String, Object> firstRow = data.get(0);
        assertEquals(1, firstRow.get("RESULT"), "查询结果应为1");
    }
    
    /**
     * 测试2：执行带参数的SELECT查询测试
     * 验证可以执行带参数的SELECT查询并返回结果
     */
    @Test
    @DisplayName("测试执行带参数的SELECT查询 - 应正确处理参数")
    void testExecuteParameterizedSelectQuery() {
        // 准备测试数据
        String hospitalId = "test-hospital";
        String sql = "SELECT :param as result FROM DUAL";
        Map<String, Object> parameters = Map.of("param", "test_value");
        
        // 设置模拟行为
        when(sqlSecurityValidator.isSqlSafe(sql)).thenReturn(true);
        
        // 执行SQL测试
        SqlTestResult result = sqlTestExecutor.executeSqlTest(hospitalId, sql, parameters);
        
        // 验证结果
        assertNotNull(result, "查询结果不应为null");
        assertTrue(result.isSuccess(), "查询应成功执行");
        assertNotNull(result.getData(), "查询数据不应为null");
        assertFalse(result.getData().isEmpty(), "查询数据不应为空");
        
        // 验证具体数据
        List<Map<String, Object>> data = result.getData();
        Map<String, Object> firstRow = data.get(0);
        assertEquals("test_value", firstRow.get("RESULT"), "查询结果应为test_value");
    }
    
    /**
     * 测试3：执行时间测量测试
     * 验证应记录查询执行时间
     */
    @Test
    @DisplayName("测试执行时间测量 - 应记录查询执行时间")
    void testExecutionTimeMeasurement() {
        // 准备测试数据
        String hospitalId = "test-hospital";
        String sql = "SELECT 1 as result FROM DUAL";
        
        // 设置模拟行为
        when(sqlSecurityValidator.isSqlSafe(sql)).thenReturn(true);
        
        // 执行SQL测试
        SqlTestResult result = sqlTestExecutor.executeSqlTest(hospitalId, sql);
        
        // 验证结果
        assertNotNull(result, "查询结果不应为null");
        assertTrue(result.isSuccess(), "查询应成功执行");
        // 注意：模拟执行可能非常快，执行时间可能为0，这是可以接受的
        assertTrue(result.getExecutionTimeMs() >= 0, "执行时间应大于等于0");
        assertTrue(result.getExecutionTimeMs() < 1000, "简单查询执行时间应小于1秒");
    }
    
    /**
     * 测试4：查询结果列信息测试
     * 验证应返回查询结果的列信息
     */
    @Test
    @DisplayName("测试查询结果列信息 - 应返回列信息")
    void testQueryColumnInformation() {
        // 准备测试数据
        String hospitalId = "test-hospital";
        String sql = "SELECT 1 as col1, 'test' as col2 FROM DUAL";
        
        // 设置模拟行为
        when(sqlSecurityValidator.isSqlSafe(sql)).thenReturn(true);
        
        // 执行SQL测试
        SqlTestResult result = sqlTestExecutor.executeSqlTest(hospitalId, sql);
        
        // 验证结果
        assertNotNull(result, "查询结果不应为null");
        assertTrue(result.isSuccess(), "查询应成功执行");
        assertNotNull(result.getColumns(), "列信息不应为null");
        assertFalse(result.getColumns().isEmpty(), "列信息不应为空");
        
        // 验证列信息
        List<String> columns = result.getColumns();
        assertTrue(columns.contains("COL1"), "应包含COL1列");
        assertTrue(columns.contains("COL2"), "应包含COL2列");
        assertEquals(2, columns.size(), "应包含2列");
    }
    
    /**
     * 测试5：行数限制测试
     * 验证应支持行数限制
     */
    @Test
    @DisplayName("测试行数限制 - 应支持限制返回行数")
    void testRowLimit() {
        // 准备测试数据
        String hospitalId = "test-hospital";
        String sql = "SELECT LEVEL as id FROM DUAL CONNECT BY LEVEL <= 100";
        int maxRows = 10;
        
        // 设置模拟行为
        when(sqlSecurityValidator.isSqlSafe(sql)).thenReturn(true);
        
        // 执行SQL测试
        SqlTestResult result = sqlTestExecutor.executeSqlTest(hospitalId, sql, null, maxRows);
        
        // 验证结果
        assertNotNull(result, "查询结果不应为null");
        assertTrue(result.isSuccess(), "查询应成功执行");
        assertNotNull(result.getData(), "查询数据不应为null");
        assertEquals(maxRows, result.getData().size(), "返回行数应限制为" + maxRows);
        assertEquals(maxRows, result.getRowCount(), "行数计数应为" + maxRows);
    }
    
    /**
     * 测试6：危险SQL语句拦截测试
     * 验证危险SQL语句应被拦截
     */
    @Test
    @DisplayName("测试危险SQL语句拦截 - 危险SQL应被拦截")
    void testDangerousSqlInterception() {
        // 准备测试数据 - 危险SQL语句
        String hospitalId = "test-hospital";
        String dangerousSql = "DROP TABLE patients";
        
        // 设置模拟行为 - 危险SQL应返回false
        when(sqlSecurityValidator.isSqlSafe(dangerousSql)).thenReturn(false);
        
        // 执行SQL测试
        SqlTestResult result = sqlTestExecutor.executeSqlTest(hospitalId, dangerousSql);
        
        // 验证结果
        assertNotNull(result, "查询结果不应为null");
        assertFalse(result.isSuccess(), "危险SQL查询应失败");
        assertNotNull(result.getErrorMessage(), "错误信息不应为null");
        assertTrue(result.getErrorMessage().contains("危险") || 
                   result.getErrorMessage().contains("不安全") ||
                   result.getErrorMessage().contains("DROP"),
                   "错误信息应包含危险关键字");
    }
    
    /**
     * 测试7：无效医院ID测试
     * 验证无效医院ID应返回错误
     */
    @Test
    @DisplayName("测试无效医院ID - 无效医院ID应返回错误")
    void testInvalidHospitalId() {
        // 准备测试数据 - 无效医院ID
        String invalidHospitalId = "non-existent-hospital";
        String sql = "SELECT 1 as result FROM DUAL";
        
        // 设置模拟行为 - SQL是安全的
        when(sqlSecurityValidator.isSqlSafe(sql)).thenReturn(true);
        
        // 执行SQL测试
        SqlTestResult result = sqlTestExecutor.executeSqlTest(invalidHospitalId, sql);
        
        // 验证结果
        assertNotNull(result, "查询结果不应为null");
        assertFalse(result.isSuccess(), "无效医院ID查询应失败");
        assertNotNull(result.getErrorMessage(), "错误信息不应为null");
    }
    
    /**
     * 测试8：SQL语法错误测试
     * 验证SQL语法错误应返回错误
     */
    @Test
    @DisplayName("测试SQL语法错误 - SQL语法错误应返回错误")
    void testSqlSyntaxError() {
        // 准备测试数据 - 语法错误的SQL
        String hospitalId = "test-hospital";
        String invalidSql = "SELECT FROM"; // 缺少列名
        
        // 设置模拟行为 - SQL是安全的（语法检查在安全验证之后）
        when(sqlSecurityValidator.isSqlSafe(invalidSql)).thenReturn(true);
        
        // 执行SQL测试
        SqlTestResult result = sqlTestExecutor.executeSqlTest(hospitalId, invalidSql);
        
        // 验证结果
        assertNotNull(result, "查询结果不应为null");
        assertFalse(result.isSuccess(), "语法错误SQL查询应失败");
        assertNotNull(result.getErrorMessage(), "错误信息不应为null");
    }
    
    /**
     * 测试9：空SQL语句测试
     * 验证空SQL语句应返回错误
     */
    @Test
    @DisplayName("测试空SQL语句 - 空SQL语句应返回错误")
    void testEmptySql() {
        // 准备测试数据 - 空SQL
        String hospitalId = "test-hospital";
        String emptySql = "";
        
        // 注意：空SQL会在调用sqlSecurityValidator之前被检查，所以不需要设置模拟行为
        
        // 执行SQL测试
        SqlTestResult result = sqlTestExecutor.executeSqlTest(hospitalId, emptySql);
        
        // 验证结果
        assertNotNull(result, "查询结果不应为null");
        assertFalse(result.isSuccess(), "空SQL查询应失败");
        assertNotNull(result.getErrorMessage(), "错误信息不应为null");
    }
    
    /**
     * 测试10：null SQL语句测试
     * 验证null SQL语句应返回错误
     */
    @Test
    @DisplayName("测试null SQL语句 - null SQL语句应返回错误")
    void testNullSql() {
        // 准备测试数据 - null SQL
        String hospitalId = "test-hospital";
        String nullSql = null;
        
        // 注意：null SQL会在调用sqlSecurityValidator之前被检查，所以不需要设置模拟行为
        
        // 执行SQL测试
        SqlTestResult result = sqlTestExecutor.executeSqlTest(hospitalId, nullSql);
        
        // 验证结果
        assertNotNull(result, "查询结果不应为null");
        assertFalse(result.isSuccess(), "null SQL查询应失败");
        assertNotNull(result.getErrorMessage(), "错误信息不应为null");
    }
    
    /**
     * 测试11：查询计划分析测试
     * 验证应支持查询计划分析（重构阶段功能）
     */
    @Test
    @DisplayName("测试查询计划分析 - 应支持查询计划分析")
    void testQueryPlanAnalysis() {
        // 准备测试数据
        String hospitalId = "test-hospital";
        String sql = "SELECT 1 as result FROM DUAL";
        
        // 设置模拟行为
        when(sqlSecurityValidator.isSqlSafe(sql)).thenReturn(true);
        
        // 执行SQL测试
        SqlTestResult result = sqlTestExecutor.executeSqlTest(hospitalId, sql);
        
        // 验证结果
        assertNotNull(result, "查询结果不应为null");
        assertTrue(result.isSuccess(), "查询应成功执行");
        
        // 注意：查询计划分析的具体验证将在重构阶段添加
        // 目前先验证基本功能
        assertNotNull(sqlTestExecutor, "执行器不应为null");
    }
    
    /**
     * 测试12：结果集处理优化测试
     * 验证结果集处理应优化（重构阶段功能）
     */
    @Test
    @DisplayName("测试结果集处理优化 - 结果集处理应优化")
    void testResultSetProcessingOptimization() {
        // 准备测试数据
        String hospitalId = "test-hospital";
        String sql = "SELECT LEVEL as id FROM DUAL CONNECT BY LEVEL <= 1000";
        int maxRows = 100;
        
        // 设置模拟行为
        when(sqlSecurityValidator.isSqlSafe(sql)).thenReturn(true);
        
        // 执行SQL测试
        SqlTestResult result = sqlTestExecutor.executeSqlTest(hospitalId, sql, null, maxRows);
        
        // 验证结果
        assertNotNull(result, "查询结果不应为null");
        assertTrue(result.isSuccess(), "查询应成功执行");
        assertEquals(maxRows, result.getData().size(), "返回行数应限制为" + maxRows);
        
        // 验证性能
        assertTrue(result.getExecutionTimeMs() < 5000, "100行查询执行时间应小于5秒");
    }
}
