package com.example.medaiassistant.drg.matching;

import com.example.medaiassistant.dto.drg.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PrimaryDiagnosisProcedureMatcher集成测试
 * 
 * TDD循环7：完整流程集成测试
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-22
 */
@DisplayName("PrimaryDiagnosisProcedureMatcher 集成测试")
class PrimaryDiagnosisProcedureMatcherIntegrationTest {

    private PrimaryDiagnosisProcedureMatcher matcher;
    private DrgCatalog catalog;

    @BeforeEach
    void setUp() {
        matcher = new PrimaryDiagnosisProcedureMatcher();
        catalog = createTestCatalog();
    }

    @Test
    @DisplayName("应该为有手术的患者返回主要诊断和手术")
    void shouldReturnPrimaryDiagnosisAndProcedureForPatientWithSurgery() {
        // Given
        PatientData patientData = createComplexPatientDataWithSurgery();
        
        // When
        MatchingResult result = matcher.match(patientData, catalog);
        
        // Then
        assertThat(result.getPrimaryDiagnoses()).isNotEmpty();
        assertThat(result.getPrimaryProcedures()).isNotEmpty();
        assertThat(result.getPrimaryDiagnoses()).allMatch(name -> !name.isEmpty());
        assertThat(result.getPrimaryProcedures()).allMatch(name -> !name.isEmpty());
        
        // 验证具体匹配结果
        assertThat(result.getPrimaryDiagnoses()).contains("心房颤动");
        assertThat(result.getPrimaryProcedures()).contains("经皮左心耳封堵术");
    }

    @Test
    @DisplayName("应该为无手术的患者返回空的手术列表")
    void shouldReturnEmptyProcedureListForPatientWithoutSurgery() {
        // Given
        PatientData patientData = createPatientDataWithoutProcedures();
        
        // When
        MatchingResult result = matcher.match(patientData, catalog);
        
        // Then
        assertThat(result.getPrimaryDiagnoses()).isNotEmpty();
        assertThat(result.getPrimaryProcedures()).isEmpty();
        assertThat(result.getPrimaryDiagnoses()).contains("原发性高血压");
    }

    @Test
    @DisplayName("应该处理无匹配的情况")
    void shouldHandleNoMatches() {
        // Given
        PatientData patientData = createPatientDataWithNoMatches();
        
        // When
        MatchingResult result = matcher.match(patientData, catalog);
        
        // Then
        assertThat(result.getPrimaryDiagnoses()).isEmpty();
        assertThat(result.getPrimaryProcedures()).isEmpty();
    }

    @Test
    @DisplayName("应该处理空患者数据")
    void shouldHandleEmptyPatientData() {
        // Given
        PatientData patientData = new PatientData(null, null);
        
        // When
        MatchingResult result = matcher.match(patientData, catalog);
        
        // Then
        assertThat(result.getPrimaryDiagnoses()).isEmpty();
        assertThat(result.getPrimaryProcedures()).isEmpty();
    }

    @Test
    @DisplayName("应该处理空DRG目录")
    void shouldHandleEmptyDrgCatalog() {
        // Given
        PatientData patientData = createComplexPatientDataWithSurgery();
        DrgCatalog emptyCatalog = new DrgCatalog(List.of());
        
        // When
        MatchingResult result = matcher.match(patientData, emptyCatalog);
        
        // Then
        assertThat(result.getPrimaryDiagnoses()).isEmpty();
        assertThat(result.getPrimaryProcedures()).isEmpty();
    }

    @Test
    @DisplayName("有手术但手术不命中时应该仅返回诊断列表")
    void shouldReturnOnlyDiagnosisWhenProcedureNotMatched() {
        // Given
        PatientData patientData = createPatientDataWithUnmatchedProcedure();
        
        // When
        MatchingResult result = matcher.match(patientData, catalog);
        
        // Then
        assertThat(result.getPrimaryDiagnoses()).isNotEmpty();
        assertThat(result.getPrimaryProcedures()).isEmpty();
        assertThat(result.getPrimaryDiagnoses()).contains("心房颤动");
    }

    @Test
    @DisplayName("应该通过别名匹配诊断")
    void shouldMatchDiagnosisThroughAlias() {
        // Given
        PatientData patientData = createPatientDataWithAliasDiagnosis();
        
        // When
        MatchingResult result = matcher.match(patientData, catalog);
        
        // Then
        assertThat(result.getPrimaryDiagnoses()).isNotEmpty();
        // 患者使用"高血压"作为诊断名称，应该匹配到"原发性高血压"的别名"高血压"
        assertThat(result.getPrimaryDiagnoses()).contains("原发性高血压");
    }

    @Test
    @DisplayName("应该通过名称包含关系触发相似度匹配")
    void shouldMatchThroughNameContainment() {
        // Given
        PatientData patientData = createPatientDataWithContainedName();
        
        // When
        MatchingResult result = matcher.match(patientData, catalog);
        
        // Then
        assertThat(result.getPrimaryDiagnoses()).isNotEmpty();
        assertThat(result.getPrimaryDiagnoses()).contains("原发性高血压");
    }

