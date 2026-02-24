package com.example.medaiassistant.util;

import org.junit.jupiter.api.Test;
import java.util.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AgeCalculator测试类
 * 
 * 该类用于测试AgeCalculator工具类的各种功能，包括：
 * - 正常年龄计算
 * - 边界情况处理
 * - 异常情况处理
 * - 日期验证功能
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2025-09-27
 */
class AgeCalculatorTest {

    @Test
    void testCalculateAge_NormalCase() {
        // 测试正常情况：出生日期是30年前
        LocalDate birthDate = LocalDate.now().minusYears(30);
        Date dateOfBirth = Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        int age = AgeCalculator.calculateAge(dateOfBirth);
        
        assertEquals(30, age, "年龄计算应该为30岁");
    }

    @Test
    void testCalculateAge_BirthdayToday() {
        // 测试生日当天：出生日期是今天
        LocalDate birthDate = LocalDate.now();
        Date dateOfBirth = Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        int age = AgeCalculator.calculateAge(dateOfBirth);
        
        assertEquals(0, age, "生日当天年龄应该为0岁");
    }

    @Test
    void testCalculateAge_BirthdayTomorrow() {
        // 测试生日明天：出生日期是明天（未来日期）
        LocalDate birthDate = LocalDate.now().plusDays(1);
        Date dateOfBirth = Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        int age = AgeCalculator.calculateAge(dateOfBirth);
        
        assertEquals(0, age, "未来出生日期应该返回0作为默认值");
    }

    @Test
    void testCalculateAge_FutureDate() {
        // 测试未来日期：出生日期是1年后
        LocalDate birthDate = LocalDate.now().plusYears(1);
        Date dateOfBirth = Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        int age = AgeCalculator.calculateAge(dateOfBirth);
        
        assertEquals(0, age, "未来出生日期应该返回0作为默认值");
    }

    @Test
    void testCalculateAge_WithDefaultValue() {
        // 测试使用默认值的情况
        Date nullDate = null;
        int defaultValue = -1;
        
        int age = AgeCalculator.calculateAge(nullDate, defaultValue);
        
        assertEquals(defaultValue, age, "null日期应该返回指定的默认值");
    }

