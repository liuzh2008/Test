package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.model.SyncTaskPriority;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 定时任务调度器服务
 * 负责医院数据同步任务的定时调度和管理
 * 
 * @author System
 * @version 3.0
 * @since 2025-12-03
 */
@Service
@Slf4j
public class SyncScheduler {
    
    // 存储医院定时任务信息（医院ID -> 任务信息）
    private static class TaskInfo {
        boolean enabled;
        SyncTaskPriority priority;
        String cronExpression;
        
        TaskInfo(boolean enabled, SyncTaskPriority priority, String cronExpression) {
            this.enabled = enabled;
            this.priority = priority;
            this.cronExpression = cronExpression;
        }
    }
    
    // 存储医院定时任务信息
    private final Map<String, TaskInfo> scheduledTasks = new ConcurrentHashMap<>();
    
    // 存储医院任务执行锁，防止同一医院任务并发执行
    private final Map<String, ReentrantLock> hospitalLocks = new ConcurrentHashMap<>();
    
    // 存储任务状态
    private final Map<String, String> taskStatuses = new ConcurrentHashMap<>();
    
    // 自动同步全局开关
    private final AtomicBoolean autoSyncEnabled = new AtomicBoolean(true);
    
    // 自动同步执行统计
    private final Map<String, Integer> executionStatistics = new ConcurrentHashMap<>();
    
    // 自动同步cron表达式配置
    @Value("${hospital.auto.sync.cron:0 0 * * * ?}")
    private String autoSyncCron;
    
    /**
     * 调度医院同步任务（使用默认优先级MEDIUM）
     * 
     * @param hospitalId 医院ID
     * @param cronExpression cron表达式
     */
    public void scheduleSyncTask(String hospitalId, String cronExpression) {
        scheduleSyncTask(hospitalId, cronExpression, SyncTaskPriority.MEDIUM);
    }
    
    /**
     * 调度医院同步任务（指定优先级）
     * 
     * @param hospitalId 医院ID
     * @param cronExpression cron表达式
     * @param priority 任务优先级
     */
    public void scheduleSyncTask(String hospitalId, String cronExpression, SyncTaskPriority priority) {
        log.info("调度医院 {} 的同步任务，cron表达式: {}，优先级: {}", hospitalId, cronExpression, priority);
        scheduledTasks.put(hospitalId, new TaskInfo(true, priority, cronExpression));
        hospitalLocks.putIfAbsent(hospitalId, new ReentrantLock());
    }
    
    /**
     * 检查是否有已调度的任务
     * 
     * @param hospitalId 医院ID
     * @return 是否有已调度的任务
     */
    public boolean hasScheduledTask(String hospitalId) {
        TaskInfo taskInfo = scheduledTasks.get(hospitalId);
        return taskInfo != null && taskInfo.enabled;
    }
    
    /**
     * 获取任务的优先级
     * 
     * @param hospitalId 医院ID
     * @return 任务优先级，如果任务不存在返回MEDIUM
     */
    public SyncTaskPriority getTaskPriority(String hospitalId) {
        TaskInfo taskInfo = scheduledTasks.get(hospitalId);
        return taskInfo != null ? taskInfo.priority : SyncTaskPriority.MEDIUM;
    }
    
    /**
     * 更新任务优先级
     * 
     * @param hospitalId 医院ID
     * @param priority 新的优先级
     */
    public void updateTaskPriority(String hospitalId, SyncTaskPriority priority) {
        TaskInfo taskInfo = scheduledTasks.get(hospitalId);
        if (taskInfo != null) {
            taskInfo.priority = priority;
            log.info("更新医院 {} 的任务优先级为: {}", hospitalId, priority);
        } else {
            log.warn("医院 {} 的任务不存在，无法更新优先级", hospitalId);
        }
    }
    
    /**
     * 检查是否可以执行任务（并发控制）
     * 
     * @param hospitalId 医院ID
     * @return 是否可以执行任务
     */
    public boolean canExecuteTask(String hospitalId) {
        // 使用computeIfAbsent确保原子性创建锁
        ReentrantLock lock = hospitalLocks.computeIfAbsent(hospitalId, k -> new ReentrantLock());
        
        // 如果锁已经被当前线程持有，返回false（防止同一线程重复执行）
        if (lock.isHeldByCurrentThread()) {
            return false;
        }
        
        // 尝试获取锁，如果获取成功表示可以执行
        return lock.tryLock();
    }
    
