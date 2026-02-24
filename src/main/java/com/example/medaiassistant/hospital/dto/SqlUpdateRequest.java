package com.example.medaiassistant.hospital.dto;

import lombok.Data;

import java.util.Map;

/**
 * SQL更新请求DTO
 */
@Data
public class SqlUpdateRequest {
    
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
     * 是否返回生成的主键
     */
    private Boolean returnGeneratedKeys = false;
    
    /**
     * 执行超时时间（秒）
     */
    private Integer timeoutSeconds = 30;
}
