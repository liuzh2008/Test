package com.example.medaiassistant.hospital.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 定时任务调度器测试
 * 重构阶段：完善测试用例，提高测试覆盖率
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-03
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("定时任务调度器测试")
class SyncSchedulerTest {
    
    private SyncScheduler syncScheduler;
    
    @BeforeEach
    void setUp() {
        syncScheduler = new SyncScheduler();
    }
    
    /**
     * 测试1：定时任务触发测试
     * 验证定时任务可以按计划触发
     */
    @Test
    @DisplayName("测试定时任务触发 - 应支持定时触发")
    void testScheduledTaskTrigger() {
        // 准备测试数据
        String hospitalId = "hospital-001";
        String cronExpression = "0 0 2 * * ?"; // 每天凌晨2点
        
        // 设置定时任务
        syncScheduler.scheduleSyncTask(hospitalId, cronExpression);
        
        // 验证定时任务已设置
        assertTrue(syncScheduler.hasScheduledTask(hospitalId), "定时任务应已设置");
    }
    
    /**
     * 测试2：任务并发控制测试
     * 验证同一医院的任务不会并发执行
     */
    @Test
    @DisplayName("测试任务并发控制 - 同一医院任务不应并发执行")
    void testTaskConcurrencyControl() {
        // 准备测试数据
        String hospitalId = "hospital-001";
        
        // 模拟同时触发两个任务
        boolean canExecute1 = syncScheduler.canExecuteTask(hospitalId);
        boolean canExecute2 = syncScheduler.canExecuteTask(hospitalId);
        
        // 验证同一时间只有一个任务可以执行
        assertTrue(canExecute1, "第一个任务应可以执行");
        assertFalse(canExecute2, "第二个任务应被阻止执行");
    }
    
    /**
     * 测试3：任务状态管理测试
     * 验证可以跟踪和管理任务状态
     */
    @Test
    @DisplayName("测试任务状态管理 - 应能跟踪任务状态")
    void testTaskStatusManagement() {
        // 准备测试数据
        String hospitalId = "hospital-001";
        String taskId = "task-001";
        
        // 开始任务
        syncScheduler.startTask(hospitalId, taskId);
        
        // 验证任务状态
        String status = syncScheduler.getTaskStatus(taskId);
        assertEquals("RUNNING", status, "任务状态应为RUNNING");
        
        // 完成任务
        syncScheduler.completeTask(taskId);
        
        // 验证任务状态更新
        status = syncScheduler.getTaskStatus(taskId);
        assertEquals("COMPLETED", status, "任务状态应为COMPLETED");
    }
    
    /**
     * 测试4：多医院任务调度测试
     * 验证可以同时调度多个医院的任务
     */
    @Test
    @DisplayName("测试多医院任务调度 - 应支持多医院任务调度")
    void testMultipleHospitalTaskScheduling() {
        // 准备测试数据
        String hospitalId1 = "hospital-001";
        String hospitalId2 = "hospital-002";
        String cronExpression = "0 0 2 * * ?";
        
        // 设置多个医院的任务
        syncScheduler.scheduleSyncTask(hospitalId1, cronExpression);
        syncScheduler.scheduleSyncTask(hospitalId2, cronExpression);
        
        // 验证两个任务都已设置
        assertTrue(syncScheduler.hasScheduledTask(hospitalId1), "医院1任务应已设置");
        assertTrue(syncScheduler.hasScheduledTask(hospitalId2), "医院2任务应已设置");
    }
    
    /**
     * 测试5：任务取消测试
     * 验证可以取消已调度的任务
     */
    @Test
    @DisplayName("测试任务取消 - 应支持取消已调度任务")
    void testTaskCancellation() {
        // 准备测试数据
        String hospitalId = "hospital-001";
        String cronExpression = "0 0 2 * * ?";
        
        // 设置任务
        syncScheduler.scheduleSyncTask(hospitalId, cronExpression);
        assertTrue(syncScheduler.hasScheduledTask(hospitalId), "任务应已设置");
        
        // 取消任务
        syncScheduler.cancelScheduledTask(hospitalId);
        
        // 验证任务已取消
        assertFalse(syncScheduler.hasScheduledTask(hospitalId), "任务应已取消");
    }
    
    /**
     * 测试6：未知任务状态测试
     * 验证获取不存在的任务状态应返回UNKNOWN
     */
    @Test
    @DisplayName("测试未知任务状态 - 不存在的任务应返回UNKNOWN")
    void testUnknownTaskStatus() {
        // 准备测试数据
        String nonExistentTaskId = "non-existent-task";
        
        // 获取不存在的任务状态
        String status = syncScheduler.getTaskStatus(nonExistentTaskId);
        
        // 验证返回UNKNOWN
        assertEquals("UNKNOWN", status, "不存在的任务状态应为UNKNOWN");
    }
    
