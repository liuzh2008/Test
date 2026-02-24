package com.example.medaiassistant.service;

import com.example.medaiassistant.config.MccScreeningProperties;
import com.example.medaiassistant.model.DrgMcc;
import com.example.medaiassistant.model.MccCandidate;
import com.example.medaiassistant.model.PatientDiagnosis;
import com.example.medaiassistant.repository.DrgMccRepository;
import com.example.medaiassistant.util.LevenshteinUtil;
import com.example.medaiassistant.util.TextNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * MCC预筛选服务 分组与排序功能测试
 * 故事5: 分组与排序输出
 * 
 * @author MedAI Assistant Team
 * @since 2025-11-09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MCC预筛选服务 分组与排序功能测试")
class MccScreeningServiceGroupingSortingTest {

    @Mock
    private LevenshteinUtil levenshteinUtil;

    @Mock
    private TextNormalizer textNormalizer;

    @Mock
    private MccScreeningProperties mccScreeningProperties;

    @Mock
    private DrgMccRepository drgMccRepository;

    private MccScreeningService mccScreeningService;

    @BeforeEach
    void setUp() {
        mccScreeningService = new MccScreeningService();
        // 使用反射设置依赖
        setField(mccScreeningService, "levenshteinUtil", levenshteinUtil);
        setField(mccScreeningService, "textNormalizer", textNormalizer);
        setField(mccScreeningService, "mccScreeningProperties", mccScreeningProperties);
        setField(mccScreeningService, "drgMccRepository", drgMccRepository);
    }

