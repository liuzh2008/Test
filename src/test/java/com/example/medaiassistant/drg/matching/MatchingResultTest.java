package com.example.medaiassistant.drg.matching;

import com.example.medaiassistant.dto.drg.MatchingResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MatchingResult类单元测试
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-21
 */
@DisplayName("MatchingResult 数据结构测试")
class MatchingResultTest {

    @Test
    @DisplayName("应该创建包含主要诊断和手术的匹配结果")
    void shouldCreateMatchingResultWithDiagnosesAndProcedures() {
        // Given
        List<String> primaryDiagnoses = Arrays.asList("心房颤动", "原发性高血压");
        List<String> primaryProcedures = Arrays.asList("经皮左心耳封堵术");
        
        // When
        MatchingResult result = new MatchingResult(primaryDiagnoses, primaryProcedures);
        
        // Then
        assertThat(result.getPrimaryDiagnoses()).hasSize(2);
        assertThat(result.getPrimaryProcedures()).hasSize(1);
        assertThat(result.getPrimaryDiagnoses()).containsExactlyInAnyOrder("心房颤动", "原发性高血压");
        assertThat(result.getPrimaryProcedures()).containsExactly("经皮左心耳封堵术");
    }

    @Test
    @DisplayName("应该正确处理空诊断列表")
    void shouldHandleEmptyDiagnosesList() {
        // Given
        List<String> primaryDiagnoses = Collections.emptyList();
        List<String> primaryProcedures = Arrays.asList("冠状动脉造影");
        
        // When
        MatchingResult result = new MatchingResult(primaryDiagnoses, primaryProcedures);
        
        // Then
        assertThat(result.getPrimaryDiagnoses()).isEmpty();
        assertThat(result.getPrimaryProcedures()).hasSize(1);
    }

    @Test
    @DisplayName("应该正确处理空手术列表")
    void shouldHandleEmptyProceduresList() {
        // Given
        List<String> primaryDiagnoses = Arrays.asList("2型糖尿病");
        List<String> primaryProcedures = Collections.emptyList();
        
        // When
        MatchingResult result = new MatchingResult(primaryDiagnoses, primaryProcedures);
        
        // Then
        assertThat(result.getPrimaryDiagnoses()).hasSize(1);
        assertThat(result.getPrimaryProcedures()).isEmpty();
    }

    @Test
    @DisplayName("应该正确处理null诊断列表")
    void shouldHandleNullDiagnosesList() {
        // Given
        List<String> primaryDiagnoses = null;
        List<String> primaryProcedures = Arrays.asList("经皮左心耳封堵术");
        
        // When
        MatchingResult result = new MatchingResult(primaryDiagnoses, primaryProcedures);
        
        // Then
        assertThat(result.getPrimaryDiagnoses()).isEmpty();
        assertThat(result.getPrimaryProcedures()).hasSize(1);
    }

    @Test
    @DisplayName("应该正确处理null手术列表")
    void shouldHandleNullProceduresList() {
        // Given
        List<String> primaryDiagnoses = Arrays.asList("心房颤动");
        List<String> primaryProcedures = null;
        
        // When
        MatchingResult result = new MatchingResult(primaryDiagnoses, primaryProcedures);
        
        // Then
        assertThat(result.getPrimaryDiagnoses()).hasSize(1);
        assertThat(result.getPrimaryProcedures()).isEmpty();
    }

    @Test
    @DisplayName("应该正确返回诊断和手术数量")
    void shouldReturnCorrectDiagnosisAndProcedureCounts() {
        // Given
        List<String> primaryDiagnoses = Arrays.asList("诊断1", "诊断2", "诊断3");
        List<String> primaryProcedures = Arrays.asList("手术1", "手术2");
        
        // When
        MatchingResult result = new MatchingResult(primaryDiagnoses, primaryProcedures);
        
        // Then
        assertThat(result.getPrimaryDiagnoses()).hasSize(3);
        assertThat(result.getPrimaryProcedures()).hasSize(2);
    }

    @Test
    @DisplayName("应该正确判断是否有主要诊断")
    void shouldCorrectlyDetermineIfHasPrimaryDiagnoses() {
        // Given
        List<String> primaryDiagnoses = Arrays.asList("心房颤动");
        List<String> primaryProcedures = Collections.emptyList();
        
        // When
        MatchingResult result = new MatchingResult(primaryDiagnoses, primaryProcedures);
        
        // Then
        assertThat(result.hasPrimaryDiagnoses()).isTrue();
        assertThat(result.hasPrimaryProcedures()).isFalse();
    }

    @Test
    @DisplayName("应该正确判断是否有主要手术")
    void shouldCorrectlyDetermineIfHasPrimaryProcedures() {
        // Given
        List<String> primaryDiagnoses = Collections.emptyList();
        List<String> primaryProcedures = Arrays.asList("经皮左心耳封堵术");
        
        // When
        MatchingResult result = new MatchingResult(primaryDiagnoses, primaryProcedures);
        
        // Then
        assertThat(result.hasPrimaryDiagnoses()).isFalse();
        assertThat(result.hasPrimaryProcedures()).isTrue();
    }
}
