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
 * 患者数据保存外部集成测试
 * 使用 SystemAvailabilityChecker API 进行系统可用性检查
 * 直接对运行中的后端服务发起HTTP请求，不加载Spring上下文
 * 
 * 红阶段：测试保存患者数据的API端点不存在（期望404）
 */
class PatientSaveExternalIntegrationTest {

    private static String baseUrl;

    @BeforeAll
    static void checkSystemAvailability() {
        // 使用 SystemAvailabilityChecker API 检查系统可用性
        baseUrl = SystemAvailabilityChecker.resolveBaseUrl();
        SystemAvailabilityChecker.ensureSystemRunning(baseUrl);
    }

    /**
     * 构建业务API端点URL（统一根地址 + /api/...）
     */
    private String buildApiUrl(String path) {
        String apiPath = path.startsWith("api/") ? path : "api/" + path;
        return baseUrl.endsWith("/") ? baseUrl + apiPath : baseUrl + "/" + apiPath;
    }

    /**
     * 测试保存患者数据的API端点不存在（期望404）
     * 红阶段：验证POST /api/patients返回404
     */
    @Test
    void savePatientEndpointShouldReturn404WhenNotImplemented() throws Exception {
        String url = buildApiUrl("patients");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // 创建测试患者数据
        String requestBody = """
            {
                "patientId": "TEST001_1",
                "name": "测试患者",
                "department": "测试科室",
                "status": "普通"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 红阶段：期望404，因为API端点尚未实现
        assertEquals(404, response.statusCode(), 
            "红阶段：POST /api/patients 应返回404，因为API端点尚未实现。实际状态码：" + response.statusCode());
    }

    /**
     * 测试使用有效数据保存患者时返回404
     * 红阶段：验证有效数据保存返回404
     */
    @Test
    void savePatientWithValidDataShouldReturn404WhenNotImplemented() throws Exception {
        String url = buildApiUrl("patients");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // 创建完整的测试患者数据
        String requestBody = """
            {
                "patientId": "TEST002_1",
                "idCard": "510123198001011234",
                "medicalRecordNumber": "MRN002",
                "name": "李四",
                "gender": "男",
                "dateOfBirth": "1980-01-01",
                "bedNumber": "101",
                "admissionTime": "2025-12-10T10:30:00",
                "dischargeTime": null,
                "isInHospital": true,
                "department": "心血管一病区",
                "importantInformation": "重要信息备注",
                "patiId": "TEST002",
                "visitId": 1,
                "patientNo": "TEST002",
                "drgsResult": "DRGs分析结果",
                "drgsSevereComplication": false,
                "drgsCommonComplication": false,
                "status": "普通"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 红阶段：期望404，因为API端点尚未实现
        assertEquals(404, response.statusCode(), 
            "红阶段：使用有效数据保存患者应返回404，因为API端点尚未实现。实际状态码：" + response.statusCode());
    }

    /**
     * 测试更新现有患者时返回404
     * 红阶段：验证更新现有患者返回404
     */
    @Test
    void updateExistingPatientShouldReturn404WhenNotImplemented() throws Exception {
        String url = buildApiUrl("patients");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // 创建更新患者数据
        String requestBody = """
            {
                "patientId": "EXISTING001_1",
                "name": "更新后的患者",
                "department": "更新科室",
                "status": "病重",
                "gender": "男",
                "dateOfBirth": "1980-01-01",
                "admissionTime": "2025-12-10T10:30:00",
                "isInHospital": true
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 红阶段：期望404，因为API端点尚未实现
        assertEquals(404, response.statusCode(), 
            "红阶段：更新现有患者应返回404，因为API端点尚未实现。实际状态码：" + response.statusCode());
    }

    /**
     * 测试健康检查端点正常
     * 确保服务正常运行
     * 使用SystemAvailabilityChecker中的健康检查端点
     */
    @Test
    void healthEndpointShouldBeUp() throws Exception {
        // 使用SystemAvailabilityChecker中的健康检查端点
        String url = SystemAvailabilityChecker.buildApiUrl(baseUrl, "hospital-config/health");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "健康检查端点应返回200");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());

        assertNotNull(root.get("status"), "响应应包含status字段");
        assertEquals("UP", root.get("status").asText(), "健康状态应为UP");
    }
}
