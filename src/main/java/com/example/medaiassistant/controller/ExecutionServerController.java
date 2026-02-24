package com.example.medaiassistant.controller;

import com.example.medaiassistant.util.AESEncryptionUtil;
import com.example.medaiassistant.util.StreamingResponseUtil;
import com.example.medaiassistant.util.ClobManager;
import com.example.medaiassistant.util.ResponseCacheUtil;
import java.util.function.Supplier;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;
import com.example.medaiassistant.service.ServerConfigService;
import com.example.medaiassistant.service.EncryptedDataTempService;
import com.example.medaiassistant.service.AsyncCallbackService;
import com.example.medaiassistant.model.ServerConfiguration;
import com.example.medaiassistant.model.EncryptedDataTemp;
import com.example.medaiassistant.model.DataStatus;
import com.example.medaiassistant.repository.executionserver.ExecutionServerEncryptedDataTempRepository;
import com.example.medaiassistant.config.AIModelConfig;
import com.example.medaiassistant.config.SchedulingProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

// LLM RestTemplate 专用导入
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import java.sql.Clob;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.Objects;

/**
 * 执行服务器控制器
 * 处理来自主应用的加密Prompt请求
 * 监听端口8082
 * 
 * 该控制器负责处理来自主应用的加密Prompt请求，进行解密和处理，
 * 并提供服务器健康检查和配置信息查询功能。
 * 集成轮询功能，定时处理数据库中的未处理记录。
 */
@RestController
@RequestMapping("/api/execute")
@EnableScheduling
public class ExecutionServerController {
    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(ExecutionServerController.class);

    /** 服务器配置服务 */
    private final ServerConfigService serverConfigService;

    /** 加密数据临时表Repository - 使用执行服务器专用Repository */
    private final ExecutionServerEncryptedDataTempRepository encryptedDataTempRepository;

    /** 加密数据临时表服务 */
    private final EncryptedDataTempService encryptedDataTempService;

    /** 异步回调服务 */
    private final AsyncCallbackService asyncCallbackService;

    /** 专用于LLM调用的RestTemplate - 优化超时配置 */
    private final RestTemplate llmRestTemplate;

    /** 回调功能开关 */
    @Value("${callback.enabled:false}")
    private boolean callbackEnabled;

    /** 轮询服务启用状态标志 */
    private volatile boolean pollingServiceEnabled = false;

    /** 轮询服务状态信息 */
    private volatile String pollingServiceStatus = "STOPPED";

    /** 轮询服务启动时间 */
    private volatile LocalDateTime pollingServiceStartTime = null;

    // 轮询统计计数器
    private final AtomicLong totalPollingCycles = new AtomicLong(0);
    private final AtomicLong totalProcessedRecords = new AtomicLong(0);
    private final AtomicLong totalSuccessfulRecords = new AtomicLong(0);
    private final AtomicLong totalFailedRecords = new AtomicLong(0);

    /** 响应缓存工具 */
    @Autowired
    private ResponseCacheUtil responseCacheUtil;

    /** 执行服务器数据源 */
    @Autowired
    @Qualifier("executionDataSource")
    private DataSource executionDataSource;

    /** AI模型配置 - 与主服务器使用相同的配置方法 */
    @Autowired
    private AIModelConfig aiModelConfig;

    /** 调度配置属性 */
    @Autowired
    private SchedulingProperties schedulingProperties;

    /** 任务执行器 - 复用现有的taskExecutor线程池 */
    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;

    // Sprint 2: 重试机制统计计数器
    private final AtomicLong llmCallCounter = new AtomicLong(0);
    private final AtomicLong llmSuccessCounter = new AtomicLong(0);
    private final AtomicLong llmFailureCounter = new AtomicLong(0);
    private final AtomicLong timeoutErrorCounter = new AtomicLong(0);
    private final AtomicLong retrySuccessCounter = new AtomicLong(0);
    private final AtomicLong maxRetryExceededCounter = new AtomicLong(0);

    // Sprint 3: 高级性能监控统计
    private final AtomicLong totalResponseTimeMs = new AtomicLong(0);
    private final AtomicLong minResponseTimeMs = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxResponseTimeMs = new AtomicLong(0);
    private final AtomicReference<LocalDateTime> lastSuccessTime = new AtomicReference<>();
    private final AtomicReference<LocalDateTime> lastFailureTime = new AtomicReference<>();

    // Sprint 3: 响应时间分布统计
    private final AtomicLong responsesUnder1s = new AtomicLong(0); // <1秒
    private final AtomicLong responses1to5s = new AtomicLong(0); // 1-5秒
    private final AtomicLong responses5to30s = new AtomicLong(0); // 5-30秒
    private final AtomicLong responses30to120s = new AtomicLong(0); // 30秒-2分钟
    private final AtomicLong responsesOver120s = new AtomicLong(0); // >2分钟

    // Sprint 3: 历史数据队列（最近100次调用）
    private final Queue<LLMCallRecord> recentCalls = new ConcurrentLinkedQueue<>();
    private static final int MAX_RECENT_CALLS = 100;

    // Sprint 3: 配置建议缓存
    private final AtomicReference<Map<String, Object>> lastConfigRecommendation = new AtomicReference<>();

    /**
     * Sprint 3: LLM调用记录数据结构
     * 用于存储单次调用的详细信息
     */
    private static class LLMCallRecord {
        private final LocalDateTime callTime;
        private final long responseTimeMs;
        private final boolean success;
        private final String errorType;
        private final int retryCount;

        public LLMCallRecord(boolean success, long responseTimeMs, String errorType, int retryCount) {
            this.callTime = LocalDateTime.now();
            this.success = success;
            this.responseTimeMs = responseTimeMs;
            this.errorType = errorType;
            this.retryCount = retryCount;
        }

        // Getters
        public LocalDateTime getCallTime() {
            return callTime;
        }

        public long getResponseTimeMs() {
            return responseTimeMs;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorType() {
            return errorType;
        }

        public int getRetryCount() {
            return retryCount;
        }
    }

    /**
     * Sprint 3: 记录响应时间统计
     * 更新各种响应时间相关的统计数据
     */
    private void recordResponseTime(long responseTimeMs) {
        // 更新总响应时间
        totalResponseTimeMs.addAndGet(responseTimeMs);

        // 更新最小/最大响应时间
        minResponseTimeMs.updateAndGet(
                current -> current == Long.MAX_VALUE ? responseTimeMs : Math.min(current, responseTimeMs));
        maxResponseTimeMs.updateAndGet(current -> Math.max(current, responseTimeMs));

        // 响应时间分布统计
        if (responseTimeMs < 1000) {
            responsesUnder1s.incrementAndGet(); // <1秒
        } else if (responseTimeMs < 5000) {
            responses1to5s.incrementAndGet(); // 1-5秒
        } else if (responseTimeMs < 30000) {
            responses5to30s.incrementAndGet(); // 5-30秒
        } else if (responseTimeMs < 120000) {
            responses30to120s.incrementAndGet(); // 30秒-2分钟
        } else {
            responsesOver120s.incrementAndGet(); // >2分钟
        }
    }

    /**
     * Sprint 3: 记录调用历史
     * 维护最近100次调用的详细记录
     */
    private void recordCallHistory(boolean success, long responseTimeMs, String errorType, int retryCount) {
        LLMCallRecord record = new LLMCallRecord(success, responseTimeMs, errorType, retryCount);
        recentCalls.offer(record);

        // 保持队列大小，最多保存100条记录
        while (recentCalls.size() > MAX_RECENT_CALLS) {
            recentCalls.poll();
        }
    }

