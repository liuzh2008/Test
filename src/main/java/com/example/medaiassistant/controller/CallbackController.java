package com.example.medaiassistant.controller;

import com.example.medaiassistant.dto.CallbackData;
import com.example.medaiassistant.util.AESEncryptionUtil;
import com.example.medaiassistant.service.ServerConfigService;
import com.example.medaiassistant.model.PromptResult;
import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.repository.PromptResultRepository;
import com.example.medaiassistant.repository.PromptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 回调控制器 - 异步回调处理核心组件
 * 
 * 负责接收来自执行服务器(8082端口)的异步回调请求，处理各种状态的回调数据，
 * 包括成功、失败、处理中、重试中和等待中状态。主要功能包括：
 * 
 * 1. 接收并验证回调数据的完整性
 * 2. 根据回调状态调用相应的处理方法
 * 3. 对加密的成功回调结果进行AES解密
 * 4. 返回结构化的响应数据给前端
 * 5. 提供完整的错误处理和日志记录
 * 
 * 关键特性：
 * - 支持标准的CallbackData数据结构
 * - 自动AES解密加密的回调结果
 * - 统一的JSON响应格式
 * - 完善的错误处理和状态管理
 * - 与ServerConfigService集成获取加密配置
 * 
 * 使用示例：
 * 执行服务器在处理完成后会发送POST请求到 /api/callback/receive 端点，
 * 包含加密的处理结果，本控制器负责解密并返回给前端显示。
 * 
 * @since 2025-09-12
 * @author Cline
 * @version 1.1
 * @see CallbackData
 * @see CallbackStatus
 * @see ServerConfigService
 * @see AESEncryptionUtil
 */
@RestController
@RequestMapping("/api/callback")
public class CallbackController {

    private static final Logger logger = LoggerFactory.getLogger(CallbackController.class);

    @Autowired
    private ServerConfigService serverConfigService;

    @Autowired
    private PromptResultRepository promptResultRepository;

    @Autowired
    private PromptRepository promptRepository;

