package com.example.medaiassistant.model;

import jakarta.persistence.*;
import java.util.Date;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "medical_records")
@SQLDelete(sql = "UPDATE medical_records SET is_deleted = 1 WHERE record_id = ?")
@SQLRestriction("is_deleted = 0")
public class MedicalRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private int recordId;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "record_time", nullable = false)
    private Date recordTime;

    @Column(name = "recording_doctor", nullable = false)
    private String recordingDoctor;

    @Column(name = "medical_content", columnDefinition = "TEXT", nullable = false)
    private String medicalContent;

    @Column(name = "modifying_doctor")
    private String modifyingDoctor;

    @Column(name = "modification_time")
    private Date modificationTime;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "TINYINT(1) default 0")
    private Integer deleted = 0;

    /**
     * 病历类型
     * @since 2025-08-08
     */
    @Column(name = "record_type", length = 50)
    private String recordType;

    // Getters and Setters
    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
    // 移除旧的boolean类型方法

    public int getRecordId() {
        return recordId;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public Date getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(Date recordTime) {
        this.recordTime = recordTime;
    }

    public String getRecordingDoctor() {
        return recordingDoctor;
    }

    public void setRecordingDoctor(String recordingDoctor) {
        this.recordingDoctor = recordingDoctor;
    }

    public String getMedicalContent() {
        return medicalContent;
    }

    public void setMedicalContent(String medicalContent) {
        this.medicalContent = medicalContent;
    }

    public String getModifyingDoctor() {
        return modifyingDoctor;
    }

    public void setModifyingDoctor(String modifyingDoctor) {
        this.modifyingDoctor = modifyingDoctor;
    }

    public Date getModificationTime() {
        return modificationTime;
    }

    public void setModificationTime(Date modificationTime) {
        this.modificationTime = modificationTime;
    }

    /**
     * 获取病历类型
     * @return 病历类型
     */
    public String getRecordType() {
        return recordType;
    }

    /**
     * 设置病历类型
     * @param recordType 病历类型
     */
    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }
}
