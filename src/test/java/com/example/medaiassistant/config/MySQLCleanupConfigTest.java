package com.example.medaiassistant.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MySQL清理配置文件验证测试
 * 
 * 按照TDD红-绿-重构流程实施阶段1：配置文件清理
 * 
 * @author Cline
 * @since 2025-11-02
 */
@DisplayName("MySQL清理 配置文件验证测试")
class MySQLCleanupConfigTest {

    private static final String[] MYSQL_CONFIG_KEYS = {
        "spring.datasource.mysql.url",
        "spring.datasource.mysql.driverClassName", 
        "spring.datasource.mysql.username",
        "spring.datasource.mysql.password"
    };

    private static final String[] ORACLE_CONFIG_KEYS = {
        "spring.datasource.url",
        "spring.datasource.driver-class-name",
        "spring.datasource.username",
        "spring.datasource.password"
    };

    private static final String[] ORACLE_SERVER_CONFIG_KEYS = {
        "oracle.server.active",
        "oracle.server.local.ip",
        "oracle.server.local.sid",
        "oracle.server.local.username",
        "oracle.server.local.password"
    };

    /**
     * ✅ 完善测试用例1：验证application.properties不包含MySQL配置
     */
    @Test
    @DisplayName("验证application.properties不包含MySQL配置")
    void testApplicationPropertiesNoMySQLConfig() throws IOException {
        Properties properties = loadProperties("application.properties");
        
        assertThat(properties.stringPropertyNames())
            .as("application.properties应该不包含MySQL配置")
            .doesNotContain(MYSQL_CONFIG_KEYS);
    }

    /**
     * ✅ 完善测试用例2：验证生产配置仅包含Oracle配置
     */
    @Test
    @DisplayName("验证生产配置仅包含Oracle配置")
    void testProdPropertiesOracleOnly() throws IOException {
        Properties properties = loadProperties("application-prod.properties");
        
        assertThat(properties.stringPropertyNames())
            .as("生产配置应该包含Oracle数据源配置")
            .contains(ORACLE_CONFIG_KEYS);
        
        assertThat(properties.stringPropertyNames())
            .as("生产配置应该不包含MySQL相关配置")
            .filteredOn(key -> key.contains("mysql"))
            .isEmpty();
    }

    /**
     * ✅ 完善测试用例3：验证配置文件语法正确性
     */
    @Test
    @DisplayName("验证配置文件语法正确性")
    void testConfigFilesSyntaxValid() throws IOException {
        assertThat(loadProperties("application.properties"))
            .as("application.properties语法应该正确")
            .isNotNull();
        
        assertThat(loadProperties("application-prod.properties"))
            .as("application-prod.properties语法应该正确")
            .isNotNull();
    }

    /**
     * ✅ 完善测试用例4：验证数据源类型配置正确
     */
    @Test
    @DisplayName("验证数据源类型配置正确")
    void testDataSourceTypeConfig() throws IOException {
        Properties properties = loadProperties("application.properties");
        
        assertThat(properties.getProperty("app.datasource.type"))
            .as("数据源类型应该配置为Oracle")
            .isEqualTo("oracle");
    }

    /**
     * ✅ 新增测试用例5：验证Oracle服务器配置完整性
     */
    @Test
    @DisplayName("验证Oracle服务器配置完整性")
    void testOracleServerConfigComplete() throws IOException {
        Properties properties = loadProperties("application.properties");
        
        assertThat(properties.stringPropertyNames())
            .as("Oracle服务器配置应该完整")
            .contains(ORACLE_SERVER_CONFIG_KEYS);
        
        // 验证关键配置不为空
        assertThat(properties.getProperty("oracle.server.active"))
            .as("Oracle服务器激活配置不能为空")
            .isNotNull()
            .isNotEmpty();
            
        assertThat(properties.getProperty("oracle.server.local.ip"))
            .as("Oracle服务器IP配置不能为空")
            .isNotNull()
            .isNotEmpty();
    }

