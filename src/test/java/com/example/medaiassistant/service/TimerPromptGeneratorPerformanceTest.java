package com.example.medaiassistant.service;

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
import com.example.medaiassistant.model.LabResult;
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
 * 性能测试：化验结果和检查结果时间区间筛选
 * 
 * 测试目标：
 * - 验证大数据量下的过滤性能
 * - 比较重构前后的性能差异
 * - 确保性能符合预期
 * 
 * @author TDD
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("性能测试：时间区间筛选")
class TimerPromptGeneratorPerformanceTest {
    
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
     * 性能测试1：化验结果筛选 - 小数据量（100条）
     */
    @Test
    @DisplayName("性能测试：化验结果筛选 - 小数据量100条")
    void testLabResultsPerformance_SmallDataSet() throws Exception {
        // Given - 准备100条化验结果
        String patientId = "PERF_TEST_001";
        LocalDateTime lastRoundTime = LocalDateTime.of(2026, 2, 8, 10, 0);
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 10, 0);
        
        List<LabResult> allResults = generateLabResults(100, lastRoundTime, currentTime);
        when(labResultRepository.findByPatientId(anyString())).thenReturn(allResults);
        
        // When - 执行性能测试
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "getLabResultsSinceLastRound", 
            String.class, LocalDateTime.class, LocalDateTime.class
        );
        method.setAccessible(true);
        
        long startTime = System.nanoTime();
        String result = (String) method.invoke(timerPromptGenerator, patientId, lastRoundTime, currentTime);
        long endTime = System.nanoTime();
        
        long executionTime = (endTime - startTime) / 1_000_000; // 转换为毫秒
        
        // Then - 验证性能
        System.out.println("化验结果筛选（100条）执行时间: " + executionTime + "ms");
        assertTrue(executionTime < 50, "100条数据筛选应在50ms内完成，实际: " + executionTime + "ms");
        assertTrue(result.length() > 0, "应返回筛选结果");
    }
    
    /**
     * 性能测试2：化验结果筛选 - 大数据量（1000条）
     */
    @Test
    @DisplayName("性能测试：化验结果筛选 - 大数据量1000条")
    void testLabResultsPerformance_LargeDataSet() throws Exception {
        // Given - 准备1000条化验结果
        String patientId = "PERF_TEST_002";
        LocalDateTime lastRoundTime = LocalDateTime.of(2026, 2, 8, 10, 0);
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 10, 0);
        
        List<LabResult> allResults = generateLabResults(1000, lastRoundTime, currentTime);
        when(labResultRepository.findByPatientId(anyString())).thenReturn(allResults);
        
        // When - 执行性能测试
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "getLabResultsSinceLastRound", 
            String.class, LocalDateTime.class, LocalDateTime.class
        );
        method.setAccessible(true);
        
        long startTime = System.nanoTime();
        String result = (String) method.invoke(timerPromptGenerator, patientId, lastRoundTime, currentTime);
        long endTime = System.nanoTime();
        
        long executionTime = (endTime - startTime) / 1_000_000; // 转换为毫秒
        
        // Then - 验证性能
        System.out.println("化验结果筛选（1000条）执行时间: " + executionTime + "ms");
        assertTrue(executionTime < 200, "1000条数据筛选应在200ms内完成，实际: " + executionTime + "ms");
        assertTrue(result.length() > 0, "应返回筛选结果");
    }
    
    /**
     * 性能测试3：检查结果筛选 - 小数据量（100条）
     */
    @Test
    @DisplayName("性能测试：检查结果筛选 - 小数据量100条")
    void testExaminationResultsPerformance_SmallDataSet() throws Exception {
        // Given - 准备100条检查结果
        String patientId = "PERF_TEST_003";
        LocalDateTime lastRoundTime = LocalDateTime.of(2026, 2, 8, 10, 0);
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 10, 0);
        
        List<ExaminationResult> allResults = generateExaminationResults(100, lastRoundTime, currentTime);
        when(examinationResultRepository.findByPatientId(anyString())).thenReturn(allResults);
        
        // When - 执行性能测试
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "getExaminationResultsSinceLastRound", 
            String.class, LocalDateTime.class, LocalDateTime.class
        );
        method.setAccessible(true);
        
        long startTime = System.nanoTime();
        String result = (String) method.invoke(timerPromptGenerator, patientId, lastRoundTime, currentTime);
        long endTime = System.nanoTime();
        
        long executionTime = (endTime - startTime) / 1_000_000; // 转换为毫秒
        
        // Then - 验证性能
        System.out.println("检查结果筛选（100条）执行时间: " + executionTime + "ms");
        assertTrue(executionTime < 50, "100条数据筛选应在50ms内完成，实际: " + executionTime + "ms");
        assertTrue(result.length() > 0, "应返回筛选结果");
    }
    
    /**
     * 性能测试4：检查结果筛选 - 大数据量（1000条）
     */
    @Test
    @DisplayName("性能测试：检查结果筛选 - 大数据量1000条")
    void testExaminationResultsPerformance_LargeDataSet() throws Exception {
        // Given - 准备1000条检查结果
        String patientId = "PERF_TEST_004";
        LocalDateTime lastRoundTime = LocalDateTime.of(2026, 2, 8, 10, 0);
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 10, 0);
        
        List<ExaminationResult> allResults = generateExaminationResults(1000, lastRoundTime, currentTime);
        when(examinationResultRepository.findByPatientId(anyString())).thenReturn(allResults);
        
        // When - 执行性能测试
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "getExaminationResultsSinceLastRound", 
            String.class, LocalDateTime.class, LocalDateTime.class
        );
        method.setAccessible(true);
        
        long startTime = System.nanoTime();
        String result = (String) method.invoke(timerPromptGenerator, patientId, lastRoundTime, currentTime);
        long endTime = System.nanoTime();
        
        long executionTime = (endTime - startTime) / 1_000_000; // 转换为毫秒
        
        // Then - 验证性能
        System.out.println("检查结果筛选（1000条）执行时间: " + executionTime + "ms");
        assertTrue(executionTime < 200, "1000条数据筛选应在200ms内完成，实际: " + executionTime + "ms");
        assertTrue(result.length() > 0, "应返回筛选结果");
    }
    
    /**
     * 生成测试用化验结果数据
     * 
     * @param count 数据条数
     * @param fromTime 起始时间
     * @param toTime 结束时间
     * @return 化验结果列表
     */
    private List<LabResult> generateLabResults(int count, LocalDateTime fromTime, LocalDateTime toTime) {
        List<LabResult> results = new ArrayList<>();
        
        // 30%在区间前，40%在区间内，30%在区间后
        int beforeCount = (int) (count * 0.3);
        int withinCount = (int) (count * 0.4);
        int afterCount = count - beforeCount - withinCount;
        
        // 区间前的数据
        for (int i = 0; i < beforeCount; i++) {
            LabResult result = new LabResult();
            result.setLabName("化验项目" + i);
            result.setLabResult(String.valueOf(5.0 + i * 0.1));
            result.setUnit("单位");
            result.setLabReportTime(Timestamp.valueOf(fromTime.minusHours(i + 1)));
            results.add(result);
        }
        
        // 区间内的数据
        for (int i = 0; i < withinCount; i++) {
            LabResult result = new LabResult();
            result.setLabName("化验项目" + (beforeCount + i));
            result.setLabResult(String.valueOf(5.0 + i * 0.1));
            result.setUnit("单位");
            result.setLabReportTime(Timestamp.valueOf(fromTime.plusHours(i)));
            results.add(result);
        }
        
        // 区间后的数据
        for (int i = 0; i < afterCount; i++) {
            LabResult result = new LabResult();
            result.setLabName("化验项目" + (beforeCount + withinCount + i));
            result.setLabResult(String.valueOf(5.0 + i * 0.1));
            result.setUnit("单位");
            result.setLabReportTime(Timestamp.valueOf(toTime.plusHours(i + 1)));
            results.add(result);
        }
        
        return results;
    }
    
    /**
     * 生成测试用检查结果数据
     * 
     * @param count 数据条数
     * @param fromTime 起始时间
     * @param toTime 结束时间
     * @return 检查结果列表
     */
    private List<ExaminationResult> generateExaminationResults(int count, LocalDateTime fromTime, LocalDateTime toTime) {
        List<ExaminationResult> results = new ArrayList<>();
        
        // 30%在区间前，40%在区间内，30%在区间后
        int beforeCount = (int) (count * 0.3);
        int withinCount = (int) (count * 0.4);
        int afterCount = count - beforeCount - withinCount;
        
        // 区间前的数据
        for (int i = 0; i < beforeCount; i++) {
            ExaminationResult result = new ExaminationResult();
            result.setExaminationId("EXAM_" + i);
            result.setCheckName("检查项目" + i);
            result.setCheckType("CT");
            result.setCheckReportTime(Timestamp.valueOf(fromTime.minusHours(i + 1)));
            results.add(result);
        }
        
        // 区间内的数据
        for (int i = 0; i < withinCount; i++) {
            ExaminationResult result = new ExaminationResult();
            result.setExaminationId("EXAM_" + (beforeCount + i));
            result.setCheckName("检查项目" + (beforeCount + i));
            result.setCheckType("CT");
            result.setCheckReportTime(Timestamp.valueOf(fromTime.plusHours(i)));
            results.add(result);
        }
        
        // 区间后的数据
        for (int i = 0; i < afterCount; i++) {
            ExaminationResult result = new ExaminationResult();
            result.setExaminationId("EXAM_" + (beforeCount + withinCount + i));
            result.setCheckName("检查项目" + (beforeCount + withinCount + i));
            result.setCheckType("CT");
            result.setCheckReportTime(Timestamp.valueOf(toTime.plusHours(i + 1)));
            results.add(result);
        }
        
        return results;
    }
}
