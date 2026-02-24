package com.example.medaiassistant.hospital.dto;

import lombok.Data;

import java.util.List;

/**
 * SQL更新结果DTO
 */
@Data
public class SqlUpdateResult {
    
    /**
     * 执行是否成功
     */
    private boolean success;
    
    /**
     * 执行时间（毫秒）
     */
    private long executionTimeMs;
    
    /**
     * 影响行数
     */
    private int affectedRows;
    
    /**
     * 生成的主键列表
     */
    private List<Object> generatedKeys;
    
    /**
     * 执行的SQL语句
     */
    private String sql;
    
    /**
     * 成功消息
     */
    private String message;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 错误代码
     */
    private String errorCode;
    
    /**
     * 创建成功结果
     */
    public static SqlUpdateResult success(int affectedRows, long executionTimeMs, String sql) {
        SqlUpdateResult result = new SqlUpdateResult();
        result.setSuccess(true);
        result.setAffectedRows(affectedRows);
        result.setExecutionTimeMs(executionTimeMs);
        result.setSql(sql);
        result.setMessage("执行成功");
        return result;
    }
    
    /**
     * 创建成功结果（带生成的主键）
     */
    public static SqlUpdateResult success(int affectedRows, List<Object> generatedKeys, 
                                          long executionTimeMs, String sql) {
        SqlUpdateResult result = success(affectedRows, executionTimeMs, sql);
        result.setGeneratedKeys(generatedKeys);
        result.setMessage("执行成功，生成" + (generatedKeys != null ? generatedKeys.size() : 0) + "个主键");
        return result;
    }
    
    /**
     * 创建错误结果
     */
    public static SqlUpdateResult error(String errorMessage, String errorCode, 
                                        long executionTimeMs, String sql) {
        SqlUpdateResult result = new SqlUpdateResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setErrorCode(errorCode);
        result.setExecutionTimeMs(executionTimeMs);
        result.setSql(sql);
        result.setAffectedRows(0);
        return result;
    }
}
