package com.example.medaiassistant.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JpaProperties配置类单元测试
 * 按照TDD红-绿-重构流程实现JPA配置标准化
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-03
 */
@SpringBootTest(classes = JpaProperties.class)
@EnableConfigurationProperties(JpaProperties.class)
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.show-sql=false",
    "spring.jpa.properties.hibernate.format_sql=false",
    "spring.jpa.properties.hibernate.use_sql_comments=false",
    "spring.jpa.database-platform=org.hibernate.dialect.OracleDialect",
    "spring.jpa.open-in-view=false"
})
@DisplayName("JpaProperties配置类 单元测试")
class JpaPropertiesTest {

    @Autowired
    private JpaProperties jpaProperties;

    @Autowired
    private Environment environment;

    @BeforeEach
    void setUp() {
        // 确保每次测试前配置对象已正确初始化
        assertThat(jpaProperties).isNotNull();
    }

    /**
     * 🟢 绿阶段测试：验证配置类正确绑定spring.jpa前缀
     * 测试通过：JpaProperties类已正确实现配置绑定
     */
    @Test
    @DisplayName("应该正确绑定spring.jpa前缀配置")
    void shouldBindJpaPropertiesCorrectly() {
        // 给定：配置属性已设置
        // 当：配置类被Spring容器管理
        // 那么：配置属性应该正确绑定
        assertThat(jpaProperties).isNotNull();
        assertThat(jpaProperties.getHibernate().getDdlAuto()).isEqualTo("none");
        assertThat(jpaProperties.getShowSql()).isFalse();
        assertThat(jpaProperties.getDatabasePlatform()).isEqualTo("org.hibernate.dialect.OracleDialect");
    }

    /**
     * 🟢 绿阶段测试：验证Hibernate配置绑定
     * 测试通过：Hibernate配置属性已正确绑定
     */
    @Test
    @DisplayName("应该正确绑定Hibernate配置")
    void shouldBindHibernatePropertiesCorrectly() {
        // 给定：Hibernate配置属性已设置
        // 当：配置类被Spring容器管理
        // 那么：Hibernate配置属性应该正确绑定
        assertThat(jpaProperties).isNotNull();
        assertThat(jpaProperties.getProperties().getHibernate().getFormatSql()).isFalse();
        assertThat(jpaProperties.getProperties().getHibernate().getUseSqlComments()).isFalse();
    }

    /**
     * 🟢 绿阶段测试：验证配置验证逻辑
     * 测试通过：validateConfiguration方法已正确实现
     */
    @Test
    @DisplayName("应该通过配置验证逻辑")
    void shouldPassConfigurationValidation() {
        // 给定：有效配置
        // 当：执行配置验证
        // 那么：不应该抛出异常
        assertThatCode(() -> jpaProperties.validateConfiguration(environment))
            .doesNotThrowAnyException();
    }

    /**
     * 🟢 绿阶段测试：验证生产环境安全配置
     * 测试通过：生产环境安全验证逻辑已正确实现
     */
    @Test
    @DisplayName("应该验证生产环境安全配置")
    void shouldValidateProductionEnvironmentSecurity() {
        // 给定：生产环境配置
        org.springframework.mock.env.MockEnvironment prodEnvironment = new org.springframework.mock.env.MockEnvironment();
        prodEnvironment.setProperty("spring.profiles.active", "prod");
        prodEnvironment.setProperty("spring.jpa.hibernate.ddl-auto", "none");
        prodEnvironment.setProperty("spring.jpa.show-sql", "false");
        
        // 当：执行生产环境配置验证
        // 那么：应该通过验证
        assertThatCode(() -> jpaProperties.validateProductionSecurity(prodEnvironment))
            .doesNotThrowAnyException();
    }