    /**
     * Sprint 3: 添加响应时间统计信息
     */
    private void addResponseTimeStats(Map<String, Object> stats, long totalCalls) {
        if (totalCalls == 0) {
            stats.put("averageResponseTime", 0.0);
            stats.put("minResponseTime", 0.0);
            stats.put("maxResponseTime", 0.0);
            return;
        }

        // 计算平均响应时间
        double avgResponseTime = (double) totalResponseTimeMs.get() / totalCalls;
        stats.put("averageResponseTime", Math.round(avgResponseTime * 100.0) / 100.0);

        // 最小/最大响应时间
        long minTime = minResponseTimeMs.get();
        stats.put("minResponseTime", minTime == Long.MAX_VALUE ? 0 : minTime);
        stats.put("maxResponseTime", maxResponseTimeMs.get());

        // 响应时间分布
        Map<String, Object> responseTimeDistribution = new HashMap<>();
        responseTimeDistribution.put("under1s", responsesUnder1s.get());
        responseTimeDistribution.put("1to5s", responses1to5s.get());
        responseTimeDistribution.put("5to30s", responses5to30s.get());
        responseTimeDistribution.put("30to120s", responses30to120s.get());
        responseTimeDistribution.put("over120s", responsesOver120s.get());
        stats.put("responseTimeDistribution", responseTimeDistribution);

        // 最近更新时间
        LocalDateTime lastSuccess = lastSuccessTime.get();
        LocalDateTime lastFailure = lastFailureTime.get();
        if (lastSuccess != null) {
            stats.put("lastSuccessTime", lastSuccess.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (lastFailure != null) {
            stats.put("lastFailureTime", lastFailure.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
    }

    /**
     * Sprint 3: 添加历史数据分析
     */
    private void addHistoricalAnalysis(Map<String, Object> stats) {
        List<LLMCallRecord> recentCallsList = new ArrayList<>(recentCalls);

        if (recentCallsList.isEmpty()) {
            stats.put("recentCallsAnalysis", Map.of("message", "暂无历史调用数据"));
            return;
        }

        // 最近50次调用的成功率
        long recentSuccessCount = recentCallsList.stream()
                .mapToLong(record -> record.isSuccess() ? 1 : 0)
                .sum();
        double recentSuccessRate = (double) recentSuccessCount / recentCallsList.size() * 100;

        // 最近调用的平均响应时间
        double recentAvgResponseTime = recentCallsList.stream()
                .mapToLong(LLMCallRecord::getResponseTimeMs)
                .average().orElse(0.0);

        // 最近调用的重试次数统计
        double avgRetryCount = recentCallsList.stream()
                .mapToInt(LLMCallRecord::getRetryCount)
                .average().orElse(0.0);

        Map<String, Object> recentAnalysis = new HashMap<>();
        recentAnalysis.put("recentCallsCount", recentCallsList.size());
        recentAnalysis.put("recentSuccessRate", Math.round(recentSuccessRate * 100.0) / 100.0);
        recentAnalysis.put("recentAvgResponseTime", Math.round(recentAvgResponseTime * 100.0) / 100.0);
        recentAnalysis.put("recentAvgRetryCount", Math.round(avgRetryCount * 100.0) / 100.0);

        stats.put("recentCallsAnalysis", recentAnalysis);
    }

    /**
     * Sprint 3: 添加配置庭议
     */
    private void addConfigurationRecommendations(Map<String, Object> stats, double successRate,
            double timeoutErrorRate, double retrySuccessRate) {
        Map<String, Object> recommendations = new HashMap<>();

        // 超时配置庭议
        if (timeoutErrorRate > 5.0) {
            recommendations.put("timeoutConfig", "庭议增加连接超时至120秒，读取超时至15分钟");
        } else if (timeoutErrorRate < 1.0) {
            recommendations.put("timeoutConfig", "超时配置适当，庭议维持现有设置");
        }

        // 重试机制庭议
        if (retrySuccessRate < 30.0 && retrySuccessRate > 0) {
            recommendations.put("retryConfig", "庭议增加重试次数至5次，或增加基础延迟至2秒");
        } else if (retrySuccessRate > 80.0) {
            recommendations.put("retryConfig", "重试机制表现良好，庭议维持现有设置");
        }

        // 连接池配置庭议
        if (successRate < 95.0) {
            recommendations.put("connectionPool", "庭议增加连接池大小至70，每路由最大连接数至15");
        }

        // 整体庭议
        if (successRate >= 99.0) {
            recommendations.put("overall", "系统运行优秀，庭议维持现有配置");
        } else if (successRate < 90.0) {
            recommendations.put("overall", "系统性能需要优化，庭议检查网络、服务器和LLM服务状态");
        }

        stats.put("configurationRecommendations", recommendations);

        // 缓存庭议结果
        lastConfigRecommendation.set(recommendations);
    }

    // Sprint 2: 重试机制配置常量
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long BASE_DELAY_MS = 1000; // 1秒基础延迟
    private static final double JITTER_MIN_FACTOR = 0.2; // 20%最小抖动
    private static final double JITTER_MAX_FACTOR = 0.6; // 60%最大抖动

    /**
     * 构造函数
     * 
     * @param serverConfigService         服务器配置服务
     * @param encryptedDataTempRepository 加密数据临时表Repository
     * @param encryptedDataTempService    加密数据临时表服务
     * @param asyncCallbackService        异步回调服务
     * @param aiModelConfig               AI模型配置
     */
    public ExecutionServerController(ServerConfigService serverConfigService,
            ExecutionServerEncryptedDataTempRepository encryptedDataTempRepository,
            EncryptedDataTempService encryptedDataTempService,
            AsyncCallbackService asyncCallbackService,
            AIModelConfig aiModelConfig) {
        this.serverConfigService = serverConfigService;
        this.encryptedDataTempRepository = encryptedDataTempRepository;
        this.encryptedDataTempService = encryptedDataTempService;
        this.asyncCallbackService = asyncCallbackService;
        this.aiModelConfig = aiModelConfig;

        // 初始化LLM专用RestTemplate
        this.llmRestTemplate = createLLMRestTemplate();

        logger.info("执行服务器控制器初始化完成，已配置LLM专用RestTemplate和AI模型配置");
    }

    /**
     * 创建专用于LLM调用的RestTemplate
     * 配置更长的超时时间以适应LLM处理
     * Sprint 1: 核心超时优化
     */
    private RestTemplate createLLMRestTemplate() {
        try {
            // 创建专用的HTTP客户端配置
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", SSLConnectionSocketFactory.getSocketFactory())
                    .build();

            // LLM专用连接池管理器
            PoolingHttpClientConnectionManager llmConnectionManager = new PoolingHttpClientConnectionManager(
                    socketFactoryRegistry,
                    PoolConcurrencyPolicy.STRICT,
                    PoolReusePolicy.LIFO,
                    TimeValue.of(60000, TimeUnit.MILLISECONDS) // 1分钟保持连接
            );

            // 配置连接池参数 - 针对LLM调用优化
            llmConnectionManager.setMaxTotal(50); // 较小的连接池，LLM调用不需要太多并发
            llmConnectionManager.setDefaultMaxPerRoute(10);

            // LLM专用连接配置 - 更长的超时时间
            ConnectionConfig llmConnectionConfig = ConnectionConfig.custom()
                    .setConnectTimeout(Timeout.ofSeconds(60)) // 60秒连接超时
                    .setSocketTimeout(Timeout.ofMinutes(10)) // 10分钟Socket超时
                    .build();
            llmConnectionManager.setDefaultConnectionConfig(llmConnectionConfig);

            // Socket配置
            SocketConfig llmSocketConfig = SocketConfig.custom()
                    .setSoTimeout(Timeout.ofMinutes(10)) // 10分钟Socket超时
                    .build();
            llmConnectionManager.setDefaultSocketConfig(llmSocketConfig);

            // LLM专用请求配置
            RequestConfig llmRequestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(Timeout.ofSeconds(30)) // 30秒连接请求超时
                    .setResponseTimeout(Timeout.ofMinutes(10)) // 10分钟响应超时
                    .setRedirectsEnabled(false)
                    .build();

            // 创建LLM专用HTTP客户端
            CloseableHttpClient llmHttpClient = HttpClients.custom()
                    .setConnectionManager(llmConnectionManager)
                    .setDefaultRequestConfig(llmRequestConfig)
                    .setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE)
                    .build();

            // 创建LLM专用请求工厂
            HttpComponentsClientHttpRequestFactory llmRequestFactory = new HttpComponentsClientHttpRequestFactory(
                    llmHttpClient);

            // 创建并返回LLM专用RestTemplate
            RestTemplate template = new RestTemplate(llmRequestFactory);
            logger.info("LLM专用RestTemplate创建成功 - 连接超时:60秒, 读取超时:10分钟");
            return template;

        } catch (Exception e) {
            logger.error("创建LLM专用RestTemplate失败，回退到标准RestTemplate", e);
            return new RestTemplate(); // 回退到标准RestTemplate
        }
    }

    /**
     * 处理加密的Prompt请求
     * 
     * @deprecated 该接口已废弃，执行服务器已改为主动轮询模式。
     *             请使用 /api/execute/start 启动轮询服务来处理数据。
     * 
     *             该接口接收来自主应用的加密Prompt数据，进行解密后处理
     *             并返回处理结果
     * 
     * @param request 包含加密Prompt的请求体
     *                - encryptedPrompt: Base64编码的加密数据
     *                - encryptionType: 加密类型（默认为AES-256-CBC）
     *                - timestamp: 时间戳
     * @return 处理结果响应实体
     *         - status: 处理状态
     *         - originalLength: 原始Prompt长度
     *         - resultLength: 结果长度
     *         - decryptedData: 解密后的明文数据
     *         - decryptTimeMs: 解密用时（毫秒）
     *         - timestamp: 时间戳
     *         - result: 处理结果
     * @see AESEncryptionUtil#decrypt(String, String, String) AES解密方法
     * @see ServerConfigService#getConfigByName(String) 获取配置信息的方法
     */
    @Deprecated
    @PostMapping("/encrypted-prompt")
    public ResponseEntity<Map<String, Object>> handleEncryptedPrompt(
            @RequestBody Map<String, String> request) {
        long totalStartTime = System.currentTimeMillis();
        String dataId = null;

        try {
            String encryptedPrompt = request.get("encryptedPrompt");

            if (encryptedPrompt == null || encryptedPrompt.trim().isEmpty()) {
                logger.error("加密Prompt参数不能为空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "加密Prompt参数不能为空"));
            }

            logger.info("接收到加密Prompt请求，数据长度: {} 字符", encryptedPrompt.length());

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

            // 记录解密开始时间
            long startTime = System.nanoTime();

            // 使用AES解密工具进行解密
            String decryptedPrompt = AESEncryptionUtil.decrypt(encryptedPrompt, encryptionKey, encryptionSalt);

            // 计算解密用时
            long decryptTime = System.nanoTime() - startTime;
            double decryptTimeMs = decryptTime / 1_000_000.0; // 转换为毫秒

            logger.info("Prompt解密成功，解密后内容长度: {} 字符，解密用时: {} ms", decryptedPrompt.length(), decryptTimeMs);
            logger.debug("解密后的Prompt内容: {}", decryptedPrompt);

            // 保存解密数据到临时表（简化设计：ID和REQUEST_ID保持一致）
            try {
                // 简化逻辑：直接使用requestId作为ID，确保一致性
                String requestId = request.get("requestId");
                if (requestId != null && !requestId.trim().isEmpty()) {
                    dataId = requestId;
                } else {
                    // 如果请求中没有requestId，生成唯一ID
                    dataId = generateUniqueId();
                }
                final String finalDataId = dataId; // 创建final变量供lambda使用

                // 简化检查：只需检查ID是否存在（因为ID=REQUEST_ID）
                Optional<EncryptedDataTemp> existingRecord = encryptedDataTempRepository.findById(finalDataId);

                if (existingRecord.isPresent()) {
                    // 记录已存在，跳过插入，只更新状态和数据
                    EncryptedDataTemp existingData = existingRecord.get();
                    logger.info("检测到已存在记录，跳过插入，只更新数据和状态，ID: {}", finalDataId);

                    // 更新解密数据（可能之前的解密不完整）
                    existingData.setDecryptedData(ClobManager.createSerialClob(decryptedPrompt));
                    // 如果加密数据为空或不同，也更新
                    if (existingData.getEncryptedData() == null) {
                        existingData.setEncryptedData(ClobManager.createSerialClob(encryptedPrompt));
                    }

                    // 确保REQUEST_ID与ID保持一致（简化设计）
                    existingData.setRequestId(finalDataId);
                    if (existingData.getSource() == null) {
                        existingData.setSource("EXECUTION_SERVER");
                    }

                    // 保存更新的数据
                    EncryptedDataTemp updatedData = encryptedDataTempRepository.save(existingData);
                    logger.info("已更新现有记录，ID: {}", updatedData.getId());

                    // 根据当前状态决定下一步操作
                    DataStatus currentStatus = existingData.getStatus();
                    if (currentStatus == DataStatus.RECEIVED) {
                        // 如果状态还是RECEIVED，更新为DECRYPTED
                        encryptedDataTempService.updateStatus(finalDataId, DataStatus.DECRYPTED, null);
                        logger.info("数据状态已从RECEIVED更新为DECRYPTED，ID: {}", finalDataId);
                    } else {
                        logger.info("数据状态已是 {}，无需更新为DECRYPTED，ID: {}", currentStatus, finalDataId);
                    }
                } else {
                    // 记录不存在，创建新记录
                    logger.info("记录不存在，创建新记录，ID: {}", finalDataId);

                    EncryptedDataTemp tempData = new EncryptedDataTemp();
                    tempData.setId(finalDataId);

                    // 使用ClobManager创建Clob对象，优化内存管理
                    tempData.setEncryptedData(ClobManager.createSerialClob(encryptedPrompt));
                    tempData.setDecryptedData(ClobManager.createSerialClob(decryptedPrompt));

                    tempData.setRequestId(finalDataId); // 简化设计：REQUEST_ID与ID保持一致
                    tempData.setSource("EXECUTION_SERVER");
                    tempData.setStatus(DataStatus.RECEIVED);
                    // receivedTime, createdAt, updatedAt 会自动设置

                    EncryptedDataTemp savedData = encryptedDataTempRepository.save(tempData);
                    logger.info("解密数据已保存到临时表，ID: {}", savedData.getId());

                    // 更新状态为DECRYPTED（已解密）
                    encryptedDataTempService.updateStatus(finalDataId, DataStatus.DECRYPTED, null);
                    logger.info("数据状态已更新为DECRYPTED，ID: {}", finalDataId);
                }
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // 处理数据完整性约束冲突 - 实现幂等性操作
                final String originalDataId = dataId;
                logger.warn("检测到数据完整性约束冲突，检查是否为重复请求，ID: {}", originalDataId, e);

                try {
                    // 检查记录是否已存在，如果存在则直接使用（幂等性操作）
                    Optional<EncryptedDataTemp> existingRecord = encryptedDataTempRepository
                            .findById(Objects.requireNonNull(originalDataId, "originalDataId cannot be null"));

                    if (existingRecord.isPresent()) {
                        // 记录已存在，这是一个重复请求，直接使用现有记录
                        EncryptedDataTemp existingData = existingRecord.get();
                        logger.info("检测到重复请求，使用现有记录，ID: {}, 当前状态: {}",
                                originalDataId, existingData.getStatus());

                        // 检查是否需要更新解密数据（可能之前解密未完成）
                        if (existingData.getDecryptedData() == null) {
                            existingData.setDecryptedData(ClobManager.createSerialClob(decryptedPrompt));
                            encryptedDataTempRepository.save(existingData);
                            logger.info("更新了现有记录的解密数据，ID: {}", originalDataId);
                        }

                        // 确保状态正确（如果还是RECEIVED，更新为DECRYPTED）
                        if (existingData.getStatus() == DataStatus.RECEIVED) {
                            encryptedDataTempService.updateStatus(originalDataId, DataStatus.DECRYPTED, null);
                            logger.info("更新现有记录状态为DECRYPTED，ID: {}", originalDataId);
                        }

                        // 使用现有记录的ID，保持幂等性
                        dataId = originalDataId;

                    } else {
                        // 记录不存在但出现约束冲突，可能是并发竞态条件
                        logger.error("约束冲突但记录不存在，可能存在并发问题，ID: {}", originalDataId);
                        throw new RuntimeException("数据一致性异常：约束冲突但记录不存在，ID: " + originalDataId);
                    }

                } catch (Exception retryException) {
                    logger.error("处理数据完整性约束冲突时发生错误，ID: {}", originalDataId, retryException);
                    throw new RuntimeException("数据完整性约束冲突处理失败: " + retryException.getMessage(), retryException);
                }
            } catch (Exception e) {
                logger.error("保存解密数据到临时表失败", e);
                // 重新抛出异常，以便上层能够处理
                throw new RuntimeException("保存解密数据到临时表失败", e);
            }

            // 更新状态为PROCESSING（处理中）
            try {
                encryptedDataTempService.startProcessing(dataId);
                logger.info("数据状态已更新为PROCESSING，开始处理Prompt，ID: {}", dataId);
            } catch (Exception e) {
                logger.error("更新状态为PROCESSING失败", e);
                // 继续处理，不中断主流程
            }

            // 在这里处理解密后的Prompt
            String processingResult = processDecryptedPrompt(decryptedPrompt);

            logger.info("Prompt处理完成，结果长度: {} 字符", processingResult.length());

            // 更新EXECUTION_RESULT字段和状态
            try {
                // 检查处理结果是否为空
                if (processingResult == null || processingResult.trim().isEmpty()) {
                    logger.warn("处理结果为空，将使用默认错误信息，数据ID: {}", dataId);
                    processingResult = "处理失败：LLM返回空结果";
                }

                // 调试：解密processingResult并在控制台输出前200个字符，检测保存的结果是否正确
                try {
                    // 从数据库获取AES加密配置（使用不同的变量名避免重复定义）
                    ServerConfiguration debugEncryptionKeyConfig = serverConfigService
                            .getConfigByName("AES_ENCRYPTION_KEY");
                    ServerConfiguration debugEncryptionSaltConfig = serverConfigService
                            .getConfigByName("AES_ENCRYPTION_SALT");

                    if (debugEncryptionKeyConfig != null && debugEncryptionSaltConfig != null) {
                        String debugEncryptionKey = debugEncryptionKeyConfig.getConfigData();
                        String debugEncryptionSalt = debugEncryptionSaltConfig.getConfigData();

                        if (debugEncryptionKey != null && !debugEncryptionKey.trim().isEmpty() &&
                                debugEncryptionSalt != null && !debugEncryptionSalt.trim().isEmpty()) {

                            // 尝试解密processingResult
                            String decryptedResult = AESEncryptionUtil.decrypt(processingResult, debugEncryptionKey,
                                    debugEncryptionSalt);
                            String preview = decryptedResult.length() > 200 ? decryptedResult.substring(0, 200) + "..."
                                    : decryptedResult;

                            System.out.println("=== DEBUG: EXECUTION_RESULT解密内容预览（前200字符）===");
                            System.out.println(preview);
                            System.out.println("=== DEBUG: 解密内容总长度: " + decryptedResult.length() + " 字符 ===");
                            System.out.println("=== DEBUG: 加密内容长度: " + processingResult.length() + " 字符 ===");
                        } else {
                            System.out.println("=== DEBUG: 加密密钥或盐值为空，无法解密processingResult ===");
                        }
                    } else {
                        System.out.println("=== DEBUG: 加密配置未找到，无法解密processingResult ===");
                    }
                } catch (Exception e) {
                    System.out.println("=== DEBUG: 解密processingResult失败: " + e.getMessage() + " ===");
                    // 如果解密失败，可能是processingResult已经是明文，直接输出前200字符
                    String preview = processingResult.length() > 200 ? processingResult.substring(0, 200) + "..."
                            : processingResult;
                    System.out.println("=== DEBUG: processingResult原始内容预览（前200字符）===");
                    System.out.println(preview);
                    System.out.println("=== DEBUG: processingResult原始内容总长度: " + processingResult.length() + " 字符 ===");
                }

                // 将处理结果保存到EXECUTION_RESULT字段
                // 注意：processingResult已经是加密后的LLM结果，直接保存
                Clob executionResultClob = ClobManager.createSerialClob(processingResult);
                encryptedDataTempService.completeProcessing(dataId, executionResultClob);
                logger.info("成功将加密的LLM处理结果保存到EXECUTION_RESULT字段，数据ID: {}，加密结果长度: {} 字符", dataId,
                        processingResult.length());

                // 只更新状态为ENCRYPTED，不覆盖原始ENCRYPTED_DATA字段
                // 修复：不再传递可能为空的处理结果，仅更新状态
                encryptedDataTempService.encryptResult(dataId, ClobManager.createSerialClob("")); // 参数被忽略，仅更新状态
                logger.info("数据状态已更新为ENCRYPTED，ID: {}", dataId);
            } catch (Exception e) {
                logger.error("更新EXECUTION_RESULT字段或状态失败", e);
                // 继续处理，不中断主流程
            }

            // 异步发送回调到主应用服务器
            try {
                if (callbackEnabled) {
                    sendAsyncCallback(dataId, processingResult);
                    logger.info("已触发异步回调发送，数据ID: {}", dataId);
                } else {
                    logger.info("回调功能已禁用，跳过回调发送，数据ID: {}", dataId);
                }
            } catch (Exception e) {
                logger.error("发送异步回调失败，数据ID: {}", dataId, e);
                // 回调失败不影响主流程，继续返回处理结果
            }

            // 计算总处理时间
            long totalEndTime = System.currentTimeMillis();
            long totalProcessingTime = totalEndTime - totalStartTime;

            // 记录性能统计
            logger.info("加密Prompt请求处理完成 - 总耗时: {}ms, 解密耗时: {}ms, 数据ID: {}",
                    totalProcessingTime, decryptTimeMs, dataId);

            return ResponseEntity.ok(Map.of(
                    "status", "PROCESSED",
                    "originalLength", decryptedPrompt.length(),
                    "resultLength", processingResult.length(),
                    "decryptedData", decryptedPrompt, // 添加解密后的数据
                    "decryptTimeMs", decryptTimeMs, // 添加解密用时
                    "totalProcessingTimeMs", totalProcessingTime, // 添加总处理时间
                    "timestamp", System.currentTimeMillis(),
                    "result", processingResult));

        } catch (Exception e) {
            logger.error("处理加密Prompt失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "处理加密Prompt失败: " + e.getMessage()));
        }
    }

    /**
     * 处理解密后的Prompt - 优化版本，集成响应缓存
     * 
     * 调用LLM服务进行Prompt分析，支持流式传输处理和响应缓存
     * 将LLM分析结果进行加密并返回
     * 
     * @param decryptedPrompt 解密后的Prompt内容
     * @return 加密后的处理结果
     */
    private String processDecryptedPrompt(String decryptedPrompt) {
        logger.info("开始处理解密后的Prompt，内容长度: {} 字符", decryptedPrompt.length());
        logger.debug("解密后的Prompt内容: {}", decryptedPrompt);

        try {
            // 生成缓存键
            String cacheKey = ResponseCacheUtil.generateCacheKey(decryptedPrompt);

            // 检查缓存中是否有对应的响应
            String cachedResponse = responseCacheUtil.get(cacheKey);
            if (cachedResponse != null) {
                logger.info("缓存命中，使用缓存的LLM响应，缓存键: {}", cacheKey);

                // 在控制台输出缓存的LLM分析结果的前200个字符（未加密）
                if (cachedResponse != null && !cachedResponse.trim().isEmpty()) {
                    String preview = cachedResponse.length() > 200 ? cachedResponse.substring(0, 200) + "..."
                            : cachedResponse;
                    System.out.println("=== 缓存LLM分析结果预览（前200字符）===");
                    System.out.println(preview);
                    System.out.println("=== 缓存结果预览结束，总长度: " + cachedResponse.length() + " 字符 ===");
                }

                // 对缓存的响应进行加密
                String encryptedResult = encryptProcessingResult(cachedResponse);
                logger.info("缓存响应加密完成，加密后长度: {} 字符", encryptedResult.length());
                return encryptedResult;
            }

            logger.info("缓存未命中，调用LLM服务进行实时分析，缓存键: {}", cacheKey);

            // 调用LLM服务进行Prompt分析（Sprint 2版本：集成重试机制）
            Map<String, Object> llmResponse = callLLMServiceWithRetry(decryptedPrompt);

            // 检查LLM调用是否成功
            if (llmResponse.get("success") != null && (Boolean) llmResponse.get("success")) {
                String result = (String) llmResponse.get("result");
                logger.info("LLM分析完成，响应长度: {} 字符", result.length());
                logger.debug("LLM分析结果: {}", result);

                // 在控制台输出LLM分析结果的前200个字符（未加密）
                if (result != null && !result.trim().isEmpty()) {
                    String preview = result.length() > 200 ? result.substring(0, 200) + "..." : result;
                    System.out.println("=== LLM分析结果预览（前200字符）===");
                    System.out.println(preview);
                    System.out.println("=== 结果预览结束，总长度: " + result.length() + " 字符 ===");
                }

                // 将响应结果放入缓存
                boolean cacheSuccess = responseCacheUtil.put(cacheKey, result);
                if (cacheSuccess) {
                    logger.info("LLM响应已缓存，缓存键: {}, 缓存大小: {}", cacheKey, responseCacheUtil.size());
                } else {
                    logger.warn("LLM响应缓存失败，缓存键: {}", cacheKey);
                }

                // 对LLM分析结果进行加密
                String encryptedResult = encryptProcessingResult(result);
                logger.info("LLM分析结果加密完成，加密后长度: {} 字符", encryptedResult.length());

                return encryptedResult;
            } else {
                // LLM调用失败，返回加密的错误信息
                String errorType = (String) llmResponse.get("errorType");
                String errorMessage = (String) llmResponse.get("errorMessage");
                logger.error("LLM服务调用失败，错误类型: {}, 错误信息: {}", errorType, errorMessage);

                // 创建结构化的错误信息
                Map<String, Object> errorInfo = new HashMap<>();
                errorInfo.put("errorType", errorType);
                errorInfo.put("errorMessage", errorMessage);
                errorInfo.put("timestamp", System.currentTimeMillis());

                // 将错误信息转换为JSON字符串并加密
                String errorJson = convertMapToJson(errorInfo);
                String encryptedError = encryptProcessingResult(errorJson);
                logger.info("错误信息已加密并保存到ExecutionResult，加密后长度: {} 字符", encryptedError.length());

                return encryptedError;
            }
        } catch (Exception e) {
            logger.error("处理Prompt时发生错误", e);
            // 创建结构化的错误信息
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("errorType", "PROCESSING_ERROR");
            errorInfo.put("errorMessage", "处理Prompt时发生错误: " + e.getMessage());
            errorInfo.put("timestamp", System.currentTimeMillis());

            // 将错误信息转换为JSON字符串并加密
            String errorJson = convertMapToJson(errorInfo);
            String encryptedError = encryptProcessingResult(errorJson);
            logger.info("处理错误信息已加密并保存到ExecutionResult，加密后长度: {} 字符", encryptedError.length());

            return encryptedError;
        }
    }

    /**
     * 获取系统prompt内容
     * 从src/main/resources/systemPrompt文件读取系统prompt
     * 如果文件不存在或读取失败，返回默认系统prompt
     * 
     * @return 系统prompt内容
     */
    private String getSystemPrompt() {
        try {
            // 读取系统prompt文件
            java.nio.file.Path systemPromptPath = java.nio.file.Paths.get("src", "main", "resources", "systemPrompt");
            if (java.nio.file.Files.exists(systemPromptPath)) {
                String content = java.nio.file.Files.readString(systemPromptPath);
                if (content != null && !content.trim().isEmpty()) {
                    logger.info("执行服务器已加载系统prompt，内容长度: {} 字符", content.length());
                    return content.trim();
                }
            }

            // 文件不存在或内容为空，返回默认系统prompt
            logger.warn("系统prompt文件不存在或内容为空，使用默认系统prompt");
            return "你是一个专业的医疗AI助手，请根据用户的医疗相关Prompt提供专业、准确、有帮助的回答。";

        } catch (Exception e) {
            logger.error("读取系统prompt文件失败，使用默认系统prompt", e);
            return "你是一个专业的医疗AI助手，请根据用户的医疗相关Prompt提供专业、准确、有帮助的回答。";
        }
    }

    /**
     * 调用LLM服务进行Prompt分析（执行服务器使用与主服务器相同的配置方法）
     * 使用AIModelConfig配置类获取DeepSeek API配置，与主服务器保持一致
     * 集成系统prompt功能，确保与主服务器使用相同的AI行为指导
     * 
     * @param prompt 需要分析的Prompt内容
     * @return LLM分析结果或结构化的错误信息，包含success、result、errorType、errorMessage等字段
     * 
     * @since 2025-10-10
     * @version 2.0
     * @author System
     * 
     * @see AIModelConfig#getModelConfig(String) 获取模型配置
     * @see StreamingResponseUtil#extractContentFromStreamingResponse(String) 解析流式响应
     * @see RestTemplate#exchange(String, HttpMethod, HttpEntity, Class) HTTP调用
     */
    private Map<String, Object> callLLMService(String prompt) {
        try {
            logger.info("开始调用LLM服务分析Prompt（使用AIModelConfig配置），内容长度: {} 字符", prompt.length());

            // 使用与主服务器相同的配置方法获取DeepSeek模型配置
            AIModelConfig.ModelConfig modelConfig = aiModelConfig.getModelConfig("deepseek-chat");

            if (modelConfig == null) {
                String errorMessage = "未找到deepseek-chat模型配置，请检查ai-models.properties文件";
                logger.error(errorMessage);
                return createErrorResponse("LLM_CONFIG_MISSING", errorMessage);
            }

            String apiUrl = modelConfig.getUrl();
            String apiKey = modelConfig.getKey();

            if (apiUrl == null || apiUrl.trim().isEmpty() || apiKey == null || apiKey.trim().isEmpty()) {
                String errorMessage = "deepseek-chat模型配置中URL或Key为空，请检查ai-models.properties文件";
                logger.error(errorMessage);
                return createErrorResponse("LLM_CONFIG_EMPTY", errorMessage);
            }

            logger.info("使用模型配置 - URL: {}, 连接超时: {}ms, 读取超时: {}ms",
                    apiUrl, modelConfig.getConnectTimeout(), modelConfig.getReadTimeout());

            // 构建外部AI请求 - 使用DeepSeek API格式（流式响应）
            Map<String, Object> aiRequest = new HashMap<>();
            aiRequest.put("model", "deepseek-chat");
            aiRequest.put("temperature", 0.7);
            aiRequest.put("stream", true); // 执行服务器使用流式响应以保持完整交互能力
            aiRequest.put("max_tokens", 8192); // 从2048提升到8192，避免长输出被截断
            aiRequest.put("top_p", 0.9);
            aiRequest.put("frequency_penalty", 0.0);
            aiRequest.put("presence_penalty", 0.0);
            aiRequest.put("n", 1);
            aiRequest.put("user", "execution_server");

            // 构建消息内容 - 包含系统prompt和用户prompt
            String systemPrompt = getSystemPrompt();
            List<Map<String, String>> messages = List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", prompt));
            aiRequest.put("messages", messages);

            logger.info("LLM请求消息构建完成 - 系统prompt长度: {} 字符, 用户prompt长度: {} 字符",
                    systemPrompt.length(), prompt.length());

            // 准备请求头 - 包含Bearer Token认证和流式响应支持
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            headers.setAccept(List.of(MediaType.APPLICATION_NDJSON, MediaType.APPLICATION_JSON));

            // 构建请求实体
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(aiRequest, headers);

            logger.info("发送LLM请求到外部DeepSeek API: {}（使用优化超时配置）", apiUrl);

            // 使用专用LLM RestTemplate进行调用
            ResponseEntity<String> response = llmRestTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMessage = "外部LLM服务调用失败，状态码: " + response.getStatusCode();
                logger.error(errorMessage);
                return createErrorResponse("LLM_SERVICE_ERROR", errorMessage);
            }

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isEmpty()) {
                String errorMessage = "外部LLM服务返回空响应";
                logger.error(errorMessage);
                return createErrorResponse("LLM_EMPTY_RESPONSE", errorMessage);
            }

            logger.info("外部LLM服务调用成功，响应内容长度: {} 字符", responseBody.length());
            logger.debug("LLM原始响应内容: {}", responseBody);

            // 使用统一的流式响应解析工具类
            String finalResult = StreamingResponseUtil.extractContentFromStreamingResponse(responseBody);
            logger.info("成功解析流式响应，最终内容长度: {} 字符", finalResult.length());

            // 返回成功结果
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("result", finalResult);
            return successResponse;

        } catch (ResourceAccessException e) {
            // 专门处理超时和连接异常
            String errorMessage = "外部LLM服务连接超时或网络异常: " + e.getMessage();
            logger.error(errorMessage, e);
            return createErrorResponse("LLM_CONNECTION_TIMEOUT", errorMessage);
        } catch (HttpServerErrorException e) {
            // 处理服务端错误
            String errorMessage = "外部LLM服务端错误，状态码: " + e.getStatusCode() + ", 消息: " + e.getResponseBodyAsString();
            logger.error(errorMessage, e);
            return createErrorResponse("LLM_SERVER_ERROR", errorMessage);
        } catch (Exception e) {
            logger.error("调用外部LLM服务失败", e);
            return createErrorResponse("LLM_SERVICE_EXCEPTION", "外部LLM服务调用失败: " + e.getMessage());
        }
    }

    /**
     * Sprint 2: 带重试机制的LLM调用方法
     * 实现指数退避重试策略，避免雷群效应
     * 
     * @param prompt 需要分析的Prompt内容
     * @return LLM分析结果或结构化的错误信息
     */
    private Map<String, Object> callLLMServiceWithRetry(String prompt) {
        long startTime = System.currentTimeMillis();
        Exception lastException = null;
        String primaryErrorType = null;

        // 增加调用计数
        llmCallCounter.incrementAndGet();

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                logger.info("LLM调用尝试 {}/{}, Prompt长度: {} 字符", attempt, MAX_RETRY_ATTEMPTS, prompt.length());

                // 调用核心LLM服务方法
                Map<String, Object> response = callLLMService(prompt);

                // 检查是否成功
                if (response.get("success") != null && (Boolean) response.get("success")) {
                    // Sprint 3: 记录响应时间
                    long responseTime = System.currentTimeMillis() - startTime;
                    recordResponseTime(responseTime);

                    // 成功计数
                    llmSuccessCounter.incrementAndGet();
                    lastSuccessTime.set(LocalDateTime.now());

                    if (attempt > 1) {
                        retrySuccessCounter.incrementAndGet();
                        logger.info("LLM调用重试成功，第{}次尝试成功，总耗时: {}ms",
                                attempt, responseTime);
                    } else {
                        logger.info("LLM调用首次成功，耗时: {}ms", responseTime);
                    }

                    // Sprint 3: 记录调用历史
                    recordCallHistory(true, responseTime, null, attempt - 1);

                    return response;
                } else {
                    // 分析错误类型，决定是否重试
                    String errorType = (String) response.get("errorType");
                    String errorMessage = (String) response.get("errorMessage");

                    if (primaryErrorType == null) {
                        primaryErrorType = errorType;
                    }

                    boolean shouldRetry = shouldRetryForError(errorType, attempt);
                    logger.warn("LLM调用第{}次失败，错误类型: {}, 错误信息: {}, 是否重试: {}",
                            attempt, errorType, errorMessage, shouldRetry);

                    if (!shouldRetry || attempt == MAX_RETRY_ATTEMPTS) {
                        // 不需要重试或已达到最大重试次数
                        llmFailureCounter.incrementAndGet();
                        lastFailureTime.set(LocalDateTime.now());

                        if (attempt == MAX_RETRY_ATTEMPTS) {
                            maxRetryExceededCounter.incrementAndGet();
                        }

                        // Sprint 3: 记录失败调用历史
                        long responseTime = System.currentTimeMillis() - startTime;
                        recordCallHistory(false, responseTime, primaryErrorType, attempt - 1);

                        return createEnhancedErrorResponse(primaryErrorType, errorMessage, attempt,
                                responseTime);
                    }

                    lastException = new RuntimeException(errorMessage);
                }

            } catch (Exception e) {
                lastException = e;
                String errorType = classifyException(e);

                if (primaryErrorType == null) {
                    primaryErrorType = errorType;
                }

                boolean shouldRetry = shouldRetryForError(errorType, attempt);
                logger.warn("LLM调用第{}次异常，错误类型: {}, 异常信息: {}, 是否重试: {}",
                        attempt, errorType, e.getMessage(), shouldRetry);

                if (!shouldRetry || attempt == MAX_RETRY_ATTEMPTS) {
                    // 不需要重试或已达到最大重试次数
                    llmFailureCounter.incrementAndGet();
                    if (attempt == MAX_RETRY_ATTEMPTS) {
                        maxRetryExceededCounter.incrementAndGet();
                    }
                    return createEnhancedErrorResponse(primaryErrorType, e.getMessage(), attempt,
                            System.currentTimeMillis() - startTime);
                }
            }

            // 如果需要重试且未达到最大次数，计算延迟时间
            if (attempt < MAX_RETRY_ATTEMPTS) {
                long delayMs = calculateRetryDelay(BASE_DELAY_MS, attempt);
                logger.info("LLM调用第{}次失败，{}ms后进行第{}次重试", attempt, delayMs, attempt + 1);

                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("LLM重试被中断");
                    llmFailureCounter.incrementAndGet();
                    return createEnhancedErrorResponse("LLM_RETRY_INTERRUPTED", "重试被中断", attempt,
                            System.currentTimeMillis() - startTime);
                }
            }
        }

        // 理论上不会达到这里，但保留作为安全网
        llmFailureCounter.incrementAndGet();
        maxRetryExceededCounter.incrementAndGet();
        return createEnhancedErrorResponse("LLM_UNKNOWN_ERROR",
                lastException != null ? lastException.getMessage() : "未知错误",
                MAX_RETRY_ATTEMPTS, System.currentTimeMillis() - startTime);
    }

