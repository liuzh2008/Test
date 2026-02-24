package com.example.medaiassistant.controller;

import com.example.medaiassistant.hospital.dto.SqlQueryRequest;
import com.example.medaiassistant.hospital.dto.SqlQueryResult;
import com.example.medaiassistant.hospital.dto.SqlUpdateRequest;
import com.example.medaiassistant.hospital.dto.SqlUpdateResult;
import com.example.medaiassistant.hospital.model.ConfigValidationResult;
import com.example.medaiassistant.hospital.model.HospitalConfig;
import com.example.medaiassistant.hospital.service.DatabaseConnectionTester;
import com.example.medaiassistant.hospital.service.HospitalConfigService;
import com.example.medaiassistant.hospital.service.SqlExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 医院配置测试控制器
 * 提供医院配置的测试、验证和查询功能
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-03
 */
@RestController
@RequestMapping("/api/hospital-config")
public class HospitalConfigTestController {
    
    private static final Logger log = LoggerFactory.getLogger(HospitalConfigTestController.class);
    
    @Autowired
    private HospitalConfigService hospitalConfigService;
    
    @Autowired
    private DatabaseConnectionTester databaseConnectionTester;
    
    @Autowired
    private SqlExecutionService sqlExecutionService;
    
    /**
     * 获取所有医院配置列表
     * 
     * @return 医院配置列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listConfigs() {
        Map<String, Object> response = new HashMap<>();
        
        List<HospitalConfig> configs = hospitalConfigService.getAllConfigs();
        response.put("timestamp", LocalDateTime.now());
        response.put("configCount", configs.size());
        response.put("configs", configs);
        response.put("configDir", hospitalConfigService.getConfigDirPath());
        
        log.info("返回医院配置列表，共 {} 个配置", configs.size());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 根据医院ID获取配置详情
     * 
     * @param hospitalId 医院ID
     * @return 医院配置详情
     */
    @GetMapping("/{hospitalId}")
    public ResponseEntity<Map<String, Object>> getConfig(@PathVariable String hospitalId) {
        Map<String, Object> response = new HashMap<>();
        
        HospitalConfig config = hospitalConfigService.getConfig(hospitalId);
        if (config == null) {
            response.put("error", "未找到医院配置: " + hospitalId);
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(404).body(response);
        }
        
        response.put("timestamp", LocalDateTime.now());
        response.put("hospitalId", hospitalId);
        response.put("config", config);
        
        log.info("返回医院配置详情: {}", hospitalId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 验证医院配置
     * 
     * @param hospitalId 医院ID
     * @return 配置验证结果
     */
    @GetMapping("/{hospitalId}/validate")
    public ResponseEntity<Map<String, Object>> validateConfig(@PathVariable String hospitalId) {
        Map<String, Object> response = new HashMap<>();
        
        HospitalConfig config = hospitalConfigService.getConfig(hospitalId);
        if (config == null) {
            response.put("error", "未找到医院配置: " + hospitalId);
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(404).body(response);
        }
        
        ConfigValidationResult validationResult = hospitalConfigService.validateConfigDetailed(config);
        
        response.put("timestamp", LocalDateTime.now());
        response.put("hospitalId", hospitalId);
        response.put("configName", config.getName());
        response.put("isValid", validationResult.isValid());
        response.put("errors", validationResult.getErrors());
        response.put("validationTime", LocalDateTime.now());
        
        log.info("验证医院配置: {} - 结果: {}", hospitalId, validationResult.isValid() ? "有效" : "无效");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 测试医院数据库连接
     * 
     * @param hospitalId 医院ID
     * @param databaseType 数据库类型 (his/lis)
     * @return 连接测试结果
     */
    @GetMapping("/{hospitalId}/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(
            @PathVariable String hospitalId,
            @RequestParam(defaultValue = "his") String databaseType) {
        
        Map<String, Object> response = new HashMap<>();
        
        HospitalConfig config = hospitalConfigService.getConfig(hospitalId);
        if (config == null) {
            response.put("error", "未找到医院配置: " + hospitalId);
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(404).body(response);
        }
        
        try {
            // 对于HIS数据库，使用现有的testConnection方法
            // 对于LIS数据库，需要创建新的测试逻辑或修改现有方法
            boolean connectionSuccessful;
            String connectionDetails;
            
            if ("his".equalsIgnoreCase(databaseType)) {
                // 使用现有的testConnection方法测试HIS数据库
                com.example.medaiassistant.hospital.model.ConnectionTestResult testResult = 
                    databaseConnectionTester.testConnection(config);
                connectionSuccessful = testResult.isSuccess();
                connectionDetails = "HIS数据库连接测试: " + 
                    (testResult.isSuccess() ? "成功" : testResult.getErrorMessage());
            } else if ("lis".equalsIgnoreCase(databaseType)) {
                // LIS数据库测试 - 简化版，直接测试连接
                connectionSuccessful = testLisConnection(config);
                connectionDetails = "LIS数据库连接测试";
            } else {
                response.put("error", "不支持的数据库类型: " + databaseType + "，支持的类型: his, lis");
                response.put("timestamp", LocalDateTime.now());
                return ResponseEntity.badRequest().body(response);
            }
            
            response.put("timestamp", LocalDateTime.now());
            response.put("hospitalId", hospitalId);
            response.put("configName", config.getName());
            response.put("databaseType", databaseType);
            response.put("connectionSuccessful", connectionSuccessful);
            response.put("connectionDetails", connectionDetails);
            response.put("testTime", LocalDateTime.now());
            
            log.info("测试医院数据库连接: {} - {} - 结果: {}", 
                    hospitalId, databaseType, connectionSuccessful ? "成功" : "失败");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("error", "连接测试异常: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            response.put("hospitalId", hospitalId);
            response.put("databaseType", databaseType);
            response.put("exception", e.getClass().getName());
            
            log.error("医院数据库连接测试异常: {} - {}", hospitalId, databaseType, e);
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 测试LIS数据库连接
     * 
     * @param config 医院配置
     * @return 连接是否成功
     */
    private boolean testLisConnection(HospitalConfig config) {
        if (config.getLisConfig() == null) {
            log.warn("医院 {} 未配置LIS数据库", config.getId());
            return false;
        }
        
        HospitalConfig.LisConfig lisConfig = config.getLisConfig();
        String url = lisConfig.getUrl();
        String username = lisConfig.getUsername();
        String password = lisConfig.getPassword();
        
        if (url == null || username == null || password == null) {
            log.warn("医院 {} LIS数据库配置不完整", config.getId());
            return false;
        }
        
        try {
            // 加载Oracle JDBC驱动
            Class.forName("oracle.jdbc.OracleDriver");
            
            // 测试连接
            try (java.sql.Connection connection = java.sql.DriverManager.getConnection(url, username, password);
                 java.sql.Statement statement = connection.createStatement();
                 java.sql.ResultSet resultSet = statement.executeQuery("SELECT 1 FROM DUAL")) {
                
                if (resultSet.next()) {
                    log.info("医院 {} LIS数据库连接测试成功", config.getId());
                    return true;
                } else {
                    log.warn("医院 {} LIS数据库连接测试返回空结果", config.getId());
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("医院 {} LIS数据库连接测试失败: {}", config.getId(), e.getMessage());
            return false;
        }
    }
    
    /**
     * 重新加载所有医院配置
     * 
     * @return 重新加载结果
     */
    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reloadConfigs() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            int beforeCount = hospitalConfigService.getConfigCount();
            hospitalConfigService.reloadAllConfigs();
            int afterCount = hospitalConfigService.getConfigCount();
            
            response.put("timestamp", LocalDateTime.now());
            response.put("success", true);
            response.put("message", "医院配置重新加载成功");
            response.put("beforeCount", beforeCount);
            response.put("afterCount", afterCount);
            response.put("reloadTime", LocalDateTime.now());
            
            log.info("重新加载医院配置，数量变化: {} -> {}", beforeCount, afterCount);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("error", "重新加载配置失败: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            response.put("success", false);
            response.put("exception", e.getClass().getName());
            
            log.error("重新加载医院配置失败", e);
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取启用的医院配置
     * 
     * @return 启用的医院配置列表
     */
    @GetMapping("/enabled")
    public ResponseEntity<Map<String, Object>> getEnabledConfigs() {
        Map<String, Object> response = new HashMap<>();
        
        List<HospitalConfig> enabledConfigs = hospitalConfigService.getEnabledConfigs();
        List<HospitalConfig> enabledAndSyncConfigs = hospitalConfigService.getEnabledAndSyncEnabledConfigs();
        
        response.put("timestamp", LocalDateTime.now());
        response.put("enabledCount", enabledConfigs.size());
        response.put("enabledAndSyncCount", enabledAndSyncConfigs.size());
        response.put("enabledConfigs", enabledConfigs);
        response.put("enabledAndSyncConfigs", enabledAndSyncConfigs);
        
        log.info("返回启用的医院配置，启用: {} 个，启用且同步: {} 个", 
                enabledConfigs.size(), enabledAndSyncConfigs.size());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 执行SQL查询
     * 
     * @param hospitalId 医院ID
     * @param request 查询请求
     * @return 查询结果
     */
    @PostMapping("/{hospitalId}/execute-query")
    public ResponseEntity<Map<String, Object>> executeQuery(
            @PathVariable String hospitalId,
            @RequestBody SqlQueryRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            SqlQueryResult result = sqlExecutionService.executeQuery(hospitalId, request);
            
            response.put("timestamp", LocalDateTime.now());
            response.put("hospitalId", hospitalId);
            response.put("databaseType", request.getDatabaseType());
            response.put("success", result.isSuccess());
            response.put("executionTimeMs", result.getExecutionTimeMs());
            response.put("rowCount", result.getRowCount());
            
            if (result.isSuccess()) {
                response.put("columns", result.getColumns());
                response.put("data", result.getData());
                if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
                    response.put("warnings", result.getWarnings());
                }
            } else {
                response.put("errorCode", result.getErrorCode());
                response.put("errorMessage", result.getErrorMessage());
            }
            
            response.put("sql", result.getSql());
            
            log.info("SQL查询执行完成 - 医院ID: {}, 数据库类型: {}, 成功: {}, 行数: {}", 
                    hospitalId, request.getDatabaseType(), result.isSuccess(), result.getRowCount());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("timestamp", LocalDateTime.now());
            response.put("hospitalId", hospitalId);
            response.put("success", false);
            response.put("errorCode", "CONTROLLER_ERROR");
            response.put("errorMessage", "控制器处理异常: " + e.getMessage());
            response.put("exception", e.getClass().getName());
            
            log.error("SQL查询控制器异常: {}", hospitalId, e);
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 执行SQL更新
     * 
     * @param hospitalId 医院ID
     * @param request 更新请求
     * @return 更新结果
     */
    @PostMapping("/{hospitalId}/execute-update")
    public ResponseEntity<Map<String, Object>> executeUpdate(
            @PathVariable String hospitalId,
            @RequestBody SqlUpdateRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            SqlUpdateResult result = sqlExecutionService.executeUpdate(hospitalId, request);
            
            response.put("timestamp", LocalDateTime.now());
            response.put("hospitalId", hospitalId);
            response.put("databaseType", request.getDatabaseType());
            response.put("success", result.isSuccess());
            response.put("executionTimeMs", result.getExecutionTimeMs());
            response.put("affectedRows", result.getAffectedRows());
            
            if (result.isSuccess()) {
                response.put("message", result.getMessage());
                if (result.getGeneratedKeys() != null && !result.getGeneratedKeys().isEmpty()) {
                    response.put("generatedKeys", result.getGeneratedKeys());
                }
            } else {
                response.put("errorCode", result.getErrorCode());
                response.put("errorMessage", result.getErrorMessage());
            }
            
            response.put("sql", result.getSql());
            
            log.info("SQL更新执行完成 - 医院ID: {}, 数据库类型: {}, 成功: {}, 影响行数: {}", 
                    hospitalId, request.getDatabaseType(), result.isSuccess(), result.getAffectedRows());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("timestamp", LocalDateTime.now());
            response.put("hospitalId", hospitalId);
            response.put("success", false);
            response.put("errorCode", "CONTROLLER_ERROR");
            response.put("errorMessage", "控制器处理异常: " + e.getMessage());
            response.put("exception", e.getClass().getName());
            
            log.error("SQL更新控制器异常: {}", hospitalId, e);
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 测试SQL执行连接
     * 
     * @param hospitalId 医院ID
     * @param databaseType 数据库类型
     * @return 连接测试结果
     */
    @GetMapping("/{hospitalId}/test-sql-connection")
    public ResponseEntity<Map<String, Object>> testSqlConnection(
            @PathVariable String hospitalId,
            @RequestParam(defaultValue = "his") String databaseType) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean connectionSuccessful = sqlExecutionService.testConnection(hospitalId, databaseType);
            
            response.put("timestamp", LocalDateTime.now());
            response.put("hospitalId", hospitalId);
            response.put("databaseType", databaseType);
            response.put("connectionSuccessful", connectionSuccessful);
            response.put("message", connectionSuccessful ? 
                "SQL执行连接测试成功" : "SQL执行连接测试失败");
            
            log.info("SQL执行连接测试 - 医院ID: {}, 数据库类型: {}, 结果: {}", 
                    hospitalId, databaseType, connectionSuccessful ? "成功" : "失败");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("timestamp", LocalDateTime.now());
            response.put("hospitalId", hospitalId);
            response.put("databaseType", databaseType);
            response.put("connectionSuccessful", false);
            response.put("errorMessage", "连接测试异常: " + e.getMessage());
            response.put("exception", e.getClass().getName());
            
            log.error("SQL执行连接测试异常: {} - {}", hospitalId, databaseType, e);
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 清除SQL执行缓存
     * 
     * @param hospitalId 医院ID（可选）
     * @param databaseType 数据库类型（可选）
     * @return 缓存清除结果
     */
    @PostMapping("/clear-cache")
    public ResponseEntity<Map<String, Object>> clearCache(
            @RequestParam(required = false) String hospitalId,
            @RequestParam(required = false) String databaseType) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            sqlExecutionService.clearCache(hospitalId, databaseType);
            
            response.put("timestamp", LocalDateTime.now());
            response.put("success", true);
            response.put("message", "缓存清除成功");
            response.put("hospitalId", hospitalId != null ? hospitalId : "所有医院");
            response.put("databaseType", databaseType != null ? databaseType : "所有数据库类型");
            
            log.info("清除SQL执行缓存 - 医院ID: {}, 数据库类型: {}", 
                    hospitalId != null ? hospitalId : "所有", 
                    databaseType != null ? databaseType : "所有");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("timestamp", LocalDateTime.now());
            response.put("success", false);
            response.put("errorMessage", "缓存清除异常: " + e.getMessage());
            response.put("exception", e.getClass().getName());
            
            log.error("清除SQL执行缓存异常", e);
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取SQL执行缓存统计信息
     * 
     * @return 缓存统计信息
     */
    @GetMapping("/cache-stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> cacheStats = sqlExecutionService.getCacheStats();
            
            response.put("timestamp", LocalDateTime.now());
            response.put("success", true);
            response.put("cacheStats", cacheStats);
            
            log.info("获取SQL执行缓存统计信息");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("timestamp", LocalDateTime.now());
            response.put("success", false);
            response.put("errorMessage", "获取缓存统计信息异常: " + e.getMessage());
            response.put("exception", e.getClass().getName());
            
            log.error("获取SQL执行缓存统计信息异常", e);
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 健康检查端点
     * 
     * @return 健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        
        int configCount = hospitalConfigService.getConfigCount();
        List<HospitalConfig> enabledConfigs = hospitalConfigService.getEnabledConfigs();
        
        response.put("timestamp", LocalDateTime.now());
        response.put("status", "UP");
        response.put("service", "Hospital Configuration Test Service");
        response.put("configCount", configCount);
        response.put("enabledConfigCount", enabledConfigs.size());
        response.put("configDir", hospitalConfigService.getConfigDirPath());
        response.put("details", "医院配置服务运行正常");
        
        return ResponseEntity.ok(response);
    }
}
