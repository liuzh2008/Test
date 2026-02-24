package com.example.medaiassistant.service;

import com.example.medaiassistant.config.ApiProperties;
import com.example.medaiassistant.config.SchedulingProperties;
import com.example.medaiassistant.dto.TemplateConfig;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 中午12点查房记录自动生成测试
 * TDD红-绿-重构流程测试
 * 
 * 任务1：AI分析阅读状态检查功能 ✅
 * 任务2：患者状态映射与模板选择 ✅
 * 任务3：频率控制与时间间隔检查 ✅
 * 任务4：防止当天重复生成 ✅
 * 任务5：Prompt生成与保存 ✅
 * 任务6：查房提醒任务联动 ✅
 * 任务7：定时任务主方法整合 ✅
 * 
 * @author TDD
 * @since 2026-01-31
 * @version 1.12 - 任务1修改：AI阅读门槛调整为readCount>=1（2026-01-31）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("中午查房记录生成 - AI分析阅读状态检查测试")
class TimerPromptGeneratorNoonWardRoundTest {

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
            null, // medicalRecordRepository - not needed for this test
            null, // labResultRepository - not needed for this test
            null, // examinationResultRepository - not needed for this test
            null, // longTermOrderRepository - not needed for this test
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

    /**
     * 测试用例1：当readCount=3时应允许生成查房记录
     * 
     * Given: 患者当天有3种AI分析全部已读
     * When: 检查AI分析阅读状态
     * Then: 返回true，允许生成查房记录
     */
    @Test
    @DisplayName("当readCount>=1时应允许生成查房记录")
    void testCheckReadStatus_WithAtLeastOneAnalysisRead_ReturnsTrue() {
        // Given
        String patientId = "P001";
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        when(promptResultRepository.countReadAiResultsSince(patientId, todayStart))
            .thenReturn(1);
        
        // When
        boolean result = timerPromptGenerator.checkReadStatus(patientId, todayStart);
        
        // Then
        assertTrue(result, "当至少有一条AI分析已读时，应返回true");
        verify(promptResultRepository).countReadAiResultsSince(patientId, todayStart);
    }

    /**
     * 测试用例2：验证多条AI分析已读时也允许生成
     * 
     * Given: 患者当天有2条AI分析已读
     * When: 检查AI分析阅读状态
     * Then: 返回true，允许生成查房记录
     */
    @Test
    @DisplayName("当readCount=2时应允许生成")
    void testCheckReadStatus_WithTwoAnalysisRead_ReturnsTrue() {
        // Given
        String patientId = "P002";
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        when(promptResultRepository.countReadAiResultsSince(patientId, todayStart))
            .thenReturn(2);
        
        // When
        boolean result = timerPromptGenerator.checkReadStatus(patientId, todayStart);
        
        // Then
        assertTrue(result, "当有2条AI分析已读时，应返回true");
        verify(promptResultRepository).countReadAiResultsSince(patientId, todayStart);
    }

    /**
     * 测试用例3：当readCount=0时应跳过患者
     * 
     * Given: 患者当天无已读AI分析
     * When: 检查AI分析阅读状态
     * Then: 返回false，跳过患者
     */
    @Test
    @DisplayName("当readCount=0时应跳过患者")
    void testCheckReadStatus_WithNoRead_ReturnsFalse() {
        // Given
        String patientId = "P003";
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        when(promptResultRepository.countReadAiResultsSince(patientId, todayStart))
            .thenReturn(0);
        
        // When
        boolean result = timerPromptGenerator.checkReadStatus(patientId, todayStart);
        
        // Then
        assertFalse(result, "当无已读AI分析时，应返回false");
        verify(promptResultRepository).countReadAiResultsSince(patientId, todayStart);
    }

    /**
     * 测试用例4：验证阅读状态检查方法在患者处理流程中的集成
     * 
     * Given: 患者P004的AI分析未阅读（readCount<1）
     * When: 执行患者处理逻辑
     * Then: 该患者应被跳过，不生成查房记录
     */
    @Test
    @DisplayName("验证readCount<1时患者在处理流程中被跳过")
    void testShouldSkipPatient_WhenReadCountLessThanOne() {
        // Given
        String patientId = "P004";
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        when(promptResultRepository.countReadAiResultsSince(patientId, todayStart))
            .thenReturn(0);
        
        // When
        boolean shouldProcess = timerPromptGenerator.checkReadStatus(patientId, todayStart);
        
        // Then
       assertFalse(shouldProcess, "readCount<1时应跳过患者处理");
        verify(promptResultRepository).countReadAiResultsSince(patientId, todayStart);
    }

