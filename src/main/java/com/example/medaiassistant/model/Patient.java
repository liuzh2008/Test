package com.example.medaiassistant.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Entity
@Table(name = "PATIENTS")
@Data
public class Patient {
    @Id
    @Column(name = "PATIENTID")
    private String patientId;

    @Column(name = "IDCARD")
    private String idCard;

    @Column(name = "MEDICALRECORDNUMBER")
    private String medicalRecordNumber;

    @Column(name = "NAME")
    private String name;

    @Column(name = "GENDER")
    private String gender;

    @Column(name = "DATEOFBIRTH")
    private Date dateOfBirth;

    @Column(name = "BEDNUMBER")
    private String bedNumber;

    @Column(name = "ADMISSIONTIME")
    private Date admissionTime;

    @Column(name = "DISCHARGETIME")
    private Date dischargeTime;

    @Column(name = "ISINHOSPITAL")
    private Boolean isInHospital;

    @Column(name = "DEPARTMENT")
    private String department;

    @Lob
    @Column(name = "IMPORTANTINFORMATION", columnDefinition = "LONGTEXT")
    private String importantInformation;

    @Column(name = "PATI_ID")
    private String patiId;

    @Column(name = "VISIT_ID")
    private Integer visitId;

    @Column(name = "PATIENT_NO")
    private String patientNo;

    @Column(name = "DRGSRESULT", columnDefinition = "LONGTEXT")
    private String drgsResult;

    @Column(name = "DRGSSEVERECOMPLICATION")
    private Boolean drgsSevereComplication;

    @Column(name = "DRGSCOMMONCOMPLICATION")
    private Boolean drgsCommonComplication;

    @Column(name = "STATUS", length = 20)
    private String status;

    // Getters and Setters
    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取格式化的出生日期
     * 使用格式：DD-MM-YYYY HH24.MI.SS.FF
     * 
     * @return 格式化后的日期字符串，如果出生日期为null则返回空字符串
     */
    public String getFormattedDateOfBirth() {
        return com.example.medaiassistant.util.AgeCalculator.formatOracleDate(this.dateOfBirth);
    }

    // 其他getter/setter方法...
}
