package com.example.medaiassistant.service.monitoring;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DRGs监控服务
 * 负责监控DRGs分析过程中的关键指标
 * 
 * 主要功能：
 * 1. 快照生成指标监控
 * 2. Prompt保存指标监控
 * 3. 用户决策指标监控
 * 4. 盈亏计算指标监控
 * 5. 监控指标统计和查询
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-13
 */
@Service
public class DrgMonitoringService {

    // 快照生成指标
    private final AtomicLong snapshotGenerationTotal = new AtomicLong(0);
    private final AtomicLong snapshotGenerationSuccess = new AtomicLong(0);
    private final AtomicLong snapshotGenerationFailure = new AtomicLong(0);
    private final AtomicLong snapshotGenerationTotalTime = new AtomicLong(0);

    // Prompt保存指标
    private final AtomicLong promptSaveTotal = new AtomicLong(0);
    private final AtomicLong promptSaveSuccess = new AtomicLong(0);
    private final AtomicLong promptSaveFailure = new AtomicLong(0);
    private final AtomicLong promptSaveTotalTime = new AtomicLong(0);

    // 用户决策指标
    private final AtomicLong userDecisionTotal = new AtomicLong(0);
    private final AtomicLong userDecisionSuccess = new AtomicLong(0);
    private final AtomicLong userDecisionFailure = new AtomicLong(0);
    private final AtomicLong userDecisionTotalTime = new AtomicLong(0);

    // 盈亏计算指标
    private final AtomicLong profitLossCalculationTotal = new AtomicLong(0);
    private final AtomicLong profitLossCalculationSuccess = new AtomicLong(0);
    private final AtomicLong profitLossCalculationFailure = new AtomicLong(0);
    private final AtomicLong profitLossCalculationTotalTime = new AtomicLong(0);

    /**
     * 记录快照生成指标
     * 
     * @param durationMillis 生成耗时（毫秒）
     * @param success 是否成功
     */
    public void recordSnapshotGeneration(long durationMillis, boolean success) {
        snapshotGenerationTotal.incrementAndGet();
        snapshotGenerationTotalTime.addAndGet(durationMillis);
        
        if (success) {
            snapshotGenerationSuccess.incrementAndGet();
        } else {
            snapshotGenerationFailure.incrementAndGet();
        }
    }

    /**
     * 记录Prompt保存指标
     * 
     * @param durationMillis 保存耗时（毫秒）
     * @param success 是否成功
     */
    public void recordPromptSave(long durationMillis, boolean success) {
        promptSaveTotal.incrementAndGet();
        promptSaveTotalTime.addAndGet(durationMillis);
        
        if (success) {
            promptSaveSuccess.incrementAndGet();
        } else {
            promptSaveFailure.incrementAndGet();
        }
    }

    /**
     * 记录用户决策指标
     * 
     * @param durationMillis 决策耗时（毫秒）
     * @param success 是否成功
     */
    public void recordUserDecision(long durationMillis, boolean success) {
        userDecisionTotal.incrementAndGet();
        userDecisionTotalTime.addAndGet(durationMillis);
        
        if (success) {
            userDecisionSuccess.incrementAndGet();
        } else {
            userDecisionFailure.incrementAndGet();
        }
    }

    /**
     * 记录盈亏计算指标
     * 
     * @param durationMillis 计算耗时（毫秒）
     * @param success 是否成功
     */
    public void recordProfitLossCalculation(long durationMillis, boolean success) {
        profitLossCalculationTotal.incrementAndGet();
        profitLossCalculationTotalTime.addAndGet(durationMillis);
        
        if (success) {
            profitLossCalculationSuccess.incrementAndGet();
        } else {
            profitLossCalculationFailure.incrementAndGet();
        }
    }

    /**
     * 获取监控指标
     * 
     * @return 监控指标数据
     */
    public Map<String, Object> getMonitoringMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>();
        
        // 快照生成指标
        Map<String, Object> snapshotMetrics = new ConcurrentHashMap<>();
        snapshotMetrics.put("total", snapshotGenerationTotal.get());
        snapshotMetrics.put("success", snapshotGenerationSuccess.get());
        snapshotMetrics.put("failure", snapshotGenerationFailure.get());
        snapshotMetrics.put("successRate", calculateSuccessRate(snapshotGenerationTotal.get(), snapshotGenerationSuccess.get()));
        snapshotMetrics.put("averageDuration", calculateAverageDuration(snapshotGenerationTotal.get(), snapshotGenerationTotalTime.get()));
        metrics.put("snapshotGeneration", snapshotMetrics);
        
        // Prompt保存指标
        Map<String, Object> promptMetrics = new ConcurrentHashMap<>();
        promptMetrics.put("total", promptSaveTotal.get());
        promptMetrics.put("success", promptSaveSuccess.get());
        promptMetrics.put("failure", promptSaveFailure.get());
        promptMetrics.put("successRate", calculateSuccessRate(promptSaveTotal.get(), promptSaveSuccess.get()));
        promptMetrics.put("averageDuration", calculateAverageDuration(promptSaveTotal.get(), promptSaveTotalTime.get()));
        metrics.put("promptSave", promptMetrics);
        
