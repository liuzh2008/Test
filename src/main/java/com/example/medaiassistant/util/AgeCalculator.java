package com.example.medaiassistant.util;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

/**
 * 年龄计算工具类
 * 
 * 该类提供了安全的年龄计算方法，包含完整的错误处理和验证逻辑，
 * 避免年龄计算出现负数或其他异常情况。专门针对医疗AI助手系统设计，
 * 特别支持Oracle数据库日期格式解析。
 * 
 * <h2>主要功能</h2>
 * <ul>
 *   <li>安全的年龄计算，包含空值检查</li>
 *   <li>日期验证，确保出生日期不晚于当前日期</li>
 *   <li>异常处理，提供默认值避免系统崩溃</li>
 *   <li>支持多种日期类型输入，特别是Oracle数据库格式</li>
 *   <li>边界情况处理，包括未来日期和无效日期</li>
 * </ul>
 * 
 * <h2>支持的日期格式</h2>
 * <ul>
 *   <li>Oracle数据库格式："24-12月-41 12.00.00.000000000 上午"</li>
 *   <li>标准Java日期格式：yyyy-MM-dd HH:mm:ss</li>
 *   <li>中文日期格式：yyyy年MM月dd日HH:mm</li>
 *   <li>多种常见日期格式</li>
 * </ul>
 * 
 * <h2>使用示例</h2>
 * <pre>
 * {@code
 * // 基本年龄计算
 * int age = AgeCalculator.calculateAge(birthDate);
 * 
 * // 字符串日期计算
 * int age = AgeCalculator.calculateAge("24-12月-41");
 * 
 * // 获取年龄描述
 * String description = AgeCalculator.getAgeDescription(birthDate);
 * }
 * </pre>
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2025-09-27
 */
public class AgeCalculator {
    
    private AgeCalculator() {
        // 工具类，防止实例化
    }
    
    /**
     * 根据出生日期计算年龄
     * 
     * 该方法使用安全的年龄计算逻辑，包含完整的错误处理：
     * 1. 检查出生日期是否为null
     * 2. 验证出生日期是否晚于当前日期
     * 3. 处理时区转换异常
     * 4. 提供默认值避免负数年龄
     * 
     * @param dateOfBirth 出生日期，不能为null
     * @return 计算出的年龄，如果出生日期无效则返回0
     * @throws IllegalArgumentException 如果出生日期为null
     */
    public static int calculateAge(Date dateOfBirth) {
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("出生日期不能为null");
        }
        
