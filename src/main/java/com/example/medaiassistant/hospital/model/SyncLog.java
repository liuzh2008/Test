package com.example.medaiassistant.hospital.model;

import lombok.Data;
import jakarta.persistence.*;
import java.util.Date;

/**
 * 同步日志实体
 * 用于记录医院数据同步任务的执行日志
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-04
 */
@Entity
@Table(name = "SYNC_LOG")
@Data
public class SyncLog {
    
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 医院ID
     */
    @Column(name = "HOSPITAL_ID", nullable = false, length = 50)
    private String hospitalId;
    
    /**
     * 同步类型：FULL（全量）、INCREMENTAL（增量）、MANUAL（手动）
     */
    @Column(name = "SYNC_TYPE", nullable = false, length = 20)
    private String syncType;
    
    /**
     * 同步开始时间
     */
    @Column(name = "START_TIME", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;
    
    /**
     * 同步结束时间
     */
    @Column(name = "END_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;
    
    /**
     * 同步状态：RUNNING（运行中）、SUCCESS（成功）、FAILED（失败）
     */
    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;
    
    /**
     * 同步的记录数
     */
    @Column(name = "RECORDS_SYNCED")
    private Integer recordsSynced;
    
    /**
     * 错误信息（如果同步失败）
     */
    @Column(name = "ERROR_MESSAGE", length = 2000)
    private String errorMessage;
    
    /**
     * 创建时间
     */
    @Column(name = "CREATED_AT", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    /**
     * 更新时间
     */
    @Column(name = "UPDATED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
    
    /**
     * 默认构造函数
     */
    public SyncLog() {
        this.createdAt = new Date();
    }
    
    /**
     * 创建同步日志（运行中状态）
     * 
     * @param hospitalId 医院ID
     * @param syncType 同步类型
     * @return 同步日志
     */
    public static SyncLog createRunning(String hospitalId, String syncType) {
        SyncLog log = new SyncLog();
        log.setHospitalId(hospitalId);
        log.setSyncType(syncType);
        log.setStartTime(new Date());
        log.setStatus("RUNNING");
        log.setCreatedAt(new Date());
        return log;
    }
    
    /**
     * 标记为成功
     * 
     * @param recordsSynced 同步的记录数
     */
    public void markSuccess(int recordsSynced) {
        this.setStatus("SUCCESS");
        this.setRecordsSynced(recordsSynced);
        this.setEndTime(new Date());
        this.setUpdatedAt(new Date());
    }
    
    /**
     * 标记为失败
     * 
     * @param errorMessage 错误信息
     */
    public void markFailed(String errorMessage) {
        this.setStatus("FAILED");
        this.setErrorMessage(errorMessage);
        this.setEndTime(new Date());
        this.setUpdatedAt(new Date());
    }
    
    /**
     * 获取执行时间（毫秒）
     * 
     * @return 执行时间，如果未结束则返回null
     */
    public Long getExecutionTimeMs() {
        if (startTime != null && endTime != null) {
            return endTime.getTime() - startTime.getTime();
        }
        return null;
    }
    
    /**
     * 检查是否已完成（成功或失败）
     * 
     * @return 是否已完成
     */
    public boolean isCompleted() {
        return "SUCCESS".equals(status) || "FAILED".equals(status);
    }
    
    /**
     * 检查是否正在运行
     * 
     * @return 是否正在运行
     */
    public boolean isRunning() {
        return "RUNNING".equals(status);
    }
    
    /**
     * 检查是否成功
     * 
     * @return 是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }
    
    /**
     * 检查是否失败
     * 
     * @return 是否失败
     */
    public boolean isFailed() {
        return "FAILED".equals(status);
    }
}
