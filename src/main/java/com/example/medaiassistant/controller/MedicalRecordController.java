package com.example.medaiassistant.controller;

import com.example.medaiassistant.dto.EmrRecordListDTO;
import com.example.medaiassistant.dto.EmrRecordContentDTO;
import com.example.medaiassistant.model.EmrContent;
import com.example.medaiassistant.model.MedicalRecord;
import com.example.medaiassistant.model.AlertTask;
import com.example.medaiassistant.repository.MedicalRecordRepository;
import com.example.medaiassistant.repository.PromptResultRepository;
import com.example.medaiassistant.repository.AlertTaskRepository;
import com.example.medaiassistant.dto.PatientPromptResultDTO;
import com.example.medaiassistant.service.EmrRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/medicalrecords")
public class MedicalRecordController {

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private PromptResultRepository promptResultRepository;
    
    @Autowired
    private EmrRecordService emrRecordService;

    /**
     * 获取病人病历记录（原始格式）
     * 根据病人ID获取病历记录列表，返回完整的MedicalRecord对象结构
     * 
     * @param patientId 病人ID（字符串，必填）
     * @return 病历记录列表，包含完整的MedicalRecord对象字段
     * @since 2025-10-13
     */
    @GetMapping
    public List<MedicalRecord> getMedicalRecordsByPatientId(@RequestParam String patientId) {
        return medicalRecordRepository.findByPatientIdAndDeleted(patientId, 0);
    }

    /**
     * 获取病人病历记录（格式化格式）
     * 根据病人ID获取病历记录列表，返回与原始格式接口相同的完整MedicalRecord对象结构
     * 修改历史：2025-10-13 - 统一数据结构，从返回String改为返回List<MedicalRecord>
     * 
     * @param patientId 病人ID（字符串，必填）
     * @return 病历记录列表，包含完整的MedicalRecord对象字段（与原始格式接口数据结构完全相同）
     * @since 2025-10-13
     */
    @GetMapping("/formatted")
    public List<MedicalRecord> getFormattedMedicalRecords(@RequestParam String patientId) {
        return medicalRecordRepository.findByPatientIdAndDeletedOrderByRecordTimeDesc(patientId, 0);
    }