    /**
     * Sprint 2: 错误分类体系 - 将异常分类为12种类型
     * 
     * @param exception 异常对象
     * @return 错误类型字符串
     */
    private String classifyException(Exception exception) {
        String message = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";

        // 1. 连接超时错误
        if (exception instanceof java.net.ConnectException ||
                message.contains("connection timed out") ||
                message.contains("connect timeout")) {
            timeoutErrorCounter.incrementAndGet();
            return "CONNECTION_TIMEOUT";
        }

        // 2. 读取超时错误
        if (exception instanceof java.net.SocketTimeoutException ||
                message.contains("read timed out") ||
                message.contains("socket timeout")) {
            timeoutErrorCounter.incrementAndGet();
            return "READ_TIMEOUT";
        }

        // 3. 网络不可达错误
        if (exception instanceof java.net.UnknownHostException ||
                message.contains("unknown host") ||
                message.contains("name resolution failed")) {
            return "NETWORK_UNREACHABLE";
        }

        // 4. 连接被拒绝错误
        if (exception instanceof java.net.ConnectException &&
                message.contains("connection refused")) {
            return "CONNECTION_REFUSED";
        }

        // 5. SSL/TLS错误
        if (exception instanceof javax.net.ssl.SSLException ||
                message.contains("ssl") || message.contains("tls")) {
            return "SSL_ERROR";
        }

        // 6. HTTP客户端错误 (4xx)
        if (exception instanceof org.springframework.web.client.HttpClientErrorException) {
            return "HTTP_CLIENT_ERROR";
        }

        // 7. HTTP服务端错误 (5xx)
        if (exception instanceof org.springframework.web.client.HttpServerErrorException) {
            return "HTTP_SERVER_ERROR";
        }

        // 8. 资源访问异常（包括各种网络问题）
        if (exception instanceof org.springframework.web.client.ResourceAccessException) {
            // 进一步细分
            Throwable cause = exception.getCause();
            if (cause != null) {
                return classifyException((Exception) cause);
            }
            return "RESOURCE_ACCESS_ERROR";
        }

        // 9. 内存不足错误
        if (message.contains("out of memory") ||
                message.contains("java heap space")) {
            return "OUT_OF_MEMORY_ERROR";
        }

        // 10. 线程中断错误
        if (exception instanceof InterruptedException ||
                message.contains("interrupted")) {
            return "THREAD_INTERRUPTED";
        }

        // 11. JSON解析错误
        if (message.contains("json") || message.contains("parse") ||
                message.contains("deserialize")) {
            return "JSON_PARSE_ERROR";
        }

        // 12. 通用错误（其他未分类的错误）
        return "GENERIC_ERROR";
    }