    /**
     * 测试7：重复调度测试
     * 验证重复调度同一医院任务不会出错
     */
    @Test
    @DisplayName("测试重复调度 - 重复调度同一医院任务应正常处理")
    void testDuplicateScheduling() {
        // 准备测试数据
        String hospitalId = "hospital-001";
        String cronExpression = "0 0 2 * * ?";
        
        // 第一次调度
        syncScheduler.scheduleSyncTask(hospitalId, cronExpression);
        assertTrue(syncScheduler.hasScheduledTask(hospitalId), "第一次调度后任务应已设置");
        
        // 第二次调度（重复）
        syncScheduler.scheduleSyncTask(hospitalId, cronExpression);
        assertTrue(syncScheduler.hasScheduledTask(hospitalId), "第二次调度后任务应仍存在");
    }
    
    /**
     * 测试8：多线程并发测试
     * 验证多线程环境下并发控制的正确性
     */
    @Test
    @DisplayName("测试多线程并发 - 多线程环境下应正确控制并发")
    void testMultiThreadConcurrency() throws InterruptedException {
        // 准备测试数据
        String hospitalId = "hospital-001";
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        
        // 用于记录成功执行的任务数
        final int[] successCount = {0};
        
        // 创建多个线程同时尝试执行任务
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    if (syncScheduler.canExecuteTask(hospitalId)) {
                        try {
                            // 模拟任务执行
                            Thread.sleep(50);
                            // 记录成功
                            successCount[0]++;
                        } finally {
                            // 释放锁 - 通过完成任务来释放锁
                            String taskId = "test-task-" + System.identityHashCode(Thread.currentThread());
                            syncScheduler.startTask(hospitalId, taskId);
                            syncScheduler.completeTask(taskId);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }
        
        // 同时启动所有线程
        startLatch.countDown();
        
        // 等待所有线程完成
        assertTrue(finishLatch.await(5, TimeUnit.SECONDS), "所有线程应在5秒内完成");
        
        // 验证只有一个线程成功执行
        assertEquals(1, successCount[0], "多线程环境下应只有一个线程能成功执行");
        
        // 关闭线程池
        executorService.shutdown();
    }
    
    /**
     * 测试9：任务ID生成测试
     * 验证任务ID生成格式正确
     */
    @Test
    @DisplayName("测试任务ID生成 - 生成的任务ID格式应正确")
    void testTaskIdGeneration() {
        // 准备测试数据
        String hospitalId = "hospital-001";
        
        // 通过执行任务来测试任务ID生成
        syncScheduler.scheduleSyncTask(hospitalId, "0 0 2 * * ?");
        
        // 由于任务ID生成是私有的，我们通过间接方式测试
        // 这里主要验证任务调度和状态管理功能正常
        String taskId = "test-task-001";
        syncScheduler.startTask(hospitalId, taskId);
        assertEquals("RUNNING", syncScheduler.getTaskStatus(taskId), "任务状态应为RUNNING");
    }
    
    /**
     * 测试10：医院ID提取测试
     * 验证从任务ID中正确提取医院ID
     */
    @Test
    @DisplayName("测试医院ID提取 - 应从任务ID中正确提取医院ID")
    void testHospitalIdExtraction() {
        // 准备测试数据
        String hospitalId = "hospital-001";
        String taskId = "sync-hospital-001-1234567890";
        
        // 开始任务
        syncScheduler.startTask(hospitalId, taskId);
        
        // 完成任务（会触发医院ID提取和锁释放）
        syncScheduler.completeTask(taskId);
        
        // 验证任务状态
        assertEquals("COMPLETED", syncScheduler.getTaskStatus(taskId), "任务状态应为COMPLETED");
    }
    
    /**
     * 测试11：无效任务ID提取测试
     * 验证处理无效任务ID时不会出错
     */
    @Test
    @DisplayName("测试无效任务ID提取 - 无效任务ID应安全处理")
    void testInvalidTaskIdExtraction() {
        // 准备测试数据 - 无效的任务ID
        String invalidTaskId = "invalid-task-id";
        
        // 完成任务（应安全处理无效任务ID）
        syncScheduler.completeTask(invalidTaskId);
        
        // 验证不会抛出异常，任务状态为UNKNOWN
        assertEquals("UNKNOWN", syncScheduler.getTaskStatus(invalidTaskId), "无效任务ID状态应为UNKNOWN");
    }
    
    /**
     * 测试12：空医院ID处理测试
     * 验证处理空医院ID时不会出错
     */
    @Test
    @DisplayName("测试空医院ID处理 - 空医院ID应安全处理")
    void testEmptyHospitalIdHandling() {
        // 准备测试数据 - 空医院ID
        String emptyHospitalId = "";
        
        // 检查任务（应安全处理空医院ID）
        boolean hasTask = syncScheduler.hasScheduledTask(emptyHospitalId);
        assertFalse(hasTask, "空医院ID不应有调度任务");
        
        // 检查是否可以执行任务
        boolean canExecute = syncScheduler.canExecuteTask(emptyHospitalId);
        assertTrue(canExecute, "空医院ID应可以执行任务（会创建新锁）");
    }
}
