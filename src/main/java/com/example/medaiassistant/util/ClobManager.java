package com.example.medaiassistant.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.rowset.serial.SerialClob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Clob对象管理工具类
 * 优化Clob对象的内存管理，防止内存泄漏
 * 
 * @author System
 * @version 1.0
 * @since 2025-09-29
 */
public class ClobManager {

    private static final Logger logger = LoggerFactory.getLogger(ClobManager.class);

    // 使用ConcurrentHashMap存储活跃的Clob对象，用于监控和调试
    private static final ConcurrentHashMap<String, ClobInfo> activeClobs = new ConcurrentHashMap<>();
    private static final AtomicInteger clobCounter = new AtomicInteger(0);
    private static final AtomicInteger memoryUsage = new AtomicInteger(0);

    // 最大内存限制（字节）
    private static final long MAX_MEMORY_LIMIT = 100 * 1024 * 1024; // 100MB

    /**
     * 创建SerialClob对象并跟踪内存使用
     * 
     * @param data 字符串数据
     * @return SerialClob对象
     * @throws SQLException 如果创建失败
     */
    public static SerialClob createSerialClob(String data) throws SQLException {
        if (data == null) {
            return null;
        }

        // 检查内存限制
        checkMemoryLimit(data.length());

        SerialClob clob = new SerialClob(data.toCharArray());

        // 跟踪Clob对象
        String clobId = "clob-" + clobCounter.incrementAndGet();
        activeClobs.put(clobId, new ClobInfo(clobId, clob, data.length()));
        memoryUsage.addAndGet(data.length());

        logger.debug("创建Clob对象 - ID: {}, 大小: {} 字节, 总内存使用: {} 字节",
                clobId, data.length(), memoryUsage.get());

        return clob;
    }

    /**
     * 安全释放Clob对象
     * 
     * @param clob Clob对象
     */
    public static void safeRelease(Clob clob) {
        if (clob == null) {
            return;
        }

        try {
            // 从活跃列表中移除
            String clobIdToRemove = null;
            for (var entry : activeClobs.entrySet()) {
                if (entry.getValue().clob == clob) {
                    clobIdToRemove = entry.getKey();
                    break;
                }
            }

            if (clobIdToRemove != null) {
                ClobInfo info = activeClobs.remove(clobIdToRemove);
                if (info != null) {
                    memoryUsage.addAndGet(-info.size);
                    logger.debug("释放Clob对象 - ID: {}, 大小: {} 字节, 剩余内存使用: {} 字节",
                            clobIdToRemove, info.size, memoryUsage.get());
                }
            }

            // 释放Clob资源
            if (clob instanceof SerialClob) {
                ((SerialClob) clob).free();
            }

        } catch (Exception e) {
            logger.warn("释放Clob对象时发生异常: {}", e.getMessage());
        }
    }

    /**
     * 检查内存限制
     * 
     * @param newDataSize 新数据大小
     */
    private static void checkMemoryLimit(int newDataSize) {
        long currentUsage = memoryUsage.get();
        if (currentUsage + newDataSize > MAX_MEMORY_LIMIT) {
            logger.warn("Clob内存使用接近限制 - 当前: {} 字节, 新数据: {} 字节, 限制: {} 字节",
                    currentUsage, newDataSize, MAX_MEMORY_LIMIT);

            // 可以在这里添加内存清理逻辑
            cleanupOldClobs();
        }
    }

    /**
     * 清理旧的Clob对象
     */
    private static void cleanupOldClobs() {
        // 简单的清理策略：如果内存使用超过限制的80%，清理一半的Clob对象
        if (memoryUsage.get() > MAX_MEMORY_LIMIT * 0.8) {
            logger.info("开始清理Clob对象 - 当前内存使用: {} 字节", memoryUsage.get());

            int targetSize = activeClobs.size() / 2;
            int removedCount = 0;

            for (var entry : activeClobs.entrySet()) {
                if (removedCount >= targetSize) {
                    break;
                }

                ClobInfo info = entry.getValue();
                safeRelease(info.clob);
                removedCount++;
            }

            logger.info("清理完成 - 移除了 {} 个Clob对象, 剩余内存使用: {} 字节",
                    removedCount, memoryUsage.get());
        }
    }

    /**
     * 获取Clob内存使用统计
     * 
     * @return 内存使用统计
     */
    public static ClobMemoryStats getMemoryStats() {
        return new ClobMemoryStats(
                activeClobs.size(),
                memoryUsage.get(),
                MAX_MEMORY_LIMIT,
                (double) memoryUsage.get() / MAX_MEMORY_LIMIT * 100);
    }

    /**
     * 强制清理所有Clob对象
     */
    public static void forceCleanup() {
        logger.info("强制清理所有Clob对象 - 当前数量: {}", activeClobs.size());

        for (var entry : activeClobs.entrySet()) {
            safeRelease(entry.getValue().clob);
        }

        activeClobs.clear();
        memoryUsage.set(0);

        logger.info("强制清理完成 - 剩余Clob对象: {}", activeClobs.size());
    }

    /**
     * Clob信息内部类
     */
    private static class ClobInfo {
        final Clob clob;
        final int size;

        ClobInfo(String id, Clob clob, int size) {
            this.clob = clob;
            this.size = size;
        }
    }

    /**
     * Clob内存统计类
     */
    public static class ClobMemoryStats {
        private final int activeClobCount;
        private final long memoryUsage;
        private final long maxMemoryLimit;
        private final double usagePercentage;

        public ClobMemoryStats(int activeClobCount, long memoryUsage, long maxMemoryLimit, double usagePercentage) {
            this.activeClobCount = activeClobCount;
            this.memoryUsage = memoryUsage;
            this.maxMemoryLimit = maxMemoryLimit;
            this.usagePercentage = usagePercentage;
        }

        public int getActiveClobCount() {
            return activeClobCount;
        }

        public long getMemoryUsage() {
            return memoryUsage;
        }

        public long getMaxMemoryLimit() {
            return maxMemoryLimit;
        }

        public double getUsagePercentage() {
            return usagePercentage;
        }

        @Override
        public String toString() {
            return String.format("ClobMemoryStats{activeClobCount=%d, memoryUsage=%d bytes, usagePercentage=%.2f%%}",
                    activeClobCount, memoryUsage, usagePercentage);
        }
    }
}