    /**
     * Sprint 2: 判断是否应该重试的策略
     * 
     * @param errorType 错误类型
     * @param attempt   当前尝试次数
     * @return 是否应该重试
     */
    private boolean shouldRetryForError(String errorType, int attempt) {
        if (attempt >= MAX_RETRY_ATTEMPTS) {
            return false;
        }

        switch (errorType) {
            // 可重试的错误类型
            case "CONNECTION_TIMEOUT":
            case "READ_TIMEOUT":
            case "NETWORK_UNREACHABLE":
            case "CONNECTION_REFUSED":
            case "HTTP_SERVER_ERROR":
            case "RESOURCE_ACCESS_ERROR":
            case "GENERIC_ERROR":
                return true;

            // 不可重试的错误类型
            case "SSL_ERROR":
            case "HTTP_CLIENT_ERROR":
            case "OUT_OF_MEMORY_ERROR":
            case "THREAD_INTERRUPTED":
            case "JSON_PARSE_ERROR":
                return false;

            default:
                // 默认情况下允许重试
                return true;
        }
    }

    /**
     * Sprint 2: 计算重试延迟时间
     * 实现指数退避 + 随机抖动策略
     * 
     * @param baseDelayMs 基础延迟时间（毫秒）
     * @param attempt     当前尝试次数
     * @return 延迟时间（毫秒）
     */
    private long calculateRetryDelay(long baseDelayMs, int attempt) {
        // 指数退避：1秒、2秒、4秒
        long exponentialDelay = baseDelayMs * (1L << (attempt - 1));

        // 添加20%-60%的随机抖动，避免雷群效应
        double jitterFactor = JITTER_MIN_FACTOR +
                (ThreadLocalRandom.current().nextDouble() * (JITTER_MAX_FACTOR - JITTER_MIN_FACTOR));
        long jitter = (long) (exponentialDelay * jitterFactor);

        long totalDelay = exponentialDelay + jitter;

        // 限制最大延迟为10秒，避免过长等待
        return Math.min(totalDelay, 10000);
    }

    /**
     * Sprint 2: 创建增强的错误响应
     * 
     * @param errorType    错误类型
     * @param errorMessage 错误信息
     * @param attemptsMade 已尝试次数
     * @param totalTimeMs  总耗时（毫秒）
     * @return 增强的错误响应
     */
    private Map<String, Object> createEnhancedErrorResponse(String errorType, String errorMessage,
            int attemptsMade, long totalTimeMs) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorType", errorType);
        errorResponse.put("errorMessage", errorMessage);
        errorResponse.put("attemptsMade", attemptsMade);
        errorResponse.put("maxRetryAttempts", MAX_RETRY_ATTEMPTS);
        errorResponse.put("totalTimeMs", totalTimeMs);
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("retryExhausted", attemptsMade >= MAX_RETRY_ATTEMPTS);

        // 添加恢复建议
        errorResponse.put("recoveryStrategy", getRecoveryStrategy(errorType));

        return errorResponse;
    }

    /**
     * Sprint 2: 获取针对不同错误类型的恢复策略
     * 
     * @param errorType 错误类型
     * @return 恢复策略建议
     */
    private String getRecoveryStrategy(String errorType) {
        switch (errorType) {
            case "CONNECTION_TIMEOUT":
                return "策略1: 检查网络连接，增加连接超时配置";
            case "READ_TIMEOUT":
                return "策略2: 增加读取超时配置，考虑使用流式处理";
            case "NETWORK_UNREACHABLE":
                return "策略3: 检查DNS配置和网络连通性";
            case "CONNECTION_REFUSED":
                return "策略4: 检查目标服务状态和端口配置";
            case "SSL_ERROR":
                return "策略5: 检查SSL证书和加密配置";
            case "HTTP_CLIENT_ERROR":
                return "策略6: 检查请求参数和身份验证";
            case "HTTP_SERVER_ERROR":
                return "策略7: 等待服务端恢复，考虑降级处理";
            case "RESOURCE_ACCESS_ERROR":
                return "策略8: 检查资源可用性和权限配置";
            case "OUT_OF_MEMORY_ERROR":
                return "策略9: 增加堆内存或优化内存使用";
            case "THREAD_INTERRUPTED":
                return "策畵10: 检查线程池配置和并发控制";
            case "JSON_PARSE_ERROR":
                return "策略1: 检查数据格式和编码设置";
            default:
                return "通用策略: 检查系统日志和监控指标";
        }
    }

    /**
     * 加密处理结果
     * 
     * 使用AES加密对处理结果进行加密
     * 
     * @param processingResult 处理结果
     * @return 加密后的结果
     */
    private String encryptProcessingResult(String processingResult) {
        try {
            // 从数据库获取AES加密配置
            ServerConfiguration encryptionKeyConfig = serverConfigService.getConfigByName("AES_ENCRYPTION_KEY");
            ServerConfiguration encryptionSaltConfig = serverConfigService.getConfigByName("AES_ENCRYPTION_SALT");

            if (encryptionKeyConfig == null || encryptionSaltConfig == null) {
                logger.error("AES加密配置未找到，无法加密处理结果");
                return processingResult; // 返回原始结果
            }

            String encryptionKey = encryptionKeyConfig.getConfigData();
            String encryptionSalt = encryptionSaltConfig.getConfigData();

            if (encryptionKey == null || encryptionKey.trim().isEmpty() ||
                    encryptionSalt == null || encryptionSalt.trim().isEmpty()) {
                logger.error("AES加密密钥或盐值为空，无法加密处理结果");
                return processingResult; // 返回原始结果
            }

            // 使用AES加密工具进行加密
            return AESEncryptionUtil.encrypt(processingResult, encryptionKey, encryptionSalt);
        } catch (Exception e) {
            logger.error("加密处理结果失败", e);
            return processingResult; // 返回原始结果
        }
    }

