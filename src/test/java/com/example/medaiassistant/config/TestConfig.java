package com.example.medaiassistant.config;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据访问层测试配置注解
 * 简化测试配置并提高代码复用性
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-02
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    // 使用环境变量配置数据库连接（安全实践）
    "spring.datasource.url=${TEST_DB_URL:jdbc:oracle:thin:@127.0.0.1:1521/FREE}",
    "spring.datasource.driver-class-name=oracle.jdbc.OracleDriver", 
    "spring.datasource.username=${TEST_DB_USERNAME:system}",
    "spring.datasource.password=${TEST_DB_PASSWORD:Liuzh_123}",
    
    // 禁用DDL和Schema管理（与项目配置一致）
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.properties.hibernate.hbm2ddl.auto=none",
    "spring.jpa.database-platform=org.hibernate.dialect.OracleDialect",
    
    // 关闭Web和调度组件
    "spring.main.web-application-type=none",
    "spring.task.scheduling.enabled=false",
    "scheduling.auto-execute.enabled=false",
    "prompt.submission.enabled=false", 
    "prompt.polling.enabled=false",
    "monitoring.metrics.enabled=false",
    
    // 连接池配置（与项目一致）
    "spring.datasource.hikari.connection-test-query=SELECT 1 FROM DUAL",
    "spring.datasource.hikari.connection-init-sql=ALTER SESSION SET NLS_DATE_FORMAT='YYYY-MM-DD HH24:MI:SS'",
    
    // 禁用执行服务器数据源
    "spring.datasource.execution.enabled=false"
})
public @interface TestConfig {
    /**
     * 测试描述信息
     */
    String description() default "";
    
    /**
     * 是否启用额外配置
     */
    boolean enableAdditionalConfig() default false;
}