    /**
     * 使用反射设置字段值
     */
    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("设置字段失败: " + fieldName, e);
        }
    }

    /**
     * 🟢 绿阶段测试1: 应该按来源诊断分组候选
     */
    @Test
    @DisplayName("应该按来源诊断分组候选")
    void shouldGroupCandidatesBySourceDiagnosis() {
        // Given
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I48.000", "心房颤动"),
            new PatientDiagnosis("I50.000", "心力衰竭")
        );
        
        // 设置配置
        when(mccScreeningProperties.getSimilarityThreshold()).thenReturn(0.3);
        when(mccScreeningProperties.isExclusionCheckEnabled()).thenReturn(true);
        
        // 设置Mock数据
        List<DrgMcc> mockMccs = Arrays.asList(
            new DrgMcc(1L, "I48.000", "心房颤动", null, "MCC"),
            new DrgMcc(2L, "I50.000", "心力衰竭", null, "MCC")
        );
        when(drgMccRepository.findAll()).thenReturn(mockMccs);
        
        // 设置相似度计算Mock
        when(levenshteinUtil.calculateNormalizedSimilarity(any(), any(), any()))
            .thenReturn(0.8);

        // When
        Map<String, List<MccCandidate>> grouped = 
            mccScreeningService.screenMccCandidatesGrouped(diagnoses);

        // Then
        assertThat(grouped).containsKeys("心房颤动", "心力衰竭");
    }

    /**
     * 🔴 红阶段测试2: 相同相似度时MCC应优先于CC
     * 这个测试应该失败，因为sortCandidates方法尚未实现
     */
    @Test
    @DisplayName("相同相似度时MCC应优先于CC")
    void shouldPrioritizeMccOverCcWhenSameSimilarity() {
        // Given
        List<MccCandidate> candidates = Arrays.asList(
            createCandidate("CC001", "CC", 0.8),
            createCandidate("MCC001", "MCC", 0.8)
        );

        // When
        List<MccCandidate> sorted = mccScreeningService.sortCandidates(candidates);

        // Then
        assertThat(sorted).hasSize(2);
        assertThat(sorted.get(0).getMccType()).isEqualTo("MCC");
        assertThat(sorted.get(1).getMccType()).isEqualTo("CC");
    }

    /**
     * 🔴 红阶段测试3: 应该按相似度降序排序
     * 这个测试应该失败，因为sortCandidates方法尚未实现
     */
    @Test
    @DisplayName("应该按相似度降序排序")
    void shouldSortCandidatesBySimilarityDescending() {
        // Given
        List<MccCandidate> candidates = Arrays.asList(
            createCandidate("MCC001", "MCC", 0.6),
            createCandidate("MCC002", "MCC", 0.9),
            createCandidate("MCC003", "MCC", 0.7)
        );

        // When
        List<MccCandidate> sorted = mccScreeningService.sortCandidates(candidates);

        // Then
        assertThat(sorted).hasSize(3);
        assertThat(sorted.get(0).getSimilarity()).isEqualTo(0.9);
        assertThat(sorted.get(1).getSimilarity()).isEqualTo(0.7);
        assertThat(sorted.get(2).getSimilarity()).isEqualTo(0.6);
    }

    /**
     * 🟢 绿阶段测试4: 应该同时提供平铺列表和分组结构
     */
    @Test
    @DisplayName("应该同时提供平铺列表和分组结构")
    void shouldProvideBothFlatListAndGroupedStructure() {
        // Given
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I48.000", "心房颤动"),
            new PatientDiagnosis("I50.000", "心力衰竭")
        );
        
        // 设置配置
        when(mccScreeningProperties.getSimilarityThreshold()).thenReturn(0.3);
        when(mccScreeningProperties.isExclusionCheckEnabled()).thenReturn(true);
        
        // 设置Mock数据
        List<DrgMcc> mockMccs = Arrays.asList(
            new DrgMcc(1L, "I48.000", "心房颤动", null, "MCC"),
            new DrgMcc(2L, "I50.000", "心力衰竭", null, "MCC")
        );
        when(drgMccRepository.findAll()).thenReturn(mockMccs);
        
        // 设置相似度计算Mock
        when(levenshteinUtil.calculateNormalizedSimilarity(any(), any(), any()))
            .thenReturn(0.8);

        // When
        List<MccCandidate> flatList = mccScreeningService.screenMccCandidates(diagnoses);
        Map<String, List<MccCandidate>> grouped = mccScreeningService.screenMccCandidatesGrouped(diagnoses);

        // Then
        assertThat(flatList).isNotNull();
        assertThat(grouped).isNotNull();
        // 平铺列表应该包含所有候选
        assertThat(flatList).hasSizeGreaterThan(0);
        // 分组结构应该包含所有诊断
        assertThat(grouped).containsKeys("心房颤动", "心力衰竭");
    }

    /**
     * 🟢 绿阶段测试5: 分组内候选应该已排序
     */
    @Test
    @DisplayName("分组内候选应该已排序")
    void shouldHaveSortedCandidatesWithinGroups() {
        // Given
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I48.000", "心房颤动")
        );
        
        // 设置配置
        when(mccScreeningProperties.getSimilarityThreshold()).thenReturn(0.3);
        when(mccScreeningProperties.isExclusionCheckEnabled()).thenReturn(true);
        
        // 设置Mock数据
        List<DrgMcc> mockMccs = Arrays.asList(
            new DrgMcc(1L, "I48.000", "心房颤动", null, "MCC"),
            new DrgMcc(2L, "I50.000", "心力衰竭", null, "MCC")
        );
        when(drgMccRepository.findAll()).thenReturn(mockMccs);
        
        // 设置相似度计算Mock
        when(levenshteinUtil.calculateNormalizedSimilarity(any(), any(), any()))
            .thenReturn(0.8);

        // When
        Map<String, List<MccCandidate>> grouped = 
            mccScreeningService.screenMccCandidatesGrouped(diagnoses);

        // Then
        assertThat(grouped).containsKey("心房颤动");
        List<MccCandidate> candidates = grouped.get("心房颤动");
        
        // 验证分组内的候选已按相似度降序排序
        for (int i = 0; i < candidates.size() - 1; i++) {
            assertThat(candidates.get(i).getSimilarity())
                .isGreaterThanOrEqualTo(candidates.get(i + 1).getSimilarity());
        }
    }

    /**
     * 创建测试用的MCC候选对象
     */
    private MccCandidate createCandidate(String code, String type, double similarity) {
        return MccCandidate.builder()
            .mccCode(code)
            .mccName("测试MCC名称")
            .mccType(type)
            .similarity(similarity)
            .matchType(MccCandidate.MATCH_TYPE_NAME_MATCH)
            .excluded(false)
            .sourceDiagnosis("测试诊断")
            .sourceIcdCode("I00.000")
            .build();
    }
}
