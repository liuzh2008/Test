package com.example.medaiassistant.hospital.repository;

import com.example.medaiassistant.config.TestConfig;
import com.example.medaiassistant.hospital.model.SyncLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 同步日志数据访问层测试
 * 测试SyncLogRepository的CRUD操作和查询功能
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-04
 */
@TestConfig(description = "同步日志数据访问层测试")
@Transactional
class SyncLogRepositoryTest {
    
    @Autowired
    private SyncLogRepository syncLogRepository;
    
    /**
     * 测试保存和查找同步日志
     * 验证基本的CRUD操作
     */
    @Test
    void testSaveAndFindSyncLog() {
        // 创建同步日志
        SyncLog syncLog = SyncLog.createRunning("hospital-001", "FULL");
        syncLog.setRecordsSynced(100);
        syncLog.markSuccess(100);
        
        // 保存
        SyncLog saved = syncLogRepository.save(syncLog);
        
        // 验证保存成功
        assertNotNull(saved.getId(), "保存后应有ID");
        assertEquals("hospital-001", saved.getHospitalId(), "医院ID应匹配");
        assertEquals("FULL", saved.getSyncType(), "同步类型应匹配");
        assertEquals("SUCCESS", saved.getStatus(), "状态应为SUCCESS");
        assertEquals(100, saved.getRecordsSynced(), "同步记录数应匹配");
        
        // 查找
        Optional<SyncLog> found = syncLogRepository.findById(saved.getId());
        assertTrue(found.isPresent(), "应能找到保存的同步日志");
        assertEquals(saved.getId(), found.get().getId(), "ID应匹配");
    }
    
    /**
     * 测试根据医院ID查询同步日志
     */
    @Test
    void testFindByHospitalId() {
        // 创建测试数据
        SyncLog log1 = SyncLog.createRunning("hospital-001", "FULL");
        log1.markSuccess(100);
        syncLogRepository.save(log1);
        
        SyncLog log2 = SyncLog.createRunning("hospital-001", "INCREMENTAL");
        log2.markSuccess(50);
        syncLogRepository.save(log2);
        
        SyncLog log3 = SyncLog.createRunning("hospital-002", "FULL");
        log3.markSuccess(200);
        syncLogRepository.save(log3);
        
        // 查询hospital-001的日志
        List<SyncLog> logs = syncLogRepository.findByHospitalId("hospital-001");
        
        // 验证结果
        assertNotNull(logs, "查询结果不应为null");
        assertEquals(2, logs.size(), "应找到2条hospital-001的日志");
        assertTrue(logs.stream().allMatch(log -> "hospital-001".equals(log.getHospitalId())),
                "所有日志的医院ID都应为hospital-001");
    }
    
    /**
     * 测试根据医院ID和同步类型查询同步日志
     */
    @Test
    void testFindByHospitalIdAndSyncType() {
        // 创建测试数据
        SyncLog log1 = SyncLog.createRunning("hospital-001", "FULL");
        log1.markSuccess(100);
        syncLogRepository.save(log1);
        
        SyncLog log2 = SyncLog.createRunning("hospital-001", "INCREMENTAL");
        log2.markSuccess(50);
        syncLogRepository.save(log2);
        
        SyncLog log3 = SyncLog.createRunning("hospital-001", "FULL");
        log3.markFailed("连接超时");
        syncLogRepository.save(log3);
        
        // 查询hospital-001的FULL类型日志
        List<SyncLog> logs = syncLogRepository.findByHospitalIdAndSyncType("hospital-001", "FULL");
        
        // 验证结果
        assertNotNull(logs, "查询结果不应为null");
        assertEquals(2, logs.size(), "应找到2条FULL类型的日志");
        assertTrue(logs.stream().allMatch(log -> 
                "hospital-001".equals(log.getHospitalId()) && "FULL".equals(log.getSyncType())),
                "所有日志的医院ID都应为hospital-001且同步类型为FULL");
    }
    
