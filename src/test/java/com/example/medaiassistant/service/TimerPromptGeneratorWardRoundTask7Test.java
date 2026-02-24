package com.example.medaiassistant.service;

import com.example.medaiassistant.config.ApiProperties;
import com.example.medaiassistant.config.SchedulingProperties;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 任务7：与 generateAndSavePromptForNoonWardRound 的集成与回归测试
 * 
 * 测试策略：
 * 1. 端到端测试 - 测试generateAndSavePromptForNoonWardRound的最终输出结果
 * 2. 回归测试 - 验证原有准入规则不受影响
 * 3. 集成测试 - 验证方法在实际场景下的行为
 * 
 * 关键改动：
 * - generateAndSavePromptForNoonWardRound应该调用buildWardRoundObjectiveContent
 *   而不是callPatientDataApi来构建objectiveContent
 * 
 * @author TDD
 * @version 1.0 - 任务7新增
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("任务7 - generateAndSavePromptForNoonWardRound集成与回归测试")
class TimerPromptGeneratorWardRoundTask7Test {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private AlertRuleService alertRuleService;

    @Mock
    private PatientStatusUpdateService patientStatusUpdateService;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PromptRepository promptRepository;

    @Mock
    private PromptResultRepository promptResultRepository;

    @Mock
    private ServerConfigService serverConfigService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ApiProperties apiProperties;

    @Mock
    private EmrRecordRepository emrRecordRepository;

    @Mock
    private EmrContentRepository emrContentRepository;

    @Mock
    private PromptGenerationLogService promptGenerationLogService;

    @Mock
    private PromptsTaskUpdater promptsTaskUpdater;
    
    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    
    @Mock
    private LabResultRepository labResultRepository;
    
    @Mock
    private ExaminationResultRepository examinationResultRepository;
    
    @Mock
    private LongTermOrderRepository longTermOrderRepository;

    private SchedulingProperties schedulingProperties;
    private TimerPromptGenerator timerPromptGenerator;

    @BeforeEach
    void setUp() {
        schedulingProperties = new SchedulingProperties();
        timerPromptGenerator = new TimerPromptGenerator(
            taskScheduler,
            alertRuleService,
            patientStatusUpdateService,
            patientRepository,
            promptRepository,
            promptResultRepository,
            medicalRecordRepository,
            labResultRepository,
            examinationResultRepository,
            longTermOrderRepository,
            serverConfigService,
            restTemplate,
            apiProperties,
            schedulingProperties,
            emrRecordRepository,
            emrContentRepository,
            null, // surgeryRepository - not needed for this test
            promptGenerationLogService,
            promptsTaskUpdater
        );
    }

    @Test
    @DisplayName("测试1：generateAndSavePromptForNoonWardRound应使用新的查房记录结构")
    void testGenerateAndSavePromptForNoonWardRound_UsesNewWardRoundStructure() {
        // Given - 准备测试数据
        String patientId = "TEST_PATIENT_001";
        String templateName = "中午查房记录";
        String promptType = "ward_round";
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 12, 0);
        LocalDateTime todayStart = currentTime.toLocalDate().atStartOfDay();
        
        // Mock阅读状态门槛检查通过（至少1条已读）
        when(promptResultRepository.countReadDiagnosisAnalysisSince(patientId, todayStart))
            .thenReturn(1);
        
        // Mock当天未生成（使用正确的repository方法）
        when(promptRepository.countByPatientIdAndPromptTemplateNameAndSubmissionTimeAfter(
            eq(patientId), 
            eq(templateName), 
            any(LocalDateTime.class)
        )).thenReturn(0L);
        
        // Mock时间间隔检查通过（无历史记录）
        when(promptRepository.findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(
            patientId, 
            templateName
        )).thenReturn(Optional.empty());
        
        // Mock Patient数据
        Patient patient = new Patient();
        patient.setPatientId(patientId);
        patient.setGender("1");
        patient.setDateOfBirth(new Date());
        patient.setAdmissionTime(new Date());
        patient.setImportantInformation("高血压病，糖尿病");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        
        // Mock空的历史数据
        when(medicalRecordRepository.findByPatientIdAndDeletedOrderByRecordTimeDesc(anyString(), anyInt()))
            .thenReturn(new ArrayList<>());
        when(labResultRepository.findByPatientId(anyString()))
            .thenReturn(new ArrayList<>());
        when(examinationResultRepository.findByPatientId(anyString()))
            .thenReturn(new ArrayList<>());
        when(longTermOrderRepository.findByPatientIdAndRepeatIndicatorOrderByOrderDateAsc(anyString(), anyInt()))
            .thenReturn(new ArrayList<>());
        
