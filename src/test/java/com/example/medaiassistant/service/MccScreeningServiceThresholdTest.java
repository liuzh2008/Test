package com.example.medaiassistant.service;

import com.example.medaiassistant.config.MccScreeningProperties;
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
 * MCC预筛选服务阈值配置测试类
 * 测试故事3: 配置化阈值管理
 * 
 * @author MedAI Assistant Team
 * @since 2025-11-09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MCC预筛选服务 阈值配置测试")
class MccScreeningServiceThresholdTest {

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
     * 🟢 绿阶段测试1: 应该从配置文件读取全局阈值
     * 测试目标: 支持从配置文件读取全局阈值（默认0.3）
     */
    @Test
    @DisplayName("应该从配置文件读取全局阈值")
    void shouldReadGlobalThresholdFromConfiguration() {
        // Given - 准备测试数据
        when(mccScreeningProperties.getSimilarityThreshold()).thenReturn(0.3);
        
        // When - 调用方法
        double currentThreshold = mccScreeningService.getCurrentThreshold();
        
        // Then - 验证结果
        assertThat(currentThreshold).isEqualTo(0.3); // 默认阈值应为0.3
    }

    /**
     * 🟢 绿阶段测试2: 应该支持流程处理器级别的局部阈值覆盖
     * 测试目标: 支持流程处理器级别的局部阈值覆盖
     */
    @Test
    @DisplayName("应该支持流程处理器级别的局部阈值覆盖")
    void shouldSupportProcessorLevelThresholdOverride() {
        // Given - 准备测试数据
        double similarity = 0.5;
        Double customThreshold = 0.4; // 局部阈值
        
        // When - 调用方法
        boolean result = mccScreeningService.isSimilarityAboveThreshold(similarity, customThreshold);
        
        // Then - 验证结果
        assertThat(result).isTrue(); // 0.5 > 0.4，应该通过
    }

    /**
     * 🟢 绿阶段测试3: 应该支持方法调用时传入可选阈值参数
     * 测试目标: 支持方法调用时传入可选阈值参数
     */
    @Test
    @DisplayName("应该支持方法调用时传入可选阈值参数")
    void shouldSupportOptionalThresholdParameter() {
        // Given - 准备测试数据
        double similarity = 0.5;
        
        // When - 调用方法（使用可选阈值参数）
        boolean resultWithThreshold = mccScreeningService.isSimilarityAboveThreshold(similarity, 0.4);
        boolean resultWithoutThreshold = mccScreeningService.isSimilarityAboveThreshold(similarity);
        
        // Then - 验证结果
        assertThat(resultWithThreshold).isTrue(); // 0.5 > 0.4，应该通过
        assertThat(resultWithoutThreshold).isTrue(); // 0.5 > 0.3（默认），应该通过
    }

    /**
     * 🟢 绿阶段测试4: 应该使用默认阈值过滤相似度候选
     * 测试目标: 验证相似度低于阈值的候选被正确过滤
     */
    @Test
    @DisplayName("应该使用默认阈值过滤相似度候选")
    void shouldFilterCandidatesByDefaultThreshold() {
        // Given - 准备测试数据
        when(mccScreeningProperties.getSimilarityThreshold()).thenReturn(0.3);
        double similarityBelowThreshold = 0.25; // 低于默认阈值0.3
        
        // When - 调用方法
        boolean result = mccScreeningService.isSimilarityAboveThreshold(similarityBelowThreshold);
        
        // Then - 验证结果
        assertThat(result).isFalse(); // 0.25 < 0.3，应该被过滤
    }

    /**
     * 🟢 绿阶段测试5: 应该保留相似度高于阈值的候选
     * 测试目标: 验证相似度高于阈值的候选被正确保留
     */
    @Test
    @DisplayName("应该保留相似度高于阈值的候选")
    void shouldKeepCandidatesAboveThreshold() {
        // Given - 准备测试数据
        when(mccScreeningProperties.getSimilarityThreshold()).thenReturn(0.3);
        double similarityAboveThreshold = 0.75; // 高于默认阈值0.3
        
        // When - 调用方法
        boolean result = mccScreeningService.isSimilarityAboveThreshold(similarityAboveThreshold);
        
        // Then - 验证结果
        assertThat(result).isTrue(); // 0.75 > 0.3，应该被保留
    }
}
