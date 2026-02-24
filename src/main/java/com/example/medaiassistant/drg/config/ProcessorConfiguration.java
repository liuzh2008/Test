package com.example.medaiassistant.drg.config;

import java.util.Map;

/**
 * 处理器配置类
 * 用于配置单个处理器的属性
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-12
 */
public class ProcessorConfiguration {
    
    private String name;
    private int order;
    private boolean enabled;
    private Map<String, Object> parameters;
    
    /**
     * 默认构造函数
     */
    public ProcessorConfiguration() {
    }
    
    /**
     * 构造函数
     * @param name 处理器名称
     * @param order 执行顺序
     * @param enabled 是否启用
     */
    public ProcessorConfiguration(String name, int order, boolean enabled) {
        this.name = name;
        this.order = order;
        this.enabled = enabled;
    }
    
    /**
     * 获取处理器名称
     * @return 处理器名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 设置处理器名称
     * @param name 处理器名称
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * 获取执行顺序
     * @return 执行顺序
     */
    public int getOrder() {
        return order;
    }
    
    /**
     * 设置执行顺序
     * @param order 执行顺序
     */
    public void setOrder(int order) {
        this.order = order;
    }
    
    /**
     * 检查是否启用
     * @return 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 设置启用状态
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * 获取处理器参数
     * @return 处理器参数映射
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    /**
     * 设置处理器参数
     * @param parameters 处理器参数映射
     */
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
    
    @Override
    public String toString() {
        return "ProcessorConfiguration{" +
                "name='" + name + '\'' +
                ", order=" + order +
                ", enabled=" + enabled +
                ", parameters=" + parameters +
                '}';
    }
}
