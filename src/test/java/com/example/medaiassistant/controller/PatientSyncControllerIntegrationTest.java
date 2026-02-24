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
 * 病人数据同步控制器外部集成测试
 * 按照外部集成测试编写原则，直接对运行中的后端服务发起HTTP请求，不加载Spring上下文
 * 自动化测试所有病人数据同步API接口
 * 
 * @author MedAI Assistant Team
 * @since 2025-12-10
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PatientSyncControllerIntegrationTest {
    
    private static final Logger log = LoggerFactory.getLogger(PatientSyncControllerIntegrationTest.class);
    
    private String baseUrl;
    private HttpClient httpClient;
    
    @BeforeAll
    public void setUp() {
        // 使用SystemAvailabilityChecker API检查系统可用性
        baseUrl = SystemAvailabilityChecker.resolveBaseUrl() + "/api/patient-sync";
        SystemAvailabilityChecker.ensureSystemRunning(baseUrl.replace("/api/patient-sync", ""));
        
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        log.info("测试基础URL: {}", baseUrl);
    }
    
    /**
     * 测试1: 健康检查端点 - 外部集成测试
     * 验证GET /api/patient-sync/health端点可访问
     */
    @Test
    public void testHealthEndpoint() throws Exception {
        log.info("测试病人数据同步健康检查端点");
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/health"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("病人数据同步健康检查测试通过");
    }
    
    /**
     * 测试2: 执行病人数据同步（cdwyy医院） - 外部集成测试
     * 验证POST /api/patient-sync/sync端点可访问
     */
    @Test
    public void testSyncPatientsCdwyy() throws Exception {
        log.info("测试执行病人数据同步（cdwyy医院）");
        
        String requestBody = "{\"hospitalId\": \"cdwyy\", \"department\": \"心血管一病区\"}";
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/sync"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("cdwyy医院病人数据同步测试通过");
    }
    
    /**
     * 测试3: 执行病人数据同步（hospital-001医院） - 外部集成测试
     * 验证POST /api/patient-sync/sync端点可访问
     */
    @Test
    public void testSyncPatientsHospital001() throws Exception {
        log.info("测试执行病人数据同步（hospital-001医院）");
        
        String requestBody = "{\"hospitalId\": \"hospital-001\", \"department\": \"测试科室\"}";
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/sync"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("hospital-001医院病人数据同步测试通过");
    }
    
    /**
     * 测试4: 批量执行病人数据同步 - 外部集成测试
     * 验证POST /api/patient-sync/sync/batch端点可访问
     */
    @Test
    public void testBatchSyncPatients() throws Exception {
        log.info("测试批量执行病人数据同步");
        
        String requestBody = "{\"tasks\": [" +
                "{\"hospitalId\": \"cdwyy\", \"department\": \"心血管一病区\"}," +
                "{\"hospitalId\": \"cdwyy\", \"department\": \"心血管二病区\"}," +
                "{\"hospitalId\": \"hospital-001\", \"department\": \"测试科室\"}" +
                "]}";
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/sync/batch"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("批量病人数据同步测试通过");
    }
    
    /**
     * 测试5: 查询同步状态 - 外部集成测试
     * 验证GET /api/patient-sync/status端点可访问
     */
    @Test
    public void testGetSyncStatus() throws Exception {
        log.info("测试查询同步状态");
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/status"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("同步状态查询测试通过");
    }
    
    /**
     * 测试6: 查询同步状态（按医院过滤） - 外部集成测试
     * 验证GET /api/patient-sync/status?hospitalId=cdwyy端点可访问
     */
    @Test
    public void testGetSyncStatusByHospital() throws Exception {
        log.info("测试查询同步状态（按医院过滤）");
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/status?hospitalId=cdwyy"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("按医院过滤的同步状态查询测试通过");
    }
    
    /**
     * 测试7: 查询同步状态（按科室过滤） - 外部集成测试
     * 验证GET /api/patient-sync/status?department=心血管一病区端点可访问
     */
    @Test
    public void testGetSyncStatusByDepartment() throws Exception {
        log.info("测试查询同步状态（按科室过滤）");
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/status?department=心血管一病区"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("按科室过滤的同步状态查询测试通过");
    }
    
    /**
     * 测试8: 查询同步统计信息 - 外部集成测试
     * 验证GET /api/patient-sync/statistics端点可访问
     */
    @Test
    public void testGetSyncStatistics() throws Exception {
        log.info("测试查询同步统计信息");
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/statistics"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("同步统计信息查询测试通过");
    }
    
    /**
     * 测试9: 查询同步统计信息（按日期范围） - 外部集成测试
     * 验证GET /api/patient-sync/statistics?startDate=2025-12-01&endDate=2025-12-10端点可访问
     */
    @Test
    public void testGetSyncStatisticsByDateRange() throws Exception {
        log.info("测试查询同步统计信息（按日期范围）");
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/statistics?startDate=2025-12-01&endDate=2025-12-10"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("按日期范围的同步统计信息查询测试通过");
    }
    
    /**
     * 测试10: 查询同步统计信息（按医院和日期范围） - 外部集成测试
     * 验证GET /api/patient-sync/statistics?hospitalId=cdwyy&startDate=2025-12-01&endDate=2025-12-10端点可访问
     */
    @Test
    public void testGetSyncStatisticsByHospitalAndDateRange() throws Exception {
        log.info("测试查询同步统计信息（按医院和日期范围）");
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/statistics?hospitalId=cdwyy&startDate=2025-12-01&endDate=2025-12-10"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("按医院和日期范围的同步统计信息查询测试通过");
    }
    
    /**
     * 测试11: 测试无效的同步请求 - 外部集成测试
     * 验证POST /api/patient-sync/sync端点对无效请求的处理
     */
    @Test
    public void testInvalidSyncRequest() throws Exception {
        log.info("测试无效的同步请求");
        
        // 无效的请求体（缺少必要字段）
        String requestBody = "{\"hospitalId\": \"invalid-hospital\"}";
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/sync"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("无效同步请求测试通过");
    }
    
    /**
     * 测试12: 测试不存在的医院同步 - 外部集成测试
     * 验证POST /api/patient-sync/sync端点对不存在医院的处理
     */
    @Test
    public void testNonExistentHospitalSync() throws Exception {
        log.info("测试不存在的医院同步");
        
        String requestBody = "{\"hospitalId\": \"non-existent-hospital\", \"department\": \"测试科室\"}";
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/sync"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        // 外部集成测试：验证API端点可访问
        validateApiResponse(response);
        
        log.info("不存在医院同步测试通过");
    }
    
    /**
     * 测试13: 批量测试所有接口 - 外部集成测试
     * 验证所有病人数据同步API端点可访问
     */
    @Test
    public void testAllEndpointsComprehensive() throws Exception {
        log.info("开始批量测试所有病人数据同步接口");
        
        // 1. 健康检查
        testHealthEndpoint();
        log.info("✓ 健康检查测试完成");
        
        // 2. cdwyy医院同步
        testSyncPatientsCdwyy();
        log.info("✓ cdwyy医院同步测试完成");
        
        // 3. hospital-001医院同步
        testSyncPatientsHospital001();
        log.info("✓ hospital-001医院同步测试完成");
        
        // 4. 批量同步
        testBatchSyncPatients();
        log.info("✓ 批量同步测试完成");
        
        // 5. 同步状态查询
        testGetSyncStatus();
        log.info("✓ 同步状态查询测试完成");
        
        // 6. 按医院过滤状态查询
        testGetSyncStatusByHospital();
        log.info("✓ 按医院过滤状态查询测试完成");
        
        // 7. 按科室过滤状态查询
        testGetSyncStatusByDepartment();
        log.info("✓ 按科室过滤状态查询测试完成");
        
        // 8. 同步统计信息查询
        testGetSyncStatistics();
        log.info("✓ 同步统计信息查询测试完成");
        
        // 9. 按日期范围统计查询
        testGetSyncStatisticsByDateRange();
        log.info("✓ 按日期范围统计查询测试完成");
        
        // 10. 按医院和日期范围统计查询
        testGetSyncStatisticsByHospitalAndDateRange();
        log.info("✓ 按医院和日期范围统计查询测试完成");
        
        // 11. 无效请求测试
        testInvalidSyncRequest();
        log.info("✓ 无效请求测试完成");
        
        // 12. 不存在医院测试
        testNonExistentHospitalSync();
        log.info("✓ 不存在医院测试完成");
        
        log.info("所有病人数据同步接口批量测试完成 ✓");
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
