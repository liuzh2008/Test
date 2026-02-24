package com.example.medaiassistant.hospital.model;

import lombok.Data;

/**
 * 数据库连接测试结果
 * 用于返回数据库连接测试的状态和相关信息
 */
@Data
public class ConnectionTestResult {
    
    /**
     * 连接测试是否成功
     */
    private boolean success;
    
    /**
     * 错误信息（连接失败时）
     */
    private String errorMessage;
    
    /**
     * 响应时间（毫秒）
     */
    private long responseTimeMs;
    
    /**
     * 医院ID
     */
    private String hospitalId;
    
    /**
     * 医院名称
     */
    private String hospitalName;
    
    /**
     * 数据库URL
     */
    private String databaseUrl;
    
    /**
     * 测试时间戳
     */
    private long timestamp;
    
    /**
     * 创建成功的连接测试结果
     */
    public static ConnectionTestResult success(String hospitalId, String hospitalName, 
                                              String databaseUrl, long responseTimeMs) {
        ConnectionTestResult result = new ConnectionTestResult();
        result.setSuccess(true);
        result.setHospitalId(hospitalId);
        result.setHospitalName(hospitalName);
        result.setDatabaseUrl(databaseUrl);
        result.setResponseTimeMs(responseTimeMs);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }
    
    /**
     * 创建失败的连接测试结果
     */
    public static ConnectionTestResult failure(String hospitalId, String hospitalName,
                                              String databaseUrl, String errorMessage,
                                              long responseTimeMs) {
        ConnectionTestResult result = new ConnectionTestResult();
        result.setSuccess(false);
        result.setHospitalId(hospitalId);
        result.setHospitalName(hospitalName);
        result.setDatabaseUrl(databaseUrl);
        result.setErrorMessage(errorMessage);
        result.setResponseTimeMs(responseTimeMs);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }
    
    /**
     * 创建配置错误的连接测试结果
     */
    public static ConnectionTestResult configError(String errorMessage) {
        ConnectionTestResult result = new ConnectionTestResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }
    
    /**
     * 创建配置错误的连接测试结果（带医院信息）
     */
    public static ConnectionTestResult configError(String hospitalId, String hospitalName, 
                                                  String errorMessage) {
        ConnectionTestResult result = new ConnectionTestResult();
        result.setSuccess(false);
        result.setHospitalId(hospitalId);
        result.setHospitalName(hospitalName);
        result.setErrorMessage(errorMessage);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }
    
    @Override
    public String toString() {
        if (success) {
            return String.format("ConnectionTestResult{success=true, hospitalId='%s', " +
                               "hospitalName='%s', responseTimeMs=%d}",
                               hospitalId, hospitalName, responseTimeMs);
        } else {
            return String.format("ConnectionTestResult{success=false, hospitalId='%s', " +
                               "hospitalName='%s', errorMessage='%s', responseTimeMs=%d}",
                               hospitalId, hospitalName, errorMessage, responseTimeMs);
        }
    }
}
