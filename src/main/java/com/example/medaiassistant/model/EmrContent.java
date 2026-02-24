package com.example.medaiassistant.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * EMR病历内容实体类
 * 用于存储从医院HIS系统同步的EMR病历内容数据
 * 支持多源数据（电子病历、护理记录等）的去重与同步
 * 
 * @author System
 * @version 1.0
 * @since 2026-01-11
 */
@Entity
@Table(name = "EMR_CONTENT")
public class EmrContent {
    
    /**
     * 自增主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    
    /**
     * 住院流水号（主服务器患者ID）
     */
    @Column(name = "PATIENT_ID", length = 50)
    private String patientId;
    
    /**
     * 病人ID（Oracle源表PATI_ID）
     */
    @Column(name = "PATI_ID", length = 50)
    private String patiId;
    
    /**
     * 住院次数
     */
    @Column(name = "VISIT_ID")
    private Integer visitId;
    
    /**
     * 患者姓名
     */
    @Column(name = "PATI_NAME", length = 200)
    private String patiName;
    
    /**
     * 科室编号
     */
    @Column(name = "DEPT_CODE", length = 50)
    private String deptCode;
    
    /**
     * 科室名称
     */
    @Column(name = "DEPT_NAME", length = 50)
    private String deptName;
    
    /**
     * 记录类型（如入院记录、病程记录等）
     */
    @Column(name = "DOC_TYPE_NAME", length = 100)
    private String docTypeName;
    
    /**
     * 记录时间
     */
    @Column(name = "RECORD_DATE")
    private Timestamp recordDate;
    
    /**
     * 病历内容（CLOB字段）
     */
    @Lob
    @Column(name = "CONTENT")
    private String content;
    
    /**
     * 创建用户ID
     */
    @Column(name = "CREATEUSERID", length = 20)
    private String createUserId;
    
    /**
     * 创建人
     */
    @Column(name = "CREATEBY", length = 20)
    private String createBy;
    
    /**
     * 文档标题时间
     */
    @Column(name = "DOC_TITLE_TIME")
    private Timestamp docTitleTime;
    
    /**
     * 修改时间
     */
    @Column(name = "MODIFIEDON")
    private Timestamp modifiedOn;
    
    /**
     * 删除标记（0-未删除，非0-已删除）
     */
    @Column(name = "DELETEMARK")
    private Integer deleteMark;
    
    /**
     * 源表名称（用于多源数据去重）
     * 示例值：emr.emr_content、nursing.nursing_record
     */
    @Column(name = "SOURCE_TABLE", length = 100)
    private String sourceTable;
    
    /**
     * 源表记录ID（用于去重）
     */
    @Column(name = "SOURCE_ID", length = 50)
    private String sourceId;

    // ==================== Getters and Setters ====================

    /**
     * 获取自增主键
     * @return 记录ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置自增主键
     * @param id 记录ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取住院流水号
     * @return 主服务器患者ID
     */
    public String getPatientId() {
        return patientId;
    }

    /**
     * 设置住院流水号
     * @param patientId 主服务器患者ID
     */
    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    /**
     * 获取病人ID
     * @return Oracle源表PATI_ID
     */
    public String getPatiId() {
        return patiId;
    }

    /**
     * 设置病人ID
     * @param patiId Oracle源表PATI_ID
     */
    public void setPatiId(String patiId) {
        this.patiId = patiId;
    }

    /**
     * 获取住院次数
     * @return 住院次数
     */
    public Integer getVisitId() {
        return visitId;
    }

    /**
     * 设置住院次数
     * @param visitId 住院次数
     */
    public void setVisitId(Integer visitId) {
        this.visitId = visitId;
    }

    /**
     * 获取患者姓名
     * @return 患者姓名
     */
    public String getPatiName() {
        return patiName;
    }

    /**
     * 设置患者姓名
     * @param patiName 患者姓名
     */
    public void setPatiName(String patiName) {
        this.patiName = patiName;
    }

    /**
     * 获取科室编号
     * @return 科室编号
     */
    public String getDeptCode() {
        return deptCode;
    }

