package com.example.medaiassistant.hospital.model;

import lombok.Data;
import java.util.Map;

/**
 * 医院配置模型类
 * 对应YAML配置文件结构，用于内存中表示医院配置
 * 不是JPA实体，不存储到数据库
 */
@Data
public class HospitalConfig {
    
    /**
     * 医院配置根节点
     */
    private Hospital hospital;
    
    /**
     * 医院配置内部类
     */
    @Data
    public static class Hospital {
        /**
         * 医院ID，唯一标识符
         */
        private String id;
        
        /**
         * 医院名称
         */
        private String name;
        
        /**
         * 集成类型：database 或 api
         */
        private String integrationType;
        
        /**
         * HIS数据库配置
         */
        private HisConfig his;
        
        /**
         * LIS数据库配置（可选）
         */
        private LisConfig lis;
        
        /**
         * 同步配置
         */
        private SyncConfig sync;
        
        /**
         * 字段映射配置
         * key: 标准字段名, value: 医院数据库字段名
         */
        private Map<String, String> fieldMappings;
        
        /**
         * SQL模板配置
         */
        private SqlTemplateConfig sqlTemplates;
    }
    
    /**
     * HIS数据库配置
     */
    @Data
    public static class HisConfig {
        /**
         * 数据库连接URL
         */
        private String url;
        
        /**
         * 数据库用户名
         */
        private String username;
        
        /**
         * 数据库密码
         */
        private String password;
        
        /**
         * 表前缀
         */
        private String tablePrefix;
    }
    
    /**
     * LIS数据库配置
     */
    @Data
    public static class LisConfig {
        /**
         * 数据库连接URL
         */
        private String url;
        
        /**
         * 数据库用户名
         */
        private String username;
        
        /**
         * 数据库密码
         */
        private String password;
        
        /**
         * 表前缀
         */
        private String tablePrefix;
    }
    
    /**
     * 同步配置
     */
    @Data
    public static class SyncConfig {
        /**
         * 定时任务cron表达式
         */
        private String cron;
        
        /**
         * 是否启用同步
         */
        private Boolean enabled;
        
        /**
         * 最大重试次数
         */
        private Integer maxRetries;
        
        /**
         * 重试间隔（毫秒）
         */
        private Integer retryInterval;
    }
    
    /**
     * SQL模板配置
     */
    @Data
    public static class SqlTemplateConfig {
        /**
         * 基础路径
         */
        private String basePath;
        
        /**
         * 覆盖模板列表
         */
        private String[] overrides;
    }
    
    /**
     * 便捷方法：获取医院ID
     */
    public String getId() {
        return hospital != null ? hospital.getId() : null;
    }
    
    /**
     * 便捷方法：获取医院名称
     */
    public String getName() {
        return hospital != null ? hospital.getName() : null;
    }
    
    /**
     * 便捷方法：获取集成类型
     */
    public String getIntegrationType() {
        return hospital != null ? hospital.getIntegrationType() : null;
    }
    
    /**
     * 便捷方法：检查是否启用同步
     */
    public boolean isSyncEnabled() {
        return hospital != null && 
               hospital.getSync() != null && 
               Boolean.TRUE.equals(hospital.getSync().getEnabled());
    }
    
    /**
     * 便捷方法：检查是否启用
     */
    public boolean isEnabled() {
        return hospital != null && 
               hospital.getSync() != null && 
               Boolean.TRUE.equals(hospital.getSync().getEnabled());
    }
    
    /**
     * 便捷方法：获取HIS配置
     */
    public HisConfig getHisConfig() {
        return hospital != null ? hospital.getHis() : null;
    }
    
    /**
     * 便捷方法：获取LIS配置
     */
    public LisConfig getLisConfig() {
        return hospital != null ? hospital.getLis() : null;
    }
    
    /**
     * 便捷方法：获取同步配置
     */
    public SyncConfig getSyncConfig() {
        return hospital != null ? hospital.getSync() : null;
    }
    
    /**
     * 便捷方法：获取字段映射
     */
    public Map<String, String> getFieldMappings() {
        return hospital != null ? hospital.getFieldMappings() : null;
    }
    
    /**
     * 便捷方法：获取SQL模板配置
     */
    public SqlTemplateConfig getSqlTemplateConfig() {
        return hospital != null ? hospital.getSqlTemplates() : null;
    }
    
    @Override
    public String toString() {
        return "HospitalConfig{" +
                "id='" + getId() + '\'' +
                ", name='" + getName() + '\'' +
                ", integrationType='" + getIntegrationType() + '\'' +
                '}';
    }
}