    /**
     * 测试根据状态查询同步日志
     */
    @Test
    void testFindByStatus() {
        // 创建测试数据
        SyncLog log1 = SyncLog.createRunning("hospital-001", "FULL");
        log1.markSuccess(100);
        syncLogRepository.save(log1);
        
        SyncLog log2 = SyncLog.createRunning("hospital-002", "INCREMENTAL");
        log2.markFailed("数据库连接失败");
        syncLogRepository.save(log2);
        
        SyncLog log3 = SyncLog.createRunning("hospital-003", "FULL");
        log3.markSuccess(200);
        syncLogRepository.save(log3);
        
        // 查询成功的日志
        List<SyncLog> successLogs = syncLogRepository.findByStatus("SUCCESS");
        
        // 验证结果
        assertNotNull(successLogs, "查询结果不应为null");
        assertEquals(2, successLogs.size(), "应找到2条成功的日志");
        assertTrue(successLogs.stream().allMatch(log -> "SUCCESS".equals(log.getStatus())),
                "所有日志的状态都应为SUCCESS");
        
        // 查询失败的日志
        List<SyncLog> failedLogs = syncLogRepository.findByStatus("FAILED");
        assertEquals(1, failedLogs.size(), "应找到1条失败的日志");
        assertEquals("FAILED", failedLogs.get(0).getStatus(), "日志状态应为FAILED");
    }
    
    /**
     * 测试根据医院ID分页查询同步日志
     */
    @Test
    void testFindByHospitalIdWithPagination() {
        // 创建测试数据（创建多条记录）
        for (int i = 0; i < 15; i++) {
            SyncLog log = SyncLog.createRunning("hospital-001", i % 2 == 0 ? "FULL" : "INCREMENTAL");
            log.markSuccess(i * 10);
            syncLogRepository.save(log);
        }
        
        // 分页查询（第一页，每页5条）
        Pageable pageable = PageRequest.of(0, 5);
        Page<SyncLog> page = syncLogRepository.findByHospitalId("hospital-001", pageable);
        
        // 验证分页结果
        assertNotNull(page, "分页结果不应为null");
        assertEquals(15, page.getTotalElements(), "总记录数应为15");
        assertEquals(5, page.getContent().size(), "当前页记录数应为5");
        assertEquals(3, page.getTotalPages(), "总页数应为3");
        assertTrue(page.getContent().stream().allMatch(log -> "hospital-001".equals(log.getHospitalId())),
                "所有日志的医院ID都应为hospital-001");
    }
    
    /**
     * 测试根据时间范围查询同步日志
     */
    @Test
    void testFindByStartTimeBetween() {
        // 创建测试数据
        Date now = new Date();
        Date oneHourAgo = new Date(now.getTime() - 3600000);
        Date twoHoursAgo = new Date(now.getTime() - 7200000);
        
        // 创建不同时间的日志
        SyncLog log1 = SyncLog.createRunning("hospital-001", "FULL");
        log1.setStartTime(twoHoursAgo);
        log1.markSuccess(100);
        syncLogRepository.save(log1);
        
        SyncLog log2 = SyncLog.createRunning("hospital-002", "INCREMENTAL");
        log2.setStartTime(oneHourAgo);
        log2.markSuccess(50);
        syncLogRepository.save(log2);
        
        SyncLog log3 = SyncLog.createRunning("hospital-003", "FULL");
        log3.setStartTime(now);
        log3.markSuccess(200);
        syncLogRepository.save(log3);
        
        // 查询过去2小时到现在的日志
        Date startTime = new Date(now.getTime() - 7200000); // 2小时前
        Date endTime = new Date(now.getTime() + 1000); // 现在+1秒
        List<SyncLog> logs = syncLogRepository.findByStartTimeBetween(startTime, endTime);
        
        // 验证结果
        assertNotNull(logs, "查询结果不应为null");
        assertEquals(3, logs.size(), "应找到3条在时间范围内的日志");
    }
    
    /**
     * 测试统计医院的成功同步次数
     */
    @Test
    void testCountSuccessByHospitalId() {
        // 创建测试数据
        SyncLog log1 = SyncLog.createRunning("hospital-001", "FULL");
        log1.markSuccess(100);
        syncLogRepository.save(log1);
        
        SyncLog log2 = SyncLog.createRunning("hospital-001", "INCREMENTAL");
        log2.markSuccess(50);
        syncLogRepository.save(log2);
        
        SyncLog log3 = SyncLog.createRunning("hospital-001", "FULL");
        log3.markFailed("连接失败");
        syncLogRepository.save(log3);
        
        SyncLog log4 = SyncLog.createRunning("hospital-002", "FULL");
        log4.markSuccess(200);
        syncLogRepository.save(log4);
        
        // 统计hospital-001的成功次数
        long successCount = syncLogRepository.countSuccessByHospitalId("hospital-001");
        
        // 验证结果
        assertEquals(2, successCount, "hospital-001应有2次成功同步");
    }
    
