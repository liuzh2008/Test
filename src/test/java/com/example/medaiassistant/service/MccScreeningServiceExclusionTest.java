package com.example.medaiassistant.service;

import com.example.medaiassistant.config.MccScreeningProperties;
import com.example.medaiassistant.model.DrgMcc;
import com.example.medaiassistant.model.PatientDiagnosis;
import com.example.medaiassistant.repository.DrgMccRepository;
import com.example.medaiassistant.util.LevenshteinUtil;
import com.example.medaiassistant.util.TextNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * MCC预筛选服务排除规则检查测试类
 * 测试故事4: 排除规则检查
 * 
 * @author MedAI Assistant Team
 * @since 2025-11-09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MCC预筛选服务 排除规则检查测试")
class MccScreeningServiceExclusionTest {

    @Mock
    private LevenshteinUtil levenshteinUtil;

    @Mock
    private TextNormalizer textNormalizer;

    @Mock
    private DrgMccRepository drgMccRepository;

    @Mock
    private MccScreeningProperties mccScreeningProperties;

    @InjectMocks
    private MccScreeningService mccScreeningService;

    @BeforeEach
    void setUp() {
        // 不需要在setUp中设置默认配置值，因为测试方法会按需设置
    }

    /**
     * 🔴 红阶段测试1: 应该解析单个排除条件并正确排除
     * 测试目标: 能够解析MCC_EXCEPT字段中的ICD编码列表（支持逗号/分号/空格分隔）
     * 预期: 当患者诊断包含排除条件中的编码时，该MCC候选被标记为excluded=true
     */
    @Test
    @DisplayName("应该解析单个排除条件并正确排除")
    void shouldParseSingleExclusionAndExcludeCorrectly() {
        // Given - 准备测试数据
        when(mccScreeningProperties.isExclusionCheckEnabled()).thenReturn(true);
        PatientDiagnosis diagnosis = new PatientDiagnosis("I48.000", "心房颤动");
        DrgMcc mcc = new DrgMcc(1L, "I50.000", "心力衰竭", "I48.000", "MCC");
        
        // When - 调用方法
        boolean excluded = mccScreeningService.checkExclusionRules(diagnosis, mcc);
        
        // Then - 验证结果
        assertThat(excluded).isTrue(); // 患者诊断I48.000在排除列表中，应该被排除
    }

    /**
     * 🔴 红阶段测试2: 应该支持多种分隔符
     * 测试目标: 能够解析逗号、分号、空格分隔的排除条件
     */
    @Test
    @DisplayName("应该支持多种分隔符")
    void shouldSupportMultipleDelimiters() {
        // Given - 准备测试数据
        when(mccScreeningProperties.isExclusionCheckEnabled()).thenReturn(true);
        PatientDiagnosis diagnosis = new PatientDiagnosis("I48.000", "心房颤动");
        
        // When & Then - 验证不同分隔符
        // 逗号分隔
        DrgMcc mcc1 = new DrgMcc(1L, "I50.000", "心力衰竭", "I10.000,I48.000", "MCC");
        assertThat(mccScreeningService.checkExclusionRules(diagnosis, mcc1)).isTrue();
        
        // 分号分隔
        DrgMcc mcc2 = new DrgMcc(2L, "I50.000", "心力衰竭", "I10.000;I48.000", "MCC");
        assertThat(mccScreeningService.checkExclusionRules(diagnosis, mcc2)).isTrue();
        
        // 空格分隔
        DrgMcc mcc3 = new DrgMcc(3L, "I50.000", "心力衰竭", "I10.000 I48.000", "MCC");
        assertThat(mccScreeningService.checkExclusionRules(diagnosis, mcc3)).isTrue();
    }

    /**
     * 🔴 红阶段测试3: 应该忽略空排除条件
     * 测试目标: 当MCC_EXCEPT为空或空白时，不进行排除检查
     */
    @Test
    @DisplayName("应该忽略空排除条件")
    void shouldIgnoreEmptyExclusionConditions() {
        // Given - 准备测试数据
        PatientDiagnosis diagnosis = new PatientDiagnosis("I48.000", "心房颤动");
        
        // When & Then - 验证空排除条件
        DrgMcc mcc1 = new DrgMcc(1L, "I50.000", "心力衰竭", null, "MCC");
        assertThat(mccScreeningService.checkExclusionRules(diagnosis, mcc1)).isFalse();
        
        DrgMcc mcc2 = new DrgMcc(2L, "I50.000", "心力衰竭", "", "MCC");
        assertThat(mccScreeningService.checkExclusionRules(diagnosis, mcc2)).isFalse();
        
        DrgMcc mcc3 = new DrgMcc(3L, "I50.000", "心力衰竭", "   ", "MCC");
        assertThat(mccScreeningService.checkExclusionRules(diagnosis, mcc3)).isFalse();
    }