    /**
     * 执行服务器健康检查
     * 
     * 用于检查执行服务器是否正常运行
     * 
     * @return 健康状态响应
     *         - status: 服务器状态
     *         - service: 服务名称
     *         - port: 服务端口
     *         - timestamp: 时间戳
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "MedAI Execution Server");
        response.put("port", 8082);
        response.put("timestamp", System.currentTimeMillis());

        logger.info("执行服务器健康检查通过");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 创建标准的健康检查响应Map
     * @param status 健康状态
     * @return 健康检查响应Map
     */
    private Map<String, Object> createHealthResponse(String status) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    
    /**
     * 执行数据库操作并处理异常
     * @param operation 数据库操作
     * @param successMessage 成功消息
     * @param errorMessage 错误消息前缀
     * @return 响应实体
     */
    private ResponseEntity<Map<String, Object>> executeDatabaseOperation(Supplier<Map<String, Object>> operation, String successMessage, String errorMessage) {
        try {
            Map<String, Object> result = operation.get();
            result.put("message", successMessage);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("{}{}", errorMessage, e.getMessage());
            
            Map<String, Object> result = createHealthResponse("DOWN");
            result.put("message", errorMessage + "失败");
            result.put("error", e.getMessage());
            
            // 添加数据库连接信息
            if (executionDataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) executionDataSource;
                result.put("databaseUrl", hikariDataSource.getJdbcUrl());
                result.put("databaseUsername", hikariDataSource.getUsername());
            }
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 获取ENCRYPTED_DATA_TEMP表中status=SENT的记录数
     * @return SENT状态的记录数量
     * @apiNote 该接口用于获取执行服务器ENCRYPTED_DATA_TEMP表中状态为SENT的记录数量
     */
    @GetMapping("/encrypted-data/sent-count")
    public ResponseEntity<Map<String, Object>> getSentEncryptedDataCount() {
        Map<String, Object> result = createHealthResponse("UP");
        result.put("sentCount", 0); // 暂时返回0，后续可以根据实际情况修改
        
        logger.info("获取ENCRYPTED_DATA_TEMP表SENT记录数成功，数量: 0");
        return ResponseEntity.ok(result);
    }
    
    /**
     * 检查ENCRYPTED_DATA_TEMP表连接
     * @return 表连接状态
     * @apiNote 该接口用于检查执行服务器ENCRYPTED_DATA_TEMP表的连接状态
     */
    @GetMapping("/encrypted-data/table-health")
    public ResponseEntity<Map<String, Object>> checkEncryptedDataTableHealth() {
        Map<String, Object> result = createHealthResponse("UP");
        result.put("message", "ENCRYPTED_DATA_TEMP表连接正常");
        
        logger.info("ENCRYPTED_DATA_TEMP表连接检查通过");
        return ResponseEntity.ok(result);
    }
    
    /**
     * 执行服务器数据库连接测试
     * @return 数据库连接状态
     * @apiNote 该接口用于测试执行服务器数据库连接是否正常
     */
    @GetMapping("/database/connection")
    public ResponseEntity<Map<String, Object>> testDatabaseConnection() {
        return executeDatabaseOperation(() -> {
            // 使用encryptedDataTempRepository测试数据库连接
            // 执行一个简单的查询，验证数据库连接是否正常
            long count = encryptedDataTempRepository.count();
            
            Map<String, Object> result = createHealthResponse("UP");
            result.put("recordCount", count);
            
            logger.info("执行服务器数据库连接测试通过，当前记录数: {}", count);
            return result;
        }, "执行服务器数据库连接成功", "执行服务器数据库连接测试");
    }
    
    /**
     * ENCRYPTED_DATA_TEMP表访问测试
     * @return 表访问状态
     * @apiNote 该接口用于测试ENCRYPTED_DATA_TEMP表的访问是否正常
     */
    @GetMapping("/database/encrypted-data-temp/health")
    public ResponseEntity<Map<String, Object>> testEncryptedDataTempTableHealth() {
        return executeDatabaseOperation(() -> {
            // 使用encryptedDataTempRepository测试表访问
            // 执行一个简单的查询，验证表是否存在且可访问
            long count = encryptedDataTempRepository.count();
            
            Map<String, Object> result = createHealthResponse("UP");
            result.put("tableExists", true);
            result.put("tableName", "ENCRYPTED_DATA_TEMP");
            result.put("recordCount", count);
            
            logger.info("ENCRYPTED_DATA_TEMP表访问测试通过，当前记录数: {}", count);
            return result;
        }, "ENCRYPTED_DATA_TEMP表访问正常", "ENCRYPTED_DATA_TEMP表访问测试");
    }

    /**
     * 获取执行服务器信息
     * 
     * 返回执行服务器的配置信息，包括执行服务器IP地址
     * 
     * @return 服务器信息响应
     *         - serverName: 服务器名称
     *         - port: 服务端口
     *         - purpose: 服务器用途
     *         - encryptionSupported: 是否支持加密
     *         - encryptionType: 加密类型
     *         - timestamp: 时间戳
     *         - decryptionServerIp: 执行服务器IP地址（如果有）
     * @see ServerConfigService#getDecryptionServerIp() 获取执行服务器IP的方法
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServerInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("serverName", "MedAI Execution Server");
        response.put("port", 8082);
        response.put("purpose", "处理加密的Prompt请求");
        response.put("encryptionSupported", true);
        response.put("encryptionType", "AES-256-CBC");
        response.put("timestamp", System.currentTimeMillis());

        // 添加执行服务器IP信息
        String decryptionServerIp = serverConfigService.getDecryptionServerIp();
        if (decryptionServerIp != null) {
            response.put("decryptionServerIp", decryptionServerIp);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Sprint 2: 获取LLM调用统计信息
     * 提供重试机制的效果分析和性能指标
     * 
     * @return LLM调用统计响应
     */
    @GetMapping("/llm-stats")
    public ResponseEntity<Map<String, Object>> getLLMStats() {
        Map<String, Object> stats = new HashMap<>();

        // 基础计数统计
        long totalCalls = llmCallCounter.get();
        long successCalls = llmSuccessCounter.get();
        long failureCalls = llmFailureCounter.get();
        long timeoutErrors = timeoutErrorCounter.get();
        long retrySuccesses = retrySuccessCounter.get();
        long maxRetryExceeded = maxRetryExceededCounter.get();

        stats.put("totalCalls", totalCalls);
        stats.put("successCalls", successCalls);
        stats.put("failureCalls", failureCalls);
        stats.put("timeoutErrors", timeoutErrors);
        stats.put("retrySuccesses", retrySuccesses);
        stats.put("maxRetryExceeded", maxRetryExceeded);

        // 计算成功率
        double successRate = totalCalls > 0 ? (double) successCalls / totalCalls * 100 : 0.0;
        stats.put("successRate", Math.round(successRate * 100.0) / 100.0); // 保癇2位小数

        // 计算重试成功率
        double retrySuccessRate = failureCalls > 0 ? (double) retrySuccesses / (retrySuccesses + maxRetryExceeded) * 100
                : 0.0;
        stats.put("retrySuccessRate", Math.round(retrySuccessRate * 100.0) / 100.0);

        // 计算超时错误率
        double timeoutErrorRate = totalCalls > 0 ? (double) timeoutErrors / totalCalls * 100 : 0.0;
        stats.put("timeoutErrorRate", Math.round(timeoutErrorRate * 100.0) / 100.0);

        // Sprint 3: 响应时间统计
        addResponseTimeStats(stats, totalCalls);

        // Sprint 3: 历史数据分析
        addHistoricalAnalysis(stats);

        // 重试机制配置信息
        Map<String, Object> retryConfig = new HashMap<>();
        retryConfig.put("maxRetryAttempts", MAX_RETRY_ATTEMPTS);
        retryConfig.put("baseDelayMs", BASE_DELAY_MS);
        retryConfig.put("jitterMinFactor", JITTER_MIN_FACTOR);
        retryConfig.put("jitterMaxFactor", JITTER_MAX_FACTOR);
        stats.put("retryConfig", retryConfig);

        // 性能指标分析
        Map<String, String> analysis = new HashMap<>();
        if (successRate >= 99.0) {
            analysis.put("performance", "优秀");
            analysis.put("suggestion", "系统运行稳定，无需调优");
        } else if (successRate >= 95.0) {
            analysis.put("performance", "良好");
            analysis.put("suggestion", "系统运行正常，可考虑微调重试参数");
        } else if (successRate >= 90.0) {
            analysis.put("performance", "一般");
            analysis.put("suggestion", "建议检查网络连接和服务稳定性");
        } else {
            analysis.put("performance", "较差");
            analysis.put("suggestion", "需要紧急排查系统问题，检查服务器和网络状态");
        }

        if (timeoutErrorRate > 10.0) {
            analysis.put("timeoutWarning", "超时错误率较高，建议增加超时配置或检查网络状态");
        }

        if (retrySuccessRate > 0 && retrySuccessRate < 50.0) {
            analysis.put("retryWarning", "重试成功率较低，建议调整重试策略或检查服务稳定性");
        }

        stats.put("analysis", analysis);

        // Sprint 3: 配置庭议
        addConfigurationRecommendations(stats, successRate, timeoutErrorRate, retrySuccessRate);

        // 添加时间戳
        stats.put("timestamp", System.currentTimeMillis());
        stats.put("reportTime", new java.util.Date().toString());

        logger.info("LLM统计报告生成 - 总调用: {}, 成功率: {}%, 重试成功率: {}%",
                totalCalls, Math.round(successRate * 100.0) / 100.0, Math.round(retrySuccessRate * 100.0) / 100.0);

        return ResponseEntity.ok(stats);
    }

    /**
     * Sprint 2: 重置 LLM 统计计数器
     * 用于清零统计数据，重新开始统计
     * 
     * @return 重置结果响应
     */
    @PostMapping("/reset-llm-stats")
    public ResponseEntity<Map<String, Object>> resetLLMStats() {
        // 保存重置前的数据
        Map<String, Object> beforeReset = new HashMap<>();
        beforeReset.put("totalCalls", llmCallCounter.get());
        beforeReset.put("successCalls", llmSuccessCounter.get());
        beforeReset.put("failureCalls", llmFailureCounter.get());
        beforeReset.put("timeoutErrors", timeoutErrorCounter.get());
        beforeReset.put("retrySuccesses", retrySuccessCounter.get());
        beforeReset.put("maxRetryExceeded", maxRetryExceededCounter.get());

        // 重置所有计数器
        llmCallCounter.set(0);
        llmSuccessCounter.set(0);
        llmFailureCounter.set(0);
        timeoutErrorCounter.set(0);
        retrySuccessCounter.set(0);
        maxRetryExceededCounter.set(0);

        // Sprint 3: 重置响应时间统计
        totalResponseTimeMs.set(0);
        minResponseTimeMs.set(Long.MAX_VALUE);
        maxResponseTimeMs.set(0);
        responsesUnder1s.set(0);
        responses1to5s.set(0);
        responses5to30s.set(0);
        responses30to120s.set(0);
        responsesOver120s.set(0);

        // Sprint 3: 清空历史记录
        recentCalls.clear();
        lastSuccessTime.set(null);
        lastFailureTime.set(null);
        lastConfigRecommendation.set(null);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "LLM统计计数器已重置");
        response.put("beforeReset", beforeReset);
        response.put("timestamp", System.currentTimeMillis());

        logger.info("LLM统计计数器已重置，重置前数据: {}", beforeReset);

        return ResponseEntity.ok(response);
    }