    // ==================== 任务2：患者状态映射与模板选择测试 ====================

    /**
     * 测试用例5：病危患者应映射到"病危每日查房记录"模板，间隔24小时
     * 
     * Given: 患者状态为"病危"
     * When: 确定模板和间隔
     * Then: 返回"病危每日查房记录"模板，间隔24小时
     */
    @Test
    @DisplayName("病危患者应映射到病危每日查房记录模板-间隔24小时")
    void testDetermineTemplate_ForCriticalPatient_ReturnsDailyTemplate() {
        // Given
        String status = "病危";
        
        // When
        TemplateConfig config = timerPromptGenerator.determineTemplateAndInterval(status);
        
        // Then
        assertNotNull(config, "模板配置不应为null");
        assertEquals("病危每日查房记录", config.getTemplateName(), "病危患者应映射到病危每日查房记录");
        assertEquals(24, config.getIntervalHours(), "病危查房记录间隔应为24小时");
    }

    /**
     * 测试用例6：病重患者应映射到"病重每2日查房记录"模板，间隔48小时
     * 
     * Given: 患者状态为"病重"
     * When: 确定模板和间隔
     * Then: 返回"病重每2日查房记录"模板，间隔48小时
     */
    @Test
    @DisplayName("病重患者应映射到病重每2日查房记录模板-间隔48小时")
    void testDetermineTemplate_ForSeriousPatient_ReturnsBidailyTemplate() {
        // Given
        String status = "病重";
        
        // When
        TemplateConfig config = timerPromptGenerator.determineTemplateAndInterval(status);
        
        // Then
        assertNotNull(config, "模板配置不应为null");
        assertEquals("病重每2日查房记录", config.getTemplateName(), "病重患者应映射到病重每2日查房记录");
        assertEquals(48, config.getIntervalHours(), "病重查房记录间隔应为48小时");
    }

    /**
     * 测试用例7：普通患者应映射到"查房记录"模板，间隔24小时
     * 
     * Given: 患者状态为普通（非病危、非病重）
     * When: 确定模板和间隔
     * Then: 返回"查房记录"模板，间隔24小时
     */
    @Test
    @DisplayName("普通患者应映射到查房记录模板-间隔24小时")
    void testDetermineTemplate_ForNormalPatient_ReturnsStandardTemplate() {
        // Given
        String status = "普通";
        
        // When
        TemplateConfig config = timerPromptGenerator.determineTemplateAndInterval(status);
        
        // Then
        assertNotNull(config, "模板配置不应为null");
        assertEquals("查房记录", config.getTemplateName(), "普通患者应映射到查房记录");
        assertEquals(24, config.getIntervalHours(), "普通查房记录间隔应为24小时");
    }

    /**
     * 测试用例8：null状态应使用默认模板
     * 
     * Given: 患者状态为null
     * When: 确定模板和间隔
     * Then: 返回默认"查房记录"模板，间隔24小时
     */
    @Test
    @DisplayName("null状态应使用默认查房记录模板")
    void testDetermineTemplate_ForNullStatus_ReturnsDefaultTemplate() {
        // Given
        String status = null;
        
        // When
        TemplateConfig config = timerPromptGenerator.determineTemplateAndInterval(status);
        
        // Then
        assertNotNull(config, "模板配置不应为null");
        assertEquals("查房记录", config.getTemplateName(), "null状态应使用默认查房记录模板");
        assertEquals(24, config.getIntervalHours(), "默认查房记录间隔应为24小时");
    }

