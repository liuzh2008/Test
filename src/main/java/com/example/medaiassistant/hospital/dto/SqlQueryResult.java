package com.example.medaiassistant.hospital.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * SQL查询结果DTO
 */
@Data
public class SqlQueryResult {
    
    /**
     * 执行是否成功
     */
    private boolean success;
    
    /**
     * 执行时间（毫秒）
     */
    private long executionTimeMs;
    
    /**
     * 返回行数
     */
    private int rowCount;
    
    /**
     * 列名列表
     */
    private List<String> columns;
    
    /**
     * 查询数据
     */
    private List<Map<String, Object>> data;
    
    /**
     * 执行的SQL语句
     */
    private String sql;
    
    /**
     * 警告信息列表
     */
    private List<String> warnings;
    
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
    public static SqlQueryResult success(List<Map<String, Object>> data, List<String> columns, 
                                         long executionTimeMs, String sql) {
        SqlQueryResult result = new SqlQueryResult();
        result.setSuccess(true);
        result.setData(data);
        result.setColumns(columns);
        result.setRowCount(data != null ? data.size() : 0);
        result.setExecutionTimeMs(executionTimeMs);
        result.setSql(sql);
        return result;
    }
    
    /**
     * 创建错误结果
     */
    public static SqlQueryResult error(String errorMessage, String errorCode, 
                                       long executionTimeMs, String sql) {
        SqlQueryResult result = new SqlQueryResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setErrorCode(errorCode);
        result.setExecutionTimeMs(executionTimeMs);
        result.setSql(sql);
        result.setRowCount(0);
        return result;
    }
}
