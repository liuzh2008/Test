package com.example.medaiassistant.service;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库连接池监控服务
 * 监控连接池状态，检测连接泄漏，提供性能指标
 * 
 * @author System
 * @version 1.0
 * @since 2025-09-29
 */
@Service
public class DatabaseConnectionPoolMonitor {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionPoolMonitor.class);

    @Autowired
    private DataSource dataSource;

    /**
     * 获取连接池状态信息
     * 
     * @return 连接池状态信息
     */
    public Map<String, Object> getConnectionPoolStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;

                status.put("poolName", hikariDataSource.getPoolName());
                status.put("activeConnections", hikariDataSource.getHikariPoolMXBean().getActiveConnections());
                status.put("idleConnections", hikariDataSource.getHikariPoolMXBean().getIdleConnections());
                status.put("totalConnections", hikariDataSource.getHikariPoolMXBean().getTotalConnections());
                status.put("threadsAwaitingConnection",
                        hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
                status.put("maximumPoolSize", hikariDataSource.getMaximumPoolSize());
                status.put("minimumIdle", hikariDataSource.getMinimumIdle());
                status.put("connectionTimeout", hikariDataSource.getConnectionTimeout());
                status.put("idleTimeout", hikariDataSource.getIdleTimeout());
                status.put("maxLifetime", hikariDataSource.getMaxLifetime());
                status.put("leakDetectionThreshold", hikariDataSource.getLeakDetectionThreshold());

                // 计算使用率
                double usageRate = calculateUsageRate(hikariDataSource);
                status.put("usageRate", String.format("%.2f%%", usageRate * 100));

                // 检查连接池健康状态
                String healthStatus = checkPoolHealth(hikariDataSource, usageRate);
                status.put("healthStatus", healthStatus);

                logger.debug("连接池状态检查完成 - 活跃连接: {}, 空闲连接: {}, 使用率: {}",
                        status.get("activeConnections"), status.get("idleConnections"), status.get("usageRate"));
            } else {
                status.put("error", "不支持的数据源类型: " + dataSource.getClass().getName());
            }
        } catch (Exception e) {
            logger.error("获取连接池状态失败", e);
            status.put("error", "获取连接池状态失败: " + e.getMessage());
        }

        return status;
    }

    /**
     * 计算连接池使用率
     * 
     * @param dataSource Hikari数据源
     * @return 使用率（0-1之间）
     */
    private double calculateUsageRate(HikariDataSource dataSource) {
        try {
            int activeConnections = dataSource.getHikariPoolMXBean().getActiveConnections();
            int maximumPoolSize = dataSource.getMaximumPoolSize();

            if (maximumPoolSize > 0) {
                return (double) activeConnections / maximumPoolSize;
            }
        } catch (Exception e) {
            logger.warn("计算连接池使用率失败", e);
        }
        return 0.0;
    }

    /**
     * 检查连接池健康状态
     * 
     * @param dataSource Hikari数据源
     * @param usageRate  使用率
     * @return 健康状态描述
     */
    private String checkPoolHealth(HikariDataSource dataSource, double usageRate) {
        try {
            int activeConnections = dataSource.getHikariPoolMXBean().getActiveConnections();
            int threadsAwaiting = dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection();

            if (usageRate >= 0.9) {
                return "CRITICAL - 连接池使用率过高";
            } else if (usageRate >= 0.8) {
                return "WARNING - 连接池使用率较高";
            } else if (threadsAwaiting > 0) {
                return "WARNING - 有线程等待连接";
            } else if (activeConnections == 0) {
                return "HEALTHY - 连接池空闲";
            } else {
                return "HEALTHY - 连接池运行正常";
            }
        } catch (Exception e) {
            logger.warn("检查连接池健康状态失败", e);
            return "UNKNOWN - 无法确定健康状态";
        }
    }

    /**
     * 检测连接泄漏
     * 
     * @return 泄漏检测结果
     */
    public Map<String, Object> detectConnectionLeaks() {
        Map<String, Object> leakInfo = new HashMap<>();

        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;

                int activeConnections = hikariDataSource.getHikariPoolMXBean().getActiveConnections();
                int maximumPoolSize = hikariDataSource.getMaximumPoolSize();

                // 如果活跃连接数接近最大连接数且持续较长时间，可能存在泄漏
                if (activeConnections >= maximumPoolSize * 0.9) {
                    leakInfo.put("leakDetected", true);
                    leakInfo.put("severity", "HIGH");
                    leakInfo.put("message", "连接池接近满载，可能存在连接泄漏");
                    leakInfo.put("activeConnections", activeConnections);
                    leakInfo.put("maximumPoolSize", maximumPoolSize);

                    logger.warn("检测到可能的连接泄漏 - 活跃连接: {}, 最大连接数: {}",
                            activeConnections, maximumPoolSize);
                } else {
                    leakInfo.put("leakDetected", false);
                    leakInfo.put("message", "未检测到明显的连接泄漏");
                }
            } else {
                leakInfo.put("error", "不支持的数据源类型");
            }
        } catch (Exception e) {
            logger.error("检测连接泄漏失败", e);
            leakInfo.put("error", "检测连接泄漏失败: " + e.getMessage());
        }

        return leakInfo;
    }

    /**
     * 定期监控连接池状态
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void scheduledPoolMonitoring() {
        try {
            Map<String, Object> status = getConnectionPoolStatus();
            String healthStatus = (String) status.get("healthStatus");

            if (healthStatus != null && (healthStatus.contains("CRITICAL") || healthStatus.contains("WARNING"))) {
                logger.warn("连接池监控告警 - 状态: {}, 活跃连接: {}, 使用率: {}",
                        healthStatus, status.get("activeConnections"), status.get("usageRate"));
            }

            // 定期检测连接泄漏
            Map<String, Object> leakInfo = detectConnectionLeaks();
            if (leakInfo.containsKey("leakDetected") && (Boolean) leakInfo.get("leakDetected")) {
                logger.error("连接泄漏检测告警 - {}", leakInfo.get("message"));
            }

        } catch (Exception e) {
            logger.error("定期连接池监控失败", e);
        }
    }

    /**
     * 获取连接池性能指标
     * 
     * @return 性能指标
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;

                metrics.put("poolName", hikariDataSource.getPoolName());
                metrics.put("maximumPoolSize", hikariDataSource.getMaximumPoolSize());
                metrics.put("minimumIdle", hikariDataSource.getMinimumIdle());
                metrics.put("connectionTimeout", hikariDataSource.getConnectionTimeout());
                metrics.put("idleTimeout", hikariDataSource.getIdleTimeout());
                metrics.put("maxLifetime", hikariDataSource.getMaxLifetime());
                metrics.put("leakDetectionThreshold", hikariDataSource.getLeakDetectionThreshold());

                // 添加性能统计
                metrics.put("activeConnections", hikariDataSource.getHikariPoolMXBean().getActiveConnections());
                metrics.put("idleConnections", hikariDataSource.getHikariPoolMXBean().getIdleConnections());
                metrics.put("totalConnections", hikariDataSource.getHikariPoolMXBean().getTotalConnections());
                metrics.put("threadsAwaitingConnection",
                        hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());

                // 计算性能指标
                double usageRate = calculateUsageRate(hikariDataSource);
                metrics.put("usageRate", usageRate);
                metrics.put("efficiency", calculatePoolEfficiency(hikariDataSource));

            } else {
                metrics.put("error", "不支持的数据源类型");
            }
        } catch (Exception e) {
            logger.error("获取连接池性能指标失败", e);
            metrics.put("error", "获取性能指标失败: " + e.getMessage());
        }

        return metrics;
    }

    /**
     * 计算连接池效率
     * 
     * @param dataSource Hikari数据源
     * @return 效率指标（0-1之间）
     */
    private double calculatePoolEfficiency(HikariDataSource dataSource) {
        try {
            int activeConnections = dataSource.getHikariPoolMXBean().getActiveConnections();
            int totalConnections = dataSource.getHikariPoolMXBean().getTotalConnections();
            int threadsAwaiting = dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection();

            if (totalConnections > 0) {
                // 效率 = (活跃连接数 / 总连接数) * (1 - 等待线程影响)
                double connectionEfficiency = (double) activeConnections / totalConnections;
                double waitingPenalty = threadsAwaiting > 0 ? 0.1 : 0.0; // 有等待线程时降低效率

                return Math.max(0, connectionEfficiency - waitingPenalty);
            }
        } catch (Exception e) {
            logger.warn("计算连接池效率失败", e);
        }
        return 0.0;
    }
}
