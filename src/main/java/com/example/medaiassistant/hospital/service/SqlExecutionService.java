package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.dto.SqlQueryRequest;
import com.example.medaiassistant.hospital.dto.SqlQueryResult;
import com.example.medaiassistant.hospital.dto.SqlUpdateRequest;
import com.example.medaiassistant.hospital.dto.SqlUpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.*;

/**
 * SQL执行服务
 * 负责执行SQL查询和更新操作
 */
@Service
public class SqlExecutionService {
    
    private static final Logger log = LoggerFactory.getLogger(SqlExecutionService.class);
    
    private final DynamicJdbcTemplateFactory jdbcTemplateFactory;
    private final SqlSecurityValidator sqlSecurityValidator;
    private final HospitalConfigService hospitalConfigService;
    
    public SqlExecutionService(DynamicJdbcTemplateFactory jdbcTemplateFactory,
                              SqlSecurityValidator sqlSecurityValidator,
                              HospitalConfigService hospitalConfigService) {
        this.jdbcTemplateFactory = jdbcTemplateFactory;
        this.sqlSecurityValidator = sqlSecurityValidator;
        this.hospitalConfigService = hospitalConfigService;
    }
    
    /**
     * 执行SQL查询
     * 
     * @param hospitalId 医院ID
     * @param request 查询请求
     * @return 查询结果
     */
    public SqlQueryResult executeQuery(String hospitalId, SqlQueryRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 验证输入参数
            validateInput(hospitalId, request.getSql(), request.getDatabaseType());
            
            // 验证SQL安全性
            if (!sqlSecurityValidator.isSqlSafe(request.getSql())) {
                return SqlQueryResult.error("SQL语句不安全，可能包含危险操作", 
                                          "SQL_SECURITY_ERROR", 
                                          System.currentTimeMillis() - startTime, 
                                          request.getSql());
            }
            
            // 获取JdbcTemplate
            JdbcTemplate jdbcTemplate = jdbcTemplateFactory.getJdbcTemplate(hospitalId, request.getDatabaseType());
            
            // 配置JdbcTemplate
            configureJdbcTemplate(jdbcTemplate, request);
            
            // 执行查询
            List<Map<String, Object>> data;
            if (request.getParameters() != null && !request.getParameters().isEmpty()) {
                data = executeParameterizedQuery(jdbcTemplate, request.getSql(), request.getParameters());
            } else {
                data = jdbcTemplate.queryForList(request.getSql());
            }
            
            // 提取列信息
            List<String> columns = extractColumns(data);
            
            // 应用行数限制
            if (request.getMaxRows() != null && data.size() > request.getMaxRows()) {
                data = data.subList(0, request.getMaxRows());
                log.warn("查询结果超过最大行数限制，已截断: {} > {}", data.size(), request.getMaxRows());
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            log.info("SQL查询执行成功 - 医院ID: {}, 数据库类型: {}, 执行时间: {}ms, 返回行数: {}", 
                    hospitalId, request.getDatabaseType(), executionTime, data.size());
            
            return SqlQueryResult.success(data, columns, executionTime, request.getSql());
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("SQL查询执行失败 - 医院ID: {}, SQL: {}", hospitalId, request.getSql(), e);
            return handleQueryException(e, executionTime, request.getSql());
        }
    }
    
    /**
     * 执行SQL更新
     * 
     * @param hospitalId 医院ID
     * @param request 更新请求
     * @return 更新结果
     */
    public SqlUpdateResult executeUpdate(String hospitalId, SqlUpdateRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 验证输入参数
            validateInput(hospitalId, request.getSql(), request.getDatabaseType());
            
            // 验证SQL安全性
            if (!sqlSecurityValidator.isSqlSafe(request.getSql())) {
                return SqlUpdateResult.error("SQL语句不安全，可能包含危险操作", 
                                           "SQL_SECURITY_ERROR", 
                                           System.currentTimeMillis() - startTime, 
                                           request.getSql());
            }
            
            // 获取JdbcTemplate
            JdbcTemplate jdbcTemplate = jdbcTemplateFactory.getJdbcTemplate(hospitalId, request.getDatabaseType());
            
            // 配置JdbcTemplate
            configureJdbcTemplate(jdbcTemplate, request);
            
            // 执行更新
            int affectedRows;
            List<Object> generatedKeys = null;
            
            if (request.getParameters() != null && !request.getParameters().isEmpty()) {
                if (Boolean.TRUE.equals(request.getReturnGeneratedKeys())) {
                    // 需要返回生成的主键
                    KeyHolder keyHolder = new GeneratedKeyHolder();
                    affectedRows = executeParameterizedUpdateWithKeys(jdbcTemplate, request.getSql(), 
                                                                      request.getParameters(), keyHolder);
                    generatedKeys = extractGeneratedKeys(keyHolder);
                } else {
                    // 普通参数化更新
                    affectedRows = executeParameterizedUpdate(jdbcTemplate, request.getSql(), request.getParameters());
                }
            } else {
                // 无参数更新
                affectedRows = jdbcTemplate.update(request.getSql());
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            log.info("SQL更新执行成功 - 医院ID: {}, 数据库类型: {}, 执行时间: {}ms, 影响行数: {}", 
                    hospitalId, request.getDatabaseType(), executionTime, affectedRows);
            
            if (Boolean.TRUE.equals(request.getReturnGeneratedKeys()) && generatedKeys != null) {
                return SqlUpdateResult.success(affectedRows, generatedKeys, executionTime, request.getSql());
            } else {
                return SqlUpdateResult.success(affectedRows, executionTime, request.getSql());
            }
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("SQL更新执行失败 - 医院ID: {}, SQL: {}", hospitalId, request.getSql(), e);
            return handleUpdateException(e, executionTime, request.getSql());
        }
    }
    
