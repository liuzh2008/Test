package com.example.medaiassistant.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DRG分析结果模型单元测试
 * 
 * @author MedAI Assistant Team
 * @since 2025-11-11
 */
class DrgAnalysisResultTest {

    /**
     * 测试：验证MCC类型枚举功能
     */
    @Test
    @DisplayName("当使用MCC类型枚举时，应该正确转换和验证")
    void mccTypeEnum_shouldWorkCorrectly() {
        // Given - 各种MCC类型代码
        String mccCode = "MCC";
        String ccCode = "CC";
        String noneCode = "NONE";
        String invalidCode = "INVALID";

        // When & Then - 验证枚举转换
        assertEquals(DrgAnalysisResult.MccType.MCC, DrgAnalysisResult.MccType.fromCode(mccCode));
        assertEquals(DrgAnalysisResult.MccType.CC, DrgAnalysisResult.MccType.fromCode(ccCode));
        assertEquals(DrgAnalysisResult.MccType.NONE, DrgAnalysisResult.MccType.fromCode(noneCode));
        assertEquals(DrgAnalysisResult.MccType.NONE, DrgAnalysisResult.MccType.fromCode(invalidCode));

        // 验证有效性检查
        assertTrue(DrgAnalysisResult.MccType.isValid(mccCode));
        assertTrue(DrgAnalysisResult.MccType.isValid(ccCode));
        assertTrue(DrgAnalysisResult.MccType.isValid(noneCode));
        assertFalse(DrgAnalysisResult.MccType.isValid(invalidCode));

        // 验证描述
        assertEquals("严重并发症", DrgAnalysisResult.MccType.MCC.getDescription());
        assertEquals("一般并发症", DrgAnalysisResult.MccType.CC.getDescription());
        assertEquals("无并发症", DrgAnalysisResult.MccType.NONE.getDescription());
    }

    /**
     * 测试：验证模型MCC类型验证功能
     */
    @Test
    @DisplayName("当模型设置MCC类型时，应该正确验证")
    void isValidMccType_shouldValidateCorrectly() {
        // Given - 创建分析结果
        DrgAnalysisResult result = new DrgAnalysisResult();

        // When & Then - 验证各种MCC类型
        result.setUserSelectedMccType("MCC");
        assertTrue(result.isValidMccType());

        result.setUserSelectedMccType("CC");
        assertTrue(result.isValidMccType());

        result.setUserSelectedMccType("NONE");
        assertTrue(result.isValidMccType());

        result.setUserSelectedMccType("INVALID");
        assertFalse(result.isValidMccType());
    }

    /**
     * 测试：验证枚举设置和获取功能
     */
    @Test
    @DisplayName("当使用枚举设置MCC类型时，应该正确设置和获取")
    void setAndGetMccType_shouldWorkCorrectly() {
        // Given - 创建分析结果
        DrgAnalysisResult result = new DrgAnalysisResult();

        // When - 使用枚举设置MCC类型
        result.setMccType(DrgAnalysisResult.MccType.MCC);

        // Then - 验证设置和获取
        assertEquals("MCC", result.getUserSelectedMccType());
        assertEquals(DrgAnalysisResult.MccType.MCC, result.getMccType());
        assertEquals("严重并发症", result.getUserSelectedMccTypeDescription());
    }

    /**
     * 测试：验证用户是否已选择MCC类型
     */
    @Test
    @DisplayName("当用户选择MCC类型时，应该正确识别")
    void hasUserSelectedMccType_shouldDetectSelection() {
        // Given - 创建分析结果
        DrgAnalysisResult result = new DrgAnalysisResult();

        // When & Then - 验证各种情况
        result.setUserSelectedMccType("NONE");
        assertFalse(result.hasUserSelectedMccType());

        result.setUserSelectedMccType("MCC");
        assertTrue(result.hasUserSelectedMccType());

        result.setUserSelectedMccType("CC");
        assertTrue(result.hasUserSelectedMccType());
    }

    /**
     * 测试：验证软删除功能
     */
    @Test
    @DisplayName("当设置删除状态时，应该正确标记")
    void softDelete_shouldMarkCorrectly() {
        // Given - 创建分析结果
        DrgAnalysisResult result = new DrgAnalysisResult();

        // When - 设置删除状态
        result.setDeleted(true);

        // Then - 验证删除状态
        assertTrue(result.isDeleted());
        assertEquals(1, result.getDeleted());

        // When - 取消删除
        result.setDeleted(false);

        // Then - 验证取消删除状态
        assertFalse(result.isDeleted());
        assertEquals(0, result.getDeleted());
    }

    /**
     * 测试：验证构造函数
     */
    @Test
    @DisplayName("当使用带参数构造函数时，应该正确初始化")
    void constructorWithParameters_shouldInitializeCorrectly() {
        // Given - 构造参数
        String patientId = "TEST_PATIENT";
        Long drgId = 123L;
        String finalDrgCode = "TEST_DRG";

        // When - 使用带参数构造函数
        DrgAnalysisResult result = new DrgAnalysisResult(patientId, drgId, finalDrgCode);

        // Then - 验证初始化
        assertEquals(patientId, result.getPatientId());
        assertEquals(drgId, result.getDrgId());
        assertEquals(finalDrgCode, result.getFinalDrgCode());
        assertNotNull(result.getCreatedTime());
    }

    /**
     * 测试：验证默认构造函数
     */
    @Test
    @DisplayName("当使用默认构造函数时，应该正确初始化")
    void defaultConstructor_shouldInitializeCorrectly() {
        // When - 使用默认构造函数
        DrgAnalysisResult result = new DrgAnalysisResult();

        // Then - 验证默认值
        assertNull(result.getResultId());
        assertNull(result.getPatientId());
        assertNull(result.getDrgId());
        assertNull(result.getFinalDrgCode());
        assertEquals("NONE", result.getUserSelectedMccType());
        assertEquals(0, result.getDeleted());
        assertNotNull(result.getCreatedTime());
    }

    /**
     * 测试：验证toString方法
     */
    @Test
    @DisplayName("当调用toString方法时，应该返回有意义的字符串")
    void toString_shouldReturnMeaningfulString() {
        // Given - 创建分析结果
        DrgAnalysisResult result = new DrgAnalysisResult("TEST_PATIENT", 123L, "TEST_DRG");

        // When - 调用toString
        String toString = result.toString();

        // Then - 验证字符串内容
        assertTrue(toString.contains("resultId"));
        assertTrue(toString.contains("patientId"));
        assertTrue(toString.contains("drgId"));
        assertTrue(toString.contains("finalDrgCode"));
        assertTrue(toString.contains("createdTime"));
    }

    /**
     * 参数化测试：验证MCC类型描述
     */
    @ParameterizedTest
    @ValueSource(strings = {"MCC", "CC", "NONE"})
    @DisplayName("当使用不同的MCC类型时，应该返回正确的描述")
    void getUserSelectedMccTypeDescription_shouldReturnCorrectDescription(String mccType) {
        // Given - 创建分析结果
        DrgAnalysisResult result = new DrgAnalysisResult();
        result.setUserSelectedMccType(mccType);

        // When - 获取描述
        String description = result.getUserSelectedMccTypeDescription();

        // Then - 验证描述
        assertNotNull(description);
        assertFalse(description.isEmpty());

        // 验证具体描述
        if ("MCC".equals(mccType)) {
            assertEquals("严重并发症", description);
        } else if ("CC".equals(mccType)) {
            assertEquals("一般并发症", description);
        } else {
            assertEquals("无并发症", description);
        }
    }
}