    /**
     * 测试统计医院的失败同步次数
     */
    @Test
    void testCountFailedByHospitalId() {
        // 创建测试数据
        SyncLog log1 = SyncLog.createRunning("hospital-001", "FULL");
        log1.markFailed("连接超时");
        syncLogRepository.save(log1);
        
        SyncLog log2 = SyncLog.createRunning("hospital-001", "INCREMENTAL");
        log2.markSuccess(50);
        syncLogRepository.save(log2);
        
        SyncLog log3 = SyncLog.createRunning("hospital-001", "FULL");
        log3.markFailed("数据库错误");
        syncLogRepository.save(log3);
        
        // 统计hospital-001的失败次数
        long failedCount = syncLogRepository.countFailedByHospitalId("hospital-001");
        
        // 验证结果
        assertEquals(2, failedCount, "hospital-001应有2次失败同步");
    }
    
    /**
     * 测试获取医院最近一次成功的同步日志
     */
    @Test
    void testFindLatestSuccessByHospitalId() {
        // 创建测试数据（按时间顺序）
        Date now = new Date();
        
        // 较早的成功日志
        SyncLog log1 = SyncLog.createRunning("hospital-001", "FULL");
        log1.setStartTime(new Date(now.getTime() - 3600000)); // 1小时前
        log1.markSuccess(100);
        syncLogRepository.save(log1);
        
        // 较近的成功日志
        SyncLog log2 = SyncLog.createRunning("hospital-001", "INCREMENTAL");
        log2.setStartTime(new Date(now.getTime() - 1800000)); // 30分钟前
        log2.markSuccess(50);
        syncLogRepository.save(log2);
        
        // 失败的日志
        SyncLog log3 = SyncLog.createRunning("hospital-001", "FULL");
        log3.setStartTime(now); // 现在
        log3.markFailed("错误");
        syncLogRepository.save(log3);
        
        // 获取最近一次成功的日志
        List<SyncLog> latestSuccess = syncLogRepository.findLatestSuccessByHospitalId("hospital-001", PageRequest.of(0, 1));
        
        // 验证结果
        assertNotNull(latestSuccess, "查询结果不应为null");
        assertFalse(latestSuccess.isEmpty(), "应找到最近一次成功的日志");
        assertEquals(log2.getId(), latestSuccess.get(0).getId(), "应返回较近的成功日志");
        assertEquals("INCREMENTAL", latestSuccess.get(0).getSyncType(), "同步类型应为INCREMENTAL");
    }
    
    /**
     * 测试默认方法：查询运行中的同步日志
     */
    @Test
    void testFindRunningLogs() {
        // 创建测试数据
        SyncLog log1 = SyncLog.createRunning("hospital-001", "FULL");
        syncLogRepository.save(log1); // 运行中
        
        SyncLog log2 = SyncLog.createRunning("hospital-002", "INCREMENTAL");
        log2.markSuccess(50);
        syncLogRepository.save(log2); // 成功
        
        SyncLog log3 = SyncLog.createRunning("hospital-003", "FULL");
        syncLogRepository.save(log3); // 运行中
        
        // 查询运行中的日志
        List<SyncLog> runningLogs = syncLogRepository.findRunningLogs();
        
        // 验证结果
        assertNotNull(runningLogs, "查询结果不应为null");
        assertEquals(2, runningLogs.size(), "应找到2条运行中的日志");
        assertTrue(runningLogs.stream().allMatch(SyncLog::isRunning),
                "所有日志都应为运行中状态");
    }
    
    /**
     * 测试统计医院同步记录数
     */
    @Test
    void testCountByHospitalId() {
        // 创建测试数据
        for (int i = 0; i < 5; i++) {
            SyncLog log = SyncLog.createRunning("hospital-001", "FULL");
            log.markSuccess(i * 10);
            syncLogRepository.save(log);
        }
        
        for (int i = 0; i < 3; i++) {
            SyncLog log = SyncLog.createRunning("hospital-002", "INCREMENTAL");
            log.markSuccess(i * 5);
            syncLogRepository.save(log);
        }
        
        // 统计hospital-001的记录数
        long count1 = syncLogRepository.countByHospitalId("hospital-001");
        assertEquals(5, count1, "hospital-001应有5条记录");
        
        // 统计hospital-002的记录数
        long count2 = syncLogRepository.countByHospitalId("hospital-002");
        assertEquals(3, count2, "hospital-002应有3条记录");
    }
}
