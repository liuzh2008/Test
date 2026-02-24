package com.example.medaiassistant.service;

import com.example.medaiassistant.model.LongTermOrder;
import com.example.medaiassistant.repository.LongTermOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * TimerPromptGenerator 查房记录任务5测试
 * 测试医嘱变更区间筛选与'近期医嘱变更'生成功能
 * 
 * @author TDD
 * @version 1.0
 * @since 2026-02-09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TimerPromptGenerator 查房记录任务5 - 医嘱变更区间筛选")
class TimerPromptGeneratorWardRoundTask5Test {

    @Mock
    private LongTermOrderRepository longTermOrderRepository;
    
    private TimerPromptGenerator timerPromptGenerator;

    @BeforeEach
    void setUp() {
        // 构造TimerPromptGenerator实例（传null给不需要的依赖）
        // 构造函数参数：taskScheduler, alertRuleService, patientStatusUpdateService,
        // patientRepository, promptRepository, promptResultRepository,
        // medicalRecordRepository, labResultRepository, examinationResultRepository,
        // longTermOrderRepository, serverConfigService, restTemplate, apiProperties, schedulingProperties,
        // emrRecordRepository, emrContentRepository, surgeryRepository,
        // promptGenerationLogService, promptsTaskUpdater
        timerPromptGenerator = new TimerPromptGenerator(
            null, null, null,
            null, null, null,
            null, null, null,
            longTermOrderRepository,
            null, null, null, null, null, null, null, null, null
        );
    }

    @Test
    @DisplayName("医嘱变更应只包含时间区间内的记录")
    void testGetTreatmentChangesSinceLastRound_IncludesOrdersWithinInterval() throws Exception {
        // Given - 准备测试数据
        String patientId = "TEST_PATIENT_001";
        LocalDateTime lastRoundTime = LocalDateTime.of(2026, 2, 8, 10, 0);
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 10, 0);
        
        List<LongTermOrder> allOrders = new ArrayList<>();
        
        // 区间前的长期医嘱（不应包含）
        LongTermOrder longTermBeforeInterval = new LongTermOrder();
        longTermBeforeInterval.setOrderName("阿司匹林-区间前");
        longTermBeforeInterval.setOrderDate(Timestamp.valueOf(lastRoundTime.minusHours(1)));
        longTermBeforeInterval.setRepeatIndicator(1); // 长期医嘱
        longTermBeforeInterval.setDosage("100");
        longTermBeforeInterval.setUnit("mg");
        longTermBeforeInterval.setFrequency("qd");
        longTermBeforeInterval.setRoute("口服");
        allOrders.add(longTermBeforeInterval);
        
        // 区间内的长期医嘱（应包含）
        LongTermOrder longTermWithinInterval = new LongTermOrder();
        longTermWithinInterval.setOrderName("头孢克肟-区间内");
        longTermWithinInterval.setOrderDate(Timestamp.valueOf(lastRoundTime.plusHours(12)));
        longTermWithinInterval.setRepeatIndicator(1); // 长期医嘱
        longTermWithinInterval.setDosage("500");
        longTermWithinInterval.setUnit("mg");
        longTermWithinInterval.setFrequency("bid");
        longTermWithinInterval.setRoute("口服");
        allOrders.add(longTermWithinInterval);
        
        // 区间内的临时医嘱（应包含）
        LongTermOrder temporaryWithinInterval = new LongTermOrder();
        temporaryWithinInterval.setOrderName("血常规-区间内");
        temporaryWithinInterval.setOrderDate(Timestamp.valueOf(lastRoundTime.plusHours(18)));
        temporaryWithinInterval.setRepeatIndicator(0); // 临时医嘱
        allOrders.add(temporaryWithinInterval);
        
        // 区间后的医嘱（不应包含）
        LongTermOrder afterInterval = new LongTermOrder();
        afterInterval.setOrderName("胰岛素-区间后");
        afterInterval.setOrderDate(Timestamp.valueOf(currentTime.plusHours(1)));
        afterInterval.setRepeatIndicator(1);
        allOrders.add(afterInterval);
        
