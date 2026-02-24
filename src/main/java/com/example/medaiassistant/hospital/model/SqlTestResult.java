package com.example.medaiassistant.hospital.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * SQL测试结果
 * 包含SQL查询执行的结果信息
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlTestResult {
    
    /**
     * 是否成功执行
     */
    private boolean success;
    
    /**
     * 查询数据
     * 每行数据是一个Map，key为列名，value为列值
     */
    private List<Map<String, Object>> data;
    
    /**
     * 执行时间（毫秒）
     */
    private long executionTimeMs;
    
    /**
     * 列信息
     */
    private List<String> columns;
    
    /**
     * 行数
     */
    private int rowCount;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 执行的SQL语句（可能经过安全处理）
     */
    private String executedSql;
    
    /**
     * 创建成功结果
     * 
     * @param data 查询数据
     * @param columns 列信息
     * @param executionTimeMs 执行时间
     * @param executedSql 执行的SQL语句
     * @return 成功结果
     */
    public static SqlTestResult success(List<Map<String, Object>> data, 
                                       List<String> columns, 
                                       long executionTimeMs,
                                       String executedSql) {
        return SqlTestResult.builder()
                .success(true)
                .data(data != null ? data : Collections.emptyList())
                .columns(columns != null ? columns : Collections.emptyList())
                .executionTimeMs(executionTimeMs)
                .rowCount(data != null ? data.size() : 0)
                .executedSql(executedSql)
                .build();
    }
    
    /**
     * 创建失败结果
     * 
     * @param errorMessage 错误信息
     * @param executedSql 执行的SQL语句
     * @return 失败结果
     */
    public static SqlTestResult error(String errorMessage, String executedSql) {
        return SqlTestResult.builder()
                .success(false)
                .data(Collections.emptyList())
                .columns(Collections.emptyList())
                .executionTimeMs(0)
                .rowCount(0)
                .errorMessage(errorMessage)
                .executedSql(executedSql)
                .build();
    }
    
    /**
     * 创建失败结果（带执行时间）
     * 
     * @param errorMessage 错误信息
     * @param executionTimeMs 执行时间
     * @param executedSql 执行的SQL语句
     * @return 失败结果
     */
    public static SqlTestResult error(String errorMessage, long executionTimeMs, String executedSql) {
        return SqlTestResult.builder()
                .success(false)
                .data(Collections.emptyList())
                .columns(Collections.emptyList())
                .executionTimeMs(executionTimeMs)
                .rowCount(0)
                .errorMessage(errorMessage)
                .executedSql(executedSql)
                .build();
    }
}