    /**
     * 验证输入参数
     */
    private void validateInput(String hospitalId, String sql, String databaseType) {
        if (hospitalId == null || hospitalId.trim().isEmpty()) {
            throw new IllegalArgumentException("医院ID不能为空");
        }
        
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL语句不能为空");
        }
        
        if (databaseType == null || (!"his".equalsIgnoreCase(databaseType) && !"lis".equalsIgnoreCase(databaseType))) {
            throw new IllegalArgumentException("数据库类型必须是his或lis");
        }
        
        // 检查医院配置是否存在
        if (!hospitalConfigService.hasConfig(hospitalId)) {
            throw new IllegalArgumentException("医院配置不存在: " + hospitalId);
        }
    }
    
    /**
     * 配置JdbcTemplate（查询）
     */
    private void configureJdbcTemplate(JdbcTemplate jdbcTemplate, SqlQueryRequest request) {
        if (request.getTimeoutSeconds() != null) {
            jdbcTemplate.setQueryTimeout(request.getTimeoutSeconds());
        }
        
        if (request.getMaxRows() != null) {
            jdbcTemplate.setMaxRows(request.getMaxRows());
        }
    }
    
    /**
     * 配置JdbcTemplate（更新）
     */
    private void configureJdbcTemplate(JdbcTemplate jdbcTemplate, SqlUpdateRequest request) {
        if (request.getTimeoutSeconds() != null) {
            jdbcTemplate.setQueryTimeout(request.getTimeoutSeconds());
        }
    }
    
    /**
     * 执行参数化查询
     */
    private List<Map<String, Object>> executeParameterizedQuery(JdbcTemplate jdbcTemplate, 
                                                               String sql, 
                                                               Map<String, Object> parameters) {
        // 按SQL中命名参数出现的顺序提取参数值
        List<String> orderedParamNames = new ArrayList<>();
        String positionalSql = convertNamedParametersToPositional(sql, parameters, orderedParamNames);
        Object[] args = extractOrderedParameterValues(parameters, orderedParamNames);
        
        return jdbcTemplate.queryForList(positionalSql, args);
    }
    
    /**
     * 执行参数化更新
     */
    private int executeParameterizedUpdate(JdbcTemplate jdbcTemplate, 
                                          String sql, 
                                          Map<String, Object> parameters) {
        List<String> orderedParamNames = new ArrayList<>();
        String positionalSql = convertNamedParametersToPositional(sql, parameters, orderedParamNames);
        Object[] args = extractOrderedParameterValues(parameters, orderedParamNames);
        
        return jdbcTemplate.update(positionalSql, args);
    }
    
    /**
     * 执行参数化更新并返回生成的主键
     */
    private int executeParameterizedUpdateWithKeys(JdbcTemplate jdbcTemplate, 
                                                  String sql, 
                                                  Map<String, Object> parameters,
                                                  KeyHolder keyHolder) {
        List<String> orderedParamNames = new ArrayList<>();
        String positionalSql = convertNamedParametersToPositional(sql, parameters, orderedParamNames);
        Object[] args = extractOrderedParameterValues(parameters, orderedParamNames);
        
        // 创建PreparedStatementCreator
        PreparedStatementCreator psc = new PreparedStatementCreatorFactory(positionalSql)
                .newPreparedStatementCreator(args);
        
        return jdbcTemplate.update(psc, keyHolder);
    }
    
    /**
     * 按指定顺序提取参数值
     */
    private Object[] extractOrderedParameterValues(Map<String, Object> parameters, List<String> orderedParamNames) {
        if (parameters == null || parameters.isEmpty() || orderedParamNames.isEmpty()) {
            return new Object[0];
        }
        
        Object[] args = new Object[orderedParamNames.size()];
        for (int i = 0; i < orderedParamNames.size(); i++) {
            args[i] = parameters.get(orderedParamNames.get(i));
        }
        return args;
    }
    
    /**
     * 将命名参数转换为位置参数，并记录参数出现的顺序
     */
    private String convertNamedParametersToPositional(String sql, Map<String, Object> parameters, List<String> orderedParamNames) {
        if (parameters == null || parameters.isEmpty()) {
            return sql;
        }
        
        StringBuilder result = new StringBuilder();
        int lastIndex = 0;
        
        // 按SQL中出现的顺序查找并替换命名参数
        while (lastIndex < sql.length()) {
            int colonIndex = sql.indexOf(':', lastIndex);
            if (colonIndex == -1) {
                result.append(sql.substring(lastIndex));
                break;
            }
            
            // 添加冒号之前的部分
            result.append(sql.substring(lastIndex, colonIndex));
            
            // 提取参数名（字母、数字、下划线组成）
            int paramStart = colonIndex + 1;
            int paramEnd = paramStart;
            while (paramEnd < sql.length() && isParamNameChar(sql.charAt(paramEnd))) {
                paramEnd++;
            }
            
            if (paramEnd > paramStart) {
                String paramName = sql.substring(paramStart, paramEnd);
                if (parameters.containsKey(paramName)) {
                    result.append("?");
                    orderedParamNames.add(paramName);
                    lastIndex = paramEnd;
                } else {
                    // 不是我们的参数，保留原样
                    result.append(":");
                    lastIndex = paramStart;
                }
            } else {
                // 只有冒号没有参数名
                result.append(":");
                lastIndex = paramStart;
            }
        }
        
        return result.toString();
    }
    
    /**
     * 判断字符是否可以作为参数名的一部分
     */
    private boolean isParamNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
    
    /**
     * 提取生成的主键
     */
    private List<Object> extractGeneratedKeys(KeyHolder keyHolder) {
        if (keyHolder.getKeyList() == null || keyHolder.getKeyList().isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Object> keys = new ArrayList<>();
        for (Map<String, Object> keyMap : keyHolder.getKeyList()) {
            if (keyMap != null && !keyMap.isEmpty()) {
                keys.addAll(keyMap.values());
            }
        }
        
        return keys;
    }
    
    /**
     * 从查询结果中提取列信息
     */
    private List<String> extractColumns(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return Collections.emptyList();
        }
        
        Map<String, Object> firstRow = data.get(0);
        return new ArrayList<>(firstRow.keySet());
    }
    
    /**
     * 处理查询异常
     */
    private SqlQueryResult handleQueryException(Exception e, long executionTime, String sql) {
        String errorCode = "SQL_EXECUTION_ERROR";
        String errorMessage = e.getMessage();
        
        if (e instanceof SQLException) {
            SQLException sqlEx = (SQLException) e;
            errorCode = "SQL_" + sqlEx.getErrorCode();
            errorMessage = extractUserFriendlyErrorMessage(sqlEx);
        } else if (e instanceof IllegalArgumentException) {
            errorCode = "VALIDATION_ERROR";
        } else if (e instanceof org.springframework.dao.DataAccessException) {
            errorCode = "DATA_ACCESS_ERROR";
        }
        
        return SqlQueryResult.error(errorMessage, errorCode, executionTime, sql);
    }
    
    /**
     * 处理更新异常
     */
    private SqlUpdateResult handleUpdateException(Exception e, long executionTime, String sql) {
        String errorCode = "SQL_EXECUTION_ERROR";
        String errorMessage = e.getMessage();
        
        if (e instanceof SQLException) {
            SQLException sqlEx = (SQLException) e;
            errorCode = "SQL_" + sqlEx.getErrorCode();
            errorMessage = extractUserFriendlyErrorMessage(sqlEx);
        } else if (e instanceof IllegalArgumentException) {
            errorCode = "VALIDATION_ERROR";
        } else if (e instanceof org.springframework.dao.DataAccessException) {
            errorCode = "DATA_ACCESS_ERROR";
        }
        
        return SqlUpdateResult.error(errorMessage, errorCode, executionTime, sql);
    }
    
    /**
     * 提取用户友好的错误信息
     */
    private String extractUserFriendlyErrorMessage(SQLException e) {
        String message = e.getMessage();
        if (message == null) {
            return "数据库操作失败";
        }
        
        // 简化常见的Oracle错误信息
        if (message.contains("ORA-00942")) {
            return "表或视图不存在";
        } else if (message.contains("ORA-00904")) {
            return "无效的列名";
        } else if (message.contains("ORA-00936")) {
            return "SQL语句缺少表达式";
        } else if (message.contains("ORA-00933")) {
            return "SQL命令未正确结束";
        } else if (message.contains("ORA-00001")) {
            return "违反唯一约束条件";
        } else if (message.contains("ORA-02291")) {
            return "违反完整性约束条件 - 未找到父项关键字";
        } else if (message.contains("ORA-02292")) {
            return "违反完整性约束条件 - 已找到子记录";
        } else if (message.contains("ORA-01400")) {
            return "无法将NULL插入非空列";
        } else if (message.contains("ORA-01438")) {
            return "值大于为此列指定的允许精度";
        }
        
        return message;
    }
    
    /**
     * 测试数据库连接
     */
    public boolean testConnection(String hospitalId, String databaseType) {
        return jdbcTemplateFactory.testConnection(hospitalId, databaseType);
    }
    
    /**
     * 清除缓存
     */
    public void clearCache(String hospitalId, String databaseType) {
        jdbcTemplateFactory.clearCache(hospitalId, databaseType);
    }
    
    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        return jdbcTemplateFactory.getCacheStats();
    }
}
