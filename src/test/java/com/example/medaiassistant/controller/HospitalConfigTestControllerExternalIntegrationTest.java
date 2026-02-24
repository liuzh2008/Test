package com.example.medaiassistant.controller;

import com.example.medaiassistant.integration.SystemAvailabilityChecker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
 * 自动化测试所有医院配置API接口
 * 直接对运行中的后端服务发起HTTP请求，不加载Spring上下文
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-03
 */
class HospitalConfigTestControllerExternalIntegrationTest {
    
    private static final Logger log = LoggerFactory.getLogger(HospitalConfigTestControllerExternalIntegrationTest.class);
    private static String baseUrl;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeAll
    static void checkSystemAvailability() {
        // 使用 SystemAvailabilityChecker API 检查系统可用性
        baseUrl = SystemAvailabilityChecker.resolveBaseUrl();
        SystemAvailabilityChecker.ensureSystemRunning(baseUrl);
        log.info("测试基础URL: {}", baseUrl);
    }
    
    /**
     * 构建API端点URL
     */
    private static String buildApiUrl(String path) {
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        // 确保路径以 api/ 开头
        if (!cleanPath.startsWith("api/")) {
            cleanPath = "api/" + cleanPath;
        }
        return baseUrl.endsWith("/") ? baseUrl + cleanPath : baseUrl + "/" + cleanPath;
    }
    
    /**
     * 发送HTTP GET请求
     */
    private static JsonNode sendGetRequest(String path) throws Exception {
        String url = buildApiUrl(path);
        
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode(), "HTTP状态码应为200: " + path);
        
