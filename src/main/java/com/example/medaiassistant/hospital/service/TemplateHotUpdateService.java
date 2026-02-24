package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.model.SqlTemplate;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模板热更新服务
 * 完整的模板热更新机制实现
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-03
 */
@Slf4j
@Service
public class TemplateHotUpdateService {
    
    private final JsonTemplateParser jsonTemplateParser;
    private final FileWatcherService fileWatcherService;
    private final TemplateCache templateCache;
    private final ThreadSafeTemplateManager templateManager;
    
    private final Map<String, String> templateFileMapping = new ConcurrentHashMap<>();
    
    public TemplateHotUpdateService(
            JsonTemplateParser jsonTemplateParser,
            FileWatcherService fileWatcherService,
            TemplateCache templateCache,
            ThreadSafeTemplateManager templateManager) {
        this.jsonTemplateParser = jsonTemplateParser;
        this.fileWatcherService = fileWatcherService;
        this.templateCache = templateCache;
        this.templateManager = templateManager;
    }
    
    @PostConstruct
    public void init() {
        // 启动文件监听服务
        fileWatcherService.start();
        log.info("模板热更新服务已初始化");
    }
    
    @PreDestroy
    public void cleanup() {
        // 停止文件监听服务
        fileWatcherService.stop();
        log.info("模板热更新服务已清理");
    }
    
    /**
     * 加载模板并启用热更新
     * 
     * <p>优先从缓存获取模板，缓存中没有才从文件加载，避免重复读取文件。</p>
     * 
     * @param filePath 模板文件路径
     * @return 加载的SQL模板对象
     */
    public SqlTemplate loadTemplate(String filePath) {
        try {
            // 优先从缓存获取，缓存中没有才从文件加载
            SqlTemplate template = templateCache.getTemplate(filePath);
            if (template == null) {
                log.error("加载模板失败: {}", filePath);
                return null;
            }
            
            // 注册文件变更监听（如果还没注册）
            if (!templateFileMapping.containsKey(template.getQueryName())) {
                Path path = Path.of(filePath);
                boolean watchSuccess = fileWatcherService.watchFile(path, changedFile -> {
                    log.info("检测到模板文件变更，触发热更新: {}", changedFile);
                    templateManager.handleFileChange(changedFile.toString());
                });
                
                if (watchSuccess) {
                    // 保存模板名称到文件路径的映射
                    templateFileMapping.put(template.getQueryName(), filePath);
                    log.info("模板已加载并启用热更新: {} -> {}", filePath, template.getQueryName());
                } else {
                    log.warn("无法启用模板热更新，文件监听注册失败: {}", filePath);
                }
            }
            
            return template;
            
        } catch (Exception e) {
            log.error("加载模板异常: {}", filePath, e);
            return null;
        }
    }
    
    /**
     * 获取模板
     * 
     * @param queryName 查询名称
     * @return SQL模板对象
     */
    public SqlTemplate getTemplate(String queryName) {
        // 首先尝试从缓存获取
        SqlTemplate template = templateCache.getTemplateByName(queryName);
        if (template != null) {
            return template;
        }
        
        // 如果缓存中没有，尝试通过文件路径映射查找
        String filePath = templateFileMapping.get(queryName);
        if (filePath != null) {
            return templateCache.getTemplate(filePath);
        }
        
        log.debug("模板未找到: {}", queryName);
        return null;
    }
    
    /**
     * 生成SQL语句
     * 
     * @param queryName 查询名称
     * @param variables 变量映射
     * @return 生成的SQL语句
     */
    public String generateSql(String queryName, Map<String, String> variables) {
        SqlTemplate template = getTemplate(queryName);
        if (template == null) {
            log.error("生成SQL失败，模板未找到: {}", queryName);
            return "";
        }
        
        return jsonTemplateParser.generateSql(template, variables);
    }
    
    /**
     * 重新加载所有模板
     * @return 成功重新加载的模板数量
     */
    public int reloadAllTemplates() {
        log.info("开始重新加载所有模板");
        int reloadedCount = templateCache.reloadAllTemplates();
        log.info("重新加载了 {} 个模板", reloadedCount);
        return reloadedCount;
    }
    
    /**
     * 手动触发模板热更新
     * 
     * @param filePath 模板文件路径
     * @return 是否成功触发更新
     */
    public boolean triggerHotUpdate(String filePath) {
        log.info("手动触发模板热更新: {}", filePath);
        templateManager.handleFileChange(filePath);
        return true;
    }
    
    /**
     * 手动触发模板热更新（通过查询名称）
     * 
     * @param queryName 查询名称
     * @return 是否成功触发更新
     */
    public boolean triggerHotUpdateByName(String queryName) {
        String filePath = templateFileMapping.get(queryName);
        if (filePath == null) {
            log.error("无法触发热更新，未找到模板对应的文件路径: {}", queryName);
            return false;
        }
        
        return triggerHotUpdate(filePath);
    }
    
