package com.example.medaiassistant.service.filter;

import com.example.medaiassistant.config.SchedulingProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 患者过滤策略工厂
 * 用于根据过滤模式选择合适的过滤策略
 */
@Component
public class PatientFilterStrategyFactory {
    private final List<PatientFilterStrategy> strategies;
    
    /**
     * 构造函数，注入所有的过滤策略
     * @param strategies 过滤策略列表
     */
    public PatientFilterStrategyFactory(List<PatientFilterStrategy> strategies) {
        this.strategies = strategies;
    }
    
    /**
     * 根据过滤模式获取合适的过滤策略
     * @param filterMode 过滤模式
     * @return 过滤策略
     * @throws IllegalArgumentException 如果找不到合适的过滤策略
     */
    public PatientFilterStrategy getStrategy(SchedulingProperties.TimerConfig.FilterMode filterMode) {
        return strategies.stream()
            .filter(strategy -> strategy.isApplicable(filterMode))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("未找到适用的过滤策略: " + filterMode));
    }
}
