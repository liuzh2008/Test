package com.example.medaiassistant.drg.matching;

/**
 * ICD精确匹配器
 * 
 * 实现ICD编码的精确匹配功能，用于DRG主要诊断与手术匹配
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-22
 */
public class IcdMatcher {

    private IcdMatcher() {
        // 工具类，防止实例化
    }

    /**
     * 执行ICD编码的精确匹配
     * 
     * 匹配规则：
     * - 两个ICD编码必须完全相同（区分大小写）
     * - 如果任一ICD编码为null或空字符串，返回false
     * - 支持各种ICD编码格式（如"I48.000", "I10", "E11.900"）
     * 
     * @param patientIcd 患者的ICD编码
     * @param drgIcd DRG记录中的ICD编码
     * @return 如果ICD编码完全匹配返回true，否则返回false
     */
    public static boolean exactMatch(String patientIcd, String drgIcd) {
        // 处理null值
        if (patientIcd == null || drgIcd == null) {
            return false;
        }
        
        // 处理空字符串
        if (patientIcd.isEmpty() || drgIcd.isEmpty()) {
            return false;
        }
        
        // 精确匹配（区分大小写）
        return patientIcd.equals(drgIcd);
    }

    /**
     * 执行ICD编码的精确匹配（忽略大小写）
     * 
     * 匹配规则：
     * - 两个ICD编码必须完全相同（忽略大小写）
     * - 如果任一ICD编码为null或空字符串，返回false
     * 
     * @param patientIcd 患者的ICD编码
     * @param drgIcd DRG记录中的ICD编码
     * @return 如果ICD编码完全匹配（忽略大小写）返回true，否则返回false
     */
    public static boolean exactMatchIgnoreCase(String patientIcd, String drgIcd) {
        // 处理null值
        if (patientIcd == null || drgIcd == null) {
            return false;
        }
        
        // 处理空字符串
        if (patientIcd.isEmpty() || drgIcd.isEmpty()) {
            return false;
        }
        
        // 精确匹配（忽略大小写）
        return patientIcd.equalsIgnoreCase(drgIcd);
    }

    /**
     * 检查ICD编码是否有效
     * 
     * 有效性规则：
     * - 非null
     * - 非空字符串
     * - 至少包含一个非空白字符
     * 
     * @param icdCode ICD编码
     * @return 如果ICD编码有效返回true，否则返回false
     */
    public static boolean isValidIcdCode(String icdCode) {
        return icdCode != null && !icdCode.trim().isEmpty();
    }
}
