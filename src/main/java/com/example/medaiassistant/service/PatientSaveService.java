package com.example.medaiassistant.service;

import com.example.medaiassistant.dto.PatientSaveRequest;
import com.example.medaiassistant.dto.PatientSaveResponse;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 患者数据保存服务
 * 
 * 负责处理患者数据的保存和更新逻辑
 * 基于patientId作为唯一标识，如果患者已存在则更新数据，否则创建新记录
 * 
 * @author System
 * @since 2025-12-10
 */
@Service
public class PatientSaveService {

    @Autowired
    private PatientRepository patientRepository;

    /**
     * 保存或更新患者数据
     * 
     * @param request 患者数据保存请求
     * @return 保存操作结果
     */
    @Transactional
    public PatientSaveResponse savePatient(PatientSaveRequest request) {
        // 验证请求数据
        String validationErrors = request.getValidationErrors();
        if (!validationErrors.isEmpty()) {
            return PatientSaveResponse.validationError(request.getPatientId(), validationErrors);
        }

        try {
            // 检查患者是否已存在
            Optional<Patient> existingPatient = patientRepository.findById(request.getPatientId());
            
            Patient patient;
            String operation;
            
            if (existingPatient.isPresent()) {
                // 更新现有患者
                patient = existingPatient.get();
                updatePatientFromRequest(patient, request);
                operation = "updated";
            } else {
                // 创建新患者
                patient = createPatientFromRequest(request);
                operation = "created";
            }
            
            // 保存到数据库
            Patient savedPatient = patientRepository.save(patient);
            
            // 返回成功响应
            if ("created".equals(operation)) {
                return PatientSaveResponse.created(savedPatient.getPatientId());
            } else {
                return PatientSaveResponse.updated(savedPatient.getPatientId());
            }
            
        } catch (Exception e) {
            // 处理数据库异常
            return PatientSaveResponse.databaseError(request.getPatientId(), e.getMessage());
        }
    }

    /**
     * 从请求创建新的患者对象
     */
    private Patient createPatientFromRequest(PatientSaveRequest request) {
        Patient patient = new Patient();
        mapRequestToPatient(request, patient);
        return patient;
    }

    /**
     * 从请求更新现有患者对象
     */
    private void updatePatientFromRequest(Patient patient, PatientSaveRequest request) {
        mapRequestToPatient(request, patient);
    }

    /**
     * 将请求数据映射到患者对象
     */
    private void mapRequestToPatient(PatientSaveRequest request, Patient patient) {
        // 必填字段
        patient.setPatientId(request.getPatientId());
        patient.setName(request.getName());
        patient.setDepartment(request.getDepartment());
        patient.setStatus(request.getStatus());
        
        // 可选字段
        if (request.getIdCard() != null) {
            patient.setIdCard(request.getIdCard());
        }
        
        if (request.getMedicalRecordNumber() != null) {
            patient.setMedicalRecordNumber(request.getMedicalRecordNumber());
        }
        
        if (request.getGender() != null) {
            patient.setGender(request.getGender());
        }
        
        if (request.getDateOfBirth() != null) {
            patient.setDateOfBirth(request.getDateOfBirth());
        }
        
        if (request.getBedNumber() != null) {
            patient.setBedNumber(request.getBedNumber());
        }
        
        if (request.getAdmissionTime() != null) {
            patient.setAdmissionTime(request.getAdmissionTime());
        }
        
        if (request.getDischargeTime() != null) {
            patient.setDischargeTime(request.getDischargeTime());
        }
        
        if (request.getIsInHospital() != null) {
            patient.setIsInHospital(request.getIsInHospital());
        }
        
        if (request.getImportantInformation() != null) {
            patient.setImportantInformation(request.getImportantInformation());
        }
        
        if (request.getPatiId() != null) {
            patient.setPatiId(request.getPatiId());
        }
        
        if (request.getVisitId() != null) {
            patient.setVisitId(request.getVisitId());
        }
        
        if (request.getPatientNo() != null) {
            patient.setPatientNo(request.getPatientNo());
        }
        
        if (request.getDrgsResult() != null) {
            patient.setDrgsResult(request.getDrgsResult());
        }
        
        if (request.getDrgsSevereComplication() != null) {
            patient.setDrgsSevereComplication(request.getDrgsSevereComplication());
        }
        
        if (request.getDrgsCommonComplication() != null) {
            patient.setDrgsCommonComplication(request.getDrgsCommonComplication());
        }
    }

    /**
     * 批量保存患者数据
     * 
     * @param requests 患者数据保存请求列表
     * @return 批量保存结果统计
     */
    @Transactional
    public BatchSaveResult batchSavePatients(java.util.List<PatientSaveRequest> requests) {
        BatchSaveResult result = new BatchSaveResult();
        
        for (PatientSaveRequest request : requests) {
            try {
                PatientSaveResponse response = savePatient(request);
                if (response.isSuccess()) {
                    result.incrementSuccessCount();
                } else {
                    result.incrementFailureCount();
                    result.addError(request.getPatientId(), response.getMessage());
                }
            } catch (Exception e) {
                result.incrementFailureCount();
                result.addError(request.getPatientId(), "系统异常: " + e.getMessage());
            }
        }
        
        return result;
    }

    /**
     * 批量保存结果类
     */
    public static class BatchSaveResult {
        private int successCount = 0;
        private int failureCount = 0;
        private java.util.Map<String, String> errors = new java.util.HashMap<>();
        
        public void incrementSuccessCount() {
            successCount++;
        }
        
        public void incrementFailureCount() {
            failureCount++;
        }
        
        public void addError(String patientId, String error) {
            errors.put(patientId, error);
        }
        
        public int getSuccessCount() {
            return successCount;
        }
        
        public int getFailureCount() {
            return failureCount;
        }
        
        public int getTotalCount() {
            return successCount + failureCount;
        }
        
        public java.util.Map<String, String> getErrors() {
            return errors;
        }
        
        public boolean isAllSuccess() {
            return failureCount == 0;
        }
        
        public double getSuccessRate() {
            int total = getTotalCount();
            return total > 0 ? (double) successCount / total * 100 : 100.0;
        }
    }
}
