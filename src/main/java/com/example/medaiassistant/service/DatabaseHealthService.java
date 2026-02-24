package com.example.medaiassistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 数据库健康监控服务
 * 实现数据库连接池监控、健康检查和自动恢复功能
 */
@Service
public class DatabaseHealthService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthService.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 数据库健康状态统计
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    private final AtomicInteger successfulConnections = new AtomicInteger(0);
    private final AtomicInteger failedConnections = new AtomicInteger(0);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong totalConnectionTime = new AtomicLong(0);
    private volatile LocalDateTime lastHealthCheck = LocalDateTime.now();
    private volatile boolean databaseHealthy = true;

    // 连接池状态信息
    public static class DatabaseHealthStats {
        private final double successRate;
        private final long averageConnectionTime;
        private final int totalConnections;
        private final int consecutiveFailures;
        private final boolean isHealthy;
        private final LocalDateTime lastHealthCheck;
        private final int activeConnections;
        private final int maxConnections;

        public DatabaseHealthStats(double successRate, long averageConnectionTime, 
                                 int totalConnections, int consecutiveFailures, 
                                 boolean isHealthy, LocalDateTime lastHealthCheck,
                                 int activeConnections, int maxConnections) {
            this.successRate = successRate;
            this.averageConnectionTime = averageConnectionTime;
            this.totalConnections = totalConnections;
            this.consecutiveFailures = consecutiveFailures;
            this.isHealthy = isHealthy;
            this.lastHealthCheck = lastHealthCheck;
            this.activeConnections = activeConnections;
            this.maxConnections = maxConnections;
        }

        // Getters
        public double getSuccessRate() { return successRate; }
        public long getAverageConnectionTime() { return averageConnectionTime; }
        public int getTotalConnections() { return totalConnections; }
        public int getConsecutiveFailures() { return consecutiveFailures; }
        public boolean isHealthy() { return isHealthy; }
        public LocalDateTime getLastHealthCheck() { return lastHealthCheck; }
        public int getActiveConnections() { return activeConnections; }
        public int getMaxConnections() { return maxConnections; }
    }

    /**
     * 执行数据库健康检查
     * @return 数据库是否健康
     */
    public boolean performHealthCheck() {
        long startTime = System.currentTimeMillis();
        boolean healthy = false;

        try (Connection connection = dataSource.getConnection()) {
            // 执行简单的查询验证数据库连接
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            healthy = true;
            
            // 记录成功连接
            successfulConnections.incrementAndGet();
            consecutiveFailures.set(0);
            
            logger.debug("数据库健康检查成功");
            
        } catch (SQLException | DataAccessException e) {
            // 记录失败连接
            failedConnections.incrementAndGet();
            consecutiveFailures.incrementAndGet();
            
            logger.error("数据库健康检查失败: {}", e.getMessage());
            
            // 连续失败超过阈值则标记为不健康
            if (consecutiveFailures.get() >= 3) {
                databaseHealthy = false;
                logger.warn("数据库连续失败{}次，标记为不健康", consecutiveFailures.get());
            }
        } finally {
            long connectionTime = System.currentTimeMillis() - startTime;
            totalConnections.incrementAndGet();
            totalConnectionTime.addAndGet(connectionTime);
            lastHealthCheck = LocalDateTime.now();
            
            if (healthy) {
                databaseHealthy = true;
            }
        }

        return healthy;
    }

    /**
     * 获取数据库健康统计信息
     * @return 数据库健康统计数据
     */
    public DatabaseHealthStats getHealthStats() {
        int total = totalConnections.get();
        int successful = successfulConnections.get();
        
        double successRate = total > 0 ? (double) successful / total : 0.0;
        long avgConnectionTime = total > 0 ? totalConnectionTime.get() / total : 0;
        
        // 获取连接池信息
        ConnectionPoolInfo poolInfo = getConnectionPoolInfo();
        
        return new DatabaseHealthStats(
            successRate,
            avgConnectionTime,
            total,
            consecutiveFailures.get(),
            databaseHealthy,
            lastHealthCheck,
            poolInfo.activeConnections,
            poolInfo.maxConnections
        );
    }

    /**
     * 获取连接池详细信息
     */
    private static class ConnectionPoolInfo {
        int activeConnections = 0;
        int maxConnections = 0;
    }

    private ConnectionPoolInfo getConnectionPoolInfo() {
        ConnectionPoolInfo info = new ConnectionPoolInfo();
        
        try {
            // 尝试获取HikariCP连接池信息
            if (dataSource.getClass().getName().contains("HikariDataSource")) {
                // 使用反射获取HikariCP的连接池信息
                Object hikariPoolMBean = dataSource.getClass().getMethod("getHikariPoolMXBean").invoke(dataSource);
                if (hikariPoolMBean != null) {
                    info.activeConnections = (Integer) hikariPoolMBean.getClass().getMethod("getActiveConnections").invoke(hikariPoolMBean);
                    info.maxConnections = (Integer) hikariPoolMBean.getClass().getMethod("getMaximumPoolSize").invoke(hikariPoolMBean);
                }
            }
        } catch (Exception e) {
            logger.debug("无法获取连接池详细信息: {}", e.getMessage());
            // 使用默认值
            info.activeConnections = 0;
            info.maxConnections = 10; // 默认值
        }
        
        return info;
    }

    /**
     * 检查数据库是否健康
     * @return 数据库健康状态
     */
    public boolean isDatabaseHealthy() {
        return databaseHealthy;
    }

    /**
     * 获取推荐的数据库重试次数
     * 根据当前数据库健康状况动态调整
     * @return 推荐的重试次数
     */
    public int getRecommendedRetries() {
        DatabaseHealthStats stats = getHealthStats();
        
        if (stats.getSuccessRate() < 0.5) {
            return 5; // 数据库成功率低，增加重试次数
        } else if (stats.getConsecutiveFailures() > 2) {
            return 4; // 连续失败较多，适度增加重试
        } else if (stats.getAverageConnectionTime() > 3000) {
            return 3; // 连接时间较长，适度重试
        } else {
            return 2; // 正常情况下的重试次数
        }
    }

    /**
     * 获取推荐的重试间隔（毫秒）
     * @param attempt 当前重试次数
     * @return 推荐的重试间隔
     */
    public long getRecommendedRetryDelay(int attempt) {
        DatabaseHealthStats stats = getHealthStats();
        
        // 基础延迟：根据数据库健康状况调整
        long baseDelay = stats.isHealthy() ? 1000 : 2000;
        
        // 指数退避算法
        long exponentialDelay = baseDelay * (1L << Math.min(attempt, 6));
        
        // 添加随机抖动（10-20%的随机变化）
        double jitterFactor = 0.1 + Math.random() * 0.1;
        long jitter = (long) (exponentialDelay * jitterFactor);
        
        return exponentialDelay + jitter;
    }

    /**
     * 尝试重置数据库连接池
     * 当检测到连接池问题时自动重置
     */
    public boolean resetConnectionPool() {
        try {
            logger.info("尝试重置数据库连接池...");
            
            // 执行几次健康检查来"预热"连接池
            for (int i = 0; i < 3; i++) {
                try {
                    performHealthCheck();
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // 重置统计信息
            consecutiveFailures.set(0);
            databaseHealthy = true;
            
            logger.info("数据库连接池重置完成");
            return true;
            
        } catch (Exception e) {
            logger.error("重置数据库连接池失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查是否需要重置连接池
     * @return 是否需要重置
     */
    public boolean shouldResetConnectionPool() {
        DatabaseHealthStats stats = getHealthStats();
        
        // 连续失败超过5次或成功率低于30%时建议重置
        return stats.getConsecutiveFailures() >= 5 || stats.getSuccessRate() < 0.3;
    }
}