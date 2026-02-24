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
 * 执行服务器ENCRYPTED_DATA_TEMP表API外部集成测试
 * 使用SystemAvailabilityChecker API进行系统可用性检查
 * 直接对运行中的后端服务发起HTTP请求，不加载Spring上下文
 */
class ExecutionServerEncryptedDataApiTest {

    private static String baseUrl;
    private static HttpClient client;
    private static ObjectMapper mapper;

    @BeforeAll
    static void setup() {
        // 使用SystemAvailabilityChecker API检查系统可用性
        baseUrl = SystemAvailabilityChecker.resolveBaseUrl();
        SystemAvailabilityChecker.ensureSystemRunning(baseUrl);
        
        // 初始化HTTP客户端
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        
        // 初始化ObjectMapper
        mapper = new ObjectMapper();
    }

    @Test
    void getSentEncryptedDataCountShouldReturnValidResponse() throws Exception {
        String url = buildApiUrl("execute/encrypted-data/sent-count");
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        // 验证HTTP状态码
        assertEquals(200, response.statusCode(), "HTTP状态码应为200");
        
        // 解析并验证响应体
        JsonNode root = mapper.readTree(response.body());
        assertNotNull(root, "响应体不应为空");
        
        // 验证响应结构
        assertTrue(root.has("status"), "响应应包含status字段");
        assertTrue(root.has("sentCount"), "响应应包含sentCount字段");
        assertTrue(root.has("timestamp"), "响应应包含timestamp字段");
        
        // 验证状态为UP
        assertEquals("UP", root.get("status").asText(), "健康状态应为UP");
        
        // 验证sentCount为数字类型
        assertTrue(root.get("sentCount").isNumber(), "sentCount应为数字类型");
        
        // 验证timestamp为数字类型
        assertTrue(root.get("timestamp").isNumber(), "timestamp应为数字类型");
    }

    @Test
    void checkEncryptedDataTableHealthShouldReturnValidResponse() throws Exception {
        String url = buildApiUrl("execute/encrypted-data/table-health");
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        // 验证HTTP状态码
        assertEquals(200, response.statusCode(), "HTTP状态码应为200");
        
        // 解析并验证响应体
        JsonNode root = mapper.readTree(response.body());
        assertNotNull(root, "响应体不应为空");
        
        // 验证响应结构
        assertTrue(root.has("status"), "响应应包含status字段");
        assertTrue(root.has("message"), "响应应包含message字段");
        assertTrue(root.has("timestamp"), "响应应包含timestamp字段");
        
        // 验证状态为UP
        assertEquals("UP", root.get("status").asText(), "健康状态应为UP");
        
        // 验证message包含表名
        String message = root.get("message").asText();
        assertTrue(message.contains("ENCRYPTED_DATA_TEMP"), "响应消息应包含表名ENCRYPTED_DATA_TEMP");
        assertTrue(message.contains("正常"), "响应消息应包含正常状态描述");
        
        // 验证timestamp为数字类型
        assertTrue(root.get("timestamp").isNumber(), "timestamp应为数字类型");
    }

    /**
     * 构建业务API端点URL（统一根地址 + /api/...）
     */
    private String buildApiUrl(String path) {
        String apiPath = path.startsWith("api/") ? path : "api/" + path;
        return baseUrl.endsWith("/") ? baseUrl + apiPath : baseUrl + "/" + apiPath;
    }
}
