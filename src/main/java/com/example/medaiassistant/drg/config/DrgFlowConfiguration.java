package com.example.medaiassistant.drg.config;

import java.util.List;
import java.util.ArrayList;

/**
 * DRGs流程配置类
 * 包含完整的流程配置信息
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-12
 */
public class DrgFlowConfiguration {
    
    private List<ProcessorConfiguration> processors;
    
    /**
     * 默认构造函数
     */
    public DrgFlowConfiguration() {
        this.processors = new ArrayList<>();
    }
    
    /**
     * 构造函数
     * @param processors 处理器配置列表
     */
    public DrgFlowConfiguration(List<ProcessorConfiguration> processors) {
        this.processors = processors != null ? processors : new ArrayList<>();
    }
    
    /**
     * 获取处理器配置列表
     * @return 处理器配置列表
     */
    public List<ProcessorConfiguration> getProcessors() {
        return processors;
    }
    
    /**
     * 设置处理器配置列表
     * @param processors 处理器配置列表
     */
    public void setProcessors(List<ProcessorConfiguration> processors) {
        this.processors = processors != null ? processors : new ArrayList<>();
    }
    
    /**
     * 添加处理器配置
     * @param processor 处理器配置
     */
    public void addProcessor(ProcessorConfiguration processor) {
        if (this.processors == null) {
            this.processors = new ArrayList<>();
        }
        this.processors.add(processor);
    }
    
    /**
     * 根据名称查找处理器配置
     * @param name 处理器名称
     * @return 处理器配置，如果未找到则返回null
     */
    public ProcessorConfiguration findProcessorByName(String name) {
        if (processors == null || name == null) {
            return null;
        }
        
        return processors.stream()
                .filter(processor -> name.equals(processor.getName()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 获取启用的处理器配置列表（按执行顺序排序）
     * @return 启用的处理器配置列表
     */
    public List<ProcessorConfiguration> getEnabledProcessors() {
        if (processors == null) {
            return new ArrayList<>();
        }
        
        return processors.stream()
                .filter(ProcessorConfiguration::isEnabled)
                .sorted((p1, p2) -> Integer.compare(p1.getOrder(), p2.getOrder()))
                .toList();
    }
    
    @Override
    public String toString() {
        return "DrgFlowConfiguration{" +
                "processors=" + processors +
                '}';
    }
}
