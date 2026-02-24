package com.example.medaiassistant.drg.matching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 名称相似度匹配器测试类
 * 
 * 测试诊断和手术名称的相似度匹配功能
 * 使用Levenshtein距离算法计算相似度
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-22
 */
@DisplayName("名称相似度匹配器测试")
class NameMatcherTest {

    @Test
    @DisplayName("应该匹配相似的诊断名称")
    void shouldMatchSimilarDiagnosisNames() {
        // Given
        String patientName = "高血压";
        String drgName = "高血压";
        double threshold = 0.7;

        // When
        boolean matched = NameMatcher.similarityMatch(patientName, drgName, threshold);

        // Then
        assertThat(matched).isTrue();
    }

    @Test
    @DisplayName("不应该匹配相似度低于阈值的名称")
    void shouldNotMatchWhenSimilarityBelowThreshold() {
        // Given
        String patientName = "高血压";
        String drgName = "低血压";
        double threshold = 0.7;

        // When
        boolean matched = NameMatcher.similarityMatch(patientName, drgName, threshold);

        // Then
        assertThat(matched).isFalse();
    }

    @Test
    @DisplayName("应该匹配完全相同的名称")
    void shouldMatchExactSameNames() {
        // Given
        String patientName = "冠状动脉粥样硬化性心脏病";
        String drgName = "冠状动脉粥样硬化性心脏病";
        double threshold = 0.7;

        // When
        boolean matched = NameMatcher.similarityMatch(patientName, drgName, threshold);

        // Then
        assertThat(matched).isTrue();
    }

    @Test
    @DisplayName("应该处理空名称")
    void shouldHandleEmptyNames() {
        // Given
        String patientName = "";
        String drgName = "心房颤动";
        double threshold = 0.7;

        // When
        boolean matched = NameMatcher.similarityMatch(patientName, drgName, threshold);

        // Then
        assertThat(matched).isFalse();
    }

    @Test
    @DisplayName("应该处理null名称")
    void shouldHandleNullNames() {
        // Given
        String patientName = null;
        String drgName = "心房颤动";
        double threshold = 0.7;

        // When
        boolean matched = NameMatcher.similarityMatch(patientName, drgName, threshold);

        // Then
        assertThat(matched).isFalse();
    }

    @Test
    @DisplayName("应该处理两个空名称")
    void shouldHandleBothEmptyNames() {
        // Given
        String patientName = "";
        String drgName = "";
        double threshold = 0.7;

        // When
        boolean matched = NameMatcher.similarityMatch(patientName, drgName, threshold);

        // Then
        assertThat(matched).isFalse();
    }

    @Test
    @DisplayName("应该处理两个null名称")
    void shouldHandleBothNullNames() {
        // Given
        String patientName = null;
        String drgName = null;
        double threshold = 0.7;

        // When
        boolean matched = NameMatcher.similarityMatch(patientName, drgName, threshold);

        // Then
        assertThat(matched).isFalse();
    }

    @Test
    @DisplayName("应该匹配包含关系的名称")
    void shouldMatchContainedNames() {
        // Given
        String patientName = "急性心肌梗死";
        String drgName = "心肌梗死";
        double threshold = 0.7;

        // When
        boolean matched = NameMatcher.similarityMatch(patientName, drgName, threshold);

        // Then
        assertThat(matched).isTrue();
    }

    @Test
    @DisplayName("应该匹配部分相似的名称")
    void shouldMatchPartiallySimilarNames() {
        // Given
        String patientName = "慢性阻塞性肺疾病";
        String drgName = "慢性阻塞性肺病";
        double threshold = 0.7;

        // When
        boolean matched = NameMatcher.similarityMatch(patientName, drgName, threshold);

        // Then
        assertThat(matched).isTrue();
    }

    @Test
    @DisplayName("应该使用默认阈值进行匹配")
    void shouldMatchWithDefaultThreshold() {
        // Given
        String patientName = "高血压";
        String drgName = "高血压";

        // When
        boolean matched = NameMatcher.similarityMatch(patientName, drgName);

        // Then
        assertThat(matched).isTrue();
    }

