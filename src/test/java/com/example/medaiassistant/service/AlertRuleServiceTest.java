package com.example.medaiassistant.service;

import com.example.medaiassistant.model.AlertRule;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.model.AlertTask;
import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.repository.AlertRuleRepository;
import com.example.medaiassistant.repository.PatientRepository;
import com.example.medaiassistant.repository.PromptRepository;
import com.example.medaiassistant.repository.AlertTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 告警规则服务层测试
 * 使用Mockito进行单元测试，不加载Spring上下文
 * 
 * @author Cline
 * @since 2025-12-04
 */
@ExtendWith(MockitoExtension.class)
class AlertRuleServiceTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PromptRepository promptRepository;

    @Mock
    private AlertTaskRepository alertTaskRepository;

    @InjectMocks
    private AlertRuleService alertRuleService;

    /**
     * 测试处理激活的时间规则 - 红阶段：测试会失败
     * 因为processActiveTimeRules方法需要数据库连接和真实数据
     */
    @Test
    void processActiveTimeRules_ShouldProcessActiveTimeRules() {
        // Arrange: 准备测试数据
        AlertRule timeRule = new AlertRule();
        timeRule.setRuleId(1);
        timeRule.setRuleName("测试时间规则");
        timeRule.setRuleType(AlertRule.RuleType.时间);
        timeRule.setIsActive(1);
        timeRule.setPriority(1);
        timeRule.setTriggerConditions("{\"offset_hours\": 24, \"not_exist\": \"测试记录\"}");
        timeRule.setAlertContent("测试告警内容");
        timeRule.setRequiredActions("[\"action1\", \"action2\"]");
        timeRule.setTargetRoles("[\"doctor\", \"nurse\"]");

        Patient patient = new Patient();
        patient.setPatientId("P001");
        patient.setName("测试患者");
        patient.setAdmissionTime(java.sql.Timestamp.valueOf(LocalDateTime.now().minusDays(2)));

        // 模拟Repository行为
        when(alertRuleRepository.findActiveTimeRulesOrderByPriority())
                .thenReturn(Collections.singletonList(timeRule));
        when(patientRepository.findByDepartmentAndAdmissionTimeBeforeOffset(
                anyString(), anyBoolean(), anyInt(), any()))
                .thenReturn(Collections.singletonList(patient));
        when(promptRepository.findByPatientIdAndPromptTemplateName(anyString(), anyString()))
                .thenReturn(Collections.emptyList()); // 模拟没有prompt记录
        when(alertTaskRepository.findByRuleIdAndPatientIdAndTaskStatus(anyInt(), anyString(), any()))
                .thenReturn(Collections.emptyList());
        when(alertTaskRepository.save(any(AlertTask.class))).thenAnswer(invocation -> {
            AlertTask task = invocation.getArgument(0);
            task.setTaskId(100); // 设置模拟ID
            return task;
        });

        // Act: 执行方法
        // 红阶段：这个方法可能会因为事务管理或其他依赖而失败
        assertDoesNotThrow(() -> alertRuleService.processActiveTimeRules(),
                "processActiveTimeRules方法不应抛出异常");

        // Assert: 验证交互
        verify(alertRuleRepository, times(1)).findActiveTimeRulesOrderByPriority();
        verify(patientRepository, times(1)).findByDepartmentAndAdmissionTimeBeforeOffset(
                anyString(), anyBoolean(), anyInt(), any());
        verify(alertTaskRepository, atLeastOnce()).save(any(AlertTask.class));
    }

    /**
     * 测试处理激活的状态规则 - 红阶段：测试会失败
     */
    @Test
    void processActiveStatusRules_ShouldProcessActiveStatusRules() {
        // Arrange: 准备测试数据
        AlertRule statusRule = new AlertRule();
        statusRule.setRuleId(2);
        statusRule.setRuleName("测试状态规则");
        statusRule.setRuleType(AlertRule.RuleType.状态);
        statusRule.setIsActive(1);
        statusRule.setPriority(1);
        statusRule.setTriggerConditions("{\"type\": \"状态\", \"patient_status\": \"病危\", \"cycle\": \"24h\", \"fixed_time\": \"08:00\"}");
        statusRule.setAlertContent("病危患者查房提醒");
        statusRule.setRequiredActions("[\"查房\", \"记录\"]");
        statusRule.setTargetRoles("[\"doctor\"]");

        Patient patient = new Patient();
        patient.setPatientId("P002");
        patient.setName("病危患者");
        patient.setStatus("病危");

        // 模拟Repository行为
        when(alertRuleRepository.findActiveStatusRulesOrderByPriority())
                .thenReturn(Collections.singletonList(statusRule));
        when(patientRepository.findByStatusAndIsInHospital(anyString(), anyBoolean()))
                .thenReturn(Collections.singletonList(patient));
        when(alertTaskRepository.findByRuleIdAndPatientIdAndTaskStatus(anyInt(), anyString(), any()))
                .thenReturn(Collections.emptyList());
        when(alertTaskRepository.save(any(AlertTask.class))).thenAnswer(invocation -> {
            AlertTask task = invocation.getArgument(0);
            task.setTaskId(101); // 设置模拟ID
            return task;
        });

        // Act: 执行方法
        // 红阶段：这个方法可能会因为事务管理或其他依赖而失败
        assertDoesNotThrow(() -> alertRuleService.processActiveStatusRules(),
                "processActiveStatusRules方法不应抛出异常");

        // Assert: 验证交互
        verify(alertRuleRepository, times(1)).findActiveStatusRulesOrderByPriority();
        verify(patientRepository, times(1)).findByStatusAndIsInHospital(anyString(), anyBoolean());
        verify(alertTaskRepository, atLeastOnce()).save(any(AlertTask.class));
    }

    /**
     * 测试获取激活的时间规则
     */
    @Test
    void getActiveTimeRulesOrderByPriority_ShouldReturnActiveTimeRules() {
        // Arrange
        AlertRule timeRule = new AlertRule();
        timeRule.setRuleId(1);
        timeRule.setRuleType(AlertRule.RuleType.时间);
        timeRule.setIsActive(1);
        timeRule.setPriority(1);

        when(alertRuleRepository.findActiveTimeRulesOrderByPriority())
                .thenReturn(Collections.singletonList(timeRule));

        // Act
        List<AlertRule> result = alertRuleService.getActiveTimeRulesOrderByPriority();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(AlertRule.RuleType.时间, result.get(0).getRuleType());
        assertEquals(1, result.get(0).getIsActive());
        verify(alertRuleRepository, times(1)).findActiveTimeRulesOrderByPriority();
    }

    /**
     * 测试获取激活的状态规则
     */
    @Test
    void getActiveStatusRulesOrderByPriority_ShouldReturnActiveStatusRules() {
        // Arrange
        AlertRule statusRule = new AlertRule();
        statusRule.setRuleId(2);
        statusRule.setRuleType(AlertRule.RuleType.状态);
        statusRule.setIsActive(1);
        statusRule.setPriority(1);

        when(alertRuleRepository.findActiveStatusRulesOrderByPriority())
                .thenReturn(Collections.singletonList(statusRule));

        // Act
        List<AlertRule> result = alertRuleService.getActiveStatusRulesOrderByPriority();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(AlertRule.RuleType.状态, result.get(0).getRuleType());
        assertEquals(1, result.get(0).getIsActive());
        verify(alertRuleRepository, times(1)).findActiveStatusRulesOrderByPriority();
    }

    /**
     * 测试处理时间规则 - JSON解析异常场景
     */
    @Test
    void processActiveTimeRules_ShouldHandleJsonParseException() {
        // Arrange: 准备测试数据，包含无效的JSON
        AlertRule invalidRule = new AlertRule();
        invalidRule.setRuleId(3);
        invalidRule.setRuleName("无效规则");
        invalidRule.setRuleType(AlertRule.RuleType.时间);
        invalidRule.setIsActive(1);
        invalidRule.setPriority(1);
        invalidRule.setTriggerConditions("无效的JSON字符串");

        when(alertRuleRepository.findActiveTimeRulesOrderByPriority())
                .thenReturn(Collections.singletonList(invalidRule));

        // Act & Assert: 验证方法不会抛出异常
        assertDoesNotThrow(() -> alertRuleService.processActiveTimeRules(),
                "JSON解析异常不应导致方法抛出异常");

        // Assert: 验证交互
        verify(alertRuleRepository, times(1)).findActiveTimeRulesOrderByPriority();
        verify(patientRepository, never()).findByDepartmentAndAdmissionTimeBeforeOffset(
                anyString(), anyBoolean(), anyInt(), any());
    }

    /**
     * 测试处理时间规则 - 规则不包含offset_hours条件
     */
    @Test
    void processActiveTimeRules_ShouldSkipRulesWithoutOffsetHours() {
        // Arrange: 准备测试数据，不包含offset_hours条件
        AlertRule ruleWithoutOffset = new AlertRule();
        ruleWithoutOffset.setRuleId(4);
        ruleWithoutOffset.setRuleName("无offset规则");
        ruleWithoutOffset.setRuleType(AlertRule.RuleType.时间);
        ruleWithoutOffset.setIsActive(1);
        ruleWithoutOffset.setPriority(1);
        ruleWithoutOffset.setTriggerConditions("{\"other_condition\": \"value\"}");

        when(alertRuleRepository.findActiveTimeRulesOrderByPriority())
                .thenReturn(Collections.singletonList(ruleWithoutOffset));

        // Act
        assertDoesNotThrow(() -> alertRuleService.processActiveTimeRules(),
                "规则不包含offset_hours条件不应导致异常");

        // Assert: 验证交互
        verify(alertRuleRepository, times(1)).findActiveTimeRulesOrderByPriority();
        verify(patientRepository, never()).findByDepartmentAndAdmissionTimeBeforeOffset(
                anyString(), anyBoolean(), anyInt(), any());
    }

    /**
     * 测试处理时间规则 - 规则包含cycle条件
     */
    @Test
    void processActiveTimeRules_ShouldSkipRulesWithCycle() {
        // Arrange: 准备测试数据，包含cycle条件
        AlertRule ruleWithCycle = new AlertRule();
        ruleWithCycle.setRuleId(5);
        ruleWithCycle.setRuleName("有cycle规则");
        ruleWithCycle.setRuleType(AlertRule.RuleType.时间);
        ruleWithCycle.setIsActive(1);
        ruleWithCycle.setPriority(1);
        ruleWithCycle.setTriggerConditions("{\"offset_hours\": 24, \"cycle\": \"24h\"}");

        when(alertRuleRepository.findActiveTimeRulesOrderByPriority())
                .thenReturn(Collections.singletonList(ruleWithCycle));

        // Act
        assertDoesNotThrow(() -> alertRuleService.processActiveTimeRules(),
                "规则包含cycle条件不应导致异常");

        // Assert: 验证交互
        verify(alertRuleRepository, times(1)).findActiveTimeRulesOrderByPriority();
        verify(patientRepository, never()).findByDepartmentAndAdmissionTimeBeforeOffset(
                anyString(), anyBoolean(), anyInt(), any());
    }

    /**
     * 测试处理时间规则 - 患者已有prompt记录，不应创建任务
     */
    @Test
    void processActiveTimeRules_ShouldNotCreateTaskWhenPromptExists() {
        // Arrange: 准备测试数据
        AlertRule timeRule = new AlertRule();
        timeRule.setRuleId(6);
        timeRule.setRuleName("测试时间规则");
        timeRule.setRuleType(AlertRule.RuleType.时间);
        timeRule.setIsActive(1);
        timeRule.setPriority(1);
        timeRule.setTriggerConditions("{\"offset_hours\": 24, \"not_exist\": \"测试记录\"}");
        timeRule.setAlertContent("测试告警内容");
        timeRule.setRequiredActions("[\"action1\", \"action2\"]");
        timeRule.setTargetRoles("[\"doctor\", \"nurse\"]");

        Patient patient = new Patient();
        patient.setPatientId("P003");
        patient.setName("测试患者");
        patient.setAdmissionTime(java.sql.Timestamp.valueOf(LocalDateTime.now().minusDays(2)));

        // 模拟Repository行为 - 患者已有prompt记录
        when(alertRuleRepository.findActiveTimeRulesOrderByPriority())
                .thenReturn(Collections.singletonList(timeRule));
        when(patientRepository.findByDepartmentAndAdmissionTimeBeforeOffset(
                anyString(), anyBoolean(), anyInt(), any()))
                .thenReturn(Collections.singletonList(patient));
        when(promptRepository.findByPatientIdAndPromptTemplateName(anyString(), anyString()))
                .thenReturn(Collections.singletonList(new Prompt())); // 模拟有prompt记录

        // Act
        assertDoesNotThrow(() -> alertRuleService.processActiveTimeRules(),
                "患者已有prompt记录不应导致异常");

        // Assert: 验证交互
        verify(alertRuleRepository, times(1)).findActiveTimeRulesOrderByPriority();
        verify(patientRepository, times(1)).findByDepartmentAndAdmissionTimeBeforeOffset(
                anyString(), anyBoolean(), anyInt(), any());
        verify(promptRepository, times(1)).findByPatientIdAndPromptTemplateName(anyString(), anyString());
        verify(alertTaskRepository, never()).save(any(AlertTask.class)); // 不应保存任务
    }
}
