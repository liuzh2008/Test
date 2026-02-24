package com.example.medaiassistant.dto;

import lombok.Data;

import java.util.Date;

/**
 * 患者数据保存响应数据传输对象
 * 
 * 用于返回保存或更新患者数据的结果
 * 
 * @author System
 * @since 2025-12-10
 */
@Data
public class PatientSaveResponse {
    
    /**
     * 操作是否成功
     */
    private boolean success;
    
    /**
     * 操作类型：created（新建）或 updated（更新）
     */
    private String operation;
    
    /**
     * 患者ID
     */
    private String patientId;
    
    /**
     * 操作结果消息
     */
    private String message;
    
    /**
     * 操作时间戳
     */
    private Date timestamp;
    
    /**
     * 创建成功响应
     */
    public static PatientSaveResponse success(String patientId, String operation, String message) {
        PatientSaveResponse response = new PatientSaveResponse();
        response.setSuccess(true);
        response.setOperation(operation);
        response.setPatientId(patientId);
        response.setMessage(message);
        response.setTimestamp(new Date());
        return response;
    }
    
    /**
     * 创建失败响应
     */
    public static PatientSaveResponse error(String patientId, String message) {
        PatientSaveResponse response = new PatientSaveResponse();
        response.setSuccess(false);
        response.setOperation("error");
        response.setPatientId(patientId);
        response.setMessage(message);
        response.setTimestamp(new Date());
        return response;
    }
    
    /**
     * 创建新建患者成功响应
     */
    public static PatientSaveResponse created(String patientId) {
        return success(patientId, "created", "患者数据保存成功");
    }
    
    /**
     * 创建更新患者成功响应
     */
    public static PatientSaveResponse updated(String patientId) {
        return success(patientId, "updated", "患者数据更新成功");
    }
    
    /**
     * 创建验证失败响应
     */
    public static PatientSaveResponse validationError(String patientId, String validationErrors) {
        return error(patientId, "数据验证失败: " + validationErrors);
    }
    
    /**
     * 创建数据库错误响应
     */
    public static PatientSaveResponse databaseError(String patientId, String errorMessage) {
        return error(patientId, "数据库操作失败: " + errorMessage);
    }
}
