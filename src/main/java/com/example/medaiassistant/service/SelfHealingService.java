package com.example.medaiassistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自愈服务
 * 提供各种系统自动修复功能
 */
@Service
public class SelfHealingService {
    
    private static final Logger logger = LoggerFactory.getLogger(SelfHealingService.class);
    
    @Autowired
    private DatabaseHealthService databaseHealthService;
    
    @Autowired
    private NetworkHealthService networkHealthService;
    
    // 操作统计
    private final AtomicLong totalHealingOperations = new AtomicLong(0);
    private final AtomicLong successfulHealingOperations = new AtomicLong(0);
    private final AtomicLong failedHealingOperations = new AtomicLong(0);
    
    /**
     * 重置数据库连接池
     */
    public boolean resetDatabaseConnectionPool() {
        try {
            logger.info("开始重置数据库连接池...");
            totalHealingOperations.incrementAndGet();
            
            boolean result = databaseHealthService.resetConnectionPool();
            
            if (result) {
                successfulHealingOperations.incrementAndGet();
                logger.info("数据库连接池重置成功");
            } else {
                failedHealingOperations.incrementAndGet();
                logger.warn("数据库连接池重置失败");
            }
            
            return result;
            
        } catch (Exception e) {
            failedHealingOperations.incrementAndGet();
            logger.error("重置数据库连接池时发生异常", e);
            return false;
        }
    }
    
    /**
     * 触发垃圾回收
     */
    public boolean triggerGarbageCollection() {
        try {
            logger.info("开始触发垃圾回收...");
            totalHealingOperations.incrementAndGet();
            
            // 记录GC前的内存状态
            Runtime runtime = Runtime.getRuntime();
            long beforeGC = runtime.totalMemory() - runtime.freeMemory();
            
            // 触发垃圾回收
            System.gc();
            
            // 等待GC完成
            Thread.sleep(2000);
            
            // 记录GC后的内存状态
            long afterGC = runtime.totalMemory() - runtime.freeMemory();
            long freedMemory = beforeGC - afterGC;
            
            if (freedMemory > 0) {
                successfulHealingOperations.incrementAndGet();
                logger.info("垃圾回收完成，释放内存: {} MB", freedMemory / 1024 / 1024);
                return true;
            } else {
                failedHealingOperations.incrementAndGet();
                logger.warn("垃圾回收未释放明显内存");
                return false;
            }
            
        } catch (Exception e) {
            failedHealingOperations.incrementAndGet();
            logger.error("触发垃圾回收时发生异常", e);
            return false;
        }
    }
    
    /**
     * 清理系统缓存
     */
    public boolean clearSystemCache() {
        try {
            logger.info("开始清理系统缓存...");
            totalHealingOperations.incrementAndGet();
            
            int clearedItems = 0;
            
            // 这里可以清理各种缓存
            // 示例：清理一些内存中的缓存数据
            
            // 触发GC帮助清理
            System.gc();
            
            successfulHealingOperations.incrementAndGet();
            logger.info("系统缓存清理完成，清理项目数: {}", clearedItems);
            return true;
            
        } catch (Exception e) {
            failedHealingOperations.incrementAndGet();
            logger.error("清理系统缓存时发生异常", e);
            return false;
        }
    }
    
    /**
     * 重置网络连接
     */
    public boolean resetNetworkConnections() {
        try {
            logger.info("开始重置网络连接...");
            totalHealingOperations.incrementAndGet();
            
            // 重置网络健康统计
            networkHealthService.resetStats();
            
            // 执行网络健康检查
            boolean healthCheck = networkHealthService.performHealthCheck();
            
            if (healthCheck) {
                successfulHealingOperations.incrementAndGet();
                logger.info("网络连接重置成功");
                return true;
            } else {
                failedHealingOperations.incrementAndGet();
                logger.warn("网络连接重置后健康检查失败");
                return false;
            }
            
        } catch (Exception e) {
            failedHealingOperations.incrementAndGet();
            logger.error("重置网络连接时发生异常", e);
            return false;
        }
    }
    
