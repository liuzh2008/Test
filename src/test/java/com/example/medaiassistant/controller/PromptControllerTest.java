package com.example.medaiassistant.controller;

import com.example.medaiassistant.service.PromptService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * PromptController测试类
 * 按照TDD红-绿-重构流程实现
 */
@ExtendWith(MockitoExtension.class)
public class PromptControllerTest {

    @Mock
    private PromptService promptService;

    @Mock
    private com.example.medaiassistant.service.TimerPromptGenerator timerPromptGenerator;

    @Mock
    private com.example.medaiassistant.service.PromptPollingService promptPollingService;

    @InjectMocks
    private PromptController promptController;

    /**
     * 测试获取Prompts记录数量API
     * 红阶段：期望测试失败，因为还没有实现该API
     */
    @Test
    void testGetPromptCounts() {
        // 模拟PromptService返回未提交和已提交的记录数量
        when(promptService.countPendingPrompts()).thenReturn(5L);
        when(promptService.countSubmittedPrompts()).thenReturn(10L);

        // 调用控制器方法
        Map<String, Object> result = promptController.getPromptCounts();

        // 验证结果
        assertEquals("UP", result.get("status"));
        assertEquals(5L, result.get("pendingCount"));
        assertEquals(10L, result.get("submittedCount"));
        assertNotNull(result.get("timestamp"));
    }
    
    /**
     * 测试获取Prompts记录数量API处理异常情况
     * 绿阶段：测试异常处理逻辑
     */
    @Test
    void testGetPromptCountsWithException() {
        // 模拟PromptService抛出异常
        when(promptService.countPendingPrompts()).thenThrow(new RuntimeException("数据库连接失败"));

        // 调用控制器方法
        Map<String, Object> result = promptController.getPromptCounts();

        // 验证结果
        assertEquals("DOWN", result.get("status"));
        assertNotNull(result.get("errorMessage"));
        assertNotNull(result.get("timestamp"));
    }
}