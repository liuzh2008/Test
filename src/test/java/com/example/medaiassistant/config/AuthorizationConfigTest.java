package com.example.medaiassistant.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 授权配置测试类
 * 按照TDD红-绿-重构流程实现故事3：授权配置管理
 * 
 * ✅ P2修订：已限定classes和禁用无关组件
 * 
 * @author System
 * @version 1.1
 * @since 2025-11-07
 */
@SpringBootTest(classes = AuthorizationConfig.class)
@TestPropertySource(properties = {
    "security.authorization.enabled=true",
    "security.authorization.role-based-access-control.enabled=true",
    "security.authorization.permission-based-access-control.enabled=false",
    "security.authorization.default-role=USER",
    "security.authorization.admin-role=ADMIN",
    "security.authorization.audit-enabled=true",
    "security.authorization.permission-validation-enabled=true",
    // 禁用无关组件
    "spring.main.web-application-type=none",
    "spring.task.scheduling.enabled=false",
    "scheduling.auto-execute.enabled=false",
    "monitoring.metrics.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.properties.hibernate.hbm2ddl.auto=none"
})
class AuthorizationConfigTest {

    @Autowired
    private AuthorizationConfig authorizationConfig;

    /**
     * 绿阶段测试用例1：验证AuthorizationConfig类存在
     * 这个测试现在应该通过，因为AuthorizationConfig类已存在
     */
    @Test
    void authorizationConfigShouldBeAutowired() {
        // 这个测试现在应该通过，因为AuthorizationConfig类已存在
        assertNotNull(authorizationConfig, "AuthorizationConfig应该被自动装配");
    }

    /**
     * 绿阶段测试用例2：验证授权配置参数绑定
     * 这个测试现在应该通过，因为AuthorizationConfig类已存在
     */
    @Test
    void authorizationConfigShouldBindProperties() {
        // 这个测试现在应该通过，因为AuthorizationConfig类已存在
        assertTrue(authorizationConfig.isEnabled(), "授权配置应该启用");
        assertTrue(authorizationConfig.isRoleBasedAccessControlEnabled(), "基于角色的访问控制应该启用");
        assertFalse(authorizationConfig.isPermissionBasedAccessControlEnabled(), "基于权限的访问控制应该禁用");
        assertEquals("USER", authorizationConfig.getDefaultRole(), "默认角色应该是USER");
        assertEquals("ADMIN", authorizationConfig.getAdminRole(), "管理员角色应该是ADMIN");
        assertTrue(authorizationConfig.isAuditEnabled(), "审计功能应该启用");
        assertTrue(authorizationConfig.isPermissionValidationEnabled(), "权限验证应该启用");
    }

    /**
     * 绿阶段测试用例3：验证配置验证功能
     * 这个测试现在应该通过，因为AuthorizationConfig类已存在
     */
    @Test
    void authorizationConfigShouldValidateConfiguration() {
        // 这个测试现在应该通过，因为AuthorizationConfig类已存在
        assertDoesNotThrow(() -> authorizationConfig.validateConfiguration(),
            "授权配置验证应该通过");
    }

    /**
     * 绿阶段测试用例4：验证环境变量映射
     * 这个测试现在应该通过，因为AuthorizationConfig类已存在
     */
    @Test
    void authorizationConfigShouldMapEnvironmentVariables() {
        // 这个测试现在应该通过，因为AuthorizationConfig类已存在
        assertEquals("USER", authorizationConfig.getDefaultRole(),
            "默认角色应该正确映射环境变量");
    }

    /**
     * 绿阶段测试用例5：验证角色权限配置
     * 这个测试现在应该通过，因为AuthorizationConfig类已存在
     */
    @Test
    void authorizationConfigShouldSupportRolePermissions() {
        // 这个测试现在应该通过，因为AuthorizationConfig类已存在
        assertNotNull(authorizationConfig.getRolePermissions(), "角色权限配置应该存在");
        assertFalse(authorizationConfig.getRolePermissions().isEmpty(), "角色权限配置不应该为空");
    }

