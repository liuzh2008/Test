package com.example.medaiassistant.dto;

import java.util.Date;

public class ReplaceDiagnosisDTO {
    private Integer oldDiagnosisId; // 原记录ID（用于软删除）
    private String patientId;
    private Integer diagnosisType;
    private String icd10Code;
    private String diagnosisText;
    private String diagnosedBy;
    private Date diagnosisTime;
    private Integer isPrimary;
    private Integer parentId;
    private Integer statusFlag;
    private Integer modificationType;
    private String addReason;
    private Integer diagnosisIndex;

    // Getters and Setters
    public Integer getOldDiagnosisId() {
        return oldDiagnosisId;
    }

    public void setOldDiagnosisId(Integer oldDiagnosisId) {
        this.oldDiagnosisId = oldDiagnosisId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public Integer getDiagnosisType() {
        return diagnosisType;
    }

    public void setDiagnosisType(Integer diagnosisType) {
        this.diagnosisType = diagnosisType;
    }

    public String getIcd10Code() {
        return icd10Code;
    }

    public void setIcd10Code(String icd10Code) {
        this.icd10Code = icd10Code;
    }

    public String getDiagnosisText() {
        return diagnosisText;
    }

    public void setDiagnosisText(String diagnosisText) {
        this.diagnosisText = diagnosisText;
    }

    public String getDiagnosedBy() {
        return diagnosedBy;
    }

    public void setDiagnosedBy(String diagnosedBy) {
        this.diagnosedBy = diagnosedBy;
    }

    public Date getDiagnosisTime() {
        return diagnosisTime;
    }

    public void setDiagnosisTime(Date diagnosisTime) {
        this.diagnosisTime = diagnosisTime;
    }

    public Integer getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Integer isPrimary) {
        this.isPrimary = isPrimary;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public Integer getStatusFlag() {
        return statusFlag;
    }

    public void setStatusFlag(Integer statusFlag) {
        this.statusFlag = statusFlag;
    }

    public Integer getModificationType() {
        return modificationType;
    }

    public void setModificationType(Integer modificationType) {
        this.modificationType = modificationType;
    }

    public String getAddReason() {
        return addReason;
    }

    public void setAddReason(String addReason) {
        this.addReason = addReason;
    }

    public Integer getDiagnosisIndex() {
        return diagnosisIndex;
    }

    public void setDiagnosisIndex(Integer diagnosisIndex) {
        this.diagnosisIndex = diagnosisIndex;
    }
}