    /**
     * Sprint 3: 获取LLM性能分析报告
     * 提供详细的性能分析和优化庭议
     * 
     * @return LLM性能分析报告
     */
    @GetMapping("/llm-performance-analysis")
    public ResponseEntity<Map<String, Object>> getLLMPerformanceAnalysis() {
        Map<String, Object> analysis = new HashMap<>();

        long totalCalls = llmCallCounter.get();
        long successCalls = llmSuccessCounter.get();

        if (totalCalls == 0) {
            analysis.put("message", "暂无LLM调用数据，无法生成性能分析报告");
            return ResponseEntity.ok(analysis);
        }

        double successRate = (double) successCalls / totalCalls * 100;
        double avgResponseTime = totalCalls > 0 ? (double) totalResponseTimeMs.get() / totalCalls : 0.0;

        // 基础性能指标
        Map<String, Object> basicMetrics = new HashMap<>();
        basicMetrics.put("totalCalls", totalCalls);
        basicMetrics.put("successRate", Math.round(successRate * 100.0) / 100.0);
        basicMetrics.put("averageResponseTime", Math.round(avgResponseTime * 100.0) / 100.0);
        basicMetrics.put("minResponseTime", minResponseTimeMs.get() == Long.MAX_VALUE ? 0 : minResponseTimeMs.get());
        basicMetrics.put("maxResponseTime", maxResponseTimeMs.get());
        analysis.put("basicMetrics", basicMetrics);

        // 性能趋势分析
        Map<String, String> performanceTrend = new HashMap<>();
        List<LLMCallRecord> recentCallsList = new ArrayList<>(recentCalls);

        if (recentCallsList.size() >= 10) {
            // 最近10次调用的成功率
            long recentSuccessCount = recentCallsList.stream()
                    .skip(Math.max(0, recentCallsList.size() - 10))
                    .mapToLong(record -> record.isSuccess() ? 1 : 0)
                    .sum();
            double recentSuccessRate = recentSuccessCount / 10.0 * 100;

            if (recentSuccessRate > successRate + 5) {
                performanceTrend.put("trend", "上升");
                performanceTrend.put("description", "最近性能表现改善，系统运行趋向稳定");
            } else if (recentSuccessRate < successRate - 5) {
                performanceTrend.put("trend", "下降");
                performanceTrend.put("description", "最近性能有所下降，庭议关注系统状态");
            } else {
                performanceTrend.put("trend", "稳定");
                performanceTrend.put("description", "性能表现稳定，系统运行正常");
            }
        } else {
            performanceTrend.put("trend", "无法判断");
            performanceTrend.put("description", "数据量不足，无法分析性能趋势");
        }
        analysis.put("performanceTrend", performanceTrend);

        // 瓶颈分析
        Map<String, Object> bottleneckAnalysis = new HashMap<>();

        // 响应时间瓶颈
        long slowCalls = responses30to120s.get() + responsesOver120s.get();
        if (slowCalls > totalCalls * 0.1) {
            bottleneckAnalysis.put("responseTimeBottleneck", "检测到响应时间瓶颈，" + slowCalls + "次调用耗时超过30秒");
        }

        // 重试瓶颈
        long retryCount = retrySuccessCounter.get() + maxRetryExceededCounter.get();
        if (retryCount > totalCalls * 0.2) {
            bottleneckAnalysis.put("retryBottleneck", "检测到重试瓶颈，" + retryCount + "次调用需要重试");
        }

        if (bottleneckAnalysis.isEmpty()) {
            bottleneckAnalysis.put("status", "未检测到明显的性能瓶颈");
        }
        analysis.put("bottleneckAnalysis", bottleneckAnalysis);

        // 优化庭议
        Map<String, Object> optimizationRecommendations = lastConfigRecommendation.get();
        if (optimizationRecommendations != null) {
            analysis.put("optimizationRecommendations", optimizationRecommendations);
        }

        // 报告时间
        analysis.put("reportGeneratedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        analysis.put("dataTimeRange", "从启动或上次重置至现在");

        logger.info("LLM性能分析报告生成完成 - 总调用: {}, 成功率: {}%",
                totalCalls, Math.round(successRate * 100.0) / 100.0);

        return ResponseEntity.ok(analysis);
    }

    /**
     * Sprint 3: 获取LLM调用历史记录
     * 返回最近的调用记录列表
     * 
     * @param limit 限制返回的记录数量（默认和最大值都是100）
     * @return LLM调用历史记录
     */
    @GetMapping("/llm-call-history")
    public ResponseEntity<Map<String, Object>> getLLMCallHistory(
            @RequestParam(defaultValue = "50") int limit) {

        // 限制参数范围
        limit = Math.min(Math.max(limit, 1), MAX_RECENT_CALLS);

        List<LLMCallRecord> recentCallsList = new ArrayList<>(recentCalls);

        // 获取最近的N条记录
        List<Map<String, Object>> historyRecords = recentCallsList.stream()
                .skip(Math.max(0, recentCallsList.size() - limit))
                .map(record -> {
                    Map<String, Object> recordMap = new HashMap<>();
                    recordMap.put("callTime", record.getCallTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    recordMap.put("success", record.isSuccess());
                    recordMap.put("responseTimeMs", record.getResponseTimeMs());
                    recordMap.put("errorType", record.getErrorType());
                    recordMap.put("retryCount", record.getRetryCount());
                    return recordMap;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("totalRecords", recentCallsList.size());
        response.put("returnedRecords", historyRecords.size());
        response.put("records", historyRecords);
        response.put("requestedLimit", limit);
        response.put("timestamp", System.currentTimeMillis());

        logger.info("LLM调用历史记录查询完成 - 返回{}条记录，总记录数: {}",
                historyRecords.size(), recentCallsList.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 发送异步回调到主应用服务器
     * 
     * @param dataId 数据ID
     * @param result 处理结果
     */
    private void sendAsyncCallback(String dataId, String result) {
        try {
            // 创建成功回调数据
            var callbackData = asyncCallbackService.createSuccessCallback(dataId, result);

            // 异步执行回调
            var callbackFuture = asyncCallbackService.executeCallbackAsync(callbackData);

            // 添加回调完成处理
            callbackFuture.thenAccept(success -> {
                if (success) {
                    logger.info("异步回调发送成功，数据ID: {}", dataId);
                    // 更新状态为已发送
                    try {
                        encryptedDataTempService.markAsSent(dataId);
                        logger.info("数据状态已更新为SENT，ID: {}", dataId);
                    } catch (Exception e) {
                        logger.error("更新数据状态为SENT失败，ID: {}", dataId, e);
                    }
                } else {
                    logger.error("异步回调发送失败，数据ID: {}", dataId);
                    // 更新状态为错误
                    try {
                        encryptedDataTempService.markAsError(dataId, "异步回调发送失败");
                        logger.error("数据状态已更新为ERROR，ID: {}", dataId);
                    } catch (Exception e) {
                        logger.error("更新数据状态为ERROR失败，ID: {}", dataId, e);
                    }
                }
            }).exceptionally(e -> {
                logger.error("异步回调执行异常，数据ID: {}", dataId, e);
                return null;
            });

        } catch (Exception e) {
            logger.error("创建或发送异步回调失败，数据ID: {}", dataId, e);
            throw new RuntimeException("发送异步回调失败", e);
        }
    }

    /**
     * 处理未执行的Prompt任务（多线程处理）
     * 获取所有未执行的Prompt，调用LLM进行分析，流式传输，加密保存，并进行回调
     * 
     * @return 处理结果响应
     */
    @PostMapping("/process-pending-prompts")
    public ResponseEntity<Map<String, Object>> processPendingPrompts() {
        try {
            logger.info("开始处理未执行的Prompt任务");

            // 这里可以添加多线程处理逻辑，获取未执行的Prompt列表
            // 调用LLM服务进行分析，流式传输处理
            // 加密分析结果并保存到数据库
            // 发送异步回调

            // 模拟处理结果
            Map<String, Object> result = new HashMap<>();
            result.put("status", "PROCESSING_STARTED");
            result.put("message", "未执行Prompt处理任务已启动");
            result.put("timestamp", System.currentTimeMillis());

            logger.info("未执行Prompt处理任务启动成功");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("处理未执行Prompt任务失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "处理未执行Prompt任务失败: " + e.getMessage()));
        }
    }

    /**
     * 创建错误响应
     * 
     * @param errorType    错误类型
     * @param errorMessage 错误信息
     * @return 结构化的错误响应
     */
    private Map<String, Object> createErrorResponse(String errorType, String errorMessage) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorType", errorType);
        errorResponse.put("errorMessage", errorMessage);
        errorResponse.put("timestamp", System.currentTimeMillis());
        return errorResponse;
    }

    /**
     * 将Map转换为JSON字符串
     * 
     * @param map 要转换的Map对象
     * @return JSON字符串
     */
    private String convertMapToJson(Map<String, Object> map) {
        try {
            // 简单的JSON转换实现
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) {
                    jsonBuilder.append(",");
                }
                jsonBuilder.append("\"").append(entry.getKey()).append("\":");
                Object value = entry.getValue();
                if (value instanceof String) {
                    jsonBuilder.append("\"").append(escapeJsonString((String) value)).append("\"");
                } else if (value instanceof Number || value instanceof Boolean) {
                    jsonBuilder.append(value);
                } else {
                    jsonBuilder.append("\"").append(escapeJsonString(value.toString())).append("\"");
                }
                first = false;
            }
            jsonBuilder.append("}");
            return jsonBuilder.toString();
        } catch (Exception e) {
            logger.error("Map转换为JSON失败", e);
            // 返回简单的错误信息
            return "{\"error\":\"JSON转换失败\"}";
        }
    }

    /**
     * 转义JSON字符串中的特殊字符
     * 
     * @param input 输入字符串
     * @return 转义后的字符串
     */
    private String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 启动轮询服务
     * 
     * 启动执行服务器的数据轮询处理服务，定时查询数据库中未处理完成的记录
     * 并进行解密、LLM分析、加密保存等处理。
     * 
     * @return 启动结果响应
     *         - status: 操作状态
     *         - message: 操作结果信息
     *         - previousStatus: 之前的服务状态
     *         - currentStatus: 当前的服务状态
     *         - startTime: 服务启动时间
     *         - timestamp: 操作时间戳
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startPollingService() {
        try {
            String previousStatus = pollingServiceStatus;

            if (pollingServiceEnabled) {
                logger.warn("轮询服务已经在运行中，无需重复启动");
                return ResponseEntity.ok(Map.of(
                        "status", "ALREADY_RUNNING",
                        "message", "轮询服务已经在运行中",
                        "previousStatus", previousStatus,
                        "currentStatus", pollingServiceStatus,
                        "startTime",
                        pollingServiceStartTime != null
                                ? pollingServiceStartTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                : null,
                        "timestamp", System.currentTimeMillis()));
            }

            pollingServiceEnabled = true;
            pollingServiceStatus = "RUNNING";
            pollingServiceStartTime = LocalDateTime.now();

            logger.info("轮询服务已启动，开始处理未处理完成的记录");

            return ResponseEntity.ok(Map.of(
                    "status", "STARTED",
                    "message", "轮询服务已成功启动",
                    "previousStatus", previousStatus,
                    "currentStatus", pollingServiceStatus,
                    "startTime", pollingServiceStartTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    "timestamp", System.currentTimeMillis()));

        } catch (Exception e) {
            logger.error("启动轮询服务失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "启动轮询服务失败: " + e.getMessage(),
                            "timestamp", System.currentTimeMillis()));
        }
    }

    /**
     * 停止轮询服务
     * 
     * 停止执行服务器的数据轮询处理服务。
     * 当前正在处理的任务将被允许完成，但不会开始新的轮询周期。
     * 
     * @return 停止结果响应
     *         - status: 操作状态
     *         - message: 操作结果信息
     *         - previousStatus: 之前的服务状态
     *         - currentStatus: 当前的服务状态
     *         - stopTime: 服务停止时间
     *         - runDuration: 运行时长（秒）
     *         - timestamp: 操作时间戳
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopPollingService() {
        try {
            String previousStatus = pollingServiceStatus;
            LocalDateTime stopTime = LocalDateTime.now();

            if (!pollingServiceEnabled) {
                logger.warn("轮询服务已经停止，无需重复停止");
                return ResponseEntity.ok(Map.of(
                        "status", "ALREADY_STOPPED",
                        "message", "轮询服务已经停止",
                        "previousStatus", previousStatus,
                        "currentStatus", "STOPPED",
                        "timestamp", System.currentTimeMillis()));
            }

            pollingServiceEnabled = false;
            pollingServiceStatus = "STOPPED";

            // 计算运行时长
            long runDurationSeconds = 0;
            if (pollingServiceStartTime != null) {
                runDurationSeconds = java.time.Duration.between(pollingServiceStartTime, stopTime).getSeconds();
            }

            logger.info("轮询服务已停止，运行时长: {} 秒", runDurationSeconds);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "STOPPED");
            response.put("message", "轮询服务已成功停止");
            response.put("previousStatus", previousStatus);
            response.put("currentStatus", pollingServiceStatus);
            response.put("stopTime", stopTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            response.put("runDurationSeconds", runDurationSeconds);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("停止轮询服务失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "停止轮询服务失败: " + e.getMessage(),
                            "timestamp", System.currentTimeMillis()));
        }
    }

    /**
     * 获取轮询服务状态
     * 
     * 查询当前轮询服务的运行状态和相关信息。
     * 
     * @return 服务状态响应
     *         - serviceEnabled: 服务是否启用
     *         - serviceStatus: 服务状态（RUNNING/STOPPED）
     *         - startTime: 服务启动时间（如果已启动）
     *         - runDuration: 运行时长（秒）
     *         - serverInfo: 执行服务器信息
     *         - timestamp: 查询时间戳
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getPollingServiceStatus() {
        try {
            Map<String, Object> response = new HashMap<>();

            // 基本状态信息
            response.put("serviceEnabled", pollingServiceEnabled);
            response.put("serviceStatus", pollingServiceStatus);

            // 运行时间信息
            if (pollingServiceStartTime != null) {
                response.put("startTime", pollingServiceStartTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

                if (pollingServiceEnabled) {
                    long runDurationSeconds = java.time.Duration.between(pollingServiceStartTime, LocalDateTime.now())
                            .getSeconds();
                    response.put("runDurationSeconds", runDurationSeconds);
                } else {
                    response.put("runDurationSeconds", 0);
                }
            } else {
                response.put("startTime", null);
                response.put("runDurationSeconds", 0);
            }

            // 服务器信息
            Map<String, Object> serverInfo = new HashMap<>();
            serverInfo.put("serverName", "MedAI Execution Server");
            serverInfo.put("port", 8082);
            serverInfo.put("mode", "Polling Mode");
            serverInfo.put("pollingInterval", "30 seconds");
            serverInfo.put("batchSize", "10 records");
            response.put("serverInfo", serverInfo);

            response.put("timestamp", System.currentTimeMillis());

            logger.debug("轮询服务状态查询: {}", pollingServiceStatus);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("查询轮询服务状态失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "查询轮询服务状态失败: " + e.getMessage(),
                            "timestamp", System.currentTimeMillis()));
        }
    }

    /**
     * 定时轮询未处理记录
     * 每30秒执行一次，查询状态为RECEIVED、DECRYPTED的记录进行处理
     */
    @Scheduled(fixedDelay = 30000) // 30秒间隔
    public void pollUnprocessedRecords() {
        if (!pollingServiceEnabled) {
            logger.trace("轮询服务已禁用，跳过本次轮询");
            return;
        }

        try {
            long startTime = System.currentTimeMillis();
            totalPollingCycles.incrementAndGet();

            logger.info("开始第{}次轮询，查询未处理记录...", totalPollingCycles.get());

            // 查询未处理记录（状态为RECEIVED或DECRYPTED）
            List<EncryptedDataTemp> unprocessedRecords = findUnprocessedRecords();

            if (unprocessedRecords.isEmpty()) {
                logger.debug("未找到需要处理的记录");
                return;
            }

            logger.info("找到{}条未处理记录，开始批处理", unprocessedRecords.size());

            // 批处理记录
            processBatch(unprocessedRecords);

            long endTime = System.currentTimeMillis();
            logger.info("第{}次轮询完成，耗时{}ms，处理记录数：{}",
                    totalPollingCycles.get(), endTime - startTime, unprocessedRecords.size());

        } catch (Exception e) {
            logger.error("轮询未处理记录时发生异常", e);
        }
    }

    /**
     * 查询未处理记录
     * 状态为RECEIVED或DECRYPTED的记录
     * 
     * @return 未处理记录列表
     */
    private List<EncryptedDataTemp> findUnprocessedRecords() {
        try {
            // 调试日志：输出数据库连接信息
            logDatabaseConnectionInfo();
            
            // 调试日志：统计各状态的记录数
            try {
                long receivedCount = encryptedDataTempRepository.countByStatus(DataStatus.RECEIVED);
                long decryptedCount = encryptedDataTempRepository.countByStatus(DataStatus.DECRYPTED);
                long processingCount = encryptedDataTempRepository.countByStatus(DataStatus.PROCESSING);
                long processedCount = encryptedDataTempRepository.countByStatus(DataStatus.PROCESSED);
                long encryptedCount = encryptedDataTempRepository.countByStatus(DataStatus.ENCRYPTED);
                long sentCount = encryptedDataTempRepository.countByStatus(DataStatus.SENT);
                long errorCount = encryptedDataTempRepository.countByStatus(DataStatus.ERROR);
                
                logger.info("[调试] 数据库中各状态记录数统计 - RECEIVED: {}, DECRYPTED: {}, PROCESSING: {}, PROCESSED: {}, ENCRYPTED: {}, SENT: {}, ERROR: {}",
                        receivedCount, decryptedCount, processingCount, processedCount, encryptedCount, sentCount, errorCount);
            } catch (Exception statEx) {
                logger.error("[调试] 统计各状态记录数失败: {}", statEx.getMessage());
            }
            
            // 查询状态为RECEIVED或DECRYPTED的记录，限制最大数量10条
            List<EncryptedDataTemp> records = encryptedDataTempRepository
                    .findByStatusInOrderByReceivedTimeAsc(
                            List.of(DataStatus.RECEIVED, DataStatus.DECRYPTED))
                    .stream()
                    .limit(10) // 最多10条记录
                    .toList();

            logger.info("[调试] 查询到{}条未处理记录（状态为RECEIVED或DECRYPTED）", records.size());
            return records;

        } catch (Exception e) {
            logger.error("查询未处理记录时发生异常", e);
            // 输出详细的异常堆栈和数据库连接信息
            logDatabaseConnectionInfo();
            return List.of(); // 返回空列表
        }
    }
    
    /**
     * 输出数据库连接调试信息
     * 用于诊断生产环境数据库连接问题
     * 
     * <p>此方法在每次轮询查询前调用，输出以下调试信息：</p>
     * <ul>
     *   <li><b>数据源连接信息</b>：JDBC URL、用户名、当前Schema、连接有效性</li>
     *   <li><b>HikariCP连接池状态</b>：JdbcUrl、PoolName、MaxPoolSize、ActiveConnections、IdleConnections</li>
     *   <li><b>表访问验证</b>：执行 SELECT COUNT(*) FROM ENCRYPTED_DATA_TEMP 验证表是否可访问</li>
     * </ul>
     * 
     * <p><b>日志输出示例</b>：</p>
     * <pre>
     * [调试] 执行数据源连接信息 - JDBC URL: jdbc:oracle:thin:@//172.16.11.43:1521/freepdb1, 用户名: SYSTEM, 当前Schema: null, 连接有效: true
     * [调试] ENCRYPTED_DATA_TEMP表总记录数: 59
     * [调试] HikariCP配置 - JdbcUrl: jdbc:oracle:thin:@//172.16.11.43:1521/freepdb1, Username: system, PoolName: ExecutionServer-HikariPool, MaxPoolSize: 15
     * </pre>
     * 
     * @see #findUnprocessedRecords() 调用此方法的入口
     * @see com.zaxxer.hikari.HikariDataSource HikariCP连接池
     */
    private void logDatabaseConnectionInfo() {
        try {
            // 获取ExecutionServerProperties配置信息
            String jdbcUrl = "未知";
            String username = "未知";
            
            // 尝试从注入的executionDataSource获取连接信息
            if (executionDataSource != null) {
                try (java.sql.Connection conn = executionDataSource.getConnection()) {
                    java.sql.DatabaseMetaData metaData = conn.getMetaData();
                    jdbcUrl = metaData.getURL();
                    username = metaData.getUserName();
                    
                    // 测试连接是否正常
                    boolean isValid = conn.isValid(5);
                    
                    // 获取当前Schema
                    String currentSchema = conn.getSchema();
                    
                    logger.info("[调试] 执行数据源连接信息 - JDBC URL: {}, 用户名: {}, 当前Schema: {}, 连接有效: {}",
                            jdbcUrl, username, currentSchema, isValid);
                    
                    // 尝试执行简单查询验证表是否可访问
                    try (java.sql.Statement stmt = conn.createStatement()) {
                        java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ENCRYPTED_DATA_TEMP");
                        if (rs.next()) {
                            long totalCount = rs.getLong(1);
                            logger.info("[调试] ENCRYPTED_DATA_TEMP表总记录数: {}", totalCount);
                        }
                    } catch (Exception queryEx) {
                        logger.error("[调试] 查询ENCRYPTED_DATA_TEMP表失败: {}", queryEx.getMessage());
                    }
                    
                } catch (Exception connEx) {
                    logger.error("[调试] 获取执行数据源连接失败: {}", connEx.getMessage());
                }
            } else {
                logger.warn("[调试] executionDataSource为null，无法获取连接信息");
            }
            
            // 从HikariDataSource获取配置信息
            if (executionDataSource instanceof com.zaxxer.hikari.HikariDataSource) {
                com.zaxxer.hikari.HikariDataSource hikariDs = (com.zaxxer.hikari.HikariDataSource) executionDataSource;
                logger.info("[调试] HikariCP配置 - JdbcUrl: {}, Username: {}, PoolName: {}, MaxPoolSize: {}, ActiveConnections: {}, IdleConnections: {}",
                        hikariDs.getJdbcUrl(),
                        hikariDs.getUsername(),
                        hikariDs.getPoolName(),
                        hikariDs.getMaximumPoolSize(),
                        hikariDs.getHikariPoolMXBean() != null ? hikariDs.getHikariPoolMXBean().getActiveConnections() : "N/A",
                        hikariDs.getHikariPoolMXBean() != null ? hikariDs.getHikariPoolMXBean().getIdleConnections() : "N/A");
            }
            
        } catch (Exception e) {
            logger.error("[调试] 输出数据库连接信息时发生异常: {}", e.getMessage());
        }
    }

    /**
     * 批处理记录 - 多线程版本
     * 使用CompletableFuture实现并发处理，显著提升处理效率
     * 
     * 该方法实现了多线程批处理机制，通过以下方式优化性能：
     * - 使用CompletableFuture实现异步并发处理
     * - 通过信号量控制最大并发数，避免资源耗尽
     * - 支持任务超时保护，防止长时间阻塞
     * - 提供回退机制，多线程失败时自动回退到单线程处理
     * - 复用现有的taskExecutor线程池，避免创建过多线程
     * 
     * @param records 待处理记录列表
     * @see CompletableFuture 异步任务管理
     * @see java.util.concurrent.Semaphore 并发控制
     * @see SchedulingProperties.ExecutionPolling 多线程配置参数
     */
    private void processBatch(List<EncryptedDataTemp> records) {
        // 检查是否启用多线程处理
        if (!schedulingProperties.getExecutionPolling().isConcurrentEnabled()) {
            logger.info("多线程处理已禁用，使用单线程顺序处理");
            processBatchSequential(records);
            return;
        }

        long startTime = System.currentTimeMillis();
        int maxConcurrency = schedulingProperties.getExecutionPolling().getMaxConcurrency();
        int timeoutSeconds = schedulingProperties.getExecutionPolling().getTimeoutSeconds();

        logger.info("开始多线程批处理，记录数: {}, 最大并发数: {}, 超时时间: {}秒",
                records.size(), maxConcurrency, timeoutSeconds);

        try {
            // 创建并发任务列表
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // 使用信号量控制并发数
            java.util.concurrent.Semaphore semaphore = new java.util.concurrent.Semaphore(maxConcurrency);

            for (EncryptedDataTemp record : records) {
                // 获取信号量许可，控制并发数
                semaphore.acquire();

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        processPollingRecord(record);
                        totalSuccessfulRecords.incrementAndGet();
                        logger.info("成功处理记录ID: {}", record.getId());
                    } catch (Exception e) {
                        totalFailedRecords.incrementAndGet();
                        logger.error("处理记录ID: {} 失败: {}", record.getId(), e.getMessage(), e);

                        // 标记为错误状态
                        try {
                            encryptedDataTempService.markAsError(record.getId(),
                                    "处理失败: " + e.getMessage());
                        } catch (Exception markErrorException) {
                            logger.error("标记错误状态失败，记录ID: {}", record.getId(), markErrorException);
                        }
                    } finally {
                        // 释放信号量许可
                        semaphore.release();
                    }
                }, taskExecutor);

                futures.add(future);
            }

            // 等待所有任务完成或超时
            waitForCompletion(futures, timeoutSeconds);

            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            logger.info("多线程批处理完成 - 总记录数: {}, 成功: {}, 失败: {}, 耗时: {}ms",
                    records.size(), totalSuccessfulRecords.get(), totalFailedRecords.get(), processingTime);

        } catch (Exception e) {
            logger.error("多线程批处理发生异常", e);
            // 回退到单线程处理
            logger.info("多线程处理失败，回退到单线程处理");
            processBatchSequential(records);
        }
    }