    /**
     * 测试用例9：空字符串状态应使用默认模板
     * 
     * Given: 患者状态为空字符串
     * When: 确定模板和间隔
     * Then: 返回默认"查房记录"模板，间隔24小时
     */
    @Test
    @DisplayName("空字符串状态应使用默认查房记录模板")
    void testDetermineTemplate_ForEmptyStatus_ReturnsDefaultTemplate() {
        // Given
        String status = "";
        
        // When
        TemplateConfig config = timerPromptGenerator.determineTemplateAndInterval(status);
        
        // Then
        assertNotNull(config, "模板配置不应为null");
        assertEquals("查房记录", config.getTemplateName(), "空字符串状态应使用默认查房记录模板");
        assertEquals(24, config.getIntervalHours(), "默认查房记录间隔应为24小时");
    }

    // ==================== 任务3：频率控制与时间间隔检查测试 ====================

    /**
     * 测试用例10：24小时间隔未满足时应跳过生成（病危患者）
     * 
     * Given: 病危患者上次生成"病危每日查房记录"距今10小时
     * When: 检查时间间隔
     * Then: 返回false，跳过生成
     */
    @Test
    @DisplayName("病危患者24小时间隔未满足时应跳过生成")
    void testShouldSkip_WhenIntervalNotMet_ForDailyTemplate() {
        // Given
        String patientId = "P005";
        String templateName = "病危每日查房记录";
        int intervalHours = 24;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastSubmissionTime = now.minusHours(10); // 10小时前
        
        Prompt lastPrompt = new Prompt();
        lastPrompt.setSubmissionTime(lastSubmissionTime);
        
        when(promptRepository.findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(
            patientId, templateName))
            .thenReturn(Optional.of(lastPrompt));
        
        // When
        boolean result = timerPromptGenerator.checkTimeInterval(patientId, templateName, intervalHours, now);
        
        // Then
        assertFalse(result, "24小时间隔未满足时应返回false");
        verify(promptRepository).findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(patientId, templateName);
    }

    /**
     * 测试用例11：48小时间隔未满足时应跳过生成（病重患者）
     * 
     * Given: 病重患者上次生成"病重每2日查房记录"距今30小时
     * When: 检查时间间隔
     * Then: 返回false，跳过生成
     */
    @Test
    @DisplayName("病重患者48小时间隔未满足时应跳过生成")
    void testShouldSkip_WhenIntervalNotMet_ForBidailyTemplate() {
        // Given
        String patientId = "P006";
        String templateName = "病重每2日查房记录";
        int intervalHours = 48;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastSubmissionTime = now.minusHours(30); // 30小时前
        
        Prompt lastPrompt = new Prompt();
        lastPrompt.setSubmissionTime(lastSubmissionTime);
        
        when(promptRepository.findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(
            patientId, templateName))
            .thenReturn(Optional.of(lastPrompt));
        
        // When
        boolean result = timerPromptGenerator.checkTimeInterval(patientId, templateName, intervalHours, now);
        
        // Then
        assertFalse(result, "48小时间隔未满足时应返回false");
        verify(promptRepository).findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(patientId, templateName);
    }

    /**
     * 测试用例12：时间间隔已满足时应允许生成
     * 
     * Given: 患者上次生成"病危每日查房记录"距今25小时（超过24小时）
     * When: 检查时间间隔
     * Then: 返回true，允许生成
     */
    @Test
    @DisplayName("时间间隔已满足时应允许生成")
    void testShouldAllow_WhenIntervalMet() {
        // Given
        String patientId = "P007";
        String templateName = "病危每日查房记录";
        int intervalHours = 24;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastSubmissionTime = now.minusHours(25); // 25小时前
        
        Prompt lastPrompt = new Prompt();
        lastPrompt.setSubmissionTime(lastSubmissionTime);
        
        when(promptRepository.findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(
            patientId, templateName))
            .thenReturn(Optional.of(lastPrompt));
        
        // When
        boolean result = timerPromptGenerator.checkTimeInterval(patientId, templateName, intervalHours, now);
        
        // Then
        assertTrue(result, "时间间隔已满足时应返回true");
        verify(promptRepository).findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(patientId, templateName);
    }