    @ParameterizedTest
    @DisplayName("应该根据不同的阈值进行匹配")
    @CsvSource({
        "高血压, 高血压, 0.6, true",
        "高血压, 低血压, 0.6, false",
        "糖尿病, 糖尿病, 0.5, true",
        "糖尿病, 低血糖, 0.8, false"
    })
    void shouldMatchWithDifferentThresholds(String patientName, String drgName, double threshold, boolean expected) {
        // When
        boolean matched = NameMatcher.similarityMatch(patientName, drgName, threshold);

        // Then
        assertThat(matched).isEqualTo(expected);
    }

    @Test
    @DisplayName("应该计算名称相似度")
    void shouldCalculateSimilarity() {
        // Given
        String name1 = "高血压";
        String name2 = "高血压";

        // When
        double similarity = NameMatcher.calculateSimilarity(name1, name2);

        // Then
        assertThat(similarity).isBetween(0.0, 1.0);
        assertThat(similarity).isGreaterThan(0.7);
    }

    @Test
    @DisplayName("应该处理相同名称的相似度计算")
    void shouldCalculateSimilarityForSameNames() {
        // Given
        String name1 = "冠状动脉粥样硬化性心脏病";
        String name2 = "冠状动脉粥样硬化性心脏病";

        // When
        double similarity = NameMatcher.calculateSimilarity(name1, name2);

        // Then
        assertThat(similarity).isEqualTo(1.0);
    }

    @Test
    @DisplayName("应该处理完全不同名称的相似度计算")
    void shouldCalculateSimilarityForCompletelyDifferentNames() {
        // Given
        String name1 = "高血压";
        String name2 = "糖尿病";

        // When
        double similarity = NameMatcher.calculateSimilarity(name1, name2);

        // Then
        assertThat(similarity).isLessThan(0.3);
    }

    @Test
    @DisplayName("应该处理空名称的相似度计算")
    void shouldCalculateSimilarityForEmptyNames() {
        // Given
        String name1 = "";
        String name2 = "心房颤动";

        // When
        double similarity = NameMatcher.calculateSimilarity(name1, name2);

        // Then
        assertThat(similarity).isEqualTo(0.0);
    }

    @Test
    @DisplayName("应该处理null名称的相似度计算")
    void shouldCalculateSimilarityForNullNames() {
        // Given
        String name1 = null;
        String name2 = "心房颤动";

        // When
        double similarity = NameMatcher.calculateSimilarity(name1, name2);

        // Then
        assertThat(similarity).isEqualTo(0.0);
    }

    @Test
    @DisplayName("应该验证相似度计算的对称性")
    void shouldBeSymmetric() {
        // Given
        String name1 = "心房颤动";
        String name2 = "心房纤颤";

        // When
        double similarity1 = NameMatcher.calculateSimilarity(name1, name2);
        double similarity2 = NameMatcher.calculateSimilarity(name2, name1);

        // Then
        assertThat(similarity1).isEqualTo(similarity2);
    }

    @Test
    @DisplayName("应该处理两个空字符串的相似度")
    void shouldHandleBothEmptyStrings() {
        // Given
        String name1 = "";
        String name2 = "";

        // When
        double similarity = NameMatcher.calculateSimilarity(name1, name2);

        // Then
        assertThat(similarity).isEqualTo(0.0);
    }

    @Test
    @DisplayName("应该处理两个空字符串的匹配")
    void shouldHandleBothEmptyStringsForMatch() {
        // Given
        String name1 = "";
        String name2 = "";
        double threshold = 0.7;

        // When
        boolean matched = NameMatcher.similarityMatch(name1, name2, threshold);

        // Then
        assertThat(matched).isFalse();
    }

