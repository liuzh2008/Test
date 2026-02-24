package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.model.SyncLog;
import com.example.medaiassistant.hospital.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 同步状态管理服务
 * 负责同步日志的创建、更新和查询
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-04
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncStatusService {
    
    private final SyncLogRepository syncLogRepository;
    
    /**
     * 创建同步日志（运行中状态）
     * 
     * @param hospitalId 医院ID
     * @param syncType 同步类型
     * @return 创建的同步日志
     */
    @Transactional
    public SyncLog createSyncLog(String hospitalId, String syncType) {
        SyncLog syncLog = SyncLog.createRunning(hospitalId, syncType);
        SyncLog saved = syncLogRepository.save(syncLog);
        log.info("创建同步日志: 医院ID={}, 同步类型={}, 日志ID={}", hospitalId, syncType, saved.getId());
        return saved;
    }
    
    /**
     * 更新同步日志为成功状态
     * 
     * @param logId 日志ID
     * @param recordsSynced 同步的记录数
     * @return 更新后的同步日志
     */
    @Transactional
    public SyncLog markSuccess(Long logId, int recordsSynced) {
        SyncLog syncLog = findSyncLogById(logId);
        syncLog.markSuccess(recordsSynced);
        SyncLog saved = syncLogRepository.save(syncLog);
        log.info("同步日志标记为成功: 日志ID={}, 记录数={}", logId, recordsSynced);
        return saved;
    }
    
    /**
     * 更新同步日志为失败状态
     * 
     * @param logId 日志ID
     * @param errorMessage 错误信息
     * @return 更新后的同步日志
     */
    @Transactional
    public SyncLog markFailed(Long logId, String errorMessage) {
        SyncLog syncLog = findSyncLogById(logId);
        syncLog.markFailed(errorMessage);
        SyncLog saved = syncLogRepository.save(syncLog);
        log.info("同步日志标记为失败: 日志ID={}, 错误信息={}", logId, errorMessage);
        return saved;
    }
    
    /**
     * 根据ID查找同步日志，如果不存在则抛出异常
     * 
     * @param logId 日志ID
     * @return 同步日志
     * @throws IllegalArgumentException 如果日志不存在
     */
    private SyncLog findSyncLogById(Long logId) {
        Optional<SyncLog> optionalLog = syncLogRepository.findById(logId);
        if (optionalLog.isPresent()) {
            return optionalLog.get();
        } else {
            log.warn("同步日志不存在: 日志ID={}", logId);
            throw new IllegalArgumentException("同步日志不存在: " + logId);
        }
    }
    
    /**
     * 根据ID获取同步日志
     * 
     * @param logId 日志ID
     * @return 同步日志
     */
    public Optional<SyncLog> getSyncLog(Long logId) {
        return syncLogRepository.findById(logId);
    }
    
    /**
     * 根据医院ID获取同步日志列表
     * 
     * @param hospitalId 医院ID
     * @return 同步日志列表
     */
    public List<SyncLog> getSyncLogsByHospital(String hospitalId) {
        return syncLogRepository.findByHospitalId(hospitalId);
    }
    
    /**
     * 根据医院ID分页获取同步日志
     * 
     * @param hospitalId 医院ID
     * @param pageable 分页参数
     * @return 分页的同步日志
     */
    public Page<SyncLog> getSyncLogsByHospital(String hospitalId, Pageable pageable) {
        return syncLogRepository.findByHospitalId(hospitalId, pageable);
    }
    
    /**
     * 获取运行中的同步日志
     * 
     * @return 运行中的同步日志列表
     */
    public List<SyncLog> getRunningSyncLogs() {
        return syncLogRepository.findRunningLogs();
    }
    
    /**
     * 获取成功的同步日志
     * 
     * @return 成功的同步日志列表
     */
    public List<SyncLog> getSuccessSyncLogs() {
        return syncLogRepository.findSuccessLogs();
    }
    
    /**
     * 获取失败的同步日志
     * 
     * @return 失败的同步日志列表
     */
    public List<SyncLog> getFailedSyncLogs() {
        return syncLogRepository.findFailedLogs();
    }
    
    /**
     * 获取医院最近一次成功的同步日志
     * 
     * @param hospitalId 医院ID
     * @return 最近一次成功的同步日志
     */
    public Optional<SyncLog> getLatestSuccessSyncLog(String hospitalId) {
        List<SyncLog> logs = syncLogRepository.findLatestSuccessByHospitalId(hospitalId, Pageable.ofSize(1));
        return logs.isEmpty() ? Optional.empty() : Optional.of(logs.get(0));
    }
    
    /**
     * 获取医院最近一次同步日志
     * 
     * @param hospitalId 医院ID
     * @return 最近一次同步日志
     */
    public Optional<SyncLog> getLatestSyncLog(String hospitalId) {
        List<SyncLog> logs = syncLogRepository.findLatestByHospitalId(hospitalId, Pageable.ofSize(1));
        return logs.isEmpty() ? Optional.empty() : Optional.of(logs.get(0));
    }
    
    /**
     * 统计医院的成功同步次数
     * 
     * @param hospitalId 医院ID
     * @return 成功同步次数
     */
    public long countSuccessSyncs(String hospitalId) {
        return syncLogRepository.countSuccessByHospitalId(hospitalId);
    }
    
    /**
     * 统计医院的失败同步次数
     * 
     * @param hospitalId 医院ID
     * @return 失败同步次数
     */
    public long countFailedSyncs(String hospitalId) {
        return syncLogRepository.countFailedByHospitalId(hospitalId);
    }
    
    /**
     * 统计医院的总同步次数
     * 
     * @param hospitalId 医院ID
     * @return 总同步次数
     */
    public long countTotalSyncs(String hospitalId) {
        return syncLogRepository.countTotalByHospitalId(hospitalId);
    }
    
    /**
     * 计算医院同步成功率
     * 
     * @param hospitalId 医院ID
     * @return 同步成功率（0-100）
     */
    public double calculateSuccessRate(String hospitalId) {
        long total = countTotalSyncs(hospitalId);
        if (total == 0) {
            return 0.0;
        }
        long success = countSuccessSyncs(hospitalId);
        return (success * 100.0) / total;
    }
    
    /**
     * 获取医院同步统计信息
     * 
     * @param hospitalId 医院ID
     * @return 同步统计信息
     */
    public SyncStats getSyncStats(String hospitalId) {
        long total = countTotalSyncs(hospitalId);
        long success = countSuccessSyncs(hospitalId);
        long failed = countFailedSyncs(hospitalId);
        double successRate = calculateSuccessRate(hospitalId);
        
        return new SyncStats(hospitalId, total, success, failed, successRate);
    }
    
    /**
     * 归档旧的同步日志
     * 
     * @param days 保留天数
     * @return 删除的记录数
     */
    @Transactional
    public int archiveOldLogs(int days) {
        Date cutoffDate = new Date(System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000));
        int deletedCount = syncLogRepository.deleteByCreatedAtBefore(cutoffDate);
        log.info("归档同步日志: 删除{}天前的记录{}条", days, deletedCount);
        return deletedCount;
    }
    
    /**
     * 检查医院是否有运行中的同步任务
     * 
     * @param hospitalId 医院ID
     * @return 是否有运行中的同步任务
     */
    public boolean hasRunningSync(String hospitalId) {
        List<SyncLog> runningLogs = syncLogRepository.findByHospitalIdAndStatus(hospitalId, "RUNNING");
        return !runningLogs.isEmpty();
    }
    
    /**
     * 同步统计信息类
     */
    public static class SyncStats {
        private final String hospitalId;
        private final long totalSyncs;
        private final long successSyncs;
        private final long failedSyncs;
        private final double successRate;
        
        public SyncStats(String hospitalId, long totalSyncs, long successSyncs, long failedSyncs, double successRate) {
            this.hospitalId = hospitalId;
            this.totalSyncs = totalSyncs;
            this.successSyncs = successSyncs;
            this.failedSyncs = failedSyncs;
            this.successRate = successRate;
        }
        
        public String getHospitalId() {
            return hospitalId;
        }
        
        public long getTotalSyncs() {
            return totalSyncs;
        }
        
        public long getSuccessSyncs() {
            return successSyncs;
        }
        
        public long getFailedSyncs() {
            return failedSyncs;
        }
        
        public double getSuccessRate() {
            return successRate;
        }
        
        @Override
        public String toString() {
            return String.format("SyncStats{hospitalId='%s', totalSyncs=%d, successSyncs=%d, failedSyncs=%d, successRate=%.2f%%}",
                    hospitalId, totalSyncs, successSyncs, failedSyncs, successRate);
        }
    }
}
