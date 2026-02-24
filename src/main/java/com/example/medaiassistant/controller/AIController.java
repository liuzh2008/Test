package com.example.medaiassistant.controller;

import com.example.medaiassistant.config.AIModelConfig;
import com.example.medaiassistant.dto.SavePromptDTO;
import com.example.medaiassistant.dto.CleanDataRequest;
import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.repository.PromptRepository;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.example.medaiassistant.repository.PromptResultRepository;
import com.example.medaiassistant.repository.PromptTemplateRepository;
import com.example.medaiassistant.dto.PatientPromptResultDTO;
import com.example.medaiassistant.dto.PromptTemplateSimpleDTO;
import com.example.medaiassistant.dto.SaveAIResultDTO;
import com.example.medaiassistant.dto.UpdatePromptTemplateDTO;
import com.example.medaiassistant.dto.CreatePromptTemplateDTO;
import com.example.medaiassistant.dto.PromptListDTO;
import com.example.medaiassistant.dto.PromptDetailSimpleDTO;
import com.example.medaiassistant.dto.ReceiveResultDTO;
import com.example.medaiassistant.model.PromptTemplate;
import com.example.medaiassistant.model.PromptResult;
import com.example.medaiassistant.service.NetworkRecoveryService;
import java.time.LocalDateTime;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Date;
import java.util.Arrays;
import com.example.medaiassistant.dto.ConversationHistoryDTO;
import com.example.medaiassistant.model.ConversationHistory;
import com.example.medaiassistant.repository.ConversationHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// 新增的导入
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.model.Diagnosis;
import com.example.medaiassistant.model.MedicalRecord;
import com.example.medaiassistant.model.LongTermOrder;
import com.example.medaiassistant.model.LabResult;
import com.example.medaiassistant.model.ExaminationResult;
import com.example.medaiassistant.model.AlertTask;
import com.example.medaiassistant.model.EmrContent;
import com.example.medaiassistant.repository.PatientRepository;
import com.example.medaiassistant.repository.DiagnosisRepository;
import com.example.medaiassistant.repository.MedicalRecordRepository;
import com.example.medaiassistant.repository.LongTermOrderRepository;
import com.example.medaiassistant.repository.LabResultRepository;
import com.example.medaiassistant.repository.ExaminationResultRepository;
import com.example.medaiassistant.repository.AlertTaskRepository;
import com.example.medaiassistant.repository.EmrContentRepository;
import com.example.medaiassistant.service.OrderFormatService;
import com.example.medaiassistant.service.ExaminationResultService;
import com.example.medaiassistant.service.EmrRecordService;
import com.example.medaiassistant.service.PatientDataDesensitizationService;
import com.example.medaiassistant.util.AgeCalculator;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Collections;
import java.time.format.DateTimeFormatter;
import com.example.medaiassistant.dto.OrderTimelineDTO;
import com.example.medaiassistant.constant.AIDisclaimerConstants;
import com.example.medaiassistant.util.AIContentResponseWrapper;


