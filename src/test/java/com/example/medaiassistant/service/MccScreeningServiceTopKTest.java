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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * MCC预筛选服务 Top-K控制功能测试
 * 
 * @author MedAI Assistant Team
 * @since 2025-11-09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MCC预筛选服务 Top-K控制功能测试")
class MccScreeningServiceTopKTest {

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
        try {
            var levenshteinUtilField = MccScreeningService.class.getDeclaredField("levenshteinUtil");
            levenshteinUtilField.setAccessible(true);
            levenshteinUtilField.set(mccScreeningService, levenshteinUtil);
            
            var textNormalizerField = MccScreeningService.class.getDeclaredField("textNormalizer");
            textNormalizerField.setAccessible(true);
            textNormalizerField.set(mccScreeningService, textNormalizer);
            
            var propertiesField = MccScreeningService.class.getDeclaredField("mccScreeningProperties");
            propertiesField.setAccessible(true);
            propertiesField.set(mccScreeningService, mccScreeningProperties);
            
            var repositoryField = MccScreeningService.class.getDeclaredField("drgMccRepository");
            repositoryField.setAccessible(true);
            repositoryField.set(mccScreeningService, drgMccRepository);
        } catch (Exception e) {
            throw new RuntimeException("设置依赖失败", e);
        }
    }

    /**
     * 🔴 红阶段测试 - 测试Top-K开关关闭时应返回完整候选列表
     */
    @Test
    @DisplayName("Top-K开关关闭时应返回完整候选列表")
    void shouldReturnFullListWhenTopKDisabled() {
        // Given - 准备测试数据
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I48.000", "心房颤动")
        );
        
        List<DrgMcc> mockMccs = Arrays.asList(
            new DrgMcc(1L, "I48.000", "心房颤动", null, "MCC"),
            new DrgMcc(2L, "I50.000", "心力衰竭", null, "MCC"),
            new DrgMcc(3L, "I10.000", "高血压", null, "CC"),
            new DrgMcc(4L, "E11.900", "2型糖尿病", null, "CC")
        );
        
        // 设置Mock行为
        when(drgMccRepository.findAll()).thenReturn(mockMccs);
        lenient().when(mccScreeningProperties.isTopKEnabled()).thenReturn(false);
        lenient().when(mccScreeningProperties.getSimilarityThreshold()).thenReturn(0.3);
        lenient().when(mccScreeningProperties.isExclusionCheckEnabled()).thenReturn(true);
        
        // 设置相似度计算Mock
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("心房颤动"), any()
        )).thenReturn(1.0);
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("心力衰竭"), any()
        )).thenReturn(0.8);
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("高血压"), any()
        )).thenReturn(0.6);
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("2型糖尿病"), any()
        )).thenReturn(0.2); // 低于阈值，应被过滤
        
        // When - 执行测试方法
        Map<String, List<MccCandidate>> grouped = 
            mccScreeningService.screenMccCandidatesGrouped(diagnoses);
        
        // Then - 验证结果
        assertThat(grouped).containsKey("心房颤动");
        List<MccCandidate> candidates = grouped.get("心房颤动");
        
        // 应该返回所有通过阈值的候选（3个）
        assertThat(candidates).hasSize(3);
    }

    /**
     * 🔴 红阶段测试 - 测试Top-K开关开启时应截断候选列表
     */
    @Test
    @DisplayName("Top-K开关开启时应截断候选列表")
    void shouldTruncateListWhenTopKEnabled() {
        // Given - 准备测试数据
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I48.000", "心房颤动")
        );
        
        List<DrgMcc> mockMccs = Arrays.asList(
            new DrgMcc(1L, "I48.000", "心房颤动", null, "MCC"),
            new DrgMcc(2L, "I50.000", "心力衰竭", null, "MCC"),
            new DrgMcc(3L, "I10.000", "高血压", null, "CC"),
            new DrgMcc(4L, "E11.900", "2型糖尿病", null, "CC"),
            new DrgMcc(5L, "I21.000", "心肌梗死", null, "MCC"),
            new DrgMcc(6L, "I63.000", "脑梗死", null, "MCC")
        );
        
        // 设置Mock行为
        when(drgMccRepository.findAll()).thenReturn(mockMccs);
        lenient().when(mccScreeningProperties.isTopKEnabled()).thenReturn(true);
        lenient().when(mccScreeningProperties.getTopKDiag()).thenReturn(3); // 每诊断Top-3
        lenient().when(mccScreeningProperties.getSimilarityThreshold()).thenReturn(0.3);
        lenient().when(mccScreeningProperties.isExclusionCheckEnabled()).thenReturn(true);
        
        // 设置相似度计算Mock
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("心房颤动"), any()
        )).thenReturn(1.0);
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("心力衰竭"), any()
        )).thenReturn(0.9);
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("心肌梗死"), any()
        )).thenReturn(0.8);
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("脑梗死"), any()
        )).thenReturn(0.7);
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("高血压"), any()
        )).thenReturn(0.6);
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("2型糖尿病"), any()
        )).thenReturn(0.4);
        
        // When - 执行测试方法
        Map<String, List<MccCandidate>> grouped = 
            mccScreeningService.screenMccCandidatesGrouped(diagnoses);
        
        // Then - 验证结果
        assertThat(grouped).containsKey("心房颤动");
        List<MccCandidate> candidates = grouped.get("心房颤动");
        
        // 应该只返回Top-3候选
        assertThat(candidates).hasSize(3);
        
        // 验证返回的是相似度最高的3个候选
        assertThat(candidates.get(0).getSimilarity()).isEqualTo(1.0);
        assertThat(candidates.get(1).getSimilarity()).isEqualTo(0.9);
        assertThat(candidates.get(2).getSimilarity()).isEqualTo(0.8);
    }

    /**
     * 🔴 红阶段测试 - 测试候选数量少于Top-K时应返回完整列表
     */
    @Test
    @DisplayName("候选数量少于Top-K时应返回完整列表")
    void shouldReturnFullListWhenCandidatesLessThanTopK() {
        // Given - 准备测试数据
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I48.000", "心房颤动")
        );
        
        List<DrgMcc> mockMccs = Arrays.asList(
            new DrgMcc(1L, "I48.000", "心房颤动", null, "MCC"),
            new DrgMcc(2L, "I50.000", "心力衰竭", null, "MCC")
        );
        
        // 设置Mock行为
        when(drgMccRepository.findAll()).thenReturn(mockMccs);
        lenient().when(mccScreeningProperties.isTopKEnabled()).thenReturn(true);
        lenient().when(mccScreeningProperties.getTopKDiag()).thenReturn(5); // Top-5
        lenient().when(mccScreeningProperties.getSimilarityThreshold()).thenReturn(0.3);
        lenient().when(mccScreeningProperties.isExclusionCheckEnabled()).thenReturn(true);
        
        // 设置相似度计算Mock
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("心房颤动"), any()
        )).thenReturn(1.0);
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("心力衰竭"), any()
        )).thenReturn(0.8);
        
        // When - 执行测试方法
        Map<String, List<MccCandidate>> grouped = 
            mccScreeningService.screenMccCandidatesGrouped(diagnoses);
        
        // Then - 验证结果
        assertThat(grouped).containsKey("心房颤动");
        List<MccCandidate> candidates = grouped.get("心房颤动");
        
        // 应该返回所有候选（2个），因为少于Top-K值
        assertThat(candidates).hasSize(2);
    }

    /**
     * 🔴 红阶段测试 - 测试平铺列表方法也应支持Top-K控制
     */
    @Test
    @DisplayName("平铺列表方法也应支持Top-K控制")
    void shouldApplyTopKToFlatList() {
        // Given - 准备测试数据
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I48.000", "心房颤动")
        );
        
        List<DrgMcc> mockMccs = Arrays.asList(
            new DrgMcc(1L, "I48.000", "心房颤动", null, "MCC"),
            new DrgMcc(2L, "I50.000", "心力衰竭", null, "MCC"),
            new DrgMcc(3L, "I10.000", "高血压", null, "CC"),
            new DrgMcc(4L, "E11.900", "2型糖尿病", null, "CC"),
            new DrgMcc(5L, "I21.000", "心肌梗死", null, "MCC")
        );
        
        // 设置Mock行为
        when(drgMccRepository.findAll()).thenReturn(mockMccs);
        lenient().when(mccScreeningProperties.isTopKEnabled()).thenReturn(true);
        lenient().when(mccScreeningProperties.getTopKDiag()).thenReturn(2); // Top-2
        lenient().when(mccScreeningProperties.getSimilarityThreshold()).thenReturn(0.3);
        lenient().when(mccScreeningProperties.isExclusionCheckEnabled()).thenReturn(true);
        
        // 设置相似度计算Mock
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("心房颤动"), any()
        )).thenReturn(1.0);
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("心力衰竭"), any()
        )).thenReturn(0.9);
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("心肌梗死"), any()
        )).thenReturn(0.8);
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("高血压"), any()
        )).thenReturn(0.7);
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("2型糖尿病"), any()
        )).thenReturn(0.6);
        
        // When - 执行测试方法
        List<MccCandidate> flatList = mccScreeningService.screenMccCandidates(diagnoses);
        
        // Then - 验证结果
        // 应该返回所有候选（5个），因为平铺列表不应用Top-K
        assertThat(flatList).hasSize(5);
    }

    /**
     * 🔴 红阶段测试 - 测试多诊断场景下的Top-K控制
     */
    @Test
    @DisplayName("多诊断场景下应对每个诊断单独应用Top-K")
    void shouldApplyTopKPerDiagnosis() {
        // Given - 准备测试数据
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I48.000", "心房颤动"),
            new PatientDiagnosis("I50.000", "心力衰竭")
        );
        
        List<DrgMcc> mockMccs = Arrays.asList(
            new DrgMcc(1L, "I48.000", "心房颤动", null, "MCC"),
            new DrgMcc(2L, "I50.000", "心力衰竭", null, "MCC"),
            new DrgMcc(3L, "I10.000", "高血压", null, "CC"),
            new DrgMcc(4L, "E11.900", "2型糖尿病", null, "CC"),
            new DrgMcc(5L, "I21.000", "心肌梗死", null, "MCC")
        );
        
        // 设置Mock行为
        when(drgMccRepository.findAll()).thenReturn(mockMccs);
        lenient().when(mccScreeningProperties.isTopKEnabled()).thenReturn(true);
        lenient().when(mccScreeningProperties.getTopKDiag()).thenReturn(2); // 每诊断Top-2
        lenient().when(mccScreeningProperties.getSimilarityThreshold()).thenReturn(0.3);
        lenient().when(mccScreeningProperties.isExclusionCheckEnabled()).thenReturn(true);
        
        // 设置相似度计算Mock
        // 心房颤动相关相似度
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("心房颤动"), any()
        )).thenReturn(1.0);
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("心力衰竭"), any()
        )).thenReturn(0.8);
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("心肌梗死"), any()
        )).thenReturn(0.7);
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("高血压"), any()
        )).thenReturn(0.6);
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心房颤动"), eq("2型糖尿病"), any()
        )).thenReturn(0.4);
        
        // 心力衰竭相关相似度
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心力衰竭"), eq("心力衰竭"), any()
        )).thenReturn(1.0);
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心力衰竭"), eq("心房颤动"), any()
        )).thenReturn(0.8);
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心力衰竭"), eq("心肌梗死"), any()
        )).thenReturn(0.7);
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心力衰竭"), eq("高血压"), any()
        )).thenReturn(0.6);
        lenient().when(levenshteinUtil.calculateNormalizedSimilarity(
            eq("心力衰竭"), eq("2型糖尿病"), any()
        )).thenReturn(0.4);
        
        // When - 执行测试方法
        Map<String, List<MccCandidate>> grouped = 
            mccScreeningService.screenMccCandidatesGrouped(diagnoses);
        
        // Then - 验证结果
        assertThat(grouped).containsKeys("心房颤动", "心力衰竭");
        
        // 每个诊断应该只返回Top-2候选
        assertThat(grouped.get("心房颤动")).hasSize(2);
        assertThat(grouped.get("心力衰竭")).hasSize(2);
    }
}