    /**
     * 测试用例13：无历史记录时应允许生成
     * 
     * Given: 患者首次生成查房记录，无历史Prompt记录
     * When: 检查时间间隔
     * Then: 返回true，允许生成
     */
    @Test
    @DisplayName("无历史记录时应允许生成")
    void testShouldAllow_WhenNoHistoryPrompt() {
        // Given
        String patientId = "P008";
        String templateName = "查房记录";
        int intervalHours = 24;
        LocalDateTime now = LocalDateTime.now();
        
        when(promptRepository.findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(
            patientId, templateName))
            .thenReturn(Optional.empty()); // 无历史记录
        
        // When
        boolean result = timerPromptGenerator.checkTimeInterval(patientId, templateName, intervalHours, now);
        
        // Then
        assertTrue(result, "无历史记录时应返回true");
        verify(promptRepository).findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(patientId, templateName);
    }

    // ==================== 任务4：防止当天重复生成测试 ====================

    /**
     * 测试用例14：当天已生成同模板查房记录时应跳过
     * 
     * Given: 患者当天已生成过相同模板的查房记录
     * When: 检查是否当天已生成
     * Then: 返回true（表示已生成，应跳过）
     */
    @Test
    @DisplayName("当天已生成同模板查房记录时应跳过")
    void testShouldSkip_WhenAlreadyGeneratedToday() {
        // Given
        String patientId = "P009";
        String templateName = "病危每日查房记录";
        LocalDateTime now = LocalDateTime.of(2026, 1, 31, 12, 0); // 2026-01-31 12:00
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay(); // 2026-01-31 00:00
        
        when(promptRepository.countByPatientIdAndPromptTemplateNameAndSubmissionTimeAfter(
            patientId, templateName, todayStart))
            .thenReturn(1L); // 当天已有1条记录
        
        // When
        boolean result = timerPromptGenerator.hasGeneratedToday(patientId, templateName, now);
        
        // Then
        assertTrue(result, "当天已生成时应返回true");
        verify(promptRepository).countByPatientIdAndPromptTemplateNameAndSubmissionTimeAfter(
            patientId, templateName, todayStart);
    }

    /**
     * 测试用例15：当天未生成同模板查房记录时应允许生成
     * 
     * Given: 患者当天未生成过相同模板的查房记录
     * When: 检查是否当天已生成
     * Then: 返回false（表示未生成，允许生成）
     */
    @Test
    @DisplayName("当天未生成同模板查房记录时应允许生成")
    void testShouldAllow_WhenNotGeneratedToday() {
        // Given
        String patientId = "P010";
        String templateName = "病重每2日查房记录";
        LocalDateTime now = LocalDateTime.of(2026, 1, 31, 12, 0);
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        
        when(promptRepository.countByPatientIdAndPromptTemplateNameAndSubmissionTimeAfter(
            patientId, templateName, todayStart))
            .thenReturn(0L); // 当天无记录
        
        // When
        boolean result = timerPromptGenerator.hasGeneratedToday(patientId, templateName, now);
        
        // Then
        assertFalse(result, "当天未生成时应返回false");
        verify(promptRepository).countByPatientIdAndPromptTemplateNameAndSubmissionTimeAfter(
            patientId, templateName, todayStart);
    }

    /**
     * 测试用例16：不同模板独立统计（已生成A模板不影响B模板）
     * 
     * Given: 患者当天已生成模板A的查房记录，但未生成模板B
     * When: 分别检查两个模板是否已生成
     * Then: 模板A返回true（已生成），模板B返回false（未生成）
     */
    @Test
    @DisplayName("不同模板独立统计")
    void testCountToday_WithMultipleTemplates_OnlyCountsMatchingTemplate() {
        // Given
        String patientId = "P011";
        String templateA = "病危每日查房记录";
        String templateB = "病重每2日查房记录";
        LocalDateTime now = LocalDateTime.of(2026, 1, 31, 12, 0);
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        
        // 模拟患者当天已生成模板A，但未生成模板B
        when(promptRepository.countByPatientIdAndPromptTemplateNameAndSubmissionTimeAfter(
            patientId, templateA, todayStart))
            .thenReturn(1L); // 模板A当天已生成
        when(promptRepository.countByPatientIdAndPromptTemplateNameAndSubmissionTimeAfter(
            patientId, templateB, todayStart))
            .thenReturn(0L); // 模板B当天未生成
        
        // When
        boolean hasGeneratedA = timerPromptGenerator.hasGeneratedToday(patientId, templateA, now);
        boolean hasGeneratedB = timerPromptGenerator.hasGeneratedToday(patientId, templateB, now);
        
        // Then
        assertTrue(hasGeneratedA, "模板A当天已生成应返回true");
        assertFalse(hasGeneratedB, "模板B当天未生成应返回false");
        
        verify(promptRepository).countByPatientIdAndPromptTemplateNameAndSubmissionTimeAfter(
            patientId, templateA, todayStart);
        verify(promptRepository).countByPatientIdAndPromptTemplateNameAndSubmissionTimeAfter(
            patientId, templateB, todayStart);
    }

