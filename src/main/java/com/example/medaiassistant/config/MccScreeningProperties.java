package com.example.medaiassistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MCC预筛选配置属性类
 * 支持配置化阈值管理
 * 
 * @author MedAI Assistant Team
 * @since 2025-11-09
 */
@Data
@Component
@ConfigurationProperties(prefix = "drg.mcc")
public class MccScreeningProperties {

    /**
     * 全局相似度阈值（默认0.3）
     * 相似度低于此值的候选将被过滤
     */
    private Double similarityThreshold = 0.3;

    /**
     * 排除规则检查开关（默认true）
     */
    private Boolean exclusionCheckEnabled = true;

    /**
     * 最大候选数量（默认50）
     */
    private Integer maxCandidates = 50;

    /**
     * Top-K配置
     */
    private TopKConfig topK = new TopKConfig();

    /**
     * 缓存配置
     */
    private CacheConfig cache = new CacheConfig();

    /**
     * Top-K配置类
     */
    @Data
    public static class TopKConfig {
        /**
         * Top-K开关（默认false）
         */
        private Boolean enabled = false;

        /**
         * 每诊断Top-K值（默认10）
         */
        private Integer diag = 10;
    }

    /**
     * 缓存配置类
     */
    @Data
    public static class CacheConfig {
        /**
         * 缓存开关（默认true）
         */
        private Boolean enabled = true;

        /**
         * 字典重载cron表达式（默认每天凌晨2点）
         */
        private String reloadCron = "0 0 2 * * ?";
    }

    /**
     * 获取相似度阈值，如果未配置则返回默认值
     */
    public double getSimilarityThreshold() {
        return similarityThreshold != null ? similarityThreshold : 0.3;
    }

    /**
     * 获取排除规则检查开关，如果未配置则返回默认值
     */
    public boolean isExclusionCheckEnabled() {
        return exclusionCheckEnabled != null ? exclusionCheckEnabled : true;
    }

    /**
     * 获取最大候选数量，如果未配置则返回默认值
     */
    public int getMaxCandidates() {
        return maxCandidates != null ? maxCandidates : 50;
    }

    /**
     * 获取Top-K开关，如果未配置则返回默认值
     */
    public boolean isTopKEnabled() {
        return topK != null && topK.getEnabled() != null ? topK.getEnabled() : false;
    }

    /**
     * 获取每诊断Top-K值，如果未配置则返回默认值
     */
    public int getTopKDiag() {
        return topK != null && topK.getDiag() != null ? topK.getDiag() : 10;
    }

    /**
     * 获取缓存开关，如果未配置则返回默认值
     */
    public boolean isCacheEnabled() {
        return cache != null && cache.getEnabled() != null ? cache.getEnabled() : true;
    }

    /**
     * 获取字典重载cron表达式，如果未配置则返回默认值
     */
    public String getReloadCron() {
        return cache != null && cache.getReloadCron() != null ? cache.getReloadCron() : "0 0 2 * * ?";
    }
}
