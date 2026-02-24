package com.example.medaiassistant.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OptimisticLockRetryService单元测试类
 * 测试乐观锁重试服务的各种场景
 * 
 * @author MedAI Assistant Team
 * @version 1.0
 * @since 2025-09-29
 */
@ExtendWith(MockitoExtension.class)
class OptimisticLockRetryServiceTest {

    @InjectMocks
    private OptimisticLockRetryService optimisticLockRetryService;

    /**
     * 测试重试机制在乐观锁冲突时的效果
     */
    @Test
    void testExecuteWithRetry_WhenOptimisticLockConflict_ShouldRetry() throws Exception {
        // 模拟乐观锁冲突场景
        int[] attemptCount = {0};
        
        String result = optimisticLockRetryService.executeWithOptimisticLockRetry(
            () -> {
                attemptCount[0]++;
                if (attemptCount[0] == 1) {
                    // 第一次尝试抛出乐观锁冲突异常
                    throw new OptimisticLockingFailureException("模拟乐观锁冲突");
                }
                // 第二次尝试成功
                return "重试成功";
            },
            null,
            "乐观锁冲突测试"
        );

        // 验证重试机制生效
        assertEquals(2, attemptCount[0], "应该进行了2次尝试（第一次失败，第二次成功）");
        assertEquals("重试成功", result, "应该返回成功结果");
    }

    /**
     * 测试重试机制在无冲突时的正常执行
     */
    @Test
    void testExecuteWithRetry_WhenNoConflict_ShouldExecuteOnce() throws Exception {
        // 模拟无冲突场景
        int[] attemptCount = {0};
        
        String result = optimisticLockRetryService.executeWithOptimisticLockRetry(
            () -> {
                attemptCount[0]++;
                return "执行成功";
            },
            null,
            "无冲突测试"
        );

        // 验证只执行了一次
        assertEquals(1, attemptCount[0], "应该只执行了一次");
        assertEquals("执行成功", result, "应该返回成功结果");
    }

    /**
     * 测试重试机制在达到最大重试次数时的行为
     */
    @Test
    void testExecuteWithRetry_WhenMaxRetriesExceeded_ShouldThrowException() {
        // 模拟持续乐观锁冲突，超过最大重试次数
        assertThrows(OptimisticLockingFailureException.class, () -> {
            optimisticLockRetryService.executeWithOptimisticLockRetry(
                () -> {
                    // 每次都抛出乐观锁冲突异常
                    throw new OptimisticLockingFailureException("持续乐观锁冲突");
                },
                null,
                "最大重试测试"
            );
        });
    }

    /**
     * 测试无返回值的重试操作
     */
    @Test
    void testExecuteWithRetryVoid_WhenOptimisticLockConflict_ShouldRetry() throws Exception {
        // 模拟乐观锁冲突场景
        int[] attemptCount = {0};
        
        optimisticLockRetryService.executeWithOptimisticLockRetry(
            () -> {
                attemptCount[0]++;
                if (attemptCount[0] == 1) {
                    // 第一次尝试抛出乐观锁冲突异常
                    throw new OptimisticLockingFailureException("模拟乐观锁冲突");
                }
                // 第二次尝试成功，无返回值
                return null;
            },
            null,
            "无返回值乐观锁冲突测试"
        );

        // 验证重试机制生效
        assertEquals(2, attemptCount[0], "应该进行了2次尝试（第一次失败，第二次成功）");
    }

    /**
     * 测试非乐观锁异常的处理
     */
    @Test
    void testExecuteWithRetry_WhenNonOptimisticLockException_ShouldNotRetry() {
        // 模拟非乐观锁异常
        RuntimeException expectedException = new RuntimeException("非乐观锁异常");
        
        RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
            optimisticLockRetryService.executeWithOptimisticLockRetry(
                () -> {
                    throw expectedException;
                },
                null,
                "非乐观锁异常测试"
            );
        });

        // 验证抛出了正确的异常
        assertEquals(expectedException, thrownException, "应该抛出原始的非乐观锁异常");
    }

    /**
     * 测试重试机制配置
     * 
     * 注意：此测试已禁用，因为OptimisticLockRetryService使用编程式重试（while循环）
     * 而非@Retryable注解方式。实际的重试逻理在executeWithOptimisticLockRetry方法中实现。
     */
    @Test
    @Disabled("系统使用编程式重试而非@Retryable注解")
    void testRetryConfiguration() {
        // 验证重试服务正确配置了重试注解
        var method = OptimisticLockRetryService.class.getDeclaredMethods();
        boolean hasRetryableAnnotation = false;
        
        for (var m : method) {
            if (m.getName().equals("executeWithOptimisticLockRetry") && 
                m.isAnnotationPresent(org.springframework.retry.annotation.Retryable.class)) {
                hasRetryableAnnotation = true;
                break;
            }
        }
        
        assertTrue(hasRetryableAnnotation, "executeWithOptimisticLockRetry方法应该配置了@Retryable注解");
    }
}
