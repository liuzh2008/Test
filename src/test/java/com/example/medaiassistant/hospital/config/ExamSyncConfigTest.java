package com.example.medaiassistant.hospital.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 检查结果同步配置管理模块测试
 * 
 * TDD绿阶段完成：验证ExamSyncConfig配置类功能
 * - 模板路径构建（5个测试用例）
 * - 默认配置值（2个测试用例）
 * - 查询名称生成（1个测试用例）
 * - 配置验证（3个测试用例）
 * 
 * 测试覆盖：
 * - 正常场景：本地医院、测试服务器路径构建
 * - 边界场景：空值、null值、大小写转换
 * - 配置获取：模板文件名、默认医院ID、查询名称
 * - 验证功能：有效/无效医院ID验证
 * 
 * @author TDD
 * @version 1.0
 * @since 2025-12-29
 */
@DisplayName("检查结果同步配置管理测试")
class ExamSyncConfigTest {

    private ExamSyncConfig config;

    @BeforeEach
    void setUp() {
        config = new ExamSyncConfig();
    }

    // ==================== 模板路径构建测试 ====================

    @Test
    @DisplayName("应正确构建本地医院的模板文件路径")
    void shouldBuildTemplatePathForLocalHospital() {
        // Given
        String hospitalId = "hospital-Local";
        
        // When
        String templatePath = config.getTemplateFilePath(hospitalId);
        
        // Then
        assertEquals("sql/hospital-local/exam-results-query.json", templatePath, 
            "本地医院模板路径应为小写格式");
    }

    @Test
    @DisplayName("应正确构建测试服务器的模板文件路径")
    void shouldBuildTemplatePathForTestServer() {
        // Given
        String hospitalId = "testserver";
        
        // When
        String templatePath = config.getTemplateFilePath(hospitalId);
        
        // Then
        assertEquals("sql/testserver/exam-results-query.json", templatePath,
            "测试服务器模板路径应正确构建");
    }

    @Test
    @DisplayName("空医院ID应返回默认模板路径")
    void shouldReturnDefaultPathForEmptyHospitalId() {
        // Given
        String hospitalId = "";
        
        // When
        String templatePath = config.getTemplateFilePath(hospitalId);
        
        // Then
        assertEquals("sql/hospital-local/exam-results-query.json", templatePath,
            "空医院ID应使用默认路径");
    }

    @Test
    @DisplayName("null医院ID应返回默认模板路径")
    void shouldReturnDefaultPathForNullHospitalId() {
        // When
        String templatePath = config.getTemplateFilePath(null);
        
        // Then
        assertEquals("sql/hospital-local/exam-results-query.json", templatePath,
            "null医院ID应使用默认路径");
    }

    @Test
    @DisplayName("大写医院ID应转换为小写路径")
    void shouldConvertUppercaseHospitalIdToLowercase() {
        // Given
        String hospitalId = "CDWYY";
        
        // When
        String templatePath = config.getTemplateFilePath(hospitalId);
        
        // Then
        assertEquals("sql/cdwyy/exam-results-query.json", templatePath,
            "大写医院ID应转换为小写路径");
    }

    // ==================== 默认配置测试 ====================

    @Test
    @DisplayName("应返回正确的模板文件名")
    void shouldReturnCorrectTemplateFileName() {
        // When
        String fileName = config.getTemplateFileName();
        
        // Then
        assertEquals("exam-results-query.json", fileName,
            "模板文件名应为exam-results-query.json");
    }

    @Test
    @DisplayName("应返回正确的默认医院ID")
    void shouldReturnDefaultHospitalId() {
        // When
        String defaultId = config.getDefaultHospitalId();
        
        // Then
        assertEquals("hospital-Local", defaultId,
            "默认医院ID应为hospital-Local");
    }

    // ==================== 查询名称测试 ====================

    @Test
    @DisplayName("应返回正确的查询名称")
    void shouldReturnCorrectQueryName() {
        // When
        String queryName = config.getQueryName();
        
        // Then
        assertEquals("getExamResults", queryName,
            "查询名称应为getExamResults");
    }

    // ==================== 配置验证测试 ====================

    @Test
    @DisplayName("应正确验证有效的医院ID")
    void shouldValidateValidHospitalId() {
        // Given
        String validHospitalId = "hospital-Local";
        
        // When
        boolean isValid = config.isValidHospitalId(validHospitalId);
        
        // Then
        assertTrue(isValid, "有效的医院ID应验证通过");
    }

    @Test
    @DisplayName("应拒绝无效的医院ID")
    void shouldRejectInvalidHospitalId() {
        // Given
        String invalidHospitalId = "";
        
        // When
        boolean isValid = config.isValidHospitalId(invalidHospitalId);
        
        // Then
        assertFalse(isValid, "空医院ID应验证失败");
    }

    @Test
    @DisplayName("应拒绝null医院ID")
    void shouldRejectNullHospitalId() {
        // When
        boolean isValid = config.isValidHospitalId(null);
        
        // Then
        assertFalse(isValid, "null医院ID应验证失败");
    }
}
