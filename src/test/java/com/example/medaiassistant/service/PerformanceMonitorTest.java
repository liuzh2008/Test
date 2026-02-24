package com.example.medaiassistant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PerformanceMonitor服务测试类
 * 按照TDD红-绿-重构流程实现性能监控服务
 * 
 * 测试目标：
 * 1. 同步性能指标收集
 * 2. 数据库性能指标收集
 * 3. 系统资源指标收集
 * 4. 指标聚合和导出
 */
@ExtendWith(MockitoExtension.class)
class PerformanceMonitorTest {

    @InjectMocks
    private PerformanceMonitor performanceMonitor;

    /**
     * 测试PerformanceMonitor服务是否成功注入
     * 绿阶段：这个测试应该通过，因为PerformanceMonitor服务已实现
     */
    @Test
    void performanceMonitorShouldBeAutowired() {
        assertNotNull(performanceMonitor, "PerformanceMonitor服务应该被成功注入");
    }

    /**
     * 测试收集同步性能指标
     * 绿阶段：这个测试应该通过，因为方法已实现
     */
    @Test
    void shouldCollectSyncPerformanceMetrics() {
        // 模拟同步操作
        String hospitalId = "hospital-001";
        String syncType = "INCREMENTAL";
        long recordsProcessed = 100;
        long executionTimeMs = 5000;
        
        // 记录同步性能指标
        performanceMonitor.recordSyncPerformance(hospitalId, syncType, recordsProcessed, executionTimeMs);
        
        // 获取同步性能指标
        Map<String, Object> syncMetrics = performanceMonitor.getSyncPerformanceMetrics();
        
        assertNotNull(syncMetrics, "同步性能指标不应为空");
        assertTrue(syncMetrics.containsKey("totalSyncOperations"), "应包含总同步操作数");
        assertTrue(syncMetrics.containsKey("averageSyncTime"), "应包含平均同步时间");
    }

    /**
     * 测试收集数据库性能指标
     * 绿阶段：这个测试应该通过，因为方法已实现
     */
    @Test
    void shouldCollectDatabasePerformanceMetrics() {
        // 模拟数据库操作
        String operationType = "QUERY";
        long executionTimeMs = 100;
        boolean success = true;
        
        // 记录数据库性能指标
        performanceMonitor.recordDatabasePerformance(operationType, executionTimeMs, success);
        
        // 获取数据库性能指标
        Map<String, Object> dbMetrics = performanceMonitor.getDatabasePerformanceMetrics();
        
        assertNotNull(dbMetrics, "数据库性能指标不应为空");
        assertTrue(dbMetrics.containsKey("totalDatabaseOperations"), "应包含总数据库操作数");
        assertTrue(dbMetrics.containsKey("successRate"), "应包含成功率");
    }

    /**
     * 测试收集系统资源指标
     * 绿阶段：这个测试应该通过，因为方法已实现
     */
    @Test
    void shouldCollectSystemResourceMetrics() {
        // 获取系统资源指标
        Map<String, Object> systemMetrics = performanceMonitor.getSystemResourceMetrics();
        
        assertNotNull(systemMetrics, "系统资源指标不应为空");
        assertTrue(systemMetrics.containsKey("memoryUsage"), "应包含内存使用率");
        assertTrue(systemMetrics.containsKey("cpuUsage"), "应包含CPU使用率");
        assertTrue(systemMetrics.containsKey("threadCount"), "应包含线程数");
    }

    /**
     * 测试获取所有性能指标
     * 绿阶段：这个测试应该通过，因为方法已实现
     */
    @Test
    void shouldGetAllPerformanceMetrics() {
        // 获取所有性能指标
        Map<String, Object> allMetrics = performanceMonitor.getAllPerformanceMetrics();
        
        assertNotNull(allMetrics, "所有性能指标不应为空");
        assertTrue(allMetrics.containsKey("syncPerformance"), "应包含同步性能指标");
        assertTrue(allMetrics.containsKey("databasePerformance"), "应包含数据库性能指标");
        assertTrue(allMetrics.containsKey("systemResources"), "应包含系统资源指标");
        assertTrue(allMetrics.containsKey("timestamp"), "应包含时间戳");
    }

    /**
     * 测试重置性能指标
     * 绿阶段：这个测试应该通过，因为方法已实现
     */
    @Test
    void shouldResetPerformanceMetrics() {
        // 先记录一些指标
        performanceMonitor.recordSyncPerformance("hospital-001", "FULL", 1000, 10000);
        performanceMonitor.recordDatabasePerformance("INSERT", 50, true);
        
        // 重置指标
        performanceMonitor.resetPerformanceMetrics();
        
        // 获取重置后的指标
        Map<String, Object> syncMetrics = performanceMonitor.getSyncPerformanceMetrics();
        Map<String, Object> dbMetrics = performanceMonitor.getDatabasePerformanceMetrics();
        
        // 验证指标已重置
        assertEquals(0L, syncMetrics.get("totalSyncOperations"), "同步操作数应重置为0");
        assertEquals(0L, dbMetrics.get("totalDatabaseOperations"), "数据库操作数应重置为0");
    }

    /**
     * 测试性能指标快照功能
     * 绿阶段：这个测试应该通过，因为方法已实现
     */
    @Test
    void shouldTakePerformanceSnapshot() {
        // 创建性能快照
        Map<String, Object> snapshot = performanceMonitor.takePerformanceSnapshot();
        
        assertNotNull(snapshot, "性能快照不应为空");
        assertTrue(snapshot.containsKey("snapshotId"), "应包含快照ID");
        assertTrue(snapshot.containsKey("timestamp"), "应包含时间戳");
        assertTrue(snapshot.containsKey("metrics"), "应包含指标数据");
    }

