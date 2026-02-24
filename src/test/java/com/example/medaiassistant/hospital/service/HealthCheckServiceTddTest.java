package com.example.medaiassistant.hospital.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 健康检查服务TDD测试
 * 按照TDD红-绿-重构流程实现任务5.1：健康检查服务
 * 
 * @author Cline
 * @since 2025-12-04
 */
@SpringBootTest(classes = {HospitalConfigService.class, DatabaseConnectionTester.class, HealthCheckService.class})
@TestPropertySource(properties = {
    "hospital.config.dir=src/test/resources/tdd-test-configs",
    "spring.main.web-application-type=none",
    "spring.task.scheduling.enabled=false",
    "scheduling.auto-execute.enabled=false",
    "prompt.submission.enabled=false",
    "prompt.polling.enabled=false",
    "monitoring.metrics.enabled=false",
    "hospital.health.check.enabled=true",
    "hospital.health.check.cron=0 */5 * * * *"
})
@DisplayName("健康检查服务TDD测试 - 任务5.1：健康检查服务")
class HealthCheckServiceTddTest {

    @Autowired
    private HealthCheckService healthCheckService;

    /**
     * 🟢 绿阶段测试1：测试数据库连接检查功能
     * 这个测试现在应该通过，因为HealthCheckService已经实现
     */
    @Test
    @DisplayName("绿阶段：测试数据库连接检查功能 - 应该返回健康状态信息")
    void testDatabaseConnectionCheck_GreenPhase() {
        // 测试数据库连接检查功能
        HealthCheckService.ConnectionHealth health = healthCheckService.checkConnection("test-hospital-id");
        
        // 断言：应该返回健康状态信息
        assertNotNull(health, "健康状态不应该为null");
        assertNotNull(health.getHospitalId(), "医院ID不应该为null");
        assertNotNull(health.getCheckTime(), "检查时间不应该为null");
        assertTrue(health.getResponseTime() >= 0, "响应时间应该大于等于0");
        
        // 注意：由于测试医院配置可能不存在，连接可能会失败
        // 我们主要验证服务能够正确处理并返回结果
        log.info("数据库连接检查测试完成，医院ID: {}, 健康状态: {}", 
                health.getHospitalId(), health.isHealthy());
    }

    /**
     * 🟢 绿阶段测试2：测试服务状态检查功能
     * 这个测试现在应该通过，因为HealthCheckService已经实现
     */
    @Test
    @DisplayName("绿阶段：测试服务状态检查功能 - 应该返回服务状态信息")
    void testServiceStatusCheck_GreenPhase() {
        // 测试服务状态检查功能
        Map<String, Object> status = healthCheckService.checkServiceStatus();
        
        // 断言：应该返回服务状态信息
        assertNotNull(status, "服务状态不应该为null");
        assertTrue(status.containsKey("service"), "服务状态应包含service字段");
        assertTrue(status.containsKey("status"), "服务状态应包含status字段");
        assertTrue(status.containsKey("timestamp"), "服务状态应包含timestamp字段");
        assertTrue(status.containsKey("cacheSize"), "服务状态应包含cacheSize字段");
        
        assertEquals("HealthCheckService", status.get("service"), "服务名称应该匹配");
        assertEquals("UP", status.get("status"), "服务状态应该为UP");
        
        log.info("服务状态检查测试完成，服务状态: {}", status);
    }