        // Mock repository返回所有医嘱
        when(longTermOrderRepository.findByPatientIdAndRepeatIndicatorOrderByOrderDateAsc(anyString(), anyInt()))
            .thenReturn(allOrders);
        
        // When - 通过反射调用private方法
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "getTreatmentChangesSinceLastRound", 
            String.class, LocalDateTime.class, LocalDateTime.class
        );
        method.setAccessible(true);
        String result = (String) method.invoke(timerPromptGenerator, patientId, lastRoundTime, currentTime);
        
        // Then - 验证只包含区间内的记录
        assertTrue(result.contains("头孢克肟-区间内"), "应包含区间内的长期医嘱");
        assertTrue(result.contains("血常规-区间内"), "应包含区间内的临时医嘱");
        assertFalse(result.contains("区间前"), "不应包含区间前的医嘱");
        assertFalse(result.contains("区间后"), "不应包含区间后的医嘱");
        
        // 验证包含关键信息
        assertTrue(result.contains("500"), "应包含剂量信息");
        assertTrue(result.contains("bid"), "应包含频次信息");
        assertTrue(result.contains("口服"), "应包含给药途径信息");
    }

    @Test
    @DisplayName("区间内无医嘱变更时应返回固定提示语")
    void testGetTreatmentChangesSinceLastRound_NoOrdersInInterval_ReturnsNoChangeMessage() throws Exception {
        // Given - 准备测试数据：区间外的医嘱
        String patientId = "TEST_PATIENT_002";
        LocalDateTime lastRoundTime = LocalDateTime.of(2026, 2, 8, 10, 0);
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 10, 0);
        
        List<LongTermOrder> allOrders = new ArrayList<>();
        
        // 只有区间前的医嘱
        LongTermOrder beforeInterval = new LongTermOrder();
        beforeInterval.setOrderName("旧医嘱");
        beforeInterval.setOrderDate(Timestamp.valueOf(lastRoundTime.minusDays(1)));
        beforeInterval.setRepeatIndicator(1);
        allOrders.add(beforeInterval);
        
        when(longTermOrderRepository.findByPatientIdAndRepeatIndicatorOrderByOrderDateAsc(anyString(), anyInt()))
            .thenReturn(allOrders);
        
        // When - 调用方法
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "getTreatmentChangesSinceLastRound", 
            String.class, LocalDateTime.class, LocalDateTime.class
        );
        method.setAccessible(true);
        String result = (String) method.invoke(timerPromptGenerator, patientId, lastRoundTime, currentTime);
        
        // Then - 验证返回固定提示语
        assertEquals("自上次查房以来无新的治疗医嘱调整", result);
    }

    @Test
    @DisplayName("首次查房应使用默认策略获取最近医嘱")
    void testGetTreatmentChangesSinceLastRound_FirstWardRound_UsesRecentOrders() throws Exception {
        // Given - 首次查房（lastRoundTime为null）
        String patientId = "TEST_PATIENT_003";
        LocalDateTime lastRoundTime = null;
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 10, 0);
        
        List<LongTermOrder> allOrders = new ArrayList<>();
        
        // 3天前的医嘱（不应包含）
        LongTermOrder oldOrder = new LongTermOrder();
        oldOrder.setOrderName("旧医嘱-3天前");
        oldOrder.setOrderDate(Timestamp.valueOf(currentTime.minusDays(3)));
        oldOrder.setRepeatIndicator(1);
        allOrders.add(oldOrder);
        
        // 1天前的医嘱（应包含）
        LongTermOrder recentOrder = new LongTermOrder();
        recentOrder.setOrderName("新医嘱-1天前");
        recentOrder.setOrderDate(Timestamp.valueOf(currentTime.minusDays(1)));
        recentOrder.setRepeatIndicator(1);
        recentOrder.setDosage("200");
        recentOrder.setUnit("mg");
        allOrders.add(recentOrder);
        
        when(longTermOrderRepository.findByPatientIdAndRepeatIndicatorOrderByOrderDateAsc(anyString(), anyInt()))
            .thenReturn(allOrders);
        
        // When - 首次查房调用
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "getTreatmentChangesSinceLastRound", 
            String.class, LocalDateTime.class, LocalDateTime.class
        );
        method.setAccessible(true);
        String result = (String) method.invoke(timerPromptGenerator, patientId, lastRoundTime, currentTime);
        
        // Then - 验证使用最近2天的医嘱
        assertTrue(result.contains("新医嘱-1天前"), "首次查房应包含最近2天内的医嘱");
        assertFalse(result.contains("3天前"), "首次查房不应包含2天前的医嘱");
    }

    @Test
    @DisplayName("医嘱边界时间点应被包含（闭区间）")
    void testGetTreatmentChangesSinceLastRound_BoundaryTimePoints_ShouldBeIncluded() throws Exception {
        // Given - 准备边界数据
        String patientId = "TEST_PATIENT_004";
        LocalDateTime lastRoundTime = LocalDateTime.of(2026, 2, 8, 10, 0, 0);
        LocalDateTime currentTime = LocalDateTime.of(2026, 2, 9, 10, 0, 0);
        
        List<LongTermOrder> allOrders = new ArrayList<>();
        
        // 区间左边界（应包含）
        LongTermOrder leftBoundary = new LongTermOrder();
        leftBoundary.setOrderName("边界医嘱-左边界");
        leftBoundary.setOrderDate(Timestamp.valueOf(lastRoundTime)); // 精确等于lastRoundTime
        leftBoundary.setRepeatIndicator(1);
        leftBoundary.setDosage("100");
        leftBoundary.setUnit("mg");
        allOrders.add(leftBoundary);
        
        // 区间右边界（应包含）
        LongTermOrder rightBoundary = new LongTermOrder();
        rightBoundary.setOrderName("边界医嘱-右边界");
        rightBoundary.setOrderDate(Timestamp.valueOf(currentTime)); // 精确等于currentTime
        rightBoundary.setRepeatIndicator(0);
        rightBoundary.setDosage("200");
        rightBoundary.setUnit("mg");
        allOrders.add(rightBoundary);
        
        // 区间外的记录（不应包含）
        LongTermOrder outsideBefore = new LongTermOrder();
        outsideBefore.setOrderName("区间外-1秒前");
        outsideBefore.setOrderDate(Timestamp.valueOf(lastRoundTime.minusSeconds(1)));
        outsideBefore.setRepeatIndicator(1);
        allOrders.add(outsideBefore);
        
        LongTermOrder outsideAfter = new LongTermOrder();
        outsideAfter.setOrderName("区间外-1秒后");
        outsideAfter.setOrderDate(Timestamp.valueOf(currentTime.plusSeconds(1)));
        outsideAfter.setRepeatIndicator(1);
        allOrders.add(outsideAfter);
        
        when(longTermOrderRepository.findByPatientIdAndRepeatIndicatorOrderByOrderDateAsc(anyString(), anyInt()))
            .thenReturn(allOrders);
        
        // When - 调用方法
        Method method = TimerPromptGenerator.class.getDeclaredMethod(
            "getTreatmentChangesSinceLastRound", 
            String.class, LocalDateTime.class, LocalDateTime.class
        );
        method.setAccessible(true);
        String result = (String) method.invoke(timerPromptGenerator, patientId, lastRoundTime, currentTime);
        
        // Then - 验证边界点被包含，区间外不被包含
        assertTrue(result.contains("左边界"), "应包含左边界时间点的医嘱");
        assertTrue(result.contains("右边界"), "应包含右边界时间点的医嘱");
        assertFalse(result.contains("1秒前"), "不应包含边界外的医嘱");
        assertFalse(result.contains("1秒后"), "不应包含边界外的医嘱");
    }
}