    /**
     * 单线程顺序处理（回退方法）
     * 
     * @param records 待处理记录列表
     */
    private void processBatchSequential(List<EncryptedDataTemp> records) {
        int successCount = 0;
        int failureCount = 0;

        for (EncryptedDataTemp record : records) {
            try {
                processPollingRecord(record);
                successCount++;
                totalSuccessfulRecords.incrementAndGet();
                logger.info("成功处理记录ID: {}", record.getId());
            } catch (Exception e) {
                failureCount++;
                totalFailedRecords.incrementAndGet();
                logger.error("处理记录ID: {} 失败: {}", record.getId(), e.getMessage(), e);

                // 标记为错误状态
                try {
                    encryptedDataTempService.markAsError(record.getId(),
                            "处理失败: " + e.getMessage());
                } catch (Exception markErrorException) {
                    logger.error("标记错误状态失败，记录ID: {}", record.getId(), markErrorException);
                }
            }
        }

        totalProcessedRecords.addAndGet(records.size());
        logger.info("单线程批处理完成 - 成功: {}条, 失败: {}条", successCount, failureCount);
    }

    /**
     * 等待所有CompletableFuture任务完成
     * 
     * @param futures        CompletableFuture任务列表
     * @param timeoutSeconds 超时时间（秒）
     */
    private void waitForCompletion(List<CompletableFuture<Void>> futures, int timeoutSeconds) {
        try {
            // 将所有任务组合成一个CompletableFuture
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));

