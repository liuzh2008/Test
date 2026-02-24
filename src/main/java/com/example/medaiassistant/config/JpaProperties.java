package com.example.medaiassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * JPA配置属性类
 * 绑定spring.jpa前缀的配置属性
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-03
 */
@Component
@ConfigurationProperties(prefix = "spring.jpa")
@Validated
public class JpaProperties {

    private Hibernate hibernate = new Hibernate();
    private Boolean showSql;
    private String databasePlatform;
    private Boolean openInView;
    private Properties properties = new Properties();

    // 最小化实现：只提供必要的getter和setter
    public Hibernate getHibernate() {
        return hibernate;
    }

    public void setHibernate(Hibernate hibernate) {
        this.hibernate = hibernate;
    }

    public Boolean getShowSql() {
        return showSql;
    }

    public void setShowSql(Boolean showSql) {
        this.showSql = showSql;
    }

    public String getDatabasePlatform() {
        return databasePlatform;
    }

    public void setDatabasePlatform(String databasePlatform) {
        this.databasePlatform = databasePlatform;
    }

    public Boolean getOpenInView() {
        return openInView;
    }

    public void setOpenInView(Boolean openInView) {
        this.openInView = openInView;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * Hibernate配置内部类
     */
    public static class Hibernate {
        private String ddlAuto;

        public String getDdlAuto() {
            return ddlAuto;
        }

        public void setDdlAuto(String ddlAuto) {
            this.ddlAuto = ddlAuto;
        }
    }

    /**
     * Properties配置内部类
     */
    public static class Properties {
        private HibernateProperties hibernate = new HibernateProperties();

        public HibernateProperties getHibernate() {
            return hibernate;
        }

        public void setHibernate(HibernateProperties hibernate) {
            this.hibernate = hibernate;
        }
    }

    /**
     * Hibernate属性配置内部类
     */
    public static class HibernateProperties {
        private Boolean formatSql;
        private Boolean useSqlComments;

        public Boolean getFormatSql() {
            return formatSql;
        }

        public void setFormatSql(Boolean formatSql) {
            this.formatSql = formatSql;
        }

        public Boolean getUseSqlComments() {
            return useSqlComments;
        }

        public void setUseSqlComments(Boolean useSqlComments) {
            this.useSqlComments = useSqlComments;
        }
    }

    /**
     * 配置验证方法
     * 验证JPA配置的完整性和正确性
     */
    public void validateConfiguration(Environment environment) {
        // 验证数据库方言
        String databasePlatform = environment.getProperty("spring.jpa.database-platform");
        if (databasePlatform == null || databasePlatform.trim().isEmpty()) {
            throw new IllegalStateException("JPA数据库方言配置缺失");
        }
        
        // 验证方言格式
        if (!databasePlatform.contains("Dialect")) {
            throw new IllegalStateException("JPA数据库方言格式错误，必须是有效的Hibernate方言");
        }
        
        // 验证DDL策略
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
        if (ddlAuto == null || ddlAuto.trim().isEmpty()) {
            throw new IllegalStateException("JPA DDL策略配置缺失");
        }
    }

    /**
     * 生产环境安全配置验证方法
     * 验证生产环境下的安全配置要求
     */
    public void validateProductionSecurity(Environment environment) {
        String activeProfile = environment.getProperty("spring.profiles.active");
        
        // 只有在生产环境才进行安全验证
        if ("prod".equals(activeProfile)) {
            // 验证DDL策略必须为none
            String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
            if (!"none".equals(ddlAuto)) {
                throw new IllegalStateException("生产环境JPA DDL策略必须设置为none，当前为: " + ddlAuto);
            }
            
            // 验证SQL显示必须为false
            String showSql = environment.getProperty("spring.jpa.show-sql");
            if ("true".equals(showSql)) {
                throw new IllegalStateException("生产环境JPA SQL显示必须设置为false，当前为: " + showSql);
            }
        }
    }
}
