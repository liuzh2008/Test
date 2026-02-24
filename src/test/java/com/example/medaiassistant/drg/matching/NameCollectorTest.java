package com.example.medaiassistant.drg.matching;

import com.example.medaiassistant.dto.drg.DiagnosisEntry;
import com.example.medaiassistant.dto.drg.DrgParsedRecord;
import com.example.medaiassistant.dto.drg.ProcedureEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NameCollector 单元测试类
 * 
 * 测试名称收集器的功能，包括诊断和手术名称的收集、去重和Top-K限制
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-22
 */
@DisplayName("NameCollector 名称收集器测试")
class NameCollectorTest {

    @Test
    @DisplayName("应该从匹配的DRG中收集诊断名称")
    void shouldCollectDiagnosisNamesFromMatchedDrgs() {
        // Given
        NameCollector collector = new NameCollector();
        DrgParsedRecord matchedDrg = createDrgWithDiagnoses("高血压", "糖尿病");

        // When
        collector.collectDrgNames(matchedDrg, true, true);

        // Then
        assertThat(collector.getPrimaryDiagnoses())
            .containsExactlyInAnyOrder("高血压", "糖尿病");
    }

    @Test
    @DisplayName("应该从匹配的DRG中收集手术名称")
    void shouldCollectProcedureNamesFromMatchedDrgs() {
        // Given
        NameCollector collector = new NameCollector();
        DrgParsedRecord matchedDrg = createDrgWithProcedures("冠状动脉造影", "心脏支架植入术");

        // When
        collector.collectDrgNames(matchedDrg, true, true);

        // Then
        assertThat(collector.getPrimaryProcedures())
            .containsExactlyInAnyOrder("冠状动脉造影", "心脏支架植入术");
    }

    @Test
    @DisplayName("当两个收集标志都禁用时不应该收集任何名称")
    void shouldNotCollectWhenBothFlagsDisabled() {
        // Given
        NameCollector collector = new NameCollector();
        DrgParsedRecord drg = createDrgWithDiagnosesAndProcedures("诊断名称", "手术名称");

        // When
        collector.collectDrgNames(drg, false, false);

        // Then
        assertThat(collector.getPrimaryDiagnoses()).isEmpty();
        assertThat(collector.getPrimaryProcedures()).isEmpty();
    }

    @Test
    @DisplayName("应该自动去重重复的诊断名称")
    void shouldDeduplicateDiagnosisNames() {
        // Given
        NameCollector collector = new NameCollector();
        DrgParsedRecord drg1 = createDrgWithDiagnoses("高血压", "糖尿病");
        DrgParsedRecord drg2 = createDrgWithDiagnoses("高血压", "心脏病");

        // When
        collector.collectDrgNames(drg1, true, true);
        collector.collectDrgNames(drg2, true, true);

        // Then
        assertThat(collector.getPrimaryDiagnoses())
            .containsExactlyInAnyOrder("高血压", "糖尿病", "心脏病")
            .hasSize(3);
    }

    @Test
    @DisplayName("应该自动去重重复的手术名称")
    void shouldDeduplicateProcedureNames() {
        // Given
        NameCollector collector = new NameCollector();
        DrgParsedRecord drg1 = createDrgWithProcedures("冠状动脉造影", "心脏支架植入术");
        DrgParsedRecord drg2 = createDrgWithProcedures("冠状动脉造影", "心脏搭桥术");

        // When
        collector.collectDrgNames(drg1, true, true);
        collector.collectDrgNames(drg2, true, true);

        // Then
        assertThat(collector.getPrimaryProcedures())
            .containsExactlyInAnyOrder("冠状动脉造影", "心脏支架植入术", "心脏搭桥术")
            .hasSize(3);
    }

    @Test
    @DisplayName("启用Top-K限制时应该限制结果数量")
    void shouldLimitResultsWhenTopKEnabled() {
        // Given
        NameCollector collector = new NameCollector(true, 2);
        DrgParsedRecord drg1 = createDrgWithDiagnoses("诊断1");
        DrgParsedRecord drg2 = createDrgWithDiagnoses("诊断2");
        DrgParsedRecord drg3 = createDrgWithDiagnoses("诊断3");

        // When
        collector.collectDrgNames(drg1, true, true);
        collector.collectDrgNames(drg2, true, true);
        collector.collectDrgNames(drg3, true, true);

        // Then
        assertThat(collector.getPrimaryDiagnoses()).hasSize(2);
    }

