package com.example.medaiassistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Prompt服务配置类
 * 用于管理提交服务、轮询服务和监控服务的独立配置参数
 * 
 * 该配置类支持：
 * 1. 独立的提交服务配置
 * 2. 独立的轮询服务配置  
 * 3. 独立的监控服务配置
 * 4. 配置属性验证
 * 5. 多环境配置支持
 */
@Component
@ConfigurationProperties(prefix = "prompt")
@Validated
@Data
public class PromptServiceConfig {
    
    /**
     * 提交服务配置
     */
    private SubmissionConfig submission = new SubmissionConfig();
    
    /**
     * 轮询服务配置
     */
    private PollingConfig polling = new PollingConfig();
    
    /**
     * 监控服务配置
     */
    private MonitoringConfig monitoring = new MonitoringConfig();
    
    /**
     * 提交服务配置类
     */
    @Data
    @Validated
    public static class SubmissionConfig {
        /**
         * 是否启用提交服务
         * 默认值：true
         */
        private boolean enabled = true;
        
        /**
         * 执行间隔时间（毫秒）
         * 默认值：10000毫秒（10秒）
         */
        @NotNull
        @Positive
        private long interval = 10000;
        
        /**
         * 每页处理数量
         * 默认值：10条记录
         */
        @NotNull
        @Min(1)
        private int pageSize = 10;
        
        /**
         * 最大线程数
         * 默认值：5个线程
         */
        @NotNull
        @Min(1)
        private int maxThreads = 5;
        
        /**
         * 重试次数
         * 默认值：3次
         */
        @NotNull
        @Min(0)
        private int maxRetries = 3;
        
        /**
         * 重试间隔（毫秒）
         * 默认值：1000毫秒（1秒）
         */
        @NotNull
        @Positive
        private long retryInterval = 1000;
        
        /**
         * 连接超时时间（毫秒）
         * 默认值：300000毫秒（5分钟）
         */
        @NotNull
        @Positive
        private long connectTimeout = 300000;
        
        /**
         * 读取超时时间（毫秒）
         * 默认值：300000毫秒（5分钟）
         */
        @NotNull
        @Positive
        private long readTimeout = 300000;
    }
    
    /**
     * 轮询服务配置类
     */
    @Data
    @Validated
    public static class PollingConfig {
        /**
         * 是否启用轮询服务
         * 默认值：true
         */
        private boolean enabled = true;
        
        /**
         * 轮询间隔时间（毫秒）
         * 默认值：30000毫秒（30秒）
         */
        @NotNull
        @Positive
        private long interval = 30000;
        
        /**
         * 每页处理数量
         * 默认值：20条记录
         */
        @NotNull
        @Min(1)
        private int pageSize = 20;
        
        /**
         * 最大重试次数
         * 默认值：5次
         */
        @NotNull
        @Min(0)
        private int maxRetries = 5;
        
        /**
         * 重试间隔（毫秒）
         * 默认值：2000毫秒（2秒）
         */
        @NotNull
        @Positive
        private long retryInterval = 2000;
        
        /**
         * 批量处理大小
         * 默认值：50条记录
         */
        @NotNull
        @Min(1)
        private int batchSize = 50;
        
        /**
         * 超时时间（毫秒）
         * 默认值：30000毫秒（30秒）
         */
        @NotNull
        @Positive
        private long timeout = 30000;
    }
    
    /**
     * 监控服务配置类
     */
    @Data
    @Validated
    public static class MonitoringConfig {
        /**
         * 是否启用监控服务
         * 默认值：true
         */
        private boolean enabled = true;
        
        /**
         * 监控间隔时间（毫秒）
         * 默认值：60000毫秒（1分钟）
         */
        @NotNull
        @Positive
        private long interval = 60000;
        
        /**
         * 健康检查超时时间（毫秒）
         * 默认值：10000毫秒（10秒）
         */
        @NotNull
        @Positive
        private long healthCheckTimeout = 10000;
        
        /**
         * 性能指标收集间隔（毫秒）
         * 默认值：30000毫秒（30秒）
         */
        @NotNull
        @Positive
        private long metricsInterval = 30000;
        
        /**
         * 告警阈值 - 错误率（百分比）
         * 默认值：5%
         */
        @NotNull
        @Min(0)
        private double errorRateThreshold = 5.0;
        
        /**
         * 告警阈值 - 响应时间（毫秒）
         * 默认值：10000毫秒（10秒）
         */
        @NotNull
        @Positive
        private long responseTimeThreshold = 10000;
        
        /**
         * 告警阈值 - 队列长度
         * 默认值：100个任务
         */
        @NotNull
        @Min(1)
        private int queueLengthThreshold = 100;
    }
    
    /**
     * 验证配置是否有效
     * @return true如果配置有效，否则false
     */
    public boolean isValid() {
        return submission != null && polling != null && monitoring != null;
    }
    
    /**
     * 获取提交服务配置摘要
     * @return 配置摘要字符串
     */
    public String getSubmissionConfigSummary() {
        return String.format("Submission[enabled=%s, interval=%dms, pageSize=%d, maxThreads=%d, maxRetries=%d]", 
                submission.enabled, submission.interval, submission.pageSize, 
                submission.maxThreads, submission.maxRetries);
    }
    
    /**
     * 获取轮询服务配置摘要
     * @return 配置摘要字符串
     */
    public String getPollingConfigSummary() {
        return String.format("Polling[enabled=%s, interval=%dms, pageSize=%d, maxRetries=%d, batchSize=%d]", 
                polling.enabled, polling.interval, polling.pageSize, 
                polling.maxRetries, polling.batchSize);
    }
    
    /**
     * 获取监控服务配置摘要
     * @return 配置摘要字符串
     */
    public String getMonitoringConfigSummary() {
        return String.format("Monitoring[enabled=%s, interval=%dms, errorRateThreshold=%.1f%%, responseTimeThreshold=%dms]", 
                monitoring.enabled, monitoring.interval, 
                monitoring.errorRateThreshold, monitoring.responseTimeThreshold);
    }
    
    /**
     * 获取完整配置摘要
     * @return 完整配置摘要字符串
     */
    public String getFullConfigSummary() {
        return String.format("PromptServiceConfig - %s | %s | %s", 
                getSubmissionConfigSummary(), getPollingConfigSummary(), getMonitoringConfigSummary());
    }
}
