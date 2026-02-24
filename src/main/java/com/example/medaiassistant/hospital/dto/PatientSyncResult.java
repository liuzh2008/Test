package com.example.medaiassistant.hospital.dto;

import lombok.Data;
import java.util.Date;

/**
 * 病人数据同步结果DTO
 * 包含同步过程的统计信息和状态信息
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-08
 */
@Data
public class PatientSyncResult {
    private String hospitalId;
    private String department;
    private Date syncTime;
    private long executionTime; // 执行时间（毫秒）
    
    // 统计信息
    private int oraclePatientCount;    // Oracle中的病人数
    private int mainServerPatientCount; // 主服务器中的病人数
    private int addedCount;            // 新增病人数
    private int updatedCount;          // 更新病人数
    private int dischargedCount;       // 标记出院数
    
    // 状态信息
    private boolean success;
    private String errorMessage;
    
    public PatientSyncResult() {
        this.syncTime = new Date();
    }
    
    public PatientSyncResult(String hospitalId, String department) {
        this();
        this.hospitalId = hospitalId;
        this.department = department;
    }
    
    /**
     * 获取总处理记录数
     */
    public int getTotalProcessed() {
        return addedCount + updatedCount + dischargedCount;
    }
    
    /**
     * 获取同步摘要
     */
    public String getSummary() {
        return String.format("同步完成 - 新增: %d, 更新: %d, 出院: %d, 总处理: %d",
            addedCount, updatedCount, dischargedCount, getTotalProcessed());
    }
}
