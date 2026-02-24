package com.example.medaiassistant.service;

import com.example.medaiassistant.config.ApiProperties;
import com.example.medaiassistant.config.SchedulingProperties;
import com.example.medaiassistant.model.MedicalRecord;
import com.example.medaiassistant.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 任务2：病历记录区间筛选与"本次病情记录"生成 - 红阶段测试
 * 
 * 测试TimerPromptGenerator中病历记录按时间区间筛选的方法
 * 使用Mockito进行业务逻辑层单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("任务2：病历记录区间筛选与本次病情记录生成")
class TimerPromptGeneratorWardRoundTask2Test {

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
        // 创建TimerPromptGenerator实例
        timerPromptGenerator = new TimerPromptGenerator(
            taskScheduler,
            alertRuleService,
            patientStatusUpdateService,
            patientRepository,
            promptRepository,
            promptResultRepository,
            medicalRecordRepository,
            null, // labResultRepository - not needed for this test
            null, // examinationResultRepository - not needed for this test
            null, // longTermOrderRepository - not needed for this test
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
     * 红阶段测试用例1：只包含区间内的病历记录
     * 
     * Given：准备多条MedicalRecord：
     *   - 1条 recordTime < lastRoundTime
     *   - 2条 recordTime ∈ [lastRoundTime, currentTime]
     *   - 1条 recordTime > currentTime
     * When：调用getCurrentMedicalRecordText(patientId, lastRoundTime, currentTime)
     * Then：返回文本只包含区间内两条记录内容，按时间排序
     */
    @Test
    @DisplayName("病历记录应只包含时间区间内的记录")
    void testGetCurrentMedicalRecordText_OnlyIncludesRecordsWithinInterval() throws Exception {
        // Arrange
        String patientId = "TEST_PATIENT_001";
        LocalDateTime lastRoundTime = LocalDateTime.of(2026, 2, 9, 12, 0);
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 10, 12, 0);
        
        List<MedicalRecord> allRecords = new ArrayList<>();
        
        // 1. 区间前的记录（不应包含）
        MedicalRecord recordBefore = new MedicalRecord();
        recordBefore.setPatientId(patientId);
        recordBefore.setRecordTime(convertToDate(LocalDateTime.of(2026, 2, 9, 10, 0)));
        recordBefore.setMedicalContent("区间前的病历记录");
        recordBefore.setRecordingDoctor("Doctor A");
        allRecords.add(recordBefore);
        
        // 2. 区间内的第一条记录（应包含）
        MedicalRecord recordInside1 = new MedicalRecord();
        recordInside1.setPatientId(patientId);
        recordInside1.setRecordTime(convertToDate(LocalDateTime.of(2026, 2, 9, 14, 0)));
        recordInside1.setMedicalContent("区间内的第一条病历记录");
        recordInside1.setRecordingDoctor("Doctor B");
        allRecords.add(recordInside1);
        
        // 3. 区间内的第二条记录（应包含）
        MedicalRecord recordInside2 = new MedicalRecord();
        recordInside2.setPatientId(patientId);
        recordInside2.setRecordTime(convertToDate(LocalDateTime.of(2026, 2, 10, 10, 0)));
        recordInside2.setMedicalContent("区间内的第二条病历记录");
        recordInside2.setRecordingDoctor("Doctor C");
        allRecords.add(recordInside2);
        
        // 4. 区间后的记录（不应包含）
        MedicalRecord recordAfter = new MedicalRecord();
        recordAfter.setPatientId(patientId);
        recordAfter.setRecordTime(convertToDate(LocalDateTime.of(2026, 2, 10, 14, 0)));
        recordAfter.setMedicalContent("区间后的病历记录");
        recordAfter.setRecordingDoctor("Doctor D");
        allRecords.add(recordAfter);
        
        // Mock repository返回所有记录
        when(medicalRecordRepository.findByPatientIdAndDeletedOrderByRecordTimeDesc(patientId, 0))
            .thenReturn(allRecords);
        
        // Act - 通过反射调用private方法
        // 注意：此时方法还不存在，所以测试会失败（红阶段）
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "getCurrentMedicalRecordText", 
            String.class, LocalDateTime.class, LocalDateTime.class
        );
        method.setAccessible(true);
        
