package com.example.medaiassistant.controller;

import com.example.medaiassistant.model.EmrContent;
import com.example.medaiassistant.service.EmrRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/oracle-test")
public class OracleTestController {
    
    @Autowired
    private EmrRecordService emrRecordService;
    
    /**
     * 测试接口：根据patient_id查询未删除的病历记录
     * @param patientId 病人ID
     * @return 病历记录列表
     */
    @GetMapping("/emr-records")
    public ResponseEntity<List<EmrContent>> getEmrRecordsByPatientId(
            @RequestParam String patientId) {
        try {
            List<EmrContent> records = emrRecordService.getEmrRecordsByPatientId(patientId);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }
}