        try {
            // 转换为LocalDate
            LocalDate birthDate = dateOfBirth.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            
            // 获取当前日期
            LocalDate currentDate = LocalDate.now();
            
            // 验证出生日期是否合理（不能晚于当前日期）
            if (birthDate.isAfter(currentDate)) {
                // 如果出生日期晚于当前日期，返回0作为默认值
                return 0;
            }
            
            // 计算年龄
            int age = Period.between(birthDate, currentDate).getYears();
            
            // 确保年龄不为负数
            return Math.max(0, age);
            
        } catch (Exception e) {
            // 处理任何可能的异常（如时区转换异常等）
            // 返回0作为默认值，避免影响系统正常运行
            return 0;
        }
    }
    
    /**
     * 根据字符串格式的出生日期计算年龄
     * 
     * 支持多种日期格式，特别是Oracle数据库格式：
     * - "24-12月-41 12.00.00.000000000 上午"
     * - "dd-MMM-yy HH.mm.ss.SSSSSSSSS a"
     * - 标准Java日期格式
     * 
     * @param dateString 出生日期字符串
     * @return 计算出的年龄，如果日期无效则返回0
     */
    public static int calculateAge(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return 0;
        }
        
        try {
            // 尝试解析Oracle日期格式
            Date date = parseOracleDate(dateString);
            if (date != null) {
                return calculateAge(date);
            }
            
            // 如果Oracle格式解析失败，尝试其他常见格式
            return calculateAge(parseDateWithMultipleFormats(dateString));
            
        } catch (Exception e) {
            // 解析失败，返回0作为默认值
            return 0;
        }
    }
    
    /**
     * 根据出生日期计算年龄，提供默认值
     * 
     * 该方法与calculateAge类似，但在出现异常时返回指定的默认值
     * 
     * @param dateOfBirth 出生日期，可以为null
     * @param defaultValue 默认年龄值，当计算失败时返回此值
     * @return 计算出的年龄，如果出生日期无效则返回默认值
     */
    public static int calculateAge(Date dateOfBirth, int defaultValue) {
        if (dateOfBirth == null) {
            return defaultValue;
        }
        
        try {
            return calculateAge(dateOfBirth);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * 验证出生日期是否有效
     * 
     * 检查出生日期是否合理：
     * 1. 不为null
     * 2. 不晚于当前日期
     * 3. 不早于合理的最小日期（1900年）
     * 
     * @param dateOfBirth 出生日期
     * @return 如果出生日期有效返回true，否则返回false
     */
    public static boolean isValidBirthDate(Date dateOfBirth) {
        if (dateOfBirth == null) {
            return false;
        }
        
        try {
            LocalDate birthDate = dateOfBirth.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            
            LocalDate currentDate = LocalDate.now();
            LocalDate minValidDate = LocalDate.of(1900, 1, 1);
            
            // 检查出生日期是否在合理范围内
            return !birthDate.isAfter(currentDate) && !birthDate.isBefore(minValidDate);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取年龄描述字符串
     * 
     * 根据年龄返回格式化的描述，如"45岁"或"未知年龄"
     * 
     * @param dateOfBirth 出生日期
     * @return 年龄描述字符串
     */
    public static String getAgeDescription(Date dateOfBirth) {
        if (dateOfBirth == null) {
            return "未知年龄";
        }
        
        // 首先验证出生日期是否有效
        if (!isValidBirthDate(dateOfBirth)) {
            return "未知年龄";
        }
        
        int age = calculateAge(dateOfBirth, -1);
        
        if (age < 0) {
            return "未知年龄";
        }
        
        return age + "岁";
    }
    
    /**
     * 获取详细的年龄信息
     * 
     * 返回包含年、月、日的完整年龄信息
     * 
     * @param dateOfBirth 出生日期
     * @return 包含年、月、日的年龄信息，格式为"X年Y月Z日"
     */
    public static String getDetailedAge(Date dateOfBirth) {
        if (dateOfBirth == null) {
            return "未知年龄";
        }
        
        try {
            LocalDate birthDate = dateOfBirth.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            
            LocalDate currentDate = LocalDate.now();
            
            if (birthDate.isAfter(currentDate)) {
                return "出生日期无效";
            }
            
            Period period = Period.between(birthDate, currentDate);
            
            return String.format("%d年%d月%d日", 
                    period.getYears(), 
                    period.getMonths(), 
                    period.getDays());
            
        } catch (Exception e) {
            return "年龄计算错误";
        }
    }
    
    /**
     * 解析Oracle数据库日期格式
     * 
     * 支持格式："24-12月-41 12.00.00.000000000 上午"
     * 特别注意：所有两位数年份都解析为1900年代（如"42"解析为1942年）
     * 
     * @param dateString Oracle日期字符串
     * @return 解析后的Date对象，解析失败返回null
     */
    public static Date parseOracleDate(String dateString) {
        try {
            // 首先检查是否包含负数年份（如"-36"）
            if (dateString.contains("-") && dateString.matches(".*-\\d{1,2}.*")) {
                // 处理负数年份的情况
                return parseOracleDateWithNegativeYear(dateString);
            }
            
            // 使用自定义的Oracle日期解析，将所有两位数年份解析为1900年代
            return parseOracleDateWithCenturyCorrection(dateString);
        } catch (Exception e) {
            System.out.println("Oracle日期解析错误: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析Oracle日期并修正世纪（所有两位数年份都解析为1900年代）
     * 
     * @param dateString Oracle日期字符串
     * @return 解析后的Date对象，解析失败返回null
     */
    private static Date parseOracleDateWithCenturyCorrection(String dateString) {
        try {
            System.out.println("解析Oracle日期: " + dateString);
            
            // 首先尝试中文月份解析
            SimpleDateFormat chineseFormat = new SimpleDateFormat("dd-MM月-yy HH.mm.ss.SSSSSSSSS a", Locale.CHINESE);
            Date date = chineseFormat.parse(dateString);
            
            // 修正年份：所有两位数年份都解析为1900年代
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(date);
            int year = cal.get(java.util.Calendar.YEAR);
            
            // 如果年份在2000-2049范围内，修正为1900年代
            if (year >= 2000 && year <= 2049) {
                int correctedYear = year - 100; // 2000-2049 -> 1900-1949
                cal.set(java.util.Calendar.YEAR, correctedYear);
                System.out.println("修正年份: " + year + " -> " + correctedYear);
            }
            
            return cal.getTime();
            
        } catch (ParseException e1) {
            try {
                // 尝试英文月份解析
                SimpleDateFormat englishFormat = new SimpleDateFormat("dd-MMM-yy HH.mm.ss.SSSSSSSSS a", Locale.ENGLISH);
                Date date = englishFormat.parse(dateString);
                
                // 修正年份
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(date);
                int year = cal.get(java.util.Calendar.YEAR);
                
                if (year >= 2000 && year <= 2049) {
                    int correctedYear = year - 100;
                    cal.set(java.util.Calendar.YEAR, correctedYear);
                    System.out.println("修正年份: " + year + " -> " + correctedYear);
                }
                
                return cal.getTime();
                
            } catch (ParseException e2) {
                // 尝试简化格式（只有日期部分）
                try {
                    // 简化格式：dd-MM月-yy
                    SimpleDateFormat simpleChineseFormat = new SimpleDateFormat("dd-MM月-yy", Locale.CHINESE);
                    Date date = simpleChineseFormat.parse(dateString);
                    
                    // 修正年份
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTime(date);
                    int year = cal.get(java.util.Calendar.YEAR);
                    
                    if (year >= 2000 && year <= 2049) {
                        int correctedYear = year - 100;
                        cal.set(java.util.Calendar.YEAR, correctedYear);
                        System.out.println("修正年份: " + year + " -> " + correctedYear);
                    }
                    
                    return cal.getTime();
                    
                } catch (ParseException e3) {
                    try {
                        // 简化格式：dd-MMM-yy（英文）
                        SimpleDateFormat simpleEnglishFormat = new SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH);
                        Date date = simpleEnglishFormat.parse(dateString);
                        
                        // 修正年份
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        cal.setTime(date);
                        int year = cal.get(java.util.Calendar.YEAR);
                        
                        if (year >= 2000 && year <= 2049) {
                            int correctedYear = year - 100;
                            cal.set(java.util.Calendar.YEAR, correctedYear);
                            System.out.println("修正年份: " + year + " -> " + correctedYear);
                        }
                        
                        return cal.getTime();
                        
                    } catch (ParseException e4) {
                        return null;
                    }
                }
            }
        }
    }
    
    /**
     * 解析包含负数年份的Oracle日期格式
     * 例如："13-4月 -36 12.00.00.000000000 上午" 应该解析为1936年
     * 
     * @param dateString 包含负数年份的Oracle日期字符串
     * @return 解析后的Date对象，解析失败返回null
     */
    private static Date parseOracleDateWithNegativeYear(String dateString) {
        try {
            System.out.println("处理负数年份日期: " + dateString);
            
            // 提取年份部分（如"-36"）
            String[] parts = dateString.split(" ");
            String yearPart = null;
            for (String part : parts) {
                if (part.startsWith("-") && part.length() <= 3) {
                    yearPart = part;
                    break;
                }
            }
            
            if (yearPart != null) {
                // 提取年份数字（去掉负号）
                int yearValue = Integer.parseInt(yearPart.substring(1));
                
                // 负数年份应该解析为1900年代
                int correctedYear = 1900 + yearValue;
                
                // 构建正确的日期字符串（将"-36"替换为"36"）
                String correctedDateString = dateString.replace(yearPart, String.valueOf(yearValue));
                
                // 使用正常的解析逻辑
                SimpleDateFormat chineseFormat = new SimpleDateFormat("dd-MM月-yy HH.mm.ss.SSSSSSSSS a", Locale.CHINESE);
                Date date = chineseFormat.parse(correctedDateString);
                
                // 手动设置正确的年份
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(date);
                cal.set(java.util.Calendar.YEAR, correctedYear);
                
                System.out.println("修正后的年份: " + correctedYear + ", 原始年份: " + yearPart);
                return cal.getTime();
            }
        } catch (Exception e) {
            System.out.println("负数年份解析错误: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 格式化日期为Oracle数据库格式
     * 使用格式：DD-MM-YYYY HH24.MI.SS.FF
     * 
     * @param date 要格式化的日期
     * @return 格式化后的日期字符串，如果日期为null则返回空字符串
     */
    public static String formatOracleDate(Date date) {
        if (date == null) {
            return "";
        }
        
        try {
            return date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH.mm.ss.SSS"));
        } catch (Exception e) {
            return "日期格式错误";
        }
    }
    
    
    /**
     * 使用多种格式尝试解析日期字符串
     * 
     * @param dateString 日期字符串
     * @return 解析后的Date对象
     * @throws ParseException 所有格式都解析失败时抛出异常
     */
    private static Date parseDateWithMultipleFormats(String dateString) throws ParseException {
        String[] formats = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyyMMdd HHmmss",
            "yyyy年MM月dd日HH:mm",
            "dd-MM-yyyy",
            "MM/dd/yyyy"
        };
        
        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                return sdf.parse(dateString);
            } catch (ParseException e) {
                // 继续尝试下一种格式
                continue;
            }
        }
        
        throw new ParseException("无法解析日期字符串: " + dateString, 0);
    }
}