        // 用户决策指标
        Map<String, Object> decisionMetrics = new ConcurrentHashMap<>();
        decisionMetrics.put("total", userDecisionTotal.get());
        decisionMetrics.put("success", userDecisionSuccess.get());
        decisionMetrics.put("failure", userDecisionFailure.get());
        decisionMetrics.put("successRate", calculateSuccessRate(userDecisionTotal.get(), userDecisionSuccess.get()));
        decisionMetrics.put("averageDuration", calculateAverageDuration(userDecisionTotal.get(), userDecisionTotalTime.get()));
        metrics.put("userDecision", decisionMetrics);
        
        // 盈亏计算指标
        Map<String, Object> calculationMetrics = new ConcurrentHashMap<>();
        calculationMetrics.put("total", profitLossCalculationTotal.get());
        calculationMetrics.put("success", profitLossCalculationSuccess.get());
        calculationMetrics.put("failure", profitLossCalculationFailure.get());
        calculationMetrics.put("successRate", calculateSuccessRate(profitLossCalculationTotal.get(), profitLossCalculationSuccess.get()));
        calculationMetrics.put("averageDuration", calculateAverageDuration(profitLossCalculationTotal.get(), profitLossCalculationTotalTime.get()));
        metrics.put("profitLossCalculation", calculationMetrics);
        
        return metrics;
    }

    /**
     * 计算成功率
     * 
     * @param total 总次数
     * @param success 成功次数
     * @return 成功率（百分比）
     */
    private double calculateSuccessRate(long total, long success) {
        if (total == 0) {
            return 0.0;
        }
        return (double) success / total * 100;
    }

    /**
     * 计算平均耗时
     * 
     * @param total 总次数
     * @param totalTime 总耗时
     * @return 平均耗时（毫秒）
     */
    private double calculateAverageDuration(long total, long totalTime) {
        if (total == 0) {
            return 0.0;
        }
        return (double) totalTime / total;
    }

    /**
     * 获取快照生成指标
     * 
     * @return 快照生成指标数据
     */
    public Map<String, Object> getSnapshotGenerationMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>();
        metrics.put("total", snapshotGenerationTotal.get());
        metrics.put("success", snapshotGenerationSuccess.get());
        metrics.put("failure", snapshotGenerationFailure.get());
        metrics.put("successRate", calculateSuccessRate(snapshotGenerationTotal.get(), snapshotGenerationSuccess.get()));
        metrics.put("averageDuration", calculateAverageDuration(snapshotGenerationTotal.get(), snapshotGenerationTotalTime.get()));
        return metrics;
    }

    /**
     * 获取Prompt保存指标
     * 
     * @return Prompt保存指标数据
     */
    public Map<String, Object> getPromptSaveMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>();
        metrics.put("total", promptSaveTotal.get());
        metrics.put("success", promptSaveSuccess.get());
        metrics.put("failure", promptSaveFailure.get());
        metrics.put("successRate", calculateSuccessRate(promptSaveTotal.get(), promptSaveSuccess.get()));
        metrics.put("averageDuration", calculateAverageDuration(promptSaveTotal.get(), promptSaveTotalTime.get()));
        return metrics;
    }

    /**
     * 获取用户决策指标
     * 
     * @return 用户决策指标数据
     */
    public Map<String, Object> getUserDecisionMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>();
        metrics.put("total", userDecisionTotal.get());
        metrics.put("success", userDecisionSuccess.get());
        metrics.put("failure", userDecisionFailure.get());
        metrics.put("successRate", calculateSuccessRate(userDecisionTotal.get(), userDecisionSuccess.get()));
        metrics.put("averageDuration", calculateAverageDuration(userDecisionTotal.get(), userDecisionTotalTime.get()));
        return metrics;
    }

    /**
     * 获取盈亏计算指标
     * 
     * @return 盈亏计算指标数据
     */
    public Map<String, Object> getProfitLossCalculationMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>();
        metrics.put("total", profitLossCalculationTotal.get());
        metrics.put("success", profitLossCalculationSuccess.get());
        metrics.put("failure", profitLossCalculationFailure.get());
        metrics.put("successRate", calculateSuccessRate(profitLossCalculationTotal.get(), profitLossCalculationSuccess.get()));
        metrics.put("averageDuration", calculateAverageDuration(profitLossCalculationTotal.get(), profitLossCalculationTotalTime.get()));
        return metrics;
    }

    /**
     * 重置所有监控指标
     */
    public void resetAllMetrics() {
        snapshotGenerationTotal.set(0);
        snapshotGenerationSuccess.set(0);
        snapshotGenerationFailure.set(0);
        snapshotGenerationTotalTime.set(0);

        promptSaveTotal.set(0);
        promptSaveSuccess.set(0);
        promptSaveFailure.set(0);
        promptSaveTotalTime.set(0);

        userDecisionTotal.set(0);
        userDecisionSuccess.set(0);
        userDecisionFailure.set(0);
        userDecisionTotalTime.set(0);

        profitLossCalculationTotal.set(0);
        profitLossCalculationSuccess.set(0);
        profitLossCalculationFailure.set(0);
        profitLossCalculationTotalTime.set(0);
    }
}