    /**
     * 接收异步回调请求端点
     * 
     * 主要功能：
     * - 验证回调数据的完整性和有效性
     * - 根据回调状态分发到相应的处理方法
     * - 构建结构化的JSON响应
     * - 处理各种异常情况并返回适当的错误信息
     * 
     * 请求体格式：
     * {
     *   "dataId": "唯一数据标识",
     *   "status": "SUCCESS|FAILED|PROCESSING|RETRYING|PENDING",
     *   "result": "加密的处理结果(成功时)",
     *   "errorMessage": "错误信息(失败时)",
     *   "timestamp": "时间戳",
     *   "retryCount": 重试次数
     * }
     * 
     * 响应格式：
     * - 成功: HTTP 200 with JSON response
     * - 参数错误: HTTP 400 with error message
     * - 服务器错误: HTTP 500 with error message
     * 
     * @param callbackData 回调数据对象，包含处理状态、结果等信息
     * @return ResponseEntity包含处理结果的状态和数据
     * @throws Exception 处理过程中可能出现的各种异常
     * 
     * @see CallbackData
     * @see CallbackStatus
     */
    @PostMapping("/receive")
    public ResponseEntity<Map<String, Object>> receiveCallback(@RequestBody CallbackData callbackData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("接收到异步回调: {}", callbackData);

            // 验证回调数据
            if (callbackData.getDataId() == null || callbackData.getDataId().isEmpty()) {
                logger.warn("回调数据缺少dataId: {}", callbackData);
                response.put("status", "error");
                response.put("message", "缺少dataId");
                return ResponseEntity.badRequest().body(response);
            }

            if (callbackData.getStatus() == null) {
                logger.warn("回调数据缺少status: {}", callbackData);
                response.put("status", "error");
                response.put("message", "缺少status");
                return ResponseEntity.badRequest().body(response);
            }

            // 设置基本响应信息 - 按照集成测试中的标准格式
            response.put("status", "SUCCESS");
            response.put("message", "结果接收成功");
            response.put("dataId", callbackData.getDataId());
            response.put("timestamp", System.currentTimeMillis());
            response.put("callbackStatus", callbackData.getStatus().toString());
            response.put("retryCount", callbackData.getRetryCount());

            // 根据状态处理回调
            switch (callbackData.getStatus()) {
                case SUCCESS:
                    String decryptedResult = handleSuccessCallback(callbackData);
                    if (decryptedResult != null) {
                        response.put("decryptedData", decryptedResult);
                        response.put("encryptedData", callbackData.getResult());
                    }
                    break;
                case FAILED:
                    handleFailedCallback(callbackData);
                    response.put("errorMessage", callbackData.getErrorMessage());
                    break;
                case PROCESSING:
                    handleProcessingCallback(callbackData);
                    break;
                case RETRYING:
                    handleRetryingCallback(callbackData);
                    break;
                case PENDING:
                    handlePendingCallback(callbackData);
                    break;
                default:
                    logger.warn("未知的回调状态: {}", callbackData.getStatus());
                    response.put("status", "error");
                    response.put("message", "未知的状态");
                    return ResponseEntity.badRequest().body(response);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("处理回调异常: {}", callbackData, e);
            response.put("status", "error");
            response.put("message", "处理回调异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 处理成功状态的回调数据
     * 
     * 主要功能：
     * - 从回调数据中提取加密的结果
     * - 从ServerConfigService获取AES加密配置
     * - 使用AESEncryptionUtil解密加密的结果
     * - 从dataId中去掉"cdwyy"作为Promptid
     * - 验证解密结果是否为LLM分析错误信息
     * - 避免重复保存相同数据到PromptResult表
     * - 将解密结果保存到PromptResult表中
     * - 更新Prompt表中的相应信息
     * - 记录解密过程和结果
     * - 返回解密后的原始数据
     * 
     * 处理流程：
     * 1. 检查加密结果是否为空
     * 2. 获取AES加密密钥和盐值配置
     * 3. 验证加密配置是否已设置
     * 4. 执行AES解密操作
     * 5. 从dataId中提取Promptid（去掉"cdwyy"前缀）
     * 6. 验证解密结果是否为LLM分析错误信息
     * 7. 检查是否已存在相同Promptid的结果记录
     * 8. 创建并保存PromptResult记录（如果不存在且不是错误信息）
     * 9. 更新Prompt表中的执行状态和结果信息
     * 10. 记录解密日志
     * 11. 返回解密结果
     * 
     * 注意事项：
     * - 保持解密数据的完整性
     * - 包含完整的错误处理
     * - 确保Promptid提取的正确性
     * - 避免数据重复保存
     * - 过滤LLM分析错误信息
     * 
     * @param callbackData 包含加密结果的成功回调数据
     * @return 解密后的明文字符串，如果解密失败返回null
     * @throws Exception 解密过程中可能出现的异常
     * 
     * @see ServerConfigService#getAesEncryptionKey()
     * @see ServerConfigService#getAesEncryptionSalt()
     * @see AESEncryptionUtil#decrypt(String, String, String)
     */
    private String handleSuccessCallback(CallbackData callbackData) {
        logger.info("处理成功回调, dataId: {}, 结果: {}", callbackData.getDataId(), callbackData.getResult());
        
        try {
            String encryptedResult = callbackData.getResult();
            if (encryptedResult != null && !encryptedResult.trim().isEmpty()) {
                // 获取AES加密密钥和盐值
                String encryptionKey = serverConfigService.getAesEncryptionKey();
                String encryptionSalt = serverConfigService.getAesEncryptionSalt();
                
                if (encryptionKey == null || encryptionSalt == null) {
                    logger.error("AES加密配置未设置，无法解密数据，数据ID: {}", callbackData.getDataId());
                    return null;
                }
                
                // 解密数据
                String decryptedResult = AESEncryptionUtil.decrypt(encryptedResult, encryptionKey, encryptionSalt);
                
                // 记录解密后的数据
                logger.info("数据ID: {} 的解密后内容: {}",
                        callbackData.getDataId(), decryptedResult);
                
                // 从dataId中提取Promptid（去掉"cdwyy"前缀）
                Integer promptId = extractPromptIdFromDataId(callbackData.getDataId());
                if (promptId != null) {
                    // 验证是否为LLM分析错误信息
                    if (isLLMErrorResult(decryptedResult)) {
                        logger.warn("LLM分析错误信息，不保存到数据库，PromptId: {}, 数据ID: {}", promptId, callbackData.getDataId());
                    } else {
                        // 检查是否已存在相同Promptid和内容的结果记录
                        if (!hasExistingPromptResult(promptId, decryptedResult)) {
                            // 保存到PromptResult表
                            PromptResult savedResult = saveToPromptResultTable(promptId, decryptedResult);
                            if (savedResult != null) {
                                // 更新Prompt表中的相应信息
                                updatePromptTable(promptId, savedResult.getResultId(), decryptedResult);
                                logger.info("成功保存Prompt结果到数据库，PromptId: {}, 数据ID: {}", promptId, callbackData.getDataId());
                            }
                        } else {
                            logger.info("已存在相同Promptid和内容的结果记录，跳过保存，PromptId: {}", promptId);
                        }
                    }
                } else {
                    logger.warn("无法从dataId中提取有效的PromptId，数据ID: {}", callbackData.getDataId());
                }
                
                // 这里可以添加前端显示逻辑，将解密后的数据发送给前端
                // 例如：通过WebSocket或消息队列将decryptedResult发送给前端显示
                logger.info("解密后的数据已准备好显示，数据ID: {}", callbackData.getDataId());
                
                // 返回解密后的原始数据，不进行任何修改
                return decryptedResult;
            }
        } catch (Exception e) {
            logger.error("处理成功回调时发生异常, dataId: {}", callbackData.getDataId(), e);
        }
        
        return null;
    }

    /**
     * 处理失败状态的回调数据
     * 
     * 主要功能：
     * - 记录失败回调的详细信息
     * - 记录错误信息和相关数据ID
     * - 为后续的错误处理和重试机制提供基础
     * 
     * 当前状态：待实现完整的失败处理逻辑
     * 计划功能：
     * - 错误信息持久化存储
     * - 失败重试机制
     * - 错误通知和告警
     * - 失败数据分析
     * 
     * @param callbackData 包含错误信息的失败回调数据
     * 
     * @see CallbackData#getErrorMessage()
     * @see CallbackData#getDataId()
     */
    private void handleFailedCallback(CallbackData callbackData) {
        logger.warn("处理失败回调, dataId: {}, 错误信息: {}", callbackData.getDataId(), callbackData.getErrorMessage());
        // 失败回调处理逻辑待实现
    }

    /**
     * 处理处理中状态的回调数据
     * 
     * 主要功能：
     * - 记录处理中状态的回调信息
     * - 为后续的状态跟踪和监控提供基础
     * 
     * 当前状态：待实现完整的处理中状态逻辑
     * 计划功能：
     * - 处理进度跟踪
     * - 状态持久化存储
     * - 超时监控和处理
     * - 进度通知机制
     * 
     * @param callbackData 处理中状态的回调数据
     * 
     * @see CallbackData#getDataId()
     */
    private void handleProcessingCallback(CallbackData callbackData) {
        logger.info("处理处理中回调, dataId: {}", callbackData.getDataId());
        // 处理中状态更新逻辑待实现
    }

    /**
     * 处理重试中状态的回调数据
     * 
     * 主要功能：
     * - 记录重试中状态的回调信息
     * - 记录当前重试次数
     * - 为重试机制提供监控基础
     * 
     * 当前状态：待实现完整的重试处理逻辑
     * 计划功能：
     * - 重试策略管理
     * - 重试间隔控制
     * - 最大重试次数限制
     * - 重试失败处理
     * 
     * @param callbackData 重试中状态的回调数据
     * 
     * @see CallbackData#getDataId()
     * @see CallbackData#getRetryCount()
     */
    private void handleRetryingCallback(CallbackData callbackData) {
        logger.info("处理重试中回调, dataId: {}, 重试次数: {}", callbackData.getDataId(), callbackData.getRetryCount());
        // 重试状态更新逻辑待实现
    }

    /**
     * 处理等待中状态的回调数据
     * 
     * 主要功能：
     * - 记录等待中状态的回调信息
     * - 为任务调度和队列管理提供基础
     * 
     * 当前状态：待实现完整的等待状态逻辑
     * 计划功能：
     * - 任务队列管理
     * - 优先级调度
     * - 超时处理
     * - 资源分配优化
     * 
     * @param callbackData 等待中状态的回调数据
     * 
     * @see CallbackData#getDataId()
     */
    private void handlePendingCallback(CallbackData callbackData) {
        logger.info("处理等待中回调, dataId: {}", callbackData.getDataId());
        // 等待状态更新逻辑待实现
    }

    /**
     * 从dataId中提取Promptid（去掉"cdwyy"前缀）
     * 
     * 处理逻辑：
     * 1. 检查dataId是否以"cdwyy"开头
     * 2. 如果是，去掉"cdwyy"前缀
     * 3. 将剩余部分转换为整数作为Promptid
     * 4. 如果转换失败或格式不正确，返回null
     * 
     * @param dataId 数据ID，格式为"cdwyy" + Promptid
     * @return 提取的Promptid，如果提取失败返回null
     */
    private Integer extractPromptIdFromDataId(String dataId) {
        try {
            if (dataId != null && dataId.startsWith("cdwyy")) {
                String promptIdStr = dataId.substring(5); // 去掉"cdwyy"前缀
                return Integer.parseInt(promptIdStr);
            }
        } catch (NumberFormatException e) {
            logger.warn("dataId格式不正确，无法提取Promptid: {}", dataId, e);
        } catch (Exception e) {
            logger.error("提取Promptid时发生异常，dataId: {}", dataId, e);
        }
        return null;
    }

    /**
     * 检查是否为LLM分析错误信息
     * 
     * 判断逻辑：
     * - 检查解密结果是否为空或空白
     * - 解析JSON格式的回调数据
     * - 检查error字段是否为null（如果error不为null，则表示分析失败）
     * - 如果无法解析JSON，则使用备用检查逻辑
     * 
     * @param decryptedResult 解密后的结果内容（JSON格式）
     * @return 如果是LLM分析错误信息返回true，否则返回false
     */
    private boolean isLLMErrorResult(String decryptedResult) {
        if (decryptedResult == null || decryptedResult.trim().isEmpty()) {
            return true;
        }
        
        String trimmedResult = decryptedResult.trim();
        
        try {
            // 尝试解析JSON格式的回调数据
            // 预期格式: {"reasoning_content": "AI推理过程", "content": "AI生成内容", "error": null}
            if (trimmedResult.startsWith("{") && trimmedResult.endsWith("}")) {
                // 简单的JSON解析来检查error字段
                if (trimmedResult.contains("\"error\":null") || 
                    trimmedResult.contains("\"error\": null") ||
                    !trimmedResult.contains("\"error\":")) {
                    // error为null或不存在error字段，表示分析成功
                    return false;
                } else {
                    // error字段存在且不为null，表示分析失败
                    return true;
                }
            }
        } catch (Exception e) {
            logger.warn("解析回调数据JSON失败，使用备用检查逻辑: {}", e.getMessage());
        }
        
        // 备用检查逻辑：如果不是JSON格式或解析失败，使用原来的检查方法
        // 检查结果长度是否过短（可能表示分析失败）
        if (trimmedResult.length() < 10) {
            return true;
        }
        
        // 检查明确的错误关键词
        String[] exactErrorPatterns = {
            "分析失败", "无法分析", "处理失败", "无法处理", 
            "analysis failed", "cannot analyze", "processing failed",
            "抱歉，", "对不起，", "请重试", "retry later"
        };
        
        String lowerResult = trimmedResult.toLowerCase();
        for (String pattern : exactErrorPatterns) {
            if (lowerResult.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        
        // 检查是否为简单的错误消息格式
        if (trimmedResult.length() < 50 && 
            (lowerResult.contains("错误") || lowerResult.contains("error") || 
             lowerResult.contains("失败") || lowerResult.contains("failed"))) {
            return true;
        }
        
        return false;
    }

    /**
     * 检查是否已存在相同Promptid和内容的结果记录
     * 
     * @param promptId Promptid
     * @param decryptedResult 解密后的结果内容
     * @return 如果已存在相同Promptid和内容的结果记录返回true，否则返回false
     */
    private boolean hasExistingPromptResult(Integer promptId, String decryptedResult) {
        try {
            List<PromptResult> existingResults = promptResultRepository.findByPromptId(promptId);
            if (existingResults != null && !existingResults.isEmpty()) {
                // 检查是否存在内容相同的记录
                for (PromptResult result : existingResults) {
                    if (result.getOriginalResultContent() != null && 
                        result.getOriginalResultContent().equals(decryptedResult)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            logger.error("检查已存在Prompt结果时发生异常，PromptId: {}", promptId, e);
            return false;
        }
    }

    /**
     * 保存解密结果到PromptResult表
     * 
     * 主要功能：
     * - 创建PromptResult对象
     * - 设置Promptid和解密后的内容
     * - 设置状态为"SUCCESS"
     * - 设置执行时间和创建时间
     * - 保存到数据库
     * - 处理重复数据插入异常
     * 
     * 重复数据处理逻辑：
     * 1. 捕获数据库重复键异常（ORA-00001, MySQL 1062）
     * 2. 当检测到重复异常时，查询已存在的记录
     * 3. 返回已存在的记录，避免重复保存
     * 4. 记录详细的日志信息用于问题追踪
     * 
     * @param promptId Promptid，从dataId中提取的唯一标识
     * @param decryptedResult 解密后的结果内容，JSON格式字符串
     * @return 保存的PromptResult对象，如果保存失败或重复则返回已存在的记录或null
     * @throws Exception 数据库操作异常或其他运行时异常
     * @since 2025-09-16
     * @version 1.2
     * @see PromptResult
     * @see PromptResultRepository
     */
    private PromptResult saveToPromptResultTable(Integer promptId, String decryptedResult) {
        try {
            PromptResult promptResult = new PromptResult();
            promptResult.setPromptId(promptId);
            promptResult.setOriginalResultContent(decryptedResult);
            promptResult.setStatus("SUCCESS");
            promptResult.setExecutionTime(LocalDateTime.now());
            promptResult.setCreatedAt(LocalDateTime.now());
            promptResult.setUpdatedAt(LocalDateTime.now());
            promptResult.setIsRead(0); // 默认未读
            promptResult.setDeleted(0); // 默认未删除
            
            PromptResult savedResult = promptResultRepository.save(promptResult);
            logger.info("Prompt结果保存成功，PromptId: {}, ResultId: {}", promptId, savedResult.getResultId());
            return savedResult;
        } catch (Exception e) {
            // 检查是否为重复键异常（Oracle: ORA-00001, MySQL: 1062）
            String message = e.getMessage().toLowerCase();
            if (message.contains("ora-00001") || message.contains("duplicate") || message.contains("1062")) {
                logger.warn("检测到重复数据，跳过保存，PromptId: {}, 异常信息: {}", promptId, e.getMessage());
                // 尝试查询已存在的记录
                try {
                    List<PromptResult> existingResults = promptResultRepository.findByPromptId(promptId);
                    if (existingResults != null && !existingResults.isEmpty()) {
                        for (PromptResult result : existingResults) {
                            if (result.getOriginalResultContent() != null && 
                                result.getOriginalResultContent().equals(decryptedResult)) {
                                logger.info("返回已存在的Prompt结果，PromptId: {}, ResultId: {}", promptId, result.getResultId());
                                return result;
                            }
                        }
                    }
                } catch (Exception ex) {
                    logger.error("查询已存在的Prompt结果时发生异常，PromptId: {}", promptId, ex);
                }
            } else {
                logger.error("保存Prompt结果到数据库时发生异常，PromptId: {}", promptId, e);
            }
            return null;
        }
    }

    /**
     * 更新Prompt表中的相应信息
     * 
     * 主要更新内容：
     * - 设置执行时间为当前时间
     * - 设置结果ID为保存的PromptResult的ID
     * - 设置执行结果为解密后的内容
     * - 更新状态为"已完成"
     * 
     * @param promptId Promptid
     * @param resultId 保存的PromptResult的ID
     * @param decryptedResult 解密后的结果内容
     */
    private void updatePromptTable(Integer promptId, Integer resultId, String decryptedResult) {
        try {
            Optional<Prompt> promptOptional = promptRepository.findById(promptId);
            if (promptOptional.isPresent()) {
                Prompt prompt = promptOptional.get();
                prompt.setExecutionTime(LocalDateTime.now());
                prompt.setResultId(resultId);
                prompt.setExecutionResult(decryptedResult);
                prompt.setStatusName("已完成");
                
                promptRepository.save(prompt);
                logger.info("Prompt表更新成功，PromptId: {}, ResultId: {}", promptId, resultId);
            } else {
                logger.warn("未找到对应的Prompt记录，PromptId: {}", promptId);
            }
        } catch (Exception e) {
            logger.error("更新Prompt表时发生异常，PromptId: {}", promptId, e);
        }
    }

    /**
     * 健康检查接口
     * 
     * 提供回调服务的健康状态检查，用于监控和运维目的。
     * 验证服务是否正常运行，能够接收和处理回调请求。
     * 
     * 响应格式：纯文本健康状态信息
     * 
     * @return ResponseEntity包含健康状态字符串
     * 
     * @see ResponseEntity
     */
    @PostMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Callback service is healthy");
    }
}
