package com.example.medaiassistant.service;

import com.example.medaiassistant.config.ApiProperties;
import com.example.medaiassistant.config.SchedulingProperties;
import com.example.medaiassistant.model.LabResult;
import com.example.medaiassistant.repository.*;
import com.example.medaiassistant.service.AlertRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 任务3：化验结果区间筛选与"近期化验结果"生成 - TDD测试
 * 
 * @author MedAiAssistant
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("任务3：化验结果区间筛选与近期化验结果生成")
class TimerPromptGeneratorWardRoundTask3Test {
    
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
            taskScheduler, alertRuleService, patientStatusUpdateService,
            patientRepository, promptRepository, promptResultRepository,
            medicalRecordRepository, labResultRepository, null,
            null, // longTermOrderRepository
            serverConfigService,
            restTemplate, apiProperties, schedulingProperties,
            emrRecordRepository, emrContentRepository, surgeryRepository,
            promptGenerationLogService, promptsTaskUpdater
        );
    }
    
    @Test
    @DisplayName("化验结果应只包含时间区间内的记录")
    void testGetLabResultsSinceLastRound_FiltersByLabReportTime() throws Exception {
        // Given: 准备测试数据
        String patientId = "test-patient-001";
        LocalDateTime lastRoundTime = LocalDateTime.of(2026, 2, 5, 10, 0);
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 14, 0);
        
        // 区间前的记录（不应包含）
        LabResult beforeResult = createLabResult(1L, "白细胞", "血常规", "5.0", 
            Timestamp.valueOf(LocalDateTime.of(2026, 2, 5, 9, 0)), patientId);
        
        // 区间内的记录（应包含）
        LabResult inResult1 = createLabResult(2L, "血红蛋白", "血常规", "120", 
            Timestamp.valueOf(LocalDateTime.of(2026, 2, 6, 10, 0)), patientId);
        LabResult inResult2 = createLabResult(3L, "血糖", "生化", "6.5", 
            Timestamp.valueOf(LocalDateTime.of(2026, 2, 8, 15, 0)), patientId);
        
        List<LabResult> allResults = Arrays.asList(beforeResult, inResult1, inResult2);
        when(labResultRepository.findByPatientId(patientId)).thenReturn(allResults);
        
        // When: 通过反射调用private方法
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "getLabResultsSinceLastRound", 
            String.class, LocalDateTime.class, LocalDateTime.class
        );
        method.setAccessible(true);
        String result = (String) method.invoke(timerPromptGenerator, patientId, lastRoundTime, currentTime);
        
        // Then: 验证结果
        assertNotNull(result, "返回结果不应为null");
        assertFalse(result.contains("白细胞"), "不应包含区间前的化验记录");
        assertTrue(result.contains("血红蛋白") || result.contains("血糖"), "应包含区间内的化验记录");
    }
    
    @Test
    @DisplayName("区间内无化验结果时应返回固定提示语")
    void testGetLabResultsSinceLastRound_NoResultsInInterval_ReturnsNoNewLabsMessage() throws Exception {
        // Given: 区间内无化验结果
        String patientId = "test-patient-002";
        LocalDateTime lastRoundTime = LocalDateTime.of(2026, 2, 5, 10, 0);
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 14, 0);
        
        // 所有记录都在区间之前
        LabResult oldResult = createLabResult(1L, "白细胞", "血常规", "5.0", 
            Timestamp.valueOf(LocalDateTime.of(2026, 2, 4, 9, 0)), patientId);
        
        List<LabResult> allResults = Arrays.asList(oldResult);
        when(labResultRepository.findByPatientId(patientId)).thenReturn(allResults);
        
        // When: 调用方法
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "getLabResultsSinceLastRound", 
            String.class, LocalDateTime.class, LocalDateTime.class
        );
        method.setAccessible(true);
        String result = (String) method.invoke(timerPromptGenerator, patientId, lastRoundTime, currentTime);
        
        // Then: 验证返回固定提示语
        assertNotNull(result, "返回结果不应为null");
        assertEquals("自上次查房以来无新的化验结果", result, "应返回固定提示语");
    }
    
    @Test
    @DisplayName("首次查房时应使用最近2天的化验结果")
    void testGetLabResultsSinceLastRound_FirstWardRound_UsesFallbackDays() throws Exception {
        // Given: lastRoundTime为null（首次查房）
        String patientId = "test-patient-003";
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 14, 0);
        
        // 3天前的记录（不应包含）
        LabResult oldResult = createLabResult(1L, "白细胞", "血常规", "5.0", 
            Timestamp.valueOf(LocalDateTime.of(2026, 2, 6, 13, 0)), patientId);
        
        // 最近2天内的记录（应包含）
        LabResult recentResult1 = createLabResult(2L, "血红蛋白", "血常规", "120", 
            Timestamp.valueOf(LocalDateTime.of(2026, 2, 7, 15, 0)), patientId);
        LabResult recentResult2 = createLabResult(3L, "血糖", "生化", "6.5", 
            Timestamp.valueOf(LocalDateTime.of(2026, 2, 9, 10, 0)), patientId);
        
        List<LabResult> allResults = Arrays.asList(oldResult, recentResult1, recentResult2);
        when(labResultRepository.findByPatientId(patientId)).thenReturn(allResults);
        
        // When: 调用方法（lastRoundTime为null）
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "getLabResultsSinceLastRound", 
            String.class, LocalDateTime.class, LocalDateTime.class
        );
        method.setAccessible(true);
        String result = (String) method.invoke(timerPromptGenerator, patientId, null, currentTime);
        
        // Then: 验证只包含最近2天的记录
        assertNotNull(result, "返回结果不应为null");
        assertFalse(result.contains("白细胞"), "不应包含3天前的化验记录");
        assertTrue(result.contains("血红蛋白") || result.contains("血糖"), "应包含最近2天的化验记录");
    }
    
    @Test
    @DisplayName("边界条件：时间区间边界点的记录应被包含")
    void testGetLabResultsSinceLastRound_BoundaryTimePoints_ShouldBeIncluded() throws Exception {
        // Given: 测试闭区间 [lastRoundTime, currentTime]
        String patientId = "test-patient-004";
        LocalDateTime lastRoundTime = LocalDateTime.of(2026, 2, 5, 10, 0, 0);
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 14, 0, 0);
        
        // 恰好在lastRoundTime的记录（应包含）
        LabResult boundaryStart = createLabResult(1L, "白细胞", "血常规", "5.0", 
            Timestamp.valueOf(lastRoundTime), patientId);
        
        // 恰好在currentTime的记录（应包含）
        LabResult boundaryEnd = createLabResult(2L, "血红蛋白", "血常规", "120", 
            Timestamp.valueOf(currentTime), patientId);
        
        // 区间之前1秒（不应包含）
        LabResult beforeStart = createLabResult(3L, "血糖", "生化", "6.5", 
            Timestamp.valueOf(lastRoundTime.minusSeconds(1)), patientId);
        
        // 区间之后1秒（不应包含）
        LabResult afterEnd = createLabResult(4L, "肉碱酮", "生化", "80", 
            Timestamp.valueOf(currentTime.plusSeconds(1)), patientId);
        
        List<LabResult> allResults = Arrays.asList(beforeStart, boundaryStart, boundaryEnd, afterEnd);
        when(labResultRepository.findByPatientId(patientId)).thenReturn(allResults);
        
        // When: 调用方法
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "getLabResultsSinceLastRound", 
            String.class, LocalDateTime.class, LocalDateTime.class
        );
        method.setAccessible(true);
        String result = (String) method.invoke(timerPromptGenerator, patientId, lastRoundTime, currentTime);
        
        // Then: 验证边界点
        assertNotNull(result, "返回结果不应为null");
        assertTrue(result.contains("白细胞"), "应包含起始边界点的记录");
        assertTrue(result.contains("血红蛋白"), "应包含结束边界点的记录");
        assertFalse(result.contains("血糖"), "不应包含区间之前的记录");
        assertFalse(result.contains("肉碱酮"), "不应包含区间之后的记录");
    }
    
    /**
     * 创建化验结果测试数据
     */
    private LabResult createLabResult(Long id, String labName, String labType, 
                                      String labResultValue, Timestamp labReportTime, String patientId) {
        LabResult result = new LabResult();
        result.setId(id);
        result.setLabName(labName);
        result.setLabType(labType);
        result.setLabResult(labResultValue);
        result.setLabReportTime(labReportTime);
        result.setPatientId(patientId);
        result.setUnit("单位");
        result.setAbnormalIndicator("N");
        return result;
    }
}
