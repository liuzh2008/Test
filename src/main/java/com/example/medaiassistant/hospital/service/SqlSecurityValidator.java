package com.example.medaiassistant.hospital.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SQL安全验证器
 * 负责验证SQL语句的安全性，防止SQL注入和危险操作
 * 主要功能：
 * 1. 检测危险关键字（DROP, DELETE, UPDATE, INSERT, ALTER等）
 * 2. 验证只读查询（只允许SELECT）
 * 3. 强制参数化查询
 * 4. 安全审计日志记录
 * 5. 支持自定义安全规则
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-03
 */
@Slf4j
@Service
public class SqlSecurityValidator {
    
    /**
     * 危险关键字集合
     * 这些关键字表示可能执行危险操作的SQL语句
     */
    private static final Set<String> DANGEROUS_KEYWORDS = new HashSet<>(Arrays.asList(
        "DROP", "DELETE", "UPDATE", "INSERT", "ALTER", 
        "TRUNCATE", "CREATE", "GRANT", "REVOKE", "EXECUTE"
    ));
    
    /**
     * 自定义安全规则集合
     * 可以动态添加自定义的安全规则
     */
    private final Set<String> customDangerousPatterns = new HashSet<>();
    
    /**
     * 日期时间格式化器，用于审计日志
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 验证SQL语句是否安全
     * 
     * @param sql 要验证的SQL语句
     * @return true表示安全，false表示危险
     */
    public boolean isSqlSafe(String sql) {
        // 记录审计日志开始
        String auditId = generateAuditId();
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            // 处理null和空字符串
            if (sql == null || sql.trim().isEmpty()) {
                String reason = "SQL语句为null或空";
                logSecurityAudit(auditId, sql, false, reason, startTime);
                log.warn("SQL语句为null或空，视为不安全");
                return false;
            }
            
            // 转换为大写进行关键字检测（大小写不敏感）
            String upperSql = sql.toUpperCase().trim();
            
            // 检查是否以SELECT开头（只允许SELECT查询）
            if (!upperSql.startsWith("SELECT")) {
                String reason = "SQL语句不是SELECT查询";
                logSecurityAudit(auditId, sql, false, reason, startTime);
                log.warn("SQL语句不是SELECT查询: {}", sql);
                return false;
            }
            
            // 检查危险关键字
            for (String keyword : DANGEROUS_KEYWORDS) {
                // 使用正则表达式确保关键字是独立的单词
                // 匹配 " DROP " 或 "DROP " 或 " DROP" 或 "DROP"（作为独立单词）
                String pattern = "\\b" + keyword + "\\b";
                if (Pattern.compile(pattern).matcher(upperSql).find()) {
                    String reason = "SQL语句包含危险关键字: " + keyword;
                    logSecurityAudit(auditId, sql, false, reason, startTime);
                    log.warn("SQL语句包含危险关键字 '{}': {}", keyword, sql);
                    return false;
                }
            }
            
            // 检查自定义安全规则
            for (String customPattern : customDangerousPatterns) {
                if (Pattern.compile(customPattern, Pattern.CASE_INSENSITIVE).matcher(sql).find()) {
                    String reason = "SQL语句违反自定义安全规则: " + customPattern;
                    logSecurityAudit(auditId, sql, false, reason, startTime);
                    log.warn("SQL语句违反自定义安全规则 '{}': {}", customPattern, sql);
                    return false;
                }
            }
            
            // 检查参数化查询（强制使用参数化查询防止SQL注入）
            // 简单的检查：如果WHERE子句中有直接的值（数字或带单引号的字符串），则视为不安全
            // 更严格的实现可以检查是否有 :param 或 ? 参数占位符
            if (!isParameterizedQuery(sql)) {
                String reason = "SQL语句未使用参数化查询，可能存在SQL注入风险";
                logSecurityAudit(auditId, sql, false, reason, startTime);
                log.warn("SQL语句未使用参数化查询，可能存在SQL注入风险: {}", sql);
                return false;
            }
            
            // 记录安全验证通过
            logSecurityAudit(auditId, sql, true, "SQL语句安全验证通过", startTime);
            log.debug("SQL语句安全验证通过: {}", sql);
            return true;
            
        } catch (Exception e) {
            // 记录验证过程中的异常
            String reason = "安全验证过程中发生异常: " + e.getMessage();
            logSecurityAudit(auditId, sql, false, reason, startTime);
            log.error("SQL安全验证过程中发生异常: {}", sql, e);
            return false;
        }
    }
    
    /**
     * 检查是否为参数化查询
     * 简单的实现：检查WHERE子句中是否有直接的值
     * 更完善的实现应该使用SQL解析器
     * 
     * @param sql SQL语句
     * @return true表示是参数化查询，false表示可能包含直接值
     */
    private boolean isParameterizedQuery(String sql) {
        // 转换为小写以便检查
        String lowerSql = sql.toLowerCase();
        
        // 检查是否有WHERE子句
        int whereIndex = lowerSql.indexOf("where");
        if (whereIndex == -1) {
            // 没有WHERE子句，视为安全
            return true;
        }
        
        // 提取WHERE子句部分
        String whereClause = lowerSql.substring(whereIndex + 5);
        
        // 简单的检查：如果WHERE子句中有 '=' 后面跟着数字或带单引号的字符串，则可能不是参数化查询
        // 这是一个简化的实现，实际应用中可能需要更复杂的SQL解析
        
        // 检查常见的参数化模式
        boolean hasNamedParameter = sql.contains(":") || lowerSql.contains("= :");
        boolean hasPositionalParameter = sql.contains("?");
        
        // 如果有参数占位符，视为参数化查询
        if (hasNamedParameter || hasPositionalParameter) {
            return true;
        }
        
        // 检查是否有直接的值（这是一个简化的检查）
        // 匹配模式：= 123 或 = 'value'
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("=\\s*\\d+|=\\s*'[^']*'");
        java.util.regex.Matcher matcher = pattern.matcher(whereClause);
        
        // 如果没有找到直接值，视为参数化查询
        // 注意：这是一个保守的检查，可能会误判一些复杂情况
        return !matcher.find();
    }
    
    /**
     * 生成审计日志ID
     * 
     * @return 审计日志ID
     */
    private String generateAuditId() {
        return "AUDIT-" + System.currentTimeMillis() + "-" + 
               System.identityHashCode(Thread.currentThread()) + "-" + 
               (int)(Math.random() * 1000);
    }
    
    /**
     * 记录安全审计日志
     * 
     * @param auditId 审计ID
     * @param sql SQL语句
     * @param isSafe 是否安全
     * @param reason 原因描述
     * @param startTime 开始时间
     */
    private void logSecurityAudit(String auditId, String sql, boolean isSafe, 
                                 String reason, LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();
        long durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        
        String sqlForLog = sql != null ? 
            (sql.length() > 200 ? sql.substring(0, 200) + "..." : sql) : "null";
        
        String logMessage = String.format(
            "SQL安全审计 [%s] | 时间: %s | 耗时: %dms | 安全: %s | 原因: %s | SQL: %s",
            auditId,
            startTime.format(DATE_TIME_FORMATTER),
            durationMs,
            isSafe ? "是" : "否",
            reason,
            sqlForLog
        );
        
        if (isSafe) {
            log.info(logMessage);
        } else {
            log.warn(logMessage);
        }
    }
    
    /**
     * 添加自定义安全规则
     * 
     * @param pattern 正则表达式模式
     */
    public void addCustomSecurityRule(String pattern) {
        if (pattern != null && !pattern.trim().isEmpty()) {
            customDangerousPatterns.add(pattern);
            log.info("添加自定义安全规则: {}", pattern);
        }
    }
    
    /**
     * 移除自定义安全规则
     * 
     * @param pattern 正则表达式模式
     */
    public void removeCustomSecurityRule(String pattern) {
        if (customDangerousPatterns.remove(pattern)) {
            log.info("移除自定义安全规则: {}", pattern);
        }
    }
    
    /**
     * 获取所有自定义安全规则
     * 
     * @return 自定义安全规则集合
     */
    public Set<String> getCustomSecurityRules() {
        return new HashSet<>(customDangerousPatterns);
    }
    
    /**
     * 清空所有自定义安全规则
     */
    public void clearCustomSecurityRules() {
        int count = customDangerousPatterns.size();
        customDangerousPatterns.clear();
        log.info("清空所有自定义安全规则，共{}条", count);
    }
}
