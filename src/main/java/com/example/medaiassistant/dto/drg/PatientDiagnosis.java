package com.example.medaiassistant.dto.drg;

import lombok.Data;

/**
 * 患者诊断条目数据传输对象
 * 
 * 用于表示患者的单个诊断记录，包含ICD编码和诊断名称
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-21
 */
@Data
public class PatientDiagnosis {
    
    /**
     * ICD-10诊断编码
     * 示例: "I48.000", "I10", "E11.900"
     */
    private final String icdCode;
    
    /**
     * 诊断名称
     * 示例: "心房颤动", "原发性高血压", "2型糖尿病"
     */
    private final String diagnosisName;
    
    /**
     * 构造函数
     * 
     * @param icdCode ICD-10诊断编码
     * @param diagnosisName 诊断名称
     */
    public PatientDiagnosis(String icdCode, String diagnosisName) {
        this.icdCode = icdCode;
        this.diagnosisName = diagnosisName;
    }
    
    /**
     * 获取ICD编码，如果为空则返回空字符串
     */
    public String getIcdCodeSafe() {
        return icdCode != null ? icdCode : "";
    }
    
    /**
     * 获取诊断名称，如果为空则返回空字符串
     */
    public String getDiagnosisNameSafe() {
        return diagnosisName != null ? diagnosisName : "";
    }
    
    @Override
    public String toString() {
        return "PatientDiagnosis{" +
                "icdCode='" + icdCode + '\'' +
                ", diagnosisName='" + diagnosisName + '\'' +
                '}';
    }
}
