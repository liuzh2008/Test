package com.example.medaiassistant.hospital.util;

/**
 * 患者ID解析工具类
 * <p>用于解析主服务器患者ID，提取 PATI_ID 和 VISIT_ID</p>
 * 
 * <p><strong>支持的格式</strong>：</p>
 * <ul>
 *   <li>{@code 990500001204401_1} - 下划线分隔符</li>
 *   <li>{@code 990500001204401-1} - 连字符分隔符</li>
 * </ul>
 * 
 * <p><strong>解析规则</strong>：</p>
 * <ul>
 *   <li>优先使用下划线 {@code _} 作为分隔符</li>
 *   <li>如果没有下划线，则使用连字符 {@code -}</li>
 *   <li>使用 lastIndexOf 查找分隔符，处理ID中可能存在多个分隔符的情况</li>
 * </ul>
 * 
 * @author TDD
 * @version 1.0
 * @since 2026-01-12
 * @see com.example.medaiassistant.hospital.service.EmrSyncService
 * @see com.example.medaiassistant.hospital.service.OrderSyncService
 * @see com.example.medaiassistant.hospital.service.LabSyncService
 */
public final class PatientIdParser {
    
    /**
     * 私有构造函数，防止实例化
     */
    private PatientIdParser() {
    }
    
    /**
     * 解析患者ID（PATI_ID）
     * <p>从主服务器患者ID中提取 PATI_ID 部分</p>
     * 
     * <p><strong>示例</strong>：</p>
     * <ul>
     *   <li>{@code 990500001204401_1} → {@code 990500001204401}</li>
     *   <li>{@code 990500001204401-1} → {@code 990500001204401}</li>
     *   <li>{@code 990500001204401} → {@code 990500001204401}（无分隔符，原样返回）</li>
     * </ul>
     * 
     * @param patientId 主服务器患者ID（格式：PATI_ID_VISIT_ID 或 PATI_ID-VISIT_ID）
     * @return PATI_ID 部分，如果无分隔符则返回原始值
     */
    public static String parsePatiId(String patientId) {
        if (patientId == null || patientId.isEmpty()) {
            return patientId;
        }
        
        // 优先查找下划线
        int underscoreIndex = patientId.lastIndexOf('_');
        if (underscoreIndex > 0) {
            return patientId.substring(0, underscoreIndex);
        }
        
        // 如果没有下划线，查找连字符
        int dashIndex = patientId.lastIndexOf('-');
        if (dashIndex > 0) {
            return patientId.substring(0, dashIndex);
        }
        
        return patientId;
    }
    
    /**
     * 解析住院次数（VISIT_ID）为字符串
     * <p>从主服务器患者ID中提取 VISIT_ID 部分</p>
     * 
     * <p><strong>示例</strong>：</p>
     * <ul>
     *   <li>{@code 990500001204401_1} → {@code "1"}</li>
     *   <li>{@code 990500001204401-1} → {@code "1"}</li>
     *   <li>{@code 990500001204401} → {@code "1"}（无分隔符，默认返回"1"）</li>
     * </ul>
     * 
     * @param patientId 主服务器患者ID（格式：PATI_ID_VISIT_ID 或 PATI_ID-VISIT_ID）
     * @return VISIT_ID 字符串，如果无分隔符则返回默认值 "1"
     */
    public static String parseVisitIdAsString(String patientId) {
        if (patientId == null || patientId.isEmpty()) {
            return "1";
        }
        
        // 优先查找下划线
        int underscoreIndex = patientId.lastIndexOf('_');
        if (underscoreIndex > 0 && underscoreIndex < patientId.length() - 1) {
            return patientId.substring(underscoreIndex + 1);
        }
        
        // 如果没有下划线，查找连字符
        int dashIndex = patientId.lastIndexOf('-');
        if (dashIndex > 0 && dashIndex < patientId.length() - 1) {
            return patientId.substring(dashIndex + 1);
        }
        
        return "1";
    }
    
    /**
     * 解析住院次数（VISIT_ID）为 Integer 类型
     * <p>从主服务器患者ID中提取 VISIT_ID 并转换为 Integer</p>
     * 
     * <p><strong>示例</strong>：</p>
     * <ul>
     *   <li>{@code 990500001204401_1} → {@code 1}</li>
     *   <li>{@code 990500001204401-5} → {@code 5}</li>
     *   <li>{@code 990500001204401} → {@code 1}（无分隔符，默认返回1）</li>
     * </ul>
     * 
     * @param patientId 主服务器患者ID（格式：PATI_ID_VISIT_ID 或 PATI_ID-VISIT_ID）
     * @return VISIT_ID 整数值，解析失败返回 1
     */
    public static Integer parseVisitId(String patientId) {
        String visitIdStr = parseVisitIdAsString(patientId);
        try {
            return Integer.parseInt(visitIdStr);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
    
    /**
     * 检查患者ID是否包含分隔符
     * 
     * @param patientId 待检查的患者ID
     * @return true 如果包含下划线或连字符分隔符
     */
    public static boolean hasSeparator(String patientId) {
        if (patientId == null || patientId.isEmpty()) {
            return false;
        }
        return patientId.contains("_") || patientId.contains("-");
    }
}
