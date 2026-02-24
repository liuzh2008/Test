package com.example.medaiassistant.controller;

import com.example.medaiassistant.dto.drg.DrgCatalog;
import com.example.medaiassistant.dto.drg.MatchingResult;
import com.example.medaiassistant.dto.drg.PatientData;
import com.example.medaiassistant.service.DrgMatchingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DRG匹配控制器
 * 
 * 提供主要诊断与手术匹配的REST API接口
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-22
 */
@RestController
@RequestMapping("/api/drg/matching")
public class DrgMatchingController {

    private final DrgMatchingService drgMatchingService;

    @Autowired
    public DrgMatchingController(DrgMatchingService drgMatchingService) {
        this.drgMatchingService = drgMatchingService;
    }

    /**
     * 匹配患者的主要诊断和手术
     * 
     * @param request 匹配请求，包含患者数据和DRG目录
     * @return 匹配结果，包含主要诊断和手术列表
     */
    @PostMapping("/primary")
    public ResponseEntity<MatchingResult> matchPrimaryDiagnosisAndProcedure(
            @RequestBody DrgMatchingRequest request) {
        
        MatchingResult result = drgMatchingService.matchPrimaryDiagnosisAndProcedure(
            request.getPatientData(), 
            request.getDrgCatalog()
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * DRG匹配请求DTO
     */
    public static class DrgMatchingRequest {
        private PatientData patientData;
        private DrgCatalog drgCatalog;

        public DrgMatchingRequest() {
        }

        public DrgMatchingRequest(PatientData patientData, DrgCatalog drgCatalog) {
            this.patientData = patientData;
            this.drgCatalog = drgCatalog;
        }

        public PatientData getPatientData() {
            return patientData;
        }

        public void setPatientData(PatientData patientData) {
            this.patientData = patientData;
        }

        public DrgCatalog getDrgCatalog() {
            return drgCatalog;
        }

        public void setDrgCatalog(DrgCatalog drgCatalog) {
            this.drgCatalog = drgCatalog;
        }
    }
}
