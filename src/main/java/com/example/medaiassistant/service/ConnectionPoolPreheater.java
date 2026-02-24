package com.example.medaiassistant.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 连接池预热服务
 * 在系统启动时预先创建连接池连接，避免启动阶段的连接竞争
 * 
 * @author Cline
 * @since 2025-10-10
 * @version 1.0.0
 */
@Component
public class ConnectionPoolPreheater {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionPoolPreheater.class);

    @Autowired
    private DataSource dataSource;

    /**
     * 应用启动完成事件监听器
     * 在Spring上下文完全初始化后预热连接池
     * 
     * @function onApplicationReady
     * @description 预先创建连接池连接，避免启动阶段的连接竞争
     * @param event 上下文刷新事件
     * @returns {void}
     * @since 2025-10-10
     * @author Cline
     * @version 1.0.0
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationReady(ContextRefreshedEvent event) {
        logger.info("开始预热数据库连接池...");
        
        try {
            // 预热连接池 - 创建2个连接（与最小空闲连接数一致）
            preheatConnectionPool(2);
            logger.info("数据库连接池预热完成");
        } catch (Exception e) {
            logger.warn("数据库连接池预热失败: {}", e.getMessage());
            // 预热失败不影响系统启动，继续运行
        }
    }

    /**
     * 预热连接池
     * 预先创建指定数量的连接，避免启动阶段的连接竞争
     * 
     * @function preheatConnectionPool
     * @description 创建指定数量的连接并执行简单查询，确保连接可用
     * @param connectionCount 要预热的连接数量
     * @returns {void}
     * @throws {SQLException} 当数据库连接或查询失败时抛出异常
     * @since 2025-10-10
     * @author Cline
     * @version 1.0.0
     */
    private void preheatConnectionPool(int connectionCount) throws SQLException {
        for (int i = 0; i < connectionCount; i++) {
            try (Connection conn = dataSource.getConnection()) {
                // 执行简单查询预热连接
                conn.createStatement().execute("SELECT 1 FROM DUAL");
                logger.debug("连接池预热 - 连接 {} 创建成功", i + 1);
                
                // 短暂延迟，避免连接创建过于集中
                if (i < connectionCount - 1) {
                    try {
                        Thread.sleep(100); // 100ms延迟
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (SQLException e) {
                logger.error("连接池预热 - 连接 {} 创建失败: {}", i + 1, e.getMessage());
                throw e;
            }
        }
    }

    /**
     * 手动预热连接池（供API调用）
     * 
     * @function manualPreheat
     * @description 手动触发连接池预热，用于系统维护或性能测试
     * @param connectionCount 要预热的连接数量
     * @returns {boolean} 预热是否成功
     * @since 2025-10-10
     * @author Cline
     * @version 1.0.0
     */
    public boolean manualPreheat(int connectionCount) {
        logger.info("手动预热数据库连接池，连接数量: {}", connectionCount);
        
        try {
            preheatConnectionPool(connectionCount);
            logger.info("手动预热数据库连接池完成");
            return true;
        } catch (Exception e) {
            logger.error("手动预热数据库连接池失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查连接池健康状态
     * 
     * @function checkConnectionPoolHealth
     * @description 检查连接池的健康状态，验证连接是否可用
     * @returns {boolean} 连接池是否健康
     * @since 2025-10-10
     * @author Cline
     * @version 1.0.0
     */
    public boolean checkConnectionPoolHealth() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5); // 5秒验证超时
        } catch (Exception e) {
            logger.error("连接池健康检查失败: {}", e.getMessage());
            return false;
        }
    }
}
