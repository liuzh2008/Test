package com.example.medaiassistant.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "diagnosis")
public class Diagnosis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DiagnosisID")
    private Integer diagnosisId;

    @Column(name = "PatientID")
    private String patientId;

    @Column(name = "DiagnosisType")
    private Integer diagnosisType;

    @Column(name = "ICD10Code")
    private String icd10Code;

    @Column(name = "DiagnosisText")
    private String diagnosisText;

    @Column(name = "DiagnosedBy")
    private String diagnosedBy;

    @Column(name = "DiagnosisTime")
    private Date diagnosisTime;

    @Column(name = "IsPrimary")
    private Integer isPrimary;

    @Column(name = "ParentID")
    private Integer parentId;

    @Column(name = "StatusFlag")
    private Integer statusFlag;

    @Column(name = "ModificationType")
    private Integer modificationType;

    @Column(name = "AddReason")
    private String addReason;

    @Column(name = "DiagnosisIndex")
    private Integer diagnosisIndex;

    @Column(name = "is_deleted")
    private Integer isDeleted;

    // Getters and Setters
    public Integer getDiagnosisId() {
        return diagnosisId;
    }

    public void setDiagnosisId(Integer diagnosisId) {
        this.diagnosisId = diagnosisId;
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

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }
}
