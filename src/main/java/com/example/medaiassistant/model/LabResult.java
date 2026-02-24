package com.example.medaiassistant.model;

import jakarta.persistence.*;
import java.sql.Timestamp;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 检验结果实体类
 * 
 * <p>对应数据库表 LABRESULTS，存储患者的检验检查结果数据。</p>
 * <p>该实体类用于保存从医院LIS系统同步的化验检验结果信息。</p>
 * 
 * <h3>时间字段格式化说明</h3>
 * <p>时间字段（labIssueTime、labReportTime）使用 {@link JsonFormat} 注解，
 * 确保API响应中返回标准日期时间字符串格式（yyyy-MM-dd HH:mm:ss），
 * 而非毫秒时间戳，便于前端直接显示。</p>
 * 
 * @author MedAiAssistant
 * @since 1.0
 * @see com.example.medaiassistant.repository.LabResultRepository
 * @see com.example.medaiassistant.controller.LabResultController
 */
@Entity
@Table(name = "labresults")
public class LabResult {
    
    /**
     * 主键ID
     * <p>数据库自动生成的唯一标识符</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /**
     * 检验项目名称
     * <p>如：白细胞计数、血红蛋白、血糖等</p>
     */
    @Column(name = "LabName")
    private String labName;

    /**
     * 检验项目类型
     * <p>如：血常规、生化、尿常规等</p>
     */
    @Column(name = "LabType")
    private String labType;

    /**
     * 检验结果值
     * <p>VARCHAR类型（已从 CLOB 优化为 VARCHAR，最大长度 255）</p>
     */
    @Column(name = "LabResult", length = 255)
    private String labResult;

    /**
     * 参考范围
     * <p>正常值范围，如：4.0-10.0、120-160等</p>
     */
    @Column(name = "ReferenceRange")
    private String referenceRange;

    /**
     * 计量单位
     * <p>如：10^9/L、g/L、mmol/L等</p>
     */
    @Column(name = "Unit")
    private String unit;

    /**
     * 异常标志
     * <p>N=正常，H=高于正常值，L=低于正常值</p>
     */
    @Column(name = "ABNORMAL_INDICATOR")
    private String abnormalIndicator;

    /**
     * 检验报告签发时间
     * <p>API响应格式：yyyy-MM-dd HH:mm:ss（如：2025-06-12 10:06:59）</p>
     * <p>时区：Asia/Shanghai（中国标准时间）</p>
     */
    @Column(name = "LabIssueTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Timestamp labIssueTime;

    /**
     * 检验报告时间
     * <p>API响应格式：yyyy-MM-dd HH:mm:ss（如：2025-06-12 10:06:59）</p>
     * <p>时区：Asia/Shanghai（中国标准时间）</p>
     */
    @Column(name = "LabReportTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Timestamp labReportTime;

    /**
     * 患者ID
     * <p>关联患者表的唯一标识符，格式如：990500000178405_1</p>
     */
    @Column(name = "PatientID")
    private String patientId;

    /**
     * 是否已分析标志
     * <p>0=未分析，1=已分析</p>
     */
    @Column(name = "IsAnalyzed")
    private Integer isAnalyzed;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabName() {
        return labName;
    }

    public void setLabName(String labName) {
        this.labName = labName;
    }

    public String getLabType() {
        return labType;
    }

    public void setLabType(String labType) {
        this.labType = labType;
    }

    public String getLabResult() {
        return labResult;
    }

    public void setLabResult(String labResult) {
        this.labResult = labResult;
    }

    public String getReferenceRange() {
        return referenceRange;
    }

    public void setReferenceRange(String referenceRange) {
        this.referenceRange = referenceRange;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getAbnormalIndicator() {
        return abnormalIndicator;
    }

    public void setAbnormalIndicator(String abnormalIndicator) {
        this.abnormalIndicator = abnormalIndicator;
    }

    public Timestamp getLabIssueTime() {
        return labIssueTime;
    }

    public void setLabIssueTime(Timestamp labIssueTime) {
        this.labIssueTime = labIssueTime;
    }

    public Timestamp getLabReportTime() {
        return labReportTime;
    }

    public void setLabReportTime(Timestamp labReportTime) {
        this.labReportTime = labReportTime;
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
