package com.example.medaiassistant.drg.matching;

import com.example.medaiassistant.dto.drg.DiagnosisEntry;
import com.example.medaiassistant.dto.drg.DrgParsedRecord;
import com.example.medaiassistant.dto.drg.ProcedureEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DrgParsedRecord类单元测试
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-21
 */
@DisplayName("DrgParsedRecord 数据结构测试")
class DrgParsedRecordTest {

    @Test
    @DisplayName("应该创建包含诊断和手术的DRG解析记录")
    void shouldCreateDrgParsedRecordWithDiagnosesAndProcedures() {
        // Given
        List<DiagnosisEntry> diagnoses = Arrays.asList(
            new DiagnosisEntry("I48.000", "心房颤动", Arrays.asList("心房纤颤"))
        );
        List<ProcedureEntry> procedures = Arrays.asList(
            new ProcedureEntry("37.9000x001", "经皮左心耳封堵术")
        );
        
        // When
        DrgParsedRecord drgRecord = new DrgParsedRecord(
            1L, "DRG001", "心脏相关DRG", new BigDecimal("15000.00"), 
            diagnoses, procedures
        );
        
        // Then
        assertThat(drgRecord.getDrgId()).isEqualTo(1L);
        assertThat(drgRecord.getDrgCode()).isEqualTo("DRG001");
        assertThat(drgRecord.getDrgName()).isEqualTo("心脏相关DRG");
        assertThat(drgRecord.getInsurancePayment()).isEqualTo(new BigDecimal("15000.00"));
        assertThat(drgRecord.getDiagnoses()).hasSize(1);
        assertThat(drgRecord.getProcedures()).hasSize(1);
        assertThat(drgRecord.hasProcedures()).isTrue();
        assertThat(drgRecord.hasDiagnoses()).isTrue();
    }

    @Test
    @DisplayName("应该正确处理空诊断列表")
    void shouldHandleEmptyDiagnosesList() {
        // Given
        List<DiagnosisEntry> diagnoses = Collections.emptyList();
        List<ProcedureEntry> procedures = Arrays.asList(
            new ProcedureEntry("37.9000x001", "经皮左心耳封堵术")
        );
        
        // When
        DrgParsedRecord drgRecord = new DrgParsedRecord(
            2L, "DRG002", "手术相关DRG", new BigDecimal("12000.00"), 
            diagnoses, procedures
        );
        
        // Then
        assertThat(drgRecord.getDiagnoses()).isEmpty();
        assertThat(drgRecord.hasDiagnoses()).isFalse();
        assertThat(drgRecord.hasProcedures()).isTrue();
    }

    @Test
    @DisplayName("应该正确处理空手术列表")
    void shouldHandleEmptyProceduresList() {
        // Given
        List<DiagnosisEntry> diagnoses = Arrays.asList(
            new DiagnosisEntry("I10", "原发性高血压", Arrays.asList("高血压"))
        );
        List<ProcedureEntry> procedures = Collections.emptyList();
        
        // When
        DrgParsedRecord drgRecord = new DrgParsedRecord(
            3L, "DRG003", "诊断相关DRG", new BigDecimal("8000.00"), 
            diagnoses, procedures
        );
        
        // Then
        assertThat(drgRecord.getProcedures()).isEmpty();
        assertThat(drgRecord.hasProcedures()).isFalse();
        assertThat(drgRecord.hasDiagnoses()).isTrue();
    }

    @Test
    @DisplayName("应该正确处理null诊断列表")
    void shouldHandleNullDiagnosesList() {
        // Given
        List<DiagnosisEntry> diagnoses = null;
        List<ProcedureEntry> procedures = Arrays.asList(
            new ProcedureEntry("88.7201", "冠状动脉造影")
        );
        
        // When
        DrgParsedRecord drgRecord = new DrgParsedRecord(
            4L, "DRG004", "造影相关DRG", new BigDecimal("10000.00"), 
            diagnoses, procedures
        );
        
        // Then
        assertThat(drgRecord.getDiagnoses()).isEmpty();
        assertThat(drgRecord.hasDiagnoses()).isFalse();
        assertThat(drgRecord.hasProcedures()).isTrue();
    }

