package com.example.medaiassistant.model;

import jakarta.persistence.*;
import lombok.Data;
import java.sql.Timestamp;

/**
 * DRG分析结果实体类
 * 对应数据库表: drg_analysis_results
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-16
 */
@Entity
@Table(name = "DRG_ANALYSIS_RESULTS")
@Data
public class DrgAnalysisResult {
    
    /**
     * 分析结果ID，主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "drg_analysis_results_seq")
    @SequenceGenerator(name = "drg_analysis_results_seq", sequenceName = "drg_analysis_results_seq", allocationSize = 1)
    @Column(name = "RESULT_ID")
    private Long resultId;
    
    /**
     * 患者ID，关联patients表
     */
    @Column(name = "PATIENT_ID", length = 50, nullable = false)
    private String patientId;

    /**
     * 匹配的DRG ID，关联drgs表
     */
    @Column(name = "DRG_ID", nullable = false)
    private Long drgId;

    /**
     * 匹配的诊断信息，JSON格式
     */
    @Column(name = "MAIN_DIAGNOSES", columnDefinition = "CLOB")
    private String mainDiagnoses;

    /**
     * 匹配的手术信息，JSON格式
     */
    @Column(name = "MAIN_PROCEDURES", columnDefinition = "CLOB")
    private String mainProcedures;

    /**
     * 用户选择的MCC类型：MCC/CC/NONE
     */
    @Column(name = "USER_SELECTED_MCC_TYPE", length = 10)
    private String userSelectedMccType = "NONE";
    
    /**
     * MCC类型枚举
     */
    public enum MccType {
        MCC("MCC", "严重并发症"),
        CC("CC", "一般并发症"),
        NONE("NONE", "无并发症");
        
        private final String code;
        private final String description;
        
        MccType(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static MccType fromCode(String code) {
            for (MccType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return NONE;
        }
        
        public static boolean isValid(String code) {
            for (MccType type : values()) {
                if (type.code.equals(code)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 最终确定的DRG编码
     */
    @Column(name = "FINAL_DRG_CODE", length = 200)
    private String finalDrgCode;

    /**
     * 首次保存时间
     */
    @Column(name = "CREATED_TIME")
    private Timestamp createdTime;

    /**
     * 软删除标志：0-未删除，1-已删除
     */
    @Column(name = "DELETED")
    private Integer deleted = 0;

    /**
     * Prompt记录ID
     */
    @Column(name = "PROMPT_ID")
    private Long promptId;

    /**
     * PromptResult记录ID
     */
    @Column(name = "PROMPT_RESULT_ID")
    private Long promptResultId;

    /**
     * 主要诊断
     */
    @Column(name = "PRIMARY_DIAGNOSIS", length = 500)
    private String primaryDiagnosis;

    /**
     * 主要手术（可为空）
     */
    @Column(name = "PRIMARY_PROCEDURE", length = 500)
    private String primaryProcedure;
    
    /**
     * 默认构造函数
     */
    public DrgAnalysisResult() {
        this.createdTime = new Timestamp(System.currentTimeMillis());
    }
    
    /**
     * 带参数构造函数
     */
    public DrgAnalysisResult(String patientId, Long drgId, String finalDrgCode) {
        this();
        this.patientId = patientId;
        this.drgId = drgId;
        this.finalDrgCode = finalDrgCode;
    }
    
    /**
     * 判断是否已删除
     */
    public boolean isDeleted() {
        return deleted != null && deleted == 1;
    }
    
    /**
     * 设置删除状态
     */
    public void setDeleted(boolean deleted) {
        this.deleted = deleted ? 1 : 0;
    }
    
    /**
     * 判断用户是否已选择MCC类型
     */
    public boolean hasUserSelectedMccType() {
        return userSelectedMccType != null && !MccType.NONE.getCode().equals(userSelectedMccType);
    }
    
    /**
     * 获取用户选择的MCC类型描述
     */
    public String getUserSelectedMccTypeDescription() {
        MccType type = MccType.fromCode(userSelectedMccType);
        return type.getDescription();
    }
    
    /**
     * 验证MCC类型是否有效
     */
    public boolean isValidMccType() {
        return MccType.isValid(userSelectedMccType);
    }
    
    /**
     * 获取MCC类型枚举
     */
    public MccType getMccType() {
        return MccType.fromCode(userSelectedMccType);
    }
    
    /**
     * 设置MCC类型枚举
     */
    public void setMccType(MccType mccType) {
        this.userSelectedMccType = mccType.getCode();
    }
    
    @Override
    public String toString() {
        return "DrgAnalysisResult{" +
                "resultId=" + resultId +
                ", patientId='" + patientId + '\'' +
                ", drgId=" + drgId +
                ", finalDrgCode='" + finalDrgCode + '\'' +
                ", createdTime=" + createdTime +
                '}';
    }
}
