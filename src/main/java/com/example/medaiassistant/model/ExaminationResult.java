package com.example.medaiassistant.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 检查结果实体类
 * 用于存储患者的各类检查结果数据，包括影像检查、超声、CT等
 * 
 * @author System
 * @version 1.1
 * @since 2025-12-29
 */
@Entity
@Table(name = "ExaminationResults")
public class ExaminationResult {
    
    /**
     * 检查申请号，主键
     */
    @Id
    @Column(name = "ExaminationID")
    private String examinationId;

    /**
     * 检查项目名称
     */
    @Column(name = "CheckName")
    private String checkName;

    /**
     * 检查类型（如CT、MRI、超声等）
     */
    @Column(name = "CheckType")
    private String checkType;

    /**
     * 检查所见（CLOB字段，存储详细检查描述）
     */
    @Lob
    @Column(name = "CheckDescription", columnDefinition = "LONGTEXT")
    private String checkDescription;

    /**
     * 检查结论（CLOB字段，存储诊断结论）
     */
    @Lob
    @Column(name = "CheckConclusion", columnDefinition = "LONGTEXT")
    private String checkConclusion;

    /**
     * 检查申请时间
     */
    @Column(name = "CheckIssueTime")
    private Timestamp checkIssueTime;

    /**
     * 检查执行时间（送检日期）
     */
    @Column(name = "CheckExecuteTime")
    private Timestamp checkExecuteTime;

    /**
     * 检查报告时间（审核日期）
     */
    @Column(name = "CheckReportTime")
    private Timestamp checkReportTime;

    /**
     * 记录更新时间
     */
    @Column(name = "UpdateDt")
    private Timestamp updateDt;

    /**
     * 患者ID（住院就诊流水号）
     */
    @Column(name = "PatientID")
    private String patientId;

    /**
     * 是否已分析标记（0-未分析，1-已分析）
     */
    @Column(name = "IsAnalyzed")
    private Integer isAnalyzed;

    // Getters and Setters
    public String getExaminationId() {
        return examinationId;
    }

    public void setExaminationId(String examinationId) {
        this.examinationId = examinationId;
    }

    public String getCheckName() {
        return checkName;
    }

    public void setCheckName(String checkName) {
        this.checkName = checkName;
    }

    public String getCheckType() {
        return checkType;
    }

    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }

    public String getCheckDescription() {
        return checkDescription;
    }

    public void setCheckDescription(String checkDescription) {
        this.checkDescription = checkDescription;
    }

    public String getCheckConclusion() {
        return checkConclusion;
    }

    public void setCheckConclusion(String checkConclusion) {
        this.checkConclusion = checkConclusion;
    }

    public Timestamp getCheckIssueTime() {
        return checkIssueTime;
    }

    public void setCheckIssueTime(Timestamp checkIssueTime) {
        this.checkIssueTime = checkIssueTime;
    }

    public Timestamp getCheckExecuteTime() {
        return checkExecuteTime;
    }

    public void setCheckExecuteTime(Timestamp checkExecuteTime) {
        this.checkExecuteTime = checkExecuteTime;
    }

    public Timestamp getCheckReportTime() {
        return checkReportTime;
    }

    public void setCheckReportTime(Timestamp checkReportTime) {
        this.checkReportTime = checkReportTime;
    }

    public Timestamp getUpdateDt() {
        return updateDt;
    }

    public void setUpdateDt(Timestamp updateDt) {
        this.updateDt = updateDt;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public Integer getIsAnalyzed() {
        return isAnalyzed;
    }

    public void setIsAnalyzed(Integer isAnalyzed) {
        this.isAnalyzed = isAnalyzed;
    }
}