    /**
     * 开始任务
     * 
     * @param hospitalId 医院ID
     * @param taskId 任务ID
     */
    public void startTask(String hospitalId, String taskId) {
        log.info("开始医院 {} 的任务 {}", hospitalId, taskId);
        taskStatuses.put(taskId, "RUNNING");
    }
    
    /**
     * 获取任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态
     */
    public String getTaskStatus(String taskId) {
        return taskStatuses.getOrDefault(taskId, "UNKNOWN");
    }
    
    /**
     * 完成任务
     * 
     * @param taskId 任务ID
     */
    public void completeTask(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            log.warn("任务ID为空，无法完成任务");
            return;
        }
        
        // 只有任务状态为RUNNING的任务才能被完成
        if ("RUNNING".equals(taskStatuses.get(taskId))) {
            log.info("完成任务 {}", taskId);
            taskStatuses.put(taskId, "COMPLETED");
            
            // 释放医院锁
            String hospitalId = extractHospitalIdFromTaskId(taskId);
            if (hospitalId != null) {
                ReentrantLock lock = hospitalLocks.get(hospitalId);
                if (lock != null && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } else {
            log.warn("任务 {} 状态不是RUNNING，无法完成。当前状态: {}", taskId, taskStatuses.get(taskId));
        }
    }
    
    /**
     * 取消已调度的任务
     * 
     * @param hospitalId 医院ID
     */
    public void cancelScheduledTask(String hospitalId) {
        log.info("取消医院 {} 的调度任务", hospitalId);
        scheduledTasks.remove(hospitalId);
    }
    
    /**
     * 启用/禁用任务
     * 
     * @param hospitalId 医院ID
     * @param enabled 是否启用
     */
    public void setTaskEnabled(String hospitalId, boolean enabled) {
        TaskInfo taskInfo = scheduledTasks.get(hospitalId);
        if (taskInfo != null) {
            taskInfo.enabled = enabled;
            log.info("{}医院 {} 的任务", enabled ? "启用" : "禁用", hospitalId);
        } else {
            log.warn("医院 {} 的任务不存在，无法{}", hospitalId, enabled ? "启用" : "禁用");
        }
    }
    
    /**
     * 定时执行医院同步任务
     * 使用Spring Scheduled注解实现定时触发
     * 优化调度算法：按照优先级执行任务
     */
    @Scheduled(cron = "${hospital.sync.cron:0 0 2 * * ?}")
    public void scheduledSync() {
        log.info("定时同步任务开始执行");
        
        // 收集所有启用的任务
        List<Map.Entry<String, TaskInfo>> enabledTasks = new ArrayList<>();
        for (Map.Entry<String, TaskInfo> entry : scheduledTasks.entrySet()) {
            if (entry.getValue().enabled) {
                enabledTasks.add(entry);
            }
        }
        
        // 按照优先级排序（优先级高的先执行）
        enabledTasks.sort((a, b) -> {
            SyncTaskPriority priorityA = a.getValue().priority;
            SyncTaskPriority priorityB = b.getValue().priority;
            return Integer.compare(priorityA.getValue(), priorityB.getValue());
        });
        
        // 按照优先级顺序执行任务
        for (Map.Entry<String, TaskInfo> entry : enabledTasks) {
            String hospitalId = entry.getKey();
            executeHospitalSync(hospitalId);
        }
        
        log.info("定时同步任务执行完成，共执行 {} 个任务", enabledTasks.size());
    }
    
    /**
     * 立即执行指定医院的任务（手动触发）
     * 
     * @param hospitalId 医院ID
     * @return 是否成功触发执行
     */
    public boolean triggerImmediateSync(String hospitalId) {
        TaskInfo taskInfo = scheduledTasks.get(hospitalId);
        if (taskInfo == null || !taskInfo.enabled) {
            log.warn("医院 {} 的任务不存在或未启用，无法立即执行", hospitalId);
            return false;
        }
        
        log.info("立即执行医院 {} 的同步任务", hospitalId);
        executeHospitalSync(hospitalId);
        return true;
    }
    
    /**
     * 获取所有已调度任务的信息
     * 
     * @return 任务信息列表
     */
    public List<String> getAllTaskInfo() {
        List<String> taskInfos = new ArrayList<>();
        for (Map.Entry<String, TaskInfo> entry : scheduledTasks.entrySet()) {
            String hospitalId = entry.getKey();
            TaskInfo taskInfo = entry.getValue();
            taskInfos.add(String.format("医院: %s, 状态: %s, 优先级: %s, Cron: %s",
                hospitalId,
                taskInfo.enabled ? "启用" : "禁用",
                taskInfo.priority,
                taskInfo.cronExpression));
        }
        return taskInfos;
    }
    
    /**
     * 执行医院同步
     * 
     * @param hospitalId 医院ID
     */
    private void executeHospitalSync(String hospitalId) {
        // 检查是否可以执行（并发控制）
        if (!canExecuteTask(hospitalId)) {
            log.warn("医院 {} 的任务正在执行中，跳过本次执行", hospitalId);
            return;
        }
        
        String taskId = generateTaskId(hospitalId);
        try {
            startTask(hospitalId, taskId);
            log.info("执行医院 {} 的数据同步", hospitalId);
            // 这里实际应该调用数据同步服务
            // 模拟执行时间
            Thread.sleep(100);
            completeTask(taskId);
        } catch (InterruptedException e) {
            log.error("医院 {} 同步任务被中断", hospitalId, e);
            Thread.currentThread().interrupt();
            taskStatuses.put(taskId, "INTERRUPTED");
        } catch (Exception e) {
            log.error("医院 {} 同步任务执行失败", hospitalId, e);
            taskStatuses.put(taskId, "FAILED");
        }
    }
    
    /**
     * 生成任务ID
     * 
     * @param hospitalId 医院ID
     * @return 任务ID
     */
    private String generateTaskId(String hospitalId) {
        return "sync-" + hospitalId + "-" + System.currentTimeMillis();
    }
    
    /**
     * 从任务ID中提取医院ID
     * 
     * @param taskId 任务ID
     * @return 医院ID
     */
    private String extractHospitalIdFromTaskId(String taskId) {
        if (taskId == null || !taskId.startsWith("sync-")) {
            return null;
        }
        String[] parts = taskId.split("-");
        if (parts.length >= 2) {
            return parts[1];
        }
        return null;
    }
    
    /**
     * 启动自动同步（每小时执行一次）
     * 
     * @return 是否成功启动
     */
    public boolean startAutoSync() {
        boolean wasEnabled = autoSyncEnabled.getAndSet(true);
        if (!wasEnabled) {
            log.info("自动同步已启动，cron表达式: {}", autoSyncCron);
            // 更新执行统计
            executionStatistics.put("autoSyncStarted", executionStatistics.getOrDefault("autoSyncStarted", 0) + 1);
            executionStatistics.put("lastAutoSyncStart", (int) (System.currentTimeMillis() / 1000));
        } else {
            log.info("自动同步已经是启动状态");
        }
        return true;
    }
    
    /**
     * 停止自动同步
     * 
     * @return 是否成功停止
     */
    public boolean stopAutoSync() {
        boolean wasEnabled = autoSyncEnabled.getAndSet(false);
        if (wasEnabled) {
            log.info("自动同步已停止");
            // 更新执行统计
            executionStatistics.put("autoSyncStopped", executionStatistics.getOrDefault("autoSyncStopped", 0) + 1);
            executionStatistics.put("lastAutoSyncStop", (int) (System.currentTimeMillis() / 1000));
        } else {
            log.info("自动同步已经是停止状态");
        }
        return wasEnabled;
    }
    
    /**
     * 获取自动同步状态
     * 
     * @return 自动同步状态信息
     */
    public Map<String, Object> getAutoSyncStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        
        // 基本状态
        status.put("autoSyncEnabled", autoSyncEnabled.get());
        status.put("autoSyncCron", autoSyncCron);
        status.put("currentTime", new Date().toString());
        
        // 任务统计
        status.put("scheduledTasksCount", scheduledTasks.size());
        status.put("enabledTasksCount", getEnabledTasksCount());
        
        // 执行统计
        status.put("executionStatistics", new HashMap<>(executionStatistics));
        
        // 最近任务状态
        List<Map<String, Object>> recentTasks = new ArrayList<>();
        for (Map.Entry<String, String> entry : taskStatuses.entrySet()) {
            if (entry.getKey().startsWith("sync-")) {
                Map<String, Object> taskInfo = new HashMap<>();
                taskInfo.put("taskId", entry.getKey());
                taskInfo.put("status", entry.getValue());
                taskInfo.put("hospitalId", extractHospitalIdFromTaskId(entry.getKey()));
                recentTasks.add(taskInfo);
            }
        }
        status.put("recentTasks", recentTasks);
        
        // 已调度任务详情
        List<Map<String, Object>> scheduledTaskDetails = new ArrayList<>();
        for (Map.Entry<String, TaskInfo> entry : scheduledTasks.entrySet()) {
            Map<String, Object> taskDetail = new HashMap<>();
            taskDetail.put("hospitalId", entry.getKey());
            taskDetail.put("enabled", entry.getValue().enabled);
            taskDetail.put("priority", entry.getValue().priority.toString());
            taskDetail.put("cronExpression", entry.getValue().cronExpression);
            scheduledTaskDetails.add(taskDetail);
        }
        status.put("scheduledTaskDetails", scheduledTaskDetails);
        
        return status;
    }
    