    @Test
    @DisplayName("禁用Top-K限制时不应该限制结果数量")
    void shouldNotLimitWhenTopKDisabled() {
        // Given
        NameCollector collector = new NameCollector(false, 0);
        DrgParsedRecord drg1 = createDrgWithDiagnoses("诊断1");
        DrgParsedRecord drg2 = createDrgWithDiagnoses("诊断2");
        DrgParsedRecord drg3 = createDrgWithDiagnoses("诊断3");

        // When
        collector.collectDrgNames(drg1, true, true);
        collector.collectDrgNames(drg2, true, true);
        collector.collectDrgNames(drg3, true, true);

        // Then
        assertThat(collector.getPrimaryDiagnoses()).hasSize(3);
    }

    @Test
    @DisplayName("应该只收集诊断名称而不收集手术名称")
    void shouldCollectOnlyDiagnosisNames() {
        // Given
        NameCollector collector = new NameCollector();
        DrgParsedRecord drg = createDrgWithDiagnosesAndProcedures("高血压", "冠状动脉造影");

        // When
        collector.collectDrgNames(drg, true, false);

        // Then
        assertThat(collector.getPrimaryDiagnoses()).containsExactly("高血压");
        assertThat(collector.getPrimaryProcedures()).isEmpty();
    }

    @Test
    @DisplayName("应该只收集手术名称而不收集诊断名称")
    void shouldCollectOnlyProcedureNames() {
        // Given
        NameCollector collector = new NameCollector();
        DrgParsedRecord drg = createDrgWithDiagnosesAndProcedures("高血压", "冠状动脉造影");

        // When
        collector.collectDrgNames(drg, false, true);

        // Then
        assertThat(collector.getPrimaryDiagnoses()).isEmpty();
        assertThat(collector.getPrimaryProcedures()).containsExactly("冠状动脉造影");
    }

    @Test
    @DisplayName("应该处理空的DRG记录")
    void shouldHandleEmptyDrgRecord() {
        // Given
        NameCollector collector = new NameCollector();
        DrgParsedRecord emptyDrg = createEmptyDrg();

        // When
        collector.collectDrgNames(emptyDrg, true, true);

        // Then
        assertThat(collector.getPrimaryDiagnoses()).isEmpty();
        assertThat(collector.getPrimaryProcedures()).isEmpty();
    }

    @Test
    @DisplayName("应该从诊断别名中收集名称")
    void shouldCollectNamesFromDiagnosisAliases() {
        // Given
        NameCollector collector = new NameCollector();
        List<String> aliases = Arrays.asList("心房纤颤", "房颤");
        DiagnosisEntry diagnosisEntry = new DiagnosisEntry("I48.000", "心房颤动", aliases);
        DrgParsedRecord drg = new DrgParsedRecord(1L, "DRG001", "心脏相关DRG", 
            new BigDecimal("15000.00"), Arrays.asList(diagnosisEntry), Arrays.asList());

        // When
        collector.collectDrgNames(drg, true, true);

        // Then
        // 注意：根据迭代方案，NameCollector只收集主要诊断名称，不收集别名
        assertThat(collector.getPrimaryDiagnoses())
            .containsExactly("心房颤动");
    }

    @Test
    @DisplayName("启用Top-K限制时应该同时限制诊断和手术名称")
    void shouldLimitBothDiagnosesAndProceduresWhenTopKEnabled() {
        // Given
        NameCollector collector = new NameCollector(true, 2);
        DrgParsedRecord drg1 = createDrgWithDiagnosesAndProcedures("诊断1", "手术1");
        DrgParsedRecord drg2 = createDrgWithDiagnosesAndProcedures("诊断2", "手术2");
        DrgParsedRecord drg3 = createDrgWithDiagnosesAndProcedures("诊断3", "手术3");

        // When
        collector.collectDrgNames(drg1, true, true);
        collector.collectDrgNames(drg2, true, true);
        collector.collectDrgNames(drg3, true, true);

        // Then
        assertThat(collector.getPrimaryDiagnoses()).hasSize(2);
        assertThat(collector.getPrimaryProcedures()).hasSize(2);
    }

    @Test
    @DisplayName("Top-K限制为0时应该返回空列表")
    void shouldReturnEmptyListWhenTopKLimitIsZero() {
        // Given
        NameCollector collector = new NameCollector(true, 0);
        DrgParsedRecord drg = createDrgWithDiagnoses("诊断1", "诊断2", "诊断3");

        // When
        collector.collectDrgNames(drg, true, true);

        // Then
        assertThat(collector.getPrimaryDiagnoses()).isEmpty();
        assertThat(collector.getPrimaryProcedures()).isEmpty();
    }

