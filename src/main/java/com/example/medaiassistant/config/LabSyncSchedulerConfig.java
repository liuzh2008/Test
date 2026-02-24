package com.example.medaiassistant.config;

import com.example.medaiassistant.hospital.model.SyncTaskPriority;
import com.example.medaiassistant.hospital.service.SyncScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * LIS检验结果同步调度配置
 * 在应用启动时自动调度LIS检验结果同步任务
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-12
 */
@Configuration
@Slf4j
public class LabSyncSchedulerConfig {
    
    /**
     * LIS检验结果同步cron表达式配置
     * 默认：每小时执行一次（0分0秒）
     */
    @Value("${lab.sync.cron:0 0 * * * ?}")
    private String labSyncCron;
    
    /**
     * LIS检验结果同步任务优先级
     * 默认：MEDIUM（中等优先级）
     */
    @Value("${lab.sync.priority:MEDIUM}")
    private SyncTaskPriority labSyncPriority;
    
    /**
     * 是否启用LIS检验结果同步
     * 默认：true（启用）
     */
    @Value("${lab.sync.enabled:true}")
    private boolean labSyncEnabled;
    
    /**
     * 默认医院ID配置
     */
    @Value("${hospital.default.id:hospital-Local}")
    private String defaultHospitalId;
    
    private final SyncScheduler syncScheduler;
    
    public LabSyncSchedulerConfig(SyncScheduler syncScheduler) {
        this.syncScheduler = syncScheduler;
    }
    
    /**
     * 应用启动完成后调度LIS检验结果同步任务
     */
    @EventListener(ApplicationReadyEvent.class)
    public void scheduleLabSyncTasks() {
        if (!labSyncEnabled) {
            log.info("LIS检验结果同步已禁用，跳过任务调度");
            return;
        }
        
        log.info("开始调度LIS检验结果同步任务");
        
        try {
            // 调度默认医院的LIS检验结果同步任务
            scheduleHospitalLabSyncTask(defaultHospitalId);
            
            // 可以根据需要调度其他医院的LIS检验结果同步任务
            // scheduleHospitalLabSyncTask("hospital-001");
            
            log.info("LIS检验结果同步任务调度完成 - 默认医院ID: {}", defaultHospitalId);
            
        } catch (Exception e) {
            log.error("调度LIS检验结果同步任务失败", e);
        }
    }
    
    /**
     * 调度指定医院的LIS检验结果同步任务
     * 
     * @param hospitalId 医院ID
     */
    private void scheduleHospitalLabSyncTask(String hospitalId) {
        try {
            // 生成医院特定的LIS同步任务ID
            String labSyncTaskId = "lab-sync-" + hospitalId;
            
            // 调度任务
            syncScheduler.scheduleSyncTask(labSyncTaskId, labSyncCron, labSyncPriority);
            
            log.info("已调度医院 {} 的LIS检验结果同步任务 - 任务ID: {}, cron表达式: {}, 优先级: {}", 
                hospitalId, labSyncTaskId, labSyncCron, labSyncPriority);
            
        } catch (Exception e) {
            log.error("调度医院 {} 的LIS检验结果同步任务失败", hospitalId, e);
        }
    }
    
    /**
     * 获取LIS检验结果同步配置信息
     */
    public String getLabSyncConfigInfo() {
        return String.format("LIS检验结果同步配置 - cron: %s, 优先级: %s, 启用: %s", 
            labSyncCron, labSyncPriority, labSyncEnabled);
    }
}