    /**
     * 获取启用的任务数量
     * 
     * @return 启用的任务数量
     */
    private int getEnabledTasksCount() {
        int count = 0;
        for (TaskInfo taskInfo : scheduledTasks.values()) {
            if (taskInfo.enabled) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 每小时自动同步任务
     * 使用配置的cron表达式执行，默认每小时执行一次
     */
    @Scheduled(cron = "${hospital.auto.sync.cron:0 0 * * * ?}")
    public void hourlyAutoSync() {
        if (!autoSyncEnabled.get()) {
            log.debug("自动同步已禁用，跳过本次执行");
            return;
        }
        
        log.info("开始每小时自动同步任务");
        
        // 收集所有启用的任务
        List<Map.Entry<String, TaskInfo>> enabledTasks = new ArrayList<>();
        for (Map.Entry<String, TaskInfo> entry : scheduledTasks.entrySet()) {
            if (entry.getValue().enabled) {
                enabledTasks.add(entry);
            }
        }
        
        if (enabledTasks.isEmpty()) {
            log.info("没有启用的同步任务，跳过自动同步");
            return;
        }
        
        // 按照优先级排序（优先级高的先执行）
        enabledTasks.sort((a, b) -> {
            SyncTaskPriority priorityA = a.getValue().priority;
            SyncTaskPriority priorityB = b.getValue().priority;
            return Integer.compare(priorityA.getValue(), priorityB.getValue());
        });
        
        // 记录执行统计
        executionStatistics.put("hourlyAutoSyncExecutions", 
            executionStatistics.getOrDefault("hourlyAutoSyncExecutions", 0) + 1);
        executionStatistics.put("lastHourlyAutoSync", (int) (System.currentTimeMillis() / 1000));
        
        // 按照优先级顺序执行任务
        int executedCount = 0;
        for (Map.Entry<String, TaskInfo> entry : enabledTasks) {
            String hospitalId = entry.getKey();
            executeHospitalSync(hospitalId);
            executedCount++;
        }
        
        log.info("每小时自动同步任务执行完成，共执行 {} 个任务", executedCount);
    }
    
    /**
     * 立即执行所有启用的医院同步任务
     * 
     * @return 执行结果
     */
    public Map<String, Object> triggerAllEnabledSync() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> executionResults = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        // 收集所有启用的任务
        List<Map.Entry<String, TaskInfo>> enabledTasks = new ArrayList<>();
        for (Map.Entry<String, TaskInfo> entry : scheduledTasks.entrySet()) {
            if (entry.getValue().enabled) {
                enabledTasks.add(entry);
            }
        }
        
        // 按照优先级排序
        enabledTasks.sort((a, b) -> {
            SyncTaskPriority priorityA = a.getValue().priority;
            SyncTaskPriority priorityB = b.getValue().priority;
            return Integer.compare(priorityA.getValue(), priorityB.getValue());
        });
        
        // 执行所有启用的任务
        for (Map.Entry<String, TaskInfo> entry : enabledTasks) {
            String hospitalId = entry.getKey();
            Map<String, Object> taskResult = new HashMap<>();
            taskResult.put("hospitalId", hospitalId);
            taskResult.put("priority", entry.getValue().priority.toString());
            
            try {
                boolean triggered = triggerImmediateSync(hospitalId);
                taskResult.put("success", triggered);
                taskResult.put("message", triggered ? "任务触发成功" : "任务触发失败");
                if (triggered) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                taskResult.put("success", false);
                taskResult.put("message", "任务触发异常: " + e.getMessage());
                failureCount++;
            }
            
            executionResults.add(taskResult);
        }
        
        result.put("totalTasks", enabledTasks.size());
        result.put("successCount", successCount);
        result.put("failureCount", failureCount);
        result.put("executionResults", executionResults);
        result.put("timestamp", new Date().toString());
        
        return result;
    }
}
