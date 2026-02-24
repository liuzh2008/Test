package com.example.medaiassistant.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import reactor.util.retry.RetryBackoffSpec;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.springframework.web.client.ResourceAccessException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RetryUtil单元测试类
 * 
 * 测试重试工具类的各种功能，包括：
 * - 可重试异常判断
 * - 重试规范创建
 * - 延迟时间计算
 * - 5xx服务端错误检测
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-14
 */
class RetryUtilTest {

    @Test
    @DisplayName("测试可重试异常判断 - UnknownHostException")
    void isRetryableException_ShouldReturnTrue_ForUnknownHostException() {
        // 准备
        UnknownHostException exception = new UnknownHostException("api.deepseek.com");

        // 执行
        boolean result = RetryUtil.isRetryableException(exception);

        // 验证
        assertTrue(result, "UnknownHostException应该是可重试的");
    }

    @Test
    @DisplayName("测试可重试异常判断 - ConnectException")
    void isRetryableException_ShouldReturnTrue_ForConnectException() {
        // 准备
        ConnectException exception = new ConnectException("Connection refused");

        // 执行
        boolean result = RetryUtil.isRetryableException(exception);

        // 验证
        assertTrue(result, "ConnectException应该是可重试的");
    }

    @Test
    @DisplayName("测试可重试异常判断 - SocketTimeoutException")
    void isRetryableException_ShouldReturnTrue_ForSocketTimeoutException() {
        // 准备
        SocketTimeoutException exception = new SocketTimeoutException("Read timed out");

        // 执行
        boolean result = RetryUtil.isRetryableException(exception);

        // 验证
        assertTrue(result, "SocketTimeoutException应该是可重试的");
    }

    @Test
    @DisplayName("测试可重试异常判断 - TimeoutException")
    void isRetryableException_ShouldReturnTrue_ForTimeoutException() {
        // 准备
        TimeoutException exception = new TimeoutException("Operation timed out");

        // 执行
        boolean result = RetryUtil.isRetryableException(exception);

        // 验证
        assertTrue(result, "TimeoutException应该是可重试的");
    }

    @Test
    @DisplayName("测试可重试异常判断 - ResourceAccessException")
    void isRetryableException_ShouldReturnTrue_ForResourceAccessException() {
        // 准备
        ResourceAccessException exception = new ResourceAccessException("I/O error");

        // 执行
        boolean result = RetryUtil.isRetryableException(exception);

        // 验证
        assertTrue(result, "ResourceAccessException应该是可重试的");
    }

    @Test
    @DisplayName("测试可重试异常判断 - 异常链中的可重试异常")
    void isRetryableException_ShouldReturnTrue_ForRetryableExceptionInChain() {
        // 准备
        UnknownHostException cause = new UnknownHostException("api.deepseek.com");
        RuntimeException wrapper = new RuntimeException("Wrapper exception", cause);

        // 执行
        boolean result = RetryUtil.isRetryableException(wrapper);

        // 验证
        assertTrue(result, "异常链中的UnknownHostException应该是可重试的");
    }

    @Test
    @DisplayName("测试可重试异常判断 - 5xx服务端错误")
    void isRetryableException_ShouldReturnTrue_For5xxServerError() {
        // 准备
        RuntimeException exception = new RuntimeException("HTTP 500 Internal Server Error");

        // 执行
        boolean result = RetryUtil.isRetryableException(exception);

        // 验证
        assertTrue(result, "5xx服务端错误应该是可重试的");
    }

    @Test
    @DisplayName("测试可重试异常判断 - 502 Bad Gateway")
    void isRetryableException_ShouldReturnTrue_For502BadGateway() {
        // 准备
        RuntimeException exception = new RuntimeException("502 Bad Gateway");

        // 执行
        boolean result = RetryUtil.isRetryableException(exception);

        // 验证
        assertTrue(result, "502 Bad Gateway应该是可重试的");
    }

    @Test
    @DisplayName("测试可重试异常判断 - 503 Service Unavailable")
    void isRetryableException_ShouldReturnTrue_For503ServiceUnavailable() {
        // 准备
        RuntimeException exception = new RuntimeException("503 Service Unavailable");

        // 执行
        boolean result = RetryUtil.isRetryableException(exception);

        // 验证
        assertTrue(result, "503 Service Unavailable应该是可重试的");
    }

    @Test
    @DisplayName("测试可重试异常判断 - 不可重试异常")
    void isRetryableException_ShouldReturnFalse_ForNonRetryableException() {
        // 准备
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

        // 执行
        boolean result = RetryUtil.isRetryableException(exception);

        // 验证
        assertFalse(result, "IllegalArgumentException应该是不可重试的");
    }