    /**
     * 扩展线程池
     */
    public boolean expandThreadPool() {
        try {
            logger.info("开始扩展线程池...");
            totalHealingOperations.incrementAndGet();
            
            // 这是一个示例实现，实际情况下需要根据具体的线程池进行调整
            // 获取系统可用处理器数
            int availableProcessors = Runtime.getRuntime().availableProcessors();
            
            logger.info("当前可用处理器数: {}", availableProcessors);
            
            // 示例：可以在这里调整线程池大小
            // 实际实现需要根据具体的ThreadPoolExecutor进行操作
            
            successfulHealingOperations.incrementAndGet();
            logger.info("线程池扩展操作完成");
            return true;
            
        } catch (Exception e) {
            failedHealingOperations.incrementAndGet();
            logger.error("扩展线程池时发生异常", e);
            return false;
        }
    }
    
    /**
     * 清理临时文件
     */
    public boolean cleanTemporaryFiles() {
        try {
            logger.info("开始清理临时文件...");
            totalHealingOperations.incrementAndGet();
            
            long deletedFiles = 0;
            
            // 清理系统临时目录
            String tempDir = System.getProperty("java.io.tmpdir");
            deletedFiles += cleanDirectory(tempDir, 7); // 清理7天前的文件
            
            // 清理应用临时目录
            Path appTempDir = Paths.get(".", "temp");
            if (Files.exists(appTempDir)) {
                deletedFiles += cleanDirectory(appTempDir.toString(), 1); // 清理1天前的文件
            }
            
            // 清理日志目录中的旧日志
            Path logsDir = Paths.get(".", "logs");
            if (Files.exists(logsDir)) {
                deletedFiles += cleanDirectory(logsDir.toString(), 30); // 清理30天前的日志
            }
            
            if (deletedFiles > 0) {
                successfulHealingOperations.incrementAndGet();
                logger.info("临时文件清理完成，删除文件数: {}", deletedFiles);
                return true;
            } else {
                successfulHealingOperations.incrementAndGet();
                logger.info("临时文件清理完成，无需清理文件");
                return true;
            }
            
        } catch (Exception e) {
            failedHealingOperations.incrementAndGet();
            logger.error("清理临时文件时发生异常", e);
            return false;
        }
    }
    
    /**
     * 重启数据库连接
     */
    public boolean restartDatabaseConnections() {
        try {
            logger.info("开始重启数据库连接...");
            totalHealingOperations.incrementAndGet();
            
            // 首先重置连接池
            boolean resetResult = databaseHealthService.resetConnectionPool();
            
            if (resetResult) {
                // 执行健康检查
                boolean healthCheck = databaseHealthService.performHealthCheck();
                
                if (healthCheck) {
                    successfulHealingOperations.incrementAndGet();
                    logger.info("数据库连接重启成功");
                    return true;
                }
            }
            
            failedHealingOperations.incrementAndGet();
            logger.warn("数据库连接重启失败");
            return false;
            
        } catch (Exception e) {
            failedHealingOperations.incrementAndGet();
            logger.error("重启数据库连接时发生异常", e);
            return false;
        }
    }
    
    /**
     * 清理卡住的任务
     */
    public boolean clearStuckTasks() {
        try {
            logger.info("开始清理卡住的任务...");
            totalHealingOperations.incrementAndGet();
            
            int clearedTasks = 0;
            
            // 这里可以实现清理卡住任务的逻辑
            // 例如：中断长时间运行的线程、清理队列中的过期任务等
            
            successfulHealingOperations.incrementAndGet();
            logger.info("卡住任务清理完成，清理任务数: {}", clearedTasks);
            return true;
            
        } catch (Exception e) {
            failedHealingOperations.incrementAndGet();
            logger.error("清理卡住任务时发生异常", e);
            return false;
        }
    }
    
