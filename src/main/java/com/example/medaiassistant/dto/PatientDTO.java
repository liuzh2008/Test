package com.example.medaiassistant.dto;

import lombok.Data;

import java.util.Date;

/**
 * 患者信息数据传输对象
 * 
 * 用于在系统各层之间传输患者基本信息的DTO类，包含了患者的核心信息字段
 * 以及用于患者状态跟踪的状态字段
 * 
 * @author Cline
 * @since 2025-08-10
 */
@Data
public class PatientDTO {
    /**
     * 患者唯一标识符
     */
    private String patientId;
    
    /**
     * 患者姓名
     */
    private String name;
    
    /**
     * 患者性别
     */
    private String gender;
    
    /**
     * 患者出生日期
     */
    private Date dateOfBirth;
    
    /**
     * 患者床位号
     */
    private String bedNumber;
    
    /**
     * 患者入院时间
     */
    private Date admissionTime;

    /**
     * 患者出院时间
     */
    private Date dischargeTime;

    /**
     * 患者所在科室
     */
    private String department;
    
    /**
     * 患者病历号
     */
    private String medicalRecordNumber;
    
    /**
     * 患者当前状态
     * 
     * 用于表示患者当前的医疗状态，如"普通"、"病危"、"病重"等
     * 该字段用于医护人员快速了解患者的重要状态信息
     */
    private String status;
}