/**
 * AI控制器类
 * 
 * 该类负责处理所有与AI相关的HTTP请求，包括Prompt模板管理、患者数据获取、AI结果保存等功能。
 * 作为RESTful API控制器，提供了一系列端点用于前端与AI服务的交互。
 * 
 * 主要功能包括：
 * - Prompt模板的创建、查询、更新和状态管理
 * - 患者相关数据的获取和格式化
 * - AI生成结果的保存和管理
 * - 对话历史的记录和查询
 * - 系统健康状态检查
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/ai")
public class AIController {
    private static final Logger logger = LoggerFactory.getLogger(AIController.class);

    private final RestTemplate restTemplate;
    private final PromptResultRepository promptResultRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final PromptRepository promptRepository;
    private final ConversationHistoryRepository conversationHistoryRepository;
    private final AIModelConfig aiModelConfig;

    // 新增的依赖项
    private final PatientRepository patientRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final LongTermOrderRepository longTermOrderRepository;
    private final LabResultRepository labResultRepository;
    private final ExaminationResultRepository examinationResultRepository;
    private final AlertTaskRepository alertTaskRepository;
    private final OrderFormatService orderFormatService;
    private final ExaminationResultService examinationResultService;
    private final EmrRecordService emrRecordService;
    private final PatientDataDesensitizationService patientDataDesensitizationService;
    private final NetworkRecoveryService networkRecoveryService;
    private final EmrContentRepository emrContentRepository;


    @Value("${api.base.url:http://localhost:8081}")
    private String apiBaseUrl;

    public AIController(RestTemplate restTemplate,
            PromptResultRepository promptResultRepository, PromptTemplateRepository promptTemplateRepository,
            PromptRepository promptRepository, ConversationHistoryRepository conversationHistoryRepository,
            AIModelConfig aiModelConfig,
            // 新增的依赖项
            PatientRepository patientRepository,
            DiagnosisRepository diagnosisRepository,
            MedicalRecordRepository medicalRecordRepository,
            LongTermOrderRepository longTermOrderRepository,
            LabResultRepository labResultRepository,
            ExaminationResultRepository examinationResultRepository,
            AlertTaskRepository alertTaskRepository, // 新增
            OrderFormatService orderFormatService,
            ExaminationResultService examinationResultService,
            EmrRecordService emrRecordService,
            PatientDataDesensitizationService patientDataDesensitizationService,
            NetworkRecoveryService networkRecoveryService,
            EmrContentRepository emrContentRepository) {
        this.restTemplate = restTemplate;
        this.promptResultRepository = promptResultRepository;
        this.promptTemplateRepository = promptTemplateRepository;
        this.promptRepository = promptRepository;
        this.conversationHistoryRepository = conversationHistoryRepository;
        this.aiModelConfig = aiModelConfig;
        // 初始化新增的依赖项
        this.patientRepository = patientRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.longTermOrderRepository = longTermOrderRepository;
        this.labResultRepository = labResultRepository;
        this.examinationResultRepository = examinationResultRepository;
        this.alertTaskRepository = alertTaskRepository;
        this.orderFormatService = orderFormatService;
        this.examinationResultService = examinationResultService;
        this.emrRecordService = emrRecordService;
        this.patientDataDesensitizationService = patientDataDesensitizationService;
        this.networkRecoveryService = networkRecoveryService;
        this.emrContentRepository = emrContentRepository;
    }

    /**
     * 获取患者Prompt结果列表
     * 
     * 该接口用于获取指定患者的所有Prompt生成结果，包括医疗摘要等信息。
     * 主要用于前端展示患者的AI生成内容历史记录。
     * 
     * @param patientId 患者ID，必需参数
     * @return ResponseEntity<Map<String, Object>> 包含患者Prompt结果的响应实体
     *         成功时返回患者的所有Prompt结果列表，并附带AI免责声明
     *         失败时返回空列表
     */
    @GetMapping("/patientPromptResults")
    public ResponseEntity<Map<String, Object>> getPatientPromptResults(
            @RequestParam String patientId) {
        List<PatientPromptResultDTO> results = promptResultRepository.findMedicalSummaryByPatientId(patientId);
        return ResponseEntity.ok(AIContentResponseWrapper.wrapWithDisclaimer(results));
    }

    /**
     * 获取患者Prompt详情列表（轻量级）
     * 返回不包含大文本内容的快速列表，用于性能优化
     * 
     * @param patientId 患者ID
     * @return 轻量级Prompt详情列表响应实体，附带AI免责声明
     */
    @GetMapping("/patientPromptDetails")
    public ResponseEntity<Map<String, Object>> getPatientPromptDetails(
            @RequestParam String patientId) {
        List<PromptDetailSimpleDTO> results = promptResultRepository.findPromptSimpleDetailsByPatientId(patientId);

        // 为每个结果添加预览内容
        results.forEach(result -> {
            // 这里可以添加预览内容的逻辑，比如截取前100个字符
            // 实际实现需要根据业务需求调整
            result.setObjectiveContentPreview("点击查看详情");
            result.setDailyRecordsPreview("点击查看详情");
        });

        return ResponseEntity.ok(AIContentResponseWrapper.wrapWithDisclaimer(results));
    }

    /**
     * 获取单个Prompt详情（完整信息）
     * 按需加载完整的大文本内容，用于详情查看
     * 
     * @param resultId 结果ID
     * @return 完整的Prompt详情响应实体，附带AI免责声明
     */
    @GetMapping("/patientPromptDetail/{resultId}")
    public ResponseEntity<Map<String, Object>> getPatientPromptDetail(
            @PathVariable Integer resultId) {
        Optional<PromptResult> result = promptResultRepository.findByIdWithDetails(resultId);
        if (result.isPresent()) {
            return ResponseEntity.ok(AIContentResponseWrapper.wrapWithDisclaimer(result.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 标记Prompt结果为已读
     * 
     * @param resultId 结果ID
     * @return 更新结果
     */
    @PutMapping("/patientPromptResult/{resultId}/markAsRead")
    public ResponseEntity<Map<String, Object>> markPromptResultAsRead(@PathVariable Integer resultId) {
        try {
            Optional<PromptResult> resultOptional = promptResultRepository.findById(resultId);
            if (resultOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", "NOT_FOUND",
                                "message", "PromptResult not found: " + resultId));
            }

            PromptResult promptResult = resultOptional.get();
            
            // 检查是否已删除
            if (promptResult.getDeleted() != null && promptResult.getDeleted() == 1) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", "DELETED",
                                "message", "PromptResult has been deleted: " + resultId));
            }

            // 更新isRead为1
            promptResult.setIsRead(1);
            promptResult.setUpdatedAt(java.time.LocalDateTime.now());
            promptResultRepository.save(promptResult);

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "resultId", resultId,
                    "isRead", 1,
                    "timestamp", System.currentTimeMillis()));
        } catch (Exception e) {
            logger.error("标记Prompt结果为已读失败，resultId: {}", resultId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "Failed to mark as read: " + e.getMessage()));
        }
    }

    /**
     * 软删除Prompt结果
     *
     * 根据 PromptResult 的 resultId 软删除结果记录（设置 deleted=1），
     * 并同步清理 PROMPTS 表中对应记录的 resultId 关联字段。
     *
     * @param resultId PromptResult 主键ID
     * @return 删除结果状态
     */
    @DeleteMapping("/patientPromptResult/{resultId}")
    public ResponseEntity<Map<String, Object>> deletePatientPromptResult(@PathVariable Integer resultId) {
        try {
            // 1. 查询目标 PromptResult（只查询未删除的）
            Optional<PromptResult> resultOptional = promptResultRepository.findById(resultId);
            if (resultOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", "NOT_FOUND",
                                "message", "PromptResult not found or already deleted: " + resultId));
            }

            PromptResult promptResult = resultOptional.get();
            
            // 检查是否已经被删除
            if (promptResult.getDeleted() != null && promptResult.getDeleted() == 1) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", "ALREADY_DELETED",
                                "message", "PromptResult already deleted: " + resultId));
            }

            Integer promptId = promptResult.getPromptId();

            // 2. 软删除 PromptResult 记录（设置 deleted=1）
            promptResult.setDeleted(1);
            promptResult.setUpdatedAt(LocalDateTime.now());
            promptResultRepository.save(promptResult);

            // 3. 清理 PROMPTS 表中对应记录的 resultId 关联字段
            if (promptId != null) {
                Optional<Prompt> promptOptional = promptRepository.findById(promptId);
                if (promptOptional.isPresent()) {
                    Prompt prompt = promptOptional.get();
                    prompt.setResultId(null);
                    promptRepository.save(prompt);
                }
            }

            // 4. 构造成功响应
            Map<String, Object> response = new HashMap<>();
            response.put("status", "DELETED");
            response.put("resultId", resultId);
            response.put("promptId", promptId);
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("软删除 PromptResult 成功 - ResultId: {}, PromptId: {}", resultId, promptId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("软删除 PromptResult 失败，ResultId: {}", resultId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "Failed to delete PromptResult: " + e.getMessage()));
        }
    }

    /**
     * 获取所有Prompt模板
     * 
     * 该接口用于获取系统中所有的Prompt模板，包括激活和非激活状态的模板。
     * 返回的模板信息为轻量级格式，仅包含基本属性，用于前端展示模板列表。
     * 
     * @return ResponseEntity<List<PromptTemplateSimpleDTO>> 包含所有Prompt模板的响应实体
     *         成功时返回所有Prompt模板的简化信息列表
     *         失败时返回空列表
     */
    @GetMapping("/promptTemplates")
    public ResponseEntity<List<PromptTemplateSimpleDTO>> getAllPromptTemplates() {
        List<PromptTemplate> templates = promptTemplateRepository.findAll();
        List<PromptTemplateSimpleDTO> result = templates.stream()
                .map(t -> new PromptTemplateSimpleDTO(
                        t.getPromptId(),
                        t.getPromptType(),
                        t.getPromptName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * 获取激活状态的Prompt模板列表
     * 
     * 该接口用于获取系统中所有处于激活状态的Prompt模板。
     * 返回的模板信息为轻量级格式，仅包含基本属性，用于前端展示可用的模板列表。
     * 
     * @return ResponseEntity<List<PromptTemplateSimpleDTO>> 包含激活Prompt模板的响应实体
     *         成功时返回所有激活状态的Prompt模板简化信息列表
     *         失败时返回空列表
     */
    @GetMapping("/activePromptTemplates")
    public ResponseEntity<List<PromptTemplateSimpleDTO>> getActivePromptTemplates() {
        List<PromptTemplate> templates = promptTemplateRepository.findByIsActive(true);
        List<PromptTemplateSimpleDTO> result = templates.stream()
                .map(t -> new PromptTemplateSimpleDTO(
                        t.getPromptId(),
                        t.getPromptType(),
                        t.getPromptName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * 根据Prompt类型获取激活状态的Prompt模板列表
     * 
     * @param promptType Prompt模板类型
     * @return 激活状态的Prompt模板基本信息列表
     */
    @GetMapping("/activePromptTemplatesByType")
    public ResponseEntity<List<PromptTemplateSimpleDTO>> getActivePromptTemplatesByType(
            @RequestParam String promptType) {
        List<PromptTemplate> templates = promptTemplateRepository.findByIsActiveAndPromptType(true, promptType);
        List<PromptTemplateSimpleDTO> result = templates.stream()
                .map(t -> new PromptTemplateSimpleDTO(
                        t.getPromptId(),
                        t.getPromptType(),
                        t.getPromptName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * 根据类型和名称获取Prompt内容
     * 
     * 该接口用于根据Prompt模板的类型和名称获取具体的Prompt内容。
     * 主要用于前端获取特定Prompt模板的文本内容，用于AI模型输入。
     * 
     * @param promptType Prompt模板类型，必需参数
     * @param promptName Prompt模板名称，必需参数
     * @return ResponseEntity<String> 包含Prompt内容的响应实体
     *         成功时返回Prompt模板的文本内容
     *         失败时返回404状态码和错误信息
     */
    @GetMapping("/prompt")
    public ResponseEntity<String> getPromptByTypeAndName(
            @RequestParam String promptType,
            @RequestParam String promptName) {

        logger.info("Requesting prompt - Type: '{}', Name: '{}'", promptType, promptName);
        PromptTemplate promptTemplate = promptTemplateRepository.findByPromptTypeAndPromptName(promptType, promptName);

        if (promptTemplate == null) {
            logger.warn("Prompt not found - Type: '{}', Name: '{}'", promptType, promptName);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Prompt not found");
        }

        logger.info("Found prompt - ID: {}", promptTemplate.getPromptId());
        return ResponseEntity.ok(promptTemplate.getPrompt());
    }

    @GetMapping("/promptTemplate")
    public ResponseEntity<PromptTemplate> getPromptTemplateByTypeAndName(
            @RequestParam String promptType,
            @RequestParam String promptName) {

        logger.info("Requesting full prompt template - Type: '{}', Name: '{}'", promptType, promptName);
        PromptTemplate promptTemplate = promptTemplateRepository.findByPromptTypeAndPromptName(promptType, promptName);

        if (promptTemplate == null) {
            logger.warn("Prompt template not found - Type: '{}', Name: '{}'", promptType, promptName);
            return ResponseEntity.notFound().build();
        }

        logger.info("Found prompt template - ID: {}", promptTemplate.getPromptId());
        return ResponseEntity.ok(promptTemplate);
    }

    /**
     * 获取患者综合数据
     * 
     * 该接口用于获取指定患者的综合医疗数据，包括一般信息、诊断信息、病情小结、病历记录、
     * 医嘱信息、化验结果、检查结果等。支持根据Prompt模板动态确定需要获取的数据类型。
     * 
     * 数据获取特点：
     * - 每种数据类型独立获取，互不影响
     * - 错误处理友好，单个数据类型失败不影响其他数据获取
     * - 返回格式化的字符串，便于AI模型分析
     * - 支持模板化数据需求配置
     * 
     * 处理流程：
     * 1. 根据Prompt模板确定需要获取的数据类型
     * 2. 按数据类型顺序获取数据
     * 3. 格式化数据并组合成最终结果
     * 4. 返回格式化的患者数据字符串
     * 
     * 错误处理策略：
     * - 单个数据类型获取失败时，返回友好的空数据提示
     * - 错误信息不会添加到返回结果中，避免干扰AI分析
     * - 详细的错误日志记录便于问题排查
     * 
     * @param patientId 患者ID，必需参数，用于标识需要获取数据的患者
     * @param promptType Prompt模板类型，可选参数，用于确定需要的数据类型
     * @param promptName Prompt模板名称，可选参数，与promptType配合使用
     * @return ResponseEntity<String> 包含患者综合数据的格式化字符串响应实体
     *         成功时返回格式化的患者数据字符串
     *         失败时返回500状态码和错误信息
     * 
     * 返回格式示例：
     * ### 一般信息:
     * 性别：男，年龄：45岁，入院时间：2025年07月15日 09:30，住院时间：12天。
     * ------
     * ### 诊断信息:
     * 原发性高血压，2型糖尿病
     * ------
     * ### 病情小结:
     * 患者目前病情稳定...
     * ------
     * 
     * @throws Exception 当数据处理过程中发生异常时抛出
     * 
     * @HTTP 200 请求成功处理，返回患者数据
     * @HTTP 500 服务器内部错误，处理过程中发生异常
     * 
     * @example 请求示例：
     * GET /api/ai/patient-data?patientId=12345&promptType=diagnosis&promptName=summary
     * 
     * @example 响应示例：
     * HTTP/1.1 200 OK
     * Content-Type: text/plain;charset=UTF-8
     * 
     * ### 一般信息:
     * 性别：男，年龄：45岁，入院时间：2025年07月15日 09:30，住院时间：12天。
     * ------
     * ### 诊断信息:
     * 原发性高血压，2型糖尿病
     * ------
     */
    /**
     * 获取患者综合数据接口（优化版：直接数据库查询）
     * 
     * <p>根据Prompt模板的requiredDataTypes获取并组合患者数据，用于AI分析生成诊断、诊疗计划、查房记录等提示词。
     * <strong>2026-02-14重大优化：</strong>消除所有HTTP自调用，改为直接数据库查询，解决生产环境间歇性超时问题。</p>
     * 
     * <h3>架构优化背景</h3>
     * <p><strong>原有架构缺陷：</strong></p>
     * <ul>
     *   <li>WebFlux（响应式）与RestTemplate（阻塞式）混用，导致线程池死锁</li>
     *   <li>HTTP自调用死锁：在响应式线程中调用同服务的多个HTTP接口</li>
     *   <li>数据库连接池竞争：一个请求触发10个内部HTTP调用，快速耗尽连接池</li>
     *   <li>生产环境典型错误：Read timed out、404 Not Found、ResourceAccessException</li>
     * </ul>
     * 
     * <p><strong>优化方案：</strong></p>
     * <ul>
     *   <li>彻底消除HTTP自调用，改为直接Repository查询</li>
     *   <li>复用现有服务：OrderFormatService、ExaminationResultService</li>
     *   <li>内联PatientController.getPatientBasicInfo()的格式化逻辑</li>
     *   <li>预期响应时间从5-30秒降至0.5-2秒</li>
     *   <li>超时错误从频繁发生降至几乎消除</li>
     * </ul>
     * 
     * <h3>接口功能</h3>
     * <p>根据患者ID和Prompt模板配置，获取患者的综合医疗数据，包括：</p>
     * <ul>
     *   <li>一般信息：性别、年龄、入院时间、住院天数（直接查询patientRepository）</li>
     *   <li>诊断信息：目前诊断列表（直接查询diagnosisRepository）</li>
     *   <li>病情小结：最新病情小结内容（直接查询medicalRecordRepository）</li>
     *   <li>病历记录：病历时间轴记录（直接查询medicalRecordRepository）</li>
     *   <li>长期医嘱：格式化的长期医嘱列表（调用orderFormatService）</li>
     *   <li>临时医嘱：格式化的临时医嘱列表（直接查询+orderFormatService）</li>
     *   <li>化验结果：格式化的化验报告（直接查询+formatLabResults）</li>
     *   <li>检查结果：格式化的检查报告（调用examinationResultService）</li>
     *   <li>入院记录：入院记录总结或原始记录（三层降级策略）</li>
     *   <li>手术记录：手术/操作记录（模糊匹配查询）</li>
     * </ul>
     * 
     * <h3>数据类型决策逻辑</h3>
     * <ol>
     *   <li>若同时提供promptType和promptName，从PromptTemplate表读取requiredDataTypes配置</li>
     *   <li>若模板配置缺少"手术记录"，代码自动补充（强制包含逻辑）</li>
     *   <li>若模板不存在或配置为空，使用默认10种数据类型</li>
     *   <li>若未提供promptType/promptName参数，直接使用默认10种数据类型</li>
     * </ol>
     * 
     * <h3>入院记录三层降级策略</h3>
     * <ol>
     *   <li>优先：PromptResult中的"入院记录总结"（originalResultContent或modifiedResultContent）</li>
     *   <li>降级：EMR_CONTENT表的原始入院记录内容列表</li>
     *   <li>兜底：输出"无入院记录数据"标记，供定时任务识别为无入院病历场景</li>
     * </ol>
     * 
     * <h3>手术记录模糊匹配策略</h3>
     * <ul>
     *   <li>查询条件：DOC_TYPE_NAME LIKE '%手术记录%' OR DOC_TYPE_NAME LIKE '%操作记录%'</li>
     *   <li>兼容命名变体：手术记录、手术/操作记录、操作记录等</li>
     *   <li>强制包含：无论模板配置如何，代码层面自动补充</li>
     * </ul>
     * 
     * @param patientId 患者ID（必填），格式如"990500000283143_1"
     * @param promptType Prompt类型（可选），用于查找对应的PromptTemplate
     * @param promptName Prompt名称（可选），用于查找对应的PromptTemplate
     * 
     * @return ResponseEntity<String> 返回格式化的患者综合数据文本，按板块分段（例如"### 一般信息:"、"### 入院记录:"等）
     * 
     * @throws Exception 数据库查询异常会被捕获并记录日志，不影响其他数据板块的获取
     * 
     * @apiNote
     * <ul>
     *   <li>接口路径：GET /api/ai/patient-data</li>
     *   <li>响应格式：text/plain，Markdown格式的分段文本</li>
     *   <li>性能指标：优化后响应时间 < 2秒（原5-30秒）</li>
     * </ul>
     * 
     * @see PatientController#getPatientBasicInfo(String) 参考了一般信息的格式化逻辑
     * @see OrderFormatService#formatLongTermOrders(String) 长期医嘱格式化服务
     * @see OrderFormatService#formatTemporaryOrders(List) 临时医嘱格式化服务
     * @see ExaminationResultService#getFormattedResultsByPatientId(String) 检查结果格式化服务
     * @see EmrRecordService#getAdmissionRecordContent(String) 入院记录降级服务
     * 
     * @since 2024
     * @version 2.0.0 (2026-02-14 架构优化版本)
     * @author MedAI Assistant Team
     * 
     * @implNote 
     * 关键修改历史：
     * - 2026-02-14：消除HTTP自调用，改为直接数据库查询，解决生产环境超时问题
     * - 2026-02-13：添加手术记录强制包含逻辑
     * - 2026-02-09：实现入院记录三层降级策略
     */
    @GetMapping("/patient-data")
    public ResponseEntity<String> getPatientData(
            @RequestParam String patientId,
            @RequestParam(required = false) String promptType,
            @RequestParam(required = false) String promptName) {

        logger.info("Requesting patient data - patientId: {}, promptType: {}, promptName: {}",
                patientId, promptType, promptName);

        try {
            /**
             * 数据类型决策逻辑
             * 
             * @description
             * 根据请求参数决定需要获取的数据类型列表：
             * 1. 若同时提供promptType和promptName，从PromptTemplate表读取requiredDataTypes配置
             * 2. 若模板配置缺少"手术记录"，代码自动补充，确保手术记录始终被获取
             * 3. 若模板不存在或配置为空，使用硬编码的默认10种数据类型
             * 4. 若未提供promptType/promptName参数，直接使用默认10种数据类型
             * 
             * @since 2026-02-13 添加手术记录强制包含逻辑
             */
            String[] dataTypes;
            if (promptType != null && promptName != null) {
                PromptTemplate promptTemplate = promptTemplateRepository.findByPromptTypeAndPromptName(promptType,
                        promptName);
                if (promptTemplate != null && promptTemplate.getRequiredDataTypes() != null) {
                    String configuredTypes = promptTemplate.getRequiredDataTypes();
                    /**
                     * 手术记录强制包含逻辑
                     * 
                     * @description
                     * 无论模板配置是否包含"手术记录"，代码层面会自动补充。
                     * 这确保了手动生成和自动生成Prompt时病人资料的一致性。
                     * 
                     * @reason 解决自动生成Prompt时缺少手术记录的问题
                     * @see doc/问题修复/自动生成Prompt缺少手术记录问题修复-2026年02月13日.md
                     */
                    if (!configuredTypes.contains("手术记录")) {
                        logger.info("Template '{}' missing '手术记录', adding it automatically", promptName);
                        configuredTypes = configuredTypes + ",手术记录";
                    }
                    dataTypes = configuredTypes.split(",");
                } else {
                    dataTypes = new String[] {
                            "一般信息",
                            "诊断信息",
                            "病情小结",
                            "病历记录",
                            "长期医嘱",
                            "临时医嘱",
                            "化验结果",
                            "检查结果",
                            "入院记录",
                            "会诊记录",
                            "手术记录"
                    };
                }
            } else {
                dataTypes = new String[] {
                        "一般信息",
                        "诊断信息",
                        "病情小结",
                        "病历记录",
                        "长期医嘱",
                        "临时医嘱",
                        "化验结果",
                        "检查结果",
                        "入院记录",
                        "会诊记录",
                        "手术记录"
                };
            }
            StringBuilder result = new StringBuilder();

            // 3. 根据数据类型获取数据
            for (String dataType : dataTypes) {
                String trimmedType = dataType.trim().toLowerCase();
                logger.info("Processing data type: '{}' (trimmed: '{}')", dataType, trimmedType);

                try {
                    String apiResponse = null;
                    switch (trimmedType) {
                        case "一般信息":
                            logger.info("Fetching basic info for patient: {}", patientId);
                            try {
                                // 直接查询数据库，避免HTTP调用超时
                                Optional<Patient> patientOpt = patientRepository.findById(patientId);
                                if (patientOpt.isPresent()) {
                                    Patient patient = patientOpt.get();
                                    
                                    // 使用安全的年龄计算方法
                                    int age = AgeCalculator.calculateAge(patient.getDateOfBirth(), 0);
                                    
                                    // 计算住院天数
                                    long days = 0;
                                    String admissionTime = "未知";
                                    if (patient.getAdmissionTime() != null) {
                                        LocalDate admissionDate = patient.getAdmissionTime().toInstant()
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalDate();
                                        days = ChronoUnit.DAYS.between(admissionDate, LocalDate.now());
                                        
                                        // 格式化日期
                                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
                                        admissionTime = sdf.format(patient.getAdmissionTime());
                                    }
                                    
                                    // 转换性别为中文（数据库值：1=男性，2=女性）
                                    String genderInChinese;
                                    switch (patient.getGender() != null ? patient.getGender() : "") {
                                        case "1":
                                            genderInChinese = "男";
                                            break;
                                        case "2":
                                            genderInChinese = "女";
                                            break;
                                        default:
                                            genderInChinese = "未确定";
                                    }
                                    
                                    String basicInfo = String.format("性别：%s，年龄：%d岁，入院时间：%s，住院时间：%d天。",
                                        genderInChinese, age, admissionTime, days);
                                    
                                    result.append("### 一般信息:\n").append(basicInfo);
                                    apiResponse = "success";
                                } else {
                                    logger.warn("Patient not found: {}", patientId);
                                    result.append("### 一般信息:\n无基本信息数据");
                                }
                            } catch (Exception e) {
                                logger.error("Failed to get basic info for patient: {}", patientId, e);
                                result.append("### 一般信息:\n无基本信息数据");
                            }
                            break;
                        case "诊断信息":
                            /**
                             * 获取患者诊断信息
                             * 
                             * 该方法通过直接查询数据库获取患者的诊断信息，避免了通过REST API调用可能带来的网络问题。
                             * 如果查询到诊断信息，则将其格式化后添加到结果中；如果没有查询到，则添加提示信息；
                             * 如果查询过程中出现异常，则记录错误日志但不添加错误信息到结果中。
                             * 
                             * @param patientId 患者ID
                             * @param result    结果字符串构建器
                             */
                            try {
                                logger.info("Attempting to get diagnosis for patient: {}", patientId);
                                // 直接使用repository查询替代restTemplate调用，避免网络问题
                                List<Diagnosis> diagnoses = diagnosisRepository.findByPatientId(patientId);
                                logger.info("Found {} diagnosis records for patient: {}", diagnoses.size(), patientId);

                                if (diagnoses != null && !diagnoses.isEmpty()) {
                                    List<String> diagnosisNames = diagnoses.stream()
                                            .map(Diagnosis::getDiagnosisText)
                                            .collect(Collectors.toList());
                                    String diagnosisText = String.join(", ", diagnosisNames);
                                    logger.info("Diagnosis text for patient {}: {}", patientId, diagnosisText);
                                    result.append("### 目前诊断:\n")
                                            .append(diagnosisText);
                                } else {
                                    logger.warn("No diagnosis found for patient: {}", patientId);
                                    result.append("### 目前诊断:\n无诊断信息数据");
                                }
                            } catch (Exception e) {
                                logger.error("Failed to get diagnosis names for patient: {}", patientId, e);
                                result.append("### 目前诊断:\n无诊断信息数据");
                            }
                            break;
                        case "病情小结":
                            try {
                                logger.info("Attempting to get medical summary for patient: {}", patientId);
                                // 直接查询数据库，获取最新的病情小结
                                List<MedicalRecord> summaries = medicalRecordRepository
                                    .findByPatientIdAndDeletedOrderByRecordTimeDesc(patientId, 0);
                                
                                // 过滤出病情小结类型的记录
                                Optional<MedicalRecord> latestSummary = summaries.stream()
                                    .filter(r -> r.getRecordType() != null && r.getRecordType().contains("病情小结"))
                                    .findFirst();
                                
                                if (latestSummary.isPresent() && latestSummary.get().getMedicalContent() != null) {
                                    result.append("### 病情小结:\n")
                                            .append(latestSummary.get().getMedicalContent());
                                    apiResponse = "success";
                                } else {
                                    logger.warn("No medical summary found for patient: {}", patientId);
                                    result.append("### 病情小结:\n无病情小结数据");
                                }
                            } catch (Exception e) {
                                logger.error("Failed to get medical summary for patient: {}", patientId, e);
                                result.append("### 病情小结:\n无病情小结数据");
                            }
                            break;
                        case "病历记录":
                            /**
                             * 获取患者病历记录
                             * 
                             * 该方法通过直接查询数据库获取患者的病历记录，避免了通过REST API调用可能带来的网络超时问题。
                             * 如果查询到病历记录，则将其格式化后添加到结果中；如果没有查询到，则添加提示信息。
                             * 
                             * @param patientId 患者ID
                             * @param result    结果字符串构建器
                             */
                            try {
                                logger.info("Attempting to get medical records for patient: {}", patientId);
                                // 直接使用repository查询替代restTemplate调用，避免网络超时问题
                                List<MedicalRecord> records = medicalRecordRepository.findByPatientIdAndDeletedOrderByRecordTimeDesc(patientId, 0);
                                logger.info("Found {} medical records for patient: {}", records.size(), patientId);

                                if (records != null && !records.isEmpty()) {
                                    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy年MM月dd日 HH:mm");
                                    StringBuilder recordsBuilder = new StringBuilder();
                                    for (MedicalRecord record : records) {
                                        if (record.getRecordTime() != null) {
                                            recordsBuilder.append(dateFormat.format(record.getRecordTime())).append("\n");
                                        }
                                        if (record.getMedicalContent() != null) {
                                            recordsBuilder.append(record.getMedicalContent()).append("\n\n");
                                        }
                                    }
                                    result.append("### 病历记录:\n").append(recordsBuilder.toString().trim());
                                    apiResponse = "success";
                                } else {
                                    logger.warn("No medical records found for patient: {}", patientId);
                                    result.append("### 病历记录:\n无病历记录数据");
                                }
                            } catch (Exception e) {
                                logger.error("Failed to get medical records for patient: {}", patientId, e);
                                result.append("### 病历记录:\n无病历记录数据");
                            }
                            break;
                        case "长期医嘱":
                            try {
                                logger.info("Attempting to get long-term orders for patient: {}", patientId);
                                // 直接调用OrderFormatService格式化长期医嘱
                                List<String> formattedOrders = orderFormatService.formatLongTermOrders(patientId);
                                
                                if (formattedOrders != null && !formattedOrders.isEmpty()) {
                                    result.append("### 长期医嘱:\n")
                                            .append(String.join("\n", formattedOrders));
                                    apiResponse = "success";
                                } else {
                                    logger.warn("No long-term orders found for patient: {}", patientId);
                                    result.append("### 长期医嘱:\n无长期医嘱数据");
                                }
                            } catch (Exception e) {
                                logger.error("Failed to get long-term orders for patient: {}", patientId, e);
                                result.append("### 长期医嘱:\n无长期医嘱数据");
                            }
                            break;
                        case "临时医嘱":
                            try {
                                logger.info("Attempting to get temporary orders for patient: {}", patientId);
                                // 直接查询数据库并格式化临时医嘱
                                List<LongTermOrder> tempOrders = longTermOrderRepository
                                    .findByPatientIdAndRepeatIndicatorOrderByOrderDateAsc(patientId, 0);
                                
                                if (tempOrders != null && !tempOrders.isEmpty()) {
                                    // 转换为OrderTimelineDTO格式
                                    List<OrderTimelineDTO> dtos = tempOrders.stream()
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
                                    
                                    String formattedTempOrders = orderFormatService.formatTemporaryOrders(dtos);
                                    result.append("### 临时医嘱:\n")
                                            .append(formattedTempOrders.isEmpty() ? "无临时医嘱数据" : formattedTempOrders);
                                    apiResponse = "success";
                                } else {
                                    logger.warn("No temporary orders found for patient: {}", patientId);
                                    result.append("### 临时医嘱:\n无临时医嘱数据");
                                }
                            } catch (Exception e) {
                                logger.error("Failed to get temporary orders for patient: {}", patientId, e);
                                result.append("### 临时医嘱:\n无临时医嘱数据");
                            }
                            break;
                        case "化验结果":
                            try {
                                logger.info("Attempting to get lab results for patient: {}", patientId);
                                // 直接查询数据库并格式化化验结果
                                List<LabResult> labResults = labResultRepository.findByPatientId(patientId);
                                
                                if (labResults != null && !labResults.isEmpty()) {
                                    String formattedLabResults = formatLabResults(labResults);
                                    result.append("### 化验结果:\n")
                                            .append(formattedLabResults.isEmpty() ? "无化验结果数据" : formattedLabResults);
                                    apiResponse = "success";
                                } else {
                                    logger.warn("No lab results found for patient: {}", patientId);
                                    result.append("### 化验结果:\n无化验结果数据");
                                }
                            } catch (Exception e) {
                                logger.error("Failed to get lab results for patient: {}", patientId, e);
                                result.append("### 化验结果:\n无化验结果数据");
                            }
                            break;
                        case "检查结果":
                            try {
                                logger.info("Attempting to get examination results for patient: {}", patientId);
                                // 直接调用ExaminationResultService格式化检查结果
                                String formattedExamResults = examinationResultService
                                    .getFormattedResultsByPatientId(patientId);
                                
                                if (formattedExamResults != null && !formattedExamResults.isEmpty()) {
                                    result.append("### 检查结果:\n").append(formattedExamResults);
                                    apiResponse = "success";
                                } else {
                                    logger.warn("No examination results found for patient: {}", patientId);
                                    result.append("### 检查结果:\n无检查结果数据");
                                }
                            } catch (Exception e) {
                                logger.error("Failed to get examination results for patient: {}", patientId, e);
                                result.append("### 检查结果:\n无检查结果数据");
                            }
                            break;
                        case "入院记录":
                            /**
                             * 入院记录数据获取逻辑（入院记录总结优先）
                             * 1. 优先从 PromptResult 中查找模板名包含“入院记录总结”的结果内容作为入院记录总结；
                             * 2. 若无入院记录总结，则从 EMR_CONTENT 表获取原始入院记录内容列表并拼接；
                             * 3. 当两者都不存在或查询异常时，输出“无入院记录数据”作为无入院病历的统一标记，供定时任务识别并跳过分析。
                             */
                            try {
                                // 1. 优先使用 PromptResult 中的入院记录总结
                                List<PatientPromptResultDTO> summaryResults = promptResultRepository.findMedicalSummaryByPatientId(patientId);
                                String admissionSummary = null;

                                if (summaryResults != null && !summaryResults.isEmpty()) {
                                    for (PatientPromptResultDTO dto : summaryResults) {
                                        String templateName = dto.getPromptTemplateName();
                                        if (templateName != null && templateName.contains("入院记录总结")) {
                                            String content = dto.getOriginalResultContent();
                                            if (content == null || content.isEmpty()) {
                                                content = dto.getModifiedResultContent();
                                            }
                                            if (content != null && !content.isEmpty()) {
                                                admissionSummary = content;
                                                break;
                                            }
                                        }
                                    }
                                }

                                if (admissionSummary != null) {
                                    result.append("### 入院记录:\n")
                                            .append(admissionSummary);
                                    apiResponse = "success";
                                } else {
                                    // 2. 无入院记录总结时，降级到 EMR_CONTENT 原始入院记录
                                    List<String> admissionContents = emrRecordService.getAdmissionRecordContent(patientId);
                                    if (admissionContents != null && !admissionContents.isEmpty()) {
                                        result.append("### 入院记录:\n")
                                                .append(String.join("\n", admissionContents));
                                        apiResponse = "success";
                                    } else {
                                        // 3. 两者都不存在时，统一标记为无入院记录数据
                                        result.append("### 入院记录:\n无入院记录数据");
                                        apiResponse = null;
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("Failed to get admission record for patient: {}", patientId, e);
                                result.append("### 入院记录:\n无入院记录数据");
                                apiResponse = null;
                            }
                            break;
                        case "会诊记录":
                            /**
                             * 会诊记录数据获取逻辑
                             *
                             * 数据来源：EMR_CONTENT 表中 DOC_TYPE_NAME = "会诊记录" 的记录
                             * 处理方式参考"手术记录"：
                             *  - 输出时间 + 会诊内容 + 记录人
                             *  - 多条记录之间使用 "------" 分隔
                             *  - 无数据或异常时输出统一标记"无会诊记录数据"
                             */
                            try {
                                logger.info("Attempting to get consultation records for patient: {}", patientId);
                                // 通过 EmrRecordService 从 EMR_CONTENT 表获取 DOC_TYPE_NAME='会诊记录' 的记录
                                List<EmrContent> consultationRecords =
                                        emrRecordService.getEmrRecordsByPatientIdAndDocType(patientId, "会诊记录");
                                int recordCount = consultationRecords != null ? consultationRecords.size() : 0;
                                logger.info("Found {} consultation records for patient: {}", recordCount, patientId);

                                if (consultationRecords != null && !consultationRecords.isEmpty()) {
                                    java.text.SimpleDateFormat dateFormat =
                                            new java.text.SimpleDateFormat("yyyy年MM月dd日 HH:mm");
                                    StringBuilder consultationBuilder = new StringBuilder();
                                    for (EmrContent record : consultationRecords) {
                                        if (record.getRecordDate() != null) {
                                            consultationBuilder
                                                    .append("记录时间: ")
                                                    .append(dateFormat.format(record.getRecordDate()))
                                                    .append("\n");
                                        }
                                        if (record.getContent() != null && !record.getContent().isEmpty()) {
                                            consultationBuilder
                                                    .append(record.getContent())
                                                    .append("\n");
                                        }
                                        if (record.getCreateBy() != null && !record.getCreateBy().isEmpty()) {
                                            consultationBuilder
                                                    .append("记录人: ")
                                                    .append(record.getCreateBy())
                                                    .append("\n");
                                        }
                                        consultationBuilder.append("------\n");
                                    }
                                    result.append("### 会诊记录:\n")
                                            .append(consultationBuilder.toString().trim());
                                    apiResponse = "success";
                                } else {
                                    logger.warn("No consultation records found for patient: {}", patientId);
                                    result.append("### 会诊记录:\n无会诊记录数据");
                                }
                            } catch (Exception e) {
                                logger.error("Failed to get consultation records for patient: {}", patientId, e);
                                result.append("### 会诊记录:\n无会诊记录数据");
                            }
                            break;
                        case "手术记录":
                            /**
                             * 获取患者手术记录
                             * 
                             * @description
                             * 使用统一的模糊匹配查询方法，兼容EMR_CONTENT表中DOC_TYPE_NAME字段的多种命名变体。
                             * 避免因精确匹配导致的手术记录遍漏问题。
                             * 
                             * @param patientId 患者ID
                             * @param result 结果字符串构建器
                             * 
                             * @process
                             * 1. 调用 emrContentRepository.findOperationsByPatientId(patientId) 查询手术相关记录
                             * 2. 查询条件：DOC_TYPE_NAME LIKE '%手术记录%' OR DOC_TYPE_NAME LIKE '%操作记录%'
                             * 3. 兼容的命名变体包括：
                             *    - "手术记录"
                             *    - "手术/操作记录"
                             *    - "操作记录"
                             *    - 其他包含相关关键字的变体
                             * 4. 若查询到记录，按以下格式输出：
                             *    - 记录时间：yyyy年MM月dd日 HH:mm
                             *    - 手术记录内容
                             *    - 记录人
                             *    - 分隔线 "------"
                             * 5. 若无数据或查询异常，输出"无手术记录数据"
                             * 
                             * @errorHandling
                             * - 查询异常时记录错误日志，不影响其他数据类型的获取
                             * - 返回统一格式的"无手术记录数据"标记
                             * 
                             * @example
                             * // 有手术记录的情况
                             * ### 手术记录:
                             * 记录时间: 2026年02月08日 14:30
                             * （手术记录内容）
                             * 记录人: 张医生
                             * ------
                             * 
                             * // 无手术记录的情况
                             * ### 手术记录:
                             * 无手术记录数据
                             * 
                             * @since 2026-02-08
                             * @author System
                             */
                            try {
                                logger.info("Attempting to get surgery records for patient: {}", patientId);
                                // 使用统一的手术记录查询方法，兼容"手术记录"、"操作记录"、"手术/操作记录"等多种命名
                                List<EmrContent> surgeryRecords = emrContentRepository.findOperationsByPatientId(patientId);
                                logger.info("Found {} surgery records for patient: {}", surgeryRecords != null ? surgeryRecords.size() : 0, patientId);

                                if (surgeryRecords != null && !surgeryRecords.isEmpty()) {
                                    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy年MM月dd日 HH:mm");
                                    StringBuilder surgeriesBuilder = new StringBuilder();
                                    for (EmrContent record : surgeryRecords) {
                                        if (record.getRecordDate() != null) {
                                            surgeriesBuilder.append("记录时间: ").append(dateFormat.format(record.getRecordDate())).append("\n");
                                        }
                                        if (record.getContent() != null && !record.getContent().isEmpty()) {
                                            surgeriesBuilder.append(record.getContent()).append("\n");
                                        }
                                        if (record.getCreateBy() != null) {
                                            surgeriesBuilder.append("记录人: ").append(record.getCreateBy()).append("\n");
                                        }
                                        surgeriesBuilder.append("------\n");
                                    }
                                    result.append("### 手术记录:\n").append(surgeriesBuilder.toString().trim());
                                    apiResponse = "success";
                                } else {
                                    logger.warn("No surgery records found for patient: {}", patientId);
                                    result.append("### 手术记录:\n无手术记录数据");
                                }
                            } catch (Exception e) {
                                logger.error("Failed to get surgery records for patient: {}", patientId, e);
                                result.append("### 手术记录:\n无手术记录数据");
                            }
                            break;
                        default:
                            logger.warn("Unknown data type: {}", trimmedType);
                            result.append("\n").append(dataType).append(":\n未知数据类型");
                    }
                    result.append("\n------\n");
                    logger.info("Processed data type: {}, response: {}", trimmedType,
                            apiResponse != null ? "success" : "null/empty");
                } catch (Exception e) {
                    logger.error("Failed to get data for type: {}", trimmedType, e);
                    result.append("\n").append(dataType).append(":\n数据获取失败: ")
                            .append(e.getMessage())
                            .append("\n------\n");
                }
            }

            String finalResult = result.toString();
            logger.info("Returning patient data - length: {}", finalResult.length());

            return ResponseEntity.ok(finalResult);
        } catch (Exception e) {
            logger.error("Error in getPatientData", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request");
        }
    }

    @PutMapping("/updatePromptTemplate")
    public ResponseEntity<Map<String, Object>> updatePromptTemplate(
            @RequestBody UpdatePromptTemplateDTO request) {
        try {
            logger.info("Updating prompt template - ID: {}", request.getPromptId());

            // 查找现有记录
            PromptTemplate template = promptTemplateRepository.findById(request.getPromptId()).orElse(null);
            if (template == null) {
                logger.error("Prompt template not found - ID: {}", request.getPromptId());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Prompt template not found"));
            }

            // 更新字段
            if (request.getPromptType() != null) {
                template.setPromptType(request.getPromptType());
            }
            if (request.getPromptName() != null) {
                template.setPromptName(request.getPromptName());
            }
            if (request.getPrompt() != null) {
                template.setPrompt(request.getPrompt());
            }
            if (request.getFilterRules() != null) {
                template.setFilterRules(request.getFilterRules());
            }
            if (request.getSpecialContent() != null) {
                template.setSpecialContent(request.getSpecialContent());
            }
            if (request.getRequiredDataTypes() != null) {
                template.setRequiredDataTypes(request.getRequiredDataTypes());
            }
            if (request.getScope() != null) {
                template.setScope(request.getScope());
            }
            if (request.getDepartmentId() != null) {
                template.setDepartmentId(request.getDepartmentId());
            }
            if (request.getIsActive() != null) {
                template.setIsActive(request.getIsActive());
            }

            // 保存更新
            PromptTemplate updatedTemplate = promptTemplateRepository.save(template);

            logger.info("Successfully updated prompt template - ID: {}", updatedTemplate.getPromptId());
            return ResponseEntity.ok(Map.of(
                    "id", updatedTemplate.getPromptId(),
                    "status", "UPDATED"));
        } catch (Exception e) {
            logger.error("Error updating prompt template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update prompt template: " + e.getMessage()));
        }
    }

    @PutMapping("/updatePromptActiveStatus")
    public ResponseEntity<Map<String, Object>> updatePromptActiveStatus(
            @RequestParam Integer promptId,
            @RequestParam Boolean isActive) {
        try {
            logger.info("Updating prompt active status - ID: {}, setting isActive to {}", promptId, isActive);

            PromptTemplate template = promptTemplateRepository.findById(promptId).orElse(null);
            if (template == null) {
                logger.error("Prompt template not found - ID: {}", promptId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Prompt template not found"));
            }

            template.setIsActive(isActive);
            promptTemplateRepository.save(template);

            logger.info("Successfully updated prompt active status - ID: {}", promptId);
            return ResponseEntity.ok(Map.of(
                    "id", promptId,
                    "status", "UPDATED",
                    "isActive", isActive));
        } catch (Exception e) {
            logger.error("Error updating prompt active status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update prompt active status: " + e.getMessage()));
        }
    }

    /**
     * 创建新的Prompt模板
     * 
     * 该接口用于创建新的Prompt模板记录，支持创建各种类型的Prompt模板
     * 
     * @param request CreatePromptTemplateDTO对象，包含需要创建的Prompt模板信息
     *                - promptType: Prompt类型（必填）
     *                - promptName: Prompt名称（必填）
     *                - prompt: Prompt内容（必填）
     *                - filterRules: 过滤规则（可选）
     *                - specialContent: 特殊内容（可选）
     *                - requiredDataTypes: 需要的数据类型（可选）
     *                - scope: 作用范围（可选）
     *                - departmentId: 科室ID（可选）
     *                - isActive: 是否激活（可选，默认true）
     * @return ResponseEntity<Map<String, Object>> 响应实体，包含创建结果信息
     *         成功时返回：
     *         - id: 创建的Prompt模板ID
     *         - status: 状态，固定为"CREATED"
     *         失败时返回：
     *         - error: 错误信息
     */
    @PostMapping("/promptTemplate")
    public ResponseEntity<Map<String, Object>> createPromptTemplate(
            @RequestBody CreatePromptTemplateDTO request) {
        try {
            logger.info("Creating new prompt template - Type: '{}', Name: '{}'",
                    request.getPromptType(), request.getPromptName());

            // 验证必填字段
            if (request.getPromptType() == null || request.getPromptType().trim().isEmpty()) {
                logger.error("Prompt type is required but not provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Prompt type is required"));
            }

            if (request.getPromptName() == null || request.getPromptName().trim().isEmpty()) {
                logger.error("Prompt name is required but not provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Prompt name is required"));
            }

            if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
                logger.error("Prompt content is required but not provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Prompt content is required"));
            }

            // 检查是否已存在相同类型和名称的模板
            PromptTemplate existingTemplate = promptTemplateRepository.findByPromptTypeAndPromptName(
                    request.getPromptType(), request.getPromptName());

            if (existingTemplate != null) {
                logger.error("Prompt template already exists - Type: '{}', Name: '{}'",
                        request.getPromptType(), request.getPromptName());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Prompt template already exists"));
            }

            // 创建新的Prompt模板
            PromptTemplate template = new PromptTemplate();
            template.setPromptType(request.getPromptType());
            template.setPromptName(request.getPromptName());
            template.setPrompt(request.getPrompt());
            template.setFilterRules(request.getFilterRules());
            template.setSpecialContent(request.getSpecialContent());
            template.setRequiredDataTypes(request.getRequiredDataTypes());
            template.setScope(request.getScope());
            template.setDepartmentId(request.getDepartmentId());
            template.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

            // 保存到数据库
            PromptTemplate savedTemplate = promptTemplateRepository.save(template);

            logger.info("Successfully created prompt template - ID: {}", savedTemplate.getPromptId());
            return ResponseEntity.ok(Map.of(
                    "id", savedTemplate.getPromptId(),
                    "status", "CREATED"));
        } catch (Exception e) {
            logger.error("Error creating prompt template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create prompt template: " + e.getMessage()));
        }
    }

    @PostMapping("/conversation")
    public ResponseEntity<Map<String, Object>> saveConversation(
            @RequestBody ConversationHistoryDTO dto) {
        try {
            logger.info("Saving conversation history - Session: {}, User: {}, Patient: {}",
                    dto.getSessionId(), dto.getUserId(), dto.getPatientId());

            ConversationHistory history = new ConversationHistory();
            history.setSessionId(dto.getSessionId());
            history.setUserId(dto.getUserId());
            history.setPatientId(dto.getPatientId());
            history.setMessageType(dto.getMessageType());
            history.setContent(dto.getContent());
            history.setModelName(dto.getModelName());
            history.setTimestamp(new Date());

            ConversationHistory saved = conversationHistoryRepository.save(history);

            return ResponseEntity.ok(Map.of(
                    "id", saved.getId(),
                    "status", "SAVED",
                    "timestamp", saved.getTimestamp()));
        } catch (Exception e) {
            logger.error("Error saving conversation history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save conversation: " + e.getMessage()));
        }
    }

    /**
     * 保存AI分析结果到数据库
     * 
     * 该接口用于保存或更新AI分析结果，支持创建新记录和更新已有记录两种场景。
     * 内置错误内容检测机制，只检测技术/系统错误（如"API错误"、"系统错误"），
     * 不会误判医学专业术语（如"功能异常"、"手术失败"）。
     * 
     * 处理流程：
     * 1. 验证必填参数：promptId不能为null
     * 2. 内容安全检测：检查是否包含系统错误关键词
     * 3. 记录查询：根据promptId查询现有PromptResult记录
     * 4. 数据处理：更新或创建记录
     * 5. 数据持久化：保存到PROMPT_RESULT表
     * 6. 保存验证：确认保存成功并返回有效ID
     * 7. 返回结果：包含id、status、timestamp
     * 
     * @param request SaveAIResultDTO对象，包含需要保存的AI结果信息
     *                - promptId (Integer, 必填): 关联的Prompt记录ID
     *                - content (String, 必填): 修改后的AI生成内容
     *                - originalContent (String, 条件必填): 原始AI生成内容，创建新记录时需要
     *                - lastModifiedBy (Integer, 可选): 最后修改人ID
     *                - isRead (Integer, 可选): 是否已读标记(0:未读,1:已读)，默认1
     * 
     * @return ResponseEntity<Map<String, Object>> 响应实体，包含保存结果信息
     *         成功时返回 (200 OK)：
     *         - id (Integer): 保存的PromptResult记录ID
     *         - status (String): 状态，固定为"SAVED"
     *         - timestamp (LocalDateTime): 创建时间
     *         失败时返回 (400 Bad Request / 500 Internal Server Error)：
     *         - error (String): 错误信息
     * 
     * @throws 无显式抛出异常，所有异常均被捕获并返回错误响应
     * 
     * @see SaveAIResultDTO
     * @see PromptResult
     * @see PromptResultRepository
     */
    @PostMapping("/saveResult")
    public ResponseEntity<Map<String, Object>> saveAIResult(
            @RequestBody SaveAIResultDTO request) {
        try {
            logger.info("Saving AI result - PromptId: {}, Content length: {}",
                    request.getPromptId(),
                    request.getContent() != null ? request.getContent().length() : 0);

            // 验证必填字段
            if (request.getPromptId() == null) {
                logger.error("PromptId is required but not provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "PromptId is required"));
            }

            // 检查LLM返回的内容中是否包含错误提示
            if (request.getContent() != null && containsErrorIndicators(request.getContent())) {
                logger.warn("AI result contains error indicators, not saving - PromptId: {}", request.getPromptId());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "AI result contains error indicators, not saving"));
            }

            PromptResult result;

            // 根据PromptId查询现有记录
            List<PromptResult> existingResults = promptResultRepository.findByPromptId(request.getPromptId());

            if (!existingResults.isEmpty()) {
                // 更新第一条找到的记录
                result = existingResults.get(0);
                result.setModifiedResultContent(request.getContent());
                result.setStatus("SAVED");
                result.setUpdatedAt(LocalDateTime.now());

                if (request.getLastModifiedBy() != null) {
                    result.setLastModifiedBy(request.getLastModifiedBy());
                }
                if (request.getIsRead() != null) {
                    result.setIsRead(request.getIsRead());
                }
            } else {
                // 创建新记录
                result = new PromptResult();
                result.setModifiedResultContent(request.getContent());
                result.setOriginalResultContent(request.getOriginalContent());
                result.setStatus("SAVED");
                result.setExecutionTime(LocalDateTime.now());
                result.setCreatedAt(LocalDateTime.now());
                result.setUpdatedAt(LocalDateTime.now());
                result.setPromptId(request.getPromptId());

                // 设置默认值：1表示已读
                result.setIsRead(request.getIsRead() != null ? request.getIsRead() : 1);
                result.setDeleted(0); // 默认未删除
            }

            // 验证必填字段
            if (request.getPromptId() == null) {
                logger.error("PromptId is required but not provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "PromptId is required"));
            }
            result.setPromptId(request.getPromptId());

            if (request.getLastModifiedBy() != null && request.getId() == null) {
                result.setLastModifiedBy(request.getLastModifiedBy());
            }

            PromptResult savedResult = promptResultRepository.save(result);

            // 验证保存结果
            if (savedResult == null || savedResult.getResultId() == null) {
                logger.error("Failed to save PromptResult - repository returned null or empty ID");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to save result to database"));
            }

            logger.info("Successfully saved ModifiedResultContent - ID: {}, Length: {} chars",
                    savedResult.getResultId(),
                    savedResult.getModifiedResultContent() != null ? savedResult.getModifiedResultContent().length()
                            : 0);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedResult.getResultId());
            response.put("status", savedResult.getStatus());
            response.put("timestamp", savedResult.getCreatedAt());

            logger.info("Successfully saved and verified AI result - ID: {}, Content: {} chars",
                    savedResult.getResultId(),
                    savedResult.getModifiedResultContent() != null ? savedResult.getModifiedResultContent().length()
                            : 0);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error saving AI result", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save AI result: " + e.getMessage()));
        }
    }

    @GetMapping("/health/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("timestamp", System.currentTimeMillis());
        response.put("service", "MedAI Assistant Backend");
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    /**
     * 数据清洗接口
     *
     * 该接口根据患者ID对传入的文本列表进行脱敏清洗，返回清洗后的文本列表。
     * 主要用于在调用大模型或对外输出前，对包含患者敏感信息的文本进行统一处理。
     *
     * 处理流程：
     * 1. 校验请求参数（patientId、texts）
     * 2. 调用 PatientDataDesensitizationService 批量脱敏
     * 3. 返回包含 cleanedTexts 的 JSON 结果
     *
     * @param request CleanDataRequest 对象，包含 patientId 与待清洗文本列表
     * @return ResponseEntity<Map<String, Object>> 标准响应，包含清洗后的文本列表
     */
    @PostMapping("/clean-data")
    public ResponseEntity<Map<String, Object>> cleanData(@RequestBody CleanDataRequest request) {
        try {
            if (request == null) {
                logger.error("CleanDataRequest is null");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Request body is required"));
            }

            String patientId = request.getPatientId();
            if (patientId == null || patientId.trim().isEmpty()) {
                logger.error("patientId is required but not provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "patientId is required"));
            }

            if (request.getTexts() == null || request.getTexts().isEmpty()) {
                logger.error("texts is required but empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "texts must not be empty"));
            }

            logger.info("Cleaning data for patientId: {}, texts count: {}", patientId, request.getTexts().size());

            String[] inputArray = request.getTexts().toArray(new String[0]);
            String[] cleanedArray = patientDataDesensitizationService.desensitizeTexts(inputArray, patientId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Data cleaned successfully");
            response.put("patientId", patientId);
            response.put("originalCount", request.getTexts().size());
            response.put("cleanedTexts", Arrays.asList(cleanedArray));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error cleaning data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to clean data: " + e.getMessage()));
        }
    }

    /**
     * 保存Prompt信息到数据库
     * 
     * 该方法接收前端传来的SavePromptDTO对象，将其转换为Prompt实体对象并保存到数据库中。
     * 在保存过程中，会对保存的结果进行验证，确保数据正确存储到数据库中，并返回保存结果给前端。
     * 
     * 处理流程：
     * 1. 从请求中提取Prompt信息并创建Prompt实体对象
     * 2. 设置Prompt实体对象的各项属性
     * 3. 调用Repository将Prompt实体保存到数据库
     * 4. 验证保存结果，确保数据正确存储
     * 5. 如果是入院记录总结，更新相关alert_tasks状态为已完成
     * 6. 返回保存结果给前端
     * 
     * @param request SavePromptDTO对象，包含需要保存的Prompt信息
     *                - userId: 用户ID
     *                - patientId: 患者ID
     *                - promptTemplateName: Prompt模板名称
     *                - objectiveContent: 目标内容
     *                - dailyRecords: 日常记录
     *                - promptTemplateContent: Prompt模板内容
     *                - priority: 优先级
     *                - submissionTime: 提交时间
     *                - sortNumber: 排序号
     *                - estimatedWaitTime: 预估等待时间
     *                - statusName: 状态名称
     *                - generatedBy: 生成者
     *                - executionTime: 执行时间
     *                - resultId: 结果ID
     *                - executionServerId: 执行服务器ID
     *                - generateCostTime: 生成耗时
     *                - processingStartTime: 处理开始时间
     *                - processingEndTime: 处理结束时间
     *                - retryCount: 重试次数
     * @return ResponseEntity<Map<String, Object>> 响应实体，包含保存结果信息
     *         成功时返回：
     *         - id: 保存的Prompt记录ID
     *         - status: 状态，固定为"SAVED"
     *         - timestamp: 提交时间
     *         失败时返回：
     *         - error: 错误信息
     * @throws Exception 当保存过程中发生异常时抛出
     * 
     *                   日志记录：
     *                   - INFO级别：记录保存操作的开始、保存结果和验证结果
     *                   - ERROR级别：记录保存失败的情况
     * 
     *                   异常处理：
     *                   - 捕获所有异常并记录错误日志
     *                   - 返回500状态码和错误信息给前端
     */
    @PostMapping("/savePrompt")
    @Transactional("transactionManager")
    public ResponseEntity<Map<String, Object>> savePrompt(
            @RequestBody SavePromptDTO request) {
        try {
            logger.info("Saving prompt - UserId: {}, PatientId: {}, Template: {}",
                    request.getUserId(), request.getPatientId(), request.getPromptTemplateName());

            // 在保存前进行数据脱敏处理
            if (request.getPatientId() != null) {
                String patientId = request.getPatientId();

                // 对objectiveContent进行脱敏
                if (request.getObjectiveContent() != null) {
                    String desensitizedObjectiveContent = patientDataDesensitizationService
                            .desensitizeTextByPatientId(request.getObjectiveContent(), patientId);
                    request.setObjectiveContent(desensitizedObjectiveContent);
                    logger.info("Objective content desensitized for patient: {}", patientId);
                }

                // 对dailyRecords进行脱敏
                if (request.getDailyRecords() != null) {
                    String desensitizedDailyRecords = patientDataDesensitizationService
                            .desensitizeTextByPatientId(request.getDailyRecords(), patientId);
                    request.setDailyRecords(desensitizedDailyRecords);
                    logger.info("Daily records desensitized for patient: {}", patientId);
                }
            }

            Prompt prompt = new Prompt();
            prompt.setUserId(request.getUserId());
            prompt.setPatientId(request.getPatientId());
            prompt.setPromptTemplateName(request.getPromptTemplateName());
            prompt.setObjectiveContent(request.getObjectiveContent());
            prompt.setDailyRecords(request.getDailyRecords());
            prompt.setPromptTemplateContent(request.getPromptTemplateContent());
            prompt.setPriority(request.getPriority());
            prompt.setSubmissionTime(
                    request.getSubmissionTime() != null ? request.getSubmissionTime() : LocalDateTime.now());
            prompt.setSortNumber(request.getSortNumber());
            prompt.setEstimatedWaitTime(request.getEstimatedWaitTime());
            prompt.setStatusName(request.getStatusName());
            prompt.setGeneratedBy(request.getGeneratedBy());
            prompt.setExecutionTime(request.getExecutionTime());
            prompt.setResultId(request.getResultId());
            prompt.setExecutionServerId(request.getExecutionServerId());
            prompt.setGenerateCostTime(request.getGenerateCostTime());
            prompt.setProcessingStartTime(request.getProcessingStartTime());
            prompt.setProcessingEndTime(request.getProcessingEndTime());
            prompt.setRetryCount(request.getRetryCount());

            logger.info("About to save prompt to database");
            Prompt savedPrompt = promptRepository.save(prompt);
            logger.info("Prompt saved to database, returned object id: {}",
                    savedPrompt != null ? savedPrompt.getPromptId() : "null");

            // 检查ID是否生成
            if (savedPrompt == null || savedPrompt.getPromptId() == null) {
                logger.error("Failed to generate prompt ID after save");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to generate prompt ID after save"));
            }

            // 验证数据是否真正保存到数据库
            Prompt verifiedPrompt = promptRepository.findById(savedPrompt.getPromptId()).orElse(null);
            if (verifiedPrompt == null) {
                logger.error("Prompt not found in database after save - ID: {}", savedPrompt.getPromptId());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Prompt not found in database after save"));
            }

            logger.info("Successfully verified prompt in database - ID: {}, Template: {}",
                    verifiedPrompt.getPromptId(), verifiedPrompt.getPromptTemplateName());

            // 如果是入院记录总结，更新相关alert_tasks状态
            if ("入院记录总结".equals(request.getPromptTemplateName())) {
                List<AlertTask> tasks = alertTaskRepository.findByPatientIdAndTaskTypeAndTaskStatus(
                        request.getPatientId(),
                        "入院记录总结",
                        AlertTask.TaskStatus.待处理);

                if (tasks.isEmpty()) {
                    logger.warn("No pending alert tasks found for patient: {}, taskType: 入院记录总结",
                            request.getPatientId());
                }

                tasks.forEach(task -> {
                    try {
                        task.setTaskStatus(AlertTask.TaskStatus.已完成);
                        task.setCompletedTime(LocalDateTime.now());
                        AlertTask savedTask = alertTaskRepository.save(task);

                        if (savedTask == null || savedTask.getTaskStatus() != AlertTask.TaskStatus.已完成) {
                            logger.error("Failed to update alert task status for patient: {}, taskId: {}",
                                    request.getPatientId(), task.getTaskId());
                        }
                    } catch (Exception e) {
                        logger.error("Error updating alert task for patient: {}, taskId: {}, error: {}",
                                request.getPatientId(), task.getTaskId(), e.getMessage(), e);
                    }
                });
            }

            return ResponseEntity.ok(Map.of(
                    "id", savedPrompt.getPromptId(),
                    "status", "SAVED",
                    "timestamp", savedPrompt.getSubmissionTime()));
        } catch (Exception e) {
            logger.error("Error saving prompt", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save prompt: " + e.getMessage()));
        }
    }

    @GetMapping("/prompts")
    public ResponseEntity<List<PromptListDTO>> getPrompts(
            @RequestParam String patientId,
            @RequestParam(required = false) String promptTemplateName) {

        List<Prompt> prompts;
        if (promptTemplateName != null && !promptTemplateName.isEmpty()) {
            prompts = promptRepository.findByPatientIdAndPromptTemplateName(patientId, promptTemplateName);
        } else {
            prompts = promptRepository.findByPatientId(patientId);
        }

        List<PromptListDTO> result = prompts.stream()
                .map(p -> new PromptListDTO(
                        p.getPromptId(),
                        p.getPatientId(),
                        p.getPromptTemplateName(),
                        p.getObjectiveContent(),
                        p.getDailyRecords(),
                        p.getPriority(),
                        p.getSubmissionTime(),
                        p.getStatusName()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * 根据 PromptId 获取单个 Prompt 的基础信息
     * 返回患者ID、模板名称、客观资料、日常记录、优先级、提交时间等字段
     *
     * @param promptId Prompt 主键ID
     * @return Prompt基础信息DTO，如果不存在则返回404
     */
    @GetMapping("/prompt/{promptId}")
    public ResponseEntity<PromptListDTO> getPromptById(@PathVariable Integer promptId) {
        Optional<Prompt> optionalPrompt = promptRepository.findById(promptId);
        if (optionalPrompt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Prompt p = optionalPrompt.get();
        PromptListDTO dto = new PromptListDTO(
                p.getPromptId(),
                p.getPatientId(),
                p.getPromptTemplateName(),
                p.getObjectiveContent(),
                p.getDailyRecords(),
                p.getPriority(),
                p.getSubmissionTime(),
                p.getStatusName()
        );

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/health/ai-status")
    public ResponseEntity<Map<String, Object>> checkAIStatus() {
        Map<String, Object> response = initializeResponse();
        Map<String, Object> modelsStatus = new HashMap<>();

        boolean overallHealthy = checkNetworkHealth(response);
        overallHealthy = checkAllModelsHealth(modelsStatus, overallHealthy);

        buildFinalResponse(response, modelsStatus, overallHealthy);
        logStatusCheckResult(response, overallHealthy);

        return ResponseEntity.ok(response);
    }

    /**
     * 初始化响应对象
     */
    private Map<String, Object> initializeResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * 检查网络健康状态
     */
    private boolean checkNetworkHealth(Map<String, Object> response) {
        String networkStatus = networkRecoveryService.getNetworkStatus();
        boolean networkHealthy = networkRecoveryService.isNetworkHealthy();
        response.put("network_status", networkStatus);
        response.put("network_healthy", networkHealthy);

        // 如果网络不健康，强制重置网络状态
        if (!networkHealthy) {
            logger.warn("Network unhealthy detected, forcing reset - status: {}", networkStatus);
            networkRecoveryService.forceReset();
            response.put("network_reset", true);
        }

        return networkHealthy;
    }

    /**
     * 检查所有AI模型的健康状态
     */
    private boolean checkAllModelsHealth(Map<String, Object> modelsStatus, boolean overallHealthy) {
        for (Map.Entry<String, AIModelConfig.ModelConfig> entry : aiModelConfig.getModels().entrySet()) {
            String modelName = entry.getKey();
            AIModelConfig.ModelConfig modelConfig = entry.getValue();

            Map<String, Object> modelStatus = checkSingleModelHealth(modelName, modelConfig);
            modelsStatus.put(modelName, modelStatus);

            if (!(Boolean) modelStatus.get("healthy")) {
                overallHealthy = false;
            }
        }
        return overallHealthy;
    }

    /**
     * 检查单个AI模型的健康状态
     */
    private Map<String, Object> checkSingleModelHealth(String modelName, AIModelConfig.ModelConfig modelConfig) {
        Map<String, Object> modelStatus = new HashMap<>();
        
        // 检查模型配置
        boolean hasKey = modelConfig.getKey() != null && !modelConfig.getKey().isEmpty();
        modelStatus.put("hasKey", hasKey);
        modelStatus.put("url", modelConfig.getUrl());
        
        // 执行健康检查
        boolean isHealthy = performModelHealthCheck(modelName, modelConfig, hasKey);
        modelStatus.put("healthy", isHealthy);
        
        return modelStatus;
    }

    /**
     * 执行模型健康检查
     */
    private boolean performModelHealthCheck(String modelName, AIModelConfig.ModelConfig modelConfig, boolean hasKey) {
        try {
            HttpHeaders headers = createModelHeaders(hasKey, modelConfig);
            HttpEntity<String> entity = new HttpEntity<>("{}", headers);
            
            ResponseEntity<String> pingResponse = restTemplate.exchange(
                modelConfig.getUrl(), HttpMethod.POST, entity, String.class);
            
            boolean isHealthy = pingResponse.getStatusCode().is2xxSuccessful();
            
            // 如果健康检查成功，重置网络失败计数
            if (isHealthy) {
                networkRecoveryService.resetFailureCount();
            }
            
            return isHealthy;
        } catch (Exception e) {
            logger.warn("Failed to ping AI model {}: {}", modelName, e.getMessage());
            handleModelHealthCheckFailure(e);
            return false;
        }
    }

    /**
     * 创建模型请求头
     */
    private HttpHeaders createModelHeaders(boolean hasKey, AIModelConfig.ModelConfig modelConfig) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        
        if (hasKey) {
            headers.set("Authorization", "Bearer " + modelConfig.getKey());
        }
        
        return headers;
    }

    /**
     * 处理模型健康检查失败
     */
    private void handleModelHealthCheckFailure(Exception e) {
        if (isRetryableNetworkException(e)) {
            logger.debug("Recording network failure for model health check");
        }
    }

    /**
     * 构建最终响应
     */
    private void buildFinalResponse(Map<String, Object> response, Map<String, Object> modelsStatus, boolean overallHealthy) {
        response.put("status", overallHealthy ? "UP" : "DOWN");
        response.put("overall_status", overallHealthy ? "healthy" : "degraded");
        response.put("models", modelsStatus);
        response.put("network_failure_count", networkRecoveryService.getFailureCount());
        response.put("network_failure_threshold", networkRecoveryService.getFailureThreshold());
    }

    /**
     * 记录状态检查结果
     */
    private void logStatusCheckResult(Map<String, Object> response, boolean overallHealthy) {
        boolean networkHealthy = (Boolean) response.get("network_healthy");
        logger.info("AI status check completed - overall: {}, network: {}, failures: {}/{}", 
                overallHealthy ? "healthy" : "degraded",
                networkHealthy ? "healthy" : "unhealthy",
                networkRecoveryService.getFailureCount(),
                networkRecoveryService.getFailureThreshold());
    }

    /**
     * 判断异常是否为可重试的网络异常
     * 
     * 该方法用于判断异常是否属于可重试的网络异常类型，包括：
     * - UnknownHostException: DNS解析失败
     * - ConnectException: 连接失败
     * - SocketTimeoutException: 套接字超时
     * - ResourceAccessException: Spring资源访问异常
     * - 5xx服务端错误
     * 
     * @param e 需要判断的异常
     * @return 如果是可重试的网络异常返回true，否则返回false
     */
    private boolean isRetryableNetworkException(Exception e) {
        if (e == null) {
            return false;
        }

        // 检查直接异常类型
        if (e instanceof java.net.UnknownHostException ||
            e instanceof java.net.ConnectException ||
            e instanceof java.net.SocketTimeoutException ||
            e instanceof org.springframework.web.client.ResourceAccessException) {
            return true;
        }

        // 检查异常链中的可重试异常
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof java.net.UnknownHostException ||
                cause instanceof java.net.ConnectException ||
                cause instanceof java.net.SocketTimeoutException) {
                return true;
            }
            
            // 检查5xx服务端错误
            if (is5xxServerError(cause)) {
                return true;
            }
            
            cause = cause.getCause();
        }

        return false;
    }

    /**
     * 判断异常是否表示5xx服务端错误
     * 
     * @param throwable 需要判断的异常
     * @return 如果是5xx服务端错误返回true，否则返回false
     */
    private boolean is5xxServerError(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null) {
            return false;
        }
        
        String message = throwable.getMessage().toLowerCase();
        
        // 检查常见的5xx错误模式
        return message.contains("500") || 
               message.contains("502") || 
               message.contains("503") || 
               message.contains("504") ||
               message.contains("internal server error") ||
               message.contains("service unavailable") ||
               message.contains("bad gateway") ||
               message.contains("gateway timeout");
    }

    /**
     * 根据患者ID获取综合信息并组合成字符串返回
     *
     * 该接口用于获取指定患者的综合信息，包括一般信息、诊断信息、病情小结、病历记录以及最近2天的医嘱和检查检验结果
     * 并将这些信息组合成一个格式化的字符串返回
     *
     * @param patientId 患者ID，必需参数
     * @return ResponseEntity<String> 包含患者综合信息的格式化字符串
     *
     *         返回格式示例:
     *         ### 一般信息:
     *         性别：男，年龄：45岁，入院时间：2023年05月10日 09:30，住院时间：5天。
     *         ------
     *         ### 诊断信息:
     *         高血压, 糖尿病
     *         ------
     *         ### 病情小结:
     *         患者目前病情稳定...
     *         ------
     *         ### 病历记录:
     *         2023年05月14日 10:00
     *         患者今日血压控制良好...
     *
     *         ------
     *         ### 最近2天长期医嘱:
     *         2023年05月14日 08:00 口服降压药 1片 每日一次 口服
     *         ------
     *         ### 最近2天临时医嘱:
     *         2023年05月14日 14:00 测量血压 q4h
     *         ------
     *         ### 最近2天化验结果:
     *         2023年05月14日10:30 血常规
     *         白细胞计数 7.5 ×10^9/L
     *         ------
     *         ### 最近2天检查结果:
     *         2023年05月14日09:00 心电图
     *         窦性心律正常
     *         ------
     *
     *         错误情况:
     *         如果发生错误，将返回500状态码和错误信息字符串
     */
    @GetMapping("/patient-comprehensive-info")
    public ResponseEntity<Map<String, Object>> getPatientComprehensiveInfo(@RequestParam String patientId) {
        try {
            StringBuilder result = new StringBuilder();

            // 1. 获取一般信息
            result.append("### 一般信息:\n");
            try {
                String basicInfo = getPatientBasicInfo(patientId);
                result.append(basicInfo != null ? basicInfo : "无基本信息数据");
            } catch (Exception e) {
                logger.error("获取一般信息失败", e);
                result.append("无基本信息数据");
            }
            result.append("\n------\n");

            // 2. 获取诊断信息
            result.append("### 诊断信息:\n");
            try {
                List<Diagnosis> diagnoses = diagnosisRepository.findByPatientId(patientId);
                if (diagnoses != null && !diagnoses.isEmpty()) {
                    String diagnosisText = diagnoses.stream()
                            .map(Diagnosis::getDiagnosisText)
                            .collect(Collectors.joining(", "));
                    result.append(diagnosisText);
                } else {
                    result.append("无诊断信息数据");
                }
            } catch (Exception e) {
                logger.error("获取诊断信息失败", e);
                result.append("无诊断信息数据");
            }
            result.append("\n------\n");

            // 3. 获取病情小结
            result.append("### 病情小结:\n");
            try {
                List<PatientPromptResultDTO> summaryResults = promptResultRepository
                        .findMedicalSummaryByPatientId(patientId);
                if (summaryResults != null && !summaryResults.isEmpty()) {
                    result.append(summaryResults.get(0).getOriginalResultContent());
                } else {
                    result.append("无病情小结数据");
                }
            } catch (Exception e) {
                logger.error("获取病情小结失败", e);
                result.append("无病情小结数据");
            }
            result.append("\n------\n");

            // 4. 获取病历记录
            result.append("### 病历记录:\n");
            try {
                List<MedicalRecord> records = medicalRecordRepository
                        .findByPatientIdAndDeletedOrderByRecordTimeDesc(patientId, 0);
                if (records != null && !records.isEmpty()) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
                    for (MedicalRecord record : records) {
                        result.append(dateFormat.format(record.getRecordTime()))
                                .append("\n")
                                .append(record.getMedicalContent())
                                .append("\n\n");
                    }
                } else {
                    result.append("无病历记录数据");
                }
            } catch (Exception e) {
                logger.error("获取病历记录失败", e);
                result.append("无病历记录数据");
            }
            result.append("\n------\n");

            // 5. 获取最近2天的长期医嘱
            result.append("### 最近2天长期医嘱:\n");
            try {
                List<LongTermOrder> longTermOrders = getRecentLongTermOrders(patientId, 2);
                if (longTermOrders != null && !longTermOrders.isEmpty()) {
                    List<String> formattedOrders = orderFormatService.formatLongTermOrders(patientId);
                    // 过滤出最近2天的医嘱
                    List<String> recentOrders = formattedOrders.stream()
                            .filter(order -> isRecentOrder(order, 2))
                            .collect(Collectors.toList());
                    if (!recentOrders.isEmpty()) {
                        result.append(String.join("\n", recentOrders));
                    } else {
                        result.append("最近2天无长期医嘱数据");
                    }
                } else {
                    result.append("最近2天无长期医嘱数据");
                }
            } catch (Exception e) {
                logger.error("获取最近2天长期医嘱失败", e);
                result.append("最近2天无长期医嘱数据");
            }
            result.append("\n------\n");

            // 6. 获取最近2天的临时医嘱
            result.append("### 最近2天临时医嘱:\n");
            try {
                List<LongTermOrder> temporaryOrders = getRecentTemporaryOrders(patientId, 2);
                if (temporaryOrders != null && !temporaryOrders.isEmpty()) {
                    // 格式化临时医嘱
                    String formattedOrders = formatTemporaryOrders(temporaryOrders);
                    if (formattedOrders != null && !formattedOrders.isEmpty()) {
                        result.append(formattedOrders);
                    } else {
                        result.append("最近2天无临时医嘱数据");
                    }
                } else {
                    result.append("最近2天无临时医嘱数据");
                }
            } catch (Exception e) {
                logger.error("获取最近2天临时医嘱失败", e);
                result.append("最近2天无临时医嘱数据");
            }
            result.append("\n------\n");

            // 7. 获取最近2天的化验结果
            result.append("### 最近2天化验结果:\n");
            try {
                List<LabResult> labResults = getRecentLabResults(patientId, 2);
                if (labResults != null && !labResults.isEmpty()) {
                    String formattedResults = formatLabResults(labResults);
                    if (formattedResults != null && !formattedResults.isEmpty()) {
                        result.append(formattedResults);
                    } else {
                        result.append("最近2天无化验结果数据");
                    }
                } else {
                    result.append("最近2天无化验结果数据");
                }
            } catch (Exception e) {
                logger.error("获取最近2天化验结果失败", e);
                result.append("最近2天无化验结果数据");
            }
            result.append("\n------\n");

            // 8. 获取最近2天的检查结果
            result.append("### 最近2天检查结果:\n");
            try {
                List<ExaminationResult> examinationResults = getRecentExaminationResults(patientId, 2);
                if (examinationResults != null && !examinationResults.isEmpty()) {
                    String formattedResults = examinationResultService.getFormattedResultsByPatientId(patientId);
                    // 过滤出最近2天的检查结果
                    String recentResults = filterRecentExaminationResults(formattedResults, 2);
                    if (recentResults != null && !recentResults.isEmpty()) {
                        result.append(recentResults);
                    } else {
                        result.append("最近2天无检查结果数据");
                    }
                } else {
                    result.append("最近2天无检查结果数据");
                }
            } catch (Exception e) {
                logger.error("获取最近2天检查结果失败", e);
                result.append("最近2天无检查结果数据");
            }
            result.append("\n------\n");

            return ResponseEntity.ok(AIContentResponseWrapper.wrapWithDisclaimer(result.toString()));
        } catch (Exception e) {
            logger.error("获取患者综合信息失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取患者综合信息失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 获取患者基本信息
     * 
     * @param patientId 患者ID
     * @return 基本信息字符串
     */
    private String getPatientBasicInfo(String patientId) {
        Optional<Patient> patientOpt = patientRepository.findById(patientId);
        if (!patientOpt.isPresent()) {
            return "未找到患者信息";
        }

        Patient patient = patientOpt.get();

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

        // 转换性别为中文
        String genderInChinese;
        switch (patient.getGender().toUpperCase()) {
            case "M":
                genderInChinese = "男";
                break;
            case "F":
                genderInChinese = "女";
                break;
            default:
                genderInChinese = "未确定";
        }

        return String.format("性别：%s，年龄：%d岁，入院时间：%s，住院时间：%d天。",
                genderInChinese, age, admissionTime, days);
    }

    /**
     * 获取最近几天的长期医嘱
     * 
     * @param patientId 患者ID
     * @param days      天数
     * @return 最近几天的长期医嘱列表
     */
    private List<LongTermOrder> getRecentLongTermOrders(String patientId, int days) {
        List<LongTermOrder> orders = longTermOrderRepository.findByPatientIdAndRepeatIndicatorOrderByOrderDateAsc(patientId, 1);
        LocalDate cutoffDate = LocalDate.now().minusDays(days);

        return orders.stream()
                .filter(order -> order.getOrderDate() != null)
                .filter(order -> {
                    LocalDate orderDate = order.getOrderDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    return !orderDate.isBefore(cutoffDate);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取最近几天的临时医嘱
     * 
     * @param patientId 患者ID
     * @param days      天数
     * @return 最近几天的临时医嘱列表
     */
    private List<LongTermOrder> getRecentTemporaryOrders(String patientId, int days) {
        List<LongTermOrder> orders = longTermOrderRepository.findByPatientIdAndRepeatIndicatorOrderByOrderDateAsc(patientId, 0);
        LocalDate cutoffDate = LocalDate.now().minusDays(days);

        return orders.stream()
                .filter(order -> order.getOrderDate() != null)
                .filter(order -> {
                    LocalDate orderDate = order.getOrderDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    return !orderDate.isBefore(cutoffDate);
                })
                .collect(Collectors.toList());
    }

    /**
     * 格式化临时医嘱
     * 
     * @param orders 临时医嘱列表
     * @return 格式化后的字符串
     */
    private String formatTemporaryOrders(List<LongTermOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");

        for (LongTermOrder order : orders) {
            sb.append(sdf.format(order.getOrderDate()))
                    .append(" ")
                    .append(order.getOrderName() != null ? order.getOrderName() : "")
                    .append(" ")
                    .append(order.getDosage() != null ? order.getDosage() : "")
                    .append(order.getUnit() != null ? order.getUnit() : "")
                    .append(" ")
                    .append(order.getFrequency() != null ? order.getFrequency() : "")
                    .append(" ")
                    .append(order.getRoute() != null ? order.getRoute() : "")
                    .append("\n");
        }

        return sb.toString().trim();
    }

    /**
     * 获取最近几天的化验结果
     * 
     * @param patientId 患者ID
     * @param days      天数
     * @return 最近几天的化验结果列表
     */
    private List<LabResult> getRecentLabResults(String patientId, int days) {
        List<LabResult> results = labResultRepository.findByPatientId(patientId);
        LocalDate cutoffDate = LocalDate.now().minusDays(days);

        return results.stream()
                .filter(result -> result.getLabReportTime() != null)
                .filter(result -> {
                    try {
                        // labReportTime现在是Timestamp类型，直接转换为LocalDate
                        java.sql.Timestamp reportTime = result.getLabReportTime();
                        LocalDate reportLocalDate = reportTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        return !reportLocalDate.isBefore(cutoffDate);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 格式化化验结果
     * 
     * @param labResults 化验结果列表
     * @return 格式化后的字符串
     */
    private String formatLabResults(List<LabResult> labResults) {
        if (labResults == null || labResults.isEmpty()) {
            return "";
        }

        // 先按时间升序排序，再按异常标志排序（H、L、N）
        labResults.sort((a, b) -> {
            try {
                if (a.getLabReportTime() == null) return 1;
                if (b.getLabReportTime() == null) return -1;
                int timeCompare = a.getLabReportTime().compareTo(b.getLabReportTime());
                if (timeCompare != 0) {
                    return timeCompare;
                }
                int abnormalOrderA = getAbnormalOrder(a.getAbnormalIndicator());
                int abnormalOrderB = getAbnormalOrder(b.getAbnormalIndicator());
                return Integer.compare(abnormalOrderA, abnormalOrderB);
            } catch (Exception e) {
                return 0;
            }
        });

        // 按时间和类型分组
        Map<String, Map<String, List<LabResult>>> groupedResults = new LinkedHashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日HH:mm");

        for (LabResult labResult : labResults) {
            String issueTime = "未知时间";
            try {
                if (labResult.getLabReportTime() != null) {
                    // labReportTime现在是Timestamp类型，直接格式化
                    issueTime = sdf.format(labResult.getLabReportTime());
                }
            } catch (Exception e) {
                System.out.println("日期格式化失败: " + labResult.getLabReportTime());
            }

            String labType = labResult.getLabType() != null ? labResult.getLabType() : "未知类型";

            groupedResults
                    .computeIfAbsent(issueTime, k -> new LinkedHashMap<>())
                    .computeIfAbsent(labType, k -> new ArrayList<>())
                    .add(labResult);
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, List<LabResult>>> timeEntry : groupedResults.entrySet()) {
            String issueTime = timeEntry.getKey();

            for (Map.Entry<String, List<LabResult>> typeEntry : timeEntry.getValue().entrySet()) {
                String labType = typeEntry.getKey();
                sb.append(issueTime).append(" ").append(labType).append("\n");

                for (LabResult labResult : typeEntry.getValue()) {
                    String labName = labResult.getLabName() != null ? labResult.getLabName() : "未知项目";
                    String resultValue = labResult.getLabResult() != null ? labResult.getLabResult() : "";
                    String unit = labResult.getUnit() != null ? labResult.getUnit() : "";
                    String abnormalIndicator = labResult.getAbnormalIndicator() != null
                            ? labResult.getAbnormalIndicator()
                            : "";

                    sb.append(labName).append(" ")
                            .append(resultValue).append(" ")
                            .append(unit);

                    if ("h".equalsIgnoreCase(abnormalIndicator)) {
                        sb.append(" (升高)");
                    } else if ("l".equalsIgnoreCase(abnormalIndicator)) {
                        sb.append(" (降低)");
                    }

                    sb.append("\n");
                }

                sb.append("\n");
            }
        }

        return sb.toString().trim();
    }

    /**
     * 异常标志排序权重：H(0) < L(1) < N(2) < 其它/空(3)
     * 
     * @param abnormalIndicator 异常标志（H=高于正常值，L=低于正常值，N=正常）
     * @return 排序权重值
     */
    private int getAbnormalOrder(String abnormalIndicator) {
        if (abnormalIndicator == null) {
            return 3;
        }
        String flag = abnormalIndicator.trim();
        if ("H".equalsIgnoreCase(flag)) {
            return 0;
        }
        if ("L".equalsIgnoreCase(flag)) {
            return 1;
        }
        if ("N".equalsIgnoreCase(flag)) {
            return 2;
        }
        return 3;
    }

    /**
     * 获取最近几天的检查结果
     * 
     * @param patientId 患者ID
     * @param days      天数
     * @return 最近几天的检查结果列表
     */
    private List<ExaminationResult> getRecentExaminationResults(String patientId, int days) {
        List<ExaminationResult> results = examinationResultRepository.findByPatientId(patientId);
        LocalDate cutoffDate = LocalDate.now().minusDays(days);

        return results.stream()
                .filter(result -> result.getCheckReportTime() != null)
                .filter(result -> {
                    try {
                        // Timestamp 可直接转换为 LocalDate
                        LocalDate reportLocalDate = result.getCheckReportTime().toLocalDateTime().toLocalDate();
                        return !reportLocalDate.isBefore(cutoffDate);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 过滤最近几天的检查结果
     * 
     * @param formattedResults 格式化的检查结果
     * @param days             天数
     * @return 过滤后的检查结果
     */
    private String filterRecentExaminationResults(String formattedResults, int days) {
        if (formattedResults == null || formattedResults.isEmpty()) {
            return "";
        }

        // 简单实现：直接返回格式化的结果
        // 在实际应用中，可能需要根据日期进一步过滤
        return formattedResults;
    }

    /**
     * 判断医嘱是否为最近几天的
     * 
     * @param order 医嘱字符串
     * @param days  天数
     * @return 是否为最近几天的医嘱
     */
    private boolean isRecentOrder(String order, int days) {
        // 简单实现：直接返回true
        // 在实际应用中，可能需要解析医嘱字符串中的日期信息进行判断
        return true;
    }

    /**
     * 检查AI返回的内容是否包含错误指示词
     * 
     * 该方法用于检测LLM返回的内容中是否包含错误提示信息，以避免将错误信息保存到数据库中。
     * 使用精确的短语匹配策略，只检测明显的技术/系统错误，不会误判医学专业术语。
     * 
     * <p>检测策略：</p>
     * <ul>
     *   <li>使用精确短语匹配，而非单独单词</li>
     *   <li>大小写不敏感匹配</li>
     *   <li>支持中英文错误提示</li>
     *   <li>检测到错误时记录警告日志</li>
     * </ul>
     * 
     * <p>检测的系统错误关键词包括：</p>
     * <ul>
     *   <li>API相关：api error/api错误, api exception/api异常</li>
     *   <li>连接相关：connection error/连接错误, connection failed/连接失败, network error/网络错误</li>
     *   <li>系统相关：system error/系统错误, internal error/内部错误</li>
     *   <li>服务相关：service unavailable/服务不可用, service error/服务错误</li>
     *   <li>认证相关：authentication failed/认证失败, authorization failed/授权失败</li>
     *   <li>其他：database error/数据库错误, timeout error/超时错误, rate limit exceeded/超出速率限制</li>
     * </ul>
     * 
     * <p><b>不会误判的医学术语示例：</b></p>
     * <ul>
     *   <li>"肾功能异常" - 包含"异常"但不包含"系统异常"</li>
     *   <li>"手术失败" - 包含"失败"但不包含"连接失败"</li>
     *   <li>"检查发现异常" - 包含"异常"但不包含"API异常"</li>
     *   <li>"诊断错误需要纠正" - 包含"错误"但不包含"系统错误"</li>
     * </ul>
     * 
     * @param content 待检查的AI返回内容字符串
     * @return 如果内容包含系统错误指示词则返回true，否则返回false
     * 
     * @since 2026-02-08 优化为精确短语匹配，避免误判医学术语
     */
    private boolean containsErrorIndicators(String content) {
        // 检查输入内容是否为空
        if (content == null || content.isEmpty()) {
            return false;
        }

        // 定义系统/技术错误指示词，使用更精确的短语避免误判医学术语
        // 只检测明显的技术错误，不检测医学专业术语（如"功能异常"、"手术失败"等）
        String[] errorIndicators = {
                "api error", "api错误",
                "api exception", "api异常",
                "connection error", "连接错误",
                "connection failed", "连接失败",
                "network error", "网络错误",
                "network timeout", "网络超时",
                "timeout error", "超时错误",
                "system error", "系统错误",
                "internal error", "内部错误",
                "internal server error", "内部服务器错误",
                "service unavailable", "服务不可用",
                "service error", "服务错误",
                "rate limit exceeded", "超出速率限制",
                "authentication failed", "认证失败",
                "authorization failed", "授权失败",
                "database error", "数据库错误",
                "无法连接", "连接超时",
                "请求失败", "响应超时",
                "服务异常", "系统异常"
        };

        // 转换为小写以便比较，提高匹配准确性
        String lowerContent = content.toLowerCase();

        // 遍历所有错误指示词，检查内容是否包含任何系统错误指示词
        for (String indicator : errorIndicators) {
            if (lowerContent.contains(indicator.toLowerCase())) {
                logger.warn("Detected system error indicator in content: '{}'", indicator);
                return true;
            }
        }

        // 如果未找到任何系统错误指示词，返回false
        return false;
    }

    /**
     * 接收执行服务器返回的AI处理结果
     * 
     * 该接口用于接收执行服务器(8082)异步处理完成后回调的结果，是主应用与执行服务器之间
     * 异步处理流程的关键环节。主要功能包括：
     * 
     * 处理流程：
     * 1. 参数验证：验证dataId字段是否为空，确保请求的完整性
     * 2. 日志记录：详细记录接收到的结果信息，包括状态、内容长度等
     * 3. 状态处理：根据不同的处理状态（SUCCESS/ERROR/未知）进行相应处理
     * 4. 响应返回：返回标准化的JSON响应格式
     * 
     * 安全特性：
     * - 参数验证：使用Jakarta Validation确保必填字段不为空
     * - 异常处理：完整的异常捕获机制，防止服务器崩溃
     * - 日志记录：详细的日志信息，便于问题追踪和调试
     * 
     * 性能考虑：
     * - 轻量级处理：不涉及复杂的数据库操作，响应快速
     * - 异步友好：适合执行服务器异步回调场景
     * 
     * @param request ReceiveResultDTO对象，包含执行服务器返回的结果信息
     *                - dataId: 数据ID（必填），用于唯一标识处理请求，不能为空
     *                - content: 处理结果内容（可选），成功时的具体处理结果
     *                - title: 结果标题（可选），结果的简短描述
     *                - timestamp: 时间戳（可选），结果生成时间
     *                - promptId: Prompt ID（可选），关联的Prompt模板标识
     *                - patientId: 患者ID（可选），结果关联的患者标识
     *                - originalContent: 原始内容（可选），处理前的原始数据
     *                - lastModifiedBy: 最后修改人ID（可选）
     *                - isRead: 是否已读标记（可选），0-未读，1-已读
     *                - status: 处理状态（可选），SUCCESS-成功，ERROR-失败
     *                - errorMessage: 错误信息（可选），处理失败时的具体错误描述
     * @return ResponseEntity<Map<String, Object>> 标准化的响应实体，包含：
     *         - status: 接口调用状态，"SUCCESS"或"ERROR"
     *         - message: 详细的响应消息
     *         - dataId: 接收到的数据ID
     *         - timestamp: 响应时间戳
     *         - 其他可选字段：errorMessage（错误时），receivedStatus（未知状态时）
     * 
     * @throws 无显式抛出异常，所有异常均被捕获并返回错误响应
     * 
     * @HTTP 200 请求成功处理，返回标准化响应
     * @HTTP 400 参数验证失败，dataId为空或其他验证错误
     * @HTTP 500 服务器内部错误，处理过程中发生异常
     * 
     * @example 成功响应示例：
     * {
     *   "status": "SUCCESS",
     *   "message": "结果接收成功",
     *   "dataId": "test-data-001",
     *   "timestamp": 1747283700000
     * }
     * 
     * @example 错误响应示例：
     * {
     *   "status": "ERROR",
     *   "message": "dataId不能为空",
     *   "timestamp": 1747283700000
     * }
     */
    @PostMapping("/receive-result")
    public ResponseEntity<Map<String, Object>> receiveAIResult(
            @RequestBody ReceiveResultDTO request) {
        try {
            logger.info("接收到AI处理结果 - dataId: {}, status: {}, contentLength: {}",
                    request.getDataId(),
                    request.getStatus(),
                    request.getContent() != null ? request.getContent().length() : 0);

            // 验证必填字段
            if (request.getDataId() == null || request.getDataId().trim().isEmpty()) {
                logger.error("接收结果失败 - dataId不能为空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "status", "ERROR",
                                "message", "dataId不能为空",
                                "timestamp", System.currentTimeMillis()));
            }

            // 记录接收到的结果详细信息
            logger.info("结果详情 - dataId: {}, status: {}, hasContent: {}, hasError: {}",
                    request.getDataId(),
                    request.getStatus(),
                    request.getContent() != null && !request.getContent().isEmpty(),
                    request.getErrorMessage() != null && !request.getErrorMessage().isEmpty());

            // 根据状态处理结果
            if ("SUCCESS".equalsIgnoreCase(request.getStatus())) {
                // 成功处理的结果
                logger.info("成功接收到AI处理结果 - dataId: {}", request.getDataId());
                
                // 这里可以添加结果保存逻辑，比如更新数据库中的相关记录
                // 例如：根据dataId找到对应的Prompt记录并更新结果内容
                
                return ResponseEntity.ok(Map.of(
                        "status", "SUCCESS",
                        "message", "结果接收成功",
                        "dataId", request.getDataId(),
                        "timestamp", System.currentTimeMillis()));
            } else if ("ERROR".equalsIgnoreCase(request.getStatus())) {
                // 处理失败的结果
                logger.warn("接收到处理失败的结果 - dataId: {}, error: {}",
                        request.getDataId(), request.getErrorMessage());
                
                // 这里可以添加失败处理逻辑，比如记录错误日志或更新任务状态为失败
                
                return ResponseEntity.ok(Map.of(
                        "status", "SUCCESS", // 接口调用本身成功，只是结果状态是ERROR
                        "message", "失败结果接收成功",
                        "dataId", request.getDataId(),
                        "errorMessage", request.getErrorMessage(),
                        "timestamp", System.currentTimeMillis()));
            } else {
                // 未知状态
                logger.warn("接收到未知状态的结果 - dataId: {}, status: {}",
                        request.getDataId(), request.getStatus());
                
                return ResponseEntity.ok(Map.of(
                        "status", "SUCCESS",
                        "message", "结果接收成功（未知状态）",
                        "dataId", request.getDataId(),
                        "receivedStatus", request.getStatus(),
                        "timestamp", System.currentTimeMillis()));
            }

        } catch (Exception e) {
            logger.error("接收AI结果时发生错误 - dataId: {}", request != null ? request.getDataId() : "null", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "结果接收失败: " + e.getMessage(),
                            "dataId", request != null ? request.getDataId() : "unknown",
                            "timestamp", System.currentTimeMillis()));
        }
    }

}
