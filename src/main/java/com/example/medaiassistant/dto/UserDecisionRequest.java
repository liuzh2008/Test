package com.example.medaiassistant.dto;

import com.example.medaiassistant.enums.MccType;

/**
 * 用户决策请求DTO
 * 包含用户选择MCC类别所需的信息
 * 
 * @author MedAI Assistant Team
 * @since 2025-11-10
 */
public class UserDecisionRequest {
    
    /**
     * 分析结果ID
     */
    private Long resultId;
    
    /**
     * 患者ID
     */
    private String patientId;
    
    /**
     * 用户选择的MCC类型
     */
    private MccType selectedMccType;
    
    /**
     * 操作者
     */
    private String operator;
    
    /**
     * 默认构造函数
     */
    public UserDecisionRequest() {
    }
    
    /**
     * 带参数构造函数
     */
    public UserDecisionRequest(Long resultId, String patientId, MccType selectedMccType, String operator) {
        this.resultId = resultId;
        this.patientId = patientId;
        this.selectedMccType = selectedMccType;
        this.operator = operator;
    }
    
    // Getter和Setter方法
    
    public Long getResultId() {
        return resultId;
    }
    
    public void setResultId(Long resultId) {
        this.resultId = resultId;
    }
    
    public String getPatientId() {
        return patientId;
    }
    
    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }
    
    public MccType getSelectedMccType() {
        return selectedMccType;
    }
    
    public void setSelectedMccType(MccType selectedMccType) {
        this.selectedMccType = selectedMccType;
    }
    
    public String getOperator() {
        return operator;
    }
    
    public void setOperator(String operator) {
        this.operator = operator;
    }
    
    @Override
    public String toString() {
        return "UserDecisionRequest{" +
                "resultId=" + resultId +
                ", patientId='" + patientId + '\'' +
                ", selectedMccType=" + selectedMccType +
                ", operator='" + operator + '\'' +
                '}';
    }
}
