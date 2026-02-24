package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.model.SyncLog;
import com.example.medaiassistant.hospital.repository.SyncLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 同步状态管理服务单元测试
 * 使用Mockito进行单元测试
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-04
 */
@ExtendWith(MockitoExtension.class)
class SyncStatusServiceUnitTest {
    
    @Mock
    private SyncLogRepository syncLogRepository;
    
    private SyncStatusService syncStatusService;
    
    @BeforeEach
    void setUp() {
        syncStatusService = new SyncStatusService(syncLogRepository);
    }
    
    @Test
    void testCreateSyncLog() {
        // 准备
        SyncLog mockLog = SyncLog.createRunning("hospital-001", "FULL");
        mockLog.setId(1L);
        when(syncLogRepository.save(any(SyncLog.class))).thenReturn(mockLog);
        
        // 执行
        SyncLog result = syncStatusService.createSyncLog("hospital-001", "FULL");
        
        // 验证
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("hospital-001", result.getHospitalId());
        assertEquals("FULL", result.getSyncType());
        assertEquals("RUNNING", result.getStatus());
        
        verify(syncLogRepository, times(1)).save(any(SyncLog.class));
    }
    
    @Test
    void testMarkSuccess() {
        // 准备
        Long logId = 1L;
        SyncLog mockLog = SyncLog.createRunning("hospital-001", "FULL");
        mockLog.setId(logId);
        
        when(syncLogRepository.findById(logId)).thenReturn(Optional.of(mockLog));
        when(syncLogRepository.save(any(SyncLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // 执行
        SyncLog result = syncStatusService.markSuccess(logId, 100);
        
        // 验证
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(100, result.getRecordsSynced());
        assertNotNull(result.getEndTime());
        assertNotNull(result.getUpdatedAt());
        
        verify(syncLogRepository, times(1)).findById(logId);
        verify(syncLogRepository, times(1)).save(any(SyncLog.class));
    }
    
    @Test
    void testMarkSuccess_LogNotFound() {
        // 准备
        Long logId = 999L;
        when(syncLogRepository.findById(logId)).thenReturn(Optional.empty());
        
        // 执行和验证
        assertThrows(IllegalArgumentException.class, () -> {
            syncStatusService.markSuccess(logId, 100);
        });
        
        verify(syncLogRepository, times(1)).findById(logId);
        verify(syncLogRepository, never()).save(any(SyncLog.class));
    }
    
    @Test
    void testMarkFailed() {
        // 准备
        Long logId = 1L;
        SyncLog mockLog = SyncLog.createRunning("hospital-001", "FULL");
        mockLog.setId(logId);
        String errorMessage = "数据库连接失败";
        
        when(syncLogRepository.findById(logId)).thenReturn(Optional.of(mockLog));
        when(syncLogRepository.save(any(SyncLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // 执行
        SyncLog result = syncStatusService.markFailed(logId, errorMessage);
        
        // 验证
        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());
        assertEquals(errorMessage, result.getErrorMessage());
        assertNotNull(result.getEndTime());
        assertNotNull(result.getUpdatedAt());
        
        verify(syncLogRepository, times(1)).findById(logId);
        verify(syncLogRepository, times(1)).save(any(SyncLog.class));
    }
    
    @Test
    void testGetSyncLog() {
        // 准备
        Long logId = 1L;
        SyncLog mockLog = SyncLog.createRunning("hospital-001", "FULL");
        mockLog.setId(logId);
        
        when(syncLogRepository.findById(logId)).thenReturn(Optional.of(mockLog));
        
        // 执行
        Optional<SyncLog> result = syncStatusService.getSyncLog(logId);
        
        // 验证
        assertTrue(result.isPresent());
        assertEquals(logId, result.get().getId());
        assertEquals("hospital-001", result.get().getHospitalId());
        
        verify(syncLogRepository, times(1)).findById(logId);
    }
    
    @Test
    void testGetSyncLogsByHospital() {
        // 准备
        String hospitalId = "hospital-001";
        List<SyncLog> mockLogs = Arrays.asList(
            SyncLog.createRunning(hospitalId, "FULL"),
            SyncLog.createRunning(hospitalId, "INCREMENTAL")
        );
        
        when(syncLogRepository.findByHospitalId(hospitalId)).thenReturn(mockLogs);
        
        // 执行
        List<SyncLog> result = syncStatusService.getSyncLogsByHospital(hospitalId);
        
        // 验证
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(log -> hospitalId.equals(log.getHospitalId())));
        
        verify(syncLogRepository, times(1)).findByHospitalId(hospitalId);
    }
    
    @Test
    void testGetRunningSyncLogs() {
        // 准备
        List<SyncLog> mockLogs = Arrays.asList(
            SyncLog.createRunning("hospital-001", "FULL"),
            SyncLog.createRunning("hospital-002", "INCREMENTAL")
        );
        
        when(syncLogRepository.findRunningLogs()).thenReturn(mockLogs);
        
        // 执行
        List<SyncLog> result = syncStatusService.getRunningSyncLogs();
        
        // 验证
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(SyncLog::isRunning));
        
        verify(syncLogRepository, times(1)).findRunningLogs();
    }
    
    @Test
    void testCountSuccessSyncs() {
        // 准备
        String hospitalId = "hospital-001";
        when(syncLogRepository.countSuccessByHospitalId(hospitalId)).thenReturn(5L);
        
        // 执行
        long result = syncStatusService.countSuccessSyncs(hospitalId);
        
        // 验证
        assertEquals(5L, result);
        
        verify(syncLogRepository, times(1)).countSuccessByHospitalId(hospitalId);
    }
    
    @Test
    void testCountFailedSyncs() {
        // 准备
        String hospitalId = "hospital-001";
        when(syncLogRepository.countFailedByHospitalId(hospitalId)).thenReturn(2L);
        
        // 执行
        long result = syncStatusService.countFailedSyncs(hospitalId);
        
        // 验证
        assertEquals(2L, result);
        
        verify(syncLogRepository, times(1)).countFailedByHospitalId(hospitalId);
    }
    
    @Test
    void testCalculateSuccessRate() {
        // 准备
        String hospitalId = "hospital-001";
        when(syncLogRepository.countTotalByHospitalId(hospitalId)).thenReturn(10L);
        when(syncLogRepository.countSuccessByHospitalId(hospitalId)).thenReturn(7L);
        
        // 执行
        double result = syncStatusService.calculateSuccessRate(hospitalId);
        
        // 验证
        assertEquals(70.0, result, 0.01);
        
        verify(syncLogRepository, times(1)).countTotalByHospitalId(hospitalId);
        verify(syncLogRepository, times(1)).countSuccessByHospitalId(hospitalId);
    }
    
    @Test
    void testCalculateSuccessRate_ZeroTotal() {
        // 准备
        String hospitalId = "hospital-001";
        when(syncLogRepository.countTotalByHospitalId(hospitalId)).thenReturn(0L);
        
        // 执行
        double result = syncStatusService.calculateSuccessRate(hospitalId);
        
        // 验证
        assertEquals(0.0, result, 0.01);
        
        verify(syncLogRepository, times(1)).countTotalByHospitalId(hospitalId);
        verify(syncLogRepository, never()).countSuccessByHospitalId(anyString());
    }
    
    @Test
    void testGetSyncStats() {
        // 准备
        String hospitalId = "hospital-001";
        when(syncLogRepository.countTotalByHospitalId(hospitalId)).thenReturn(10L);
        when(syncLogRepository.countSuccessByHospitalId(hospitalId)).thenReturn(7L);
        when(syncLogRepository.countFailedByHospitalId(hospitalId)).thenReturn(3L);
        
        // 执行
        SyncStatusService.SyncStats stats = syncStatusService.getSyncStats(hospitalId);
        
        // 验证
        assertNotNull(stats);
        assertEquals(hospitalId, stats.getHospitalId());
        assertEquals(10L, stats.getTotalSyncs());
        assertEquals(7L, stats.getSuccessSyncs());
        assertEquals(3L, stats.getFailedSyncs());
        assertEquals(70.0, stats.getSuccessRate(), 0.01);
        
        // countTotalByHospitalId 被调用了两次：一次在getSyncStats中，一次在calculateSuccessRate中
        verify(syncLogRepository, times(2)).countTotalByHospitalId(hospitalId);
        // countSuccessByHospitalId 被调用了两次：一次在getSyncStats中，一次在calculateSuccessRate中
        verify(syncLogRepository, times(2)).countSuccessByHospitalId(hospitalId);
        verify(syncLogRepository, times(1)).countFailedByHospitalId(hospitalId);
    }
    
    @Test
    void testHasRunningSync() {
        // 准备
        String hospitalId = "hospital-001";
        List<SyncLog> mockLogs = Collections.singletonList(
            SyncLog.createRunning(hospitalId, "FULL")
        );
        
        when(syncLogRepository.findByHospitalIdAndStatus(hospitalId, "RUNNING")).thenReturn(mockLogs);
        
        // 执行
        boolean result = syncStatusService.hasRunningSync(hospitalId);
        
        // 验证
        assertTrue(result);
        
        verify(syncLogRepository, times(1)).findByHospitalIdAndStatus(hospitalId, "RUNNING");
    }
    
    @Test
    void testHasRunningSync_NoRunning() {
        // 准备
        String hospitalId = "hospital-001";
        when(syncLogRepository.findByHospitalIdAndStatus(hospitalId, "RUNNING")).thenReturn(Collections.emptyList());
        
        // 执行
        boolean result = syncStatusService.hasRunningSync(hospitalId);
        
        // 验证
        assertFalse(result);
        
        verify(syncLogRepository, times(1)).findByHospitalIdAndStatus(hospitalId, "RUNNING");
    }
    
    @Test
    void testArchiveOldLogs() {
        // 准备
        int days = 30;
        when(syncLogRepository.deleteByCreatedAtBefore(any(Date.class))).thenReturn(5);
        
        // 执行
        int result = syncStatusService.archiveOldLogs(days);
        
        // 验证
        assertEquals(5, result);
        
        verify(syncLogRepository, times(1)).deleteByCreatedAtBefore(any(Date.class));
    }
}