    @Test
    @DisplayName("应该正确处理null手术列表")
    void shouldHandleNullProceduresList() {
        // Given
        List<DiagnosisEntry> diagnoses = Arrays.asList(
            new DiagnosisEntry("E11.900", "2型糖尿病", Arrays.asList("糖尿病"))
        );
        List<ProcedureEntry> procedures = null;
        
        // When
        DrgParsedRecord drgRecord = new DrgParsedRecord(
            5L, "DRG005", "糖尿病相关DRG", new BigDecimal("6000.00"), 
            diagnoses, procedures
        );
        
        // Then
        assertThat(drgRecord.getProcedures()).isEmpty();
        assertThat(drgRecord.hasProcedures()).isFalse();
        assertThat(drgRecord.hasDiagnoses()).isTrue();
    }

    @Test
    @DisplayName("应该正确返回诊断和手术数量")
    void shouldReturnCorrectDiagnosisAndProcedureCounts() {
        // Given
        List<DiagnosisEntry> diagnoses = Arrays.asList(
            new DiagnosisEntry("I48.000", "心房颤动", Arrays.asList("心房纤颤")),
            new DiagnosisEntry("I10", "原发性高血压", Arrays.asList("高血压"))
        );
        List<ProcedureEntry> procedures = Arrays.asList(
            new ProcedureEntry("37.9000x001", "经皮左心耳封堵术"),
            new ProcedureEntry("88.7201", "冠状动脉造影")
        );
        
        // When
        DrgParsedRecord drgRecord = new DrgParsedRecord(
            6L, "DRG006", "复杂心脏DRG", new BigDecimal("18000.00"), 
            diagnoses, procedures
        );
        
        // Then
        assertThat(drgRecord.getDiagnoses()).hasSize(2);
        assertThat(drgRecord.getProcedures()).hasSize(2);
        assertThat(drgRecord.getDiagnosisCount()).isEqualTo(2);
        assertThat(drgRecord.getProcedureCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("应该正确获取保险支付金额")
    void shouldReturnCorrectInsurancePayment() {
        // Given
        List<DiagnosisEntry> diagnoses = Arrays.asList(
            new DiagnosisEntry("I48.000", "心房颤动", Arrays.asList("心房纤颤"))
        );
        List<ProcedureEntry> procedures = Arrays.asList(
            new ProcedureEntry("37.9000x001", "经皮左心耳封堵术")
        );
        BigDecimal insurancePayment = new BigDecimal("15000.00");
        
        // When
        DrgParsedRecord drgRecord = new DrgParsedRecord(
            7L, "DRG007", "标准心脏DRG", insurancePayment, 
            diagnoses, procedures
        );
        
        // Then
        assertThat(drgRecord.getInsurancePayment()).isEqualTo(insurancePayment);
        assertThat(drgRecord.getInsurancePaymentSafe()).isEqualTo(insurancePayment);
    }

    @Test
    @DisplayName("应该正确处理null保险支付金额")
    void shouldHandleNullInsurancePayment() {
        // Given
        List<DiagnosisEntry> diagnoses = Arrays.asList(
            new DiagnosisEntry("I48.000", "心房颤动", Arrays.asList("心房纤颤"))
        );
        List<ProcedureEntry> procedures = Arrays.asList(
            new ProcedureEntry("37.9000x001", "经皮左心耳封堵术")
        );
        
        // When
        DrgParsedRecord drgRecord = new DrgParsedRecord(
            8L, "DRG008", "无支付DRG", null, 
            diagnoses, procedures
        );
        
        // Then
        assertThat(drgRecord.getInsurancePayment()).isNull();
        assertThat(drgRecord.getInsurancePaymentSafe()).isEqualTo(BigDecimal.ZERO);
    }
}
