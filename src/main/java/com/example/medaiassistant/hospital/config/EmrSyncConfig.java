package com.example.medaiassistant.hospital.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * EMR病历内容同步配置类
 * 管理EMR同步相关的配置属性
 * 
 * <p><strong>配置项</strong>：</p>
 * <ul>
 *   <li>templateFilePath - SQL模板文件路径</li>
 *   <li>defaultHospitalId - 默认医院ID</li>
 *   <li>batchSize - 批量处理大小</li>
 *   <li>queryTimeout - 查询超时时间（秒）</li>
 * </ul>
 * 
 * @author TDD
 * @version 1.0
 * @since 2026-01-11
 */
@Configuration
@ConfigurationProperties(prefix = "emr.sync")
public class EmrSyncConfig {
    
    /** SQL模板文件路径 */
    private String templateFilePath = "sql/hospital-local/emr-content-query.json";
    
    /** 默认医院ID */
    private String defaultHospitalId = "Local";
    
    /** 批量处理大小（每批处理的记录数） */
    private int batchSize = 500;
    
    /** 查询超时时间（秒） */
    private int queryTimeout = 60;
    
    /** 最大查询行数 */
    private int maxRows = 2000;

    // ==================== Getters and Setters ====================

    /**
     * 获取SQL模板文件路径
     * @return 模板文件路径
     */
    public String getTemplateFilePath() {
        return templateFilePath;
    }
    
    /**
     * 根据医院ID获取模板文件路径
     * @param hospitalId 医院ID
     * @return 对应医院的模板文件路径
     */
    public String getTemplateFilePath(String hospitalId) {
        // 可根据不同医院ID返回不同路径，当前统一返回配置的路径
        return templateFilePath;
    }

    /**
     * 设置SQL模板文件路径
     * @param templateFilePath 模板文件路径
     */
    public void setTemplateFilePath(String templateFilePath) {
        this.templateFilePath = templateFilePath;
    }

    /**
     * 获取默认医院ID
     * @return 默认医院ID
     */
    public String getDefaultHospitalId() {
        return defaultHospitalId;
    }

    /**
     * 设置默认医院ID
     * @param defaultHospitalId 默认医院ID
     */
    public void setDefaultHospitalId(String defaultHospitalId) {
        this.defaultHospitalId = defaultHospitalId;
    }

    /**
     * 获取批量处理大小
     * @return 每批处理的记录数
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * 设置批量处理大小
     * @param batchSize 每批处理的记录数
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * 获取查询超时时间
     * @return 超时时间（秒）
     */
    public int getQueryTimeout() {
        return queryTimeout;
    }

    /**
     * 设置查询超时时间
     * @param queryTimeout 超时时间（秒）
     */
    public void setQueryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    /**
     * 获取最大查询行数
     * @return 最大行数
     */
    public int getMaxRows() {
        return maxRows;
    }

    /**
     * 设置最大查询行数
     * @param maxRows 最大行数
     */
    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }
}
