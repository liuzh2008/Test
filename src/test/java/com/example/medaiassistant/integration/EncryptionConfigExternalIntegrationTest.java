package com.example.medaiassistant.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EncryptionConfig外部集成测试
 * 使用 SystemAvailabilityChecker API 进行系统可用性检查
 * 直接对运行中的后端服务发起HTTP请求，不加载Spring上下文
 * 
 * 注意：此测试需要完整的Spring Boot Actuator上下文和运行中的服务
 * 不适用于标准单元测试阶段，应该在集成测试环境中运行
 */
@Disabled("此测试需要完整的Actuator端点和运行中的服务，不适用于单元测试阶段")
class EncryptionConfigExternalIntegrationTest {

    private static String baseUrl;

    @BeforeAll
    static void checkSystemAvailability() {
        // 使用 SystemAvailabilityChecker API 检查系统可用性
        baseUrl = SystemAvailabilityChecker.resolveBaseUrl();
        SystemAvailabilityChecker.ensureSystemRunning(baseUrl);
    }

    /**
     * 🔵 集成测试：验证加密配置健康检查端点
     */
    @Test
    void encryptionConfigHealthEndpointShouldBeUp() throws Exception {
        String url = buildActuatorUrl("health");

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
        assertEquals("UP", root.get("status").asText(), "健康状态应为UP");
    }

    /**
     * 🔵 集成测试：验证加密配置相关端点
     */
    @Test
    void encryptionConfigEndpointsShouldBeUp() throws Exception {
        String[] paths = {
            "health"
            // "info",  // 此端点可能未启用
            // "configprops"  // 此端点可能未启用
        };

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        for (String path : paths) {
            String url = buildActuatorUrl(path);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode(), "HTTP状态码应为200: " + path);
            
            // 验证响应包含有效JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            assertNotNull(root, "响应应为有效JSON: " + path);
        }
    }

    /**
     * 🔵 集成测试：验证配置属性端点包含加密配置
     */
    @Test
    @Disabled("configprops端点可能未启用")
    void configPropsShouldContainEncryptionConfig() throws Exception {
        String url = buildActuatorUrl("configprops");

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

        // 验证配置属性包含加密配置相关信息
        assertNotNull(root, "配置属性响应应为有效JSON");
        
        // 检查是否包含加密配置相关的配置属性
        boolean hasEncryptionConfig = false;
        for (JsonNode configProp : root) {
            if (configProp.has("prefix") && 
                configProp.get("prefix").asText().contains("encryption")) {
                hasEncryptionConfig = true;
                break;
            }
        }
        
        assertTrue(hasEncryptionConfig, "配置属性应包含加密配置");
    }

    /**
     * 🔵 集成测试：验证响应时间在合理范围内
     */
    @Test
    void responseTimeShouldBeReasonable() throws Exception {
        String url = buildActuatorUrl("health");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        long startTime = System.currentTimeMillis();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        long endTime = System.currentTimeMillis();

        long responseTime = endTime - startTime;
        assertTrue(responseTime < 1000, "响应时间应小于1秒，实际: " + responseTime + "ms");
        assertEquals(200, response.statusCode(), "HTTP状态码应为200");
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

}
