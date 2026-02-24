package com.example.medaiassistant.service;

import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.repository.PromptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Prompt保存幂等性测试类
 * 按照TDD红-绿-重构流程进行开发
 * 
 * 测试用例：
 * - 测试相同上下文多次保存返回相同prompt_id
 */
@ExtendWith(MockitoExtension.class)
class PromptSaveIdempotencyTest {

    @Mock
    private PromptRepository promptRepository;

    @InjectMocks
    private DrgAiAnalysisService drgAiAnalysisService;

    /**
     * 测试相同上下文多次保存返回相同prompt_id
     * 绿阶段：测试用例应该通过，因为save方法已实现
     */
    @Test
    void savePrompt_ShouldReturnSamePromptId_WhenSameContextSavedMultipleTimes() {
        // 准备测试数据
        String patientId = "P001";
        String templateName = "合并症或并发症分析";
        String objectiveContent = "患者基本信息...";
        String dailyRecords = "日常记录...";
        String templateContent = "模板内容...";
        
        // 模拟已存在的Prompt
        Prompt existingPrompt = new Prompt();
        existingPrompt.setPromptId(123);
        existingPrompt.setPatientId(patientId);
        existingPrompt.setPromptTemplateName(templateName);
        existingPrompt.setObjectiveContent(objectiveContent);
        existingPrompt.setDailyRecords(dailyRecords);
        existingPrompt.setPromptTemplateContent(templateContent);
        existingPrompt.setSubmissionTime(LocalDateTime.now());
        
        // 设置Mock行为 - 查找已存在的Prompt
        when(promptRepository.findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(
                patientId, templateName))
                .thenReturn(Optional.of(existingPrompt));
        
        // 执行测试 - 期望返回已存在的prompt_id
        Integer result = drgAiAnalysisService.savePrompt(patientId, templateName, objectiveContent, dailyRecords, templateContent);
        
        // 验证结果 - 应该返回已存在的prompt_id
        assertEquals(123, result, "应该返回已存在的prompt_id");
        
        // 验证Repository被调用
        verify(promptRepository).findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(
                patientId, templateName);
        // 验证没有创建新的Prompt
        verify(promptRepository, never()).save(any(Prompt.class));
    }

}