        // Mock promptRepository.saveAndFlush方法，使其返回保存的对象
        when(promptRepository.saveAndFlush(any(Prompt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Mock ApiProperties配置，使得callPromptApi能正常工作
        ApiProperties.Base baseConfig = new ApiProperties.Base();
        baseConfig.setUrl("http://test-api");
        ApiProperties.Retry retryConfig = new ApiProperties.Retry();
        retryConfig.setMaxAttempts(3);
        baseConfig.setRetry(retryConfig);
        when(apiProperties.getBase()).thenReturn(baseConfig);
        
        // Mock restTemplate调用（用于callPromptApi）
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn("模板内容");
        
        // When - 调用方法
        Prompt result = timerPromptGenerator.generateAndSavePromptForNoonWardRound(
            patientId, 
            templateName, 
            promptType, 
            currentTime
        );
        
        // Then - 验证结果
        assertNotNull(result, "应成功生成Prompt");
        
        // 验证objectiveContent包含新的查房记录结构的7个块标题
        String objectiveContent = result.getObjectiveContent();
        assertNotNull(objectiveContent, "objectiveContent不应为null");
        
        // 验证7个标准块标题都存在
        assertTrue(objectiveContent.contains("【病人基本信息】"), 
            "应包含病人基本信息块标题");
        assertTrue(objectiveContent.contains("【上次查房记录摘要】"), 
            "应包含上次查房记录摘要块标题");
        assertTrue(objectiveContent.contains("【本次病情记录（含主诉与查体原文）】"), 
            "应包含本次病情记录块标题");
        assertTrue(objectiveContent.contains("【自上次查房以来的化验结果】"), 
            "应包含化验结果块标题");
        assertTrue(objectiveContent.contains("【自上次查房以来的检查结果】"), 
            "应包含检查结果块标题");
        assertTrue(objectiveContent.contains("【目前诊断】"), 
            "应包含目前诊断块标题");
        assertTrue(objectiveContent.contains("【最近医嘱和治疗措施】"), 
            "应包含医嘱和治疗措施块标题");
        
        // 验证promptRepository的saveAndFlush被调用
        verify(promptRepository, times(1)).saveAndFlush(any(Prompt.class));
        
        // 捕获保存的Prompt对象，验证其他字段
        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(promptRepository).saveAndFlush(promptCaptor.capture());
        Prompt savedPrompt = promptCaptor.getValue();
        
        assertEquals(patientId, savedPrompt.getPatientId(), "patientId应正确");
        assertEquals(templateName, savedPrompt.getPromptTemplateName(), "模板名称应正确");
        assertEquals("待处理", savedPrompt.getStatusName(), "状态应为待处理");
        assertEquals("System-NoonWardRound", savedPrompt.getGeneratedBy(), "生成来源应正确");
    }

    @Test
    @DisplayName("测试2：原有准入规则 - 阅读门槛未满足时应跳过")
    void testGenerateAndSavePromptForNoonWardRound_StillHonorsReadCountCheck() {
        // Given - 准备测试数据
        String patientId = "TEST_PATIENT_002";
        String templateName = "中午查房记录";
        String promptType = "ward_round";
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 12, 0);
        LocalDateTime todayStart = currentTime.toLocalDate().atStartOfDay();
        
        // Mock阅读状态门槛未满足（0条已读）
        when(promptResultRepository.countReadDiagnosisAnalysisSince(patientId, todayStart))
            .thenReturn(0);
        
        // When - 调用方法
        Prompt result = timerPromptGenerator.generateAndSavePromptForNoonWardRound(
            patientId, 
            templateName, 
            promptType, 
            currentTime
        );
        
        // Then - 验证结果
        assertNull(result, "阅读门槛未满足时应返回null");
        
        // 验证promptRepository的saveAndFlush未被调用
        verify(promptRepository, never()).saveAndFlush(any(Prompt.class));
        
        // 验证patientRepository的findById未被调用（因为在准入检查阶段就被拦截了）
        verify(patientRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("测试3：原有准入规则 - 当天已生成时应跳过")
    void testGenerateAndSavePromptForNoonWardRound_StillHonorsFrequencyCheck() {
        // Given - 准备测试数据
        String patientId = "TEST_PATIENT_003";
        String templateName = "中午查房记录";
        String promptType = "ward_round";
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 12, 0);
        LocalDateTime todayStart = currentTime.toLocalDate().atStartOfDay();
        
        // Mock阅读状态门槛检查通过
        when(promptResultRepository.countReadDiagnosisAnalysisSince(patientId, todayStart))
            .thenReturn(1);
        
        // Mock当天已生成（返回count > 0）
        when(promptRepository.countByPatientIdAndPromptTemplateNameAndSubmissionTimeAfter(
            eq(patientId), 
            eq(templateName), 
            any(LocalDateTime.class)
        )).thenReturn(1L);
        
        // When - 调用方法
        Prompt result = timerPromptGenerator.generateAndSavePromptForNoonWardRound(
            patientId, 
            templateName, 
            promptType, 
            currentTime
        );
        
        // Then - 验证结果
        assertNull(result, "当天已生成时应返回null");
        
        // 验证promptRepository的saveAndFlush未被调用
        verify(promptRepository, never()).saveAndFlush(any(Prompt.class));
        
        // 验证patientRepository的findById未被调用
        verify(patientRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("测试4：首次查房应生成包含默认占位符的查房记录")
    void testGenerateAndSavePromptForNoonWardRound_FirstWardRound_BuildsFromFallbackRanges() {
        // Given - 准备测试数据
        String patientId = "TEST_PATIENT_004";
        String templateName = "中午查房记录";
        String promptType = "ward_round";
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 12, 0);
        LocalDateTime todayStart = currentTime.toLocalDate().atStartOfDay();
        
        // Mock阅读状态门槛检查通过
        when(promptResultRepository.countReadDiagnosisAnalysisSince(patientId, todayStart))
            .thenReturn(1);
        
        // Mock当天未生成
        when(promptRepository.countByPatientIdAndPromptTemplateNameAndSubmissionTimeAfter(
            eq(patientId), 
            eq(templateName), 
            any(LocalDateTime.class)
        )).thenReturn(0L);
        
        // Mock无历史查房记录（首次查房）
        when(promptRepository.findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(
            patientId, 
            templateName
        )).thenReturn(Optional.empty());
        
        // Mock Patient数据
        Patient patient = new Patient();
        patient.setPatientId(patientId);
        patient.setGender("1");
        patient.setDateOfBirth(new Date());
        patient.setAdmissionTime(new Date());
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        
        // Mock空的历史数据（首次查房，无数据）
        when(medicalRecordRepository.findByPatientIdAndDeletedOrderByRecordTimeDesc(anyString(), anyInt()))
            .thenReturn(new ArrayList<>());
        when(labResultRepository.findByPatientId(anyString()))
            .thenReturn(new ArrayList<>());
        when(examinationResultRepository.findByPatientId(anyString()))
            .thenReturn(new ArrayList<>());
        when(longTermOrderRepository.findByPatientIdAndRepeatIndicatorOrderByOrderDateAsc(anyString(), anyInt()))
            .thenReturn(new ArrayList<>());
        
        // Mock promptRepository.saveAndFlush方法
        when(promptRepository.saveAndFlush(any(Prompt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Mock ApiProperties配置
        ApiProperties.Base baseConfig = new ApiProperties.Base();
        baseConfig.setUrl("http://test-api");
        ApiProperties.Retry retryConfig = new ApiProperties.Retry();
        retryConfig.setMaxAttempts(3);
        baseConfig.setRetry(retryConfig);
        when(apiProperties.getBase()).thenReturn(baseConfig);
        
        // Mock restTemplate调用（用于callPromptApi）
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn("模板内容");
        
        // When - 调用方法
        Prompt result = timerPromptGenerator.generateAndSavePromptForNoonWardRound(
            patientId, 
            templateName, 
            promptType, 
            currentTime
        );
        
        // Then - 验证结果
        assertNotNull(result, "首次查房应成功生成Prompt");
        
        String objectiveContent = result.getObjectiveContent();
        
        // 验证首次查房的标识
        assertTrue(objectiveContent.contains("首次查房"), 
            "首次查房时objectiveContent应包含'首次查房'标识");
        
        // 验证空块有默认占位符
        assertTrue(objectiveContent.contains("自上次查房以来无新的化验结果") || 
                   objectiveContent.contains("无新的化验结果"), 
            "首次查房时化验结果块应有默认占位符");
        assertTrue(objectiveContent.contains("自上次查房以来无新的检查结果") || 
                   objectiveContent.contains("无新的检查结果"), 
            "首次查房时检查结果块应有默认占位符");
    }

    @Test
    @DisplayName("测试5：时间间隔检查未满足时应跳过")
    void testGenerateAndSavePromptForNoonWardRound_StillHonorsTimeIntervalCheck() {
        // Given - 准备测试数据
        String patientId = "TEST_PATIENT_005";
        String templateName = "中午查房记录";
        String promptType = "ward_round";
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 12, 0);
        LocalDateTime todayStart = currentTime.toLocalDate().atStartOfDay();
        
        // Mock阅读状态门槛检查通过
        when(promptResultRepository.countReadDiagnosisAnalysisSince(patientId, todayStart))
            .thenReturn(1);
        
        // Mock当天未生成
        when(promptRepository.countByPatientIdAndPromptTemplateNameAndSubmissionTimeAfter(
            eq(patientId), 
            eq(templateName), 
            any(LocalDateTime.class)
        )).thenReturn(0L);
        
        // Mock有最近一次查房记录，但时间间隔不足（10小时前，小于24小时）
        Prompt lastPrompt = new Prompt();
        lastPrompt.setSubmissionTime(currentTime.minusHours(10));
        when(promptRepository.findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(
            patientId, 
            templateName
        )).thenReturn(Optional.of(lastPrompt));
        
        // When - 调用方法
        Prompt result = timerPromptGenerator.generateAndSavePromptForNoonWardRound(
            patientId, 
            templateName, 
            promptType, 
            currentTime
        );
        
        // Then - 验证结果
        assertNull(result, "时间间隔不足时应返回null");
        
        // 验证promptRepository的saveAndFlush未被调用
        verify(promptRepository, never()).saveAndFlush(any(Prompt.class));
    }
}