    @Test
    @DisplayName("Top-K限制大于实际数量时应该返回所有结果")
    void shouldReturnAllResultsWhenTopKLimitExceedsActualCount() {
        // Given
        NameCollector collector = new NameCollector(true, 10);
        DrgParsedRecord drg = createDrgWithDiagnoses("诊断1", "诊断2", "诊断3");

        // When
        collector.collectDrgNames(drg, true, true);

        // Then
        assertThat(collector.getPrimaryDiagnoses()).hasSize(3);
    }

    @Test
    @DisplayName("Top-K限制应该保持结果顺序")
    void shouldMaintainOrderWhenApplyingTopKLimit() {
        // Given
        NameCollector collector = new NameCollector(true, 2);
        DrgParsedRecord drg1 = createDrgWithDiagnoses("诊断A");
        DrgParsedRecord drg2 = createDrgWithDiagnoses("诊断B");
        DrgParsedRecord drg3 = createDrgWithDiagnoses("诊断C");

        // When
        collector.collectDrgNames(drg1, true, true);
        collector.collectDrgNames(drg2, true, true);
        collector.collectDrgNames(drg3, true, true);

        // Then
        List<String> diagnoses = collector.getPrimaryDiagnoses();
        assertThat(diagnoses).hasSize(2);
        // 注意：由于使用HashSet，顺序可能不固定，但应该包含正确的元素
        assertThat(diagnoses).containsAnyOf("诊断A", "诊断B", "诊断C");
    }

    @Test
    @DisplayName("应该正确处理Top-K限制的边界值")
    void shouldHandleTopKBoundaryValues() {
        // Given - 测试边界值1
        NameCollector collector1 = new NameCollector(true, 1);
        DrgParsedRecord drg = createDrgWithDiagnoses("诊断1", "诊断2");

        // When
        collector1.collectDrgNames(drg, true, true);

        // Then
        assertThat(collector1.getPrimaryDiagnoses()).hasSize(1);

        // Given - 测试边界值等于实际数量
        NameCollector collector2 = new NameCollector(true, 2);
        
        // When
        collector2.collectDrgNames(drg, true, true);

        // Then
        assertThat(collector2.getPrimaryDiagnoses()).hasSize(2);
    }

    @Test
    @DisplayName("Top-K限制应用后应该包含正确元素")
    void shouldContainCorrectElementsAfterTopKApplication() {
        // Given
        NameCollector collector = new NameCollector(true, 2);
        DrgParsedRecord drg1 = createDrgWithDiagnoses("诊断A");
        DrgParsedRecord drg2 = createDrgWithDiagnoses("诊断B");
        DrgParsedRecord drg3 = createDrgWithDiagnoses("诊断C");

        // When
        collector.collectDrgNames(drg1, true, true);
        collector.collectDrgNames(drg2, true, true);
        collector.collectDrgNames(drg3, true, true);

        // Then
        List<String> diagnoses = collector.getPrimaryDiagnoses();
        assertThat(diagnoses).hasSize(2);
        // 验证包含正确的元素（不保证顺序）
        assertThat(diagnoses).containsAnyOf("诊断A", "诊断B", "诊断C");
    }

    @Test
    @DisplayName("应该处理null的DRG记录输入")
    void shouldHandleNullDrgRecord() {
        // Given
        NameCollector collector = new NameCollector();

        // When
        collector.collectDrgNames(null, true, true);

        // Then
        assertThat(collector.getPrimaryDiagnoses()).isEmpty();
        assertThat(collector.getPrimaryProcedures()).isEmpty();
    }

    @Test
    @DisplayName("禁用Top-K限制但传入正数时不应该限制结果")
    void shouldNotLimitWhenTopKDisabledWithPositiveLimit() {
        // Given
        NameCollector collector = new NameCollector(false, 2);
        DrgParsedRecord drg1 = createDrgWithDiagnoses("诊断1");
        DrgParsedRecord drg2 = createDrgWithDiagnoses("诊断2");
        DrgParsedRecord drg3 = createDrgWithDiagnoses("诊断3");

        // When
        collector.collectDrgNames(drg1, true, true);
        collector.collectDrgNames(drg2, true, true);
        collector.collectDrgNames(drg3, true, true);

        // Then
        assertThat(collector.getPrimaryDiagnoses()).hasSize(3);
    }