    @Test
    @DisplayName("测试可重试异常判断 - null异常")
    void isRetryableException_ShouldReturnFalse_ForNullException() {
        // 执行
        boolean result = RetryUtil.isRetryableException(null);

        // 验证
        assertFalse(result, "null异常应该是不可重试的");
    }

    @Test
    @DisplayName("测试重试规范创建")
    void createAIRetrySpec_ShouldReturnConfiguredRetrySpec() {
        // 执行
        RetryBackoffSpec retrySpec = RetryUtil.createAIRetrySpec();

        // 验证
        assertNotNull(retrySpec, "重试规范不应该为null");
        // 这里我们主要验证方法能够正常执行，不抛出异常
    }

    @Test
    @DisplayName("测试延迟时间计算 - 第一次重试")
    void calculateRetryDelay_ShouldReturnValidDelay_ForFirstRetry() {
        // 执行
        long delay = RetryUtil.calculateRetryDelay(0);

        // 验证
        assertTrue(delay >= 500 && delay <= 1500, 
                "第一次重试延迟应该在500-1500ms范围内，实际值: " + delay);
    }

    @Test
    @DisplayName("测试延迟时间计算 - 第二次重试")
    void calculateRetryDelay_ShouldReturnValidDelay_ForSecondRetry() {
        // 执行
        long delay = RetryUtil.calculateRetryDelay(1);

        // 验证
        assertTrue(delay >= 1000 && delay <= 3000, 
                "第二次重试延迟应该在1000-3000ms范围内，实际值: " + delay);
    }

    @Test
    @DisplayName("测试延迟时间计算 - 第三次重试")
    void calculateRetryDelay_ShouldReturnValidDelay_ForThirdRetry() {
        // 执行
        long delay = RetryUtil.calculateRetryDelay(2);

        // 验证
        assertTrue(delay >= 2000 && delay <= 6000, 
                "第三次重试延迟应该在2000-6000ms范围内，实际值: " + delay);
    }

    @Test
    @DisplayName("测试延迟时间计算 - 边界值检查")
    void calculateRetryDelay_ShouldRespectBoundaries() {
        // 执行
        long delay = RetryUtil.calculateRetryDelay(10); // 非常大的重试次数

        // 验证
        assertTrue(delay <= 30000, 
                "延迟时间不应该超过最大退避时间30秒，实际值: " + delay);
        assertTrue(delay >= 1000, 
                "延迟时间不应该小于初始退避时间1秒，实际值: " + delay);
    }

    @Test
    @DisplayName("测试配置常量获取")
    void getConfigurationConstants_ShouldReturnCorrectValues() {
        // 验证
        assertEquals(3, RetryUtil.getMaxRetries(), "最大重试次数应该是3");
        assertEquals(Duration.ofSeconds(1), RetryUtil.getInitialBackoff(), "初始退避时间应该是1秒");
        assertEquals(Duration.ofSeconds(30), RetryUtil.getMaxBackoff(), "最大退避时间应该是30秒");
        assertEquals(0.5, RetryUtil.getJitterFactor(), 0.001, "随机抖动因子应该是0.5");
    }

    @Test
    @DisplayName("测试5xx服务端错误检测 - 各种5xx错误")
    void is5xxServerError_ShouldReturnTrue_ForVarious5xxErrors() {
        // 准备各种5xx错误消息
        String[] errorMessages = {
            "HTTP 500 Internal Server Error",
            "502 Bad Gateway",
            "503 Service Unavailable",
            "504 Gateway Timeout",
            "internal server error occurred",
            "service unavailable at this time",
            "bad gateway error",
            "gateway timeout"
        };

        // 验证每个错误消息
        for (String errorMessage : errorMessages) {
            RuntimeException exception = new RuntimeException(errorMessage);
            assertTrue(RetryUtil.isRetryableException(exception), 
                    "应该检测到5xx错误: " + errorMessage);
        }
    }

    @Test
    @DisplayName("测试5xx服务端错误检测 - 非5xx错误")
    void is5xxServerError_ShouldReturnFalse_ForNon5xxErrors() {
        // 准备非5xx错误消息
        String[] errorMessages = {
            "HTTP 400 Bad Request",
            "401 Unauthorized",
            "403 Forbidden",
            "404 Not Found",
            "429 Too Many Requests",
            "Client error",
            "Validation error"
        };

        // 验证每个错误消息
        for (String errorMessage : errorMessages) {
            RuntimeException exception = new RuntimeException(errorMessage);
            assertFalse(RetryUtil.isRetryableException(exception), 
                    "不应该检测为5xx错误: " + errorMessage);
        }
    }
}
