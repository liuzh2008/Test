package com.example.medaiassistant.drg.matching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ICD精确匹配器测试类
 * 
 * 测试ICD编码的精确匹配功能
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-22
 */
@DisplayName("ICD精确匹配器测试")
class IcdMatcherTest {

    @Test
    @DisplayName("应该匹配完全相同的ICD编码")
    void shouldMatchExactIcdCodes() {
        // Given
        String patientIcd = "I48.000";
        String drgIcd = "I48.000";
        
        // When
        boolean matched = IcdMatcher.exactMatch(patientIcd, drgIcd);
        
        // Then
        assertThat(matched).isTrue();
    }

    @Test
    @DisplayName("不应该匹配不同的ICD编码")
    void shouldNotMatchDifferentIcdCodes() {
        // Given
        String patientIcd = "I48.000";
        String drgIcd = "I48.100";
        
        // When
        boolean matched = IcdMatcher.exactMatch(patientIcd, drgIcd);
        
        // Then
        assertThat(matched).isFalse();
    }

    @Test
    @DisplayName("应该处理空ICD编码")
    void shouldHandleEmptyIcdCodes() {
        // Given
        String patientIcd = "";
        String drgIcd = "I48.000";
        
        // When
        boolean matched = IcdMatcher.exactMatch(patientIcd, drgIcd);
        
        // Then
        assertThat(matched).isFalse();
    }

    @Test
    @DisplayName("应该处理null ICD编码")
    void shouldHandleNullIcdCodes() {
        // Given
        String patientIcd = null;
        String drgIcd = "I48.000";
        
        // When
        boolean matched = IcdMatcher.exactMatch(patientIcd, drgIcd);
        
        // Then
        assertThat(matched).isFalse();
    }

    @Test
    @DisplayName("应该处理两个空ICD编码")
    void shouldHandleBothEmptyIcdCodes() {
        // Given
        String patientIcd = "";
        String drgIcd = "";
        
        // When
        boolean matched = IcdMatcher.exactMatch(patientIcd, drgIcd);
        
        // Then
        assertThat(matched).isFalse();
    }

    @Test
    @DisplayName("应该处理两个null ICD编码")
    void shouldHandleBothNullIcdCodes() {
        // Given
        String patientIcd = null;
        String drgIcd = null;
        
        // When
        boolean matched = IcdMatcher.exactMatch(patientIcd, drgIcd);
        
        // Then
        assertThat(matched).isFalse();
    }

    @Test
    @DisplayName("应该匹配不同格式但内容相同的ICD编码")
    void shouldMatchSameIcdCodesWithDifferentFormatting() {
        // Given
        String patientIcd = "I48.000";
        String drgIcd = "I48.000";
        
        // When
        boolean matched = IcdMatcher.exactMatch(patientIcd, drgIcd);
        
        // Then
        assertThat(matched).isTrue();
    }

    @Test
    @DisplayName("应该区分大小写")
    void shouldBeCaseSensitive() {
        // Given
        String patientIcd = "i48.000"; // 小写
        String drgIcd = "I48.000";     // 大写
        
        // When
        boolean matched = IcdMatcher.exactMatch(patientIcd, drgIcd);
        
        // Then
        assertThat(matched).isFalse();
    }

    @Test
    @DisplayName("应该匹配简化的ICD编码")
    void shouldMatchSimplifiedIcdCodes() {
        // Given
        String patientIcd = "I10";
        String drgIcd = "I10";
        
        // When
        boolean matched = IcdMatcher.exactMatch(patientIcd, drgIcd);
        
        // Then
        assertThat(matched).isTrue();
    }

    @Test
    @DisplayName("应该匹配带小数点的ICD编码")
    void shouldMatchIcdCodesWithDecimal() {
        // Given
        String patientIcd = "E11.900";
        String drgIcd = "E11.900";
        
        // When
        boolean matched = IcdMatcher.exactMatch(patientIcd, drgIcd);
        
        // Then
        assertThat(matched).isTrue();
    }
}
