package com.example.medaiassistant.dto;

/**
 * 查房记录模板配置DTO
 * 
 * 用于封装查房记录模板名称和时间间隔配置
 * 
 * @author TDD
 * @since 2026-01-31
 * @version 1.0
 */
public class TemplateConfig {
    
    /**
     * 模板名称
     */
    private final String templateName;
    
    /**
     * 时间间隔（小时）
     */
    private final int intervalHours;
    
    /**
     * 构造函数
     * 
     * @param templateName 模板名称
     * @param intervalHours 时间间隔（小时）
     */
    public TemplateConfig(String templateName, int intervalHours) {
        this.templateName = templateName;
        this.intervalHours = intervalHours;
    }
    
    /**
     * 获取模板名称
     * 
     * @return 模板名称
     */
    public String getTemplateName() {
        return templateName;
    }
    
    /**
     * 获取时间间隔（小时）
     * 
     * @return 时间间隔
     */
    public int getIntervalHours() {
        return intervalHours;
    }
    
    @Override
    public String toString() {
        return "TemplateConfig{" +
                "templateName='" + templateName + '\'' +
                ", intervalHours=" + intervalHours +
                '}';
    }
}
