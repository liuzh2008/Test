package com.example.medaiassistant.service;

import com.example.medaiassistant.config.PromptServiceConfig;
import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.repository.PromptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * PromptSubmissionService单元测试
 * 
 * 测试覆盖：
 * 1. 单个Prompt提交
 * 2. 批量Prompt提交
 * 3. 状态验证逻辑
 * 4. 错误处理
 * 5. 配置信息获取
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2025-09-30
 */
@ExtendWith(MockitoExtension.class)
class PromptSubmissionServiceTest {

    @Mock
    private PromptRepository promptRepository;

    @Mock
    private PromptServiceConfig promptServiceConfig;

    @Mock
    private PromptServiceConfig.SubmissionConfig submissionConfig;

    @InjectMocks
    private PromptSubmissionService promptSubmissionService;

    private Prompt validPrompt;
    private Prompt invalidPrompt;

    @BeforeEach
    void setUp() {
        // 创建有效的Prompt（状态为"待处理"）
        validPrompt = new Prompt();
        validPrompt.setPromptId(1);
        validPrompt.setStatusName("待处理");
        validPrompt.setSubmissionTime(null);
        validPrompt.setProcessingStartTime(null);
        validPrompt.setRetryCount(0);

        // 创建无效的Prompt（状态为"已提交"）
        invalidPrompt = new Prompt();
        invalidPrompt.setPromptId(2);
        invalidPrompt.setStatusName("已提交");
        invalidPrompt.setSubmissionTime(LocalDateTime.now());
        invalidPrompt.setProcessingStartTime(LocalDateTime.now());
        invalidPrompt.setRetryCount(0);
    }

    @Test
    void testSubmitPrompt_Success() {
        // 准备
        when(promptRepository.findById(1)).thenReturn(Optional.of(validPrompt));
        when(promptRepository.save(any(Prompt.class))).thenReturn(validPrompt);

        // 执行
        boolean result = promptSubmissionService.submitPrompt(1);

        // 验证
        assertTrue(result);
        verify(promptRepository).findById(1);
        verify(promptRepository).save(any(Prompt.class));
    }

    @Test
    void testSubmitPrompt_PromptNotFound() {
        // 准备
        when(promptRepository.findById(999)).thenReturn(Optional.empty());

        // 执行
        boolean result = promptSubmissionService.submitPrompt(999);

        // 验证
        assertFalse(result);
        verify(promptRepository).findById(999);
        verify(promptRepository, never()).save(any(Prompt.class));
    }

    @Test
    void testSubmitPrompt_InvalidState() {
        // 准备
        when(promptRepository.findById(2)).thenReturn(Optional.of(invalidPrompt));

        // 执行
        boolean result = promptSubmissionService.submitPrompt(2);

        // 验证
        assertFalse(result);
        verify(promptRepository).findById(2);
        verify(promptRepository, never()).save(any(Prompt.class));
    }

