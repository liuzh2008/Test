package com.example.medaiassistant.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * AI模型配置类，用于管理所有AI模型的配置信息
 * 
 * 🔄 重构改进：
 * - 添加日志记录替代System.out
 * - 使用Optional改进API设计
 * - 添加配置验证方法
 * - 改进错误处理
 * - 添加配置验证注解
 * - 改进配置加载逻辑
 * 
 * @author Cline AI
 * @version 2.1.0
 * @since 2025-09-19
 * 
 * @configurationProperties 绑定以"ai"为前缀的配置属性
 */
@Component
@ConfigurationProperties(prefix = "ai")
public class AIModelConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AIModelConfig.class);
    
    /**
     * 是否启用流式响应
     */
    private boolean stream;
    
    /**
     * AI模型配置映射表，key为模型名称，value为模型配置
     */
    private Map<String, ModelConfig> models = new HashMap<>();

    /**
     * 设置是否启用流式响应
     * @param stream 是否启用流式响应
     */
    @Value("${ai.model.stream:false}")
    public void setStream(boolean stream) {
        this.stream = stream;
        logger.debug("流式响应配置已设置: {}", stream);
    }

    /**
     * 获取是否启用流式响应
     * @return 是否启用流式响应
     */
    public boolean isStream() {
        return stream;
    }

    /**
     * 构造函数，初始化AI模型配置
     */
    public AIModelConfig() {
        logger.info("AIModelConfig 初始化完成");
    }

    /**
     * AI模型配置内部类，包含单个AI模型的详细配置信息
     * 
     * @author Cline AI
     * @version 2.0.0
     * @since 2025-09-19
     * 
     * @description
     * 单个AI模型的配置信息，包含API端点、密钥、超时设置等参数。
     * 使用@ConfigurationProperties注解支持Spring Boot配置属性绑定。
     * 
     * 🔄 重构改进：
     * - 添加配置验证方法
     * - 添加配置有效性检查
     * - 改进默认值设置
     * - 添加配置描述方法
     * 
     * @configuration
     * - url: AI模型API端点URL
     * - key: AI模型API密钥
     * - connectTimeout: 连接超时时间（毫秒）
     * - readTimeout: 读取超时时间（毫秒）
     * - maxRetries: 最大重试次数
     * - retryDelay: 初始重试延迟时间（毫秒）
     */
    @org.springframework.boot.context.properties.ConfigurationProperties
    public static class ModelConfig {
        /**
         * AI模型API端点URL
         */
        private String url;
        
        /**
         * AI模型API密钥
         */
        private String key;
        
        /**
         * 最大重试次数，默认3次
         */
        private int maxRetries = 3;
        
        /**
         * 初始重试延迟时间，单位毫秒，默认1000ms
         */
        private long retryDelay = 1000;
        
        /**
         * 连接超时时间，单位毫秒，默认10000ms
         */
        private long connectTimeout = 10000;
        
        /**
         * 读取超时时间，单位毫秒，默认600000ms（10分钟）
         */
        private long readTimeout = 600000;

        // Getters and setters
        
        /**
         * 获取AI模型API端点URL
         * @return API端点URL字符串
         */
        public String getUrl() {
            return url;
        }

        /**
         * 设置AI模型API端点URL
         * @param url API端点URL字符串
         */
        public void setUrl(String url) {
            this.url = url;
        }

        /**
         * 获取AI模型API密钥
         * @return API密钥字符串
         */
        public String getKey() {
            return key;
        }

        /**
         * 设置AI模型API密钥
         * @param key API密钥字符串
         */
        public void setKey(String key) {
            this.key = key;
        }

        /**
         * 获取最大重试次数
         * @return 最大重试次数，默认3次
         */
        public int getMaxRetries() {
            return maxRetries;
        }

        /**
         * 设置最大重试次数
         * @param maxRetries 最大重试次数
         */
        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        /**
         * 获取初始重试延迟时间
         * @return 重试延迟时间，单位毫秒
         */
        public long getRetryDelay() {
            return retryDelay;
        }

        /**
         * 设置初始重试延迟时间
         * @param retryDelay 重试延迟时间，单位毫秒
         */
        public void setRetryDelay(long retryDelay) {
            this.retryDelay = retryDelay;
        }

        /**
         * 获取连接超时时间
         * @return 连接超时时间，单位毫秒
         */
        public long getConnectTimeout() {
            return connectTimeout;
        }

        /**
         * 设置连接超时时间
         * @param connectTimeout 连接超时时间，单位毫秒
         */
        public void setConnectTimeout(long connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        /**
         * 获取读取超时时间
         * @return 读取超时时间，单位毫秒
         */
        public long getReadTimeout() {
            return readTimeout;
        }

        /**
         * 设置读取超时时间
         * @param readTimeout 读取超时时间，单位毫秒
         */
        public void setReadTimeout(long readTimeout) {
            this.readTimeout = readTimeout;
        }

        /**
         * 🔄 重构新增：验证配置是否完整
         * @return 配置是否完整有效
         */
        public boolean isValid() {
            return url != null && !url.trim().isEmpty() &&
                   key != null && !key.trim().isEmpty() &&
                   maxRetries > 0 &&
                   retryDelay >= 0 &&
                   connectTimeout > 0 &&
                   readTimeout > 0;
        }

        /**
         * 🔄 重构新增：获取配置摘要信息
         * @return 配置摘要字符串
         */
        public String getSummary() {
            return String.format("URL: %s, 连接超时: %dms, 读取超时: %dms, 最大重试: %d, 重试延迟: %dms",
                url, connectTimeout, readTimeout, maxRetries, retryDelay);
        }

        /**
         * 🔄 重构新增：验证URL格式
         * @return URL格式是否有效
         */
        public boolean hasValidUrl() {
            return url != null && 
                   (url.startsWith("http://") || url.startsWith("https://"));
        }

        /**
         * 🔄 重构新增：获取安全摘要（隐藏敏感信息）
         * @return 安全摘要字符串
         */
        public String getSecureSummary() {
            return String.format("URL: %s, 连接超时: %dms, 读取超时: %dms, 最大重试: %d, 重试延迟: %dms, Key: ***",
                url, connectTimeout, readTimeout, maxRetries, retryDelay);
        }
    }

    /**
     * 设置AI模型配置映射表
     * @param models AI模型配置映射表，key为模型名称，value为模型配置
     */
    public void setModels(Map<String, ModelConfig> models) {
        this.models = models != null ? models : new HashMap<>();
        logger.info("已加载AI模型配置: {}", this.models.keySet());
        
        // 详细记录每个模型的配置
        for (Map.Entry<String, ModelConfig> entry : this.models.entrySet()) {
            ModelConfig config = entry.getValue();
            if (config != null) {
                logger.debug("模型: {}, URL: {}, Key: {}", 
                    entry.getKey(), 
                    config.getUrl(),
                    config.getKey() != null ? "***" : "null");
            } else {
                logger.warn("模型 {} 的配置为空", entry.getKey());
            }
        }
        
        // 验证配置有效性
        validateConfiguration();
    }
    
    /**
     * 🔄 重构新增：验证配置有效性
     */
    private void validateConfiguration() {
        if (models.isEmpty()) {
            logger.warn("未配置任何AI模型，系统可能无法正常工作");
            return;
        }
        
        int validConfigs = 0;
        for (Map.Entry<String, ModelConfig> entry : models.entrySet()) {
            if (entry.getValue() != null && entry.getValue().isValid()) {
                validConfigs++;
            } else {
                logger.warn("模型 {} 的配置无效或缺失必要字段", entry.getKey());
            }
        }
        
        logger.info("配置验证完成: {}/{} 个模型配置有效", validConfigs, models.size());
    }

    /**
     * 获取AI模型配置映射表
     * @return AI模型配置映射表
     */
    public Map<String, ModelConfig> getModels() {
        return models;
    }

    /**
     * 获取特定模型的配置信息
     * @param modelName 模型名称
     * @return 模型配置信息，如果不存在则返回null
     */
    public ModelConfig getModelConfig(String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) {
            logger.warn("请求模型配置时模型名称为空");
            return null;
        }
        
        logger.debug("请求模型配置: {}, 可用模型: {}", modelName, models.keySet());
        ModelConfig config = models.get(modelName);
        
        if (config == null) {
            logger.debug("未找到模型 {} 的配置", modelName);
        }
        
        return config;
    }

    /**
     * 🔄 重构新增：使用Optional获取模型配置
     * @param modelName 模型名称
     * @return 包含模型配置的Optional对象
     */
    public Optional<ModelConfig> getModelConfigOptional(String modelName) {
        return Optional.ofNullable(models.get(modelName));
    }

    /**
     * 🔄 重构新增：验证模型配置是否有效
     * @param modelName 模型名称
     * @return 配置是否有效
     */
    public boolean isValidModelConfig(String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) {
            return false;
        }
        
        ModelConfig config = getModelConfig(modelName);
        return config != null && config.isValid();
    }
    
    /**
     * 🔄 重构新增：获取默认模型配置
     * @return 默认模型配置，如果不存在则返回第一个有效配置
     */
    public Optional<ModelConfig> getDefaultModelConfig() {
        // 优先返回deepseek-chat配置
        Optional<ModelConfig> defaultConfig = getModelConfigOptional("deepseek-chat");
        if (defaultConfig.isPresent() && defaultConfig.get().isValid()) {
            return defaultConfig;
        }
        
        // 返回第一个有效配置
        return getValidModelNames().stream()
                .findFirst()
                .flatMap(this::getModelConfigOptional);
    }
    
    /**
     * 🔄 重构新增：检查是否有有效配置
     * @return 是否存在有效配置
     */
    public boolean hasValidConfigurations() {
        return !getValidModelNames().isEmpty();
    }

    /**
     * 🔄 重构新增：获取所有有效模型名称
     * @return 有效模型名称列表
     */
    public java.util.List<String> getValidModelNames() {
        return models.keySet().stream()
                .filter(this::isValidModelConfig)
                .collect(java.util.stream.Collectors.toList());
    }
}
