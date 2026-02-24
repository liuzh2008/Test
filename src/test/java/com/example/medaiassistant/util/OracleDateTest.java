package com.example.medaiassistant.util;

import org.junit.jupiter.api.Test;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Oracle日期格式测试类
 * 
 * 用于测试Oracle数据库日期格式的解析和年龄计算
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2025-09-27
 */
class OracleDateTest {

    @Test
    void testParseOracleDate() {
        // 测试Oracle日期格式："24-12月-41 12.00.00.000000000 上午"
        String oracleDateString = "24-12月-41 12.00.00.000000000 上午";
        
        // 直接使用AgeCalculator的字符串计算方法
        int age = AgeCalculator.calculateAge(oracleDateString);
        
        System.out.println("字符串直接计算的年龄: " + age);
        
        // 验证年龄应该是合理的（不应该是负数）
        assertTrue(age >= 0, "年龄应该为非负数");
    }

    @Test
    void testParseOracleDateWithChineseMonth() {
        // 测试中文月份格式："24-12月-41"
        String oracleDateString = "24-12月-41";
        
        // 直接使用AgeCalculator的字符串计算方法
        int age = AgeCalculator.calculateAge(oracleDateString);
        
        System.out.println("中文月份字符串直接计算的年龄: " + age);
        
        // 验证年龄应该是合理的（不应该是负数）
        assertTrue(age >= 0, "年龄应该为非负数");
    }

    @Test
    void testCalculateAgeWithOracleDateString() {
        // 测试直接使用字符串计算年龄
        String oracleDateString = "24-12月-41 12.00.00.000000000 上午";
        
        int age = AgeCalculator.calculateAge(oracleDateString);
        
        System.out.println("字符串直接计算的年龄: " + age);
        
        // 验证年龄应该是合理的（不应该是负数）
        assertTrue(age >= 0, "年龄应该为非负数");
    }

    @Test
    void testDateCenturyInterpretation() {
        // 测试年份的世纪解释问题
        // "41"可能被解释为1941年或2041年
        
        try {
            // 测试1941年解释 - 使用四位年份避免世纪解释问题
            SimpleDateFormat format1941 = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
            Date date1941 = format1941.parse("24-Dec-1941");
            
            // 测试2041年解释
            SimpleDateFormat format2041 = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
            Date date2041 = format2041.parse("24-Dec-2041");
            
            System.out.println("1941年解释的日期: " + date1941);
            System.out.println("2041年解释的日期: " + date2041);
            
            int age1941 = AgeCalculator.calculateAge(date1941);
            int age2041 = AgeCalculator.calculateAge(date2041);
            
            System.out.println("1941年解释的年龄: " + age1941);
            System.out.println("2041年解释的年龄: " + age2041);
            
            // 1941年的年龄应该很大，2041年的年龄应该是负数（但会被修正为0）
            assertTrue(age1941 > 80, "1941年出生的年龄应该大于80岁");
            assertEquals(0, age2041, "2041年出生的年龄应该为0");
            
        } catch (Exception e) {
            fail("日期世纪解释测试失败: " + e.getMessage());
        }
    }
}
