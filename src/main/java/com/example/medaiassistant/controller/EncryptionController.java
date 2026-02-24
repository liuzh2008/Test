package com.example.medaiassistant.controller;

import com.example.medaiassistant.util.AESEncryptionUtil;
import com.example.medaiassistant.service.ServerConfigService;
import com.example.medaiassistant.model.ServerConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * 加密解密控制器类
 * 
 * 该类负责处理所有与数据加密解密相关的HTTP请求，包括AES加密、解密和安全传输功能。
 * 作为RESTful API控制器，提供了一系列端点用于前后端安全数据交互。
 * 
 * 主要功能包括：
 * - 数据AES-256加密
 * - 加密数据解密
 * - 加密数据安全传输
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2025
 */
@RestController
@RequestMapping("/api/encryption")
public class EncryptionController {
    private static final Logger logger = LoggerFactory.getLogger(EncryptionController.class);

    private final RestTemplate restTemplate;
    private final ServerConfigService serverConfigService;

    @Value("${api.base.url:http://localhost:8081}")
    private String apiBaseUrl;

    public EncryptionController(RestTemplate restTemplate,
                                ServerConfigService serverConfigService) {
        this.restTemplate = restTemplate;
        this.serverConfigService = serverConfigService;
    }

    /**
     * 加密并传输Prompt到执行服务器
     * 
     * 该接口接收字符串Prompt，使用AES-256加密后传输给执行服务器
     * 加密密钥和盐值从服务器配置中获取，支持安全的数据传输
     * 
     * @param plainPrompt 明文字符串Prompt，必需参数
     * @return ResponseEntity<Map<String, Object>> 包含加密传输结果的响应实体
     *         成功时返回：
     *         - status: 状态，固定为"ENCRYPTED_AND_SENT"
     *         - encryptedData: Base64编码的加密数据
     *         - timestamp: 加密时间戳
     *         失败时返回：
     *         - error: 错误信息
     * 
     *         处理流程：
     *         1. 从数据库获取AES加密密钥和盐值配置
     *         2. 使用AESEncryptionUtil对Prompt进行加密
     *         3. 将加密后的数据传输给执行服务器
     *         4. 返回加密和传输结果
     */
    @PostMapping("/encryptAndSendPrompt")
    public ResponseEntity<Map<String, Object>> encryptAndSendPrompt(
            @RequestBody Map<String, String> request) {
        try {
            String plainPrompt = request.get("prompt");

            if (plainPrompt == null || plainPrompt.trim().isEmpty()) {
                logger.error("Prompt参数不能为空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Prompt参数不能为空"));
            }

            logger.info("开始加密Prompt，内容长度: {} 字符", plainPrompt.length());

            // 从数据库获取AES加密配置
            ServerConfiguration encryptionKeyConfig = serverConfigService.getConfigByName("AES_ENCRYPTION_KEY");
            ServerConfiguration encryptionSaltConfig = serverConfigService.getConfigByName("AES_ENCRYPTION_SALT");

            if (encryptionKeyConfig == null || encryptionSaltConfig == null) {
                logger.error("AES加密配置未找到，请检查数据库配置");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "AES加密配置未找到，请检查数据库配置"));
            }

            String encryptionKey = encryptionKeyConfig.getConfigData();
            String encryptionSalt = encryptionSaltConfig.getConfigData();

            if (encryptionKey == null || encryptionKey.trim().isEmpty() ||
                    encryptionSalt == null || encryptionSalt.trim().isEmpty()) {
                logger.error("AES加密密钥或盐值为空");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "AES加密密钥或盐值为空"));
            }

            // 使用AES加密工具进行加密
            String encryptedData = AESEncryptionUtil.encrypt(plainPrompt, encryptionKey, encryptionSalt);

            logger.info("Prompt加密成功，加密后数据长度: {} 字符", encryptedData.length());

            // 构建传输到执行服务器的请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> transmissionRequest = new HashMap<>();
            transmissionRequest.put("encryptedPrompt", encryptedData);
            transmissionRequest.put("encryptionType", "AES-256-CBC");
            transmissionRequest.put("timestamp", String.valueOf(System.currentTimeMillis()));

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(transmissionRequest, headers);

            // 构建执行服务器URL，优先使用从数据库读取的IP地址
            String executionServerIp = serverConfigService.getDecryptionServerIp();
            String executionServerUrl;

            if (executionServerIp != null && !executionServerIp.isEmpty()) {
                // 使用从数据库读取的IP地址
                executionServerUrl = "http://" + executionServerIp + ":8082/api/execute/encrypted-prompt";
            } else {
                // 回退到默认的localhost地址
                executionServerUrl = apiBaseUrl.replace("8081", "8082") + "/api/execute/encrypted-prompt";
                logger.warn("未找到数据库中的执行服务器IP配置，使用默认地址: {}", executionServerUrl);
            }

            logger.info("准备发送加密数据到执行服务器: {}", executionServerUrl);

            ResponseEntity<String> response = restTemplate.exchange(
                    executionServerUrl,
                    HttpMethod.POST,
                    entity,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("加密Prompt成功发送到执行服务器");

                // 解析执行服务器的响应，提取解密用时
                Double decryptTimeMs = null;
                String decryptedData = null;
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> serverResponseMap = objectMapper.readValue(response.getBody(), Map.class);

                    // 记录执行服务器的完整响应内容，以便调试
                    logger.info("执行服务器响应内容: {}", response.getBody());

                    // 提取解密用时
                    Object decryptTimeObj = serverResponseMap.get("decryptTimeMs");
                    if (decryptTimeObj instanceof Number) {
                        decryptTimeMs = ((Number) decryptTimeObj).doubleValue();
                    } else if (decryptTimeObj != null) {
                        logger.warn("decryptTimeMs字段类型不正确: {}", decryptTimeObj.getClass().getName());
                    } else {
                        logger.warn("执行服务器响应中未找到decryptTimeMs字段");
                    }

                    // 提取解密后的数据
                    Object decryptedDataObj = serverResponseMap.get("decryptedData");
                    if (decryptedDataObj instanceof String) {
                        decryptedData = (String) decryptedDataObj;
                    } else if (decryptedDataObj != null) {
                        logger.warn("decryptedData字段类型不正确: {}", decryptedDataObj.getClass().getName());
                    } else {
                        logger.warn("执行服务器响应中未找到decryptedData字段");
                    }
                } catch (Exception e) {
                    logger.warn("解析执行服务器响应失败: {}", e.getMessage(), e);
                }

                // 构建返回给前端的响应
                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("status", "ENCRYPTED_AND_SENT");
                responseBody.put("encryptedData", encryptedData);
                responseBody.put("timestamp", System.currentTimeMillis());
                responseBody.put("serverResponse", response.getBody());

                // 如果成功提取了解密用时，则添加到响应中
                if (decryptTimeMs != null) {
                    responseBody.put("decryptTimeMs", decryptTimeMs);
                    logger.info("成功提取解密用时: {} ms", decryptTimeMs);
                } else {
                    logger.warn("未能提取解密用时");
                }

                // 如果成功提取了解密后的数据，则添加到响应中
                if (decryptedData != null) {
                    responseBody.put("decryptedData", decryptedData);
                    logger.info("成功提取解密后的数据，长度: {} 字符", decryptedData.length());
                } else {
                    logger.warn("未能提取解密后的数据");
                }

                return ResponseEntity.ok(responseBody);
            } else {
                logger.warn("执行服务器返回非成功状态: {}", response.getStatusCode());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "执行服务器处理失败: " + response.getStatusCode()));
            }

        } catch (Exception e) {
            logger.error("加密并传输Prompt失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "加密并传输Prompt失败: " + e.getMessage()));
        }
    }

    /**
     * 解密加密的Prompt字符串
     * 
     * 该接口接收加密的字符串，使用AES-256解密后返回明文
     * 解密密钥和盐值从服务器配置中获取，支持安全的数据解密
     * 
     * @param request 包含加密字符串的请求体，必需包含encryptedPrompt字段
     * @return ResponseEntity<Map<String, Object>> 包含解密结果的响应实体
     *         成功时返回：
     *         - status: 状态，固定为"DECRYPTED"
     *         - decryptedData: 解密后的明文字符串
     *         - timestamp: 解密时间戳
     *         失败时返回：
     *         - error: 错误信息
     * 
     * @throws Exception 解密过程中可能抛出异常
     * 
     *                   处理流程：
     *                   1. 验证加密字符串参数不能为空
     *                   2. 从数据库获取AES加密密钥和盐值配置
     *                   3. 使用AESEncryptionUtil对加密字符串进行解密
     *                   4. 返回解密后的明文或相应的错误信息
     * 
     * @example
     *          // 请求示例
     *          {
     *          "encryptedPrompt":
     *          "U2FsdGVkX1+3v4J7K2Z8q1w3Xy7aBcDeFgHiJkLmNoPqRsTuVwXyZ0123456789ABCDEF=="
     *          }
     * 
     *          // 成功响应示例
     *          {
     *          "status": "DECRYPTED",
     *          "decryptedData": "这是一个需要解密的Prompt内容",
     *          "timestamp": 1725852345678
     *          }
     * 
     *          // 错误响应示例
     *          {
     *          "error": "加密Prompt参数不能为空"
     *          }
     */
    @PostMapping("/decryptPrompt")
    public ResponseEntity<Map<String, Object>> decryptPrompt(
            @RequestBody Map<String, String> request) {
        try {
            String encryptedPrompt = request.get("encryptedPrompt");

            if (encryptedPrompt == null || encryptedPrompt.trim().isEmpty()) {
                logger.error("加密Prompt参数不能为空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "加密Prompt参数不能为空"));
            }

            logger.info("开始解密Prompt，加密数据长度: {} 字符", encryptedPrompt.length());

            // 从数据库获取AES加密配置
            ServerConfiguration encryptionKeyConfig = serverConfigService.getConfigByName("AES_ENCRYPTION_KEY");
            ServerConfiguration encryptionSaltConfig = serverConfigService.getConfigByName("AES_ENCRYPTION_SALT");

            if (encryptionKeyConfig == null || encryptionSaltConfig == null) {
                logger.error("AES加密配置未找到，请检查数据库配置");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "AES加密配置未找到，请检查数据库配置"));
            }

            String encryptionKey = encryptionKeyConfig.getConfigData();
            String encryptionSalt = encryptionSaltConfig.getConfigData();

            if (encryptionKey == null || encryptionKey.trim().isEmpty() ||
                    encryptionSalt == null || encryptionSalt.trim().isEmpty()) {
                logger.error("AES加密密钥或盐值为空");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "AES加密密钥或盐值为空"));
            }

            // 使用AES解密工具进行解密
            String decryptedData = AESEncryptionUtil.decrypt(encryptedPrompt, encryptionKey, encryptionSalt);

            logger.info("Prompt解密成功，解密后数据长度: {} 字符", decryptedData.length());

            return ResponseEntity.ok(Map.of(
                    "status", "DECRYPTED",
                    "decryptedData", decryptedData,
                    "timestamp", System.currentTimeMillis()));

        } catch (Exception e) {
            logger.error("解密Prompt失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "解密Prompt失败: " + e.getMessage()));
        }
    }
}
