package com.example.medaiassistant.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

/**
 * 手术名称实体类
 * 映射到数据库中的surgeryname表，存储患者的手术记录信息
 * 
 * @author Cline
 * @version 1.0.0
 * @since 2025-09-26
 * 
 * @see jakarta.persistence.Entity
 * @see jakarta.persistence.Table
 * @see lombok.Data
 * 
 * @description
 * 手术名称实体类用于存储患者的手术记录信息，包括手术名称、手术日期、是否为主要手术等。
 * 
 * @table surgeryname
 * @column SurgeryId - 手术记录主键，自增ID
 * @column PatientId - 患者ID，关联患者信息
 * @column SurgeryName - 手术名称，存储具体的手术操作名称
 * @column IsPrimary - 是否为主要手术，布尔值标识
 * @column SurgeryCode - 手术代码，CLOB类型存储手术编码
 * @column SurgeryDate - 手术日期，记录手术执行的具体日期
 * 
 * @example
 * Surgery surgery = new Surgery();
 * surgery.setPatientId("99050801275226_1");
 * surgery.setSurgeryName("阑尾切除术");
 * surgery.setSurgeryDate(new Date());
 * surgery.setIsPrimary(1);
 * surgery.setSurgeryCode("APPENDECTOMY");
 */
@Entity
@Table(name = "surgeryname")
@Data
public class Surgery {
    /**
     * 手术记录主键ID
     * 自增主键，唯一标识每条手术记录
     * 
     * @type {Integer}
     * @column SURGERYID
     * @generatedValue strategy = GenerationType.IDENTITY
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SURGERYID")
    private Integer surgeryId;

    /**
     * 患者ID
     * 关联患者信息，格式如"99050801275226_1"
     * 
     * @type {String}
     * @column PATIENTID
     */
    @Column(name = "PATIENTID")
    private String patientId;

    /**
     * 手术名称
     * 存储具体的手术操作名称，如"阑尾切除术"
     * 
     * @type {String}
     * @column SURGERYNAME
     */
    @Column(name = "SURGERYNAME")
    private String surgeryName;

    /**
     * 是否为主要手术
     * 数值标识，1表示主要手术，0表示次要手术
     * 
     * @type {Integer}
     * @column ISPRIMARY
     */
    @Column(name = "ISPRIMARY")
    private Integer isPrimary;

    /**
     * 手术代码
     * CLOB类型存储手术编码，如"APPENDECTOMY"
     * 
     * @type {String}
     * @column SURGERYCODE
     * @columnDefinition CLOB
     */
    @Lob
    @Column(name = "SURGERYCODE", columnDefinition = "CLOB")
    private String surgeryCode;

    /**
     * 手术日期
     * 记录手术执行的具体日期
     * 
     * @type {Date}
     * @column SURGERYDATE
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "SURGERYDATE")
    private Date surgeryDate;
}