    @GetMapping("/all")
    public List<MedicalRecord> getAllMedicalRecords() {
        return medicalRecordRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalRecord> getMedicalRecordById(@PathVariable int id) {
        Optional<MedicalRecord> record = medicalRecordRepository.findById(id);
        // 添加软删除检查
        if (record.isPresent() && record.get().getDeleted() == 1) {
            return ResponseEntity.notFound().build();
        }
        return record.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/save")
    @Transactional
    public ResponseEntity<Integer> createMedicalRecord(@RequestBody MedicalRecord record) {
        MedicalRecord savedRecord = medicalRecordRepository.save(record);
        
        // 如果是入院记录总结，更新相关alert_tasks状态
        if ("入院记录总结".equals(record.getRecordType())) {
            List<AlertTask> tasks = alertTaskRepository.findByPatientIdAndTaskTypeAndTaskStatus(
                record.getPatientId(), 
                "入院记录总结", 
                AlertTask.TaskStatus.待处理);
            
            tasks.forEach(task -> {
                task.setTaskStatus(AlertTask.TaskStatus.已完成);
                task.setCompletedTime(LocalDateTime.now());
                alertTaskRepository.save(task);
            });
        }
        
        // 只返回记录ID，确保前端正确获取
        return ResponseEntity.ok(savedRecord.getRecordId());
    }

    @Autowired
    private AlertTaskRepository alertTaskRepository;

    /**
     * 更新病历记录
     * @param id 病历ID
     * @param recordDetails 包含更新信息的病历对象
     * @return 更新后的病历记录
     * @throws RuntimeException 当软删除失败时抛出异常
     * @since 2025-08-08
     */
    @PutMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<Integer> updateMedicalRecord(@PathVariable int id,
            @RequestBody MedicalRecord recordDetails) {
        Optional<MedicalRecord> record = medicalRecordRepository.findById(id);
        if (record.isPresent()) {
            // 软删除原记录并检查结果
            int deleteCount = medicalRecordRepository.softDeleteByRecordId(id);
            if (deleteCount <= 0) {
                // 软删除失败，抛出异常触发事务回滚
                throw new RuntimeException("软删除失败，记录ID: " + id);
            }

            // 创建新记录
            MedicalRecord newRecord = new MedicalRecord();
            newRecord.setPatientId(recordDetails.getPatientId());
            newRecord.setRecordTime(recordDetails.getRecordTime());
            newRecord.setRecordingDoctor(recordDetails.getRecordingDoctor());
            newRecord.setMedicalContent(recordDetails.getMedicalContent());
            newRecord.setModifyingDoctor(recordDetails.getModifyingDoctor());
            newRecord.setModificationTime(recordDetails.getModificationTime());
            newRecord.setRecordType(recordDetails.getRecordType());

            MedicalRecord savedRecord = medicalRecordRepository.save(newRecord);
            // 只返回记录ID，确保前端正确获取
            return ResponseEntity.ok(savedRecord.getRecordId());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicalRecord(@PathVariable int id) {
        if (medicalRecordRepository.existsById(id)) {
            medicalRecordRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 新增软删除接口
    @PutMapping("/{recordId}/soft-delete")
    @Transactional
    public ResponseEntity<Void> softDeleteMedicalRecord(@PathVariable("recordId") int recordId) {
        medicalRecordRepository.softDeleteByRecordId(recordId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/latest-summary")
    public ResponseEntity<String> getLatestPatientSummary(@RequestParam String patientId) {
        List<PatientPromptResultDTO> results = promptResultRepository.findMedicalSummaryByPatientId(patientId);
        if (results.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(results.get(0).getOriginalResultContent());
    }

    /**
     * 根据PATIENTID查询未删除的病历记录
     * @param patientId 病人ID
     * @return 病历记录列表
     */
    @GetMapping("/emr-by-patient")
    public ResponseEntity<List<EmrContent>> getEmrRecordsByPatientId(@RequestParam String patientId) {
        List<EmrContent> records = emrRecordService.getEmrRecordsByPatientId(patientId);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/admission-content")
    public ResponseEntity<List<String>> getAdmissionRecordContent(@RequestParam String patientId) {
        List<String> contents = emrRecordService.getAdmissionRecordContent(patientId);
        return ResponseEntity.ok(contents);
    }

    /**
     * 接口1：获取病人病历记录列表
     * 根据病人ID获取EMR-Record中病历记录列表，DELETEMARK=0
     * 
     * @param patientId 病人ID
     * @return 病历记录列表，包含ID、DOC_TYPE_NAME、DOC_TITLE_TIME字段
     */
    @GetMapping("/emr-list")
    public ResponseEntity<List<EmrRecordListDTO>> getEmrRecordList(@RequestParam String patientId) {
        List<EmrRecordListDTO> records = emrRecordService.getEmrRecordListByPatientId(patientId);
        return ResponseEntity.ok(records);
    }

    /**
     * 接口2：获取病历记录内容
     * 根据记录ID获取病历记录内容
     * 
     * @param id 记录ID
     * @return 病历记录内容
     */
    @GetMapping("/emr-content/{id}")
    public ResponseEntity<EmrRecordContentDTO> getEmrRecordContent(@PathVariable String id) {
        // 处理空记录ID的情况
        if (id == null || id.trim().isEmpty()) {
            return ResponseEntity.ok(new EmrRecordContentDTO(""));
        }
        EmrRecordContentDTO content = emrRecordService.getEmrRecordContentById(id);
        return ResponseEntity.ok(content);
    }
    
}
