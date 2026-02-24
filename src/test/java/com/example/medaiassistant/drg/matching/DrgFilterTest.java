package com.example.medaiassistant.drg.matching;

import com.example.medaiassistant.dto.drg.DrgParsedRecord;
import com.example.medaiassistant.dto.drg.PatientData;
import com.example.medaiassistant.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DRG分流规则测试类
 * 
 * 测试根据患者是否有手术来过滤DRG记录的逻辑
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-22
 */
@DisplayName("DRG分流规则测试")
class DrgFilterTest {

    @Test
    @DisplayName("应该为有手术患者过滤出有手术的DRG记录")
    void shouldFilterDrgsWithProceduresForPatientWithProcedures() {
        // Given
        PatientData patientData = TestDataFactory.createPatientWithAtrialFibrillation();
        List<DrgParsedRecord> allDrgs = Arrays.asList(
            TestDataFactory.createDrgWithCardiacProcedures(),  // 有手术
            TestDataFactory.createDrgWithHypertension(),       // 无手术
            TestDataFactory.createDrgWithAngiography()         // 有手术
        );

        // When
        List<DrgParsedRecord> filtered = DrgFilter.filterByProcedurePresence(patientData, allDrgs);

        // Then
        assertThat(filtered).hasSize(2);
        assertThat(filtered).allMatch(DrgParsedRecord::hasProcedures);
        assertThat(filtered).extracting(DrgParsedRecord::getDrgCode)
            .containsExactlyInAnyOrder("DRG001", "DRG005");
    }

    @Test
    @DisplayName("应该为无手术患者过滤出无手术的DRG记录")
    void shouldFilterDrgsWithoutProceduresForPatientWithoutProcedures() {
        // Given
        PatientData patientData = TestDataFactory.createPatientWithHypertension();
        List<DrgParsedRecord> allDrgs = Arrays.asList(
            TestDataFactory.createDrgWithCardiacProcedures(),  // 有手术
            TestDataFactory.createDrgWithHypertension(),       // 无手术
            TestDataFactory.createDrgWithDiabetes()            // 无手术
        );

        // When
        List<DrgParsedRecord> filtered = DrgFilter.filterByProcedurePresence(patientData, allDrgs);

        // Then
        assertThat(filtered).hasSize(2);
        assertThat(filtered).allMatch(drg -> !drg.hasProcedures());
        assertThat(filtered).extracting(DrgParsedRecord::getDrgCode)
            .containsExactlyInAnyOrder("DRG002", "DRG003");
    }

    @Test
    @DisplayName("应该正确处理空患者手术列表")
    void shouldHandleEmptyPatientProcedures() {
        // Given
        PatientData patientData = new PatientData(
            Arrays.asList(TestDataFactory.createPatientWithHypertension().getDiagnoses().get(0)),
            Collections.emptyList()
        );
        List<DrgParsedRecord> allDrgs = Arrays.asList(
            TestDataFactory.createDrgWithCardiacProcedures(),  // 有手术
            TestDataFactory.createDrgWithHypertension()        // 无手术
        );

        // When
        List<DrgParsedRecord> filtered = DrgFilter.filterByProcedurePresence(patientData, allDrgs);

        // Then
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getDrgCode()).isEqualTo("DRG002");
        assertThat(filtered.get(0).hasProcedures()).isFalse();
    }

    @Test
    @DisplayName("应该正确处理空DRG列表")
    void shouldHandleEmptyDrgList() {
        // Given
        PatientData patientData = TestDataFactory.createPatientWithAtrialFibrillation();
        List<DrgParsedRecord> allDrgs = Collections.emptyList();

        // When
        List<DrgParsedRecord> filtered = DrgFilter.filterByProcedurePresence(patientData, allDrgs);

        // Then
        assertThat(filtered).isEmpty();
    }

    @Test
    @DisplayName("应该正确处理空患者数据")
    void shouldHandleEmptyPatientData() {
        // Given
        PatientData patientData = new PatientData(Collections.emptyList(), Collections.emptyList());
        List<DrgParsedRecord> allDrgs = Arrays.asList(
            TestDataFactory.createDrgWithHypertension(),       // 无手术
            TestDataFactory.createDrgWithDiabetes()            // 无手术
        );

        // When
        List<DrgParsedRecord> filtered = DrgFilter.filterByProcedurePresence(patientData, allDrgs);

        // Then
        assertThat(filtered).hasSize(2);
        assertThat(filtered).allMatch(drg -> !drg.hasProcedures());
    }

    @Test
    @DisplayName("应该正确处理混合DRG记录")
    void shouldHandleMixedDrgRecords() {
        // Given
        PatientData patientData = TestDataFactory.createPatientWithComplexDiagnoses();
        List<DrgParsedRecord> allDrgs = Arrays.asList(
            TestDataFactory.createDrgWithCardiacProcedures(),      // 有手术
            TestDataFactory.createDrgWithHypertension(),           // 无手术
            TestDataFactory.createDrgWithDiabetes(),               // 无手术
            TestDataFactory.createDrgWithComplexDiagnosesAndProcedures(), // 有手术
            TestDataFactory.createDrgWithAngiography()             // 有手术
        );

        // When
        List<DrgParsedRecord> filtered = DrgFilter.filterByProcedurePresence(patientData, allDrgs);

        // Then
        assertThat(filtered).hasSize(3);
        assertThat(filtered).allMatch(DrgParsedRecord::hasProcedures);
        assertThat(filtered).extracting(DrgParsedRecord::getDrgCode)
            .containsExactlyInAnyOrder("DRG001", "DRG004", "DRG005");
    }
}
