package com.example.medaiassistant.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Oracle数据库连接测试控制器
 * 提供Oracle数据库连接状态测试和相关信息查询接口
 * 
 * @author System
 * @version 1.0
 * @since 2025-08-31
 */
@RestController
@RequestMapping("/api/oracle-test")
public class OracleConnectionTestController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 测试Oracle数据库连接状态
     * 通过执行简单的SQL查询验证数据库连接是否正常
     * 
     * @return String 连接成功返回"Oracle连接成功!"，失败返回错误信息
     * @apiNote 使用SELECT 'Oracle连接成功!' FROM dual进行连接测试
     * @example 响应示例: "Oracle连接成功!" 或 "Oracle连接失败: ORA-12541: TNS:无监听程序"
     */
    @GetMapping("/connection")
    public String testConnection() {
        try {
            String result = jdbcTemplate.queryForObject("SELECT 'Oracle连接成功!' FROM dual", String.class);
            return result;
        } catch (Exception e) {
            return "Oracle连接失败: " + e.getMessage();
        }
    }

    /**
     * 获取Oracle数据库版本信息
     * 查询Oracle系统表product_component_version获取数据库版本
     * 
     * @return String 包含数据库版本信息的字符串
     * @apiNote 使用SELECT version FROM product_component_version WHERE product LIKE '%Database%'查询版本
     * @example 响应示例: "Oracle数据库版本: 23.0.0.0.0" 或 "获取版本失败: ORA-00942: 表或视图不存在"
     */
    @GetMapping("/version")
    public String getOracleVersion() {
        try {
            String version = jdbcTemplate.queryForObject("SELECT version FROM product_component_version WHERE product LIKE '%Database%'", String.class);
            return "Oracle数据库版本: " + version;
        } catch (Exception e) {
            return "获取版本失败: " + e.getMessage();
        }
    }

    /**
     * 查看当前用户下的所有表
     * 查询Oracle系统视图user_tables获取当前用户拥有的表列表
     * 
     * @return List<Map<String, Object>> 包含表名信息的列表，失败时返回错误信息
     * @apiNote 使用SELECT table_name FROM user_tables ORDER BY table_name查询表信息
     * @example 响应示例: [{"TABLE_NAME": "ALERT_RULES"}, {"TABLE_NAME": "PATIENTS"}] 或 [{"error": "查询表失败: ORA-00942: 表或视图不存在"}]
     */
    @GetMapping("/tables")
    public List<Map<String, Object>> getTables() {
        try {
            return jdbcTemplate.queryForList(
                "SELECT table_name FROM user_tables ORDER BY table_name"
            );
        } catch (Exception e) {
            return List.of(Map.of("error", "查询表失败: " + e.getMessage()));
        }
    }

    /**
     * 查看MEDAI用户信息
     * 查询Oracle系统视图all_users获取MEDAI用户的详细信息
     * 
     * @return List<Map<String, Object>> 包含用户信息的列表，失败时返回错误信息
     * @apiNote 使用SELECT username, user_id, created FROM all_users WHERE username = 'MEDAI'查询用户信息
     * @example 响应示例: [{"USERNAME": "MEDAI", "USER_ID": 123, "CREATED": "2024-01-01"}] 或 [{"error": "查询用户失败: ORA-00942: 表或视图不存在"}]
     */
    @GetMapping("/users")
    public List<Map<String, Object>> getUsers() {
        try {
            return jdbcTemplate.queryForList(
                "SELECT username, user_id, created FROM all_users WHERE username = 'MEDAI'"
            );
        } catch (Exception e) {
            return List.of(Map.of("error", "查询用户失败: " + e.getMessage()));
        }
    }
}
