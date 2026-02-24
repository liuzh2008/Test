package com.example.medaiassistant.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;

/**
 * 患者数据保存请求数据传输对象
 * 
 * 用于接收保存或更新患者数据的请求，包含PATIENTS表的所有字段
 * 基于patientId作为唯一标识，如果患者已存在则更新数据
 * 
 * @author System
 * @since 2025-12-10
 */
@Data
public class PatientSaveRequest {
    
    /**
     * 患者唯一标识符（PATIENTID）
     * 必填字段，作为主键和唯一标识
     */
    @NotNull(message = "患者ID不能为空")
    @Size(max = 255, message = "患者ID长度不能超过255个字符")
    private String patientId;
    
    /**
     * 身份证号（IDCARD）
     */
    @Size(max = 255, message = "身份证号长度不能超过255个字符")
    private String idCard;
    
    /**
     * 病历号（MEDICALRECORDNUMBER）
     */
    @Size(max = 255, message = "病历号长度不能超过255个字符")
    private String medicalRecordNumber;
    
    /**
     * 患者姓名（NAME）
     * 必填字段
     */
    @NotNull(message = "患者姓名不能为空")
    @Size(max = 255, message = "患者姓名长度不能超过255个字符")
    private String name;
    
    /**
     * 患者性别（GENDER）
     */
    @Size(max = 255, message = "性别长度不能超过255个字符")
    private String gender;
    
    /**
     * 出生日期（DATEOFBIRTH）
     */
    private Date dateOfBirth;
    
    /**
     * 床位号（BEDNUMBER）
     */
    @Size(max = 255, message = "床位号长度不能超过255个字符")
    private String bedNumber;
    
    /**
     * 入院时间（ADMISSIONTIME）
     */
    private Date admissionTime;
    
    /**
     * 出院时间（DISCHARGETIME）
     */
    private Date dischargeTime;
    
    /**
     * 是否在院（ISINHOSPITAL）
     * 使用Boolean类型对应数据库的NUMBER(10,0)字段
     */
    private Boolean isInHospital;
    
    /**
     * 科室（DEPARTMENT）
     * 必填字段
     */
    @NotNull(message = "科室不能为空")
    @Size(max = 255, message = "科室长度不能超过255个字符")
    private String department;
    
    /**
     * 重要信息（IMPORTANTINFORMATION）
     * CLOB字段，存储大量文本信息
     */
    private String importantInformation;
    
    /**
     * 患者内部ID（PATI_ID）
     */
    @Size(max = 255, message = "患者内部ID长度不能超过255个字符")
    private String patiId;
    
    /**
     * 就诊ID（VISIT_ID）
     */
    private Integer visitId;
    
    /**
     * 患者编号（PATIENT_NO）
     */
    @Size(max = 255, message = "患者编号长度不能超过255个字符")
    private String patientNo;
    
    /**
     * DRGs结果（DRGSRESULT）
     * CLOB字段，存储DRGs分析结果
     */
    private String drgsResult;
    
    /**
     * DRGs严重并发症（DRGSSEVERECOMPLICATION）
     * 使用Boolean类型对应数据库的NUMBER(10,0)字段
     */
    private Boolean drgsSevereComplication;
    
    /**
     * DRGs常见并发症（DRGSCOMMONCOMPLICATION）
     * 使用Boolean类型对应数据库的NUMBER(10,0)字段
     */
    private Boolean drgsCommonComplication;
    
    /**
     * 状态（STATUS）
     * 必填字段，最大长度20字符
     */
    @NotNull(message = "状态不能为空")
    @Size(max = 20, message = "状态长度不能超过20个字符")
    private String status;
    
    /**
     * 验证数据完整性
     * 检查必填字段是否已设置
     */
    public boolean isValid() {
        return patientId != null && !patientId.trim().isEmpty() &&
               name != null && !name.trim().isEmpty() &&
               department != null && !department.trim().isEmpty() &&
               status != null && !status.trim().isEmpty();
    }
    
    /**
     * 获取验证错误信息
     */
    public String getValidationErrors() {
        StringBuilder errors = new StringBuilder();
        
        if (patientId == null || patientId.trim().isEmpty()) {
            errors.append("患者ID不能为空; ");
        } else if (patientId.length() > 255) {
            errors.append("患者ID长度不能超过255个字符; ");
        }
        
        if (name == null || name.trim().isEmpty()) {
            errors.append("患者姓名不能为空; ");
        } else if (name.length() > 255) {
            errors.append("患者姓名长度不能超过255个字符; ");
        }
        
        if (department == null || department.trim().isEmpty()) {
            errors.append("科室不能为空; ");
        } else if (department.length() > 255) {
            errors.append("科室长度不能超过255个字符; ");
        }
        
        if (status == null || status.trim().isEmpty()) {
            errors.append("状态不能为空; ");
        } else if (status.length() > 20) {
            errors.append("状态长度不能超过20个字符; ");
        }
        
        return errors.toString();
    }
}