    /**
     * 🟢 绿阶段测试：验证生产环境不安全配置
     * 测试通过：生产环境安全验证逻辑正确检测到不安全配置
     */
    @Test
    @DisplayName("应该在生产环境不安全配置时抛出异常")
    void shouldThrowExceptionWhenProductionSecurityInvalid() {
        // 给定：生产环境不安全配置
        org.springframework.mock.env.MockEnvironment prodEnvironment = new org.springframework.mock.env.MockEnvironment();
        prodEnvironment.setProperty("spring.profiles.active", "prod");
        prodEnvironment.setProperty("spring.jpa.hibernate.ddl-auto", "create");
        prodEnvironment.setProperty("spring.jpa.show-sql", "true");
        
        // 当：执行生产环境配置验证
        // 那么：应该抛出IllegalStateException
        assertThatThrownBy(() -> jpaProperties.validateProductionSecurity(prodEnvironment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("生产环境");
    }

    /**
     * 🟢 绿阶段测试：验证配置默认值设置
     * 测试通过：默认值设置逻辑已正确实现
     */
    @Test
    @DisplayName("应该设置合理的配置默认值")
    void shouldSetReasonableDefaultValues() {
        // 给定：新创建的JpaProperties实例
        JpaProperties freshProperties = new JpaProperties();
        
        // 当：未设置任何配置值
        // 那么：应该设置合理的默认值
        assertThat(freshProperties.getHibernate()).isNotNull();
        assertThat(freshProperties.getProperties()).isNotNull();
        assertThat(freshProperties.getProperties().getHibernate()).isNotNull();
    }

    /**
     * 🔵 重构阶段测试：验证缺失数据库方言配置时的验证失败
     * 测试通过：配置验证逻辑正确检测到缺失的方言配置
     */
    @Test
    @DisplayName("应该在数据库方言缺失时抛出异常")
    void shouldThrowExceptionWhenDatabasePlatformMissing() {
        // 给定：缺失数据库方言的环境
        org.springframework.mock.env.MockEnvironment invalidEnvironment = new org.springframework.mock.env.MockEnvironment();
        invalidEnvironment.setProperty("spring.jpa.hibernate.ddl-auto", "none");
        
        // 当：执行配置验证
        // 那么：应该抛出IllegalStateException
        assertThatThrownBy(() -> jpaProperties.validateConfiguration(invalidEnvironment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("数据库方言");
    }

    /**
     * 🔵 重构阶段测试：验证方言格式验证
     * 测试通过：配置验证逻辑正确检测到格式错误的方言
     */
    @Test
    @DisplayName("应该在数据库方言格式错误时抛出异常")
    void shouldThrowExceptionWhenDatabasePlatformFormatInvalid() {
        // 给定：格式错误的数据库方言
        org.springframework.mock.env.MockEnvironment invalidEnvironment = new org.springframework.mock.env.MockEnvironment();
        invalidEnvironment.setProperty("spring.jpa.database-platform", "invalid-dialect");
        invalidEnvironment.setProperty("spring.jpa.hibernate.ddl-auto", "none");
        
        // 当：执行配置验证
        // 那么：应该抛出IllegalStateException
        assertThatThrownBy(() -> jpaProperties.validateConfiguration(invalidEnvironment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("方言格式");
    }

    /**
     * 🔵 重构阶段测试：验证缺失DDL策略配置时的验证失败
     * 测试通过：配置验证逻辑正确检测到缺失的DDL策略配置
     */
    @Test
    @DisplayName("应该在DDL策略缺失时抛出异常")
    void shouldThrowExceptionWhenDdlAutoMissing() {
        // 给定：缺失DDL策略的环境
        org.springframework.mock.env.MockEnvironment invalidEnvironment = new org.springframework.mock.env.MockEnvironment();
        invalidEnvironment.setProperty("spring.jpa.database-platform", "org.hibernate.dialect.OracleDialect");
        
        // 当：执行配置验证
        // 那么：应该抛出IllegalStateException
        assertThatThrownBy(() -> jpaProperties.validateConfiguration(invalidEnvironment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("DDL策略");
    }

    /**
     * 🔵 重构阶段测试：验证非生产环境不进行安全验证
     * 测试通过：非生产环境跳过安全验证
     */
    @Test
    @DisplayName("应该在非生产环境跳过安全验证")
    void shouldSkipSecurityValidationInNonProductionEnvironment() {
        // 给定：开发环境配置
        org.springframework.mock.env.MockEnvironment devEnvironment = new org.springframework.mock.env.MockEnvironment();
        devEnvironment.setProperty("spring.profiles.active", "dev");
        devEnvironment.setProperty("spring.jpa.hibernate.ddl-auto", "create");
        devEnvironment.setProperty("spring.jpa.show-sql", "true");
        
        // 当：执行生产环境配置验证
        // 那么：不应该抛出异常（因为不是生产环境）
        assertThatCode(() -> jpaProperties.validateProductionSecurity(devEnvironment))
            .doesNotThrowAnyException();
    }

    /**
     * 🔵 重构阶段测试：验证配置验证性能
     * 测试通过：配置验证在合理时间内完成
     */
    @Test
    @DisplayName("配置验证应该在合理时间内完成")
    void shouldCompleteConfigurationValidationInReasonableTime() {
        // 给定：有效配置环境
        // 当：执行配置验证
        // 那么：应该在1秒内完成
        assertThatCode(() -> {
            long startTime = System.currentTimeMillis();
            jpaProperties.validateConfiguration(environment);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            assertThat(duration).isLessThan(1000); // 1秒内完成
        }).doesNotThrowAnyException();
    }
}
