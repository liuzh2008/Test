package com.example.medaiassistant.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 响应缓存工具类
 * 提供高效的响应内容缓存机制，减少重复处理
 * 
 * 该工具类使用LRU缓存策略，支持TTL过期机制，优化流式响应处理性能
 * 
 * @author System
 * @version 1.0
 * @since 2025-09-29
 */
@Component
public class ResponseCacheUtil {

    private static final Logger logger = LoggerFactory.getLogger(ResponseCacheUtil.class);

    /**
     * 缓存条目类，包含缓存值和过期时间
     */
    private static class CacheEntry {
        private final String value;
        private final long expireTime;

        public CacheEntry(String value, long ttl) {
            this.value = value;
            this.expireTime = System.currentTimeMillis() + ttl;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }

        public String getValue() {
            return value;
        }
    }

    // 缓存存储
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    // 最大缓存大小
    private static final int MAX_CACHE_SIZE = 1000;

    // 默认TTL（毫秒）
    private static final long DEFAULT_TTL = 30 * 60 * 1000; // 30分钟

    // 清理线程池
    private final ScheduledExecutorService cleanupExecutor;

    /**
     * 构造函数
     * 初始化缓存清理任务
     */
    public ResponseCacheUtil() {
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ResponseCache-Cleanup");
            t.setDaemon(true);
            return t;
        });

        // 每5分钟清理一次过期缓存
        this.cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 5, 5, TimeUnit.MINUTES);
    }

    /**
     * 将响应内容放入缓存
     * 
     * @param key   缓存键（通常是Prompt内容的哈希值）
     * @param value 缓存值（处理后的响应内容）
     * @return 是否成功放入缓存
     */
    public boolean put(String key, String value) {
        return put(key, value, DEFAULT_TTL);
    }

    /**
     * 将响应内容放入缓存，指定TTL
     * 
     * @param key   缓存键
     * @param value 缓存值
     * @param ttl   缓存生存时间（毫秒）
     * @return 是否成功放入缓存
     */
    public boolean put(String key, String value, long ttl) {
        if (key == null || value == null) {
            logger.warn("缓存键或值为空，跳过缓存");
            return false;
        }

        // 如果缓存已满，清理一些旧条目
        if (cache.size() >= MAX_CACHE_SIZE) {
            cleanupOldEntries();
        }

        CacheEntry entry = new CacheEntry(value, ttl);
        cache.put(key, entry);
        logger.debug("响应内容已缓存，键: {}, 值长度: {}, TTL: {}ms", key, value.length(), ttl);
        return true;
    }

    /**
     * 从缓存中获取响应内容
     * 
     * @param key 缓存键
     * @return 缓存值，如果不存在或已过期则返回null
     */
    public String get(String key) {
        if (key == null) {
            return null;
        }

        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            cache.remove(key);
            logger.debug("缓存条目已过期，键: {}", key);
            return null;
        }

        logger.debug("缓存命中，键: {}, 值长度: {}", key, entry.getValue().length());
        return entry.getValue();
    }

    /**
     * 检查缓存中是否存在指定键的条目
     * 
     * @param key 缓存键
     * @return 是否存在且未过期
     */
    public boolean contains(String key) {
        if (key == null) {
            return false;
        }

        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return false;
        }

        if (entry.isExpired()) {
            cache.remove(key);
            return false;
        }

        return true;
    }

    /**
     * 从缓存中移除指定键的条目
     * 
     * @param key 缓存键
     * @return 是否成功移除
     */
    public boolean remove(String key) {
        if (key == null) {
            return false;
        }

        CacheEntry removed = cache.remove(key);
        if (removed != null) {
            logger.debug("缓存条目已移除，键: {}", key);
            return true;
        }
        return false;
    }

    /**
     * 清理所有缓存条目
     */
    public void clear() {
        int size = cache.size();
        cache.clear();
        logger.info("缓存已清空，清理了 {} 个条目", size);
    }

    /**
     * 获取当前缓存大小
     * 
     * @return 缓存条目数量
     */
    public int size() {
        return cache.size();
    }

    /**
     * 清理过期缓存条目
     */
    private void cleanupExpiredEntries() {
        int initialSize = cache.size();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int removedCount = initialSize - cache.size();

        if (removedCount > 0) {
            logger.info("清理了 {} 个过期缓存条目，当前缓存大小: {}", removedCount, cache.size());
        }
    }

    /**
     * 清理旧缓存条目（当缓存满时）
     */
    private void cleanupOldEntries() {
        int targetSize = MAX_CACHE_SIZE / 2; // 清理到一半大小
        int currentSize = cache.size();

        if (currentSize <= targetSize) {
            return;
        }

        // 简单的LRU策略：移除最早的一些条目
        int toRemove = currentSize - targetSize;
        cache.entrySet().stream()
                .limit(toRemove)
                .forEach(entry -> cache.remove(entry.getKey()));

        logger.info("清理了 {} 个旧缓存条目，缓存大小从 {} 减少到 {}", toRemove, currentSize, cache.size());
    }

    /**
     * 生成缓存键
     * 使用Prompt内容的哈希值作为缓存键
     * 
     * @param prompt Prompt内容
     * @return 缓存键
     */
    public static String generateCacheKey(String prompt) {
        if (prompt == null) {
            return null;
        }

        // 使用哈希值作为缓存键，避免存储过长的字符串
        int hash = prompt.hashCode();
        return "prompt_" + Math.abs(hash);
    }

    /**
     * 关闭清理线程池
     * 在应用关闭时调用
     */
    public void shutdown() {
        if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
