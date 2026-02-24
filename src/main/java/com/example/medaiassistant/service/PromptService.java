package com.example.medaiassistant.service;

import com.example.medaiassistant.dto.AIRequest;
import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.model.PromptResult;
import com.example.medaiassistant.repository.PromptRepository;
import com.example.medaiassistant.repository.PromptResultRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Prompt服务类，负责处理Prompt的自动执行和多线程管理
 * 
 * 该服务类提供了以下主要功能：
 * 1. 自动执行待处理的Prompt任务
 * 2. 多线程管理，支持并发执行多个Prompt任务
 * 3. 重试机制，确保Prompt任务的可靠执行
 * 4. 结果保存，将执行结果持久化到数据库
 */
@Service
public class PromptService {
    /**
     * Prompt数据访问层接口
     */
    private final PromptRepository promptRepository;

    /**
     * Prompt结果数据访问层接口
     */
    private final PromptResultRepository promptResultRepository;

    /**
     * REST调用模板，用于调用AI分析接口
     */
    private final RestTemplate restTemplate;

    @Value("${api.base.url:http://localhost:8081}")
    private String apiBaseUrl;

    /**
     * 标识自动执行任务是否正在进行中的原子布尔值
     */
    private final AtomicBoolean isAutoExecuting = new AtomicBoolean(false);

    /**
     * 线程池执行器，用于管理并发执行的线程
     */
    private ExecutorService executorService;

    /**
     * 线程局部变量，用于存储当前线程正在执行的Prompt ID
     */
    private final ThreadLocal<Integer> currentPromptId = new ThreadLocal<>();

    /**
     * AI接口路径常量
     */
    private static final String AI_API_PATH = "/api/ai/response";

    /**
     * 默认AI模型名称常量
     */
    private static final String DEFAULT_AI_MODEL = "deepseek-chat";

    /**
     * 构造函数，初始化PromptService
     * 
     * @param promptRepository       Prompt仓库接口，用于访问Prompt数据
     * @param promptResultRepository PromptResult仓库接口，用于访问Prompt结果数据
     * @param restTemplate           REST调用模板，用于调用AI分析接口
     * 
     *                               超时设置问题的根本原因及解决方案：
     *                               问题：在RestTemplateConfig.java中使用的是SimpleClientHttpRequestFactory，
     *                               但在之前的实现中，代码试图将其转换为HttpComponentsClientHttpRequestFactory，
     *                               导致超时设置无法正确应用，从而在处理较大的Prompt时出现超时提示。
     * 
     *                               解决方案：修改构造函数中的超时设置代码，使其能够正确处理不同类型的ClientHttpRequestFactory：
     *                               1.
     *                               如果是HttpComponentsClientHttpRequestFactory，则设置相应的超时时间
     *                               2. 如果是SimpleClientHttpRequestFactory，也设置相应的超时时间
     * 
     *                               现在无论使用哪种类型的ClientHttpRequestFactory，都能正确设置300秒（5分钟）的连接超时和读取超时，
     *                               与前端JavaScript代码中的超时设置保持一致，解决了处理较大Prompt时的超时问题。
     */
    public PromptService(PromptRepository promptRepository, PromptResultRepository promptResultRepository,
            RestTemplate restTemplate) {
        this.promptRepository = promptRepository;
        this.promptResultRepository = promptResultRepository;
        this.restTemplate = restTemplate;

        // 配置超时设置，支持不同类型的ClientHttpRequestFactory
        var requestFactory = restTemplate.getRequestFactory();
        if (requestFactory instanceof HttpComponentsClientHttpRequestFactory httpFactory) {
            // 使用新的RequestConfig来设置超时，避免使用已弃用的方法
            org.apache.hc.client5.http.config.RequestConfig requestConfig = org.apache.hc.client5.http.config.RequestConfig.custom()
                    .setConnectionRequestTimeout(org.apache.hc.core5.util.Timeout.ofSeconds(300)) // 300s连接请求超时
                    .setResponseTimeout(org.apache.hc.core5.util.Timeout.ofSeconds(300)) // 300s读取超时
                    .build();
            // 使用HttpClient来设置RequestConfig
            org.apache.hc.client5.http.impl.classic.HttpClientBuilder clientBuilder = org.apache.hc.client5.http.impl.classic.HttpClientBuilder.create()
                    .setDefaultRequestConfig(requestConfig);
            httpFactory.setHttpClient(clientBuilder.build());
        } else if (requestFactory instanceof SimpleClientHttpRequestFactory simpleFactory) {
            simpleFactory.setConnectTimeout(300000); // 300s连接超时
            simpleFactory.setReadTimeout(300000); // 300s读取超时
        }
    }

