package com.example.medaiassistant.hospital.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 医嘱同步配置管理模块测试
 * 
 * TDD绿阶段完成：验证OrderSyncConfig配置类功能
 * - 模板路径构建（5个测试用例）
 * - 默认配置值（3个测试用例）
 * - 配置验证（3个测试用例）
 * 
 * 测试覆盖：
 * - 正常场景：本地医院、测试服务器路径构建
 * - 边界场景：空值、null值处理、大小写转换
 * - 配置获取：模板文件名、查询名称、默认医院ID
 * - 验证功能：有效/无效医院ID验证
 * - 配置摘要：配置信息摘要获取
 * 
 * 测试结果：12个测试全部通过
 * 
 * @author TDD
 * @version 1.0
 * @since 2026-01-10
 */
@DisplayName("医嘱同步配置管理测试")
class OrderSyncConfigTest {

    private OrderSyncConfig config;

    /**
     * 测试初始化
     * 每个测试方法执行前创建新的配置实例
     */
    @BeforeEach
    void setUp() {
        config = new OrderSyncConfig();
    }

    // ==================== 模板路径构建测试 ====================

    /**
     * 测试用例 CFG-01: 验证本地医院模板路径构建
     * 
     * <p>验证当传入hospital-Local时，路径应转换为小写格式</p>
     * 
     * @see OrderSyncConfig#getTemplateFilePath(String)
     */
    @Test
    @DisplayName("CFG-01: 应正确构建本地医院的模板文件路径")
    void shouldBuildTemplatePathForLocalHospital() {
        // Given
        String hospitalId = "hospital-Local";
        
        // When
        String templatePath = config.getTemplateFilePath(hospitalId);
        
        // Then
        assertEquals("sql/hospital-local/orders-query.json", templatePath, 
            "本地医院模板路径应为小写格式");
    }

    /**
     * 测试用例 CFG-02: 验证测试服务器模板路径构建
     * 
     * <p>验证小写医院ID的路径构建正确性</p>
     * 
     * @see OrderSyncConfig#getTemplateFilePath(String)
     */
    @Test
    @DisplayName("CFG-02: 应正确构建测试服务器的模板文件路径")
    void shouldBuildTemplatePathForTestServer() {
        // Given
        String hospitalId = "testserver";
        
        // When
        String templatePath = config.getTemplateFilePath(hospitalId);
        
        // Then
        assertEquals("sql/testserver/orders-query.json", templatePath,
            "测试服务器模板路径应正确构建");
    }

    /**
     * 测试用例 CFG-03: 验证空字符串医院ID的默认处理
     * 
     * <p>边界条件：空字符串应使用默认医院ID</p>
     * 
     * @see OrderSyncConfig#getTemplateFilePath(String)
     */
    @Test
    @DisplayName("CFG-03: 空医院ID应返回默认模板路径")
    void shouldReturnDefaultPathForEmptyHospitalId() {
        // Given
        String hospitalId = "";
        
        // When
        String templatePath = config.getTemplateFilePath(hospitalId);
        
        // Then
        assertEquals("sql/hospital-local/orders-query.json", templatePath,
            "空医院ID应使用默认路径");
    }

    /**
     * 测试用例 CFG-04: 验证null医院ID的默认处理
     * 
     * <p>边界条件：null值应使用默认医院ID</p>
     * 
     * @see OrderSyncConfig#getTemplateFilePath(String)
     */
    @Test
    @DisplayName("CFG-04: null医院ID应返回默认模板路径")
    void shouldReturnDefaultPathForNullHospitalId() {
        // When
        String templatePath = config.getTemplateFilePath(null);
        
        // Then
        assertEquals("sql/hospital-local/orders-query.json", templatePath,
            "null医院ID应使用默认路径");
    }

    /**
     * 测试用例: 验证大写医院ID的小写转换
     * 
     * <p>验证Linux环境大小写敏感问题的处理</p>
     * 
     * @see OrderSyncConfig#getTemplateFilePath(String)
     */
    @Test
    @DisplayName("大写医院ID应转换为小写路径")
    void shouldConvertUppercaseHospitalIdToLowercase() {
        // Given
        String hospitalId = "CDWYY";
        
        // When
        String templatePath = config.getTemplateFilePath(hospitalId);
        
        // Then
        assertEquals("sql/cdwyy/orders-query.json", templatePath,
            "大写医院ID应转换为小写路径");
    }

