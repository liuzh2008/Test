package com.example.medaiassistant.drg.matching;

import com.example.medaiassistant.dto.drg.PatientData;
import com.example.medaiassistant.dto.drg.PatientDiagnosis;
import com.example.medaiassistant.dto.drg.PatientProcedure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PatientData类单元测试
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-21
 */
@DisplayName("PatientData 数据结构测试")
class PatientDataTest {

    @Test
    @DisplayName("应该创建包含诊断和手术的患者数据")
    void shouldCreatePatientDataWithDiagnosesAndProcedures() {
        // Given
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I48.000", "心房颤动")
        );
        List<PatientProcedure> procedures = Arrays.asList(
            new PatientProcedure("37.9000x001", "经皮左心耳封堵术")
        );
        
        // When
        PatientData patientData = new PatientData(diagnoses, procedures);
        
        // Then
        assertThat(patientData.hasProcedures()).isTrue();
        assertThat(patientData.getDiagnoses()).hasSize(1);
        assertThat(patientData.getProcedures()).hasSize(1);
    }

    @Test
    @DisplayName("应该正确处理空诊断列表")
    void shouldHandleEmptyDiagnosesList() {
        // Given
        List<PatientDiagnosis> diagnoses = Collections.emptyList();
        List<PatientProcedure> procedures = Arrays.asList(
            new PatientProcedure("37.9000x001", "经皮左心耳封堵术")
        );
        
        // When
        PatientData patientData = new PatientData(diagnoses, procedures);
        
        // Then
        assertThat(patientData.getDiagnoses()).isEmpty();
        assertThat(patientData.hasProcedures()).isTrue();
    }

    @Test
    @DisplayName("应该正确处理空手术列表")
    void shouldHandleEmptyProceduresList() {
        // Given
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I48.000", "心房颤动")
        );
        List<PatientProcedure> procedures = Collections.emptyList();
        
        // When
        PatientData patientData = new PatientData(diagnoses, procedures);
        
        // Then
        assertThat(patientData.hasProcedures()).isFalse();
        assertThat(patientData.getDiagnoses()).hasSize(1);
    }

    @Test
    @DisplayName("应该正确处理null诊断列表")
    void shouldHandleNullDiagnosesList() {
        // Given
        List<PatientDiagnosis> diagnoses = null;
        List<PatientProcedure> procedures = Arrays.asList(
            new PatientProcedure("37.9000x001", "经皮左心耳封堵术")
        );
        
        // When
        PatientData patientData = new PatientData(diagnoses, procedures);
        
        // Then
        assertThat(patientData.getDiagnoses()).isEmpty();
        assertThat(patientData.hasProcedures()).isTrue();
    }

    @Test
    @DisplayName("应该正确处理null手术列表")
    void shouldHandleNullProceduresList() {
        // Given
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I48.000", "心房颤动")
        );
        List<PatientProcedure> procedures = null;
        
        // When
        PatientData patientData = new PatientData(diagnoses, procedures);
        
        // Then
        assertThat(patientData.hasProcedures()).isFalse();
        assertThat(patientData.getProcedures()).isEmpty();
    }

    @Test
    @DisplayName("应该正确获取诊断和手术数量")
    void shouldReturnCorrectDiagnosisAndProcedureCounts() {
        // Given
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I48.000", "心房颤动"),
            new PatientDiagnosis("I10", "原发性高血压")
        );
        List<PatientProcedure> procedures = Arrays.asList(
            new PatientProcedure("37.9000x001", "经皮左心耳封堵术"),
            new PatientProcedure("88.7201", "冠状动脉造影")
        );
        
        // When
        PatientData patientData = new PatientData(diagnoses, procedures);
        
        // Then
        assertThat(patientData.getDiagnoses()).hasSize(2);
        assertThat(patientData.getProcedures()).hasSize(2);
        assertThat(patientData.hasProcedures()).isTrue();
    }
}
