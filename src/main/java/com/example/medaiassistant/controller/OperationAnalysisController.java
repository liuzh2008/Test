package com.example.medaiassistant.controller;

import com.example.medaiassistant.config.AIModelConfig;
import com.example.medaiassistant.dto.AIRequest;
import com.example.medaiassistant.service.EmrRecordService;
import com.example.medaiassistant.service.SurgeryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 手术分析控制器
 * 提供手术记录分析和手术名称整理的API接口
 * 
 * @author Cline
 * @version 1.0.0
 * @since 2025-09-24
 * 
 * @see EmrRecordService
 * @see AIModelConfig
 * @see AIRequest
 * 
 * @example
 * // 单个患者手术名称分析
 * POST /api/operations/analyze-names?patientId=99050801275226_1
 * 
 * // 批量患者手术名称分析
 * POST /api/operations/batch-analyze-names
 * Body: ["99050801275226_1", "99050801275226_2", "99050801275226_3"]
 */
@RestController
@RequestMapping("/api/operations")
public class OperationAnalysisController {
    private static final Logger logger = LoggerFactory.getLogger(OperationAnalysisController.class);

    @Autowired
    private EmrRecordService emrRecordService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AIModelConfig aiModelConfig;

    @Autowired
    private SurgeryService surgeryService;