    /**
     * 🔴 红阶段测试4: 应该支持排除规则开关控制
     * 测试目标: 支持开关控制排除检查（drg.mcc.exclusion-check-enabled）
     */
    @Test
    @DisplayName("应该支持排除规则开关控制")
    void shouldSupportExclusionCheckSwitch() {
        // Given - 准备测试数据
        PatientDiagnosis diagnosis = new PatientDiagnosis("I48.000", "心房颤动");
        DrgMcc mcc = new DrgMcc(1L, "I50.000", "心力衰竭", "I48.000", "MCC");
        
        // When - 禁用排除检查
        when(mccScreeningProperties.isExclusionCheckEnabled()).thenReturn(false);
        boolean excludedWhenDisabled = mccScreeningService.checkExclusionRules(diagnosis, mcc);
        
        // Then - 验证禁用时不被排除
        assertThat(excludedWhenDisabled).isFalse();
        
        // When - 启用排除检查
        when(mccScreeningProperties.isExclusionCheckEnabled()).thenReturn(true);
        boolean excludedWhenEnabled = mccScreeningService.checkExclusionRules(diagnosis, mcc);
        
        // Then - 验证启用时被排除
        assertThat(excludedWhenEnabled).isTrue();
    }

    /**
     * 🔴 红阶段测试5: 应该忽略大小写比较
     * 测试目标: ICD编码比较应该忽略大小写
     */
    @Test
    @DisplayName("应该忽略大小写比较")
    void shouldIgnoreCaseInComparison() {
        // Given - 准备测试数据（大小写混合）
        when(mccScreeningProperties.isExclusionCheckEnabled()).thenReturn(true);
        PatientDiagnosis diagnosis1 = new PatientDiagnosis("i48.000", "心房颤动"); // 小写
        PatientDiagnosis diagnosis2 = new PatientDiagnosis("I48.000", "心房颤动"); // 大写
        DrgMcc mcc = new DrgMcc(1L, "I50.000", "心力衰竭", "I48.000", "MCC");
        
        // When & Then - 验证大小写不敏感
        assertThat(mccScreeningService.checkExclusionRules(diagnosis1, mcc)).isTrue();
        assertThat(mccScreeningService.checkExclusionRules(diagnosis2, mcc)).isTrue();
    }

    /**
     * 🔴 红阶段测试6: 应该处理空诊断编码
     * 测试目标: 当患者诊断编码为空时，不进行排除检查
     */
    @Test
    @DisplayName("应该处理空诊断编码")
    void shouldHandleNullDiagnosisCode() {
        // Given - 准备测试数据
        PatientDiagnosis diagnosis = new PatientDiagnosis(null, "心房颤动");
        DrgMcc mcc = new DrgMcc(1L, "I50.000", "心力衰竭", "I48.000", "MCC");
        
        // When - 调用方法
        boolean excluded = mccScreeningService.checkExclusionRules(diagnosis, mcc);
        
        // Then - 验证结果
        assertThat(excluded).isFalse(); // 诊断编码为空，不应该被排除
    }

    /**
     * 🔴 红阶段测试7: 应该处理空MCC对象
     * 测试目标: 当MCC对象为空时，不进行排除检查
     */
    @Test
    @DisplayName("应该处理空MCC对象")
    void shouldHandleNullMccObject() {
        // Given - 准备测试数据
        PatientDiagnosis diagnosis = new PatientDiagnosis("I48.000", "心房颤动");
        
        // When - 调用方法
        boolean excluded = mccScreeningService.checkExclusionRules(diagnosis, null);
        
        // Then - 验证结果
        assertThat(excluded).isFalse(); // MCC对象为空，不应该被排除
    }

    /**
     * 🔴 红阶段测试8: 应该处理空诊断对象
     * 测试目标: 当诊断对象为空时，不进行排除检查
     */
    @Test
    @DisplayName("应该处理空诊断对象")
    void shouldHandleNullDiagnosisObject() {
        // Given - 准备测试数据
        DrgMcc mcc = new DrgMcc(1L, "I50.000", "心力衰竭", "I48.000", "MCC");
        
        // When - 调用方法
        boolean excluded = mccScreeningService.checkExclusionRules(null, mcc);
        
        // Then - 验证结果
        assertThat(excluded).isFalse(); // 诊断对象为空，不应该被排除
    }
}
