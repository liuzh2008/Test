package com.example.medaiassistant.hospital.model;

import lombok.Data;

import java.util.Date;

/**
 * 数据同步结果
 * 用于表示数据同步操作的结果
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-04
 */
@Data
public class SyncResult {
    
    /**
     * 同步是否成功
     */
    private boolean success;
    
    /**
     * 同步任务ID
     */
    private String taskId;
    
    /**
     * 医院ID
     */
    private String hospitalId;
    
    /**
     * 同步类型：FULL（全量）或 INCREMENTAL（增量）
     */
    private String syncType;
    
    /**
     * 同步开始时间
     */
    private Date startTime;
    
    /**
     * 同步结束时间
     */
    private Date endTime;
    
    /**
     * 同步的记录数
     */
    private Integer recordsSynced;
    
    /**
     * 错误信息（如果同步失败）
     */
    private String errorMessage;
    
    /**
     * 执行时间（毫秒）
     */
    private Long executionTimeMs;
    
    /**
     * 创建成功的同步结果
     * 
     * @param taskId 任务ID
     * @param hospitalId 医院ID
     * @param syncType 同步类型
     * @param recordsSynced 同步的记录数
     * @return 成功的同步结果
     */
    public static SyncResult success(String taskId, String hospitalId, String syncType, Integer recordsSynced) {
        SyncResult result = new SyncResult();
        result.setSuccess(true);
        result.setTaskId(taskId);
        result.setHospitalId(hospitalId);
        result.setSyncType(syncType);
        result.setRecordsSynced(recordsSynced);
        result.setStartTime(new Date());
        result.setEndTime(new Date());
        result.setExecutionTimeMs(System.currentTimeMillis() - result.getStartTime().getTime());
        return result;
    }
    
    /**
     * 创建失败的同步结果
     * 
     * @param taskId 任务ID
     * @param hospitalId 医院ID
     * @param syncType 同步类型
     * @param errorMessage 错误信息
     * @return 失败的同步结果
     */
    public static SyncResult error(String taskId, String hospitalId, String syncType, String errorMessage) {
        SyncResult result = new SyncResult();
        result.setSuccess(false);
        result.setTaskId(taskId);
        result.setHospitalId(hospitalId);
        result.setSyncType(syncType);
        result.setErrorMessage(errorMessage);
        result.setStartTime(new Date());
        result.setEndTime(new Date());
        result.setExecutionTimeMs(System.currentTimeMillis() - result.getStartTime().getTime());
        return result;
    }
    
    /**
     * 创建简单的成功结果（用于测试）
     * 
     * @return 简单的成功结果
     */
    public static SyncResult simpleSuccess() {
        return success("test-task", "test-hospital", "INCREMENTAL", 0);
    }
}
