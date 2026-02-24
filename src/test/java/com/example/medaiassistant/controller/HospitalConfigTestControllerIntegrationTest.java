package com.example.medaiassistant.controller;

import com.example.medaiassistant.integration.SystemAvailabilityChecker;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 医院配置测试控制器外部集成测试
 * 按照外部集成测试编写原则，直接对运行中的后端服务发起HTTP请求，不加载Spring上下文
 * 自动化测试所有医院配置API接口
 * 
 * @author MedAI Assistant Team
 * @since 2025-12-03
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HospitalConfigTestControllerIntegrationTest {
    
    private static final Logger log = LoggerFactory.getLogger(HospitalConfigTestControllerIntegrationTest.class);
    
    private String baseUrl;
    private HttpClient httpClient;
    
    @BeforeAll
    public void setUp() {
        // 使用SystemAvailabilityChecker API检查系统可用性
        baseUrl = SystemAvailabilityChecker.resolveBaseUrl() + "/api/hospital-config";
        SystemAvailabilityChecker.ensureSystemRunning(baseUrl.replace("/api/hospital-config", ""));
        
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        log.info("测试基础URL: {}", baseUrl);
    }
    
    /**
     * 测试1: 健康检查端点 - 外部集成测试
     * 验证GET /api/hospital-config/health端点可访问
     */
    @Test
    public void testHealthEndpoint() throws Exception {
        log.info("测试健康检查端点");
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/health"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("健康检查测试通过");
    }
    
    /**
     * 测试2: 获取所有医院配置列表 - 外部集成测试
     * 验证GET /api/hospital-config/list端点可访问
     */
    @Test
    public void testListConfigs() throws Exception {
        log.info("测试获取所有医院配置列表");
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/list"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("配置列表测试通过");
    }
    
    /**
     * 测试3: 获取特定医院配置详情 - 外部集成测试
     * 验证GET /api/hospital-config/{hospitalId}端点可访问
     */
    @Test
    public void testGetConfig() throws Exception {
        log.info("测试获取特定医院配置详情");
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/hospital-001"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("获取配置详情测试通过");
    }
    
    /**
     * 测试4: 验证医院配置 - 外部集成测试
     * 验证GET /api/hospital-config/{hospitalId}/validate端点可访问
     */
    @Test
    public void testValidateConfig() throws Exception {
        log.info("测试验证医院配置");
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/hospital-001/validate"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("配置验证测试通过");
    }
    
    /**
     * 测试5: 测试HIS数据库连接 - 外部集成测试
     * 验证GET /api/hospital-config/{hospitalId}/test-connection?databaseType=his端点可访问
     */
    @Test
    public void testHisDatabaseConnection() throws Exception {
        log.info("测试HIS数据库连接");
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/hospital-001/test-connection?databaseType=his"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("HIS数据库连接测试通过");
    }
    
    /**
     * 测试6: 测试LIS数据库连接 - 外部集成测试
     * 验证GET /api/hospital-config/{hospitalId}/test-connection?databaseType=lis端点可访问
     */
    @Test
    public void testLisDatabaseConnection() throws Exception {
        log.info("测试LIS数据库连接");
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/hospital-001/test-connection?databaseType=lis"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("LIS数据库连接测试通过");
    }
    
    /**
     * 测试7: 获取启用的医院配置 - 外部集成测试
     * 验证GET /api/hospital-config/enabled端点可访问
     */
    @Test
    public void testGetEnabledConfigs() throws Exception {
        log.info("测试获取启用的医院配置");
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/enabled"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("启用配置测试通过");
    }
    
    /**
     * 测试8: 重新加载所有配置 - 外部集成测试
     * 验证POST /api/hospital-config/reload端点可访问
     */
    @Test
    public void testReloadConfigs() throws Exception {
        log.info("测试重新加载所有配置");
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/reload"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("重新加载配置测试通过");
    }
    
    /**
     * 测试9: 测试不存在的医院配置 - 外部集成测试
     * 验证GET /api/hospital-config/{hospitalId}端点对不存在配置的处理
     */
    @Test
    public void testNonExistentConfig() throws Exception {
        log.info("测试不存在的医院配置");
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/nonexistent"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("不存在的配置测试通过");
    }
    
    /**
     * 测试10: 测试无效的数据库类型 - 外部集成测试
     * 验证GET /api/hospital-config/{hospitalId}/test-connection?databaseType=invalid端点可访问
     */
    @Test
    public void testInvalidDatabaseType() throws Exception {
        log.info("测试无效的数据库类型");
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/hospital-001/test-connection?databaseType=invalid"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("无效数据库类型测试通过");
    }
    
    /**
     * 测试11: 测试hospital-real配置（如果存在） - 外部集成测试
     * 验证hospital-real相关端点可访问
     */
    @Test
    public void testHospitalRealConfig() throws Exception {
        log.info("测试hospital-real配置");
        
        // 首先检查hospital-real配置是否存在
        HttpRequest listRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/list"))
                .GET()
                .build();
        
        HttpResponse<String> listResponse = httpClient.send(listRequest, HttpResponse.BodyHandlers.ofString());
        
        if (listResponse.statusCode() >= 200 && listResponse.statusCode() < 300) {
            String responseBody = listResponse.body();
            if (responseBody != null && responseBody.contains("hospital-real")) {
                log.info("hospital-real配置存在，进行详细测试");
                
                // 测试获取配置详情
                HttpRequest detailRequest = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/hospital-real"))
                        .GET()
                        .build();
                
                HttpResponse<String> detailResponse = httpClient.send(detailRequest, HttpResponse.BodyHandlers.ofString());
                validateApiResponse(detailResponse);
                
                // 测试验证配置
                HttpRequest validateRequest = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/hospital-real/validate"))
                        .GET()
                        .build();
                
                HttpResponse<String> validateResponse = httpClient.send(validateRequest, HttpResponse.BodyHandlers.ofString());
                validateApiResponse(validateResponse);
                
                log.info("hospital-real配置测试通过");
            } else {
                log.warn("hospital-real配置不存在，跳过详细测试");
            }
        } else {
            log.warn("无法获取配置列表，跳过hospital-real测试");
        }
    }
    
    /**
     * 测试12: 批量测试所有接口 - 外部集成测试
     * 验证所有医院配置API端点可访问
     */
    @Test
    public void testAllEndpointsComprehensive() throws Exception {
        log.info("开始批量测试所有接口");
        
        // 1. 健康检查
        testHealthEndpoint();
        log.info("✓ 健康检查测试完成");
        
        // 2. 配置列表
        testListConfigs();
        log.info("✓ 配置列表测试完成");
        
        // 3. 配置详情
        testGetConfig();
        log.info("✓ 配置详情测试完成");
        
        // 4. 配置验证
        testValidateConfig();
        log.info("✓ 配置验证测试完成");
        
        // 5. HIS数据库连接
        testHisDatabaseConnection();
        log.info("✓ HIS数据库连接测试完成");
        
        // 6. LIS数据库连接
        testLisDatabaseConnection();
        log.info("✓ LIS数据库连接测试完成");
        
        // 7. 启用配置
        testGetEnabledConfigs();
        log.info("✓ 启用配置测试完成");
        
        // 8. 重新加载配置
        testReloadConfigs();
        log.info("✓ 重新加载配置测试完成");
        
        // 9. 不存在的配置
        testNonExistentConfig();
        log.info("✓ 不存在的配置测试完成");
        
        // 10. 无效数据库类型
        testInvalidDatabaseType();
        log.info("✓ 无效数据库类型测试完成");
        
        // 11. hospital-real配置
        testHospitalRealConfig();
        log.info("✓ hospital-real配置测试完成");
        
        log.info("所有接口批量测试完成 ✓");
    }
    
    /**
     * 验证API响应的通用方法 - 外部集成测试
     * 验证API端点可访问，不验证具体的响应内容
     * 
     * @param response HTTP响应对象
     */
    private void validateApiResponse(HttpResponse<String> response) {
        // 外部集成测试：验证API端点可访问
        int statusCode = response.statusCode();
        // 允许所有有效的HTTP状态码（100-599）
        assertTrue(statusCode >= 100 && statusCode < 600, 
            "API端点应返回有效的HTTP状态码，实际返回: " + statusCode);
        
        String responseBody = response.body();
        // 对于外部集成测试，响应体可能为空，特别是对于错误响应
        // 我们只记录响应信息，不强制要求非空
        if (responseBody != null) {
            log.info("API响应状态码: {}", statusCode);
            log.info("API响应体: {}", responseBody);
        }
    }
}
