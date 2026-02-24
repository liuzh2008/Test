package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.model.SqlTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 线程安全模板管理器
 * 负责管理模板的并发访问和更新
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-03
 */
@Slf4j
@Component
public class ThreadSafeTemplateManager {
    
    private final TemplateCache templateCache;
    private final Map<String, ReentrantLock> fileLocks = new ConcurrentHashMap<>();
    private final AtomicInteger concurrentUpdates = new AtomicInteger(0);
    private final AtomicInteger totalUpdates = new AtomicInteger(0);
    
    public ThreadSafeTemplateManager(TemplateCache templateCache) {
        this.templateCache = templateCache;
    }
    
    /**
     * 处理文件变更事件（线程安全）
     * 
     * @param filePath 变更的文件路径
     */
    public void handleFileChange(String filePath) {
        ReentrantLock fileLock = fileLocks.computeIfAbsent(filePath, k -> new ReentrantLock());
        
        concurrentUpdates.incrementAndGet();
        totalUpdates.incrementAndGet();
        
        long startTime = System.currentTimeMillis();
        fileLock.lock();
        
        try {
            log.debug("开始处理文件变更: {} (并发更新数: {})", filePath, concurrentUpdates.get());
            
            // 重新加载模板
            SqlTemplate template = templateCache.loadTemplate(filePath);
            
            if (template != null) {
                log.info("文件变更处理成功: {} -> {} (耗时: {}ms)", 
                        filePath, template.getQueryName(), 
                        System.currentTimeMillis() - startTime);
            } else {
                log.warn("文件变更处理失败，模板加载失败: {}", filePath);
            }
            
        } catch (Exception e) {
            log.error("处理文件变更异常: {}", filePath, e);
        } finally {
            fileLock.unlock();
            concurrentUpdates.decrementAndGet();
            
            // 清理长时间未使用的锁
            cleanupOldLocks();
        }
    }
    
    /**
     * 获取模板（线程安全）
     * 
     * @param filePath 模板文件路径
     * @return SQL模板对象
     */
    public SqlTemplate getTemplate(String filePath) {
        return templateCache.getTemplate(filePath);
    }
    
    /**
     * 获取模板（通过查询名称，线程安全）
     * 
     * @param queryName 查询名称
     * @return SQL模板对象
     */
    public SqlTemplate getTemplateByName(String queryName) {
        return templateCache.getTemplateByName(queryName);
    }
    
    /**
     * 加载模板（线程安全）
     * 
     * @param filePath 模板文件路径
     * @return 加载的SQL模板对象
     */
    public SqlTemplate loadTemplate(String filePath) {
        ReentrantLock fileLock = fileLocks.computeIfAbsent(filePath, k -> new ReentrantLock());
        
        fileLock.lock();
        try {
            return templateCache.loadTemplate(filePath);
        } finally {
            fileLock.unlock();
        }
    }
    
    /**
     * 批量处理文件变更
     * 
     * @param filePaths 文件路径数组
     */
    public void handleBatchFileChanges(String[] filePaths) {
        if (filePaths == null || filePaths.length == 0) {
            return;
        }
        
        log.info("开始批量处理 {} 个文件变更", filePaths.length);
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        
        for (String filePath : filePaths) {
            try {
                handleFileChange(filePath);
                successCount++;
            } catch (Exception e) {
                log.error("批量处理文件变更失败: {}", filePath, e);
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("批量处理完成: {}/{} 成功，耗时: {}ms", successCount, filePaths.length, duration);
    }
    
    /**
     * 获取并发更新数量
     */
    public int getConcurrentUpdateCount() {
        return concurrentUpdates.get();
    }
    
    /**
     * 获取总更新数量
     */
    public int getTotalUpdateCount() {
        return totalUpdates.get();
    }
    
    /**
     * 获取活动文件锁数量
     */
    public int getActiveFileLockCount() {
        return fileLocks.size();
    }
    
    /**
     * 清理长时间未使用的锁
     */
    private void cleanupOldLocks() {
        // 简单实现：定期清理，实际可以根据使用频率和最后使用时间进行更智能的清理
        if (fileLocks.size() > 100) { // 如果锁数量过多
            fileLocks.entrySet().removeIf(entry -> 
                !entry.getValue().isLocked() && fileLocks.size() > 50);
            log.debug("清理了未使用的文件锁，剩余: {}", fileLocks.size());
        }
    }
    
    /**
     * 获取文件锁状态
     */
    public Map<String, Boolean> getFileLockStatus() {
        Map<String, Boolean> status = new ConcurrentHashMap<>();
        fileLocks.forEach((filePath, lock) -> {
            status.put(filePath, lock.isLocked());
        });
        return status;
    }
    
    /**
     * 强制释放所有锁（用于紧急情况）
     */
    public void forceReleaseAllLocks() {
        log.warn("强制释放所有文件锁");
        fileLocks.clear();
        concurrentUpdates.set(0);
    }
    
    /**
     * 检查文件是否正在被更新
     */
    public boolean isFileBeingUpdated(String filePath) {
        ReentrantLock lock = fileLocks.get(filePath);
        return lock != null && lock.isLocked();
    }
    
    /**
     * 等待文件更新完成
     * 
     * @param filePath 文件路径
     * @param timeoutMs 超时时间（毫秒）
     * @return 是否在超时前完成
     */
    public boolean waitForFileUpdate(String filePath, long timeoutMs) {
        ReentrantLock lock = fileLocks.get(filePath);
        if (lock == null) {
            return true; // 没有锁，说明没有更新在进行
        }
        
        long startTime = System.currentTimeMillis();
        while (lock.isLocked() && (System.currentTimeMillis() - startTime) < timeoutMs) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return !lock.isLocked();
    }
}
