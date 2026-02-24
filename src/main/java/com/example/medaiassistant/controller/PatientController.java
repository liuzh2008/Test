package com.example.medaiassistant.controller;

import com.example.medaiassistant.dto.PatientDTO;
import com.example.medaiassistant.dto.AddDiagnosisDTO;
import com.example.medaiassistant.dto.PatientSaveRequest;
import com.example.medaiassistant.dto.PatientSaveResponse;
import java.util.Comparator;
import java.util.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import com.example.medaiassistant.util.AgeCalculator;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.model.LongTermOrder;
import com.example.medaiassistant.model.Diagnosis;
import com.example.medaiassistant.model.ConversationHistory;
import com.example.medaiassistant.repository.PatientRepository;
import com.example.medaiassistant.repository.LongTermOrderRepository;
import com.example.medaiassistant.repository.DiagnosisRepository;
import com.example.medaiassistant.repository.ConversationHistoryRepository;
import com.example.medaiassistant.service.OrderFormatService;
import com.example.medaiassistant.service.PatientStatusUpdateService;
import com.example.medaiassistant.service.PatientSaveService;
import com.example.medaiassistant.hospital.service.PatientSyncService;
import com.example.medaiassistant.hospital.service.HospitalConfigService;
import com.example.medaiassistant.hospital.dto.PatientSyncResult;
import com.example.medaiassistant.dto.ConversationHistoryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;
import com.example.medaiassistant.dto.OrderTimelineDTO;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private static final Logger log = LoggerFactory.getLogger(PatientController.class);

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private LongTermOrderRepository longTermOrderRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private OrderFormatService orderFormatService;

    @Autowired
    private ConversationHistoryRepository conversationHistoryRepository;

    @Autowired
    private PatientStatusUpdateService patientStatusUpdateService;

    @Autowired
    private PatientSaveService patientSaveService;

    @Autowired
    private PatientSyncService patientSyncService;

    @Autowired
    private HospitalConfigService hospitalConfigService;

