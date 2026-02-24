package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.model.SqlTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 模板缓存管理
 * 负责缓存SQL模板，支持热更新
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-03
 */
@Slf4j
@Component
public class TemplateCache {
    
    private final JsonTemplateParser jsonTemplateParser;
    private final Map<String, SqlTemplate> templateCache = new ConcurrentHashMap<>();
    private final Map<String, Path> templatePaths = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    public TemplateCache(JsonTemplateParser jsonTemplateParser) {
        this.jsonTemplateParser = jsonTemplateParser;
    }
    
    /**
     * 获取模板
     * 
     * @param filePath 模板文件路径
     * @return SQL模板对象
     */
    public SqlTemplate getTemplate(String filePath) {
        lock.readLock().lock();
        try {
            SqlTemplate template = templateCache.get(filePath);
            if (template != null) {
                return template;
            }
        } finally {
            lock.readLock().unlock();
        }
        
        // 缓存未命中，加载模板
        return loadTemplate(filePath);
    }
    
    /**
     * 获取模板（通过查询名称）
     * 
     * @param queryName 查询名称
     * @return SQL模板对象
     */
    public SqlTemplate getTemplateByName(String queryName) {
        lock.readLock().lock();
        try {
            return templateCache.values().stream()
                    .filter(template -> queryName.equals(template.getQueryName()))
                    .findFirst()
                    .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 加载模板到缓存
     * 
     * @param filePath 模板文件路径
     * @return 加载的SQL模板对象
     */
    public SqlTemplate loadTemplate(String filePath) {
        lock.writeLock().lock();
        try {
            Path path = Path.of(filePath);
            SqlTemplate template = jsonTemplateParser.parseFromFile(path);
            
            if (template != null && jsonTemplateParser.validateTemplate(template)) {
                templateCache.put(filePath, template);
                templatePaths.put(filePath, path);
                log.info("模板已加载到缓存: {} -> {}", filePath, template.getQueryName());
                return template;
            } else {
                log.error("模板加载或验证失败: {}", filePath);
                return null;
            }
        } catch (IOException e) {
            log.error("加载模板文件失败: {}", filePath, e);
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 处理文件变更事件
     * 
     * @param filePath 变更的文件路径
     */
    public void onFileChanged(String filePath) {
        log.info("检测到模板文件变更，重新加载: {}", filePath);
        
        // 从缓存中移除旧模板
        lock.writeLock().lock();
        try {
            templateCache.remove(filePath);
            templatePaths.remove(filePath);
        } finally {
            lock.writeLock().unlock();
        }
        
        // 重新加载模板
        loadTemplate(filePath);
    }
    
    /**
     * 从缓存中移除模板
     * 
     * @param filePath 模板文件路径
     * @return 是否成功移除
     */
    public boolean removeTemplate(String filePath) {
        lock.writeLock().lock();
        try {
            SqlTemplate removed = templateCache.remove(filePath);
            templatePaths.remove(filePath);
            
            if (removed != null) {
                log.info("模板已从缓存移除: {} -> {}", filePath, removed.getQueryName());
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 清空缓存
     */
    public void clearCache() {
        lock.writeLock().lock();
        try {
            int size = templateCache.size();
            templateCache.clear();
            templatePaths.clear();
            log.info("模板缓存已清空，移除了 {} 个模板", size);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        lock.readLock().lock();
        try {
            return templateCache.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 检查模板是否在缓存中
     */
    public boolean containsTemplate(String filePath) {
        lock.readLock().lock();
        try {
            return templateCache.containsKey(filePath);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取所有缓存的模板查询名称
     */
    public String[] getCachedTemplateNames() {
        lock.readLock().lock();
        try {
            return templateCache.values().stream()
                    .map(SqlTemplate::getQueryName)
                    .toArray(String[]::new);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 重新加载所有缓存中的模板
     * @return 成功重新加载的模板数量
     */
    public int reloadAllTemplates() {
        lock.writeLock().lock();
        try {
            int successCount = 0;
            Map<String, Path> pathsCopy = new ConcurrentHashMap<>(templatePaths);
            
            for (Map.Entry<String, Path> entry : pathsCopy.entrySet()) {
                try {
                    SqlTemplate template = jsonTemplateParser.parseFromFile(entry.getValue());
                    if (template != null && jsonTemplateParser.validateTemplate(template)) {
                        templateCache.put(entry.getKey(), template);
                        successCount++;
                        log.debug("模板重新加载成功: {}", entry.getKey());
                    }
                } catch (IOException e) {
                    log.error("重新加载模板失败: {}", entry.getKey(), e);
                }
            }
            
            log.info("重新加载了 {}/{} 个模板", successCount, pathsCopy.size());
            return successCount;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