    /**
     * 🟢 绿阶段测试3：测试性能指标收集功能
     * 这个测试现在应该通过，因为HealthCheckService已经实现
     */
    @Test
    @DisplayName("绿阶段：测试性能指标收集功能 - 应该返回性能指标信息")
    void testPerformanceMetricsCollection_GreenPhase() {
        // 测试性能指标收集功能
        Map<String, Object> metrics = healthCheckService.collectPerformanceMetrics();
        
        // 断言：应该返回性能指标信息
        assertNotNull(metrics, "性能指标不应该为null");
        assertTrue(metrics.containsKey("timestamp"), "性能指标应包含timestamp字段");
        assertTrue(metrics.containsKey("totalChecks"), "性能指标应包含totalChecks字段");
        assertTrue(metrics.containsKey("successfulChecks"), "性能指标应包含successfulChecks字段");
        assertTrue(metrics.containsKey("failedChecks"), "性能指标应包含failedChecks字段");
        assertTrue(metrics.containsKey("averageResponseTime"), "性能指标应包含averageResponseTime字段");
        assertTrue(metrics.containsKey("successRate"), "性能指标应包含successRate字段");
        
        // 验证数值类型
        assertTrue(((Number) metrics.get("totalChecks")).longValue() >= 0, "总检查次数应该大于等于0");
        assertTrue(((Number) metrics.get("successfulChecks")).longValue() >= 0, "成功检查次数应该大于等于0");
        assertTrue(((Number) metrics.get("failedChecks")).longValue() >= 0, "失败检查次数应该大于等于0");
        
        log.info("性能指标收集测试完成，总检查次数: {}", metrics.get("totalChecks"));
    }

    /**
     * 🟢 绿阶段测试4：测试定时健康检查任务
     * 这个测试现在应该通过，因为HealthCheckService已经实现
     */
    @Test
    @DisplayName("绿阶段：测试定时健康检查任务 - 应该能够执行健康检查")
    void testScheduledHealthCheck_GreenPhase() {
        // 测试定时健康检查任务（手动调用）
        healthCheckService.checkAllConnections();
        
        // 断言：应该能够执行健康检查而不抛出异常
        Map<String, HealthCheckService.ConnectionHealth> allHealth = healthCheckService.getAllHealthStatus();
        assertNotNull(allHealth, "所有健康状态不应该为null");
        
        // 验证缓存状态
        Map<String, Object> status = healthCheckService.checkServiceStatus();
        assertNotNull(status.get("cacheSize"), "缓存大小不应该为null");
        
        log.info("定时健康检查任务测试完成，缓存大小: {}", status.get("cacheSize"));
    }

    /**
     * 🟢 绿阶段测试5：测试健康状态报告功能
     * 这个测试现在应该通过，因为HealthCheckService已经实现
     */
    @Test
    @DisplayName("绿阶段：测试健康状态报告功能 - 应该返回完整的健康状态报告")
    void testHealthStatusReport_GreenPhase() {
        // 测试健康状态报告功能
        Map<String, Object> report = healthCheckService.generateHealthStatusReport();
        
        // 断言：应该返回完整的健康状态报告
        assertNotNull(report, "健康状态报告不应该为null");
        assertTrue(report.containsKey("reportType"), "报告应包含reportType字段");
        assertTrue(report.containsKey("generatedAt"), "报告应包含generatedAt字段");
        assertTrue(report.containsKey("service"), "报告应包含service字段");
        assertTrue(report.containsKey("serviceStatus"), "报告应包含serviceStatus字段");
        assertTrue(report.containsKey("totalHospitals"), "报告应包含totalHospitals字段");
        assertTrue(report.containsKey("healthyCount"), "报告应包含healthyCount字段");
        assertTrue(report.containsKey("unhealthyCount"), "报告应包含unhealthyCount字段");
        assertTrue(report.containsKey("detailedHealth"), "报告应包含detailedHealth字段");
        assertTrue(report.containsKey("performanceMetrics"), "报告应包含performanceMetrics字段");
        assertTrue(report.containsKey("recommendations"), "报告应包含recommendations字段");
        
        assertEquals("HealthStatusReport", report.get("reportType"), "报告类型应该匹配");
        assertEquals("HospitalDataSyncSystem", report.get("service"), "服务名称应该匹配");
        
        log.info("健康状态报告测试完成，报告类型: {}", report.get("reportType"));
    }
    
    // 添加日志记录器
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HealthCheckServiceTddTest.class);
}