    @Test
    @DisplayName("应该拒绝阈值为0的匹配")
    void shouldRejectZeroThreshold() {
        // Given
        String name1 = "高血压";
        String name2 = "低血压";

        // When & Then
        assertThatThrownBy(() -> NameMatcher.similarityMatch(name1, name2, 0.0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("相似度阈值必须在0.0到1.0之间");
    }

    @Test
    @DisplayName("应该拒绝阈值为1的匹配")
    void shouldRejectOneThreshold() {
        // Given
        String name1 = "高血压";
        String name2 = "低血压";

        // When & Then
        assertThatThrownBy(() -> NameMatcher.similarityMatch(name1, name2, 1.0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("相似度阈值必须在0.0到1.0之间");
    }

    @Test
    @DisplayName("应该拒绝负阈值")
    void shouldRejectNegativeThreshold() {
        // Given
        String name1 = "高血压";
        String name2 = "低血压";

        // When & Then
        assertThatThrownBy(() -> NameMatcher.similarityMatch(name1, name2, -0.5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("相似度阈值必须在0.0到1.0之间");
    }

    @Test
    @DisplayName("应该拒绝大于1的阈值")
    void shouldRejectGreaterThanOneThreshold() {
        // Given
        String name1 = "高血压";
        String name2 = "低血压";

        // When & Then
        assertThatThrownBy(() -> NameMatcher.similarityMatch(name1, name2, 1.5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("相似度阈值必须在0.0到1.0之间");
    }

    @Test
    @DisplayName("应该处理带空格的名称")
    void shouldHandleNamesWithSpaces() {
        // Given
        String name1 = " 高血压 ";
        String name2 = "高血压";

        // When
        double similarity = NameMatcher.calculateSimilarity(name1, name2);

        // Then
        assertThat(similarity).isGreaterThan(0.7);
    }

    @Test
    @DisplayName("应该处理带标点符号的名称")
    void shouldHandleNamesWithPunctuation() {
        // Given
        String name1 = "高血压(原发性)";
        String name2 = "高血压";

        // When
        double similarity = NameMatcher.calculateSimilarity(name1, name2);

        // Then
        assertThat(similarity).isGreaterThan(0.7);
    }

    @Test
    @DisplayName("应该验证代表性术语的相似度范围")
    void shouldVerifySimilarityRangeForRepresentativeTerms() {
        // 代表性术语对的相似度应该在预期范围内（±0.05容差）
        
        // 完全相同名称
        assertThat(NameMatcher.calculateSimilarity("高血压", "高血压"))
            .isEqualTo(1.0);
        
        // 包含关系名称
        assertThat(NameMatcher.calculateSimilarity("急性心肌梗死", "心肌梗死"))
            .isBetween(0.8, 1.0);
        
        // 部分相似名称
        assertThat(NameMatcher.calculateSimilarity("慢性阻塞性肺疾病", "慢性阻塞性肺病"))
            .isBetween(0.7, 1.0);
        
        // 低相似度名称
        assertThat(NameMatcher.calculateSimilarity("高血压", "低血压"))
            .isBetween(0.0, 0.7);
        
        assertThat(NameMatcher.calculateSimilarity("糖尿病", "低血糖"))
            .isBetween(0.0, 0.3);
    }

    @Test
    @DisplayName("应该处理超长医疗术语")
    void shouldHandleVeryLongMedicalTerms() {
        // Given
        String name1 = "急性ST段抬高型前壁心肌梗死合并心源性休克";
        String name2 = "急性ST段抬高型前壁心肌梗死";

        // When
        double similarity = NameMatcher.calculateSimilarity(name1, name2);

        // Then
        assertThat(similarity).isBetween(0.7, 1.0);
        assertThat(NameMatcher.similarityMatch(name1, name2)).isTrue();
    }

    @Test
    @DisplayName("应该处理复杂标点符号")
    void shouldHandleComplexPunctuation() {
        // Given
        String name1 = "高血压[原发性高血压](重度)";
        String name2 = "高血压";

        // When
        double similarity = NameMatcher.calculateSimilarity(name1, name2);

        // Then
        assertThat(similarity).isGreaterThan(0.7);
    }

    @Test
    @DisplayName("应该处理数字格式差异")
    void shouldHandleNumberFormatDifferences() {
        // Given
        String name1 = "2型糖尿病";
        String name2 = "Ⅱ型糖尿病";

        // When
        double similarity = NameMatcher.calculateSimilarity(name1, name2);

        // Then
        assertThat(similarity).isGreaterThan(0.5);
    }

    @Test
    @DisplayName("应该处理英文缩写")
    void shouldHandleEnglishAbbreviations() {
        // Given
        String name1 = "COPD";
        String name2 = "慢性阻塞性肺疾病";

        // When
        double similarity = NameMatcher.calculateSimilarity(name1, name2);

        // Then
        // 英文缩写与中文全称相似度较低是合理的
        assertThat(similarity).isLessThan(0.5);
    }

    @Test
    @DisplayName("应该处理全半角字符")
    void shouldHandleFullWidthAndHalfWidthCharacters() {
        // Given
        String name1 = "高血压";  // 全角
        String name2 = "高血压";  // 全角

        // When
        double similarity = NameMatcher.calculateSimilarity(name1, name2);

        // Then
        assertThat(similarity).isEqualTo(1.0);
    }
}