    /**
     * ✅ 新增测试用例6：验证数据库连接池配置
     */
    @Test
    @DisplayName("验证数据库连接池配置")
    void testDatabaseConnectionPoolConfig() throws IOException {
        Properties properties = loadProperties("application.properties");
        
        // 验证连接池关键配置存在
        assertThat(properties.stringPropertyNames())
            .as("连接池配置应该存在")
            .contains(
                "spring.datasource.hikari.maximum-pool-size",
                "spring.datasource.hikari.connection-timeout",
                "spring.datasource.hikari.connection-test-query"
            );
        
        // 验证连接池配置值合理
        String maxPoolSize = properties.getProperty("spring.datasource.hikari.maximum-pool-size");
        assertThat(maxPoolSize)
            .as("连接池最大连接数应该为正整数")
            .isNotNull()
            .matches("\\d+");
            
        int maxPoolSizeValue = Integer.parseInt(maxPoolSize);
        assertThat(maxPoolSizeValue)
            .as("连接池最大连接数应该在合理范围内")
            .isGreaterThan(0)
            .isLessThanOrEqualTo(50);
    }

    /**
     * ✅ 新增测试用例7：验证配置文件路径正确性
     */
    @ParameterizedTest
    @ValueSource(strings = {"application.properties", "application-prod.properties"})
    @DisplayName("验证配置文件路径正确性")
    void testConfigFilePathsValid(String configFile) {
        assertThat(new ClassPathResource(configFile).exists())
            .as("配置文件 " + configFile + " 应该存在")
            .isTrue();
    }

    /**
     * ✅ 新增测试用例8：验证配置值格式正确性
     */
    @Test
    @DisplayName("验证配置值格式正确性")
    void testConfigValuesFormat() throws IOException {
        Properties properties = loadProperties("application.properties");
        
        // 验证端口配置格式
        String port = properties.getProperty("server.port");
        assertThat(port)
            .as("服务器端口应该为数字")
            .isNotNull()
            .matches("\\d+");
            
        int portValue = Integer.parseInt(port);
        assertThat(portValue)
            .as("服务器端口应该在有效范围内")
            .isGreaterThan(0)
            .isLessThan(65536);
            
        // 验证布尔值配置格式
        String devToolsEnabled = properties.getProperty("spring.devtools.restart.enabled");
        assertThat(devToolsEnabled)
            .as("DevTools配置应该为布尔值")
            .isIn("true", "false");
    }

    /**
     * ✅ 新增测试用例9：验证配置依赖关系
     */
    @Test
    @DisplayName("验证配置依赖关系")
    void testConfigDependencies() throws IOException {
        Properties properties = loadProperties("application.properties");
        
        // 如果启用了定时任务，则相关配置应该存在
        String autoExecuteEnabled = properties.getProperty("scheduling.auto-execute.enabled");
        if ("true".equals(autoExecuteEnabled)) {
            assertThat(properties.stringPropertyNames())
                .as("当自动执行启用时，相关配置应该存在")
                .contains(
                    "scheduling.auto-execute.interval",
                    "scheduling.auto-execute.max-threads"
                );
        }
        
        // 如果启用了监控，则监控配置应该存在
        String monitoringEnabled = properties.getProperty("monitoring.metrics.enabled");
        if ("true".equals(monitoringEnabled)) {
            assertThat(properties.stringPropertyNames())
                .as("当监控启用时，监控配置应该存在")
                .contains(
                    "monitoring.metrics.snapshot.interval",
                    "monitoring.metrics.retention.days"
                );
        }
    }

    /**
     * ✅ 新增测试用例10：验证配置文件不包含敏感信息
     */
    @Test
    @DisplayName("验证配置文件不包含敏感信息")
    void testConfigNoSensitiveInfo() throws IOException {
        Properties properties = loadProperties("application.properties");
        
        // 验证不包含明显的敏感信息模式
        assertThat(properties.stringPropertyNames())
            .as("配置键不应该包含明显的敏感信息")
            .noneMatch(key -> 
                key.toLowerCase().contains("secret") ||
                key.toLowerCase().contains("private") ||
                key.toLowerCase().contains("key") && 
                !key.contains("api.base") && 
                !key.contains("deepseek")
            );
    }

    /**
     * 辅助方法：加载配置文件
     */
    private Properties loadProperties(String fileName) throws IOException {
        return PropertiesLoaderUtils.loadProperties(new ClassPathResource(fileName));
    }
}
