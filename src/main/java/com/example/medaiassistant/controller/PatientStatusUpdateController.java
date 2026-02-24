package com.example.medaiassistant.controller;

import com.example.medaiassistant.service.PatientStatusUpdateService;
import org.springframework.web.bind.annotation.*;

/**
 * 患者状态更新控制器
 * 
 * 该控制器提供手动触发患者状态更新的功能，主要用于测试和调试。
 * 
 * @author Cline
 * @since 2025-08-10
 */
@RestController
@RequestMapping("/api/patient-status")
public class PatientStatusUpdateController {
    
    private final PatientStatusUpdateService patientStatusUpdateService;
    
    public PatientStatusUpdateController(PatientStatusUpdateService patientStatusUpdateService) {
        this.patientStatusUpdateService = patientStatusUpdateService;
    }
    
    /**
     * 手动触发所有患者状态更新
     * 
     * @return 操作结果
     */
    @PostMapping("/update-all")
    public String updateAllPatientStatus() {
        try {
            patientStatusUpdateService.updateAllPatientStatus();
            return "所有患者状态更新完成";
        } catch (Exception e) {
            return "患者状态更新失败: " + e.getMessage();
        }
    }
    
}