/**
     * 根据科室获取在院患者列表
     * 
     * 该接口用于获取指定科室的所有在院患者信息，包括患者的基本信息和当前状态
     * 返回的患者信息包含患者ID、姓名、性别、出生日期、床位号、入院时间、科室、病历号和状态
     * 
     * 请求示例：
     * GET /api/patients/by-department?department=心内科
     * 
     * 响应示例：
     * [
     *   {
     *     "patientId": "0000077563",
     *     "name": "张三",
     *     "gender": "M",
     *     "dateOfBirth": "1980-01-01T00:00:00.000+00:00",
     *     "bedNumber": "3-22",
     *     "admissionTime": "2025-08-01T09:30:00.000+00:00",
     *     "department": "心内科",
     *     "medicalRecordNumber": "MR000001",
     *     "status": "病危"
     *   }
     * ]
     * 
     * @param department 科室名称，用于筛选指定科室的在院患者
     * @return List<PatientDTO> 在院患者信息列表，包含患者的基本信息和状态
     * @since 2025-08-10
     * @author Cline
     */
    @GetMapping("/by-department")
    public ResponseEntity<?> getPatientsByDepartment(
            @RequestParam String department,
            @RequestParam(required = false, defaultValue = "true") Boolean sync) {
        
        // 1. 从医院配置服务获取启用的医院ID
        String hospitalId = getEnabledHospitalId();
        if (hospitalId == null) {
            return ResponseEntity.badRequest()
                    .body("未找到启用的医院配置，请检查医院配置文件");
        }
        
        // 2. 如果sync=true（默认），执行病人数据同步
        if (Boolean.TRUE.equals(sync)) {
            try {
                PatientSyncResult syncResult = patientSyncService.syncPatients(hospitalId, department);
                log.info("科室 {} 病人数据同步完成: 新增={}, 更新={}, 出院={}", 
                        department, syncResult.getAddedCount(), 
                        syncResult.getUpdatedCount(), syncResult.getDischargedCount());
            } catch (Exception e) {
                log.error("科室 {} 病人数据同步失败，继续返回现有数据: {}", department, e.getMessage());
                // 同步失败时继续返回现有数据，不中断接口
            }
        }
        
        // 3. 查询并返回同步后的病人列表
        List<Patient> patients = patientRepository.findByDepartmentAndIsInHospital(department, true);
        List<PatientDTO> patientDTOs = patients.stream().map(patient -> {
            PatientDTO dto = new PatientDTO();
            dto.setPatientId(patient.getPatientId());
            dto.setName(patient.getName());
            dto.setGender(patient.getGender());
            dto.setDateOfBirth(patient.getDateOfBirth());
            dto.setBedNumber(patient.getBedNumber());
            dto.setAdmissionTime(patient.getAdmissionTime());
            dto.setDepartment(patient.getDepartment());
            dto.setMedicalRecordNumber(patient.getMedicalRecordNumber());
            dto.setStatus(patient.getStatus());
            return dto;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(patientDTOs);
    }
    
    /**
     * 获取启用的医院ID
     * 从医院配置服务获取启用的医院配置，返回第一个启用的医院ID
     * 
     * @return 启用的医院ID，如果没有启用的医院配置则返回null
     */
    private String getEnabledHospitalId() {
        List<com.example.medaiassistant.hospital.model.HospitalConfig> enabledConfigs = 
            hospitalConfigService.getEnabledConfigs();
        
        if (enabledConfigs.isEmpty()) {
            log.warn("未找到启用的医院配置");
            return null;
        }
        
        String hospitalId = enabledConfigs.get(0).getId();
        log.debug("获取到启用的医院ID: {}", hospitalId);
        return hospitalId;
    }

    @GetMapping("/{patientId}/long-term-orders")
    public List<LongTermOrder> getLongTermOrdersByPatientId(@PathVariable String patientId) {
        return longTermOrderRepository.findByPatientIdAndRepeatIndicatorOrderByOrderDateAsc(patientId, 1);
    }

    @GetMapping("/{patientId}/temporary-orders")
    public List<LongTermOrder> getTemporaryOrdersByPatientId(@PathVariable String patientId) {
        return longTermOrderRepository.findByPatientIdAndRepeatIndicatorOrderByOrderDateAsc(patientId, 0);
    }

    @GetMapping("/{patientId}/diagnoses")
    public List<Diagnosis> getDiagnosesByPatientId(@PathVariable String patientId) {
        return diagnosisRepository.findByPatientId(patientId);
    }

    @PostMapping("/{patientId}/diagnoses")
    public Diagnosis addDiagnosis(@PathVariable String patientId, @RequestBody AddDiagnosisDTO dto) {
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setPatientId(patientId);
        diagnosis.setDiagnosisText(dto.getDiagnosisText());
        diagnosis.setIcd10Code(dto.getDiagnosisCode());
        diagnosis.setDiagnosisTime(new Date());
        diagnosis.setDiagnosedBy("system"); 
        diagnosis.setIsPrimary(0);
        diagnosis.setStatusFlag(1);
        return diagnosisRepository.save(diagnosis);
    }

    @GetMapping("/{patientId}/formatted-orders")
    public List<String> getFormattedLongTermOrders(@PathVariable String patientId) {
        return orderFormatService.formatLongTermOrders(patientId);
    }

    @GetMapping("/{patientId}/formatted-temporary-orders")
    public String getFormattedTemporaryOrders(@PathVariable String patientId) {
        List<LongTermOrder> orders = longTermOrderRepository.findByPatientIdAndRepeatIndicatorOrderByOrderDateAsc(patientId, 0);
        List<OrderTimelineDTO> dtos = orders.stream()
            .map(order -> new OrderTimelineDTO(
                order.getOrderDate().toInstant().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy年M月d日 H:mm")) + " " + 
                    order.getOrderName() + " " + 
                    (order.getDosage() != null ? order.getDosage() : "") + 
                    (order.getUnit() != null ? order.getUnit() : "") + " " + 
                    (order.getFrequency() != null ? order.getFrequency() : "") + " " + 
                    (order.getRoute() != null ? order.getRoute() : ""),
                Collections.emptyList()))
            .collect(Collectors.toList());
        return orderFormatService.formatTemporaryOrders(dtos);
    }

    @GetMapping("/{patientId}/basic-info")
    public String getPatientBasicInfo(@PathVariable String patientId) {
        Patient patient = patientRepository.findById(patientId).orElseThrow();
        
        // 使用安全的年龄计算方法
        int age = AgeCalculator.calculateAge(patient.getDateOfBirth(), 0);
        
        // 计算住院天数
        LocalDate admissionDate = patient.getAdmissionTime().toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate();
        long days = ChronoUnit.DAYS.between(admissionDate, LocalDate.now());
        
        // 格式化日期
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy年MM月dd日 HH:mm");
        String admissionTime = sdf.format(patient.getAdmissionTime());
        
        // 转换性别为中文（数据库值：1=男性，2=女性）
        String genderInChinese;
        switch (patient.getGender()) {
            case "1":
                genderInChinese = "男";
                break;
            case "2":
                genderInChinese = "女";
                break;
            default:
                genderInChinese = "未确定";
        }
        
        return String.format("性别：%s，年龄：%d岁，入院时间：%s，住院时间：%d天。",
            genderInChinese, age, admissionTime, days);
    }

    @DeleteMapping("/diagnoses/{diagnosisId}")
    public ResponseEntity<?> softDeleteDiagnosis(@PathVariable Integer diagnosisId) {
        try {
            diagnosisRepository.softDeleteById(diagnosisId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("删除失败");
        }
    }

    @GetMapping("/{patientId}/conversation-history")
    public List<ConversationHistoryDTO> getConversationHistoryByPatientId(@PathVariable String patientId) {
        List<ConversationHistory> histories = conversationHistoryRepository.findByPatientId(patientId);
        
        // 按时间戳排序，相同时间user放在前面
        histories.sort(Comparator
            .comparing(ConversationHistory::getTimestamp)
            .thenComparing(history -> history.getMessageType() == ConversationHistory.MessageType.user ? 0 : 1)
        );
        
        return histories.stream().map(history -> {
            ConversationHistoryDTO dto = new ConversationHistoryDTO();
            dto.setSessionId(history.getSessionId());
            dto.setUserId(history.getUserId());
            dto.setPatientId(history.getPatientId());
            dto.setMessageType(history.getMessageType());
            dto.setContent(history.getContent());
            dto.setModelName(history.getModelName());
            dto.setTimestamp(history.getTimestamp());
            return dto;
        }).collect(Collectors.toList());
    }
    
    /**
     * 智能更新患者状态API
     * 
     * 该接口用于根据传入的状态参数智能更新指定患者的当前状态。
     * 该方法会根据患者当前状态和新状态的语义关系进行智能处理，而不是直接替换。
     * 
     * 智能处理逻辑：
     * - 如果患者当前状态为空，直接设置为新状态
     * - 如果新状态与当前状态相同，返回提示信息
     * - 如果新状态有"普通"或"病重"或"病危"，则用新状态中的"普通"或"病重"或"病危"替换原状态中的"普通"或"病重"或"病危"
     * - 如果新状态有"入院"或"出院"，则用新状态中的"入院"或"出院"替换原状态中的"入院"或"出院"
     * 
     * 请求示例：
     * PUT /api/patients/12345/status?status=病危
     * 
     * 响应示例：
     * 成功：HTTP 200 OK，返回"患者状态更新成功，从 '普通' 更新为 '病危'"
     * 失败：HTTP 400 Bad Request，返回"患者状态更新失败: 错误信息"
     * 
     * @param patientId 患者ID，用于标识要更新状态的患者，必须是数据库中已存在的患者ID
     * @param status 新的状态值，将用于更新患者的status字段，支持智能处理多种状态变更
     * @return ResponseEntity<String> 响应实体，包含更新结果信息
     *         - 成功：HTTP 200 OK，返回"患者状态更新成功，从 '原状态' 更新为 '新状态'"
     *         - 失败：HTTP 400 Bad Request，返回"患者状态更新失败: 错误信息"
     * @since 2025-08-10
     * @author Cline
     * @see PatientStatusUpdateService#updatePatientStatusByParam(String, String)
     */
    @PutMapping("/{patientId}/status")
    public ResponseEntity<String> updatePatientStatus(@PathVariable String patientId, @RequestParam String status) {
        try {
            String result = patientStatusUpdateService.updatePatientStatusByParam(patientId, status);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("患者状态更新失败: " + e.getMessage());
        }
    }

    /**
     * 查询7天内出院的患者，按出院时间倒序排列
     * 
     * 该接口用于查询指定科室在过去7天内出院的患者信息，
     * 并按出院时间从最新到最旧排序，支持分页功能。
     * 
     * 请求示例：
     * GET /api/patients/discharged-in-7days?department=心内科&page=0&size=10
     * 
     * 响应示例：
     * {
     *   "content": [
     *     {
     *       "patientId": "0000077563",
     *       "name": "张三",
     *       "gender": "M",
     *       "dateOfBirth": "1980-01-01T00:00:00.000+00:00",
     *       "bedNumber": "3-22",
     *       "admissionTime": "2025-08-01T09:30:00.000+00:00",
     *       "dischargeTime": "2025-08-10T14:30:00.000+00:00",
     *       "department": "心内科",
     *       "medicalRecordNumber": "MR000001",
     *       "status": "出院"
     *     }
     *   ],
     *   "totalElements": 15,
     *   "totalPages": 2,
     *   "size": 10,
     *   "number": 0
     * }
     * 
     * @param department 科室名称，用于筛选指定科室的出院患者
     * @param pageable 分页参数（可选，默认page=0, size=20）
     * @return 分页的患者信息列表，包含患者的基本信息和出院时间，按出院时间倒序排列
     * @since 2025-09-22
     * @author Cline
     */
    @GetMapping("/discharged-in-7days")
    public Page<PatientDTO> getDischargedPatientsInLast7Days(
            @RequestParam String department,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        // 计算日期范围：当前时间往前推7天
        Date endDate = new Date();
        Date startDate = new Date(endDate.getTime() - 7 * 24 * 60 * 60 * 1000L);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dischargeTime"));
        
        Page<Patient> patients = patientRepository.findDischargedPatientsByDepartmentInLast7Days(
            department, startDate, endDate, pageable
        );
        
        return patients.map(patient -> {
            PatientDTO dto = new PatientDTO();
            dto.setPatientId(patient.getPatientId());
            dto.setName(patient.getName());
            dto.setGender(patient.getGender());
            dto.setDateOfBirth(patient.getDateOfBirth());
            dto.setBedNumber(patient.getBedNumber());
            dto.setAdmissionTime(patient.getAdmissionTime());
            dto.setDischargeTime(patient.getDischargeTime());
            dto.setDepartment(patient.getDepartment());
            dto.setMedicalRecordNumber(patient.getMedicalRecordNumber());
            dto.setStatus(patient.getStatus());
            return dto;
        });
    }
    
    /**
     * 保存或更新患者数据
     * 
     * 该接口用于保存新的患者数据或更新现有的患者数据。
     * 基于patientId作为唯一标识，如果患者已存在则更新数据，否则创建新记录。
     * 
     * 请求示例：
     * POST /api/patients/save-or-update
     * Content-Type: application/json
     * 
     * {
     *   "patientId": "TEST001",
     *   "name": "张三",
     *   "gender": "男",
     *   "dateOfBirth": "1980-01-01T00:00:00.000+00:00",
     *   "bedNumber": "101",
     *   "admissionTime": "2025-12-10T10:30:00.000+00:00",
     *   "dischargeTime": null,
     *   "isInHospital": true,
     *   "department": "心血管一病区",
     *   "importantInformation": "重要信息备注",
     *   "status": "普通"
     * }
     * 
     * 响应示例：
     * 成功创建：
     * {
     *   "success": true,
     *   "operation": "created",
     *   "patientId": "TEST001",
     *   "message": "患者数据保存成功",
     *   "timestamp": "2025-12-10T10:30:00.000+00:00"
     * }
     * 
     * 成功更新：
     * {
     *   "success": true,
     *   "operation": "updated",
     *   "patientId": "TEST001",
     *   "message": "患者数据更新成功",
     *   "timestamp": "2025-12-10T10:30:00.000+00:00"
     * }
     * 
     * 验证失败：
     * {
     *   "success": false,
     *   "operation": "error",
     *   "patientId": "TEST001",
     *   "message": "数据验证失败: 患者姓名不能为空; 科室不能为空;",
     *   "timestamp": "2025-12-10T10:30:00.000+00:00"
     * }
     * 
     * @param request 患者数据保存请求，包含PATIENTS表的所有字段
     * @return PatientSaveResponse 保存操作结果，包含操作状态和详细信息
     * @since 2025-12-10
     * @author System
     */
    @PostMapping("/save-or-update")
    public ResponseEntity<PatientSaveResponse> savePatient(@Valid @RequestBody PatientSaveRequest request) {
        try {
            PatientSaveResponse response = patientSaveService.savePatient(request);
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PatientSaveResponse.databaseError(request.getPatientId(), e.getMessage()));
        }
    }
    
}