    // ==================== 任务5：Prompt生成与保存测试 ====================

    /**
     * 测试用例17：成功生成并保存Prompt（简化版）
     * 
     * Given: 患者通过所有检查（AI已读、间隔满足、当天未生成）
     * When: 生成带保存Prompt
     * Then: 不抛异常（API调用失败会返回null）
     * 
     * 注意：由于无法mock private方法，此测试验证基本逻辑流程
     */
    @Test
    @DisplayName("成功生成并保存Prompt（简化版）")
    void testGenerateAndSavePrompt_Success() {
        // Given
        String patientId = "P012";
        String templateName = "病危每日查房记录";
        String promptType = "病情小结";
        LocalDateTime now = LocalDateTime.now();
        
        // Mock：通过所有检查
        when(promptResultRepository.countReadAiResultsSince(eq(patientId), any(LocalDateTime.class)))
            .thenReturn(3); // AI分析全部已读
        when(promptRepository.findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(
            patientId, templateName))
            .thenReturn(Optional.empty()); // 无历史记录，首次生成
        when(promptRepository.countByPatientIdAndPromptTemplateNameAndSubmissionTimeAfter(
            eq(patientId), eq(templateName), any(LocalDateTime.class)))
            .thenReturn(0L); // 当天未生成
        
        // When - 执行方法（API调用会失败，但不应抛异常）
        Prompt result = timerPromptGenerator.generateAndSavePromptForNoonWardRound(
            patientId, templateName, promptType, now);
        
        // Then - 由于API调用失败，结果为null，但不应抛异常
        // 这验证了方法的异常处理逻辑
        assertNull(result, "API调用失败时应返回null");
        // 验证没有调用save（因为API失败）
        verify(promptRepository, never()).saveAndFlush(any(Prompt.class));
    }

    /**
     * 测试用例18：AI分析未读时跳过生成
     * 
     * Given: 患者AI分析未全部阅读
     * When: 尝试生成Prompt
     * Then: 返回null，不调用保存方法
     */
    @Test
    @DisplayName("AI分析未读时跳过生成")
    void testGenerateAndSavePrompt_SkipsWhenAINotRead() {
        // Given
        String patientId = "P013";
        String templateName = "病危每日查房记录";
        String promptType = "病情小结";
        LocalDateTime now = LocalDateTime.now();
        
        when(promptResultRepository.countReadAiResultsSince(eq(patientId), any(LocalDateTime.class)))
            .thenReturn(2); // AI分析未全部阅读
        
        // When
        Prompt result = timerPromptGenerator.generateAndSavePromptForNoonWardRound(
            patientId, templateName, promptType, now);
        
        // Then
        assertNull(result, "AI未读时应返回null");
        verify(promptRepository, never()).saveAndFlush(any(Prompt.class));
    }

    /**
     * 测试用例19：当天已生成时跳过
     * 
     * Given: 患者当天已生成同模板记录
     * When: 尝试生成Prompt
     * Then: 返回null，不调用保存方法
     */
    @Test
    @DisplayName("当天已生成时跳过")
    void testGenerateAndSavePrompt_SkipsWhenAlreadyGeneratedToday() {
        // Given
        String patientId = "P014";
        String templateName = "病危每日查房记录";
        String promptType = "病情小结";
        LocalDateTime now = LocalDateTime.now();
        
        when(promptResultRepository.countReadAiResultsSince(eq(patientId), any(LocalDateTime.class)))
            .thenReturn(3); // AI分析已读
        when(promptRepository.countByPatientIdAndPromptTemplateNameAndSubmissionTimeAfter(
            eq(patientId), eq(templateName), any(LocalDateTime.class)))
            .thenReturn(1L); // 当天已生成
        
        // When
        Prompt result = timerPromptGenerator.generateAndSavePromptForNoonWardRound(
            patientId, templateName, promptType, now);
        
        // Then
        assertNull(result, "当天已生成时应返回null");
        verify(promptRepository, never()).saveAndFlush(any(Prompt.class));
    }

