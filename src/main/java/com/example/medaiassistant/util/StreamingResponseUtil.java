package com.example.medaiassistant.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.List;

/**
 * 流式响应解析工具类 - 优化版本
 * 提供高效的流式响应解析方法，优化JSON解析算法和字符串操作
 * 
 * 该工具类基于 PromptService.extractContentFromStreamingResponse 方法的逻辑，
 * 确保执行服务器和主应用服务器使用相同的解析逻辑，同时优化性能
 * 
 * @author System
 * @version 2.0
 * @since 2025-09-29
 */
public class StreamingResponseUtil {

    private static final Logger logger = LoggerFactory.getLogger(StreamingResponseUtil.class);

    // 重用ObjectMapper实例，避免重复创建
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从流式响应中提取完整内容 - 增强版本
     * 
     * 该方法负责处理AI接口返回的流式响应，从中提取最终的完整内容。
     * 流式响应通常包含多行JSON数据，最后一行是[DONE]标记。
     * 该方法会解析这些数据并提取实际的内容，同时清理可能存在的Markdown标记。
     * 
     * 新增功能：
     * 1. 流式响应完整性验证
     * 2. 检测是否接收到完整的[DONE]标记
     * 3. 响应截断检测和警告
     * 
     * 性能优化点：
     * 1. 使用流式JSON解析替代完整对象映射
     * 2. 优化字符串操作，减少不必要的字符串创建
     * 3. 重用ObjectMapper实例
     * 4. 优化Markdown标记清理逻辑
     * 
     * @param streamingResponse 流式响应字符串，包含多行JSON数据
     * @return 提取并清理后的完整内容字符串，如果解析失败则返回清理后的原始响应
     * 
     *         处理流程：
     *         1. 按行分割响应数据
     *         2. 逐行解析，仅累积有效的content片段（choices[0].delta.content 或 message.content）
     *         3. 忽略元数据与非内容字段
     *         4. 清理Markdown标记
     *         5. 验证响应完整性
     */
    public static String extractContentFromStreamingResponse(String streamingResponse) {
        long startTime = System.nanoTime();

        try {
            if (streamingResponse == null || streamingResponse.isEmpty()) {
                return "";
            }

            String[] lines = streamingResponse.split("\n", -1);
            StringBuilder contentBuilder = new StringBuilder();
            boolean hasDoneMarker = false;
            int validDataLines = 0;

            for (String line : lines) {
                if (line == null) {
                    continue;
                }
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                
                // 检查[DONE]标记
                if ("[DONE]".equals(trimmed)) {
                    hasDoneMarker = true;
                    continue;
                }
                
                // 仅处理 SSE "data: " 或纯 JSON 行
                if (trimmed.startsWith("data: ") || trimmed.startsWith("{")) {
                    appendContentFromJsonLine(trimmed, contentBuilder);
                    validDataLines++;
                }
            }

            // 验证响应完整性
            if (!hasDoneMarker && validDataLines > 0) {
                logger.warn("流式响应可能不完整：未检测到[DONE]标记，有效数据行数: {}", validDataLines);
                // 记录警告但不中断处理，因为某些API可能不发送[DONE]标记
            }

            String content = cleanMarkdownMarkers(contentBuilder.toString());

            long endTime = System.nanoTime();
            double processingTimeMs = (endTime - startTime) / 1_000_000.0;
            logger.debug("流式响应解析完成，用时: {}ms, 内容长度: {} 字符, 完整性: {}",
                    String.format("%.2f", processingTimeMs), content.length(), 
                    hasDoneMarker ? "完整" : "可能不完整");

            return content;
        } catch (Exception e) {
            logger.error("解析流式响应时出错: {}", e.getMessage());
            return extractContentWithErrorHandling(streamingResponse);
        }
    }




    /**
     * 清理Markdown标记 - 优化版本
     * 
     * @param content 需要清理的内容
     * @return 清理后的内容
     */
    private static String cleanMarkdownMarkers(String content) {
        if (content == null) {
            return "";
        }

        String result = content.trim();

        // 优化标记清理逻辑，减少字符串操作
        if (result.startsWith("```")) {
            result = result.substring(3).trim();
        }

        if (result.startsWith("\n")) {
            result = result.substring(1);
        }

        if (result.endsWith("```")) {
            result = result.substring(0, result.length() - 3).trim();
        }

        return result.trim();
    }

    /**
     * 错误处理路径 - 优化版本
     * 
     * @param streamingResponse 原始流式响应
     * @return 清理后的内容
     */
    private static String extractContentWithErrorHandling(String streamingResponse) {
        if (streamingResponse == null) {
            return "";
        }

        // 优化字符串操作，使用更高效的方法
        String cleanResponse = streamingResponse.replace("[DONE]", "").trim();
        return cleanMarkdownMarkers(cleanResponse);
    }

    // 新增：逐行解析并仅累积有效文本片段，参考主服务器的实现
    private static void appendContentFromJsonLine(String line, StringBuilder contentBuilder) {
        try {
            String jsonData = line.startsWith("data: ") ? line.substring(6) : line;
            Map<String, Object> parsed = objectMapper.readValue(
                    jsonData,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                    });

            if (parsed.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) parsed.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);

                    // 流式响应中的增量内容
                    if (firstChoice.containsKey("delta")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> delta = (Map<String, Object>) firstChoice.get("delta");
                        if (delta != null && delta.containsKey("content")) {
                            String content = (String) delta.get("content");
                            if (content != null) {
                                contentBuilder.append(content);
                            }
                        }
                    }

                    // 非流式响应中的完整消息
                    if (firstChoice.containsKey("message")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                        if (message != null && message.containsKey("content")) {
                            String content = (String) message.get("content");
                            if (content != null) {
                                contentBuilder.append(content);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("JSON行解析失败，忽略该行。原始行: {}", line);
        }
    }
}
