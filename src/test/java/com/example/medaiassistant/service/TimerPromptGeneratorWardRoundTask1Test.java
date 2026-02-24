package com.example.medaiassistant.service;

import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.repository.PromptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 任务1：lastRoundTime 计算与首次查房处理 - 绿阶段测试
 * 
 * 测试TimerPromptGenerator中查找上次查房时间的方法
 * 使用Mockito进行业务逻辑层单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("任务1：lastRoundTime 计算与首次查房处理")
class TimerPromptGeneratorWardRoundTask1Test {

    @Mock
    private PromptRepository promptRepository;
    
    private TimerPromptGenerator timerPromptGenerator;
    
    @BeforeEach
    void setUp() {
        // 创建TimerPromptGenerator实例，使用最小化的构造参数
        // 只传promptRepository，其他参数传null（因为本测试只测试findLastWardRoundTime方法）
        timerPromptGenerator = new TimerPromptGenerator(
            null, null, null, null, 
            promptRepository, // 只有promptRepository是必需的
            null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
    }

    /**
     * 绿阶段测试用例1：存在历史查房记录时返回最新的submissionTime
     * 
     * Given：prompRepository中存在同一患者、同一模板名的Prompt记录
     * When：调用findLastWardRoundTime(patientId, templateName)
     * Then：返回的时间等于最新一条记录的submissionTime
     */
    @Test
    @DisplayName("存在历史查房记录时应返回最新的submissionTime")
    void testFindLastWardRoundTime_WithHistory_ReturnsLatestSubmissionTime() {
        // Arrange
        String patientId = "TEST_PATIENT_001";
        String templateName = "病危每日查房记录";
        LocalDateTime expectedTime = LocalDateTime.of(2026, 2, 9, 12, 0);
        
        Prompt mockPrompt = new Prompt();
        mockPrompt.setSubmissionTime(expectedTime);
        
        // Mock repository返回值
        when(promptRepository.findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(
            patientId, templateName))
            .thenReturn(Optional.of(mockPrompt));
        
        // Act - 使用反射调用private方法findLastWardRoundTime
        LocalDateTime result = invokeFindLastWardRoundTime(patientId, templateName);
        
        // Assert
        assertNotNull(result, "应该返回非空的时间");
        assertEquals(expectedTime, result, "应该返回最新的submissionTime");
        
        // Verify repository被调用
        verify(promptRepository).findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(
            patientId, templateName);
    }

    /**
     * 绿阶段测试用例2：无历史记录时返回null
     * 
     * Given：prompRepository中不存在该患者该模板名记录
     * When：调用findLastWardRoundTime(patientId, templateName)
     * Then：返回null
     */
    @Test
    @DisplayName("无历史查房记录时应返回null")
    void testFindLastWardRoundTime_NoHistory_ReturnsNull() {
        // Arrange
        String patientId = "TEST_PATIENT_NONEXISTENT";
        String templateName = "病危每日查房记录";
        
        // Mock repository返回Optional.empty()
        when(promptRepository.findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(
            patientId, templateName))
            .thenReturn(Optional.empty());
        
        // Act - 使用反射调用private方法findLastWardRoundTime
        LocalDateTime result = invokeFindLastWardRoundTime(patientId, templateName);
        
        // Assert
        assertNull(result, "不存在历史记录时应返回null");
        
        // Verify repository被调用
        verify(promptRepository).findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(
            patientId, templateName);
    }

    /**
     * 绿阶段测试用例3：不同模板名的记录不相互干扰
     * 
     * Given：存在不同模板名的Prompt记录
     * When：按特定模板名查询
     * Then：只考虑该模板名下的历史记录
     */
    @Test
    @DisplayName("不同模板名的记录不应相互干扰")
    void testFindLastWardRoundTime_DifferentTemplateNotMixed() {
        // Arrange
        String patientId = "TEST_PATIENT_002";
        String templateName1 = "病危每日查房记录";
        String templateName2 = "病重每2日查房记录";
        
        LocalDateTime time1 = LocalDateTime.of(2026, 2, 7, 12, 0);
        
        Prompt mockPrompt1 = new Prompt();
        mockPrompt1.setSubmissionTime(time1);
        
        // Mock repository对不同模板名返回不同结果
        when(promptRepository.findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(
            patientId, templateName1))
            .thenReturn(Optional.of(mockPrompt1));
        when(promptRepository.findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(
            patientId, templateName2))
            .thenReturn(Optional.empty());
        
        // Act - 查询templateName1的最新记录
        LocalDateTime result1 = invokeFindLastWardRoundTime(patientId, templateName1);
        LocalDateTime result2 = invokeFindLastWardRoundTime(patientId, templateName2);
        
        // Assert
        assertNotNull(result1, "应该返回非空的时间");
        assertEquals(time1, result1, "应该返回templateName1的submissionTime");
        assertNull(result2, "templateName2应该返回null");
        
        // Verify repository被调用
        verify(promptRepository).findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(
            patientId, templateName1);
        verify(promptRepository).findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(
            patientId, templateName2);
    }
    
    /**
     * 辅助方法：使用反射调用private方法findLastWardRoundTime
     */
    private LocalDateTime invokeFindLastWardRoundTime(String patientId, String templateName) {
        try {
            Method method = TimerPromptGenerator.class.getDeclaredMethod(
                "findLastWardRoundTime", String.class, String.class);
            method.setAccessible(true);
            return (LocalDateTime) method.invoke(timerPromptGenerator, patientId, templateName);
        } catch (Exception e) {
            throw new RuntimeException("调用findLastWardRoundTime方法失败", e);
        }
    }
}
