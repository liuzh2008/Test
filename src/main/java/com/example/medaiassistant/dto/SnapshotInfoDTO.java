package com.example.medaiassistant.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 快照信息DTO
 * 
 * 用于返回快照的详细信息
 * 
 * @author Cline
 * @since 2025-10-24
 */
@Data
public class SnapshotInfoDTO {
    
    /**
     * 快照ID
     */
    private Long snapshotId;
    
    /**
     * 患者ID
     */
    private String patientId;
    
    /**
     * 诊断ID列表JSON
     */
    private String diagnosisIdsJson;
    
    /**
     * 手术ID列表JSON
     */
    private String surgeryIdsJson;
    
    /**
     * 目录版本
     */
    private String catalogVersion;
    
    /**
     * 上次源诊断数量
     */
    private Integer lastSourceDiagCount;
    
    /**
     * 上次源手术数量
     */
    private Integer lastSourceProcCount;
    
    /**
     * 操作来源
     */
    private String operationSource;
    
    /**
     * 是否强制重算
     */
    private Boolean forceReanalyze;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 是否已删除
     */
    private Boolean deleted;
    
    /**
     * 默认构造函数
     */
    public SnapshotInfoDTO() {
        this.lastSourceDiagCount = 0;
        this.lastSourceProcCount = 0;
        this.forceReanalyze = false;
        this.deleted = false;
    }
    
    /**
     * 带参数的构造函数
     */
    public SnapshotInfoDTO(Long snapshotId, String patientId, String catalogVersion, 
                          String operationSource) {
        this();
        this.snapshotId = snapshotId;
        this.patientId = patientId;
        this.catalogVersion = catalogVersion;
        this.operationSource = operationSource;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
