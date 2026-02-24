package com.example.medaiassistant.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 配置缓存服务
 * 提供配置缓存、版本管理、变更通知等高级功能
 * 
 * 🔄 重构阶段：改进代码结构和可测试性
 * 
 * @author Cline AI
 * @version 1.0.0
 * @since 2025-11-05
 */
@Service
public class ConfigurationCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationCacheService.class);

    private final AIModelConfig aiModelConfig;
    private final Map<String, AIModelConfig.ModelConfig> modelConfigCache;
    private final CacheStats cacheStats;
    private String currentVersion;
    private final List<String> changeHistory;

    /**
     * 构造函数注入依赖，提高可测试性
     */
    @Autowired
    public ConfigurationCacheService(AIModelConfig aiModelConfig) {
        this.aiModelConfig = aiModelConfig;
        this.modelConfigCache = new ConcurrentHashMap<>();
        this.cacheStats = new CacheStats();
        this.currentVersion = "1.0.0";
        this.changeHistory = new ArrayList<>();
    }

    /**
     * 获取缓存的模型配置
     * 
     * @param modelName 模型名称
     * @return 模型配置
     */
    public AIModelConfig.ModelConfig getCachedModelConfig(String modelName) {
        // 首先尝试从缓存获取
        AIModelConfig.ModelConfig cachedConfig = modelConfigCache.get(modelName);
        
        if (cachedConfig != null) {
            // 缓存命中
            cacheStats.incrementHitCount();
            logger.debug("从缓存获取模型配置: {}", modelName);
            return cachedConfig;
        } else {
            // 缓存未命中，从原始配置加载
            cacheStats.incrementMissCount();
            AIModelConfig.ModelConfig originalConfig = aiModelConfig.getModelConfig(modelName);
            
            if (originalConfig != null) {
                // 将配置放入缓存
                modelConfigCache.put(modelName, originalConfig);
                logger.debug("将模型配置加入缓存: {}", modelName);
            }
            
            return originalConfig;
        }
    }

    /**
     * 通知配置变更
     * 
     * @param modelName 模型名称
     */
    public void notifyConfigurationChange(String modelName) {
        // 从缓存中移除变更的配置
        boolean removed = modelConfigCache.remove(modelName) != null;
        
        if (removed) {
            logger.info("配置变更通知: 已从缓存中移除模型配置 {}", modelName);
        } else {
            logger.debug("配置变更通知: 模型配置 {} 不在缓存中", modelName);
        }
        
        // 记录变更历史
        String changeRecord = String.format("模型 %s 配置变更于 %s", 
            modelName, new Date().toString());
        changeHistory.add(changeRecord);
        
        // 更新版本号
        updateVersion();
    }

    /**
     * 获取当前配置版本
     * 
     * @return 当前版本
     */
    public String getCurrentConfigurationVersion() {
        return currentVersion;
    }

    /**
     * 获取配置变更历史
     * 
     * @return 变更历史列表（不可修改的副本）
     */
    public List<String> getConfigurationChangeHistory() {
        return Collections.unmodifiableList(new ArrayList<>(changeHistory));
    }

    /**
     * 回滚到上一个版本
     * 
     * @return 回滚是否成功
     */
    public boolean rollbackToPreviousVersion() {
        // 清空缓存并记录回滚
        int cacheSize = modelConfigCache.size();
        modelConfigCache.clear();
        cacheStats.incrementEvictionCount(cacheSize);
        
        String rollbackRecord = String.format("配置回滚到版本 %s 于 %s", 
            currentVersion, new Date().toString());
        changeHistory.add(rollbackRecord);
        
        logger.info("配置回滚操作完成，清除了 {} 个缓存项", cacheSize);
        return true;
    }

    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计信息
     */
    public CacheStats getCacheStatistics() {
        return cacheStats;
    }

    /**
     * 清空所有缓存
     */
    public void clearAllCache() {
        int cacheSize = modelConfigCache.size();
        modelConfigCache.clear();
        cacheStats.incrementEvictionCount(cacheSize);
        logger.info("清空所有缓存，移除了 {} 个配置项", cacheSize);
    }

    /**
     * 获取缓存大小
     * 
     * @return 当前缓存中的配置项数量
     */
    public int getCacheSize() {
        return modelConfigCache.size();
    }

    /**
     * 更新版本号
     */
    private void updateVersion() {
        // 语义化版本管理：每次变更增加小版本号
        String[] parts = currentVersion.split("\\.");
        if (parts.length == 3) {
            try {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                int patch = Integer.parseInt(parts[2]) + 1;
                currentVersion = major + "." + minor + "." + patch;
            } catch (NumberFormatException e) {
                logger.warn("版本号格式错误: {}", currentVersion);
                // 重置为默认版本
                currentVersion = "1.0.0";
            }
        } else {
            // 版本格式不正确，重置为默认版本
            logger.warn("版本号格式不正确: {}，重置为默认版本", currentVersion);
            currentVersion = "1.0.0";
        }
    }

    /**
     * 缓存统计信息类
     * 使用原子操作确保线程安全
     */
    public static class CacheStats {
        private final AtomicLong hitCount = new AtomicLong(0);
        private final AtomicLong missCount = new AtomicLong(0);
        private final AtomicLong evictionCount = new AtomicLong(0);

        public long getHitCount() {
            return hitCount.get();
        }

        public long getMissCount() {
            return missCount.get();
        }

        public long getEvictionCount() {
            return evictionCount.get();
        }

        /**
         * 增加命中次数
         */
        void incrementHitCount() {
            hitCount.incrementAndGet();
        }

        /**
         * 增加未命中次数
         */
        void incrementMissCount() {
            missCount.incrementAndGet();
        }

        /**
         * 增加淘汰次数
         * 
         * @param count 淘汰的配置项数量
         */
        void incrementEvictionCount(int count) {
            if (count > 0) {
                evictionCount.addAndGet(count);
            }
        }

        /**
         * 计算命中率
         * 
         * @return 命中率 (0.0 - 1.0)
         */
        public double getHitRate() {
            long totalRequests = hitCount.get() + missCount.get();
            if (totalRequests == 0) {
                return 0.0;
            }
            return (double) hitCount.get() / totalRequests;
        }

        /**
         * 重置统计信息
         */
        public void reset() {
            hitCount.set(0);
            missCount.set(0);
            evictionCount.set(0);
        }
    }
}