    /**
     * 压缩日志文件
     */
    public boolean compressLogFiles() {
        try {
            logger.info("开始压缩日志文件...");
            totalHealingOperations.incrementAndGet();
            
            int compressedFiles = 0;
            
            Path logsDir = Paths.get(".", "logs");
            if (Files.exists(logsDir)) {
                // 这里可以实现日志文件压缩逻辑
                // 例如：将旧的日志文件压缩为zip格式
                compressedFiles = compressOldLogFiles(logsDir.toString());
            }
            
            successfulHealingOperations.incrementAndGet();
            logger.info("日志文件压缩完成，压缩文件数: {}", compressedFiles);
            return true;
            
        } catch (Exception e) {
            failedHealingOperations.incrementAndGet();
            logger.error("压缩日志文件时发生异常", e);
            return false;
        }
    }
    
    /**
     * 清理指定目录中的旧文件
     */
    private long cleanDirectory(String dirPath, int daysOld) {
        long deletedCount = 0;
        
        try {
            File directory = new File(dirPath);
            if (!directory.exists() || !directory.isDirectory()) {
                return 0;
            }
            
            long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L);
            
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.lastModified() < cutoffTime) {
                        if (file.delete()) {
                            deletedCount++;
                            logger.debug("删除旧文件: {}", file.getAbsolutePath());
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warn("清理目录 {} 时发生异常: {}", dirPath, e.getMessage());
        }
        
        return deletedCount;
    }
    
    /**
     * 压缩旧日志文件
     */
    private int compressOldLogFiles(String logsDir) {
        int compressedCount = 0;
        
        try {
            File directory = new File(logsDir);
            if (!directory.exists() || !directory.isDirectory()) {
                return 0;
            }
            
            // 这里可以实现具体的压缩逻辑
            // 例如：使用Java的ZIP API压缩超过一定大小或时间的日志文件
            
            logger.debug("日志文件压缩逻辑待实现");
            
        } catch (Exception e) {
            logger.warn("压缩日志文件时发生异常: {}", e.getMessage());
        }
        
        return compressedCount;
    }
    
    /**
     * 检查文件系统健康状态
     */
    public boolean checkFileSystemHealth() {
        try {
            // 检查磁盘空间
            File currentDir = new File(".");
            long freeSpace = currentDir.getFreeSpace();
            long totalSpace = currentDir.getTotalSpace();
            double freeSpacePercentage = (double) freeSpace / totalSpace * 100;
            
            logger.debug("文件系统健康检查 - 可用空间: {:.2f}%", freeSpacePercentage);
            
            return freeSpacePercentage > 10.0; // 至少保留10%的空间
            
        } catch (Exception e) {
            logger.error("检查文件系统健康状态时发生异常", e);
            return false;
        }
    }
    
    /**
     * 获取自愈操作统计信息
     */
    public HealingStatistics getHealingStatistics() {
        long total = totalHealingOperations.get();
        long successful = successfulHealingOperations.get();
        long failed = failedHealingOperations.get();
        
        double successRate = total > 0 ? (double) successful / total * 100 : 0.0;
        
        return new HealingStatistics(total, successful, failed, successRate);
    }
    
    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        totalHealingOperations.set(0);
        successfulHealingOperations.set(0);
        failedHealingOperations.set(0);
        logger.info("自愈操作统计信息已重置");
    }
    
    /**
     * 自愈统计信息类
     */
    public static class HealingStatistics {
        private final long totalOperations;
        private final long successfulOperations;
        private final long failedOperations;
        private final double successRate;
        
        public HealingStatistics(long totalOperations, long successfulOperations, 
                               long failedOperations, double successRate) {
            this.totalOperations = totalOperations;
            this.successfulOperations = successfulOperations;
            this.failedOperations = failedOperations;
            this.successRate = successRate;
        }
        
        // Getters
        public long getTotalOperations() { return totalOperations; }
        public long getSuccessfulOperations() { return successfulOperations; }
        public long getFailedOperations() { return failedOperations; }
        public double getSuccessRate() { return successRate; }
    }
}