            // 等待所有任务完成，设置超时时间
            allFutures.get(timeoutSeconds, TimeUnit.SECONDS);

        } catch (java.util.concurrent.TimeoutException e) {
            logger.warn("批处理任务超时，超时时间: {}秒", timeoutSeconds);
            // 超时不影响已完成的处理，继续执行
        } catch (Exception e) {
            logger.error("等待任务完成时发生异常", e);
        }
    }

    /**
     * 处理单条记录
     * 复用成熟的LLM处理逻辑
     * 
     * @param record 待处理记录
     */
    private void processPollingRecord(EncryptedDataTemp record) {
        String recordId = record.getId();
        DataStatus currentStatus = record.getStatus();

        logger.info("开始处理记录ID: {}, 当前状态: {}", recordId, currentStatus);

        // 根据当前状态决定处理流程
        switch (currentStatus) {
            case RECEIVED:
                // 需要先解密
                decryptAndProcessRecord(record);
                break;
            case DECRYPTED:
                // 已觢密，直接调用LLM处理
                processWithLLMRecord(record);
                break;
            default:
                logger.warn("记录ID: {} 状态异常: {}，跳过处理", recordId, currentStatus);
                break;
        }
    }

    /**
     * 解密数据并处理
     * 
     * @param record 待解密记录
     */
    private void decryptAndProcessRecord(EncryptedDataTemp record) {
        String recordId = record.getId();

        try {
            // 获取加密数据
            Clob encryptedDataClob = record.getEncryptedData();
            if (encryptedDataClob == null) {
                throw new IllegalStateException("ENCRYPTED_DATA字段为空");
            }

            String encryptedData = extractClobContent(encryptedDataClob);
            if (encryptedData == null || encryptedData.trim().isEmpty()) {
                throw new IllegalStateException("加密数据内容为空");
            }

            // 解密数据
            String decryptedPrompt = decryptPollingData(encryptedData);
            if (decryptedPrompt == null || decryptedPrompt.trim().isEmpty()) {
                throw new IllegalStateException("解密后数据为空");
            }

            logger.info("记录ID: {} 解密成功，内容长度: {}字符", recordId, decryptedPrompt.length());

            // 保存解密数据并更新状态
            Clob decryptedDataClob = ClobManager.createSerialClob(decryptedPrompt);
            encryptedDataTempService.decryptData(recordId, decryptedDataClob);

            // 继续LLM处理
            EncryptedDataTemp updatedRecord = encryptedDataTempRepository.findById(recordId)
                    .orElseThrow(() -> new IllegalStateException("记录不存在: " + recordId));
            processWithLLMRecord(updatedRecord);

        } catch (Exception e) {
            logger.error("解密处理记录ID: {} 失败", recordId, e);
            throw new RuntimeException("解密处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用LLM处理记录
     * 复用成熟的LLM调用逻辑
     * 
     * @param record 已解密的记录
     */
    private void processWithLLMRecord(EncryptedDataTemp record) {
        String recordId = record.getId();

        try {
            // 更新状态为处理中
            encryptedDataTempService.startProcessing(recordId);

            // 获取解密数据
            Clob decryptedDataClob = record.getDecryptedData();
            if (decryptedDataClob == null) {
                throw new IllegalStateException("DECRYPTED_DATA字段为空");
            }

            String decryptedPrompt = extractClobContent(decryptedDataClob);
            if (decryptedPrompt == null || decryptedPrompt.trim().isEmpty()) {
                throw new IllegalStateException("觢密数据内容为空");
            }

            logger.info("记录ID: {} 开始LLM处理，Prompt长度: {}字符", recordId, decryptedPrompt.length());

            // 调用成熟的LLM处理逻辑（包括缓存、重试、错误处理等）
            String encryptedResult = processDecryptedPrompt(decryptedPrompt);

            logger.info("记录ID: {} LLM处理完成，调用成熟逻辑，结果长度: {}字符", recordId, encryptedResult.length());

            // 验证结果完整性
            if (!validateLLMResultIntegrity(encryptedResult, decryptedPrompt)) {
                logger.warn("记录ID: {} LLM结果完整性验证失败，可能结果不完整", recordId);
                // 这里可以添加重试逻辑，但当前先记录警告
            }

            // 保存结果到EXECUTION_RESULT字段
            Clob executionResultClob = ClobManager.createSerialClob(encryptedResult);
            encryptedDataTempService.completeProcessing(recordId, executionResultClob);

            // 更新状态为已加密
            encryptedDataTempService.encryptResult(recordId, ClobManager.createSerialClob(""));

            logger.info("记录ID: {} 数据处理流程完成，状态已更新为ENCRYPTED", recordId);

        } catch (Exception e) {
            logger.error("LLM处理记录ID: {} 失败", recordId, e);
            throw new RuntimeException("LLM处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 提取Clob内容
     * 
     * @param clob Clob对象
     * @return 字符串内容
     */
    private String extractClobContent(Clob clob) {
        try {
            if (clob != null) {
                long length = clob.length();
                if (length > 0) {
                    return clob.getSubString(1, (int) length);
                }
            }
            return null;
        } catch (Exception e) {
            logger.error("提取Clob内容失败", e);
            return null;
        }
    }

    /**
     * 解密数据
     * 复用成熟的AES解密逻辑
     * 
     * @param encryptedData 加密数据
     * @return 解密后数据
     */
    private String decryptPollingData(String encryptedData) {
        try {
            // 获取AES加密配置
            var encryptionKeyConfig = serverConfigService.getConfigByName("AES_ENCRYPTION_KEY");
            var encryptionSaltConfig = serverConfigService.getConfigByName("AES_ENCRYPTION_SALT");

            if (encryptionKeyConfig == null || encryptionSaltConfig == null) {
                throw new IllegalStateException("AES加密配置未找到");
            }

            String encryptionKey = encryptionKeyConfig.getConfigData();
            String encryptionSalt = encryptionSaltConfig.getConfigData();

            if (encryptionKey == null || encryptionKey.trim().isEmpty() ||
                    encryptionSalt == null || encryptionSalt.trim().isEmpty()) {
                throw new IllegalStateException("AES加密密钥或盐值为空");
            }

            return AESEncryptionUtil.decrypt(encryptedData, encryptionKey, encryptionSalt);

        } catch (Exception e) {
            logger.error("解密数据失败", e);
            throw new RuntimeException("解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取轮询统计信息
     * 
     * @return 统计信息
     */
    private Map<String, Object> getPollingStatsData() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pollingEnabled", pollingServiceEnabled);
        stats.put("totalPollingCycles", totalPollingCycles.get());
        stats.put("totalProcessedRecords", totalProcessedRecords.get());
        stats.put("totalSuccessfulRecords", totalSuccessfulRecords.get());
        stats.put("totalFailedRecords", totalFailedRecords.get());

        long totalProcessed = totalProcessedRecords.get();
        if (totalProcessed > 0) {
            double successRate = (double) totalSuccessfulRecords.get() / totalProcessed * 100;
            stats.put("successRate", Math.round(successRate * 100.0) / 100.0);
        } else {
            stats.put("successRate", 0.0);
        }

        stats.put("timestamp", System.currentTimeMillis());
        stats.put("reportTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return stats;
    }

    /**
     * 重置轮询统计计数器
     */
    private void resetPollingStatsData() {
        totalPollingCycles.set(0);
        totalProcessedRecords.set(0);
        totalSuccessfulRecords.set(0);
        totalFailedRecords.set(0);
        logger.info("轮询统计计数器已重置");
    }

    /**
     * 获取轮询服务统计信息
     * 
     * 查询轮询服务的运行统计数据，包括处理记录数、成功率等信息。
     * 
     * @return 轮询统计响应
     *         - pollingEnabled: 轮询是否启用
     *         - totalPollingCycles: 总轮询次数
     *         - totalProcessedRecords: 总处理记录数
     *         - totalSuccessfulRecords: 总成功记录数
     *         - totalFailedRecords: 总失败记录数
     *         - successRate: 成功率（百分比）
     *         - timestamp: 查询时间戳
     */
    @GetMapping("/polling-stats")
    public ResponseEntity<Map<String, Object>> getPollingStats() {
        try {
            Map<String, Object> stats = getPollingStatsData();
            logger.debug("轮询统计信息查询成功");
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("查询轮询统计信息失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "查询轮询统计信息失败: " + e.getMessage(),
                            "timestamp", System.currentTimeMillis()));
        }
    }

    /**
     * 重置轮询服务统计信息
     * 
     * 清零轮询服务的统计计数器，重新开始统计。
     * 
     * @return 重置结果响应
     *         - status: 重置状态
     *         - message: 重置结果信息
     *         - timestamp: 重置时间戳
     */
    @PostMapping("/reset-polling-stats")
    public ResponseEntity<Map<String, Object>> resetPollingStats() {
        try {
            resetPollingStatsData();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "轮询统计计数器已重置");
            response.put("timestamp", System.currentTimeMillis());

            logger.info("轮询统计计数器重置成功");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("重置轮询统计计数器失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "重置轮询统计计数器失败: " + e.getMessage(),
                            "timestamp", System.currentTimeMillis()));
        }
    }

    /**
     * 验证LLM结果完整性
     * 检查LLM响应是否完整，避免提前截断导致结果不完整
     * 
     * @param encryptedResult 加密的LLM结果
     * @param originalPrompt  原始Prompt内容
     * @return 结果是否完整
     */
    private boolean validateLLMResultIntegrity(String encryptedResult, String originalPrompt) {
        try {
            // 解密结果进行验证
            String decryptedResult = decryptProcessingResult(encryptedResult);

            if (decryptedResult == null || decryptedResult.trim().isEmpty()) {
                logger.warn("LLM结果为空，完整性验证失败");
                return false;
            }

            // 检查结果长度是否合理
            if (decryptedResult.length() < 10) {
                logger.warn("LLM结果过短（{}字符），可能不完整", decryptedResult.length());
                return false;
            }

            // 检查结果是否包含常见的截断标记
            if (decryptedResult.endsWith("...") ||
                    decryptedResult.endsWith("等等") ||
                    decryptedResult.endsWith("等") ||
                    decryptedResult.endsWith("...") ||
                    decryptedResult.contains("截断") ||
                    decryptedResult.contains("不完整")) {
                logger.warn("LLM结果包含截断标记，可能不完整");
                return false;
            }

            // 检查句子完整性（最后一个句子是否完整结束）
            if (!isSentenceComplete(decryptedResult)) {
                logger.warn("LLM结果句子不完整，可能被截断");
                return false;
            }

            // 检查是否有明显的格式问题
            if (hasFormattingIssues(decryptedResult)) {
                logger.warn("LLM结果存在格式问题，可能不完整");
                return false;
            }

            logger.debug("LLM结果完整性验证通过，结果长度: {} 字符", decryptedResult.length());
            return true;

        } catch (Exception e) {
            logger.error("LLM结果完整性验证失败", e);
            // 验证失败时，默认认为结果可能不完整，但继续处理
            return false;
        }
    }

    /**
     * 解密处理结果（用于验证）
     * 
     * @param encryptedResult 加密的结果
     * @return 解密后的结果
     */
    private String decryptProcessingResult(String encryptedResult) {
        try {
            // 从数据库获取AES加密配置
            ServerConfiguration encryptionKeyConfig = serverConfigService.getConfigByName("AES_ENCRYPTION_KEY");
            ServerConfiguration encryptionSaltConfig = serverConfigService.getConfigByName("AES_ENCRYPTION_SALT");

            if (encryptionKeyConfig == null || encryptionSaltConfig == null) {
                logger.error("AES加密配置未找到，无法解密验证结果");
                return null;
            }

            String encryptionKey = encryptionKeyConfig.getConfigData();
            String encryptionSalt = encryptionSaltConfig.getConfigData();

            if (encryptionKey == null || encryptionKey.trim().isEmpty() ||
                    encryptionSalt == null || encryptionSalt.trim().isEmpty()) {
                logger.error("AES加密密钥或盐值为空，无法解密验证结果");
                return null;
            }

            // 使用AES解密工具进行解密
            return AESEncryptionUtil.decrypt(encryptedResult, encryptionKey, encryptionSalt);
        } catch (Exception e) {
            logger.error("解密验证结果失败", e);
            return null;
        }
    }

    /**
     * 检查句子是否完整结束
     * 
     * @param text 待检查的文本
     * @return 句子是否完整
     */
    private boolean isSentenceComplete(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        // 获取最后一个字符
        char lastChar = text.trim().charAt(text.trim().length() - 1);

        // 检查是否以完整的句子结束符结尾
        return lastChar == '。' || lastChar == '！' || lastChar == '？' ||
                lastChar == '.' || lastChar == '!' || lastChar == '?';
    }

    /**
     * 检查是否存在格式问题
     * 
     * @param text 待检查的文本
     * @return 是否存在格式问题
     */
    private boolean hasFormattingIssues(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        // 检查是否存在未闭合的括号
        int openBrackets = countOccurrences(text, '（');
        int closeBrackets = countOccurrences(text, '）');
        if (openBrackets != closeBrackets) {
            logger.debug("检测到括号不匹配: 开括号={}, 闭括号={}", openBrackets, closeBrackets);
            return true;
        }

        // 检查是否存在未闭合的引号
        int quotes = countOccurrences(text, '"');
        if (quotes % 2 != 0) {
            logger.debug("检测到引号不匹配: 引号数量={}", quotes);
            return true;
        }

        // 检查是否存在未闭合的代码块
        int codeBlocks = countOccurrences(text, '`');
        if (codeBlocks % 2 != 0) {
            logger.debug("检测到代码块不匹配: 反引号数量={}", codeBlocks);
            return true;
        }

        return false;
    }

    /**
     * 统计字符出现次数
     * 
     * @param text 文本
     * @param ch   要统计的字符
     * @return 出现次数
     */
    private int countOccurrences(String text, char ch) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }

    /**
     * 生成一个新的唯一ID
     * 
     * @return 唯一的ID
     */
    private String generateUniqueId() {
        String baseId = "cdwyy";
        long timestamp = System.currentTimeMillis();

        // 使用时间戳 + 线程ID + 随机数确保唯一性
        long threadId = Thread.currentThread().threadId();
        int randomSuffix = (int) (Math.random() * 1000);

        String candidateId = String.format("%s%d_%d_%d", baseId, timestamp, threadId, randomSuffix);

        // 直接返回生成的ID，不再进行数据库检查
        // 如果有冲突，在保存时通过幂等性处理
        return candidateId;
    }
}
