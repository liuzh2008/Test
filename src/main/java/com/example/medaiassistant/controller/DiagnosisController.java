package com.example.medaiassistant.controller;

import com.example.medaiassistant.dto.ReplaceDiagnosisDTO;
import com.example.medaiassistant.model.Diagnosis;
import com.example.medaiassistant.repository.DiagnosisRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/diagnosis")
public class DiagnosisController {

        private final DiagnosisRepository diagnosisRepository;

        public DiagnosisController(DiagnosisRepository diagnosisRepository) {
                this.diagnosisRepository = diagnosisRepository;
        }

        @PostMapping("/replace")
        @Transactional(rollbackFor = Exception.class)
        public ResponseEntity<?> replaceDiagnosis(@RequestBody ReplaceDiagnosisDTO dto) {
                try {
                        // 1. 查询原记录
                        Diagnosis originalDiagnosis = diagnosisRepository.findById(dto.getOldDiagnosisId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "找不到原诊断记录，ID: " + dto.getOldDiagnosisId()));

                        // 2. 软删除原记录
                        int deletedCount = diagnosisRepository.softDeleteById(dto.getOldDiagnosisId());
                        if (deletedCount <= 0) {
                                throw new RuntimeException("软删除失败，记录ID: " + dto.getOldDiagnosisId());
                        }

                        // 3. 创建并保存新诊断记录（合并新旧数据）
                        Diagnosis newDiagnosis = new Diagnosis();
                        // 使用传入的值，如果未传入则使用原记录的值
                        newDiagnosis
                                        .setPatientId(dto.getPatientId() != null ? dto.getPatientId()
                                                        : originalDiagnosis.getPatientId());
                        newDiagnosis.setDiagnosisType(
                                        dto.getDiagnosisType() != null ? dto.getDiagnosisType()
                                                        : originalDiagnosis.getDiagnosisType());
                        newDiagnosis
                                        .setIcd10Code(dto.getIcd10Code() != null ? dto.getIcd10Code()
                                                        : originalDiagnosis.getIcd10Code());
                        newDiagnosis.setDiagnosisText(
                                        dto.getDiagnosisText() != null ? dto.getDiagnosisText()
                                                        : originalDiagnosis.getDiagnosisText());
                        newDiagnosis.setDiagnosedBy(
                                        dto.getDiagnosedBy() != null ? dto.getDiagnosedBy()
                                                        : originalDiagnosis.getDiagnosedBy());
                        newDiagnosis.setDiagnosisTime(
                                        dto.getDiagnosisTime() != null ? dto.getDiagnosisTime()
                                                        : originalDiagnosis.getDiagnosisTime());
                        newDiagnosis
                                        .setIsPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary()
                                                        : originalDiagnosis.getIsPrimary());
                        newDiagnosis.setParentId(dto.getParentId() != null ? dto.getParentId()
                                        : originalDiagnosis.getParentId());
                        newDiagnosis.setStatusFlag(
                                        dto.getStatusFlag() != null ? dto.getStatusFlag()
                                                        : originalDiagnosis.getStatusFlag());
                        newDiagnosis.setModificationType(dto.getModificationType() != null ? dto.getModificationType()
                                        : originalDiagnosis.getModificationType());
                        newDiagnosis
                                        .setAddReason(dto.getAddReason() != null ? dto.getAddReason()
                                                        : originalDiagnosis.getAddReason());
                        newDiagnosis.setDiagnosisIndex(
                                        dto.getDiagnosisIndex() != null ? dto.getDiagnosisIndex()
                                                        : originalDiagnosis.getDiagnosisIndex());
                        newDiagnosis.setIsDeleted(0);

                        Diagnosis savedDiagnosis = diagnosisRepository.save(newDiagnosis);

                        // 4. 返回新记录
                        return ResponseEntity.ok(savedDiagnosis);

                } catch (Exception e) {
                        return ResponseEntity.internalServerError()
                                        .body("操作失败: " + e.getMessage());
                }
        }

        @GetMapping("/combined/{patientId}")
        public ResponseEntity<String> getCombinedDiagnosis(@PathVariable String patientId) {
                List<Diagnosis> diagnoses = diagnosisRepository.findByPatientId(patientId);
                if (diagnoses.isEmpty()) {
                        return ResponseEntity.ok("目前诊断：无");
                }

                String combined = diagnoses.stream()
                                .map(Diagnosis::getDiagnosisText)
                                .collect(Collectors.joining("，"));

                return ResponseEntity.ok("目前诊断：" + combined);
        }

        @GetMapping("/names/{patientId}")
        public ResponseEntity<List<String>> getPatientDiagnosisNames(@PathVariable String patientId) {
                List<Diagnosis> diagnoses = diagnosisRepository.findByPatientId(patientId);
                List<String> diagnosisNames = diagnoses.stream()
                                .map(Diagnosis::getDiagnosisText)
                                .collect(Collectors.toList());
                return ResponseEntity.ok(diagnosisNames);
        }
}