    @Test
    @DisplayName("ICD大小写不匹配应该导致ICD精确匹配失败")
    void shouldNotMatchWhenIcdCaseMismatch() {
        // Given
        PatientData patientData = createPatientDataWithLowercaseIcdAndUniqueName();
        
        // When
        MatchingResult result = matcher.match(patientData, catalog);
        
        // Then
        // 由于ICD大小写不匹配，精确匹配失败
        // 但由于名称相似度匹配，可能仍然会匹配到其他诊断
        // 这里主要验证ICD精确匹配确实区分大小写
        assertThat(result.getPrimaryDiagnoses()).doesNotContain("原发性高血压");
    }

    @Test
    @DisplayName("应该支持Top-K限制")
    void shouldSupportTopKLimit() {
        // Given
        NameCollector collector = new NameCollector(true, 2); // 启用Top-K，限制为2
        
        // When
        // 手动收集多个匹配的名称
        DrgParsedRecord drg1 = createDrgWithDiagnoses("诊断1");
        DrgParsedRecord drg2 = createDrgWithDiagnoses("诊断2");
        DrgParsedRecord drg3 = createDrgWithDiagnoses("诊断3");
        
        collector.collectDrgNames(drg1, true, false);
        collector.collectDrgNames(drg2, true, false);
        collector.collectDrgNames(drg3, true, false);
        
        // Then
        assertThat(collector.getPrimaryDiagnoses()).hasSize(2);
    }

    /**
     * 创建测试DRG目录
     */
    private DrgCatalog createTestCatalog() {
        List<DrgParsedRecord> drgRecords = Arrays.asList(
            // 有手术的DRG记录
            new DrgParsedRecord(
                1L, "DRG001", "心脏相关DRG", new BigDecimal("15000.00"),
                Arrays.asList(
                    new DiagnosisEntry("I48.000", "心房颤动", Arrays.asList("心房纤颤"))
                ),
                Arrays.asList(
                    new ProcedureEntry("37.9000x001", "经皮左心耳封堵术")
                )
            ),
            // 无手术的DRG记录
            new DrgParsedRecord(
                2L, "DRG002", "高血压相关DRG", new BigDecimal("8000.00"),
                Arrays.asList(
                    new DiagnosisEntry("I10", "原发性高血压", Arrays.asList("高血压"))
                ),
                List.of()
            ),
            // 另一个有手术的DRG记录
            new DrgParsedRecord(
                3L, "DRG003", "糖尿病相关DRG", new BigDecimal("12000.00"),
                Arrays.asList(
                    new DiagnosisEntry("E11.900", "2型糖尿病", Arrays.asList("糖尿病"))
                ),
                Arrays.asList(
                    new ProcedureEntry("99.2900x001", "胰岛素泵植入术")
                )
            )
        );
        
        return new DrgCatalog(drgRecords);
    }

    /**
     * 创建有手术的复杂患者数据
     */
    private PatientData createComplexPatientDataWithSurgery() {
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I48.000", "心房颤动"),
            new PatientDiagnosis("I10", "原发性高血压")
        );
        List<PatientProcedure> procedures = Arrays.asList(
            new PatientProcedure("37.9000x001", "经皮左心耳封堵术")
        );
        return new PatientData(diagnoses, procedures);
    }

    /**
     * 创建无手术的患者数据
     */
    private PatientData createPatientDataWithoutProcedures() {
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I10", "原发性高血压")
        );
        return new PatientData(diagnoses, null);
    }

    /**
     * 创建无匹配的患者数据
     */
    private PatientData createPatientDataWithNoMatches() {
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("Z00.000", "健康检查")
        );
        return new PatientData(diagnoses, null);
    }

    /**
     * 创建有手术但手术不命中的患者数据
     */
    private PatientData createPatientDataWithUnmatchedProcedure() {
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I48.000", "心房颤动")
        );
        List<PatientProcedure> procedures = Arrays.asList(
            new PatientProcedure("99.9999", "未匹配手术") // 不存在的ICD编码
        );
        return new PatientData(diagnoses, procedures);
    }

    /**
     * 创建使用别名诊断的患者数据
     */
    private PatientData createPatientDataWithAliasDiagnosis() {
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I10", "高血压") // 使用别名，匹配"原发性高血压"的别名
        );
        return new PatientData(diagnoses, null);
    }

    /**
     * 创建名称包含关系的患者数据
     */
    private PatientData createPatientDataWithContainedName() {
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I10", "高血压") // 包含在"原发性高血压"中
        );
        return new PatientData(diagnoses, null);
    }


    /**
     * 创建小写ICD编码且名称唯一的患者数据
     */
    private PatientData createPatientDataWithLowercaseIcdAndUniqueName() {
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("i10", "独特诊断名称") // 小写ICD编码 + 唯一名称，避免名称相似度匹配
        );
        return new PatientData(diagnoses, null);
    }


    /**
     * 创建包含指定诊断的DRG记录
     */
    private DrgParsedRecord createDrgWithDiagnoses(String diagnosisName) {
        return new DrgParsedRecord(
            999L, "TEST_DRG", "测试DRG", new BigDecimal("10000.00"),
            Arrays.asList(
                new DiagnosisEntry("TEST.001", diagnosisName, List.of())
            ),
            List.of()
        );
    }
}
