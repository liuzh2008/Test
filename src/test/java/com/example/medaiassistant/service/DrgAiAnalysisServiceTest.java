package com.example.medaiassistant.service;

import com.example.medaiassistant.model.PromptTemplate;
import com.example.medaiassistant.repository.PromptTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DRGs AI分析服务测试类
 * 按照TDD红-绿-重构流程进行开发
 */
@ExtendWith(MockitoExtension.class)
class DrgAiAnalysisServiceTest {

    @Mock
    private PromptTemplateRepository promptTemplateRepository;

    @InjectMocks
    private DrgAiAnalysisService drgAiAnalysisService;

    /**
     * 测试从PROMPTTEMPLATE表读取"合并症或并发症分析"模板
     */
    @Test
    void generateAnalysisPrompt_ShouldReturnPrompt_WhenTemplateExists() {
        // 准备测试数据
        String templateName = "合并症或并发症分析";
        String expectedPrompt = "请分析患者的合并症或并发症情况...";
        
        PromptTemplate mockTemplate = new PromptTemplate();
        mockTemplate.setPromptName(templateName);
        mockTemplate.setPrompt(expectedPrompt);
        
        // 设置Mock行为
        when(promptTemplateRepository.findByPromptTypeAndPromptName("DRG_ANALYSIS", templateName))
                .thenReturn(mockTemplate);

        // 执行测试
        String result = drgAiAnalysisService.generateAnalysisPrompt(templateName);

        // 验证结果
        assertNotNull(result);
        assertEquals(expectedPrompt, result);
        verify(promptTemplateRepository).findByPromptTypeAndPromptName("DRG_ANALYSIS", templateName);
    }

    /**
     * 测试模板不存在时的异常处理
     */
    @Test
    void generateAnalysisPrompt_ShouldThrowException_WhenTemplateNotFound() {
        // 准备测试数据
        String templateName = "不存在的模板";
        
        // 设置Mock行为 - 返回null表示模板不存在
        when(promptTemplateRepository.findByPromptTypeAndPromptName("DRG_ANALYSIS", templateName))
                .thenReturn(null);

        // 执行测试 - 期望抛出异常
        assertThrows(DrgAiAnalysisService.TemplateNotFoundException.class, () -> {
            drgAiAnalysisService.generateAnalysisPrompt(templateName);
        });

        // 验证Repository被调用
        verify(promptTemplateRepository).findByPromptTypeAndPromptName("DRG_ANALYSIS", templateName);
    }

    /**
     * 测试模板读取的性能基准
     */
    @Test
    void generateAnalysisPrompt_ShouldCompleteWithinReasonableTime() {
        // 准备测试数据
        String templateName = "合并症或并发症分析";
        PromptTemplate mockTemplate = new PromptTemplate();
        mockTemplate.setPromptName(templateName);
        mockTemplate.setPrompt("测试模板内容");
        
        // 设置Mock行为
        when(promptTemplateRepository.findByPromptTypeAndPromptName("DRG_ANALYSIS", templateName))
                .thenReturn(mockTemplate);

        // 执行性能测试
        long startTime = System.currentTimeMillis();
        
        // 执行测试
        String result = drgAiAnalysisService.generateAnalysisPrompt(templateName);
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // 验证性能要求：执行时间应小于100毫秒
        assertTrue(executionTime < 100, "模板读取应在100毫秒内完成，实际耗时：" + executionTime + "毫秒");
        
        // 验证结果不为空
        assertNotNull(result);
    }

