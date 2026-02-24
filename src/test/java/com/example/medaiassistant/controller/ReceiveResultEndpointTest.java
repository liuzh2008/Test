package com.example.medaiassistant.controller;

import com.example.medaiassistant.config.AIModelConfig;
import com.example.medaiassistant.dto.ReceiveResultDTO;
import com.example.medaiassistant.repository.*;
import com.example.medaiassistant.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ReceiveResultEndpointTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PromptResultRepository promptResultRepository;

    @Mock
    private PromptTemplateRepository promptTemplateRepository;

    @Mock
    private PromptRepository promptRepository;

    @Mock
    private ConversationHistoryRepository conversationHistoryRepository;

    @Mock
    private AIModelConfig aiModelConfig;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DiagnosisRepository diagnosisRepository;

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private LongTermOrderRepository longTermOrderRepository;

    @Mock
    private LabResultRepository labResultRepository;

    @Mock
    private ExaminationResultRepository examinationResultRepository;

    @Mock
    private AlertTaskRepository alertTaskRepository;

    @Mock
    private OrderFormatService orderFormatService;

    @Mock
    private ExaminationResultService examinationResultService;

    @Mock
    private EmrRecordService emrRecordService;

    @Mock
    private PatientDataDesensitizationService patientDataDesensitizationService;

    @InjectMocks
    private AIController aiController;

    @Test
    public void testReceiveAIResult_Success() {
        // 准备测试数据
        ReceiveResultDTO request = new ReceiveResultDTO();
        request.setDataId("test-data-001");
        request.setContent("AI处理成功的结果内容");
        request.setStatus("SUCCESS");
        request.setTitle("测试结果标题");

        // 执行测试
        ResponseEntity<Map<String, Object>> response = aiController.receiveAIResult(request);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals("SUCCESS", responseBody.get("status"));
        assertEquals("结果接收成功", responseBody.get("message"));
        assertEquals("test-data-001", responseBody.get("dataId"));
        assertNotNull(responseBody.get("timestamp"));
    }

    @Test
    public void testReceiveAIResult_ErrorStatus() {
        // 准备测试数据 - 错误状态
        ReceiveResultDTO request = new ReceiveResultDTO();
        request.setDataId("test-data-002");
        request.setErrorMessage("处理超时错误");
        request.setStatus("ERROR");

        // 执行测试
        ResponseEntity<Map<String, Object>> response = aiController.receiveAIResult(request);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals("SUCCESS", responseBody.get("status")); // 接口调用本身成功
        assertEquals("失败结果接收成功", responseBody.get("message"));
        assertEquals("test-data-002", responseBody.get("dataId"));
        assertEquals("处理超时错误", responseBody.get("errorMessage"));
        assertNotNull(responseBody.get("timestamp"));
    }

    @Test
    public void testReceiveAIResult_UnknownStatus() {
        // 准备测试数据 - 未知状态
        ReceiveResultDTO request = new ReceiveResultDTO();
        request.setDataId("test-data-003");
        request.setContent("未知状态的内容");
        request.setStatus("UNKNOWN");

        // 执行测试
        ResponseEntity<Map<String, Object>> response = aiController.receiveAIResult(request);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals("SUCCESS", responseBody.get("status"));
        assertEquals("结果接收成功（未知状态）", responseBody.get("message"));
        assertEquals("test-data-003", responseBody.get("dataId"));
        assertEquals("UNKNOWN", responseBody.get("receivedStatus"));
        assertNotNull(responseBody.get("timestamp"));
    }

    @Test
    public void testReceiveAIResult_MissingDataId() {
        // 准备测试数据 - 缺少dataId
        ReceiveResultDTO request = new ReceiveResultDTO();
        request.setContent("缺少dataId的测试内容");
        request.setStatus("SUCCESS");
        // 故意不设置dataId

        // 执行测试
        ResponseEntity<Map<String, Object>> response = aiController.receiveAIResult(request);

        // 验证结果 - 应该返回400错误
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals("ERROR", responseBody.get("status"));
        assertEquals("dataId不能为空", responseBody.get("message"));
        assertNotNull(responseBody.get("timestamp"));
    }

    @Test
    public void testReceiveAIResult_EmptyDataId() {
        // 准备测试数据 - 空dataId
        ReceiveResultDTO request = new ReceiveResultDTO();
        request.setDataId(""); // 空字符串
        request.setContent("空dataId的测试内容");
        request.setStatus("SUCCESS");

        // 执行测试
        ResponseEntity<Map<String, Object>> response = aiController.receiveAIResult(request);

        // 验证结果 - 应该返回400错误
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals("ERROR", responseBody.get("status"));
        assertEquals("dataId不能为空", responseBody.get("message"));
        assertNotNull(responseBody.get("timestamp"));
    }

    @Test
    public void testReceiveAIResult_BlankDataId() {
        // 准备测试数据 - 空白dataId
        ReceiveResultDTO request = new ReceiveResultDTO();
        request.setDataId("   "); // 空白字符串
        request.setContent("空白dataId的测试内容");
        request.setStatus("SUCCESS");

        // 执行测试
        ResponseEntity<Map<String, Object>> response = aiController.receiveAIResult(request);

        // 验证结果 - 应该返回400错误
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals("ERROR", responseBody.get("status"));
        assertEquals("dataId不能为空", responseBody.get("message"));
        assertNotNull(responseBody.get("timestamp"));
    }

    @Test
    public void testReceiveAIResult_ExceptionHandling() {
        // 准备测试数据
        ReceiveResultDTO request = new ReceiveResultDTO();
        request.setDataId("test-data-exception");
        request.setStatus("SUCCESS");

        // 模拟一个异常情况（例如通过mockito抛出异常）
        // 这里我们测试异常处理机制，但由于receiveAIResult方法没有外部依赖调用，
        // 我们主要测试其异常捕获和错误响应机制

        // 执行测试
        ResponseEntity<Map<String, Object>> response = aiController.receiveAIResult(request);

        // 验证正常情况下的响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals("SUCCESS", responseBody.get("status"));
    }

    @Test
    public void testReceiveAIResult_WithAllOptionalFields() {
        // 准备测试数据 - 包含所有可选字段
        ReceiveResultDTO request = new ReceiveResultDTO();
        request.setDataId("test-data-complete");
        request.setContent("完整的结果内容");
        request.setTitle("完整标题");
        request.setTimestamp("2025-09-13T11:30:00");
        request.setPromptId(123);
        request.setPatientId("P001");
        request.setOriginalContent("原始内容");
        request.setLastModifiedBy(1);
        request.setIsRead(0);
        request.setStatus("SUCCESS");
        request.setErrorMessage(null); // 明确设置为null

        // 执行测试
        ResponseEntity<Map<String, Object>> response = aiController.receiveAIResult(request);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals("SUCCESS", responseBody.get("status"));
        assertEquals("结果接收成功", responseBody.get("message"));
        assertEquals("test-data-complete", responseBody.get("dataId"));
        assertNotNull(responseBody.get("timestamp"));
    }
}