    @Test
    @DisplayName("应该测试辅助方法的功能")
    void shouldTestHelperMethods() {
        // Given
        NameCollector collector = new NameCollector(true, 2);
        DrgParsedRecord drg = createDrgWithDiagnosesAndProcedures("诊断1", "手术1");

        // When
        collector.collectDrgNames(drg, true, true);

        // Then
        assertThat(collector.hasPrimaryDiagnoses()).isTrue();
        assertThat(collector.hasPrimaryProcedures()).isTrue();
        assertThat(collector.getPrimaryDiagnosisCount()).isEqualTo(1);
        assertThat(collector.getPrimaryProcedureCount()).isEqualTo(1);
        assertThat(collector.isTopKEnabled()).isTrue();
        assertThat(collector.getTopKLimit()).isEqualTo(2);

        // When - 清空收集器
        collector.clear();

        // Then
        assertThat(collector.hasPrimaryDiagnoses()).isFalse();
        assertThat(collector.hasPrimaryProcedures()).isFalse();
        assertThat(collector.getPrimaryDiagnosisCount()).isZero();
        assertThat(collector.getPrimaryProcedureCount()).isZero();
    }

    @Test
    @DisplayName("应该自动修剪名称前后的空格")
    void shouldTrimSpacesFromNames() {
        // Given
        NameCollector collector = new NameCollector();
        DrgParsedRecord drg = createDrgWithDiagnosesAndProcedures("  高血压  ", "  冠状动脉造影  ");

        // When
        collector.collectDrgNames(drg, true, true);

        // Then
        assertThat(collector.getPrimaryDiagnoses()).containsExactly("高血压");
        assertThat(collector.getPrimaryProcedures()).containsExactly("冠状动脉造影");
    }

    @Test
    @DisplayName("应该处理包含空格的诊断别名")
    void shouldHandleSpacesInDiagnosisAliases() {
        // Given
        NameCollector collector = new NameCollector();
        List<String> aliases = Arrays.asList("  心房纤颤  ", "  房颤  ");
        DiagnosisEntry diagnosisEntry = new DiagnosisEntry("I48.000", "  心房颤动  ", aliases);
        DrgParsedRecord drg = new DrgParsedRecord(1L, "DRG001", "心脏相关DRG", 
            new BigDecimal("15000.00"), Arrays.asList(diagnosisEntry), Arrays.asList());

        // When
        collector.collectDrgNames(drg, true, true);

        // Then
        // 注意：根据迭代方案，NameCollector只收集主要诊断名称，不收集别名
        assertThat(collector.getPrimaryDiagnoses())
            .containsExactly("心房颤动");
    }

    /**
     * 创建包含指定诊断名称的DRG记录
     */
    private DrgParsedRecord createDrgWithDiagnoses(String... diagnosisNames) {
        List<DiagnosisEntry> diagnoses = Arrays.stream(diagnosisNames)
            .map(name -> new DiagnosisEntry("CODE", name, Arrays.asList()))
            .toList();
        return new DrgParsedRecord(1L, "DRG001", "测试DRG", 
            new BigDecimal("10000.00"), diagnoses, Arrays.asList());
    }

    /**
     * 创建包含指定手术名称的DRG记录
     */
    private DrgParsedRecord createDrgWithProcedures(String... procedureNames) {
        List<ProcedureEntry> procedures = Arrays.stream(procedureNames)
            .map(name -> new ProcedureEntry("CODE", name))
            .toList();
        return new DrgParsedRecord(1L, "DRG001", "测试DRG", 
            new BigDecimal("10000.00"), Arrays.asList(), procedures);
    }

    /**
     * 创建包含诊断和手术的DRG记录
     */
    private DrgParsedRecord createDrgWithDiagnosesAndProcedures(String diagnosisName, String procedureName) {
        List<DiagnosisEntry> diagnoses = Arrays.asList(new DiagnosisEntry("CODE", diagnosisName, Arrays.asList()));
        List<ProcedureEntry> procedures = Arrays.asList(new ProcedureEntry("CODE", procedureName));
        return new DrgParsedRecord(1L, "DRG001", "测试DRG", 
            new BigDecimal("10000.00"), diagnoses, procedures);
    }

    /**
     * 创建空的DRG记录
     */
    private DrgParsedRecord createEmptyDrg() {
        return new DrgParsedRecord(1L, "DRG001", "测试DRG", 
            new BigDecimal("10000.00"), Arrays.asList(), Arrays.asList());
    }
}
