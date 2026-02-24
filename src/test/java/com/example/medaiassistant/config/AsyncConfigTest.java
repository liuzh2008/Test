package com.example.medaiassistant.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 异步配置测试类
 * 测试AsyncConfig配置类的功能
 * 
 * ✅ P2修订：已限定classes和禁用无关组件
 * 注：AsyncConfig依赖SchedulingProperties，需同时加载
 * 
 * @author System
 * @version 1.1
 * @since 2025-11-07
 */
@SpringBootTest(
    classes = {AsyncConfig.class, SchedulingProperties.class},
    properties = {
        // 调度配置（AsyncConfig所需）
        "scheduling.executor.core-pool-size=10",
        "scheduling.executor.max-pool-size=20",
        "scheduling.executor.queue-capacity=200",
        "scheduling.prompt-generation-pool.core-pool-size=5",
        "scheduling.prompt-generation-pool.max-pool-size=10",
        "scheduling.prompt-generation-pool.queue-capacity=100",
        "scheduling.prompt-generation-pool.thread-name-prefix=test-",
        "scheduling.surgery-analysis-pool.core-pool-size=3",
        "scheduling.surgery-analysis-pool.max-pool-size=6",
        "scheduling.surgery-analysis-pool.queue-capacity=50",
        "scheduling.surgery-analysis-pool.thread-name-prefix=test-",
        
        // 禁用Web组件
        "spring.main.web-application-type=none",
        // 禁用调度组件
        "spring.task.scheduling.enabled=false",
        "scheduling.auto-execute.enabled=false",
        // 禁用监控组件
        "monitoring.metrics.enabled=false",
        // 禁用DDL管理
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=none"
    }
)
public class AsyncConfigTest {

    @Autowired
    private AsyncConfig asyncConfig;

    /**
     * 测试异步任务执行器Bean创建
     * 验证异步任务执行器Bean是否成功创建并正确类型
     */
    @Test
    public void testAsyncTaskExecutorBeanCreation() {
        // 测试异步任务执行器Bean是否成功创建
        Executor executor = asyncConfig.taskExecutor();
        assertNotNull(executor, "异步任务执行器不应为null");
        assertTrue(executor instanceof ThreadPoolTaskExecutor, "执行器应为ThreadPoolTaskExecutor类型");
    }

    /**
     * 测试异步任务执行器配置
     * 验证线程池的各项参数配置是否正确
     */
    @Test
    public void testAsyncTaskExecutorConfiguration() {
        // 测试异步任务执行器的配置参数
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.taskExecutor();
        
        // 验证核心线程池大小（使用配置值）
        assertEquals(10, executor.getCorePoolSize(), "核心线程池大小应为10");
        
        // 验证最大线程池大小（使用配置值）
        assertEquals(20, executor.getMaxPoolSize(), "最大线程池大小应为20");
        
        // 验证线程名前缀
        assertTrue(executor.getThreadNamePrefix().startsWith("async-task-"), 
                  "线程名前缀应以'async-task-'开头");
        
        // 验证队列容量（使用配置值）
        assertEquals(200, executor.getQueueCapacity(), "队列容量应为200");
        
        // 验证执行器已初始化（通过检查线程池执行器不为null来间接验证）
        assertNotNull(executor.getThreadPoolExecutor(), "执行器应已初始化");
    }

    /**
     * 测试@EnableAsync注解启用
     * 验证@EnableAsync注解是否正确启用异步功能
     */
    @Test
    public void testAsyncAnnotationEnabled() {
        // 测试@EnableAsync注解是否生效
        // 这个测试主要验证配置类能够正常加载，@EnableAsync注解不会导致配置失败
        assertNotNull(asyncConfig, "AsyncConfig Bean应成功创建");
    }
}