    /**
     * 绿阶段测试用例6：验证访问控制规则
     * 这个测试现在应该通过，因为AuthorizationConfig类已存在
     */
    @Test
    void authorizationConfigShouldSupportAccessControlRules() {
        // 这个测试现在应该通过，因为AuthorizationConfig类已存在
        assertNotNull(authorizationConfig.getAccessControlRules(), "访问控制规则应该存在");
        assertFalse(authorizationConfig.getAccessControlRules().isEmpty(), "访问控制规则不应该为空");
    }

    /**
     * 绿阶段测试用例7：验证权限验证功能
     * 这个测试现在应该通过，因为AuthorizationConfig类已存在
     */
    @Test
    void authorizationConfigShouldSupportPermissionValidation() {
        // 这个测试现在应该通过，因为AuthorizationConfig类已存在
        assertTrue(authorizationConfig.isPermissionValidationEnabled(), "权限验证功能应该启用");
    }

    /**
     * 绿阶段测试用例8：验证权限审计功能
     * 这个测试现在应该通过，因为AuthorizationConfig类已存在
     */
    @Test
    void authorizationConfigShouldSupportPermissionAudit() {
        // 这个测试现在应该通过，因为AuthorizationConfig类已存在
        assertTrue(authorizationConfig.isAuditEnabled(), "权限审计功能应该启用");
    }

    /**
     * 边界条件测试用例1：验证角色权限配置不为空
     */
    @Test
    void rolePermissionsShouldNotBeEmpty() {
        Map<String, List<String>> rolePermissions = authorizationConfig.getRolePermissions();
        assertNotNull(rolePermissions, "角色权限配置不能为null");
        assertFalse(rolePermissions.isEmpty(), "角色权限配置不能为空");
        assertTrue(rolePermissions.containsKey("USER"), "应该包含USER角色");
        assertTrue(rolePermissions.containsKey("ADMIN"), "应该包含ADMIN角色");
        assertTrue(rolePermissions.containsKey("VIEWER"), "应该包含VIEWER角色");
    }

    /**
     * 边界条件测试用例2：验证访问控制规则不为空
     */
    @Test
    void accessControlRulesShouldNotBeEmpty() {
        List<String> accessControlRules = authorizationConfig.getAccessControlRules();
        assertNotNull(accessControlRules, "访问控制规则不能为null");
        assertFalse(accessControlRules.isEmpty(), "访问控制规则不能为空");
        assertTrue(accessControlRules.size() >= 5, "应该包含至少5条访问控制规则");
    }

    /**
     * 边界条件测试用例3：验证默认角色和管理员角色不同
     */
    @Test
    void defaultRoleAndAdminRoleShouldBeDifferent() {
        String defaultRole = authorizationConfig.getDefaultRole();
        String adminRole = authorizationConfig.getAdminRole();
        assertNotNull(defaultRole, "默认角色不能为null");
        assertNotNull(adminRole, "管理员角色不能为null");
        assertNotEquals(defaultRole, adminRole, "默认角色和管理员角色应该不同");
    }

    /**
     * 异常情况测试用例：验证配置验证在禁用时抛出异常
     */
    @Test
    void shouldThrowExceptionWhenAuthorizationDisabled() {
        // 创建一个新的配置实例来测试异常情况
        AuthorizationConfig disabledConfig = new AuthorizationConfig(authorizationConfig.getEnvironment());
        disabledConfig.setEnabled(false);
        
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            () -> disabledConfig.validateConfiguration());
        assertEquals("授权配置已禁用", exception.getMessage());
    }

    /**
     * 性能基准测试：验证配置加载时间在合理范围内
     */
    @Test
    void configurationLoadingShouldBeFast() {
        long startTime = System.currentTimeMillis();
        
        // 执行配置验证
        authorizationConfig.validateConfiguration();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue(duration < 100, "配置验证应该在100毫秒内完成，实际耗时: " + duration + "毫秒");
    }
}
