package com.example.medaiassistant.controller;

import com.example.medaiassistant.service.DatabaseConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * 数据库测试控制器
 * 提供数据库连接测试、配置查询和数据库类型切换功能
 * 
 * @author 系统
 * @version 1.0
 * @since 2025-08-31
 */
@RestController
@RequestMapping("/api/db-test")
public class DatabaseTestController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DatabaseConfigService databaseConfigService;

    /**
     * 测试数据库连接状态
     * @return 连接状态信息
     */
    @GetMapping("/connection")
    public ResponseEntity<String> testConnection() {
        try (Connection connection = dataSource.getConnection()) {
            String dbType = databaseConfigService.getCurrentDatabaseType();
            String dbUrl = connection.getMetaData().getURL();
            String dbProduct = connection.getMetaData().getDatabaseProductName();
            String dbVersion = connection.getMetaData().getDatabaseProductVersion();
            
            String message = String.format(
                "数据库连接成功！\n" +
                "数据库类型: %s\n" +
                "连接URL: %s\n" +
                "数据库产品: %s\n" +
                "数据库版本: %s",
                dbType, dbUrl, dbProduct, dbVersion
            );
            
            return ResponseEntity.ok(message);
        } catch (SQLException e) {
            return ResponseEntity.status(500)
                .body("数据库连接失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前数据库配置信息
     * @return 配置信息
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getConfig() {
        Map<String, String> config = databaseConfigService.getDatabaseConfig();
        return ResponseEntity.ok(config);
    }

    /**
     * 切换数据库类型
     * @param dbType 数据库类型 (mysql/oracle)
     * @return 切换结果信息
     */
    @PostMapping("/switch/{dbType}")
    public ResponseEntity<String> switchDatabase(@PathVariable String dbType) {
        try {
            String result = databaseConfigService.switchDatabase(dbType);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 获取当前数据库类型
     * @return 当前数据库类型
     */
    @GetMapping("/current-type")
    public ResponseEntity<String> getCurrentDatabaseType() {
        return ResponseEntity.ok(databaseConfigService.getCurrentDatabaseType());
    }
}
