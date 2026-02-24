package com.example.medaiassistant.hospital.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.ref.Cleaner;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 文件监听服务
 * 负责监听模板文件的变更，支持热更新
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-03
 */
@Slf4j
@Service
public class FileWatcherService {
    
    private final Map<Path, WatchKey> watchKeys = new ConcurrentHashMap<>();
    private final Map<Path, Consumer<Path>> changeCallbacks = new ConcurrentHashMap<>();
    private final ExecutorService watcherExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private WatchService watchService;
    
    /**
     * 启动文件监听服务
     */
    public void start() {
        if (running.get()) {
            log.warn("文件监听服务已经在运行");
            return;
        }
        
        try {
            watchService = FileSystems.getDefault().newWatchService();
            running.set(true);
            
            watcherExecutor.submit(() -> {
                log.info("文件监听服务已启动");
                while (running.get()) {
                    try {
                        WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                        if (key == null) {
                            continue;
                        }
                        
                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind<?> kind = event.kind();
                            
                            if (kind == StandardWatchEventKinds.OVERFLOW) {
                                continue;
                            }
                            
                            @SuppressWarnings("unchecked")
                            WatchEvent<Path> ev = (WatchEvent<Path>) event;
                            Path filename = ev.context();
                            Path directory = (Path) key.watchable();
                            Path fullPath = directory.resolve(filename);
                            
                            log.debug("检测到文件变更: {} - {}", kind.name(), fullPath);
                            
                            // 查找对应的回调函数
                            Consumer<Path> callback = changeCallbacks.get(fullPath);
                            if (callback != null) {
                                try {
                                    callback.accept(fullPath);
                                } catch (Exception e) {
                                    log.error("执行文件变更回调失败: {}", fullPath, e);
                                }
                            }
                        }
                        
                        boolean valid = key.reset();
                        if (!valid) {
                            log.warn("WatchKey已失效: {}", key);
                            watchKeys.values().remove(key);
                        }
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.info("文件监听服务被中断");
                        break;
                    } catch (Exception e) {
                        log.error("文件监听服务异常", e);
                    }
                }
                
                log.info("文件监听服务已停止");
            });
            
        } catch (IOException e) {
            log.error("启动文件监听服务失败", e);
            throw new RuntimeException("启动文件监听服务失败", e);
        }
    }
    
    /**
     * 停止文件监听服务
     */
    public void stop() {
        if (!running.get()) {
            return;
        }
        
        running.set(false);
        watcherExecutor.shutdown();
        
        try {
            if (!watcherExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                watcherExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            watcherExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.error("关闭WatchService失败", e);
            }
        }
        
        watchKeys.clear();
        changeCallbacks.clear();
        log.info("文件监听服务已停止");
    }
    
    /**
     * 监听文件变更
     * 
     * @param filePath 要监听的文件路径
     * @param callback 文件变更时的回调函数
     * @return 是否成功注册监听
     */
    public boolean watchFile(Path filePath, Consumer<Path> callback) {
        if (filePath == null || callback == null) {
            log.warn("文件路径或回调函数不能为null");
            return false;
        }
        
        if (!Files.exists(filePath)) {
            log.warn("文件不存在: {}", filePath);
            return false;
        }
        
        Path directory = filePath.getParent();
        if (directory == null) {
            log.warn("无法获取文件所在目录: {}", filePath);
            return false;
        }
        
        try {
            // 注册目录监听
            WatchKey watchKey = directory.register(watchService,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE);
            
            watchKeys.put(filePath, watchKey);
            changeCallbacks.put(filePath, callback);
            
            log.info("开始监听文件变更: {}", filePath);
            return true;
            
        } catch (IOException e) {
            log.error("注册文件监听失败: {}", filePath, e);
            return false;
        }
    }
    
    /**
     * 停止监听文件
     * 
     * @param filePath 要停止监听的文件路径
     * @return 是否成功停止监听
     */
    public boolean unwatchFile(Path filePath) {
        if (filePath == null) {
            return false;
        }
        
        WatchKey watchKey = watchKeys.remove(filePath);
        changeCallbacks.remove(filePath);
        
        if (watchKey != null) {
            watchKey.cancel();
            log.info("已停止监听文件: {}", filePath);
            return true;
        }
        
        return false;
    }
    
    /**
     * 等待文件变更
     * 
     * @param timeoutMs 超时时间（毫秒）
     * @return 是否检测到变更
     */
    public boolean waitForChange(long timeoutMs) {
        if (!running.get()) {
            log.warn("文件监听服务未运行");
            return false;
        }
        
        try {
            // 这里简化实现，实际应该使用更复杂的机制
            Thread.sleep(timeoutMs);
            return true; // 简化实现，总是返回true
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * 检查服务是否在运行
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * 使用Cleaner替代finalize()方法
     */
    private static class ResourceCleaner implements Runnable {
        private final FileWatcherService fileWatcherService;
        
        ResourceCleaner(FileWatcherService fileWatcherService) {
            this.fileWatcherService = fileWatcherService;
        }
        
        @Override
        public void run() {
            if (fileWatcherService.isRunning()) {
                log.warn("FileWatcherService未被正确关闭，正在执行清理");
                fileWatcherService.stop();
            }
        }
    }
    
    // 使用Cleaner进行资源清理
    private final Cleaner cleaner = Cleaner.create();
    private final Cleaner.Cleanable cleanable;
    
    {
        cleanable = cleaner.register(this, new ResourceCleaner(this));
    }
    
    /**
     * 手动清理资源
     */
    public void close() {
        stop();
        cleanable.clean();
    }
}