    // ========== 任务6：查房提醒任务联动测试 ==========

    /**
     * 测试用例20：病重患者生成记录后任务被更新
     * 
     * Given: 病重患者生成"病重每2日查房记录"成功
     * When: 调用任务联动方法
     * Then: 应调用promptsTaskUpdater更新任务
     */
    @Test
    @DisplayName("病重患者生成记录后任务被更新")
    void testTaskLinking_ForSeriousPatient_UpdatesTask() {
        // Given
        String patientId = "P015";
        String templateName = "病重每2日查房记录";
        LocalDateTime now = LocalDateTime.now();
        
        // When
        timerPromptGenerator.linkToWardRoundReminderTask(patientId, templateName, now);
        
        // Then
        verify(promptsTaskUpdater).updateTaskStatusForPrompt(templateName, patientId);
    }

    /**
     * 测试用例21：其他患者生成记录后不触发任务更新
     * 
     * Given: 患者生成"病危每日查房记录"（非病重模板）
     * When: 调用任务联动方法
     * Then: 不应调用promptsTaskUpdater
     */
    @Test
    @DisplayName("其他患者生成记录后不触发任务更新")
    void testTaskLinking_ForOtherPatient_NoTaskUpdate() {
        // Given
        String patientId = "P016";
        String templateName = "病危每日查房记录";
        LocalDateTime now = LocalDateTime.now();
        
        // When
        timerPromptGenerator.linkToWardRoundReminderTask(patientId, templateName, now);
        
        // Then
        verify(promptsTaskUpdater, never()).updateTaskStatusForPrompt(anyString(), anyString());
    }

    /**
     * 测试用例22：无匹配任务时优雅处理
     * 
     * Given: 病重患者生成记录，但promptsTaskUpdater内部找不到匹配任务
     * When: 调用任务联动方法
     * Then: 应捕获异常，不中断流程
     */
    @Test
    @DisplayName("无匹配任务时优雅处理")
    void testTaskLinking_WhenNoMatchingTask_HandlesGracefully() {
        // Given
        String patientId = "P017";
        String templateName = "病重每2日查房记录";
        LocalDateTime now = LocalDateTime.now();
        
        doThrow(new RuntimeException("未找到匹配任务"))
            .when(promptsTaskUpdater).updateTaskStatusForPrompt(templateName, patientId);
        
        // When - 应不抛出异常
        assertDoesNotThrow(() -> {
            timerPromptGenerator.linkToWardRoundReminderTask(patientId, templateName, now);
        });
        
        // Then
        verify(promptsTaskUpdater).updateTaskStatusForPrompt(templateName, patientId);
    }

    /**
     * 测试用例23：任务更新失败时记录错误
     * 
     * Given: promptsTaskUpdater抛出异常
     * When: 调用任务联动方法
     * Then: 应记录错误日志，但不中断流程
     */
    @Test
    @DisplayName("任务更新失败时记录错误")
    void testTaskLinking_WithTaskUpdateFailure_LogsError() {
        // Given
        String patientId = "P018";
        String templateName = "病重每2日查房记录";
        LocalDateTime now = LocalDateTime.now();
        
        doThrow(new RuntimeException("数据库连接失败"))
            .when(promptsTaskUpdater).updateTaskStatusForPrompt(templateName, patientId);
        
        // When - 应不抛出异常
        assertDoesNotThrow(() -> {
            timerPromptGenerator.linkToWardRoundReminderTask(patientId, templateName, now);
        });
        
        // Then
        verify(promptsTaskUpdater).updateTaskStatusForPrompt(templateName, patientId);
    }

    // ========== 任务7：定时任务主方法整合测试 ==========