    /**
     * 测试空模板名称的处理
     */
    @Test
    void generateAnalysisPrompt_ShouldThrowException_WhenTemplateNameIsEmpty() {
        // 准备测试数据
        String templateName = "";
        
        // 执行测试 - 期望抛出IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            drgAiAnalysisService.generateAnalysisPrompt(templateName);
        });

        // 验证Repository未被调用，因为参数验证失败
        verify(promptTemplateRepository, never()).findByPromptTypeAndPromptName(anyString(), anyString());
    }

    /**
     * 测试null模板名称的处理
     */
    @Test
    void generateAnalysisPrompt_ShouldThrowException_WhenTemplateNameIsNull() {
        // 执行测试 - 期望抛出IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            drgAiAnalysisService.generateAnalysisPrompt(null);
        });

        // 验证Repository未被调用，因为参数验证失败
        verify(promptTemplateRepository, never()).findByPromptTypeAndPromptName(anyString(), anyString());
    }

    /**
     * 测试模板内容为空的处理
     */
    @Test
    void generateAnalysisPrompt_ShouldReturnEmptyString_WhenTemplateContentIsEmpty() {
        // 准备测试数据
        String templateName = "空内容模板";
        PromptTemplate mockTemplate = new PromptTemplate();
        mockTemplate.setPromptName(templateName);
        mockTemplate.setPrompt("");
        
        // 设置Mock行为
        when(promptTemplateRepository.findByPromptTypeAndPromptName("DRG_ANALYSIS", templateName))
                .thenReturn(mockTemplate);

        // 执行测试
        String result = drgAiAnalysisService.generateAnalysisPrompt(templateName);

        // 验证结果
        assertNotNull(result);
        assertEquals("", result);
        verify(promptTemplateRepository).findByPromptTypeAndPromptName("DRG_ANALYSIS", templateName);
    }

    // ========== 迭代2：变量替换测试用例 ==========

    /**
     * 测试模板变量替换的正确性
     */
    @Test
    void generateAnalysisPrompt_ShouldReplaceVariables_WhenTemplateContainsVariables() {
        // 准备测试数据
        String templateName = "患者基本信息分析";
        String templateContent = "患者姓名：{{patientName}}，年龄：{{age}}，诊断：{{diagnosis}}";
        String expectedPrompt = "患者姓名：张三，年龄：45，诊断：高血压";
        
        Map<String, String> variables = new HashMap<>();
        variables.put("patientName", "张三");
        variables.put("age", "45");
        variables.put("diagnosis", "高血压");
        
        PromptTemplate mockTemplate = new PromptTemplate();
        mockTemplate.setPromptName(templateName);
        mockTemplate.setPrompt(templateContent);
        
        // 设置Mock行为
        when(promptTemplateRepository.findByPromptTypeAndPromptName("DRG_ANALYSIS", templateName))
                .thenReturn(mockTemplate);

        // 执行测试 - 使用新的方法签名进行变量替换
        String result = drgAiAnalysisService.generateAnalysisPrompt(templateName, variables);

        // 验证结果 - 期望变量被正确替换
        assertNotNull(result);
        assertEquals(expectedPrompt, result, "变量应该被正确替换");
        verify(promptTemplateRepository).findByPromptTypeAndPromptName("DRG_ANALYSIS", templateName);
    }

    /**
     * 测试部分变量替换的情况
     */
    @Test
    void generateAnalysisPrompt_ShouldReplaceOnlyProvidedVariables_WhenPartialVariablesProvided() {
        // 准备测试数据
        String templateName = "患者基本信息分析";
        String templateContent = "患者姓名：{{patientName}}，年龄：{{age}}，诊断：{{diagnosis}}";
        String expectedPrompt = "患者姓名：张三，年龄：{{age}}，诊断：高血压";
        
        Map<String, String> variables = new HashMap<>();
        variables.put("patientName", "张三");
        variables.put("diagnosis", "高血压");
        // 注意：age变量没有提供，应该保持原样
        
        PromptTemplate mockTemplate = new PromptTemplate();
        mockTemplate.setPromptName(templateName);
        mockTemplate.setPrompt(templateContent);
        
        // 设置Mock行为
        when(promptTemplateRepository.findByPromptTypeAndPromptName("DRG_ANALYSIS", templateName))
                .thenReturn(mockTemplate);

        // 执行测试 - 使用部分变量进行替换
        String result = drgAiAnalysisService.generateAnalysisPrompt(templateName, variables);

        // 验证结果 - 只有提供的变量被替换，未提供的变量保持原样
        assertNotNull(result);
        assertEquals(expectedPrompt, result, "只有提供的变量应该被替换，未提供的变量应该保持原样");
        verify(promptTemplateRepository).findByPromptTypeAndPromptName("DRG_ANALYSIS", templateName);
    }

    /**
     * 测试变量缺失时的默认值处理
     */
    @Test
    void generateAnalysisPrompt_ShouldHandleMissingVariables_WhenVariablesNotProvided() {
        // 准备测试数据
        String templateName = "患者基本信息分析";
        String templateContent = "患者姓名：{{patientName}}，年龄：{{age}}，诊断：{{diagnosis}}";
        // 期望结果应该是变量被替换为默认值或保持原样
        String expectedPrompt = "患者姓名：未知，年龄：未知，诊断：未知";
        
        Map<String, String> variables = new HashMap<>();
        variables.put("patientName", "未知");
        variables.put("age", "未知");
        variables.put("diagnosis", "未知");
        
        PromptTemplate mockTemplate = new PromptTemplate();
        mockTemplate.setPromptName(templateName);
        mockTemplate.setPrompt(templateContent);
        
        // 设置Mock行为
        when(promptTemplateRepository.findByPromptTypeAndPromptName("DRG_ANALYSIS", templateName))
                .thenReturn(mockTemplate);

        // 执行测试 - 使用新的方法签名进行变量替换
        String result = drgAiAnalysisService.generateAnalysisPrompt(templateName, variables);

        // 验证结果 - 期望变量被替换为默认值
        assertNotNull(result);
        assertEquals(expectedPrompt, result, "变量应该被替换为默认值");
        verify(promptTemplateRepository).findByPromptTypeAndPromptName("DRG_ANALYSIS", templateName);
    }

    /**
     * 测试特殊字符的转义处理
     */
    @Test
    void generateAnalysisPrompt_ShouldEscapeSpecialCharacters_WhenTemplateContainsSpecialChars() {
        // 准备测试数据
        String templateName = "特殊字符测试";
        String templateContent = "诊断包含特殊字符：{{diagnosis}}，注意：{{note}}";
        String expectedPrompt = "诊断包含特殊字符：高血压(高危)，注意：需要密切监测";
        
        Map<String, String> variables = new HashMap<>();
        variables.put("diagnosis", "高血压(高危)");
        variables.put("note", "需要密切监测");
        
        PromptTemplate mockTemplate = new PromptTemplate();
        mockTemplate.setPromptName(templateName);
        mockTemplate.setPrompt(templateContent);
        
        // 设置Mock行为
        when(promptTemplateRepository.findByPromptTypeAndPromptName("DRG_ANALYSIS", templateName))
                .thenReturn(mockTemplate);

        // 执行测试 - 使用新的方法签名进行变量替换
        String result = drgAiAnalysisService.generateAnalysisPrompt(templateName, variables);

        // 验证结果 - 期望特殊字符被正确处理
        assertNotNull(result);
        assertEquals(expectedPrompt, result, "特殊字符应该被正确处理");
        verify(promptTemplateRepository).findByPromptTypeAndPromptName("DRG_ANALYSIS", templateName);
    }

    /**
     * 测试空变量映射的处理
     */
    @Test
    void generateAnalysisPrompt_ShouldReturnOriginalTemplate_WhenVariablesMapIsEmpty() {
        // 准备测试数据
        String templateName = "患者基本信息分析";
        String templateContent = "患者姓名：{{patientName}}，年龄：{{age}}，诊断：{{diagnosis}}";
        
        Map<String, String> variables = new HashMap<>();
        
        PromptTemplate mockTemplate = new PromptTemplate();
        mockTemplate.setPromptName(templateName);
        mockTemplate.setPrompt(templateContent);
        
        // 设置Mock行为
        when(promptTemplateRepository.findByPromptTypeAndPromptName("DRG_ANALYSIS", templateName))
                .thenReturn(mockTemplate);

        // 执行测试 - 使用空变量映射
        String result = drgAiAnalysisService.generateAnalysisPrompt(templateName, variables);

        // 验证结果 - 应该返回原始模板内容
        assertNotNull(result);
        assertEquals(templateContent, result, "空变量映射应该返回原始模板内容");
        verify(promptTemplateRepository).findByPromptTypeAndPromptName("DRG_ANALYSIS", templateName);
    }

    /**
     * 测试null变量映射的处理
     */
    @Test
    void generateAnalysisPrompt_ShouldReturnOriginalTemplate_WhenVariablesMapIsNull() {
        // 准备测试数据
        String templateName = "患者基本信息分析";
        String templateContent = "患者姓名：{{patientName}}，年龄：{{age}}，诊断：{{diagnosis}}";
        
        PromptTemplate mockTemplate = new PromptTemplate();
        mockTemplate.setPromptName(templateName);
        mockTemplate.setPrompt(templateContent);
        
        // 设置Mock行为
        when(promptTemplateRepository.findByPromptTypeAndPromptName("DRG_ANALYSIS", templateName))
                .thenReturn(mockTemplate);

        // 执行测试 - 使用null变量映射
        String result = drgAiAnalysisService.generateAnalysisPrompt(templateName, null);

        // 验证结果 - 应该返回原始模板内容
        assertNotNull(result);
        assertEquals(templateContent, result, "null变量映射应该返回原始模板内容");
        verify(promptTemplateRepository).findByPromptTypeAndPromptName("DRG_ANALYSIS", templateName);
    }
}