    /**
     * 测试获取最后一次性能快照
     * 完善测试：验证快照缓存功能
     */
    @Test
    void shouldGetLastSnapshot() {
        // 初始状态下应为null
        assertNull(performanceMonitor.getLastSnapshot(), "初始状态下最后一次快照应为null");
        
        // 创建快照
        Map<String, Object> snapshot = performanceMonitor.takePerformanceSnapshot();
        
        // 获取最后一次快照
        Map<String, Object> lastSnapshot = performanceMonitor.getLastSnapshot();
        
        assertNotNull(lastSnapshot, "最后一次快照不应为空");
        assertEquals(snapshot.get("snapshotId"), lastSnapshot.get("snapshotId"), "快照ID应匹配");
    }

    /**
     * 测试多次记录同步性能指标
     * 完善测试：验证累计统计功能
     */
    @Test
    void shouldAccumulateSyncPerformanceMetrics() {
        // 第一次记录
        performanceMonitor.recordSyncPerformance("hospital-001", "INCREMENTAL", 100, 5000);
        
        // 第二次记录
        performanceMonitor.recordSyncPerformance("hospital-002", "FULL", 200, 10000);
        
        // 获取同步性能指标
        Map<String, Object> syncMetrics = performanceMonitor.getSyncPerformanceMetrics();
        
        assertEquals(2L, syncMetrics.get("totalSyncOperations"), "总同步操作数应为2");
        assertEquals(300L, syncMetrics.get("totalSyncRecords"), "总同步记录数应为300");
        assertEquals(15000L, syncMetrics.get("totalSyncTimeMs"), "总同步时间应为15000ms");
        assertEquals(7500.0, (double) syncMetrics.get("averageSyncTime"), 0.01, "平均同步时间应为7500ms");
    }

    /**
     * 测试数据库操作失败统计
     * 完善测试：验证失败操作统计功能
     */
    @Test
    void shouldRecordFailedDatabaseOperations() {
        // 记录成功的数据库操作
        performanceMonitor.recordDatabasePerformance("QUERY", 100, true);
        performanceMonitor.recordDatabasePerformance("INSERT", 200, true);
        
        // 记录失败的数据库操作
        performanceMonitor.recordDatabasePerformance("UPDATE", 150, false);
        performanceMonitor.recordDatabasePerformance("DELETE", 50, false);
        
        // 获取数据库性能指标
        Map<String, Object> dbMetrics = performanceMonitor.getDatabasePerformanceMetrics();
        
        assertEquals(4L, dbMetrics.get("totalDatabaseOperations"), "总数据库操作数应为4");
        assertEquals(2L, dbMetrics.get("successfulDatabaseOperations"), "成功操作数应为2");
        assertEquals(2L, dbMetrics.get("failedDatabaseOperations"), "失败操作数应为2");
        assertEquals(50.0, (double) dbMetrics.get("successRate"), 0.01, "成功率应为50%");
    }

    /**
     * 测试系统资源指标的有效性
     * 完善测试：验证系统资源指标在合理范围内
     */
    @Test
    void shouldReturnValidSystemResourceMetrics() {
        // 获取系统资源指标
        Map<String, Object> systemMetrics = performanceMonitor.getSystemResourceMetrics();
        
        // 验证内存使用率在合理范围内
        double memoryUsage = (double) systemMetrics.get("memoryUsage");
        assertTrue(memoryUsage >= 0 && memoryUsage <= 100, "内存使用率应在0-100%范围内");
        
        // 验证线程数为正整数
        int threadCount = (int) systemMetrics.get("threadCount");
        assertTrue(threadCount > 0, "线程数应为正数");
        
        // 验证可用处理器数为正数
        int availableProcessors = (int) systemMetrics.get("availableProcessors");
        assertTrue(availableProcessors > 0, "可用处理器数应为正数");
    }

    /**
     * 测试重置后指标的正确性
     * 完善测试：验证重置功能完全清除所有指标
     */
    @Test
    void shouldCompletelyResetAllMetrics() {
        // 记录各种指标
        performanceMonitor.recordSyncPerformance("hospital-001", "INCREMENTAL", 100, 5000);
        performanceMonitor.recordDatabasePerformance("QUERY", 100, true);
        performanceMonitor.takePerformanceSnapshot();
        
        // 重置指标
        performanceMonitor.resetPerformanceMetrics();
        
        // 验证所有指标已重置
        Map<String, Object> syncMetrics = performanceMonitor.getSyncPerformanceMetrics();
        Map<String, Object> dbMetrics = performanceMonitor.getDatabasePerformanceMetrics();
        
        assertEquals(0L, syncMetrics.get("totalSyncOperations"), "同步操作数应重置为0");
        assertEquals(0L, syncMetrics.get("totalSyncRecords"), "同步记录数应重置为0");
        assertEquals(0L, syncMetrics.get("totalSyncTimeMs"), "同步时间应重置为0");
        
        assertEquals(0L, dbMetrics.get("totalDatabaseOperations"), "数据库操作数应重置为0");
        assertEquals(0L, dbMetrics.get("successfulDatabaseOperations"), "成功操作数应重置为0");
        assertEquals(0L, dbMetrics.get("failedDatabaseOperations"), "失败操作数应重置为0");
        
        // 验证快照已清除
        assertNull(performanceMonitor.getLastSnapshot(), "最后一次快照应重置为null");
    }
}
