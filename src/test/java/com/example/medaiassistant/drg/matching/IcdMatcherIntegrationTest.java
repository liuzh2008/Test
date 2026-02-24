package com.example.medaiassistant.drg.matching;

import com.example.medaiassistant.dto.drg.DiagnosisEntry;
import com.example.medaiassistant.dto.drg.DrgParsedRecord;
import com.example.medaiassistant.dto.drg.PatientData;
import com.example.medaiassistant.dto.drg.PatientDiagnosis;
import com.example.medaiassistant.dto.drg.PatientProcedure;
import com.example.medaiassistant.dto.drg.ProcedureEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ICD精确匹配器集成测试
 * 
 * 测试ICD精确匹配在实际DRG匹配场景中的使用
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-22
 */
@DisplayName("ICD精确匹配器集成测试")
class IcdMatcherIntegrationTest {

    @Test
    @DisplayName("应该在实际DRG匹配场景中正确匹配ICD编码")
    void shouldMatchIcdCodesInRealDrgScenario() {
        // Given - 创建患者数据和DRG记录
        PatientData patientData = createPatientDataWithAtrialFibrillation();
        List<DrgParsedRecord> drgRecords = createDrgRecords();
        
        // When - 过滤DRG记录并检查ICD匹配
        List<DrgParsedRecord> filteredDrgs = DrgFilter.filterByProcedurePresence(patientData, drgRecords);
        
        // Then - 验证匹配结果
        assertThat(filteredDrgs).hasSize(2); // 应该匹配到2个有手术的DRG记录
        
        // 验证第一个DRG记录的ICD匹配
        DrgParsedRecord matchedDrg1 = filteredDrgs.get(0);
        boolean icdMatched1 = IcdMatcher.exactMatch(
            patientData.getDiagnoses().get(0).getIcdCode(),
            matchedDrg1.getDiagnoses().get(0).getIcdCode()
        );
        assertThat(icdMatched1).isTrue();
        
        // 验证第二个DRG记录的ICD匹配
        DrgParsedRecord matchedDrg2 = filteredDrgs.get(1);
        boolean icdMatched2 = IcdMatcher.exactMatch(
            patientData.getDiagnoses().get(0).getIcdCode(),
            matchedDrg2.getDiagnoses().get(0).getIcdCode()
        );
        assertThat(icdMatched2).isTrue();
    }

    @Test
    @DisplayName("应该在实际DRG匹配场景中正确处理不匹配的ICD编码")
    void shouldHandleUnmatchedIcdCodesInRealDrgScenario() {
        // Given - 创建患者数据和DRG记录
        PatientData patientData = createPatientDataWithHypertension();
        List<DrgParsedRecord> drgRecords = createDrgRecords();
        
        // When - 过滤DRG记录并检查ICD匹配
        List<DrgParsedRecord> filteredDrgs = DrgFilter.filterByProcedurePresence(patientData, drgRecords);
        
        // Then - 验证匹配结果
        assertThat(filteredDrgs).hasSize(2); // 应该匹配到2个无手术的DRG记录（高血压和糖尿病）
        
        // 验证所有匹配的DRG记录都没有手术
        assertThat(filteredDrgs).allMatch(drg -> !drg.hasProcedures());
        
        // 验证ICD编码不匹配（患者有高血压，但匹配到的DRG中有高血压和糖尿病）
        // 检查高血压DRG的ICD匹配
        DrgParsedRecord hypertensionDrg = filteredDrgs.stream()
            .filter(drg -> drg.getDrgName().equals("高血压DRG"))
            .findFirst()
            .orElseThrow();
        boolean icdMatchedHypertension = IcdMatcher.exactMatch(
            patientData.getDiagnoses().get(0).getIcdCode(),
            hypertensionDrg.getDiagnoses().get(0).getIcdCode()
        );
        assertThat(icdMatchedHypertension).isTrue(); // 高血压ICD编码相同，应该匹配
        
        // 检查糖尿病DRG的ICD匹配
        DrgParsedRecord diabetesDrg = filteredDrgs.stream()
            .filter(drg -> drg.getDrgName().equals("糖尿病DRG"))
            .findFirst()
            .orElseThrow();
        boolean icdMatchedDiabetes = IcdMatcher.exactMatch(
            patientData.getDiagnoses().get(0).getIcdCode(),
            diabetesDrg.getDiagnoses().get(0).getIcdCode()
        );
        assertThat(icdMatchedDiabetes).isFalse(); // 糖尿病ICD编码不同，应该不匹配
    }

    /**
     * 创建有心房颤动诊断的患者数据
     */
    private PatientData createPatientDataWithAtrialFibrillation() {
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I48.000", "心房颤动")
        );
        List<PatientProcedure> procedures = Arrays.asList(
            new PatientProcedure("37.9000x001", "经皮左心耳封堵术")
        );
        return new PatientData(diagnoses, procedures);
    }

    /**
     * 创建有高血压诊断的患者数据（无手术）
     */
    private PatientData createPatientDataWithHypertension() {
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I10", "原发性高血压")
        );
        return new PatientData(diagnoses, null);
    }

    /**
     * 创建测试DRG记录
     */
    private List<DrgParsedRecord> createDrgRecords() {
        // DRG记录1：有心房颤动诊断和手术
        DrgParsedRecord drg1 = new DrgParsedRecord(
            1L, "DRG001", "心脏相关DRG", new BigDecimal("15000.00"),
            Arrays.asList(new DiagnosisEntry("I48.000", "心房颤动", Arrays.asList("心房纤颤"))),
            Arrays.asList(new ProcedureEntry("37.9000x001", "经皮左心耳封堵术"))
        );

        // DRG记录2：有心房颤动诊断和手术（不同DRG）
        DrgParsedRecord drg2 = new DrgParsedRecord(
            2L, "DRG002", "心脏手术DRG", new BigDecimal("20000.00"),
            Arrays.asList(new DiagnosisEntry("I48.000", "心房颤动", Arrays.asList("房颤"))),
            Arrays.asList(new ProcedureEntry("37.9000x001", "经皮左心耳封堵术"))
        );

        // DRG记录3：有高血压诊断（无手术）
        DrgParsedRecord drg3 = new DrgParsedRecord(
            3L, "DRG003", "高血压DRG", new BigDecimal("8000.00"),
            Arrays.asList(new DiagnosisEntry("I10", "原发性高血压", Arrays.asList("高血压"))),
            Arrays.asList()
        );

        // DRG记录4：有糖尿病诊断（无手术）
        DrgParsedRecord drg4 = new DrgParsedRecord(
            4L, "DRG004", "糖尿病DRG", new BigDecimal("9000.00"),
            Arrays.asList(new DiagnosisEntry("E11.900", "2型糖尿病", Arrays.asList("糖尿病"))),
            Arrays.asList()
        );

        return Arrays.asList(drg1, drg2, drg3, drg4);
    }
}