    /**
     * 获取所有待处理的Prompt列表
     * 
     * @return 待处理的Prompt列表
     */
    public List<Prompt> getPendingPrompts() {
        return promptRepository.findByStatusName("待处理");
    }

    /**
     * 分页获取待处理的Prompt列表
     * 
     * @param pageable 分页参数
     * @return 待处理的Prompt分页结果
     */
    public Page<Prompt> getPendingPrompts(Pageable pageable) {
        return promptRepository.findByStatusName("待处理", pageable);
    }

    /**
     * 统计待处理的Prompt数量
     * 
     * @return 待处理的Prompt数量
     */
    public long countPendingPrompts() {
        return promptRepository.countByStatusName("待处理");
    }
    
    /**
     * 统计已提交的Prompt数量
     * 
     * @return 已提交的Prompt数量
     */
    public long countSubmittedPrompts() {
        return promptRepository.countByStatusName("已提交");
    }

    /**
     * 启动自动执行Prompt任务
     * 持续获取未执行的Prompt并进行分析，直到前端调用stopAutoExecution
     * 
     * @param maxThreads 最大线程数
     */
    @Async
    public void startAutoExecution(int maxThreads) {
        if (isAutoExecuting.compareAndSet(false, true)) {
            // 使用有界队列和更精细的线程池控制
            executorService = new ThreadPoolExecutor(
                    maxThreads,
                    maxThreads,
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(100),
                    new ThreadPoolExecutor.CallerRunsPolicy());
            try {
                // 持续执行直到调用stopAutoExecution
                while (isAutoExecuting.get()) {
                    List<Prompt> pendingPrompts = getPendingPrompts();
                    if (!pendingPrompts.isEmpty()) {
                        for (Prompt prompt : pendingPrompts) {
                            // 检查是否已停止执行
                            if (!isAutoExecuting.get()) {
                                break;
                            }

                            // 将Prompt状态更新为"处理中"，避免重复执行
                            Prompt updatedPrompt;
                            try {
                                // 使用entityManager直接更新，避免重复行问题
                                updatedPrompt = promptRepository.findById(prompt.getPromptId()).orElse(null);
                                if (updatedPrompt == null) {
                                    System.err.println("Prompt不存在，ID: " + prompt.getPromptId());
                                    continue;
                                }
                                // 检查状态是否已经是"处理中"，避免重复处理
                                if ("处理中".equals(updatedPrompt.getStatusName())) {
                                    System.out.println("Prompt已在处理中，跳过: " + prompt.getPromptId());
                                    continue;
                                }
                                updatedPrompt.setStatusName("处理中");
                                updatedPrompt = promptRepository.save(updatedPrompt);
                            } catch (Exception e) {
                                // 如果更新失败，可能是因为并发访问，跳过这个Prompt
                                System.err.println("更新Prompt状态失败，可能正在被其他线程处理: " + prompt.getPromptId() + ", 错误: "
                                        + e.getMessage());
                                continue;
                            }

                            // 添加间隔避免突发请求
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                isAutoExecuting.set(false);
                                break;
                            }

                            final Prompt finalPrompt = updatedPrompt;
                            executorService.submit(() -> {
                                try {
                                    currentPromptId.set(finalPrompt.getPromptId());
                                    executePrompt(finalPrompt);
                                } finally {
                                    currentPromptId.remove();
                                }
                            });
                        }
                    } else {
                        // 如果没有待处理的Prompt，等待一段时间再检查
                        try {
                            Thread.sleep(5000); // 等待5秒再检查
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            isAutoExecuting.set(false);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isAutoExecuting.set(false);
                if (executorService != null) {
                    executorService.shutdown();
                    try {
                        if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                            executorService.shutdownNow();
                        }
                    } catch (InterruptedException e) {
                        executorService.shutdownNow();
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    /**
     * 停止自动执行Prompt任务
     */
    public void stopAutoExecution() {
        isAutoExecuting.set(false);
    }

    /**
     * 检查是否正在自动执行
     * 
     * @return true如果正在自动执行，否则false
     */
    public boolean isAutoExecuting() {
        return isAutoExecuting.get();
    }

    /**
     * 获取当前执行的Prompt ID
     * 
     * @return 当前执行的Prompt ID，如果没有则为null
     */
    public Integer getCurrentPromptId() {
        return currentPromptId.get();
    }

    /**
     * 获取Prompt执行结果
     * 
     * @param promptId Prompt ID
     * @return 执行结果内容
     * @throws RuntimeException 如果未找到Prompt结果
     */
    public String getPromptResult(Integer promptId) {
        return promptRepository.findById(promptId)
                .map(Prompt::getExecutionResult)
                .orElseThrow(() -> new RuntimeException("未找到Prompt结果"));
    }

    /**
     * 执行单个Prompt任务
     * 
     * @param prompt 要执行的Prompt对象
     */
    private void executePrompt(Prompt prompt) {
        int maxRetries = 5; // 最大重试次数
        int retryCount = 0;
        LocalDateTime processingStartTime = LocalDateTime.now();
        long startTime = System.currentTimeMillis();

        while (retryCount <= maxRetries) {
            try {
                // 记录开始处理时间
                long processStartTime = System.currentTimeMillis();

                // 实际执行Prompt逻辑
                String analysisResult = processPrompt(
                        prompt.getObjectiveContent(),
                        prompt.getDailyRecords(),
                        prompt.getPromptTemplateContent());

                // 计算处理时间
                long processTime = System.currentTimeMillis() - processStartTime;

                // 如果成功执行，保存成功结果
                LocalDateTime processingEndTime = LocalDateTime.now();
                saveSuccessResult(prompt, analysisResult, retryCount, processingStartTime, processingEndTime);

                // 记录性能日志
                long totalTime = System.currentTimeMillis() - startTime;
                System.out.println("Prompt执行成功 - ID: " + prompt.getPromptId() +
                        ", 处理时间: " + processTime + "ms, 总时间: " + totalTime + "ms, 重试次数: " + retryCount);

                // 跳出循环
                break;
            } catch (Exception e) {
                retryCount++;
                if (retryCount > maxRetries) {
                    // 如果达到最大重试次数，保存失败结果
                    saveFailureResult(prompt, e.getMessage(), retryCount);

                    // 记录失败日志
                    long totalTime = System.currentTimeMillis() - startTime;
                    System.err.println("Prompt执行失败 - ID: " + prompt.getPromptId() +
                            ", 总时间: " + totalTime + "ms, 重试次数: " + retryCount + ", 错误: " + e.getMessage());

                    // 抛出异常
                    throw new RuntimeException("执行Prompt失败，已重试" + maxRetries + "次: " + e.getMessage(), e);
                } else {
                    // 记录重试信息
                    System.out.println("执行Prompt失败，正在进行第" + retryCount + "次重试 - ID: " + prompt.getPromptId() +
                            ", 错误: " + e.getMessage());
                    try {
                        // 等待一段时间再重试，避免过于频繁的重试
                        Thread.sleep(1000 * retryCount); // 递增等待时间
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试过程中被中断", ie);
                    }
                }
            }
        }
    }

    /**
     * 保存成功结果到数据库
     * 
     * @param prompt              Prompt对象
     * @param analysisResult      分析结果
     * @param retryCount          重试次数
     * @param processingStartTime 处理开始时间
     * @param processingEndTime   处理结束时间
     */
    private void saveSuccessResult(Prompt prompt, String analysisResult, int retryCount,
            LocalDateTime processingStartTime, LocalDateTime processingEndTime) {
        try {
            // 先查询最新的Prompt对象，避免重复行问题
            Prompt currentPrompt = promptRepository.findById(prompt.getPromptId()).orElse(prompt);

            // 更新Prompt表中的字段
            currentPrompt.setStatusName("已处理");
            currentPrompt.setExecutionTime(LocalDateTime.now());
            currentPrompt.setRetryCount(retryCount);
            currentPrompt.setProcessingStartTime(processingStartTime);
            currentPrompt.setProcessingEndTime(processingEndTime);
            currentPrompt.setExecutionResult(analysisResult); // 设置执行结果
            // 计算并设置生成耗时
            long costTime = java.time.Duration.between(processingStartTime, processingEndTime).toMillis();
            currentPrompt.setGenerateCostTime(String.valueOf(costTime) + "ms");
            // 保存Prompt
            currentPrompt = promptRepository.save(currentPrompt);

            // 创建并保存PromptResult
            PromptResult promptResult = new PromptResult();
            promptResult.setPromptId(currentPrompt.getPromptId());
            promptResult.setOriginalResultContent(analysisResult);
            promptResult.setStatus("成功");
            promptResult.setExecutionTime(LocalDateTime.now());
            promptResult.setCreatedAt(LocalDateTime.now());
            promptResult.setUpdatedAt(LocalDateTime.now());
            promptResult.setLastModifiedBy(0); // 默认值，可以根据需要修改
            promptResult.setIsRead(0); // 默认未读
            promptResult.setDeleted(0); // 默认未删除
            PromptResult savedResult = promptResultRepository.save(promptResult);

            // 更新Prompt的ResultId字段
            currentPrompt.setResultId(savedResult.getResultId());
            promptRepository.save(currentPrompt);
        } catch (Exception e) {
            System.err.println("保存成功结果时出错: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 保存失败结果到数据库
     * 
     * @param prompt       Prompt对象
     * @param errorMessage 错误信息
     * @param retryCount   重试次数
     */
    private void saveFailureResult(Prompt prompt, String errorMessage, int retryCount) {
        try {
            // 先查询最新的Prompt对象，避免重复行问题
            Prompt currentPrompt = promptRepository.findById(prompt.getPromptId()).orElse(prompt);

            // 更新Prompt表中的字段
            currentPrompt.setStatusName("已处理");
            currentPrompt.setRetryCount(retryCount);
            currentPrompt.setExecutionTime(LocalDateTime.now());
            // 保存Prompt
            currentPrompt = promptRepository.save(currentPrompt);

            // 创建并保存PromptResult
            PromptResult promptResult = new PromptResult();
            promptResult.setPromptId(currentPrompt.getPromptId());
            promptResult.setOriginalResultContent(errorMessage);
            promptResult.setStatus("失败");
            promptResult.setExecutionTime(LocalDateTime.now());
            promptResult.setCreatedAt(LocalDateTime.now());
            promptResult.setUpdatedAt(LocalDateTime.now());
            promptResult.setLastModifiedBy(0); // 默认值，可以根据需要修改
            promptResult.setIsRead(0); // 默认未读
            promptResult.setDeleted(0); // 默认未删除
            promptResultRepository.save(promptResult);
        } catch (Exception e) {
            System.err.println("保存失败结果时出错: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 处理Prompt内容并返回结果
     * 
     * @param objectiveContent 客观内容
     * @param dailyRecords     日常记录
     * @param templateContent  模板内容
     * @return 处理结果字符串
     */
    private String processPrompt(String objectiveContent, String dailyRecords, String templateContent) {
        // 按指定格式组合Prompt
        String combinedPrompt = objectiveContent + "\n" + dailyRecords + "\n" + templateContent;

        // 输出组合后的Prompt到控制台
        System.out.println("========== 组合后的Prompt内容 ==========");
        System.out.println(combinedPrompt);
        System.out.println("=====================================");

        // 调用AI接口分析
        String analysisResult = callAIAnalysis(combinedPrompt);

        // 输出分析结果到控制台
        System.out.println("========== AI分析结果 ==========");
        System.out.println(analysisResult);
        System.out.println("===============================");

        return analysisResult;
    }

    /**
     * 调用AI接口分析Prompt内容
     * 此方法参考前端代码实现，确保与前端调用AI服务的方式保持一致
     * 
     * @param prompt 组合后的Prompt内容
     * @return AI分析结果字符串
     * 
     *         更新说明：
     *         1. 使用与前端相同的模型名称 "deepseek-chat"
     *         2. 使用与前端相同的温度参数 0.7
     *         3. 启用流式传输以提高响应速度
     *         4. 设置最大token数为4096
     *         5. 使用相同的system和user消息结构
     *         6. 调用相同的本地API路径 /api/ai/response
     *         7. 修复流式响应处理问题，确保能正确提取完整内容
     */
    private String callAIAnalysis(String prompt) {
        try {
            // 创建AIRequest对象，参考前端代码的参数
            AIRequest aiRequest = new AIRequest();
            aiRequest.setModel(DEFAULT_AI_MODEL); // 使用默认模型 "deepseek-chat"
            aiRequest.setTemperature(0.7); // 使用前端代码中的温度参数
            aiRequest.setStream(true); // 使用流式传输
            aiRequest.setMaxTokens(4096); // 设置最大token数

            // 设置消息内容，与前端代码保持一致
            // 使用动态系统prompt，从文件读取
            String systemPromptContent = getSystemPrompt();
            List<Map<String, String>> messages = List.of(
                    Map.of(
                            "role", "system",
                            "content", systemPromptContent),
                    Map.of(
                            "role", "user",
                            "content", prompt));
            aiRequest.setMessages(messages);

            // 构造请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 构造请求实体
            HttpEntity<AIRequest> request = new HttpEntity<>(aiRequest, headers);

            System.out.println("开始调用本地AI分析接口，prompt长度: " + prompt.length());

            // 调用本地/api/ai/response接口，与前端代码保持一致
            // 使用String.class来处理流式响应
            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + AI_API_PATH, // 使用配置的基础URL
                    org.springframework.http.HttpMethod.POST,
                    request,
                    String.class);

            // 处理响应，参考前端代码的处理方式
            if (!response.getStatusCode().is2xxSuccessful()) {
                return "调用AI接口失败，状态码: " + response.getStatusCode().value();
            }

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isEmpty()) {
                return "AI接口返回空响应体";
            }

            // 处理流式响应，提取最终的完整内容
            String content = extractContentFromStreamingResponse(responseBody);

            System.out.println("本地AI分析完成");

            return content;
        } catch (org.springframework.web.client.ResourceAccessException e) {
            return "AI接口连接超时，请检查网络或服务状态: " + e.getMessage();
        } catch (Exception e) {
            return "调用AI分析接口异常: " + e.getMessage();
        }
    }

    /**
     * 读取系统prompt文件内容
     * 
     * 该方法从src/main/resources/systemPrompt文件读取系统prompt内容。
     * 如果文件不存在或读取失败，则返回默认的系统prompt。
     * 
     * @return 系统prompt字符串内容
     */
    private String getSystemPrompt() {
        try {
            // 系统prompt文件路径
            String systemPromptPath = "src/main/resources/systemPrompt";
            
            // 读取文件内容
            String content = Files.readString(Paths.get(systemPromptPath), StandardCharsets.UTF_8);
            
            // 去除首尾空白字符
            content = content.trim();
            
            // 如果文件内容为空，使用默认值
            if (content.isEmpty()) {
                System.err.println("系统prompt文件为空，使用默认系统prompt");
                return "You are a helpful assistant.";
            }
            
            System.out.println("成功读取系统prompt，长度: " + content.length() + " 字符");
            return content;
            
        } catch (Exception e) {
            System.err.println("读取系统prompt文件失败，使用默认系统prompt: " + e.getMessage());
            return "You are a helpful assistant.";
        }
    }

    /**
     * 系统启动时初始化方法
     * 
     * 该方法在应用启动完成后执行，用于输出系统prompt加载状态
     * 便于确认系统prompt功能正常工作
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            // 尝试读取系统prompt文件
            String systemPromptPath = "src/main/resources/systemPrompt";
            String content = Files.readString(Paths.get(systemPromptPath), StandardCharsets.UTF_8);
            content = content.trim();
            
            if (!content.isEmpty()) {
                System.out.println("已经加载systemPrompt，长度: " + content.length() + " 字符");
            } else {
                System.out.println("已经加载systemPrompt（文件为空，使用默认值）");
            }
        } catch (Exception e) {
            System.out.println("已经加载systemPrompt（文件读取失败，使用默认值）");
        }
    }

    /**
     * 从流式响应中提取完整内容
     * 
     * 该方法负责处理AI接口返回的流式响应，从中提取最终的完整内容。
     * 流式响应通常包含多行JSON数据，最后一行是[DONE]标记。
     * 该方法会解析这些数据并提取实际的内容，同时清理可能存在的Markdown标记。
     * 
     * @param streamingResponse 流式响应字符串，包含多行JSON数据
     * @return 提取并清理后的完整内容字符串，如果解析失败则返回清理后的原始响应
     * 
     *         处理流程：
     *         1. 按行分割响应数据
     *         2. 查找最后一个有效的JSON行（非空且不是[DONE]）
     *         3. 尝试解析JSON并提取content字段
     *         4. 如果解析失败，则使用原始响应数据
     *         5. 清理内容中的Markdown标记（如```markdown）
     *         6. 返回清理后的内容
     */
    private String extractContentFromStreamingResponse(String streamingResponse) {
        try {
            // 按行分割响应
            String[] lines = streamingResponse.split("\n");
            String finalLine = null;

            // 查找包含完整内容的最后一行（通常是包含content字段的JSON行）
            for (int i = lines.length - 1; i >= 0; i--) {
                String line = lines[i].trim();
                if (!line.isEmpty() && !line.equals("[DONE]")) {
                    finalLine = line;
                    break;
                }
            }

            String content = null;
            if (finalLine != null) {
                // 尝试解析JSON并提取content字段
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(finalLine,
                        new TypeReference<Map<String, Object>>() {
                        });

                if (responseMap.containsKey("content")) {
                    Object contentObj = responseMap.get("content");
                    if (contentObj != null) {
                        content = contentObj.toString();
                    }
                }
            }

            // 如果无法从JSON中提取content，则使用原始响应（去除[DONE]标记）
            if (content == null) {
                StringBuilder cleanResponse = new StringBuilder();
                for (String line : lines) {
                    if (!line.trim().equals("[DONE]")) {
                        cleanResponse.append(line);
                    }
                }
                content = cleanResponse.toString();
            }

            // 去除内容开头的"```markdown"标记和结尾的"```"标记
            if (content != null) {
                content = content.trim();
                if (content.startsWith("```markdown")) {
                    content = content.substring(11); // 去除"```markdown"
                }
                if (content.startsWith("\n")) {
                    content = content.substring(1); // 去除开头的换行符
                }
                if (content.endsWith("```")) {
                    content = content.substring(0, content.length() - 3); // 去除结尾的"```"
                }
                content = content.trim(); // 再次去除首尾空白
            }

            return content != null ? content : "";
        } catch (Exception e) {
            System.err.println("解析流式响应时出错: " + e.getMessage());
            // 如果解析失败，返回原始响应（去除[DONE]标记和markdown标记）
            String cleanResponse = streamingResponse.replace("[DONE]", "").trim();
            if (cleanResponse.startsWith("```markdown")) {
                cleanResponse = cleanResponse.substring(11);
            }
            if (cleanResponse.startsWith("\n")) {
                cleanResponse = cleanResponse.substring(1);
            }
            if (cleanResponse.endsWith("```")) {
                cleanResponse = cleanResponse.substring(0, cleanResponse.length() - 3);
            }
            return cleanResponse.trim();
        }
    }
}