    /**
     * 获取服务状态
     */
    public ServiceStatus getServiceStatus() {
        ServiceStatus status = new ServiceStatus();
        status.setFileWatcherRunning(fileWatcherService.isRunning());
        status.setCachedTemplateCount(templateCache.getCacheSize());
        status.setConcurrentUpdateCount(templateManager.getConcurrentUpdateCount());
        status.setTotalUpdateCount(templateManager.getTotalUpdateCount());
        status.setActiveFileLockCount(templateManager.getActiveFileLockCount());
        status.setTemplateFileMappingCount(templateFileMapping.size());
        return status;
    }
    
    /**
     * 服务状态类
     */
    public static class ServiceStatus {
        private boolean fileWatcherRunning;
        private int cachedTemplateCount;
        private int concurrentUpdateCount;
        private int totalUpdateCount;
        private int activeFileLockCount;
        private int templateFileMappingCount;
        
        // Getters and setters
        public boolean isFileWatcherRunning() {
            return fileWatcherRunning;
        }
        
        public void setFileWatcherRunning(boolean fileWatcherRunning) {
            this.fileWatcherRunning = fileWatcherRunning;
        }
        
        public int getCachedTemplateCount() {
            return cachedTemplateCount;
        }
        
        public void setCachedTemplateCount(int cachedTemplateCount) {
            this.cachedTemplateCount = cachedTemplateCount;
        }
        
        public int getConcurrentUpdateCount() {
            return concurrentUpdateCount;
        }
        
        public void setConcurrentUpdateCount(int concurrentUpdateCount) {
            this.concurrentUpdateCount = concurrentUpdateCount;
        }
        
        public int getTotalUpdateCount() {
            return totalUpdateCount;
        }
        
        public void setTotalUpdateCount(int totalUpdateCount) {
            this.totalUpdateCount = totalUpdateCount;
        }
        
        public int getActiveFileLockCount() {
            return activeFileLockCount;
        }
        
        public void setActiveFileLockCount(int activeFileLockCount) {
            this.activeFileLockCount = activeFileLockCount;
        }
        
        public int getTemplateFileMappingCount() {
            return templateFileMappingCount;
        }
        
        public void setTemplateFileMappingCount(int templateFileMappingCount) {
            this.templateFileMappingCount = templateFileMappingCount;
        }
        
        @Override
        public String toString() {
            return "ServiceStatus{" +
                    "fileWatcherRunning=" + fileWatcherRunning +
                    ", cachedTemplateCount=" + cachedTemplateCount +
                    ", concurrentUpdateCount=" + concurrentUpdateCount +
                    ", totalUpdateCount=" + totalUpdateCount +
                    ", activeFileLockCount=" + activeFileLockCount +
                    ", templateFileMappingCount=" + templateFileMappingCount +
                    '}';
        }
    }
    
    /**
     * 验证模板文件
     * 
     * @param filePath 模板文件路径
     * @return 验证结果
     */
    public ValidationResult validateTemplateFile(String filePath) {
        ValidationResult result = new ValidationResult();
        result.setFilePath(filePath);
        
        try {
            SqlTemplate template = jsonTemplateParser.parseFromFile(Path.of(filePath));
            if (template == null) {
                result.setValid(false);
                result.setMessage("模板解析失败");
                return result;
            }
            
            boolean isValid = jsonTemplateParser.validateTemplate(template);
            result.setValid(isValid);
            result.setTemplate(template);
            
            if (isValid) {
                result.setMessage("模板验证通过");
            } else {
                result.setMessage("模板验证失败");
            }
            
        } catch (IOException e) {
            result.setValid(false);
            result.setMessage("文件读取失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private String filePath;
        private boolean valid;
        private String message;
        private SqlTemplate template;
        
        // Getters and setters
        public String getFilePath() {
            return filePath;
        }
        
        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public void setValid(boolean valid) {
            this.valid = valid;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public SqlTemplate getTemplate() {
            return template;
        }
        
        public void setTemplate(SqlTemplate template) {
            this.template = template;
        }
        
        @Override
        public String toString() {
            return "ValidationResult{" +
                    "filePath='" + filePath + '\'' +
                    ", valid=" + valid +
                    ", message='" + message + '\'' +
                    ", template=" + (template != null ? template.getQueryName() : "null") +
                    '}';
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        CacheStats stats = new CacheStats();
        stats.setCacheSize(templateCache.getCacheSize());
        stats.setCachedTemplateNames(templateCache.getCachedTemplateNames());
        return stats;
    }
    
    /**
     * 缓存统计类
     */
    public static class CacheStats {
        private int cacheSize;
        private String[] cachedTemplateNames;
        
        // Getters and setters
        public int getCacheSize() {
            return cacheSize;
        }
        
        public void setCacheSize(int cacheSize) {
            this.cacheSize = cacheSize;
        }
        
        public String[] getCachedTemplateNames() {
            return cachedTemplateNames;
        }
        
        public void setCachedTemplateNames(String[] cachedTemplateNames) {
            this.cachedTemplateNames = cachedTemplateNames;
        }
        
        @Override
        public String toString() {
            return "CacheStats{" +
                    "cacheSize=" + cacheSize +
                    ", cachedTemplateNames=" + (cachedTemplateNames != null ? cachedTemplateNames.length : 0) +
                    '}';
        }
    }
}
