package com.example.medaiassistant.model;

import jakarta.persistence.*;
import java.util.Date;

/**
 * EMR_RECORD表实体类
 * 映射EMR_RECORD表结构，用于存储电子病历记录
 * 
 * @since 1.0.0
 */
@Entity
@Table(name = "EMR_RECORD")
public class EmrRecord {
    
    /**
     * 记录唯一标识：主键，每条病历记录的唯一编号
     */
    @Id
    @Column(name = "ID", length = 50)
    private String id;
    
    /**
     * 病人ID：关联患者主表的外键字段，标识病历所属患者
     */
    @Column(name = "PATI_ID", length = 50)
    private String patiId;
    
    /**
     * 住院次数：同一患者多次住院的序号，用于区分不同住院周期的病历
     */
    @Column(name = "VISIT_ID")
    private Integer visitId;
    
    /**
     * 记录类型：病历文档的分类（如入院记录、手术记录等）
     */
    @Column(name = "DOC_TYPE_NAME", length = 100)
    private String docTypeName;
    
    /**
     * 记录时间：病历内容实际创建的时间
     */
    @Column(name = "RECORD_DATE")
    private Date recordDate;
    
    /**
     * 记录内容：病历的具体文本内容，使用CLOB类型支持大文本存储
     */
    @Lob
    @Column(name = "CONTENT")
    private String content;
    
    /**
     * 文档标题时间：病历标题中注明的时间（如"2025-08-20 病程记录"中的日期）
     */
    @Column(name = "DOC_TITLE_TIME")
    private Date docTitleTime;
    
    /**
     * 修改时间：记录最后一次被编辑修改的时间，默认值为系统当前时间
     */
    @Column(name = "MODIFIEDON")
    private Date modifiedOn;
    
    /**
     * 软删除标记：0表示正常数据，1表示已逻辑删除，用于数据归档而非物理删除
     */
    @Column(name = "DELETEMARK")
    private Integer deleteMark;
    
    /**
     * 病人住院流水号
     */
    @Column(name = "PATIENTID", length = 255)
    private String patientId;
    
    /**
     * 更新时间
     */
    @Column(name = "UPDATEDT")
    private Date updatedt;

    // Getters and Setters
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPatiId() {
        return patiId;
    }

    public void setPatiId(String patiId) {
        this.patiId = patiId;
    }

    public Integer getVisitId() {
        return visitId;
    }

    public void setVisitId(Integer visitId) {
        this.visitId = visitId;
    }

    public String getDocTypeName() {
        return docTypeName;
    }

    public void setDocTypeName(String docTypeName) {
        this.docTypeName = docTypeName;
    }

    public Date getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(Date recordDate) {
        this.recordDate = recordDate;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getDocTitleTime() {
        return docTitleTime;
    }

    public void setDocTitleTime(Date docTitleTime) {
        this.docTitleTime = docTitleTime;
    }

    public Date getModifiedOn() {
        return modifiedOn;
    }

    public void setModifiedOn(Date modifiedOn) {
        this.modifiedOn = modifiedOn;
    }

    public Integer getDeleteMark() {
        return deleteMark;
    }

    public void setDeleteMark(Integer deleteMark) {
        this.deleteMark = deleteMark;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public Date getUpdatedt() {
        return updatedt;
    }

    public void setUpdatedt(Date updatedt) {
        this.updatedt = updatedt;
    }

    @Override
    public String toString() {
        return "EmrRecord{" +
                "id='" + id + '\'' +
                ", patiId='" + patiId + '\'' +
                ", visitId=" + visitId +
                ", docTypeName='" + docTypeName + '\'' +
                ", recordDate=" + recordDate +
                ", content='" + (content != null ? content.substring(0, Math.min(content.length(), 50)) + "..." : "null") + '\'' +
                ", docTitleTime=" + docTitleTime +
                ", modifiedOn=" + modifiedOn +
                ", deleteMark=" + deleteMark +
                ", patientId='" + patientId + '\'' +
                ", updatedt=" + updatedt +
                '}';
    }
}