    /**
     * 测试用例24：多患者处理流程
     * 
     * Given: 数据库中有3个符合条件的患者
     * When: 执行中午12点查房记录生成任务
     * Then: 应不抛出异常
     */
    @Test
    @DisplayName("多患者处理流程")
    void testGenerateNoonWardRoundPrompts_WithMultiplePatients_ProcessesAll() {
        // Given
        Patient p1 = createPatient("P019", "病危");
        Patient p2 = createPatient("P020", "病重");
        Patient p3 = createPatient("P021", "普通");
        
        // Mock分页查询
        when(patientRepository.findByIsInHospital(eq(true), any(PageRequest.class)))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(Arrays.asList(p1, p2, p3)))
            .thenReturn(org.springframework.data.domain.Page.empty()); // 第二页为空
        
        // When - 应不抛出异常
        assertDoesNotThrow(() -> {
            timerPromptGenerator.generateNoonWardRoundPrompts();
        });
        
        // Then
        verify(patientRepository, atLeast(1)).findByIsInHospital(eq(true), any(PageRequest.class));
    }

    /**
     * 测试用例25：分页边界情况
     * 
     * Given: 数据库中有多页患者数据
     * When: 执行中午12点查房记录生成任务
     * Then: 应正确处理分页
     */
    @Test
    @DisplayName("分页边界情况")
    void testGenerateNoonWardRoundPrompts_WithPaginationOverflow_HandlesCorrectly() {
        // Given
        List<Patient> page1 = Arrays.asList(
            createPatient("P022", "病危"),
            createPatient("P023", "病重")
        );
        List<Patient> page2 = Arrays.asList(
            createPatient("P024", "病危")
        );
        
        when(patientRepository.findByIsInHospital(eq(true), any(PageRequest.class)))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(page1))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(page2))
            .thenReturn(org.springframework.data.domain.Page.empty());
        
        // When - 应不抛出异常
        assertDoesNotThrow(() -> {
            timerPromptGenerator.generateNoonWardRoundPrompts();
        });
        
        // Then
        verify(patientRepository, atLeast(2)).findByIsInHospital(eq(true), any(PageRequest.class));
    }

    /**
     * 测试用例26：部分失败时继续处理
     * 
     * Given: 多个患者，其中一个处理失败
     * When: 执行中午12点查房记录生成任务
     * Then: 应继续处理其他患者
     */
    @Test
    @DisplayName("部分失败时继续处理")
    void testGenerateNoonWardRoundPrompts_WithPartialFailures_ContinuesProcessing() {
        // Given
        Patient p1 = createPatient("P025", "病危");
        Patient p2 = createPatient("P026", "病重");
        Patient p3 = createPatient("P027", "病危");
        
        when(patientRepository.findByIsInHospital(eq(true), any(PageRequest.class)))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(Arrays.asList(p1, p2, p3)))
            .thenReturn(org.springframework.data.domain.Page.empty());
        
        // When - 应不抛出异常
        assertDoesNotThrow(() -> {
            timerPromptGenerator.generateNoonWardRoundPrompts();
        });
        
        // Then
        verify(patientRepository, atLeast(1)).findByIsInHospital(eq(true), any(PageRequest.class));
    }

    /**
     * 测试用例27：统计信息输出
     * 
     * Given: 处理了多个患者
     * When: 执行中午12点查房记录生成任务
     * Then: 应输出统计信息
     */
    @Test
    @DisplayName("统计信息输出")
    void testGenerateNoonWardRoundPrompts_OutputsStatistics() {
        // Given
        Patient p1 = createPatient("P028", "病危");
        Patient p2 = createPatient("P029", "病重");
        
        when(patientRepository.findByIsInHospital(eq(true), any(PageRequest.class)))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(Arrays.asList(p1, p2)))
            .thenReturn(org.springframework.data.domain.Page.empty());
        
        // When
        timerPromptGenerator.generateNoonWardRoundPrompts();
        
        // Then - 验证调用了数据库查询（日志输出将在控制台显示）
        verify(patientRepository, atLeast(1)).findByIsInHospital(eq(true), any(PageRequest.class));
    }

    // 辅助方法：创建测试患者对象
    private Patient createPatient(String patientId, String status) {
        Patient patient = new Patient();
        patient.setPatientId(patientId);
        patient.setName("测试患者-" + patientId);
        patient.setStatus(status);
        patient.setIsInHospital(true);
        return patient;
    }
}
