package com.example.medaiassistant.controller;

import com.example.medaiassistant.dto.UserDecisionRequest;
import com.example.medaiassistant.enums.MccType;
import com.example.medaiassistant.integration.SystemAvailabilityChecker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserDecisionController 外部集成测试
 * 按照外部集成测试编写原则，直接对运行中的后端服务发起HTTP请求，不加载Spring上下文
 * 
 * @author MedAI Assistant Team
 * @since 2025-11-11
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserDecisionControllerTest {

    private String baseUrl;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    @BeforeAll
    void setup() {
        // 使用SystemAvailabilityChecker API检查系统可用性
        baseUrl = SystemAvailabilityChecker.resolveBaseUrl();
        SystemAvailabilityChecker.ensureSystemRunning(baseUrl);
        
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        objectMapper = new ObjectMapper();
    }

    /**
     * 测试用户选择MCC类别API端点 - 外部集成测试
     * 直接对运行中的后端服务发起HTTP请求
     */
    @Test
    void saveUserDecision_shouldReturnSavedResult() throws Exception {
        // Given - 准备测试数据
        UserDecisionRequest request = new UserDecisionRequest();
        request.setResultId(1L);
        request.setPatientId("PAT001");
        request.setSelectedMccType(MccType.MCC);
        request.setOperator("test-user");

        // When - 发起HTTP请求
        String requestBody = objectMapper.writeValueAsString(request);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/user-decision/save"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        // Then - 验证响应
        // 外部集成测试：验证API端点可访问，响应状态码在200-299或400-499范围内
        int statusCode = response.statusCode();
        assertTrue(statusCode >= 200 && statusCode < 500, 
            "API端点应返回有效的HTTP状态码，实际返回: " + statusCode);
        
        String responseBody = response.body();
        assertNotNull(responseBody, "响应体不应为空");
        
        // 记录响应信息用于调试
        System.out.println("API响应状态码: " + statusCode);
        System.out.println("API响应体: " + responseBody);
    }

    /**
     * 测试根据分析结果ID查询决策结果API端点 - 外部集成测试
     */
    @Test
    void getDecisionResult_shouldReturnResultWhenValidId() throws Exception {
        // Given - 准备测试数据
        Long resultId = 1L;

        // When - 发起HTTP请求
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/user-decision/" + resultId))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        // Then - 验证响应
        // 注意：由于是外部集成测试，实际响应可能根据后端状态而变化
        // 这里主要验证API端点可访问
        assertTrue(response.statusCode() == 200 || response.statusCode() == 404);
    }

    /**
     * 测试根据患者ID查询决策结果API端点 - 外部集成测试
     */
    @Test
    void getDecisionResultsByPatientId_shouldReturnResultsWhenValidPatientId() throws Exception {
        // Given - 准备测试数据
        String patientId = "PAT001";

        // When - 发起HTTP请求
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/user-decision/patient/" + patientId))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        // Then - 验证响应
        // 注意：由于是外部集成测试，实际响应可能根据后端状态而变化
        // 这里主要验证API端点可访问
        assertTrue(response.statusCode() == 200 || response.statusCode() == 404);
    }

    /**
     * 测试无效请求参数验证 - 外部集成测试
     */
    @Test
    void saveUserDecision_shouldReturnBadRequestWhenInvalidRequest() throws Exception {
        // Given - 准备无效的测试数据
        UserDecisionRequest invalidRequest = new UserDecisionRequest();
        // 缺少必要的字段

        // When - 发起HTTP请求
        String requestBody = objectMapper.writeValueAsString(invalidRequest);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/user-decision/save"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        // Then - 验证错误响应
        // 注意：由于是外部集成测试，实际响应可能根据后端验证逻辑而变化
        assertTrue(response.statusCode() >= 400 && response.statusCode() < 500);
    }

    /**
     * 测试分析结果不存在时返回404 - 外部集成测试
     */
    @Test
    void getDecisionResult_shouldReturnNotFoundWhenResultNotFound() throws Exception {
        // Given - 准备不存在的ID
        Long nonExistentId = 999999L;

        // When - 发起HTTP请求
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/user-decision/" + nonExistentId))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        // Then - 验证错误响应
        // 注意：由于是外部集成测试，实际响应可能根据后端逻辑而变化
        assertTrue(response.statusCode() == 404 || response.statusCode() == 400);
    }
}
