package com.example.medaiassistant.service;

import com.example.medaiassistant.config.RetryPolicyConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 重试策略服务测试类
 * 
 * 测试RetryPolicyService的各种功能，包括：
 * 1. 指数退避重试策略
 * 2. 状态码检查
 * 3. 异常重试
 * 4. 最大重试次数限制
 * 
 * @since 2025-09-28
 * @author Cline
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class RetryPolicyServiceTest {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryPolicyServiceTest.class);
    
    @Mock
    private RetryPolicyConfig retryPolicyConfig;
    
    private RetryPolicyService retryPolicyService;
    
    /**
     * 测试前设置
     * 
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @BeforeEach
    void setUp() {
        // 配置重试策略 - 使用lenient避免不必要的stubbing警告
        lenient().when(retryPolicyConfig.getMaxRetries()).thenReturn(3);
        lenient().when(retryPolicyConfig.getInitialIntervalMs()).thenReturn(1000L);
        lenient().when(retryPolicyConfig.getMultiplier()).thenReturn(2.0);
        lenient().when(retryPolicyConfig.getMaxIntervalMs()).thenReturn(30000L);
        lenient().when(retryPolicyConfig.getRetryableStatusCodes()).thenReturn(Set.of(429, 500, 502, 503, 504));
        
        retryPolicyService = new RetryPolicyService(retryPolicyConfig);
    }
    
    /**
     * 测试成功执行无重试
     * 
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Test
    void testExecuteWithRetry_Success() throws Exception {
        // 准备
        String expectedResult = "success";
        RetryPolicyService.RetryableOperation<String> operation = () -> expectedResult;
        
        // 执行
        String result = retryPolicyService.executeWithRetry(operation);
        
        // 验证
        assertEquals(expectedResult, result);
    }
    
    /**
     * 测试SocketTimeoutException重试成功
     * 
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Test
    void testExecuteWithRetry_SocketTimeoutException_RetrySuccess() throws Exception {
        // 准备
        String expectedResult = "success";
        @SuppressWarnings("unchecked")
        RetryPolicyService.RetryableOperation<String> operation = mock(RetryPolicyService.RetryableOperation.class);
        
        // 模拟前两次失败，第三次成功
        when(operation.execute())
            .thenThrow(new SocketTimeoutException("Connection timeout"))
            .thenThrow(new SocketTimeoutException("Connection timeout"))
            .thenReturn(expectedResult);
        
        // 执行
        String result = retryPolicyService.executeWithRetry(operation);
        
        // 验证
        assertEquals(expectedResult, result);
        verify(operation, times(3)).execute();
    }
    
    /**
     * 测试IOException重试成功
     * 
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Test
    void testExecuteWithRetry_IOException_RetrySuccess() throws Exception {
        // 准备
        String expectedResult = "success";
        @SuppressWarnings("unchecked")
        RetryPolicyService.RetryableOperation<String> operation = mock(RetryPolicyService.RetryableOperation.class);
        
        // 模拟前一次失败，第二次成功
        when(operation.execute())
            .thenThrow(new IOException("Network error"))
            .thenReturn(expectedResult);
        
        // 执行
        String result = retryPolicyService.executeWithRetry(operation);
        
        // 验证
        assertEquals(expectedResult, result);
        verify(operation, times(2)).execute();
    }
    
    /**
     * 测试达到最大重试次数后抛出异常
     * 
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Test
    void testExecuteWithRetry_MaxRetriesExceeded() throws Exception {
        // 准备
        SocketTimeoutException expectedException = new SocketTimeoutException("Connection timeout");
        @SuppressWarnings("unchecked")
        RetryPolicyService.RetryableOperation<String> operation = mock(RetryPolicyService.RetryableOperation.class);
        
        // 模拟所有重试都失败
        when(operation.execute()).thenThrow(expectedException);
        
        // 执行和验证
        SocketTimeoutException thrownException = assertThrows(
            SocketTimeoutException.class,
            () -> retryPolicyService.executeWithRetry(operation)
        );
        
        // 验证
        assertEquals(expectedException, thrownException);
        verify(operation, times(4)).execute(); // 初始执行 + 3次重试
    }
    
    /**
     * 测试不可重试的异常直接抛出
     * 
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Test
    void testExecuteWithRetry_NonRetryableException() throws Exception {
        // 准备
        IllegalArgumentException expectedException = new IllegalArgumentException("Invalid argument");
        @SuppressWarnings("unchecked")
        RetryPolicyService.RetryableOperation<String> operation = mock(RetryPolicyService.RetryableOperation.class);
        
        // 模拟不可重试的异常
        when(operation.execute()).thenThrow(expectedException);
        
        // 执行和验证
        IllegalArgumentException thrownException = assertThrows(
            IllegalArgumentException.class,
            () -> retryPolicyService.executeWithRetry(operation)
        );
        
        // 验证
        assertEquals(expectedException, thrownException);
        verify(operation, times(1)).execute(); // 只执行一次，不重试
    }
    
    /**
     * 测试状态码检查功能
     * 
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Test
    void testIsRetryableStatusCode() {
        // 测试可重试状态码
        assertTrue(retryPolicyService.isRetryableStatusCode(429));
        assertTrue(retryPolicyService.isRetryableStatusCode(500));
        assertTrue(retryPolicyService.isRetryableStatusCode(502));
        assertTrue(retryPolicyService.isRetryableStatusCode(503));
        assertTrue(retryPolicyService.isRetryableStatusCode(504));
        
        // 测试不可重试状态码
        assertFalse(retryPolicyService.isRetryableStatusCode(200));
        assertFalse(retryPolicyService.isRetryableStatusCode(400));
        assertFalse(retryPolicyService.isRetryableStatusCode(404));
    }
    
    /**
     * 测试状态码重试功能
     * 
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Test
    void testExecuteWithRetry_StatusCodeRetry() throws Exception {
        // 准备
        String expectedResult = "success";
        @SuppressWarnings("unchecked")
        RetryPolicyService.RetryableOperation<String> operation = mock(RetryPolicyService.RetryableOperation.class);
        RetryPolicyService.StatusCodeChecker statusCodeChecker = mock(RetryPolicyService.StatusCodeChecker.class);
        
        // 模拟状态码检查 - 前两次返回true（需要重试），第三次返回false（不需要重试）
        when(statusCodeChecker.getStatusCode()).thenReturn(500);
        when(statusCodeChecker.shouldRetry(500))
            .thenReturn(true)  // 第一次需要重试
            .thenReturn(true)  // 第二次需要重试
            .thenReturn(false); // 第三次不需要重试
        
        // 模拟前两次返回可重试状态码，第三次成功
        when(operation.execute())
            .thenReturn("result1")
            .thenReturn("result2")
            .thenReturn(expectedResult);
        
        // 执行
        String result = retryPolicyService.executeWithRetry(operation, statusCodeChecker);
        
        // 验证
        assertEquals(expectedResult, result);
        verify(operation, times(3)).execute();
        verify(statusCodeChecker, times(3)).shouldRetry(500);
    }
    
    /**
     * 测试等待时间计算
     * 
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Test
    void testCalculateWaitTime() throws Exception {
        // 使用反射访问私有方法
        java.lang.reflect.Method calculateWaitTimeMethod = RetryPolicyService.class.getDeclaredMethod("calculateWaitTime", int.class);
        calculateWaitTimeMethod.setAccessible(true);
        
        // 测试不同重试次数的等待时间
        long waitTime0 = (long) calculateWaitTimeMethod.invoke(retryPolicyService, 0);
        long waitTime1 = (long) calculateWaitTimeMethod.invoke(retryPolicyService, 1);
        long waitTime2 = (long) calculateWaitTimeMethod.invoke(retryPolicyService, 2);
        
        // 验证指数退避算法：1000ms, 2000ms, 4000ms
        assertEquals(1000L, waitTime0);
        assertEquals(2000L, waitTime1);
        assertEquals(4000L, waitTime2);
        
        logger.info("重试等待时间测试: 第0次={}ms, 第1次={}ms, 第2次={}ms", waitTime0, waitTime1, waitTime2);
    }
    
    /**
     * 测试重试策略配置获取
     * 
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Test
    void testGetRetryPolicyConfig() {
        // 执行
        RetryPolicyConfig config = retryPolicyService.getRetryPolicyConfig();
        
        // 验证
        assertNotNull(config);
        assertEquals(retryPolicyConfig, config);
    }
    
    /**
     * 测试线程中断处理
     * 
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Test
    void testExecuteWithRetry_ThreadInterrupted() throws Exception {
        // 准备
        @SuppressWarnings("unchecked")
        RetryPolicyService.RetryableOperation<String> operation = mock(RetryPolicyService.RetryableOperation.class);
        
        // 模拟第一次失败，第二次被中断
        when(operation.execute())
            .thenThrow(new SocketTimeoutException("Connection timeout"))
            .thenAnswer(invocation -> {
                Thread.currentThread().interrupt();
                throw new SocketTimeoutException("Connection timeout");
            });
        
        // 执行和验证
        assertThrows(
            SocketTimeoutException.class,
            () -> retryPolicyService.executeWithRetry(operation)
        );
        
        // 验证操作被正确调用 - 由于线程中断，重试逻辑会继续执行直到达到最大重试次数
        verify(operation, atLeast(2)).execute();
    }
}
