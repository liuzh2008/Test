package com.example.medaiassistant.hospital.dto;

import lombok.Data;

import java.util.Map;

/**
 * SQL查询请求DTO
 */
@Data
public class SqlQueryRequest {
    
    /**
     * SQL语句
     */
    private String sql;
    
    /**
     * 数据库类型（his/lis）
     */
    private String databaseType = "his";
    
    /**
     * 查询参数
     */
    private Map<String, Object> parameters;
    
    /**
     * 最大返回行数
     */
    private Integer maxRows = 1000;
    
    /**
     * 查询超时时间（秒）
     */
    private Integer timeoutSeconds = 30;
    
    /**
     * 是否返回列信息
     */
    private Boolean includeColumns = true;
}
