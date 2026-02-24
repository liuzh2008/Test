package com.example.medaiassistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * DRG分析输入快照实体类
 * 用于存储DRG分析请求的快照，避免重复分析相同输入
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-19
 */
@Entity
@Table(name = "drg_analysis_input_snapshot")
public class DrgAnalysisInputSnapshot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dais_seq")
    @SequenceGenerator(name = "dais_seq", sequenceName = "drg_analysis_input_snapshot_seq", allocationSize = 1)
    @Column(name = "snapshot_id")
    private Long snapshotId;
    
    /**
     * 患者ID
     */
    @Column(name = "patient_id", length = 50, nullable = false)
    private String patientId;
    
    /**
     * 诊断ID集合（JSON数组）
     */
    @Column(name = "diagnosis_ids_json", columnDefinition = "CLOB")
    private String diagnosisIdsJson;
    
    /**
     * 手术ID集合（JSON数组）
     */
    @Column(name = "surgery_ids_json", columnDefinition = "CLOB")
    private String surgeryIdsJson;
    
    /**
     * 诊断集合大小
     */
    @Column(name = "diag_count", nullable = false)
    private Integer diagCount = 0;
    
    /**
     * 手术集合大小
     */
    @Column(name = "proc_count", nullable = false)
    private Integer procCount = 0;
    
    /**
     * 生成快照时源表诊断条目数
     */
    @Column(name = "last_source_diag_count", nullable = false)
    private Integer lastSourceDiagCount = 0;
    
    /**
     * 生成快照时源表手术条目数
     */
    @Column(name = "last_source_proc_count", nullable = false)
    private Integer lastSourceProcCount = 0;
    
    /**
     * DRG目录快照版本标识
     */
    @Column(name = "catalog_version", length = 64)
    private String catalogVersion;
    
    /**
     * 关联Prompt记录ID（可选）
     */
    @Column(name = "prompt_id")
    private Long promptId;
    
    /**
     * 关联PromptResult记录ID（可选）
     */
    @Column(name = "prompt_result_id")
    private Long promptResultId;
    
    /**
     * 快照创建时间
     */
    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;
    
    /**
     * 版本（乐观锁）
     */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;
    
    /**
     * 软删除标记：0未删除，1已删除
     */
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;
    
    /**
     * 默认构造函数
     */
    public DrgAnalysisInputSnapshot() {
        this.createdTime = LocalDateTime.now();
    }
    
    /**
     * 带参数构造函数
     * 
     * @param patientId 患者ID
     * @param diagnosisIdsJson 诊断ID集合（JSON格式）
     * @param surgeryIdsJson 手术ID集合（JSON格式）
     * @param diagCount 诊断集合大小
     * @param procCount 手术集合大小
     * @param lastSourceDiagCount 源表诊断条目数
     * @param lastSourceProcCount 源表手术条目数
     * @param catalogVersion DRG目录版本
     */
    public DrgAnalysisInputSnapshot(String patientId, String diagnosisIdsJson, String surgeryIdsJson,
                                   Integer diagCount, Integer procCount, 
                                   Integer lastSourceDiagCount, Integer lastSourceProcCount,
                                   String catalogVersion) {
        this();
        this.patientId = patientId;
        this.diagnosisIdsJson = diagnosisIdsJson;
        this.surgeryIdsJson = surgeryIdsJson;
        this.diagCount = diagCount;
        this.procCount = procCount;
        this.lastSourceDiagCount = lastSourceDiagCount;
        this.lastSourceProcCount = lastSourceProcCount;
        this.catalogVersion = catalogVersion;
    }
    
    // Getter和Setter方法
    
    /**
     * 获取主键ID
     * 
     * @return 主键ID
     */
    public Long getSnapshotId() {
        return snapshotId;
    }
    
    /**
     * 设置主键ID
     * 
     * @param snapshotId 主键ID
     */
    public void setSnapshotId(Long snapshotId) {
        this.snapshotId = snapshotId;
    }
    
    /**
     * 获取患者ID
     * 
     * @return 患者ID
     */
    public String getPatientId() {
        return patientId;
    }
    
    /**
     * 设置患者ID
     * 
     * @param patientId 患者ID
     */
    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }
    
    /**
     * 获取诊断ID集合（JSON格式）
     * 
     * @return 诊断ID集合
     */
    public String getDiagnosisIdsJson() {
        return diagnosisIdsJson;
    }
    
    /**
     * 设置诊断ID集合（JSON格式）
     * 
     * @param diagnosisIdsJson 诊断ID集合
     */
    public void setDiagnosisIdsJson(String diagnosisIdsJson) {
        this.diagnosisIdsJson = diagnosisIdsJson;
    }
    
    /**
     * 获取手术ID集合（JSON格式）
     * 
     * @return 手术ID集合
     */
    public String getSurgeryIdsJson() {
        return surgeryIdsJson;
    }
    
    /**
     * 设置手术ID集合（JSON格式）
     * 
     * @param surgeryIdsJson 手术ID集合
     */
    public void setSurgeryIdsJson(String surgeryIdsJson) {
        this.surgeryIdsJson = surgeryIdsJson;
    }
    
    /**
     * 获取诊断集合大小
     * 
     * @return 诊断集合大小
     */
    public Integer getDiagCount() {
        return diagCount;
    }
    
    /**
     * 设置诊断集合大小
     * 
     * @param diagCount 诊断集合大小
     */
    public void setDiagCount(Integer diagCount) {
        this.diagCount = diagCount;
    }
    
    /**
     * 获取手术集合大小
     * 
     * @return 手术集合大小
     */
    public Integer getProcCount() {
        return procCount;
    }
    
    /**
     * 设置手术集合大小
     * 
     * @param procCount 手术集合大小
     */
    public void setProcCount(Integer procCount) {
        this.procCount = procCount;
    }
    
    /**
     * 获取源表诊断条目数
     * 
     * @return 源表诊断条目数
     */
    public Integer getLastSourceDiagCount() {
        return lastSourceDiagCount;
    }
    
    /**
     * 设置源表诊断条目数
     * 
     * @param lastSourceDiagCount 源表诊断条目数
     */
    public void setLastSourceDiagCount(Integer lastSourceDiagCount) {
        this.lastSourceDiagCount = lastSourceDiagCount;
    }
    
    /**
     * 获取源表手术条目数
     * 
     * @return 源表手术条目数
     */
    public Integer getLastSourceProcCount() {
        return lastSourceProcCount;
    }
    
    /**
     * 设置源表手术条目数
     * 
     * @param lastSourceProcCount 源表手术条目数
     */
    public void setLastSourceProcCount(Integer lastSourceProcCount) {
        this.lastSourceProcCount = lastSourceProcCount;
    }
    
    /**
     * 获取DRG目录版本
     * 
     * @return DRG目录版本
     */
    public String getCatalogVersion() {
        return catalogVersion;
    }
    
    /**
     * 设置DRG目录版本
     * 
     * @param catalogVersion DRG目录版本
     */
    public void setCatalogVersion(String catalogVersion) {
        this.catalogVersion = catalogVersion;
    }
    
    /**
     * 获取关联Prompt记录ID
     * 
     * @return Prompt记录ID
     */
    public Long getPromptId() {
        return promptId;
    }
    
    /**
     * 设置关联Prompt记录ID
     * 
     * @param promptId Prompt记录ID
     */
    public void setPromptId(Long promptId) {
        this.promptId = promptId;
    }
    
    /**
     * 获取关联PromptResult记录ID
     * 
     * @return PromptResult记录ID
     */
    public Long getPromptResultId() {
        return promptResultId;
    }
    
    /**
     * 设置关联PromptResult记录ID
     * 
     * @param promptResultId PromptResult记录ID
     */
    public void setPromptResultId(Long promptResultId) {
        this.promptResultId = promptResultId;
    }
    
    /**
     * 获取创建时间
     * 
     * @return 创建时间
     */
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }
    
    /**
     * 设置创建时间
     * 
     * @param createdTime 创建时间
     */
    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
    
    /**
     * 获取版本号
     * 
     * @return 版本号
     */
    public Integer getVersion() {
        return version;
    }
    
    /**
     * 设置版本号
     * 
     * @param version 版本号
     */
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    /**
     * 获取删除标记
     * 
     * @return 删除标记
     */
    public Boolean getDeleted() {
        return deleted;
    }
    
    /**
     * 设置删除标记
     * 
     * @param deleted 删除标记
     */
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
    
    @Override
    public String toString() {
        return "DrgAnalysisInputSnapshot{" +
                "snapshotId=" + snapshotId +
                ", patientId='" + patientId + '\'' +
                ", diagCount=" + diagCount +
                ", procCount=" + procCount +
                ", catalogVersion='" + catalogVersion + '\'' +
                ", createdTime=" + createdTime +
                ", deleted=" + deleted +
                '}';
    }
}