        String result = (String) method.invoke(timerPromptGenerator, patientId, lastRoundTime, currentTime);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.contains("区间内的第一条病历记录"), "应包含区间内的第一条记录");
        assertTrue(result.contains("区间内的第二条病历记录"), "应包含区间内的第二条记录");
        assertFalse(result.contains("区间前的病历记录"), "不应包含区间前的记录");
        assertFalse(result.contains("区间后的病历记录"), "不应包含区间后的记录");
        
        // 验证repository被调用
        verify(medicalRecordRepository).findByPatientIdAndDeletedOrderByRecordTimeDesc(patientId, 0);
    }

    /**
     * 红阶段测试用例2：区间内无记录时返回固定提示语
     * 
     * Given：区间内无病历记录
     * When：调用getCurrentMedicalRecordText方法
     * Then：返回固定提示语"本次查房时间段内无病程记录"
     */
    @Test
    @DisplayName("区间内无病历记录时应返回固定提示语")
    void testGetCurrentMedicalRecordText_NoRecordsInInterval_ReturnsPlaceholder() throws Exception {
        // Arrange
        String patientId = "TEST_PATIENT_002";
        LocalDateTime lastRoundTime = LocalDateTime.of(2026, 2, 9, 12, 0);
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 10, 12, 0);
        
        List<MedicalRecord> allRecords = new ArrayList<>();
        
        // 只有区间外的记录
        MedicalRecord recordBefore = new MedicalRecord();
        recordBefore.setPatientId(patientId);
        recordBefore.setRecordTime(convertToDate(LocalDateTime.of(2026, 2, 9, 10, 0)));
        recordBefore.setMedicalContent("区间前的病历记录");
        allRecords.add(recordBefore);
        
        when(medicalRecordRepository.findByPatientIdAndDeletedOrderByRecordTimeDesc(patientId, 0))
            .thenReturn(allRecords);
        
        // Act
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "getCurrentMedicalRecordText", 
            String.class, LocalDateTime.class, LocalDateTime.class
        );
        method.setAccessible(true);
        
        String result = (String) method.invoke(timerPromptGenerator, patientId, lastRoundTime, currentTime);
        
        // Assert
        assertNotNull(result);
        assertEquals("本次查房时间段内无病程记录", result);
        
        verify(medicalRecordRepository).findByPatientIdAndDeletedOrderByRecordTimeDesc(patientId, 0);
    }

    /**
     * 红阶段测试用例3：首次查房时使用默认策略
     * 
     * Given：lastRoundTime = null，存在多条病历记录
     * When：调用getCurrentMedicalRecordText方法
     * Then：根据约定策略（取最近3条记录）生成"本次病情记录"
     */
    @Test
    @DisplayName("首次查房时应使用最近3条记录的默认策略")
    void testGetCurrentMedicalRecordText_FirstWardRound_UsesRecentRecords() throws Exception {
        // Arrange
        String patientId = "TEST_PATIENT_003";
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 10, 12, 0);
        
        List<MedicalRecord> allRecords = new ArrayList<>();
        
        // 准备5条记录，但只应取最近的3条
        for (int i = 1; i <= 5; i++) {
            MedicalRecord record = new MedicalRecord();
            record.setPatientId(patientId);
            record.setRecordTime(convertToDate(currentTime.minusDays(i)));
            record.setMedicalContent("病历记录" + i);
            record.setRecordingDoctor("Doctor " + i);
            allRecords.add(record);
        }
        
        when(medicalRecordRepository.findByPatientIdAndDeletedOrderByRecordTimeDesc(patientId, 0))
            .thenReturn(allRecords);
        
        // Act
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "getCurrentMedicalRecordText", 
            String.class, LocalDateTime.class, LocalDateTime.class
        );
        method.setAccessible(true);
        
        String result = (String) method.invoke(timerPromptGenerator, patientId, null, currentTime);
        
        // Assert
        assertNotNull(result);
        // 应包含最近的3条记录
        assertTrue(result.contains("病历记录1"), "应包含最近的第1条记录");
        assertTrue(result.contains("病历记录2"), "应包含最近的第2条记录");
        assertTrue(result.contains("病历记录3"), "应包含最近的第3条记录");
        // 不应包含更早的记录
        assertFalse(result.contains("病历记录4"), "不应包含第4条记录");
        assertFalse(result.contains("病历记录5"), "不应包含第5条记录");
        
        verify(medicalRecordRepository).findByPatientIdAndDeletedOrderByRecordTimeDesc(patientId, 0);
    }

    /**
     * 辅助方法：将LocalDateTime转换为Date
     */
    private Date convertToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 补充测试用例4：测试边界时间点的处理
     * 
     * Given：存在记录时间正好等于lastRoundTime或currentTime
     * When：调用getCurrentMedicalRecordText方法
     * Then：这些边界时间点的记录应被包含在结果中（闭区间）
     */
    @Test
    @DisplayName("应正确处理边界时间点的记录")
    void testGetCurrentMedicalRecordText_BoundaryTimes_IncludesRecordsAtBoundaries() throws Exception {
        // Arrange
        String patientId = "TEST_PATIENT_004";
        LocalDateTime lastRoundTime = LocalDateTime.of(2026, 2, 9, 12, 0);
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 10, 12, 0);
        
        List<MedicalRecord> allRecords = new ArrayList<>();
        
        // 1. 记录时间正好等于lastRoundTime（应包含）
        MedicalRecord recordAtStart = new MedicalRecord();
        recordAtStart.setPatientId(patientId);
        recordAtStart.setRecordTime(convertToDate(lastRoundTime));
        recordAtStart.setMedicalContent("边界开始时间的记录");
        recordAtStart.setRecordingDoctor("Doctor A");
        allRecords.add(recordAtStart);
        
        // 2. 记录时间正好等于currentTime（应包含）
        MedicalRecord recordAtEnd = new MedicalRecord();
        recordAtEnd.setPatientId(patientId);
        recordAtEnd.setRecordTime(convertToDate(currentTime));
        recordAtEnd.setMedicalContent("边界结束时间的记录");
        recordAtEnd.setRecordingDoctor("Doctor B");
        allRecords.add(recordAtEnd);
        
        // 3. 区间外的记录
        MedicalRecord recordBefore = new MedicalRecord();
        recordBefore.setPatientId(patientId);
        recordBefore.setRecordTime(convertToDate(lastRoundTime.minusSeconds(1)));
        recordBefore.setMedicalContent("区间前的记录");
        recordBefore.setRecordingDoctor("Doctor C");
        allRecords.add(recordBefore);
        
        MedicalRecord recordAfter = new MedicalRecord();
        recordAfter.setPatientId(patientId);
        recordAfter.setRecordTime(convertToDate(currentTime.plusSeconds(1)));
        recordAfter.setMedicalContent("区间后的记录");
        recordAfter.setRecordingDoctor("Doctor D");
        allRecords.add(recordAfter);
        
        when(medicalRecordRepository.findByPatientIdAndDeletedOrderByRecordTimeDesc(patientId, 0))
            .thenReturn(allRecords);
        
        // Act
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "getCurrentMedicalRecordText", 
            String.class, LocalDateTime.class, LocalDateTime.class
        );
        method.setAccessible(true);
        
        String result = (String) method.invoke(timerPromptGenerator, patientId, lastRoundTime, currentTime);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.contains("边界开始时间的记录"), "应包含lastRoundTime时间点的记录");
        assertTrue(result.contains("边界结束时间的记录"), "应包含currentTime时间点的记录");
        assertFalse(result.contains("区间前的记录"), "不应包含区间前的记录");
        assertFalse(result.contains("区间后的记录"), "不应包含区间后的记录");
        
        verify(medicalRecordRepository).findByPatientIdAndDeletedOrderByRecordTimeDesc(patientId, 0);
    }
}
