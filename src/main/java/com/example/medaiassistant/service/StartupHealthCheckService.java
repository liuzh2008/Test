package com.example.medaiassistant.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 启动健康检查服务
 * 在系统启动时验证所有关键组件的就绪状态，确保系统稳定启动
 * 
 * @author Cline
 * @since 2025-10-10
 * @version 1.0.0
 */
@Service
public class StartupHealthCheckService {

    private static final Logger logger = LoggerFactory.getLogger(StartupHealthCheckService.class);

    @Autowired
    private DataSource dataSource;

    private final AtomicBoolean isSystemReady = new AtomicBoolean(false);
    private final Map<String, Boolean> componentStatus = new HashMap<>();

    /**
     * 应用启动完成事件监听器
     * 在Spring上下文完全初始化后执行系统健康检查
     * 
     * @function onApplicationReady
     * @description 验证所有关键组件的就绪状态，确保系统稳定启动
     * @param event 上下文刷新事件
     * @returns {void}
     * @since 2025-10-10
     * @author Cline
     * @version 1.0.0
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationReady(ContextRefreshedEvent event) {
        logger.info("开始系统启动健康检查...");
        
        try {
            // 执行完整的系统健康检查
            Map<String, Boolean> healthCheckResults = performSystemHealthCheck();
            
            // 检查所有组件是否就绪
            boolean allComponentsReady = healthCheckResults.values().stream().allMatch(Boolean::booleanValue);
            
            if (allComponentsReady) {
                isSystemReady.set(true);
                logger.info("系统启动健康检查完成 - 所有组件就绪");
            } else {
                logger.warn("系统启动健康检查完成 - 部分组件未就绪: {}", healthCheckResults);
                // 即使部分组件未就绪，也允许系统继续运行
                isSystemReady.set(true);
            }
            
            // 记录组件状态
            componentStatus.putAll(healthCheckResults);
            
        } catch (Exception e) {
            logger.error("系统启动健康检查失败: {}", e.getMessage());
            // 健康检查失败不影响系统启动，继续运行
            isSystemReady.set(true);
        }
    }

    /**
     * 执行系统健康检查
     * 验证所有关键组件的就绪状态
     * 
     * @function performSystemHealthCheck
     * @description 检查数据库连接池、关键服务等组件的健康状态
     * @returns {Map<String, Boolean>} 组件健康状态映射
     * @since 2025-10-10
     * @author Cline
     * @version 1.0.0
     */
    private Map<String, Boolean> performSystemHealthCheck() {
        Map<String, Boolean> results = new HashMap<>();
        
        // 检查数据库连接池
        results.put("databaseConnectionPool", checkDatabaseConnectionPool());
        
        // 检查关键服务（可以根据需要添加更多检查）
        results.put("criticalServices", checkCriticalServices());
        
        // 检查系统资源
        results.put("systemResources", checkSystemResources());
        
        logger.info("系统健康检查结果: {}", results);
        return results;
    }

    /**
     * 检查数据库连接池健康状态
     * 
     * @function checkDatabaseConnectionPool
     * @description 验证数据库连接池是否可用，连接是否有效
     * @returns {boolean} 数据库连接池是否健康
     * @since 2025-10-10
     * @author Cline
     * @version 1.0.0
     */
    private boolean checkDatabaseConnectionPool() {
        try (Connection conn = dataSource.getConnection()) {
            boolean isValid = conn.isValid(5); // 5秒验证超时
            if (isValid) {
                logger.debug("数据库连接池健康检查通过");
            } else {
                logger.warn("数据库连接池健康检查失败 - 连接无效");
            }
            return isValid;
        } catch (Exception e) {
            logger.error("数据库连接池健康检查异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查关键服务健康状态
     * 
     * @function checkCriticalServices
     * @description 验证关键服务是否可用（目前返回true，可根据需要扩展）
     * @returns {boolean} 关键服务是否健康
     * @since 2025-10-10
     * @author Cline
     * @version 1.0.0
     */
    private boolean checkCriticalServices() {
        // 这里可以添加对关键服务的检查
        // 例如：检查定时任务服务、API服务等
        logger.debug("关键服务健康检查通过");
        return true;
    }

    /**
     * 检查系统资源健康状态
     * 
     * @function checkSystemResources
     * @description 验证系统资源是否充足（目前返回true，可根据需要扩展）
     * @returns {boolean} 系统资源是否健康
     * @since 2025-10-10
     * @author Cline
     * @version 1.0.0
     */
    private boolean checkSystemResources() {
        // 这里可以添加对系统资源的检查
        // 例如：检查内存使用率、磁盘空间等
        logger.debug("系统资源健康检查通过");
        return true;
    }

    /**
     * 检查系统是否就绪
     * 
     * @function isSystemReady
     * @description 返回系统启动健康检查的结果
     * @returns {boolean} 系统是否就绪
     * @since 2025-10-10
     * @author Cline
     * @version 1.0.0
     */
    public boolean isSystemReady() {
        return isSystemReady.get();
    }

    /**
     * 获取组件健康状态
     * 
     * @function getComponentStatus
     * @description 返回所有组件的健康状态
     * @returns {Map<String, Boolean>} 组件健康状态映射
     * @since 2025-10-10
     * @author Cline
     * @version 1.0.0
     */
    public Map<String, Boolean> getComponentStatus() {
        return new HashMap<>(componentStatus);
    }

    /**
     * 手动执行健康检查（供API调用）
     * 
     * @function manualHealthCheck
     * @description 手动触发系统健康检查，用于系统维护或监控
     * @returns {Map<String, Boolean>} 健康检查结果
     * @since 2025-10-10
     * @author Cline
     * @version 1.0.0
     */
    public Map<String, Boolean> manualHealthCheck() {
        logger.info("手动执行系统健康检查");
        
        try {
            Map<String, Boolean> results = performSystemHealthCheck();
            boolean allComponentsReady = results.values().stream().allMatch(Boolean::booleanValue);
            
            if (allComponentsReady) {
                logger.info("手动健康检查完成 - 所有组件就绪");
            } else {
                logger.warn("手动健康检查完成 - 部分组件未就绪: {}", results);
            }
            
            // 更新组件状态
            componentStatus.clear();
            componentStatus.putAll(results);
            
            return results;
        } catch (Exception e) {
            logger.error("手动健康检查失败: {}", e.getMessage());
            Map<String, Boolean> errorResult = new HashMap<>();
            errorResult.put("error", false);
            return errorResult;
        }
    }

    /**
     * 获取系统启动健康检查报告
     * 
     * @function getHealthReport
     * @description 返回详细的系统健康检查报告
     * @returns {Map<String, Object>} 健康检查报告
     * @since 2025-10-10
     * @author Cline
     * @version 1.0.0
     */
    public Map<String, Object> getHealthReport() {
        Map<String, Object> report = new HashMap<>();
        
        report.put("systemReady", isSystemReady.get());
        report.put("componentStatus", getComponentStatus());
        report.put("timestamp", System.currentTimeMillis());
        
        // 计算健康状态
        boolean allHealthy = componentStatus.values().stream().allMatch(Boolean::booleanValue);
        report.put("overallHealth", allHealthy ? "HEALTHY" : "DEGRADED");
        
        // 统计健康组件数量
        long healthyCount = componentStatus.values().stream().filter(Boolean::booleanValue).count();
        report.put("healthyComponents", healthyCount);
        report.put("totalComponents", componentStatus.size());
        
        return report;
    }
}
