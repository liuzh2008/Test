package com.example.medaiassistant.hospital.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 医嘱同步配置类
 * 
 * <p>管理医嘱同步的模板路径、查询名称等配置。
 * 遵循动态路径构建原则，避免硬编码医院名称。</p>
 * 
 * <p><strong>设计原则</strong>：</p>
 * <ul>
 *   <li>动态路径构建 - 根据医院ID自动生成模板路径</li>
 *   <li>大小写统一 - 路径统一使用小写，避免Linux环境大小写敏感问题</li>
 *   <li>空值保护 - 空或null的医院ID使用默认值</li>
 *   <li>配置与代码分离 - 新增医院只需添加配置文件，无需修改代码</li>
 * </ul>
 * 
 * @author TDD
 * @version 1.0
 * @since 2026-01-10
 * @see com.example.medaiassistant.hospital.model.SqlTemplate
 * @see com.example.medaiassistant.hospital.service.JsonTemplateParser
 */
@Slf4j
@Component
public class OrderSyncConfig {

    /**
     * 默认医院ID
     */
    private static final String DEFAULT_HOSPITAL_ID = "hospital-Local";

    /**
     * 模板文件名
     */
    private static final String TEMPLATE_FILE_NAME = "orders-query.json";

    /**
     * 查询名称
     */
    private static final String QUERY_NAME = "getOrders";

    /**
     * SQL模板目录前缀
     */
    private static final String SQL_TEMPLATE_DIR_PREFIX = "sql/";

    /**
     * 从配置文件读取的默认医院ID
     */
    @Value("${hospital.default.id:hospital-Local}")
    private String configuredDefaultHospitalId;

    /**
     * 根据医院ID构建模板文件路径
     * 
     * <p>动态构建路径，避免硬编码医院名称。
     * 路径格式: sql/{hospitalId小写}/orders-query.json</p>
     * 
     * <p><strong>示例</strong>:
     * <ul>
     *   <li>hospital-Local → sql/hospital-local/orders-query.json</li>
     *   <li>testserver → sql/testserver/orders-query.json</li>
     *   <li>CDWYY → sql/cdwyy/orders-query.json</li>
     * </ul></p>
     * 
     * @param hospitalId 医院ID，可为空（将使用默认值）
     * @return 模板文件路径
     */
    public String getTemplateFilePath(String hospitalId) {
        String effectiveHospitalId = resolveHospitalId(hospitalId);
        String templatePath = String.format("%s%s/%s", 
            SQL_TEMPLATE_DIR_PREFIX, 
            effectiveHospitalId.toLowerCase(), 
            TEMPLATE_FILE_NAME);
        log.debug("构建医嘱模板路径 - 医院ID: {}, 路径: {}", hospitalId, templatePath);
        return templatePath;
    }

    /**
     * 获取模板文件名
     * 
     * @return 模板文件名
     */
    public String getTemplateFileName() {
        return TEMPLATE_FILE_NAME;
    }

    /**
     * 获取默认医院ID
     * 
     * @return 默认医院ID
     */
    public String getDefaultHospitalId() {
        return DEFAULT_HOSPITAL_ID;
    }

    /**
     * 获取配置的默认医院ID（从application.properties读取）
     * 
     * @return 配置的默认医院ID
     */
    public String getConfiguredDefaultHospitalId() {
        return configuredDefaultHospitalId != null ? configuredDefaultHospitalId : DEFAULT_HOSPITAL_ID;
    }

    /**
     * 获取查询名称
     * 
     * @return 查询名称
     */
    public String getQueryName() {
        return QUERY_NAME;
    }

    /**
     * 验证医院ID是否有效
     * 
     * @param hospitalId 医院ID
     * @return true表示有效，false表示无效
     */
    public boolean isValidHospitalId(String hospitalId) {
        return hospitalId != null && !hospitalId.trim().isEmpty();
    }

    /**
     * 解析有效的医院ID
     * 如果传入的hospitalId为空或null，返回默认值
     * 
     * @param hospitalId 医院ID
     * @return 有效的医院ID
     */
    private String resolveHospitalId(String hospitalId) {
        if (!isValidHospitalId(hospitalId)) {
            return DEFAULT_HOSPITAL_ID;
        }
        return hospitalId.trim();
    }

    /**
     * 获取配置摘要信息
     * 
     * @return 配置摘要
     */
    public String getSummary() {
        return String.format(
            "OrderSyncConfig{defaultHospitalId='%s', templateFileName='%s', queryName='%s'}",
            DEFAULT_HOSPITAL_ID, TEMPLATE_FILE_NAME, QUERY_NAME);
    }
}