    // ==================== 默认配置测试 ====================

    /**
     * 测试用例 CFG-05: 验证模板文件名获取
     * 
     * <p>预期返回: orders-query.json</p>
     * 
     * @see OrderSyncConfig#getTemplateFileName()
     */
    @Test
    @DisplayName("CFG-05: 应返回正确的模板文件名")
    void shouldReturnCorrectTemplateFileName() {
        // When
        String fileName = config.getTemplateFileName();
        
        // Then
        assertEquals("orders-query.json", fileName,
            "模板文件名应为orders-query.json");
    }

    /**
     * 测试用例 CFG-06: 验证查询名称获取
     * 
     * <p>预期返回: getOrders</p>
     * 
     * @see OrderSyncConfig#getQueryName()
     */
    @Test
    @DisplayName("CFG-06: 应返回正确的查询名称")
    void shouldReturnCorrectQueryName() {
        // When
        String queryName = config.getQueryName();
        
        // Then
        assertEquals("getOrders", queryName,
            "查询名称应为getOrders");
    }

    /**
     * 测试用例: 验证默认医院ID获取
     * 
     * <p>预期返回: hospital-Local</p>
     * 
     * @see OrderSyncConfig#getDefaultHospitalId()
     */
    @Test
    @DisplayName("应返回正确的默认医院ID")
    void shouldReturnDefaultHospitalId() {
        // When
        String defaultId = config.getDefaultHospitalId();
        
        // Then
        assertEquals("hospital-Local", defaultId,
            "默认医院ID应为hospital-Local");
    }

    // ==================== 配置验证测试 ====================

    /**
     * 测试用例 CFG-07: 验证有效医院ID的校验
     * 
     * <p>非空字符串应返回true</p>
     * 
     * @see OrderSyncConfig#isValidHospitalId(String)
     */
    @Test
    @DisplayName("CFG-07: 应正确验证有效的医院ID")
    void shouldValidateValidHospitalId() {
        // Given
        String validHospitalId = "hospital-Local";
        
        // When
        boolean isValid = config.isValidHospitalId(validHospitalId);
        
        // Then
        assertTrue(isValid, "有效的医院ID应验证通过");
    }

    /**
     * 测试用例 CFG-08: 验证无效医院ID的校验
     * 
     * <p>空字符串和null应返回false</p>
     * 
     * @see OrderSyncConfig#isValidHospitalId(String)
     */
    @Test
    @DisplayName("CFG-08: 应拒绝无效的医院ID")
    void shouldRejectInvalidHospitalId() {
        // Given - 空字符串
        String emptyHospitalId = "";
        
        // When
        boolean isEmptyValid = config.isValidHospitalId(emptyHospitalId);
        
        // Then
        assertFalse(isEmptyValid, "空医院ID应验证失败");
        
        // Given - null值
        // When
        boolean isNullValid = config.isValidHospitalId(null);
        
        // Then
        assertFalse(isNullValid, "null医院ID应验证失败");
    }

    /**
     * 测试用例: 验证null医院ID的单独校验
     * 
     * <p>null值应返回false</p>
     * 
     * @see OrderSyncConfig#isValidHospitalId(String)
     */
    @Test
    @DisplayName("应拒绝null医院ID")
    void shouldRejectNullHospitalId() {
        // When
        boolean isValid = config.isValidHospitalId(null);
        
        // Then
        assertFalse(isValid, "null医院ID应验证失败");
    }

    // ==================== 配置摘要测试 ====================

    /**
     * 测试用例: 验证配置摘要信息
     * 
     * <p>摘要应包含类名、默认医院ID、模板文件名和查询名称</p>
     * 
     * @see OrderSyncConfig#getSummary()
     */
    @Test
    @DisplayName("应返回正确的配置摘要")
    void shouldReturnCorrectSummary() {
        // When
        String summary = config.getSummary();
        
        // Then
        assertNotNull(summary, "配置摘要不应为null");
        assertTrue(summary.contains("OrderSyncConfig"), "摘要应包含类名");
        assertTrue(summary.contains("hospital-Local"), "摘要应包含默认医院ID");
        assertTrue(summary.contains("orders-query.json"), "摘要应包含模板文件名");
        assertTrue(summary.contains("getOrders"), "摘要应包含查询名称");
    }
}