    @Test
    void testCalculateAge_NullDateThrowsException() {
        // 测试null日期抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            AgeCalculator.calculateAge((Date) null);
        }, "null日期应该抛出IllegalArgumentException");
    }

    @Test
    void testIsValidBirthDate_ValidDate() {
        // 测试有效出生日期
        LocalDate birthDate = LocalDate.now().minusYears(25);
        Date dateOfBirth = Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        boolean isValid = AgeCalculator.isValidBirthDate(dateOfBirth);
        
        assertTrue(isValid, "25年前的出生日期应该是有效的");
    }

    @Test
    void testIsValidBirthDate_FutureDate() {
        // 测试未来出生日期
        LocalDate birthDate = LocalDate.now().plusYears(1);
        Date dateOfBirth = Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        boolean isValid = AgeCalculator.isValidBirthDate(dateOfBirth);
        
        assertFalse(isValid, "未来出生日期应该是无效的");
    }

    @Test
    void testIsValidBirthDate_TooOldDate() {
        // 测试过于古老的出生日期（早于1900年）
        LocalDate birthDate = LocalDate.of(1899, 12, 31);
        Date dateOfBirth = Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        boolean isValid = AgeCalculator.isValidBirthDate(dateOfBirth);
        
        assertFalse(isValid, "1899年的出生日期应该是无效的");
    }

    @Test
    void testIsValidBirthDate_NullDate() {
        // 测试null日期
        boolean isValid = AgeCalculator.isValidBirthDate(null);
        
        assertFalse(isValid, "null日期应该是无效的");
    }

    @Test
    void testGetAgeDescription_NormalCase() {
        // 测试正常年龄描述
        LocalDate birthDate = LocalDate.now().minusYears(45);
        Date dateOfBirth = Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        String description = AgeCalculator.getAgeDescription(dateOfBirth);
        
        assertEquals("45岁", description, "年龄描述应该为'45岁'");
    }

    @Test
    void testGetAgeDescription_NullDate() {
        // 测试null日期的年龄描述
        String description = AgeCalculator.getAgeDescription(null);
        
        assertEquals("未知年龄", description, "null日期应该返回'未知年龄'");
    }

    @Test
    void testGetAgeDescription_FutureDate() {
        // 测试未来日期的年龄描述
        LocalDate birthDate = LocalDate.now().plusYears(1);
        Date dateOfBirth = Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        String description = AgeCalculator.getAgeDescription(dateOfBirth);
        
        assertEquals("未知年龄", description, "未来出生日期应该返回'未知年龄'");
    }

    @Test
    void testGetDetailedAge_NormalCase() {
        // 测试详细年龄信息
        LocalDate birthDate = LocalDate.now().minusYears(2).minusMonths(3).minusDays(15);
        Date dateOfBirth = Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        String detailedAge = AgeCalculator.getDetailedAge(dateOfBirth);
        
        assertTrue(detailedAge.contains("年") && detailedAge.contains("月") && detailedAge.contains("日"),
                "详细年龄应该包含年、月、日信息");
    }

    @Test
    void testGetDetailedAge_NullDate() {
        // 测试null日期的详细年龄信息
        String detailedAge = AgeCalculator.getDetailedAge(null);
        
        assertEquals("未知年龄", detailedAge, "null日期应该返回'未知年龄'");
    }

    @Test
    void testGetDetailedAge_FutureDate() {
        // 测试未来日期的详细年龄信息
        LocalDate birthDate = LocalDate.now().plusYears(1);
        Date dateOfBirth = Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        String detailedAge = AgeCalculator.getDetailedAge(dateOfBirth);
        
        assertEquals("出生日期无效", detailedAge, "未来出生日期应该返回'出生日期无效'");
    }

    @Test
    void testCalculateAge_EdgeCase_LeapYear() {
        // 测试闰年边界情况：2月29日出生
        LocalDate birthDate = LocalDate.of(2000, 2, 29); // 闰年
        Date dateOfBirth = Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        int age = AgeCalculator.calculateAge(dateOfBirth);
        
        // 验证年龄计算正确（非闰年时应该正确处理）
        assertTrue(age >= 0, "闰年出生日期应该正确计算年龄");
    }

    @Test
    void testCalculateAge_EdgeCase_YearBoundary() {
        // 测试年份边界情况：确保测试日期在当前日期之前
        LocalDate currentDate = LocalDate.now();
        LocalDate birthDate = currentDate.minusYears(1).minusDays(1); // 确保是过去日期
        
        Date dateOfBirth = Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        int age = AgeCalculator.calculateAge(dateOfBirth);
        
        // 年龄应该至少为1岁
        assertTrue(age >= 1, "过去日期出生应该至少为1岁");
    }

    @Test
    void testCalculateAge_EdgeCase_NewYear() {
        // 测试新年边界情况：今年1月1日出生
        LocalDate birthDate = LocalDate.now().withMonth(1).withDayOfMonth(1);
        Date dateOfBirth = Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        int age = AgeCalculator.calculateAge(dateOfBirth);
        
        // 年龄应该为0或1，取决于当前日期
        assertTrue(age == 0 || age == 1, "今年1月1日出生年龄应该在0-1岁之间");
    }

    @Test
    void testCalculateAge_Consistency() {
        // 测试计算一致性：多次计算应该得到相同结果
        LocalDate birthDate = LocalDate.now().minusYears(40);
        Date dateOfBirth = Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        int age1 = AgeCalculator.calculateAge(dateOfBirth);
        int age2 = AgeCalculator.calculateAge(dateOfBirth);
        int age3 = AgeCalculator.calculateAge(dateOfBirth);
        
        assertEquals(age1, age2, "多次计算应该得到相同结果");
        assertEquals(age2, age3, "多次计算应该得到相同结果");
        assertEquals(age1, age3, "多次计算应该得到相同结果");
    }
}
