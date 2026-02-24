package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.model.HospitalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态JdbcTemplate工厂
 * 负责根据医院配置动态创建和管理JdbcTemplate实例
 * 支持HIS和LIS数据库连接
 */
@Component
public class DynamicJdbcTemplateFactory {
    
    private static final Logger log = LoggerFactory.getLogger(DynamicJdbcTemplateFactory.class);
    
    /**
     * Oracle JDBC驱动类
     */
    private static final String ORACLE_DRIVER_CLASS = "oracle.jdbc.OracleDriver";
    
    /**
     * JdbcTemplate缓存
     * key: 缓存键（hospitalId + ":" + databaseType）
     * value: JdbcTemplate实例
     */
    private final Map<String, JdbcTemplate> jdbcTemplateCache = new ConcurrentHashMap<>();
    
    /**
     * 数据源缓存
     * key: 缓存键（hospitalId + ":" + databaseType）
     * value: DataSource实例
     */
    private final Map<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();
    
    private final HospitalConfigService hospitalConfigService;
    
    public DynamicJdbcTemplateFactory(HospitalConfigService hospitalConfigService) {
        this.hospitalConfigService = hospitalConfigService;
    }
    
    /**
     * 获取JdbcTemplate实例
     * 
     * @param hospitalId 医院ID
     * @param databaseType 数据库类型（his/lis）
     * @return JdbcTemplate实例
     * @throws IllegalArgumentException 如果医院配置不存在或无效
     * @throws RuntimeException 如果数据库连接失败
     */
    public JdbcTemplate getJdbcTemplate(String hospitalId, String databaseType) {
        String cacheKey = buildCacheKey(hospitalId, databaseType);
        
        // 检查缓存
        JdbcTemplate cachedTemplate = jdbcTemplateCache.get(cacheKey);
        if (cachedTemplate != null) {
            log.debug("从缓存获取JdbcTemplate: {}", cacheKey);
            return cachedTemplate;
        }
        
        // 创建新的JdbcTemplate
        log.info("创建新的JdbcTemplate: {}", cacheKey);
        JdbcTemplate jdbcTemplate = createJdbcTemplate(hospitalId, databaseType);
        
        // 放入缓存
        jdbcTemplateCache.put(cacheKey, jdbcTemplate);
        
        return jdbcTemplate;
    }
    
    /**
     * 创建JdbcTemplate实例
     */
    private JdbcTemplate createJdbcTemplate(String hospitalId, String databaseType) {
        DataSource dataSource = createDataSource(hospitalId, databaseType);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        
        // 配置JdbcTemplate
        configureJdbcTemplate(jdbcTemplate);
        
        return jdbcTemplate;
    }
    
    /**
     * 创建数据源
     */
    private DataSource createDataSource(String hospitalId, String databaseType) {
        String cacheKey = buildCacheKey(hospitalId, databaseType);
        
        // 检查数据源缓存
        DataSource cachedDataSource = dataSourceCache.get(cacheKey);
        if (cachedDataSource != null) {
            return cachedDataSource;
        }
        
        // 获取医院配置
        HospitalConfig config = hospitalConfigService.getConfig(hospitalId);
        if (config == null) {
            throw new IllegalArgumentException("医院配置不存在: " + hospitalId);
        }
        
        // 验证集成类型
        if (!"database".equals(config.getIntegrationType())) {
            throw new IllegalArgumentException("医院 " + hospitalId + " 的集成类型不是database，无法创建数据库连接");
        }
        
        // 获取数据库配置
        HospitalConfig.HisConfig hisConfig = config.getHisConfig();
        HospitalConfig.LisConfig lisConfig = config.getLisConfig();
        
        String url;
        String username;
        String password;
        
        if ("his".equalsIgnoreCase(databaseType)) {
            if (hisConfig == null) {
                throw new IllegalArgumentException("医院 " + hospitalId + " 未配置HIS数据库");
            }
            url = hisConfig.getUrl();
            username = hisConfig.getUsername();
            password = hisConfig.getPassword();
        } else if ("lis".equalsIgnoreCase(databaseType)) {
            if (lisConfig == null) {
                throw new IllegalArgumentException("医院 " + hospitalId + " 未配置LIS数据库");
            }
            url = lisConfig.getUrl();
            username = lisConfig.getUsername();
            password = lisConfig.getPassword();
        } else {
            throw new IllegalArgumentException("不支持的数据库类型: " + databaseType + "，支持的类型: his, lis");
        }
        
        // 验证配置完整性
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("数据库URL不能为空");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("数据库用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("数据库密码不能为空");
        }
        
        // 创建数据源
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(ORACLE_DRIVER_CLASS);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        
        // 配置连接池属性
        configureDataSource(dataSource);
        
        // 放入缓存
        dataSourceCache.put(cacheKey, dataSource);
        
        log.info("创建数据源成功: {} - {}", cacheKey, url);
        return dataSource;
    }
    
