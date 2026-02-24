package com.example.medaiassistant.dto;

import lombok.Data;
import java.util.List;

/**
 * 快照生成请求DTO
 * 
 * 用于接收快照生成请求的参数
 * 
 * @author Cline
 * @since 2025-10-24
 */
@Data
public class SnapshotRequestDTO {
    
    /**
     * 患者ID
     */
    private String patientId;
    
    /**
     * 诊断ID列表
     */
    private List<String> diagnosisIds;
    
    /**
     * 手术ID列表
     */
    private List<String> surgeryIds;
    
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
     * 支持的值: API_CALL, TIMER_TASK, MANUAL, PROMPT_TRIGGER
     */
    private String operationSource;
    
    /**
     * 是否强制重算
     */
    private boolean forceReanalyze;
    
    /**
     * 默认构造函数
     */
    public SnapshotRequestDTO() {
        this.lastSourceDiagCount = 0;
        this.lastSourceProcCount = 0;
        this.forceReanalyze = false;
    }
    
    /**
     * 带参数的构造函数
     */
    public SnapshotRequestDTO(String patientId, List<String> diagnosisIds, List<String> surgeryIds, 
                             String catalogVersion, String operationSource) {
        this();
        this.patientId = patientId;
        this.diagnosisIds = diagnosisIds;
        this.surgeryIds = surgeryIds;
        this.catalogVersion = catalogVersion;
        this.operationSource = operationSource;
    }
}