    /**
     * 设置科室编号
     * @param deptCode 科室编号
     */
    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
    }

    /**
     * 获取科室名称
     * @return 科室名称
     */
    public String getDeptName() {
        return deptName;
    }

    /**
     * 设置科室名称
     * @param deptName 科室名称
     */
    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    /**
     * 获取记录类型
     * @return 记录类型（如入院记录、病程记录等）
     */
    public String getDocTypeName() {
        return docTypeName;
    }

    /**
     * 设置记录类型
     * @param docTypeName 记录类型（如入院记录、病程记录等）
     */
    public void setDocTypeName(String docTypeName) {
        this.docTypeName = docTypeName;
    }

    /**
     * 获取记录时间
     * @return 记录时间
     */
    public Timestamp getRecordDate() {
        return recordDate;
    }

    /**
     * 设置记录时间
     * @param recordDate 记录时间
     */
    public void setRecordDate(Timestamp recordDate) {
        this.recordDate = recordDate;
    }

    /**
     * 获取病历内容
     * @return 病历内容（CLOB字段）
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置病历内容
     * @param content 病历内容（CLOB字段）
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取创建用户ID
     * @return 创建用户ID
     */
    public String getCreateUserId() {
        return createUserId;
    }

    /**
     * 设置创建用户ID
     * @param createUserId 创建用户ID
     */
    public void setCreateUserId(String createUserId) {
        this.createUserId = createUserId;
    }

    /**
     * 获取创建人
     * @return 创建人姓名
     */
    public String getCreateBy() {
        return createBy;
    }

    /**
     * 设置创建人
     * @param createBy 创建人姓名
     */
    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    /**
     * 获取文档标题时间
     * @return 文档标题时间
     */
    public Timestamp getDocTitleTime() {
        return docTitleTime;
    }

    /**
     * 设置文档标题时间
     * @param docTitleTime 文档标题时间
     */
    public void setDocTitleTime(Timestamp docTitleTime) {
        this.docTitleTime = docTitleTime;
    }

    /**
     * 获取修改时间
     * @return 修改时间
     */
    public Timestamp getModifiedOn() {
        return modifiedOn;
    }

    /**
     * 设置修改时间
     * @param modifiedOn 修改时间
     */
    public void setModifiedOn(Timestamp modifiedOn) {
        this.modifiedOn = modifiedOn;
    }

    /**
     * 获取删除标记
     * @return 删除标记（0-未删除，非0-已删除）
     */
    public Integer getDeleteMark() {
        return deleteMark;
    }

    /**
     * 设置删除标记
     * @param deleteMark 删除标记（0-未删除，非0-已删除）
     */
    public void setDeleteMark(Integer deleteMark) {
        this.deleteMark = deleteMark;
    }

    /**
     * 获取源表名称
     * @return 源表名称（用于多源数据去重）
     */
    public String getSourceTable() {
        return sourceTable;
    }

    /**
     * 设置源表名称
     * @param sourceTable 源表名称（如emr.emr_content、nursing.nursing_record）
     */
    public void setSourceTable(String sourceTable) {
        this.sourceTable = sourceTable;
    }

    /**
     * 获取源表记录ID
     * @return 源表记录ID（用于去重）
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * 设置源表记录ID
     * @param sourceId 源表记录ID（用于去重）
     */
    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    // ==================== Object 方法重写 ====================

    /**
     * 返回实体的字符串表示
     * 包含主要字段信息，CONTENT字段截取前50个字符
     * @return 实体的字符串表示
     */
    @Override
    public String toString() {
        return "EmrContent{" +
                "id=" + id +
                ", patientId='" + patientId + '\'' +
                ", patiId='" + patiId + '\'' +
                ", visitId=" + visitId +
                ", patiName='" + patiName + '\'' +
                ", deptCode='" + deptCode + '\'' +
                ", deptName='" + deptName + '\'' +
                ", docTypeName='" + docTypeName + '\'' +
                ", recordDate=" + recordDate +
                ", content='" + (content != null ? content.substring(0, Math.min(content.length(), 50)) + "..." : null) + '\'' +
                ", sourceTable='" + sourceTable + '\'' +
                ", sourceId='" + sourceId + '\'' +
                '}';
    }
}
