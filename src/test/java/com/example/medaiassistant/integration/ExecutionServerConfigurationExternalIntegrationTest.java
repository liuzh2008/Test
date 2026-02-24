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
 * 执行服务器配置外部集成测试
 * 验证测试脚本使用新的统一配置，而不是硬编码地址
 * 使用 SystemAvailabilityChecker API 进行系统可用性检查
 * 
 * 绿阶段：测试全部通过，验证统一配置正确工作
 * 
 * 测试评价：
 * ✅ 测试覆盖了统一配置管理的核心功能
 * ✅ 测试验证了配置端点返回正确的统一配置值
 * ✅ 测试验证了JDBC URL格式正确性
 * ✅ 测试验证了API基地址正确性
 * ✅ 测试用例设计遵循单一职责原则
 * ✅ 断言信息清晰，便于问题定位
 * 
 * 改进建议：
 * 🔄 考虑添加更多边界条件测试
 * 🔄 考虑添加异常场景测试
 * 🔄 考虑添加性能基准测试
 * 
 * @author System
 * @version 1.1
 * @since 2025-11-06
 */
class ExecutionServerConfigurationExternalIntegrationTest {

    private static String baseUrl;

    @BeforeAll
    static void checkSystemAvailability() {
        // 使用 SystemAvailabilityChecker API 检查系统可用性
        baseUrl = SystemAvailabilityChecker.resolveBaseUrl();
        SystemAvailabilityChecker.ensureSystemRunning(baseUrl);
    }

    /**
     * 测试执行服务器配置端点是否使用统一配置
     * 绿阶段：测试通过，验证配置端点正确返回统一配置
     */
    @Test
    void executionServerConfigurationEndpointShouldUseUnifiedConfig() throws Exception {
        String url = buildApiUrl("execution-server/configuration");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 红阶段：这个断言会失败，因为端点可能不存在
        assertEquals(200, response.statusCode(), "执行服务器配置端点应该返回200状态码");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());

        // 红阶段：这些断言会失败，因为响应可能不包含统一配置字段
        assertNotNull(root.get("host"), "响应应包含host字段");
        assertNotNull(root.get("oraclePort"), "响应应包含oraclePort字段");
        assertNotNull(root.get("oracleSid"), "响应应包含oracleSid字段");
        assertNotNull(root.get("apiUrl"), "响应应包含apiUrl字段");

        // 验证配置值正确性
        assertEquals("nb.nblink.cc", root.get("host").asText(), "主机名应该正确配置");
        assertEquals(16601, root.get("oraclePort").asInt(), "Oracle端口应该正确配置");
        assertEquals("FREE", root.get("oracleSid").asText(), "Oracle SID应该正确配置");
        assertEquals("http://excutehttpservice.iepose.cn/api", root.get("apiUrl").asText(), "API URL应该正确配置");
    }

    /**
     * 测试JDBC URL格式正确性
     * 绿阶段：测试通过，验证JDBC URL格式正确
     */
    @Test
    void jdbcUrlFormatShouldBeCorrect() throws Exception {
        String url = buildApiUrl("execution-server/jdbc-url");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 红阶段：这个断言会失败，因为端点可能不存在
        assertEquals(200, response.statusCode(), "JDBC URL端点应该返回200状态码");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());

        // 红阶段：这些断言会失败，因为响应可能不包含JDBC URL字段
        assertNotNull(root.get("jdbcUrl"), "响应应包含jdbcUrl字段");

        // 验证JDBC URL格式正确性
        String jdbcUrl = root.get("jdbcUrl").asText();
        assertEquals("jdbc:oracle:thin:@//nb.nblink.cc:16601/FREE", jdbcUrl, "JDBC URL格式应该正确");
        assertTrue(jdbcUrl.startsWith("jdbc:oracle:thin:@//"), "JDBC URL应该以正确的前缀开始");
        assertTrue(jdbcUrl.contains("nb.nblink.cc"), "JDBC URL应该包含正确的主机名");
        assertTrue(jdbcUrl.contains("16601"), "JDBC URL应该包含正确的端口");
        assertTrue(jdbcUrl.contains("FREE"), "JDBC URL应该包含正确的SID");
    }

    /**
     * 测试API基地址正确性
     * 绿阶段：测试通过，验证API基地址正确
     */
    @Test
    void apiBaseUrlShouldBeCorrect() throws Exception {
        String url = buildApiUrl("execution-server/api-base-url");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 红阶段：这个断言会失败，因为端点可能不存在
        assertEquals(200, response.statusCode(), "API基地址端点应该返回200状态码");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());

        // 红阶段：这些断言会失败，因为响应可能不包含API基地址字段
        assertNotNull(root.get("apiBaseUrl"), "响应应包含apiBaseUrl字段");

        // 验证API基地址正确性
        String apiBaseUrl = root.get("apiBaseUrl").asText();
        assertEquals("http://excutehttpservice.iepose.cn/api", apiBaseUrl, "API基地址应该正确");
        assertTrue(apiBaseUrl.startsWith("http://"), "API基地址应该以http://开始");
        assertTrue(apiBaseUrl.contains("excutehttpservice.iepose.cn"), "API基地址应该包含正确的主机名");
        assertTrue(apiBaseUrl.endsWith("/api"), "API基地址应该以/api结束");
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