    /**
     * 分析手术名称
     * 通过LLM分析手术记录，提取和整理手术/操作名称
     * 
     * @param patientId 患者ID
     * @return 手术名称分析结果（JSON格式）
     */
    @PostMapping("/analyze-names")
    public ResponseEntity<Map<String, Object>> analyzeOperationNames(@RequestParam String patientId) {
        try {
            logger.info("开始分析手术名称 - 患者ID: {}", patientId);

            // 1. 获取手术记录数据
            String operationContent = emrRecordService.findOperations(patientId, "");
            if (operationContent == null || operationContent.trim().isEmpty()) {
                logger.warn("未找到患者 {} 的手术记录", patientId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "未找到该患者的手术记录"));
            }

            logger.info("获取到手术记录内容，长度: {} 字符", operationContent.length());

            // 2. 获取手术名称整理模板
            String operationPromptTemplate = getOperationPromptTemplate();
            if (operationPromptTemplate == null || operationPromptTemplate.trim().isEmpty()) {
                logger.error("未找到手术名称整理模板");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "系统配置错误：未找到手术名称整理模板"));
            }

            // 3. 构建LLM请求消息
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 系统提示词
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个专业的医疗AI助手，专门负责分析和整理手术记录中的手术名称。请严格按照要求格式输出JSON结果。");
            messages.add(systemMessage);

            // 用户消息（手术记录内容 + 模板要求）
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", operationPromptTemplate + "\n\n手术记录内容：\n" + operationContent);
            messages.add(userMessage);

            // 4. 构建AI请求
            AIRequest aiRequest = new AIRequest();
            aiRequest.setModel("deepseek-chat"); // 使用DeepSeek模型
            aiRequest.setMessages(messages);
            aiRequest.setStream(false); // 非流式响应
            aiRequest.setTemperature(0.1); // 低温度确保一致性
            aiRequest.setMaxTokens(2000); // 足够的token数量

            // 5. 调用AI服务
            logger.info("调用AI服务分析手术名称...");
            ResponseEntity<String> aiResponse = callAIService(aiRequest);
            
            if (!aiResponse.getStatusCode().is2xxSuccessful()) {
                String errorBody = aiResponse.getBody();
                logger.error("AI服务调用失败: {}", errorBody != null ? errorBody : "空响应体");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "AI服务调用失败: " + (errorBody != null ? errorBody : "空响应体")));
            }

            // 6. 解析AI响应
            String responseBody = aiResponse.getBody();
            if (responseBody == null) {
                logger.error("AI响应体为空");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "AI服务返回空响应"));
            }
            logger.info("AI响应接收成功，长度: {} 字符", responseBody.length());

            // 尝试解析JSON响应
            try {
                ObjectMapper mapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = (Map<String, Object>) mapper.readValue(responseBody, Map.class);
                
                // 检查是否有错误
                if (responseMap.containsKey("error")) {
                    logger.error("AI服务返回错误: {}", responseMap.get("error"));
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "AI分析失败: " + responseMap.get("error")));
                }

                // 提取内容 - 处理DeepSeek API的标准响应格式
                String content = extractContentFromDeepSeekResponse(responseMap);
                if (content == null || content.trim().isEmpty()) {
                logger.error("AI响应内容为空");
                logger.error("完整的AI响应: {}", responseBody != null ? responseBody : "空响应体");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "AI分析结果为空", "rawResponse", responseBody != null ? responseBody : "空响应体"));
                }

                logger.info("提取到AI分析内容，长度: {} 字符", content.length());
                
                // 7. 验证和返回结果
                Map<String, Object> result = validateAndParseOperationResult(content);
                logger.info("手术名称分析完成，找到 {} 条手术记录", 
                        result.containsKey("SurgeryRecords") ? 
                        ((List<?>) result.get("SurgeryRecords")).size() : 0);

                // 8. 处理并保存手术名称到数据库
                try {
                    int savedCount = surgeryService.processAndSaveOperationNames(patientId, result);
                    logger.info("成功保存 {} 条手术名称记录到数据库", savedCount);
                    
                    // 在返回结果中添加保存信息
                    Map<String, Object> finalResult = new HashMap<>(result);
                    finalResult.put("savedCount", savedCount);
                    finalResult.put("saveStatus", savedCount > 0 ? "success" : "no_new_records");
                    
                    return ResponseEntity.ok(finalResult);
                } catch (Exception e) {
                    logger.error("保存手术名称到数据库失败: {}", e.getMessage(), e);
                    // 即使保存失败，仍然返回分析结果，但添加错误信息
                    Map<String, Object> finalResult = new HashMap<>(result);
                    finalResult.put("saveError", "保存到数据库失败: " + e.getMessage());
                    finalResult.put("savedCount", 0);
                    finalResult.put("saveStatus", "failed");
                    return ResponseEntity.ok(finalResult);
                }

            } catch (Exception e) {
                logger.error("解析AI响应失败: {}", e.getMessage(), e);
                logger.error("原始响应内容: {}", responseBody != null ? responseBody : "空响应体");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "解析AI响应失败: " + e.getMessage(), "rawResponse", responseBody != null ? responseBody : "空响应体"));
            }

        } catch (Exception e) {
            logger.error("分析手术名称时发生错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "分析手术名称失败: " + e.getMessage()));
        }
    }

    /**
     * 获取手术名称整理模板
     * 
     * @return 手术名称整理模板字符串
     * 
     * @description
     * 返回预定义的手术名称整理模板，用于指导LLM分析手术记录。
     * 模板包含JSON格式的输出要求，确保LLM返回标准化的手术名称结果。
     * 
     * @example
     * 模板内容示例：
     * "请整理手术及操作记录，分析本次手术及操作名称。
     * 要求：
     * 1.只分析手术及操作的名称。
     * 2.输出Json格式的手术及操作名称，格式如下：
     * {
     *   \"SurgeryRecords\": [
     *     {
     *       \"SurgeryDate\": \"2025-04-01\",
     *       \"SurgeryName\": [
     *         \"手术/操作名称1\",
     *         \"手术/操作名称2\"
     *       ]
     *     }
     *   ]
     * }"
     */
    private String getOperationPromptTemplate() {
        // 这里使用用户提供的模板内容
        return "请整理手术及操作记录，分析本次手术及操作名称。\n" +
               "要求：\n" +
               "1.只分析手术及操作的名称。\n" +
               "2.输出Json格式的手术及操作名称，格式如下：\n" +
               "{\n" +
               "  \"SurgeryRecords\": [\n" +
               "    {\n" +
               "      \"SurgeryDate\": \"2025-04-01\",\n" +
               "      \"SurgeryName\": [\n" +
               "        \"手术/操作名称1\",\n" +
               "        \"手术/操作名称2\"\n" +
               "      ]\n" +
               "    }\n" +
               "  ]\n" +
               "}";
    }

    /**
     * 调用AI服务
     * 
     * @param aiRequest AI请求对象，包含模型、消息、参数等信息
     * @return AI服务响应实体
     * 
     * @throws RuntimeException 当模型配置不存在时抛出异常
     * 
     * @description
     * 直接调用DeepSeek API进行AI分析，使用RestTemplate发送HTTP请求。
     * 强制设置为非流式响应以确保响应格式的一致性。
     * 
     * @process
     * 1. 获取模型配置
     * 2. 准备请求头和认证信息
     * 3. 构建请求体（强制stream=false）
     * 4. 发送HTTP请求
     * 5. 记录详细的响应信息用于调试
     * 
     * @example
     * AI请求示例：
     * {
     *   "model": "deepseek-chat",
     *   "messages": [...],
     *   "stream": false,
     *   "temperature": 0.1,
     *   "max_tokens": 2000
     * }
     */
    private ResponseEntity<String> callAIService(AIRequest aiRequest) {
        try {
            // 获取模型配置
            AIModelConfig.ModelConfig modelConfig = aiModelConfig.getModelConfig(aiRequest.getModel());
            if (modelConfig == null) {
                throw new RuntimeException("不支持的模型: " + aiRequest.getModel());
            }

            // 准备请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(modelConfig.getKey());

            // 构建请求体 - 使用DeepSeek API的标准格式
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", aiRequest.getModel());
            requestBody.put("messages", aiRequest.getMessages());
            requestBody.put("stream", false); // 强制设置为非流式
            requestBody.put("temperature", aiRequest.getTemperature());
            requestBody.put("max_tokens", aiRequest.getMaxTokens());

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // 发送请求
            logger.info("发送AI请求到: {}", modelConfig.getUrl());
            ResponseEntity<String> response = restTemplate.exchange(
                    modelConfig.getUrl(), 
                    HttpMethod.POST, 
                    requestEntity, 
                    String.class
            );

            logger.info("AI服务响应状态: {}", response.getStatusCode());
            logger.info("AI服务响应头: {}", response.getHeaders());
            String responseBody = response.getBody();
            logger.info("AI服务响应体长度: {}", responseBody != null ? responseBody.length() : 0);

            // 记录响应内容的前200个字符用于调试
            String body = response.getBody();
            if (body != null && body.length() > 200) {
                logger.info("AI响应预览: {}...", body.substring(0, 200));
            } else if (body != null) {
                logger.info("AI响应内容: {}", body);
            }

            return response;

        } catch (Exception e) {
            logger.error("调用AI服务失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"调用AI服务失败: " + e.getMessage() + "\"}");
        }
    }

    /**
     * 验证和解析手术结果
     * 
     * @param content AI分析返回的内容字符串
     * @return 验证后的手术结果Map
     * 
     * @throws RuntimeException 当JSON格式无效或缺少必需字段时抛出异常
     * 
     * @description
     * 验证AI返回的手术名称分析结果是否符合预期的JSON格式。
     * 支持直接解析JSON和从文本中提取JSON两种方式。
     * 
     * @validation
     * - 必需字段：SurgeryRecords
     * - 每条手术记录必需字段：SurgeryDate, SurgeryName
     * - SurgeryName必须是字符串数组
     * 
     * @fallback
     * 如果直接解析失败，会尝试从文本中提取JSON内容
     * 
     * @example
     * 有效结果示例：
     * {
     *   "SurgeryRecords": [
     *     {
     *       "SurgeryDate": "2025-04-01",
     *       "SurgeryName": ["阑尾切除术", "腹腔镜探查"]
     *     }
     *   ]
     * }
     */
    private Map<String, Object> validateAndParseOperationResult(String content) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            
            // 首先尝试直接解析JSON
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) mapper.readValue(content, Map.class);
                
                // 验证必需字段
                if (!result.containsKey("SurgeryRecords")) {
                    throw new RuntimeException("缺少SurgeryRecords字段");
                }
                
                List<?> surgeryRecords = (List<?>) result.get("SurgeryRecords");
                for (Object record : surgeryRecords) {
                    if (record instanceof Map) {
                        Map<?, ?> recordMap = (Map<?, ?>) record;
                        if (!recordMap.containsKey("SurgeryDate") || !recordMap.containsKey("SurgeryName")) {
                            throw new RuntimeException("手术记录缺少必需字段");
                        }
                    }
                }
                
                return result;
            } catch (Exception e) {
                logger.warn("直接解析JSON失败，尝试从文本中提取JSON: {}", e.getMessage());
                
            // 尝试从文本中提取JSON部分
            String jsonContent = extractJsonFromText(content);
            if (jsonContent != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) mapper.readValue(jsonContent, Map.class);
                return result;
            } else {
                throw new RuntimeException("无法从响应中提取有效的JSON格式");
            }
            }
            
        } catch (Exception e) {
            logger.error("验证和解析手术结果失败: {}", e.getMessage());
            // 返回错误格式的结果
            return Map.of(
                "error", "解析手术结果失败: " + e.getMessage(),
                "rawContent", content
            );
        }
    }

    /**
     * 从DeepSeek API响应中提取内容
     * 
     * @param responseMap DeepSeek API返回的响应Map
     * @return 提取的内容字符串，如果提取失败返回null
     * 
     * @description
     * 解析DeepSeek API的标准响应格式，从嵌套结构中提取实际的分析内容。
     * DeepSeek API响应结构：response → choices → message → content
     * 
     * @structure
     * {
     *   "choices": [
     *     {
     *       "message": {
     *         "content": "实际分析内容"
     *       }
     *     }
     *   ]
     * }
     * 
     * @validation
     * - 检查choices字段是否存在
     * - 检查choices数组是否为空
     * - 检查message字段是否存在
     * - 检查content字段是否存在
     * 
     * @example
     * 响应示例：
     * {
     *   "choices": [
     *     {
     *       "message": {
     *         "content": "{\"SurgeryRecords\":[...]}"
     *       }
     *     }
     *   ]
     * }
     */
    private String extractContentFromDeepSeekResponse(Map<String, Object> responseMap) {
        try {
            logger.info("开始解析DeepSeek API响应结构");
            
            // 检查是否有choices字段
            if (!responseMap.containsKey("choices")) {
                logger.error("DeepSeek响应缺少choices字段");
                return null;
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices == null || choices.isEmpty()) {
                logger.error("DeepSeek响应中choices为空");
                return null;
            }
            
            // 获取第一个choice
            Map<String, Object> firstChoice = choices.get(0);
            if (!firstChoice.containsKey("message")) {
                logger.error("DeepSeek响应中choice缺少message字段");
                return null;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
            if (!message.containsKey("content")) {
                logger.error("DeepSeek响应中message缺少content字段");
                return null;
            }
            
            String content = (String) message.get("content");
            logger.info("成功从DeepSeek响应中提取内容，长度: {} 字符", content != null ? content.length() : 0);
            return content;
            
        } catch (Exception e) {
            logger.error("解析DeepSeek响应失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从文本中提取JSON内容
     * 
     * @param text 包含JSON内容的文本字符串
     * @return 提取的JSON字符串，如果提取失败返回null
     * 
     * @description
     * 从可能包含其他文本内容的字符串中提取有效的JSON部分。
     * 用于处理AI响应中可能包含非JSON内容的情况。
     * 
     * @algorithm
     * 1. 查找第一个'{'字符作为JSON开始位置
     * 2. 查找最后一个'}'字符作为JSON结束位置
     * 3. 提取'{'和'}'之间的内容
     * 4. 验证提取的内容是否为有效JSON
     * 
     * @fallback
     * 如果提取失败或验证无效，返回null
     * 
     * @example
     * 输入文本：
     * "这是AI的响应：{\"SurgeryRecords\":[...]} 分析完成"
     * 
     * 提取结果：
     * "{\"SurgeryRecords\":[...]}"
     */
    private String extractJsonFromText(String text) {
        try {
            // 查找第一个{和最后一个}
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            
            if (start != -1 && end != -1 && end > start) {
                String jsonContent = text.substring(start, end + 1);
                // 验证提取的内容是否为有效JSON
                ObjectMapper mapper = new ObjectMapper();
                mapper.readTree(jsonContent); // 如果无效会抛出异常
                return jsonContent;
            }
        } catch (Exception e) {
            logger.warn("提取JSON失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 批量分析手术名称（支持多个患者）
     * 
     * @param patientIds 患者ID列表
     * @return 批量分析结果，包含成功和失败的患者分析结果
     * 
     * @description
     * 对多个患者的手术记录进行批量分析，支持并发处理。
     * 每个患者的分析都是独立的，失败的分析不会影响其他患者。
     * 
     * @response
     * {
     *   "totalPatients": 3,
     *   "successfulCount": 2,
     *   "failedCount": 1,
     *   "successfulResults": [
     *     {
     *       "patientId": "99050801275226_1",
     *       "data": { "SurgeryRecords": [...] }
     *     }
     *   ],
     *   "failedResults": [
     *     {
     *       "patientId": "99050801275226_3",
     *       "error": "分析失败原因"
     *     }
     *   ]
     * }
     * 
     * @concurrency
     * 当前实现是顺序处理，未来可以优化为并发处理以提高性能
     * 
     * @errorHandling
     * - 单个患者分析失败不影响其他患者
     * - 失败的患者会记录详细的错误信息
     * - 返回结果中包含完整的成功和失败统计
     */
    @PostMapping("/batch-analyze-names")
    public ResponseEntity<Map<String, Object>> batchAnalyzeOperationNames(@RequestBody List<String> patientIds) {
        try {
            logger.info("批量分析手术名称 - 患者数量: {}", patientIds.size());
            
            Map<String, Object> results = new HashMap<>();
            List<Map<String, Object>> successfulResults = new ArrayList<>();
            List<Map<String, Object>> failedResults = new ArrayList<>();
            
            for (String patientId : patientIds) {
                try {
                    ResponseEntity<Map<String, Object>> result = analyzeOperationNames(patientId);
                    if (result.getStatusCode().is2xxSuccessful()) {
                        Map<String, Object> successResult = new HashMap<>();
                        successResult.put("patientId", patientId);
                        successResult.put("data", result.getBody());
                        successfulResults.add(successResult);
                    } else {
                        Map<String, Object> failedResult = new HashMap<>();
                        failedResult.put("patientId", patientId);
                        Map<String, Object> errorBody = result.getBody();
                        if (errorBody != null && errorBody.containsKey("error")) {
                            failedResult.put("error", errorBody.get("error"));
                        } else {
                            failedResult.put("error", "未知错误");
                        }
                        failedResults.add(failedResult);
                    }
                } catch (Exception e) {
                    Map<String, Object> failedResult = new HashMap<>();
                    failedResult.put("patientId", patientId);
                    failedResult.put("error", "分析过程中发生异常: " + e.getMessage());
                    failedResults.add(failedResult);
                }
            }
            
            results.put("totalPatients", patientIds.size());
            results.put("successfulCount", successfulResults.size());
            results.put("failedCount", failedResults.size());
            results.put("successfulResults", successfulResults);
            results.put("failedResults", failedResults);
            
            logger.info("批量分析完成 - 成功: {}, 失败: {}", successfulResults.size(), failedResults.size());
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("批量分析手术名称失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "批量分析失败: " + e.getMessage()));
        }
    }
}
