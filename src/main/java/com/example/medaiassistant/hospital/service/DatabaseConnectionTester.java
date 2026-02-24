package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.model.ConnectionTestResult;
import com.example.medaiassistant.hospital.model.HospitalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

/**
 * 数据库连接测试服务
 * 负责测试医院数据库连接，验证配置的正确性
 */
@Service
public class DatabaseConnectionTester {
    
    private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionTester.class);
    
    private static final String DATABASE_INTEGRATION_TYPE = "database";
    private static final String ORACLE_DRIVER_CLASS = "oracle.jdbc.OracleDriver";
    private static final String TEST_QUERY = "SELECT 1 FROM DUAL";
    
    /**
     * 测试数据库连接
     * 
     * @param config 医院配置
     * @return 连接测试结果
     */
    public ConnectionTestResult testConnection(HospitalConfig config) {
        // 验证配置
        ValidationResult validation = validateConfig(config);
        if (!validation.isValid()) {
            return validation.getErrorResult();
        }
        
        HospitalConfig.Hospital hospital = config.getHospital();
        HospitalConfig.HisConfig hisConfig = hospital.getHis();
        String hospitalId = hospital.getId();
        String hospitalName = hospital.getName();
        String url = hisConfig.getUrl();
        
        // 测试数据库连接
        long startTime = System.currentTimeMillis();
        try {
            return testDatabaseConnection(url, hisConfig.getUsername(), hisConfig.getPassword(), 
                                        hospitalId, hospitalName, startTime);
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("医院 {} 数据库连接测试失败: {}", hospitalId, e.getMessage(), e);
            return ConnectionTestResult.failure(hospitalId, hospitalName, url, 
                                              e.getMessage(), responseTime);
        }
    }
    
    /**
     * 验证医院配置
     */
    private ValidationResult validateConfig(HospitalConfig config) {
        // 处理null配置
        if (config == null) {
            log.warn("尝试测试null配置的连接");
            return ValidationResult.error(ConnectionTestResult.configError("医院配置不能为null"));
        }
        
        // 检查医院配置是否有效
        if (config.getHospital() == null) {
            log.warn("医院配置缺少hospital节点: {}", config);
            return ValidationResult.error(ConnectionTestResult.configError("医院配置缺少hospital节点"));
        }
        
        HospitalConfig.Hospital hospital = config.getHospital();
        String hospitalId = hospital.getId();
        String hospitalName = hospital.getName();
        
        // 检查集成类型
        if (!DATABASE_INTEGRATION_TYPE.equals(hospital.getIntegrationType())) {
            log.warn("医院 {} 的集成类型不是database: {}", hospitalId, hospital.getIntegrationType());
            return ValidationResult.error(ConnectionTestResult.configError(hospitalId, hospitalName, 
                                                  "集成类型必须是database才能测试数据库连接"));
        }
        
        // 检查HIS配置
        if (hospital.getHis() == null) {
            log.warn("医院 {} 缺少HIS数据库配置", hospitalId);
            return ValidationResult.error(ConnectionTestResult.configError(hospitalId, hospitalName, 
                                                  "缺少HIS数据库配置"));
        }
        
        HospitalConfig.HisConfig hisConfig = hospital.getHis();
        String url = hisConfig.getUrl();
        String username = hisConfig.getUsername();
        String password = hisConfig.getPassword();
        
        // 验证配置完整性
        if (url == null || url.trim().isEmpty()) {
            return ValidationResult.error(ConnectionTestResult.configError(hospitalId, hospitalName, 
                                                  "数据库URL不能为空"));
        }
        if (username == null || username.trim().isEmpty()) {
            return ValidationResult.error(ConnectionTestResult.configError(hospitalId, hospitalName, 
                                                  "数据库用户名不能为空"));
        }
        if (password == null || password.trim().isEmpty()) {
            return ValidationResult.error(ConnectionTestResult.configError(hospitalId, hospitalName, 
                                                  "数据库密码不能为空"));
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * 实际测试数据库连接
     */
    private ConnectionTestResult testDatabaseConnection(String url, String username, 
                                                       String password, String hospitalId,
                                                       String hospitalName, long startTime) 
            throws SQLException {
        
        try {
            // 加载Oracle JDBC驱动
            Class.forName(ORACLE_DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("Oracle JDBC驱动未找到: {}", e.getMessage());
            return ConnectionTestResult.failure(hospitalId, hospitalName, url, 
                                              "Oracle JDBC驱动未找到: " + e.getMessage(), 
                                              responseTime);
        }
        
        // 使用try-with-resources自动关闭资源
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(TEST_QUERY)) {
            
            if (resultSet.next()) {
                long responseTime = System.currentTimeMillis() - startTime;
                log.info("医院 {} 数据库连接测试成功，响应时间: {}ms", hospitalId, responseTime);
                return ConnectionTestResult.success(hospitalId, hospitalName, url, responseTime);
            } else {
                long responseTime = System.currentTimeMillis() - startTime;
                log.warn("医院 {} 数据库连接测试返回空结果", hospitalId);
                return ConnectionTestResult.failure(hospitalId, hospitalName, url, 
                                                  "数据库连接测试返回空结果", responseTime);
            }
            
        } catch (SQLException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("医院 {} 数据库连接失败: {}", hospitalId, e.getMessage());
            return ConnectionTestResult.failure(hospitalId, hospitalName, url, 
                                              extractUserFriendlyErrorMessage(e), responseTime);
        }
    }
    
    /**
     * 提取用户友好的错误信息
     */
    private String extractUserFriendlyErrorMessage(SQLException e) {
        String message = e.getMessage();
        if (message == null) {
            return "数据库连接失败";
        }
        
        // 简化常见的Oracle错误信息
        if (message.contains("IO Error: The Network Adapter could not establish the connection")) {
            return "网络适配器无法建立连接，请检查网络和数据库服务器状态";
        } else if (message.contains("Listener refused the connection")) {
            return "数据库监听器拒绝连接，请检查数据库URL和监听器状态";
        } else if (message.contains("ORA-01017")) {
            return "用户名或密码无效";
        } else if (message.contains("ORA-12541")) {
            return "数据库监听器未启动";
        } else if (message.contains("ORA-12154")) {
            return "无法解析数据库连接标识符";
        } else if (message.contains("ORA-12514")) {
            return "监听器未配置指定的服务名";
        }
        
        return message;
    }
    
    /**
     * 测试数据库连接（简化版，使用医院ID）
     * 
     * @param hospitalId 医院ID
     * @return 连接测试结果
     */
    public ConnectionTestResult testConnection(String hospitalId, HospitalConfigService configService) {
        HospitalConfig config = configService.getConfig(hospitalId);
        if (config == null) {
            log.warn("医院配置不存在: {}", hospitalId);
            return ConnectionTestResult.configError(hospitalId, null, 
                                                  "医院配置不存在: " + hospitalId);
        }
        return testConnection(config);
    }
    
    /**
     * 批量测试所有启用的医院数据库连接
     * 
     * @param configService 医院配置服务
     * @return 连接测试结果列表
     */
    public java.util.List<ConnectionTestResult> testAllEnabledConnections(HospitalConfigService configService) {
        java.util.List<ConnectionTestResult> results = new java.util.ArrayList<>();
        java.util.List<HospitalConfig> enabledConfigs = configService.getEnabledConfigs();
        
        log.info("开始批量测试 {} 个启用的医院数据库连接", enabledConfigs.size());
        
        for (HospitalConfig config : enabledConfigs) {
            ConnectionTestResult result = testConnection(config);
            results.add(result);
            
            if (result.isSuccess()) {
                log.info("医院 {} 数据库连接测试成功，响应时间: {}ms", 
                        config.getId(), result.getResponseTimeMs());
            } else {
                log.warn("医院 {} 数据库连接测试失败: {}", 
                        config.getId(), result.getErrorMessage());
            }
        }
        
        log.info("批量测试完成，成功: {}, 失败: {}", 
                results.stream().filter(ConnectionTestResult::isSuccess).count(),
                results.stream().filter(r -> !r.isSuccess()).count());
        
        return results;
    }
    
    /**
     * 验证结果内部类
     */
    private static class ValidationResult {
        private final boolean valid;
        private final ConnectionTestResult errorResult;
        
        private ValidationResult(boolean valid, ConnectionTestResult errorResult) {
            this.valid = valid;
            this.errorResult = errorResult;
        }
        
        static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        
        static ValidationResult error(ConnectionTestResult errorResult) {
            return new ValidationResult(false, errorResult);
        }
        
        boolean isValid() {
            return valid;
        }
        
        ConnectionTestResult getErrorResult() {
            return errorResult;
        }
    }
}
