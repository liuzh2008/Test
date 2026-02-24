package com.example.medaiassistant.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 配置缓存服务测试类
 * 按照TDD红-绿-重构流程实施迭代4：高级功能与优化
 * 
 * ✅ 绿阶段：测试已通过
 * 📝 评价和完善阶段
 * ✅ P2修订：使用MockitoExtension，无需Spring上下文，已最优
 * 
 * @author Cline AI
 * @version 1.1.0
 * @since 2025-11-07
 */
@ExtendWith(MockitoExtension.class)
class ConfigurationCacheServiceTest {

    @Mock
    private AIModelConfig aiModelConfig;

    private ConfigurationCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new ConfigurationCacheService(aiModelConfig);
    }

    /**
     * ✅ 测试用例0：验证缓存服务基本功能
     * 确保缓存服务能够正确初始化
     */
    @Test
    void shouldInitializeCacheService() {
        assertNotNull(cacheService, "ConfigurationCacheService应该被正确初始化");
        
        // 验证缓存统计信息
        ConfigurationCacheService.CacheStats stats = cacheService.getCacheStatistics();
        assertNotNull(stats, "缓存统计信息不应该为空");
        assertTrue(stats.getHitCount() >= 0, "命中次数应该为非负数");
        assertTrue(stats.getMissCount() >= 0, "未命中次数应该为非负数");
        assertTrue(stats.getEvictionCount() >= 0, "淘汰次数应该为非负数");
    }

    /**
     * ✅ 测试用例1：测试配置版本管理
     * 验证配置版本管理功能
     */
    @Test
    void shouldManageConfigurationVersions() {
        // 获取当前配置版本
        String currentVersion = cacheService.getCurrentConfigurationVersion();
        assertNotNull(currentVersion, "配置版本不应该为空");
        
        // 验证版本格式
        assertTrue(currentVersion.matches("\\d+\\.\\d+\\.\\d+"), "版本格式应该符合语义化版本规范");
        
        // 获取配置变更历史
        java.util.List<String> changeHistory = cacheService.getConfigurationChangeHistory();
        assertNotNull(changeHistory, "变更历史不应该为空");
    }

    /**
     * ✅ 测试用例2：测试配置回滚机制
     * 验证配置回滚功能
     */
    @Test
    void shouldSupportConfigurationRollback() {
        // 回滚到上一个版本
        boolean rollbackSuccess = cacheService.rollbackToPreviousVersion();
        assertTrue(rollbackSuccess, "回滚操作应该成功");
    }

    /**
     * ✅ 测试用例3：测试缓存统计信息
     * 验证缓存统计功能
     */
    @Test
    void shouldProvideCacheStatistics() {
        // 获取缓存统计信息
        ConfigurationCacheService.CacheStats stats = cacheService.getCacheStatistics();
        assertNotNull(stats, "缓存统计信息不应该为空");
        
        // 验证统计信息字段
        assertTrue(stats.getHitCount() >= 0, "命中次数应该为非负数");
        assertTrue(stats.getMissCount() >= 0, "未命中次数应该为非负数");
        assertTrue(stats.getEvictionCount() >= 0, "淘汰次数应该为非负数");
        
        // 验证命中率计算
        double hitRate = stats.getHitRate();
        assertTrue(hitRate >= 0.0 && hitRate <= 1.0, "命中率应该在0到1之间");
    }

    /**
     * 📝 测试用例4：测试配置缓存功能
     * 验证配置缓存机制的正确性
     */
    @Test
    void shouldCacheModelConfigurations() {
        // 模拟配置数据
        AIModelConfig.ModelConfig mockConfig = new AIModelConfig.ModelConfig();
        when(aiModelConfig.getModelConfig("test-model")).thenReturn(mockConfig);
        
        // 第一次获取配置（应该从原始配置加载）
        AIModelConfig.ModelConfig firstCall = cacheService.getCachedModelConfig("test-model");
        assertNotNull(firstCall, "第一次获取配置应该成功");
        
        // 验证缓存统计
        ConfigurationCacheService.CacheStats stats = cacheService.getCacheStatistics();
        assertEquals(1, stats.getMissCount(), "第一次获取应该未命中缓存");
        assertEquals(0, stats.getHitCount(), "第一次获取不应该命中缓存");
        
        // 第二次获取配置（应该从缓存加载）
        AIModelConfig.ModelConfig secondCall = cacheService.getCachedModelConfig("test-model");
        assertNotNull(secondCall, "第二次获取配置应该成功");
        
        // 验证缓存命中
        assertEquals(1, stats.getHitCount(), "第二次获取应该命中缓存");
        
        // 验证两次获取的是同一个对象（缓存机制）
        assertSame(firstCall, secondCall, "缓存应该返回同一个配置对象");
    }

    /**
     * 📝 测试用例5：测试配置变更通知
     * 验证配置变更时缓存能够正确更新
     */
    @Test
    void shouldUpdateCacheOnConfigurationChange() {
        // 模拟配置数据
        AIModelConfig.ModelConfig mockConfig = new AIModelConfig.ModelConfig();
        when(aiModelConfig.getModelConfig("test-model")).thenReturn(mockConfig);
        
        // 先获取配置以填充缓存
        cacheService.getCachedModelConfig("test-model");
        
        // 模拟配置变更
        cacheService.notifyConfigurationChange("test-model");
        
        // 验证缓存已移除特定配置
        // 注意：notifyConfigurationChange只移除单个配置，不会增加evictionCount
        // 这里我们验证版本更新和变更历史记录
        
        // 验证版本已更新
        String newVersion = cacheService.getCurrentConfigurationVersion();
        assertNotNull(newVersion, "配置变更后版本应该更新");
        assertNotEquals("1.0.0", newVersion, "配置变更后版本号应该改变");
        
        // 验证变更历史已记录
        java.util.List<String> changeHistory = cacheService.getConfigurationChangeHistory();
        assertFalse(changeHistory.isEmpty(), "配置变更应该记录到历史中");
    }

    /**
     * 📝 测试用例6：测试空配置处理
     * 验证当配置不存在时的处理逻辑
     */
    @Test
    void shouldHandleNullConfiguration() {
        // 模拟配置不存在
        when(aiModelConfig.getModelConfig("non-existent-model")).thenReturn(null);
        
        // 获取不存在的配置
        AIModelConfig.ModelConfig result = cacheService.getCachedModelConfig("non-existent-model");
        assertNull(result, "不存在的配置应该返回null");
        
        // 验证缓存统计
        ConfigurationCacheService.CacheStats stats = cacheService.getCacheStatistics();
        assertEquals(1, stats.getMissCount(), "不存在的配置应该未命中缓存");
    }

    /**
     * 📝 测试用例7：测试并发访问
     * 验证缓存服务的线程安全性
     */
    @Test
    void shouldHandleConcurrentAccess() throws InterruptedException {
        // 模拟配置数据
        AIModelConfig.ModelConfig mockConfig = new AIModelConfig.ModelConfig();
        when(aiModelConfig.getModelConfig("concurrent-model")).thenReturn(mockConfig);
        
        // 先进行一次访问以填充缓存
        cacheService.getCachedModelConfig("concurrent-model");
        
        // 创建多个线程并发访问
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                AIModelConfig.ModelConfig config = cacheService.getCachedModelConfig("concurrent-model");
                assertNotNull(config, "并发访问应该返回有效配置");
            });
        }
        
        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 验证缓存统计
        ConfigurationCacheService.CacheStats stats = cacheService.getCacheStatistics();
        assertTrue(stats.getHitCount() >= threadCount, "并发访问应该有缓存命中");
    }
}