        return objectMapper.readTree(response.body());
    }
    
    /**
     * 发送HTTP POST请求
     */
    private static JsonNode sendPostRequest(String path, String body) throws Exception {
        String url = buildApiUrl(path);
        
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode(), "HTTP状态码应为200: " + path);
        
        return objectMapper.readTree(response.body());
    }
    
    /**
     * 测试1: 健康检查端点
     */
    @Test
    void testHealthEndpoint() throws Exception {
        log.info("测试健康检查端点");
        
        JsonNode response = sendGetRequest("hospital-config/health");
        
        assertEquals("UP", response.get("status").asText(), "状态应为UP");
        assertEquals("Hospital Configuration Test Service", response.get("service").asText(), "服务名称应正确");
        assertTrue(response.has("configCount"), "应包含configCount字段");
        assertTrue(response.has("enabledConfigCount"), "应包含enabledConfigCount字段");
        
        log.info("健康检查测试通过");
    }
    
    /**
     * 测试2: 获取所有医院配置列表
     */
    @Test
    void testListConfigs() throws Exception {
        log.info("测试获取所有医院配置列表");
        
        JsonNode response = sendGetRequest("hospital-config/list");
        
        assertTrue(response.has("configCount"), "应包含configCount字段");
        assertTrue(response.has("configs"), "应包含configs字段");
        assertTrue(response.has("configDir"), "应包含configDir字段");
        
        int configCount = response.get("configCount").asInt();
        JsonNode configs = response.get("configs");
        
        // 注意：在测试环境中，医院配置可能为空，这是正常情况
        // 如果配置为空，只需要验证响应结构正确即可
        if (configCount == 0) {
            log.warn("测试环境中没有医院配置文件，这是正常情况");
            assertEquals(0, configs.size(), "配置数量应该匹配");
        } else {
            assertTrue(configCount >= 1, "至少应该有一个医院配置");
            assertEquals(configCount, configs.size(), "配置数量应该匹配");
            
            // 验证至少包含hospital-001配置
            boolean hasHospital001 = false;
            for (JsonNode config : configs) {
                if ("hospital-001".equals(config.get("id").asText())) {
                    hasHospital001 = true;
                    break;
                }
            }
            assertTrue(hasHospital001, "应该包含hospital-001配置");
        }
        
        log.info("配置列表测试通过，共 {} 个配置", configCount);
    }
    
    /**
     * 测试3: 获取特定医院配置详情
     */
    @Test
    void testGetConfig() throws Exception {
        log.info("测试获取特定医院配置详情");
        
        JsonNode response = sendGetRequest("hospital-config/hospital-001");
        
        assertEquals("hospital-001", response.get("hospitalId").asText(), "医院ID应正确");
        assertTrue(response.has("config"), "应包含config字段");
        
        JsonNode config = response.get("config");
        assertEquals("hospital-001", config.get("id").asText(), "配置ID应正确");
        assertEquals("测试医院", config.get("name").asText(), "医院名称应正确");
        assertEquals("database", config.get("integrationType").asText(), "集成类型应正确");
        
        log.info("获取配置详情测试通过");
    }
    
    /**
     * 测试4: 验证医院配置
     */
    @Test
    void testValidateConfig() throws Exception {
        log.info("测试验证医院配置");
        
        JsonNode response = sendGetRequest("hospital-config/hospital-001/validate");
        
        assertEquals("hospital-001", response.get("hospitalId").asText(), "医院ID应正确");
        assertEquals("测试医院", response.get("configName").asText(), "配置名称应正确");
        assertTrue(response.get("isValid").asBoolean(), "配置应该有效");
        assertTrue(response.get("errors").isArray(), "errors应为数组");
        assertEquals(0, response.get("errors").size(), "错误列表应该为空");
        
        log.info("配置验证测试通过，配置有效");
    }
    
    /**
     * 测试5: 测试HIS数据库连接
     */
    @Test
    void testHisDatabaseConnection() throws Exception {
        log.info("测试HIS数据库连接");
        
        JsonNode response = sendGetRequest("hospital-config/hospital-001/test-connection?databaseType=his");
        
        assertEquals("hospital-001", response.get("hospitalId").asText(), "医院ID应正确");
        assertEquals("his", response.get("databaseType").asText(), "数据库类型应正确");
        assertTrue(response.has("connectionSuccessful"), "应包含connectionSuccessful字段");
        assertTrue(response.has("connectionDetails"), "应包含connectionDetails字段");
        assertTrue(response.has("testTime"), "应包含testTime字段");
        assertTrue(response.has("timestamp"), "应包含timestamp字段");
        
        boolean connectionSuccessful = response.get("connectionSuccessful").asBoolean();
        log.info("HIS数据库连接测试结果: {}", connectionSuccessful ? "成功" : "失败");
    }
    
    /**
     * 测试6: 测试LIS数据库连接
     */
    @Test
    void testLisDatabaseConnection() throws Exception {
        log.info("测试LIS数据库连接");
        
        JsonNode response = sendGetRequest("hospital-config/hospital-001/test-connection?databaseType=lis");
        
        assertEquals("hospital-001", response.get("hospitalId").asText(), "医院ID应正确");
        assertEquals("lis", response.get("databaseType").asText(), "数据库类型应正确");
        assertTrue(response.has("connectionSuccessful"), "应包含connectionSuccessful字段");
        assertTrue(response.has("connectionDetails"), "应包含connectionDetails字段");
        assertTrue(response.has("testTime"), "应包含testTime字段");
        assertTrue(response.has("timestamp"), "应包含timestamp字段");
        
        boolean connectionSuccessful = response.get("connectionSuccessful").asBoolean();
        log.info("LIS数据库连接测试结果: {}", connectionSuccessful ? "成功" : "失败");
    }
    
    /**
     * 测试7: 获取启用的医院配置
     */
    @Test
    void testGetEnabledConfigs() throws Exception {
        log.info("测试获取启用的医院配置");
        
        JsonNode response = sendGetRequest("hospital-config/enabled");
        
        assertTrue(response.has("enabledCount"), "应包含enabledCount字段");
        assertTrue(response.has("enabledAndSyncCount"), "应包含enabledAndSyncCount字段");
        assertTrue(response.has("enabledConfigs"), "应包含enabledConfigs字段");
        assertTrue(response.has("enabledAndSyncConfigs"), "应包含enabledAndSyncConfigs字段");
        
        int enabledCount = response.get("enabledCount").asInt();
        int enabledAndSyncCount = response.get("enabledAndSyncCount").asInt();
        
        assertTrue(enabledCount >= 0, "启用配置数量应该大于等于0");
        assertTrue(enabledAndSyncCount >= 0, "启用且同步配置数量应该大于等于0");
        assertTrue(enabledCount >= enabledAndSyncCount, "启用配置数量应该大于等于启用且同步配置数量");
        
        log.info("启用配置测试通过: 启用 {} 个，启用且同步 {} 个", enabledCount, enabledAndSyncCount);
    }
    
    /**
     * 测试8: 重新加载所有配置
     */
    @Test
    void testReloadConfigs() throws Exception {
        log.info("测试重新加载所有配置");
        
        JsonNode response = sendPostRequest("hospital-config/reload", "{}");
        
        assertTrue(response.get("success").asBoolean(), "重新加载应该成功");
        assertEquals("医院配置重新加载成功", response.get("message").asText(), "消息应正确");
        assertTrue(response.has("beforeCount"), "应包含beforeCount字段");
        assertTrue(response.has("afterCount"), "应包含afterCount字段");
        assertTrue(response.has("reloadTime"), "应包含reloadTime字段");
        
        int beforeCount = response.get("beforeCount").asInt();
        int afterCount = response.get("afterCount").asInt();
        
        assertEquals(beforeCount, afterCount, "重新加载前后配置数量应该相同");
        
        log.info("重新加载配置测试通过: {} -> {}", beforeCount, afterCount);
    }
    
    /**
     * 测试9: 测试不存在的医院配置
     */
    @Test
    void testNonExistentConfig() throws Exception {
        log.info("测试不存在的医院配置");
        
        String url = buildApiUrl("hospital-config/nonexistent");
        
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(404, response.statusCode(), "HTTP状态码应为404");
        
        JsonNode errorResponse = objectMapper.readTree(response.body());
        assertEquals("未找到医院配置: nonexistent", errorResponse.get("error").asText(), "错误信息应正确");
        assertTrue(errorResponse.has("timestamp"), "应包含timestamp字段");
        
        log.info("不存在的配置测试通过，正确返回404错误");
    }
    
    /**
     * 测试10: 测试无效的数据库类型
     */
    @Test
    void testInvalidDatabaseType() throws Exception {
        log.info("测试无效的数据库类型");
        
        String url = buildApiUrl("hospital-config/hospital-001/test-connection?databaseType=invalid");
        
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(400, response.statusCode(), "HTTP状态码应为400");
        
        JsonNode errorResponse = objectMapper.readTree(response.body());
        String errorMessage = errorResponse.get("error").asText();
        assertTrue(errorMessage.contains("不支持的数据库类型"), 
                "错误信息应该包含'不支持的数据库类型'，实际错误: " + errorMessage);
        assertTrue(errorResponse.has("timestamp"), "应包含timestamp字段");
        
        log.info("无效数据库类型测试通过，正确返回400错误");
    }
    
    /**
     * 测试11: 测试hospital-real配置（如果存在）
     */
    @Test
    void testHospitalRealConfig() throws Exception {
        log.info("测试hospital-real配置");
        
        // 首先检查hospital-real配置是否存在
        JsonNode listResponse = sendGetRequest("hospital-config/list");
        JsonNode configs = listResponse.get("configs");
        
        boolean hasHospitalReal = false;
        for (JsonNode config : configs) {
            if ("hospital-real".equals(config.get("id").asText())) {
                hasHospitalReal = true;
                break;
            }
        }
        
        if (hasHospitalReal) {
            log.info("hospital-real配置存在，进行详细测试");
            
            // 测试获取配置详情
            JsonNode getResponse = sendGetRequest("hospital-config/hospital-real");
            assertEquals("hospital-real", getResponse.get("hospitalId").asText(), "医院ID应正确");
            
            JsonNode config = getResponse.get("config");
            assertEquals("实际医院", config.get("name").asText(), "医院名称应正确");
            
            // 测试验证配置
            JsonNode validateResponse = sendGetRequest("hospital-config/hospital-real/validate");
            assertTrue(validateResponse.get("isValid").asBoolean(), "hospital-real配置应该有效");
            
            log.info("hospital-real配置测试通过");
        } else {
            log.warn("hospital-real配置不存在，跳过详细测试");
        }
    }
    
    /**
     * 测试12: 批量测试所有接口
     */
    @Test
    void testAllEndpointsComprehensive() throws Exception {
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
}
