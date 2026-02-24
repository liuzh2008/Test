package com.example.medaiassistant.hospital.util;

import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日期转换工具类
 * 用于在Oracle数据格式和Java Date类型之间转换
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-08
 */
@Slf4j
public class DateConverter {
    
    /**
     * 将对象转换为Date类型
     * 支持多种日期格式的转换
     * 
     * @param dateObj 日期对象，可以是Date、java.sql.Date、java.sql.Timestamp或String
     * @return 转换后的Date对象，如果转换失败则返回null
     */
    public static Date convertToDate(Object dateObj) {
        if (dateObj == null) {
            return null;
        }
        
        if (dateObj instanceof Date) {
            return (Date) dateObj;
        } else if (dateObj instanceof java.sql.Date) {
            return new Date(((java.sql.Date) dateObj).getTime());
        } else if (dateObj instanceof java.sql.Timestamp) {
            return new Date(((java.sql.Timestamp) dateObj).getTime());
        } else if (dateObj instanceof String) {
            return parseStringToDate((String) dateObj);
        }
        
        log.warn("不支持的日期类型: {}", dateObj.getClass().getName());
        return null;
    }
    
    /**
     * 解析字符串为Date对象
     * 支持多种日期格式
     */
    private static Date parseStringToDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        // 常见日期格式
        String[] patterns = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd",
            "yyyyMMdd HH:mm:ss",
            "yyyyMMdd"
        };
        
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern);
                format.setLenient(false); // 严格模式
                return format.parse(dateStr);
            } catch (ParseException e) {
                // 继续尝试下一个格式
            }
        }
        
        log.warn("日期字符串解析失败: {}", dateStr);
        return null;
    }
    
    /**
     * 格式化Date对象为字符串
     * 
     * @param date 日期对象
     * @param pattern 格式模式
     * @return 格式化后的字符串，如果date为null则返回空字符串
     */
    public static String formatDate(Date date, String pattern) {
        if (date == null) {
            return "";
        }
        
        try {
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            return format.format(date);
        } catch (Exception e) {
            log.warn("日期格式化失败: {}, pattern: {}", date, pattern, e);
            return "";
        }
    }
    
    /**
     * 格式化Date对象为Oracle日期字符串
     * 使用格式：yyyy-MM-dd HH:mm:ss
     * 
     * @param date 日期对象
     * @return Oracle格式的日期字符串
     */
    public static String formatToOracleDate(Date date) {
        return formatDate(date, "yyyy-MM-dd HH:mm:ss");
    }
}
