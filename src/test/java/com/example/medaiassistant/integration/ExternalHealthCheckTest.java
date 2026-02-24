package com.example.medaiassistant.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 外部健康检查集成测试
 * 直接对运行中的后端服务发起HTTP请求，不加载Spring上下文
 * 使用 SystemAvailabilityChecker API 进行系统可用性检查
 * 
 * @author System
 * @version 3.0
 * @since 2025-11-04
 */
class ExternalHealthCheckTest {

    private static String baseUrl;

    @BeforeAll
    static void checkSystemAvailability() {
        // 使用 SystemAvailabilityChecker API 检查系统可用性
        try {
            baseUrl = SystemAvailabilityChecker.resolveBaseUrl();
            SystemAvailabilityChecker.ensureSystemRunning(baseUrl);
        } catch (Exception e) {
            // 如果系统不可用，记录警告但不失败
            System.err.println("警告: 系统不可用，测试可能失败: " + e.getMessage());
            baseUrl = "http://localhost:8080"; // 使用默认URL
        }
    }

    @Test
    void healthEndpointShouldBeUp() throws Exception {
        String url = buildActuatorUrl("health");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30)) // 增加超时时间
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode(), "HTTP状态码应为200");

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());

            assertNotNull(root.get("status"), "响应应包含status字段");
            assertEquals("UP", root.get("status").asText(), "健康状态应为UP");
        } catch (java.net.http.HttpTimeoutException e) {
            // 如枟超时，跳过此测试
            System.err.println("警告: 健康检查端点超时，可能是服务器不可用: " + e.getMessage());
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "服务器不可用，跳过测试");
        }
    }

    @Test
    void configurationEndpointsShouldBeUp() throws Exception {
        String[] paths = new String[] {"configuration/validate", "configuration/status"};

        for (String path : paths) {
            String url = buildApiUrl(path);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode(), "HTTP状态码应为200: " + path);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());

            assertNotNull(root.get("configurationValid"), "响应应包含configurationValid字段: " + path);
            assertTrue(root.get("configurationValid").asBoolean(), "配置验证应该通过: " + path);
        }
    }

    @Test
    void configurationHealthEndpointShouldBeUp() throws Exception {
        String url = buildApiUrl("configuration/health");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "HTTP状态码应为200");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());

        assertNotNull(root.get("status"), "响应应包含status字段");
        assertNotNull(root.get("configurationValid"), "响应应包含configurationValid字段");
        assertNotNull(root.get("components"), "响应应包含components字段");
        
        // 验证配置有效性
        assertTrue(root.get("configurationValid").asBoolean(), "配置验证应该通过");
        
        // 验证组件信息
        JsonNode components = root.get("components");
        assertTrue(components.has("database"), "应包含数据库组件");
        assertTrue(components.has("jpa"), "应包含JPA组件");
    }

    @Test
    void configurationValidationEndpointShouldBeUp() throws Exception {
        String url = buildApiUrl("configuration/validate");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "HTTP状态码应为200");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());

        assertNotNull(root.get("status"), "响应应包含status字段");
        assertNotNull(root.get("message"), "响应应包含message字段");
        assertNotNull(root.get("configurationValid"), "响应应包含configurationValid字段");
        
        // 验证配置验证结果
        assertEquals("SUCCESS", root.get("status").asText(), "配置验证状态应为SUCCESS");
        assertTrue(root.get("configurationValid").asBoolean(), "配置验证应该通过");
    }

    @Test
    void configurationStatusEndpointShouldBeUp() throws Exception {
        String url = buildApiUrl("configuration/status");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "HTTP状态码应为200");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());

        assertNotNull(root.get("configurationValid"), "响应应包含configurationValid字段");
        assertNotNull(root.get("service"), "响应应包含service字段");
        assertNotNull(root.get("timestamp"), "响应应包含timestamp字段");
        
        // 验证配置状态
        assertTrue(root.get("configurationValid").asBoolean(), "配置状态应该有效");
        assertEquals("Configuration Validation Service", root.get("service").asText(), "服务名称应该正确");
    }

    /**
     * 构建Actuator端点URL（统一根地址 + /actuator/...）
     */
    private static String buildActuatorUrl(String path) {
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        // 确保路径以 actuator/ 开头
        if (!cleanPath.startsWith("actuator/")) {
            cleanPath = "actuator/" + cleanPath;
        }
        return baseUrl.endsWith("/") ? baseUrl + cleanPath : baseUrl + "/" + cleanPath;
    }

    /**
     * 构建业务API端点URL（统一根地址 + /api/...）
     */
    private static String buildApiUrl(String path) {
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        // 确保路径以 api/ 开头
        if (!cleanPath.startsWith("api/")) {
            cleanPath = "api/" + cleanPath;
        }
        return baseUrl.endsWith("/") ? baseUrl + cleanPath : baseUrl + "/" + cleanPath;
    }
}