    /**
     * 配置数据源连接池属性
     */
    private void configureDataSource(DriverManagerDataSource dataSource) {
        // 设置连接池属性
        // 注意：DriverManagerDataSource是简单的数据源实现，不支持连接池
        // 如果需要连接池功能，可以考虑使用HikariCP或其他连接池
        // 这里使用简单实现，适合轻量级使用场景
        
        // 可以添加连接验证查询
        // dataSource.setConnectionProperties("validationQuery=SELECT 1 FROM DUAL");
    }
    
    /**
     * 配置JdbcTemplate
     */
    private void configureJdbcTemplate(JdbcTemplate jdbcTemplate) {
        // 设置查询超时（秒）
        jdbcTemplate.setQueryTimeout(30);
        
        // 设置最大行数限制
        jdbcTemplate.setMaxRows(1000);
        
        // 设置获取大小（每次从数据库获取的行数）
        jdbcTemplate.setFetchSize(100);
        
        // 启用SQL日志（调试用）
        jdbcTemplate.setSkipResultsProcessing(false);
    }
    
    /**
     * 测试数据库连接
     * 
     * @param hospitalId 医院ID
     * @param databaseType 数据库类型
     * @return 连接测试结果
     */
    public boolean testConnection(String hospitalId, String databaseType) {
        try {
            JdbcTemplate jdbcTemplate = getJdbcTemplate(hospitalId, databaseType);
            // 执行简单的测试查询
            Integer result = jdbcTemplate.queryForObject("SELECT 1 FROM DUAL", Integer.class);
            return result != null && result == 1;
        } catch (Exception e) {
            log.error("数据库连接测试失败: {} - {}", hospitalId, databaseType, e);
            return false;
        }
    }
    
    /**
     * 清除缓存
     * 
     * @param hospitalId 医院ID（可选，如果为null则清除所有缓存）
     * @param databaseType 数据库类型（可选）
     */
    public void clearCache(String hospitalId, String databaseType) {
        if (hospitalId == null) {
            // 清除所有缓存
            jdbcTemplateCache.clear();
            dataSourceCache.clear();
            log.info("清除所有JdbcTemplate缓存");
        } else if (databaseType == null) {
            // 清除指定医院的所有缓存
            String hisKey = buildCacheKey(hospitalId, "his");
            String lisKey = buildCacheKey(hospitalId, "lis");
            jdbcTemplateCache.remove(hisKey);
            jdbcTemplateCache.remove(lisKey);
            dataSourceCache.remove(hisKey);
            dataSourceCache.remove(lisKey);
            log.info("清除医院 {} 的所有JdbcTemplate缓存", hospitalId);
        } else {
            // 清除指定医院和数据库类型的缓存
            String cacheKey = buildCacheKey(hospitalId, databaseType);
            jdbcTemplateCache.remove(cacheKey);
            dataSourceCache.remove(cacheKey);
            log.info("清除JdbcTemplate缓存: {}", cacheKey);
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        return Map.of(
            "jdbcTemplateCacheSize", jdbcTemplateCache.size(),
            "dataSourceCacheSize", dataSourceCache.size(),
            "jdbcTemplateCacheKeys", jdbcTemplateCache.keySet(),
            "dataSourceCacheKeys", dataSourceCache.keySet()
        );
    }
    
    /**
     * 构建缓存键
     */
    private String buildCacheKey(String hospitalId, String databaseType) {
        return hospitalId + ":" + databaseType.toLowerCase();
    }
    
    /**
     * 获取数据源URL（用于调试）
     */
    public String getDataSourceUrl(String hospitalId, String databaseType) {
        try {
            DataSource dataSource = createDataSource(hospitalId, databaseType);
            if (dataSource instanceof DriverManagerDataSource) {
                return ((DriverManagerDataSource) dataSource).getUrl();
            }
            return "未知数据源类型";
        } catch (Exception e) {
            return "获取数据源URL失败: " + e.getMessage();
        }
    }
}
