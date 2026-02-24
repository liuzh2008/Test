package com.example.medaiassistant.model;

import lombok.Data;

/**
 * 患者诊断信息实体类
 * 用于MCC预筛选的患者诊断数据
 * 
 * @author MedAI Assistant Team
 * @since 2025-11-09
 */
@Data
public class PatientDiagnosis {
    
    /**
     * 诊断ICD编码
     */
    private String icdCode;
    
    /**
     * 诊断名称
     */
    private String diagnosisName;
    
    /**
     * 默认构造函数
     */
    public PatientDiagnosis() {
    }
    
    /**
     * 带参数构造函数
     */
    public PatientDiagnosis(String icdCode, String diagnosisName) {
        this.icdCode = icdCode;
        this.diagnosisName = diagnosisName;
    }
    
    /**
     * 判断是否有ICD编码
     */
    public boolean hasIcdCode() {
        return icdCode != null && !icdCode.trim().isEmpty();
    }
    
    /**
     * 判断是否有诊断名称
     */
    public boolean hasDiagnosisName() {
        return diagnosisName != null && !diagnosisName.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return "PatientDiagnosis{" +
                "icdCode='" + icdCode + '\'' +
                ", diagnosisName='" + diagnosisName + '\'' +
                '}';
    }
}
