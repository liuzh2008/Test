package com.example.medaiassistant.drg.matching;

import com.example.medaiassistant.dto.drg.DrgParsedRecord;
import com.example.medaiassistant.dto.drg.MatchingResult;
import com.example.medaiassistant.dto.drg.PatientData;
import com.example.medaiassistant.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TestDataFactory类单元测试
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-21
 */
@DisplayName("TestDataFactory 测试数据工厂测试")
class TestDataFactoryTest {

    @Test
    @DisplayName("应该创建有心房颤动和左心耳封堵术的患者数据")
    void shouldCreatePatientWithAtrialFibrillation() {
        // When
        PatientData patientData = TestDataFactory.createPatientWithAtrialFibrillation();
        
        // Then
        assertThat(patientData.hasProcedures()).isTrue();
        assertThat(patientData.getDiagnoses()).hasSize(1);
        assertThat(patientData.getProcedures()).hasSize(1);
        assertThat(patientData.getDiagnoses().get(0).getIcdCode()).isEqualTo("I48.000");
        assertThat(patientData.getDiagnoses().get(0).getDiagnosisName()).isEqualTo("心房颤动");
        assertThat(patientData.getProcedures().get(0).getProcedureCode()).isEqualTo("37.9000x001");
        assertThat(patientData.getProcedures().get(0).getProcedureName()).isEqualTo("经皮左心耳封堵术");
    }

    @Test
    @DisplayName("应该创建有原发性高血压的患者数据")
    void shouldCreatePatientWithHypertension() {
        // When
        PatientData patientData = TestDataFactory.createPatientWithHypertension();
        
        // Then
        assertThat(patientData.hasProcedures()).isFalse();
        assertThat(patientData.getDiagnoses()).hasSize(1);
        assertThat(patientData.getDiagnoses().get(0).getIcdCode()).isEqualTo("I10");
        assertThat(patientData.getDiagnoses().get(0).getDiagnosisName()).isEqualTo("原发性高血压");
    }

    @Test
    @DisplayName("应该创建有复杂诊断的患者数据")
    void shouldCreatePatientWithComplexDiagnoses() {
        // When
        PatientData patientData = TestDataFactory.createPatientWithComplexDiagnoses();
        
        // Then
        assertThat(patientData.hasProcedures()).isTrue();
        assertThat(patientData.getDiagnoses()).hasSize(3);
        assertThat(patientData.getProcedures()).hasSize(2);
    }

    @Test
    @DisplayName("应该创建有心脏手术的DRG记录")
    void shouldCreateDrgWithCardiacProcedures() {
        // When
        DrgParsedRecord drgRecord = TestDataFactory.createDrgWithCardiacProcedures();
        
        // Then
        assertThat(drgRecord.hasProcedures()).isTrue();
        assertThat(drgRecord.getDiagnoses()).hasSize(1);
        assertThat(drgRecord.getProcedures()).hasSize(1);
        assertThat(drgRecord.getDrgCode()).isEqualTo("DRG001");
        assertThat(drgRecord.getDrgName()).isEqualTo("心脏相关DRG");
        assertThat(drgRecord.getInsurancePayment()).isNotNull();
    }

    @Test
    @DisplayName("应该创建有高血压诊断的DRG记录")
    void shouldCreateDrgWithHypertension() {
        // When
        DrgParsedRecord drgRecord = TestDataFactory.createDrgWithHypertension();
        
        // Then
        assertThat(drgRecord.hasProcedures()).isFalse();
        assertThat(drgRecord.getDiagnoses()).hasSize(1);
        assertThat(drgRecord.getDrgCode()).isEqualTo("DRG002");
        assertThat(drgRecord.getDrgName()).isEqualTo("高血压相关DRG");
    }

    @Test
    @DisplayName("应该创建有复杂诊断和手术的DRG记录")
    void shouldCreateDrgWithComplexDiagnosesAndProcedures() {
        // When
        DrgParsedRecord drgRecord = TestDataFactory.createDrgWithComplexDiagnosesAndProcedures();
        
        // Then
        assertThat(drgRecord.hasProcedures()).isTrue();
        assertThat(drgRecord.getDiagnoses()).hasSize(3);
        assertThat(drgRecord.getProcedures()).hasSize(2);
        assertThat(drgRecord.getDrgCode()).isEqualTo("DRG004");
        assertThat(drgRecord.getDrgName()).isEqualTo("复杂心脏DRG");
    }

    @Test
    @DisplayName("应该创建空的匹配结果")
    void shouldCreateEmptyMatchingResult() {
        // When
        MatchingResult result = TestDataFactory.createEmptyMatchingResult();
        
        // Then
        assertThat(result.hasPrimaryDiagnoses()).isFalse();
        assertThat(result.hasPrimaryProcedures()).isFalse();
        assertThat(result.getPrimaryDiagnoses()).isEmpty();
        assertThat(result.getPrimaryProcedures()).isEmpty();
    }

    @Test
    @DisplayName("应该创建有诊断的匹配结果")
    void shouldCreateMatchingResultWithDiagnoses() {
        // When
        MatchingResult result = TestDataFactory.createMatchingResultWithDiagnoses();
        
        // Then
        assertThat(result.hasPrimaryDiagnoses()).isTrue();
        assertThat(result.hasPrimaryProcedures()).isFalse();
        assertThat(result.getPrimaryDiagnoses()).hasSize(2);
        assertThat(result.getPrimaryDiagnoses()).containsExactlyInAnyOrder("心房颤动", "原发性高血压");
    }

    @Test
    @DisplayName("应该创建有手术的匹配结果")
    void shouldCreateMatchingResultWithProcedures() {
        // When
        MatchingResult result = TestDataFactory.createMatchingResultWithProcedures();
        
        // Then
        assertThat(result.hasPrimaryDiagnoses()).isFalse();
        assertThat(result.hasPrimaryProcedures()).isTrue();
        assertThat(result.getPrimaryProcedures()).hasSize(1);
        assertThat(result.getPrimaryProcedures()).containsExactly("经皮左心耳封堵术");
    }

    @Test
    @DisplayName("应该创建有诊断和手术的匹配结果")
    void shouldCreateMatchingResultWithDiagnosesAndProcedures() {
        // When
        MatchingResult result = TestDataFactory.createMatchingResultWithDiagnosesAndProcedures();
        
        // Then
        assertThat(result.hasPrimaryDiagnoses()).isTrue();
        assertThat(result.hasPrimaryProcedures()).isTrue();
        assertThat(result.getPrimaryDiagnoses()).hasSize(3);
        assertThat(result.getPrimaryProcedures()).hasSize(2);
        assertThat(result.getPrimaryDiagnoses()).containsExactlyInAnyOrder("心房颤动", "原发性高血压", "2型糖尿病");
        assertThat(result.getPrimaryProcedures()).containsExactlyInAnyOrder("经皮左心耳封堵术", "冠状动脉造影");
    }
}
