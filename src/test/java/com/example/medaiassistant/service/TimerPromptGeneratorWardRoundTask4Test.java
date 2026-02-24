package com.example.medaiassistant.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.client.RestTemplate;

import com.example.medaiassistant.config.ApiProperties;
import com.example.medaiassistant.config.SchedulingProperties;
import com.example.medaiassistant.model.ExaminationResult;
import com.example.medaiassistant.repository.EmrContentRepository;
import com.example.medaiassistant.repository.EmrRecordRepository;
import com.example.medaiassistant.repository.ExaminationResultRepository;
import com.example.medaiassistant.repository.LabResultRepository;
import com.example.medaiassistant.repository.MedicalRecordRepository;
import com.example.medaiassistant.repository.PatientRepository;
import com.example.medaiassistant.repository.PromptRepository;
import com.example.medaiassistant.repository.PromptResultRepository;
import com.example.medaiassistant.repository.SurgeryRepository;

/**
 * 任务4：检查结果区间筛选与近期检查结果生成
 * 
 * 测试目标：
 * - 根据checkReportTime字段筛选检查结果
 * - 首次查房时返回最近2天的检查结果
 * - 非首次查房时只返回区间内的检查结果
 * - 区间内无记录时返回固定提示语
 * 
 * @author TDD
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("任务4：检查结果区间筛选与近期检查结果生成")
class TimerPromptGeneratorWardRoundTask4Test {
    
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
    private MedicalRecordRepository medicalRecordRepository;
    
    @Mock
    private LabResultRepository labResultRepository;
    
    @Mock
    private ExaminationResultRepository examinationResultRepository;
    
    @Mock
    private ServerConfigService serverConfigService;
    
    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private ApiProperties apiProperties;
    
    @Mock
    private SchedulingProperties schedulingProperties;
    
    @Mock
    private EmrRecordRepository emrRecordRepository;
    
    @Mock
    private EmrContentRepository emrContentRepository;
    
    @Mock
    private SurgeryRepository surgeryRepository;
    
    @Mock
    private PromptGenerationLogService promptGenerationLogService;
    
    @Mock
    private PromptsTaskUpdater promptsTaskUpdater;
    
    private TimerPromptGenerator timerPromptGenerator;
    
    @BeforeEach
    void setUp() {
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
            null, // longTermOrderRepository
            serverConfigService,
            restTemplate,
            apiProperties,
            schedulingProperties,
            emrRecordRepository,
            emrContentRepository,
            surgeryRepository,
            promptGenerationLogService,
            promptsTaskUpdater
        );
    }
    
    /**
     * 红阶段测试1：检查结果应只包含时间区间内的记录
     * 
     * 验收标准：getExaminationResultsSinceLastRound()方法根据checkReportTime字段筛选检查结果
     */
    @Test
    @DisplayName("检查结果应只包含时间区间内的记录")
    void testGetExaminationResultsSinceLastRound_FiltersByCheckReportTime() throws Exception {
        // Given - 准备测试数据
        String patientId = "TEST_PATIENT_001";
        LocalDateTime lastRoundTime = LocalDateTime.of(2026, 2, 8, 10, 0);
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 10, 0);
        
        List<ExaminationResult> allResults = new ArrayList<>();
        
        // 区间前的记录（不应包含）
        ExaminationResult beforeInterval = new ExaminationResult();
        beforeInterval.setExaminationId("EXAM_BEFORE");
        beforeInterval.setCheckName("CT检查-区间前");
        beforeInterval.setCheckReportTime(Timestamp.valueOf(lastRoundTime.minusHours(1)));
        allResults.add(beforeInterval);
        
        // 区间内的记录（应包含）
        ExaminationResult withinInterval = new ExaminationResult();
        withinInterval.setExaminationId("EXAM_WITHIN");
        withinInterval.setCheckName("超声检查-区间内");
        withinInterval.setCheckReportTime(Timestamp.valueOf(lastRoundTime.plusHours(12)));
        allResults.add(withinInterval);
        
        // 区间后的记录（不应包含）
        ExaminationResult afterInterval = new ExaminationResult();
        afterInterval.setExaminationId("EXAM_AFTER");
        afterInterval.setCheckName("MRI检查-区间后");
        afterInterval.setCheckReportTime(Timestamp.valueOf(currentTime.plusHours(1)));
        allResults.add(afterInterval);
        
        when(examinationResultRepository.findByPatientId(anyString())).thenReturn(allResults);
        
        // When - 通过反射调用private方法
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "getExaminationResultsSinceLastRound", 
            String.class, 
            LocalDateTime.class, 
            LocalDateTime.class
        );
        method.setAccessible(true);
        String result = (String) method.invoke(timerPromptGenerator, patientId, lastRoundTime, currentTime);
        
        // Then - 验证只包含区间内的记录
        assertTrue(result.contains("超声检查-区间内"), "应包含区间内的记录");
        assertFalse(result.contains("区间前"), "不应包含区间前的记录");
        assertFalse(result.contains("区间后"), "不应包含区间后的记录");
    }
    
    /**
     * 红阶段测试2：区间内无检查结果时应返回固定提示语
     * 
     * 验收标准：区间内无记录时返回"自上次查房以来无新的检查结果"
     */
    @Test
    @DisplayName("区间内无检查结果时应返回固定提示语")
    void testGetExaminationResultsSinceLastRound_NoResultsInInterval_ReturnsNoNewExamsMessage() throws Exception {
        // Given - 准备测试数据
        String patientId = "TEST_PATIENT_002";
        LocalDateTime lastRoundTime = LocalDateTime.of(2026, 2, 8, 10, 0);
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 10, 0);
        
        List<ExaminationResult> allResults = new ArrayList<>();
        
        // 只有区间前的记录
        ExaminationResult beforeInterval = new ExaminationResult();
        beforeInterval.setExaminationId("EXAM_OLD");
        beforeInterval.setCheckName("旧检查记录");
        beforeInterval.setCheckReportTime(Timestamp.valueOf(lastRoundTime.minusDays(1)));
        allResults.add(beforeInterval);
        
        when(examinationResultRepository.findByPatientId(anyString())).thenReturn(allResults);
        
        // When - 通过反射调用private方法
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "getExaminationResultsSinceLastRound", 
            String.class, 
            LocalDateTime.class, 
            LocalDateTime.class
        );
        method.setAccessible(true);
        String result = (String) method.invoke(timerPromptGenerator, patientId, lastRoundTime, currentTime);
        
        // Then - 验证返回固定提示语
        assertTrue(result.contains("自上次查房以来无新的检查结果"), "区间内无记录时应返回固定提示语");
    }
    
    /**
     * 红阶段测试3：首次查房应使用最近2天的检查结果
     * 
     * 验收标准：lastRoundTime为null时，返回最近2天的检查结果
     */
    @Test
    @DisplayName("首次查房应使用最近2天的检查结果")
    void testGetExaminationResultsSinceLastRound_FirstWardRound_UsesFallbackDays() throws Exception {
        // Given - 准备测试数据
        String patientId = "TEST_PATIENT_003";
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 10, 0);
        
        List<ExaminationResult> allResults = new ArrayList<>();
        
        // 3天前的记录（不应包含）
        ExaminationResult threeDaysAgo = new ExaminationResult();
        threeDaysAgo.setExaminationId("EXAM_3DAYS");
        threeDaysAgo.setCheckName("CT检查-3天前");
        threeDaysAgo.setCheckReportTime(Timestamp.valueOf(currentTime.minusDays(3)));
        allResults.add(threeDaysAgo);
        
        // 1天前的记录（应包含）
        ExaminationResult oneDayAgo = new ExaminationResult();
        oneDayAgo.setExaminationId("EXAM_1DAY");
        oneDayAgo.setCheckName("超声检查-1天前");
        oneDayAgo.setCheckReportTime(Timestamp.valueOf(currentTime.minusDays(1)));
        allResults.add(oneDayAgo);
        
        when(examinationResultRepository.findByPatientId(anyString())).thenReturn(allResults);
        
        // When - 通过反射调用private方法（lastRoundTime = null表示首次查房）
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "getExaminationResultsSinceLastRound", 
            String.class, 
            LocalDateTime.class, 
            LocalDateTime.class
        );
        method.setAccessible(true);
        String result = (String) method.invoke(timerPromptGenerator, patientId, null, currentTime);
        
        // Then - 验证只包含最近2天的记录
        assertTrue(result.contains("超声检查-1天前"), "应包含最近2天的记录");
        assertFalse(result.contains("3天前"), "不应包含3天前的记录");
    }
    
    /**
     * 边界测试：时间区间边界点的记录应被包含
     * 
     * 验证闭区间 [lastRoundTime, currentTime] 的正确性
     */
    @Test
    @DisplayName("边界条件：时间区间边界点的记录应被包含")
    void testGetExaminationResultsSinceLastRound_BoundaryTimePoints_ShouldBeIncluded() throws Exception {
        // Given - 准备测试数据
        String patientId = "TEST_PATIENT_004";
        LocalDateTime lastRoundTime = LocalDateTime.of(2026, 2, 8, 10, 0, 0);
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 10, 0, 0);
        
        List<ExaminationResult> allResults = new ArrayList<>();
        
        // 区间前1秒（不应包含）
        ExaminationResult beforeStart = new ExaminationResult();
        beforeStart.setExaminationId("EXAM_BEFORE");
        beforeStart.setCheckName("检查-区间前");
        beforeStart.setCheckReportTime(Timestamp.valueOf(lastRoundTime.minusSeconds(1)));
        allResults.add(beforeStart);
        
        // 恰好在lastRoundTime（应包含）
        ExaminationResult atStart = new ExaminationResult();
        atStart.setExaminationId("EXAM_AT_START");
        atStart.setCheckName("检查-起始时间");
        atStart.setCheckReportTime(Timestamp.valueOf(lastRoundTime));
        allResults.add(atStart);
        
        // 恨好在currentTime（应包含）
        ExaminationResult atEnd = new ExaminationResult();
        atEnd.setExaminationId("EXAM_AT_END");
        atEnd.setCheckName("检查-结束时间");
        atEnd.setCheckReportTime(Timestamp.valueOf(currentTime));
        allResults.add(atEnd);
        
        // 区间后1秒（不应包含）
        ExaminationResult afterEnd = new ExaminationResult();
        afterEnd.setExaminationId("EXAM_AFTER");
        afterEnd.setCheckName("检查-区间后");
        afterEnd.setCheckReportTime(Timestamp.valueOf(currentTime.plusSeconds(1)));
        allResults.add(afterEnd);
        
        when(examinationResultRepository.findByPatientId(anyString())).thenReturn(allResults);
        
        // When - 调用方法
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "getExaminationResultsSinceLastRound", 
            String.class, 
            LocalDateTime.class, 
            LocalDateTime.class
        );
        method.setAccessible(true);
        String result = (String) method.invoke(timerPromptGenerator, patientId, lastRoundTime, currentTime);
        
        // Then - 验证闭区间
        assertTrue(result.contains("检查-起始时间"), "应包含起始时间点的记录");
        assertTrue(result.contains("检查-结束时间"), "应包含结束时间点的记录");
        assertFalse(result.contains("区间前"), "不应包含区间前的记录");
        assertFalse(result.contains("区间后"), "不应包含区间后的记录");
    }
}
