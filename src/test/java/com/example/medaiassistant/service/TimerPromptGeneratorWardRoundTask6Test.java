package com.example.medaiassistant.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.medaiassistant.model.*;
import com.example.medaiassistant.repository.*;

/**
 * TDD测试 - 任务6: buildWardRoundObjectiveContent() 结构化拼装
 * 
 * 测试覆盖：
 * 1. 验证所有块的存在性
 * 2. 验证块的固定顺序
 * 3. 验证部分块为空时的处理
 * 
 * @author TDD
 * @version 1.0 - 任务6实施
 */
@ExtendWith(MockitoExtension.class)
public class TimerPromptGeneratorWardRoundTask6Test {

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
    private LongTermOrderRepository longTermOrderRepository;

    private TimerPromptGenerator timerPromptGenerator;

    @BeforeEach
    void setUp() {
        // 使用19个参数构造TimerPromptGenerator（其他参数传null，只需要用到的repository）
        timerPromptGenerator = new TimerPromptGenerator(
            null, null, null,
            patientRepository, promptRepository, promptResultRepository,
            medicalRecordRepository, labResultRepository, examinationResultRepository,
            longTermOrderRepository,
            null, null, null, null, null, null, null, null, null
        );
    }

    /**
     * 测试用例1: 验证所有块的存在性
     * 
     * Given: Mock返回正常数据
     * When: 调用buildWardRoundObjectiveContent
     * Then: 返回字符串包含所有7个标准块标题
     */
    @Test
    @DisplayName("查房记录应包含所有标准块标题")
    void testBuildWardRoundObjectiveContent_ContainsAllSections() throws Exception {
        // Given - 准备测试数据
        String patientId = "TEST_PATIENT_001";
        String templateName = "中午查房记录";
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 12, 0);
        
        // Mock Patient数据
        Patient patient = new Patient();
        patient.setPatientId(patientId);
        patient.setGender("1");
        patient.setDateOfBirth(new Date());
        patient.setAdmissionTime(new Date());
        patient.setImportantInformation("高血压病，糖尿病");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        
        // Mock无历史查房记录
        when(promptRepository.findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(anyString(), anyString()))
            .thenReturn(Optional.empty());
        
        // Mock空的病历记录
        when(medicalRecordRepository.findByPatientIdAndDeletedOrderByRecordTimeDesc(anyString(), anyInt()))
            .thenReturn(new ArrayList<>());
        
        // Mock空的化验结果
        when(labResultRepository.findByPatientId(anyString()))
            .thenReturn(new ArrayList<>());
        
        // Mock空的检查结果
        when(examinationResultRepository.findByPatientId(anyString()))
            .thenReturn(new ArrayList<>());
        
        // Mock空的医嘱
        when(longTermOrderRepository.findByPatientIdAndRepeatIndicatorOrderByOrderDateAsc(anyString(), anyInt()))
            .thenReturn(new ArrayList<>());
        