    @Test
    void testBatchSubmitPrompts_Success() {
        // 准备
        List<Integer> promptIds = Arrays.asList(1, 3, 5);
        
        Prompt prompt1 = new Prompt();
        prompt1.setPromptId(1);
        prompt1.setStatusName("待处理");
        
        Prompt prompt3 = new Prompt();
        prompt3.setPromptId(3);
        prompt3.setStatusName("待处理");
        
        Prompt prompt5 = new Prompt();
        prompt5.setPromptId(5);
        prompt5.setStatusName("待处理");

        when(promptRepository.findById(1)).thenReturn(Optional.of(prompt1));
        when(promptRepository.findById(3)).thenReturn(Optional.of(prompt3));
        when(promptRepository.findById(5)).thenReturn(Optional.of(prompt5));
        when(promptRepository.save(any(Prompt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 执行
        int successCount = promptSubmissionService.batchSubmitPrompts(promptIds);

        // 验证
        assertEquals(3, successCount);
        verify(promptRepository, times(3)).findById(anyInt());
        verify(promptRepository, times(3)).save(any(Prompt.class));
    }

    @Test
    void testBatchSubmitPrompts_PartialSuccess() {
        // 准备
        List<Integer> promptIds = Arrays.asList(1, 2, 3);
        
        Prompt prompt1 = new Prompt();
        prompt1.setPromptId(1);
        prompt1.setStatusName("待处理");
        
        Prompt prompt2 = new Prompt();
        prompt2.setPromptId(2);
        prompt2.setStatusName("已提交"); // 无效状态
        
        Prompt prompt3 = new Prompt();
        prompt3.setPromptId(3);
        prompt3.setStatusName("待处理");

        when(promptRepository.findById(1)).thenReturn(Optional.of(prompt1));
        when(promptRepository.findById(2)).thenReturn(Optional.of(prompt2));
        when(promptRepository.findById(3)).thenReturn(Optional.of(prompt3));
        when(promptRepository.save(any(Prompt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 执行
        int successCount = promptSubmissionService.batchSubmitPrompts(promptIds);

        // 验证
        assertEquals(2, successCount);
        verify(promptRepository, times(3)).findById(anyInt());
        verify(promptRepository, times(2)).save(any(Prompt.class));
    }

    @Test
    void testBatchSubmitPrompts_AllFailed() {
        // 准备
        List<Integer> promptIds = Arrays.asList(2, 4, 6);
        
        Prompt prompt2 = new Prompt();
        prompt2.setPromptId(2);
        prompt2.setStatusName("已提交");
        
        Prompt prompt4 = new Prompt();
        prompt4.setPromptId(4);
        prompt4.setStatusName("已完成");
        
        Prompt prompt6 = new Prompt();
        prompt6.setPromptId(6);
        prompt6.setStatusName("已提交");

        when(promptRepository.findById(2)).thenReturn(Optional.of(prompt2));
        when(promptRepository.findById(4)).thenReturn(Optional.of(prompt4));
        when(promptRepository.findById(6)).thenReturn(Optional.of(prompt6));

        // 执行
        int successCount = promptSubmissionService.batchSubmitPrompts(promptIds);

        // 验证
        assertEquals(0, successCount);
        verify(promptRepository, times(3)).findById(anyInt());
        verify(promptRepository, never()).save(any(Prompt.class));
    }

    @Test
    void testGetSubmissionConfigInfo() {
        // 准备
        when(promptServiceConfig.getSubmission()).thenReturn(submissionConfig);
        when(submissionConfig.getMaxRetries()).thenReturn(3);
        when(submissionConfig.getRetryInterval()).thenReturn(1000L);

        // 执行
        String configInfo = promptSubmissionService.getSubmissionConfigInfo();

        // 验证
        assertNotNull(configInfo);
        assertTrue(configInfo.contains("提交服务配置"));
        assertTrue(configInfo.contains("最大重试次数: 3"));
        assertTrue(configInfo.contains("重试间隔: 1000ms"));
    }

    @Test
    void testHealthCheck_Success() {
        // 准备
        when(promptRepository.count()).thenReturn(100L);

        // 执行
        String healthStatus = promptSubmissionService.healthCheck();

        // 验证
        assertNotNull(healthStatus);
        assertTrue(healthStatus.contains("提交服务健康状态"));
        assertTrue(healthStatus.contains("数据库连接: 正常"));
        assertTrue(healthStatus.contains("总Prompt数: 100"));
    }

    @Test
    void testHealthCheck_Exception() {
        // 准备
        when(promptRepository.count()).thenThrow(new RuntimeException("Database connection failed"));

        // 执行
        String healthStatus = promptSubmissionService.healthCheck();

        // 验证
        assertNotNull(healthStatus);
        assertTrue(healthStatus.contains("异常"));
        assertTrue(healthStatus.contains("Database connection failed"));
    }

    @Test
    void testGetPendingSubmissionCount() {
        // 准备
        when(promptRepository.countByStatusName("待处理")).thenReturn(25L);

        // 执行
        long count = promptSubmissionService.getPendingSubmissionCount();

        // 验证
        assertEquals(25L, count);
        verify(promptRepository).countByStatusName("待处理");
    }

    @Test
    void testGetPendingSubmissionCount_Exception() {
        // 准备
        when(promptRepository.countByStatusName("待处理")).thenThrow(new RuntimeException("Query failed"));

        // 执行
        long count = promptSubmissionService.getPendingSubmissionCount();

        // 验证
        assertEquals(0L, count);
        verify(promptRepository).countByStatusName("待处理");
    }

    @Test
    void testGetSubmissionStats() {
        // 准备
        when(promptRepository.countByStatusName("待处理")).thenReturn(15L);
        when(promptRepository.countByStatusName("已提交")).thenReturn(35L);

        // 执行
        String stats = promptSubmissionService.getSubmissionStats();

        // 验证
        assertNotNull(stats);
        assertTrue(stats.contains("提交"));
        assertTrue(stats.contains("15"));
        assertTrue(stats.contains("35"));
    }

    @Test
    void testIsValidStateForSubmission() {
        // 测试有效状态
        assertTrue(promptSubmissionService.isValidStateForSubmission("待处理"));
        
        // 测试无效状态
        assertFalse(promptSubmissionService.isValidStateForSubmission("已提交"));
        assertFalse(promptSubmissionService.isValidStateForSubmission("已完成"));
        assertFalse(promptSubmissionService.isValidStateForSubmission("执行失败"));
        assertFalse(promptSubmissionService.isValidStateForSubmission(null));
        assertFalse(promptSubmissionService.isValidStateForSubmission(""));
    }
}