        // When - 通过反射调用private方法
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "buildWardRoundObjectiveContent", 
            String.class, String.class, LocalDateTime.class
        );
        method.setAccessible(true);
        String result = (String) method.invoke(timerPromptGenerator, patientId, templateName, currentTime);
        
        // Then - 验证包含所有标准块标题
        assertTrue(result.contains("【病人基本信息】"), "应包含病人基本信息块标题");
        assertTrue(result.contains("【上次查房记录摘要】"), "应包含上次查房记录摘要块标题");
        assertTrue(result.contains("【本次病情记录（含主诉与查体原文）】"), "应包含本次病情记录块标题");
        assertTrue(result.contains("【自上次查房以来的化验结果】"), "应包含化验结果块标题");
        assertTrue(result.contains("【自上次查房以来的检查结果】"), "应包含检查结果块标题");
        assertTrue(result.contains("【目前诊断】"), "应包含目前诊断块标题");
        assertTrue(result.contains("【最近医嘱和治疗措施】"), "应包含医嘱和治疗措施块标题");
    }

    /**
     * 测试用例2: 验证块的固定顺序
     * 
     * Given: Mock返回正常数据
     * When: 调用buildWardRoundObjectiveContent
     * Then: 各块按预定义顺序出现
     */
    @Test
    @DisplayName("查房记录各块应按固定顺序排列")
    void testBuildWardRoundObjectiveContent_SectionOrderIsStable() throws Exception {
        // Given - 准备测试数据
        String patientId = "TEST_PATIENT_002";
        String templateName = "中午查房记录";
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 12, 0);
        
        // Mock Patient数据
        Patient patient = new Patient();
        patient.setPatientId(patientId);
        patient.setGender("2");
        patient.setDateOfBirth(new Date());
        patient.setAdmissionTime(new Date());
        patient.setImportantInformation("测试诊断");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        
        // Mock其他repository返回空数据
        when(promptRepository.findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(anyString(), anyString()))
            .thenReturn(Optional.empty());
        when(medicalRecordRepository.findByPatientIdAndDeletedOrderByRecordTimeDesc(anyString(), anyInt()))
            .thenReturn(new ArrayList<>());
                when(labResultRepository.findByPatientId(anyString()))
            .thenReturn(new ArrayList<>());
                when(examinationResultRepository.findByPatientId(anyString()))
            .thenReturn(new ArrayList<>());
        when(longTermOrderRepository.findByPatientIdAndRepeatIndicatorOrderByOrderDateAsc(anyString(), anyInt()))
            .thenReturn(new ArrayList<>());
        
        // When - 调用方法
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "buildWardRoundObjectiveContent", 
            String.class, String.class, LocalDateTime.class
        );
        method.setAccessible(true);
        String result = (String) method.invoke(timerPromptGenerator, patientId, templateName, currentTime);
        
        // Then - 验证顺序（通过indexOf比较）
        int posBasicInfo = result.indexOf("【病人基本信息】");
        int posLastRound = result.indexOf("【上次查房记录摘要】");
        int posCurrent = result.indexOf("【本次病情记录（含主诉与查体原文）】");
        int posLab = result.indexOf("【自上次查房以来的化验结果】");
        int posExam = result.indexOf("【自上次查房以来的检查结果】");
        int posDiagnosis = result.indexOf("【目前诊断】");
        int posTreatment = result.indexOf("【最近医嘱和治疗措施】");
        
        assertTrue(posBasicInfo < posLastRound, "病人基本信息应在上次查房记录之前");
        assertTrue(posLastRound < posCurrent, "上次查房记录应在本次病情记录之前");
        assertTrue(posCurrent < posLab, "本次病情记录应在化验结果之前");
        assertTrue(posLab < posExam, "化验结果应在检查结果之前");
        assertTrue(posExam < posDiagnosis, "检查结果应在目前诊断之前");
        assertTrue(posDiagnosis < posTreatment, "目前诊断应在医嘱和治疗措施之前");
    }

    /**
     * 测试用例3: 验证部分块为空时的处理
     * 
     * Given: Patient不存在
     * When: 调用buildWardRoundObjectiveContent
     * Then: 对应块仍存在标题，内容为默认占位符，不抛异常
     */
    @Test
    @DisplayName("部分块为空时应优雅处理不抛异常")
    void testBuildWardRoundObjectiveContent_HandlesNullSubSectionsGracefully() throws Exception {
        // Given - 准备测试数据（Patient不存在）
        String patientId = "TEST_PATIENT_003";
        String templateName = "中午查房记录";
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 12, 0);
        
        // Mock Patient不存在
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());
        
        // Mock其他repository返回空数据
        when(promptRepository.findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(anyString(), anyString()))
            .thenReturn(Optional.empty());
        when(medicalRecordRepository.findByPatientIdAndDeletedOrderByRecordTimeDesc(anyString(), anyInt()))
            .thenReturn(new ArrayList<>());
        when(labResultRepository.findByPatientId(anyString()))
            .thenReturn(new ArrayList<>());
        when(examinationResultRepository.findByPatientId(anyString()))
            .thenReturn(new ArrayList<>());
        when(longTermOrderRepository.findByPatientIdAndRepeatIndicatorOrderByOrderDateAsc(anyString(), anyInt()))
            .thenReturn(new ArrayList<>());
        
        // When - 调用方法（应不抛异常）
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "buildWardRoundObjectiveContent", 
            String.class, String.class, LocalDateTime.class
        );
        method.setAccessible(true);
        
        String result = assertDoesNotThrow(() -> {
            try {
                return (String) method.invoke(timerPromptGenerator, patientId, templateName, currentTime);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "即使部分块为空也不应抛异常");
        
        // Then - 验证仍包含所有标题，且空块有默认占位符
        assertTrue(result.contains("【病人基本信息】"), "空块也应包含标题");
        assertTrue(result.contains("【上次查房记录摘要】"), "空块也应包含标题");
        assertTrue(result.contains("【本次病情记录（含主诉与查体原文）】"), "空块也应包含标题");
        assertTrue(result.contains("【自上次查房以来的化验结果】"), "空块也应包含标题");
        assertTrue(result.contains("【自上次查房以来的检查结果】"), "空块也应包含标题");
        assertTrue(result.contains("【目前诊断】"), "空块也应包含标题");
        assertTrue(result.contains("【最近医嘱和治疗措施】"), "空块也应包含标题");
        
        // 验证有默认占位符文本
        assertTrue(result.contains("未找到患者信息") || result.contains("无基本信息"), "应有默认占位符");
        assertTrue(result.contains("首次查房") || result.contains("无上次查房记录"), "应有默认占位符");
        assertTrue(result.contains("无病程记录") || result.contains("本次查房时间段内无病程记录"), "应有默认占位符");
        
        // 验证返回结果不为空
        assertNotNull(result, "返回结果不应为null");
        assertFalse(result.isEmpty(), "返回结果不应为空字符串");
    }
}
