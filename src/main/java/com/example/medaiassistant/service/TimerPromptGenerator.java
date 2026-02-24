package com.example.medaiassistant.service;

import com.example.medaiassistant.config.ApiProperties;
import com.example.medaiassistant.config.SchedulingProperties;
import com.example.medaiassistant.dto.TemplateConfig;
import com.example.medaiassistant.util.AgeCalculator;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import com.example.medaiassistant.repository.PatientRepository;
import com.example.medaiassistant.repository.PromptRepository;
import com.example.medaiassistant.repository.PromptResultRepository;
import com.example.medaiassistant.repository.MedicalRecordRepository;
import com.example.medaiassistant.repository.LabResultRepository;
import com.example.medaiassistant.repository.ExaminationResultRepository;
import com.example.medaiassistant.repository.LongTermOrderRepository;
import com.example.medaiassistant.repository.EmrRecordRepository;
import com.example.medaiassistant.repository.EmrContentRepository;
import com.example.medaiassistant.repository.SurgeryRepository;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.model.ServerConfiguration;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

/**
 * 定时任务生成器服务类
 * 
 * 该服务类负责管理定时任务的启动、停止和执行，包括：
 * 1. 患者状态更新任务
 * 2. 告警规则处理任务
 * 3. 每日Prompt生成任务
 * 
 * @author Cline
 * @since 2025-08-06
 */
@Service
@EnableScheduling
public class TimerPromptGenerator {

    /**
     * AI分析阅读门槛常量：至少有1条AI分析已读
     */
    private static final int AI_ANALYSIS_READ_THRESHOLD = 1;

    /**
     * 查房记录模板常量
     */
    private static final String TEMPLATE_CRITICAL_DAILY = "病危每日查房记录";
    private static final String TEMPLATE_SERIOUS_BIDAILY = "病重每2日查房记录";
    private static final String TEMPLATE_STANDARD = "查房记录";

    /**
     * 首次查房时默认获取的病历记录条数
     */
    private static final int DEFAULT_MEDICAL_RECORDS_COUNT = 3;
    
    /**
     * 首次查房时化验结果回溯天数
     */
    private static final int DEFAULT_LAB_RESULTS_FALLBACK_DAYS = 2;

    /**
     * 时间间隔常量（小时）
     */
    private static final int INTERVAL_DAILY = 24;
    private static final int INTERVAL_BIDAILY = 48;

    /**
     * 任务调度器，用于调度和执行定时任务
     */
    private final TaskScheduler taskScheduler;

    /**
     * 告警规则服务，用于处理激活的时间规则
     */
    private final AlertRuleService alertRuleService;

    /**
     * 患者状态更新服务，用于定时更新患者状态
     */
    private final PatientStatusUpdateService patientStatusUpdateService;

    private final PatientRepository patientRepository;
    private final PromptRepository promptRepository;
    private final PromptResultRepository promptResultRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final LabResultRepository labResultRepository;
    private final ExaminationResultRepository examinationResultRepository;
    private final LongTermOrderRepository longTermOrderRepository;
    private final ServerConfigService serverConfigService;
    private final RestTemplate restTemplate;
    private final ApiProperties apiProperties;
    private final SchedulingProperties schedulingProperties;
    private final EmrRecordRepository emrRecordRepository;
        private final EmrContentRepository emrContentRepository;
    private final PromptGenerationLogService promptGenerationLogService;
    private final PromptsTaskUpdater promptsTaskUpdater;
    // 配置缓存
    private String cachedDailyTaskTime = "0 0 7 * * *";
    private int cachedMaxConcurrency = 5;
    
    // 性能监控指标
    private final java.util.concurrent.atomic.AtomicLong totalApiCalls = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong successfulApiCalls = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong failedApiCalls = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong totalProcessingTime = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong totalPatientsProcessed = new java.util.concurrent.atomic.AtomicLong(0);

    /**
     * 定时任务的句柄，用于控制任务的取消
     */
    private ScheduledFuture<?> scheduledTask;

    /**
     * 设置每日任务执行时间
     * 
     * @param time 时间表达式，如"0 0 7 * * *"表示7:00
     */
    public void setDailyTaskTime(String time) {
        this.cachedDailyTaskTime = time;
    }

    /**
     * 设置最大并发数
     * 
     * @param max 最大并发数
     */
    public void setMaxConcurrency(int max) {
        this.cachedMaxConcurrency = max;
    }

    /**
     * 获取当前每日任务执行时间
     * 
     * @return cron表达式格式的时间
     */
    public String getDailyTaskTime() {
        return cachedDailyTaskTime;
    }

    /**
     * 获取当前最大并发数
     * 
     * @return 最大并发数
     */
    public int getMaxConcurrency() {
        return cachedMaxConcurrency;
    }

    /**
     * 从数据库加载配置
     */
    private void loadConfig() {
        try {
            ServerConfiguration config = serverConfigService.getConfigByName("auto_generator_config");
            if (config != null && config.getConfigData() != null) {
                JsonNode json = new ObjectMapper().readTree(config.getConfigData());
                if (json.has("dailyTaskTime")) {
                    this.cachedDailyTaskTime = json.get("dailyTaskTime").asText();
                }
                if (json.has("max_concurrency")) {
                    this.cachedMaxConcurrency = json.get("max_concurrency").asInt();
                }
            }
        } catch (Exception e) {
            System.err.println("加载定时任务配置失败: " + e.getMessage());
        }
    }

    /**
     * 定时器运行状态标志
     */
    private volatile boolean isRunning = false;

    /**
     * 应用启动完成事件监听器
     * 已移除自动启动逻辑，改为手动启用定时任务
     * 
     * @function onApplicationReady
     * @description 在Spring上下文完全初始化后记录启动信息，但不自动启动定时任务
     * @param event 上下文刷新事件
     * @returns {void}
     * @since 2025-10-10
     * @author Cline
     * @version 1.5.0 - 移除自动启动，改为手动控制
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationReady(ContextRefreshedEvent event) {
        System.out.println("系统启动完成，定时任务已设置为手动启用模式");
        System.out.println("如需启动定时任务，请调用startTimer()方法");
    }

    /**
     * 构造函数，通过依赖注入初始化服务
     * 
     * @param taskScheduler              任务调度器
     * @param alertRuleService           告警规则服务
     * @param patientStatusUpdateService 患者状态更新服务
     */
    public TimerPromptGenerator(TaskScheduler taskScheduler,
            AlertRuleService alertRuleService,
            PatientStatusUpdateService patientStatusUpdateService,
            PatientRepository patientRepository,
            PromptRepository promptRepository,
            PromptResultRepository promptResultRepository,
            MedicalRecordRepository medicalRecordRepository,
            LabResultRepository labResultRepository,
            ExaminationResultRepository examinationResultRepository,
            LongTermOrderRepository longTermOrderRepository,
            ServerConfigService serverConfigService,
            RestTemplate restTemplate,
            ApiProperties apiProperties,
            SchedulingProperties schedulingProperties,
            EmrRecordRepository emrRecordRepository,
            EmrContentRepository emrContentRepository,
            SurgeryRepository surgeryRepository,
            PromptGenerationLogService promptGenerationLogService,
            PromptsTaskUpdater promptsTaskUpdater) {
        this.taskScheduler = taskScheduler;
        this.alertRuleService = alertRuleService;
        this.patientStatusUpdateService = patientStatusUpdateService;
        this.patientRepository = patientRepository;
        this.promptRepository = promptRepository;
        this.promptResultRepository = promptResultRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.labResultRepository = labResultRepository;
        this.examinationResultRepository = examinationResultRepository;
        this.longTermOrderRepository = longTermOrderRepository;
        this.serverConfigService = serverConfigService;
        this.restTemplate = restTemplate;
        this.apiProperties = apiProperties;
        this.schedulingProperties = schedulingProperties;
        this.emrRecordRepository = emrRecordRepository;
        this.emrContentRepository = emrContentRepository;
        this.promptGenerationLogService = promptGenerationLogService;
        this.promptsTaskUpdater = promptsTaskUpdater;
        loadConfig();
    }

    /**
     * 启动定时器
     * 
     * 该方法会启动一个定时任务，每隔5分钟执行一次printScheduledMessage方法，
     * 用于处理激活的时间规则并生成相应的告警任务
     * 
     * @function startTimer
     * @description 线程安全的定时器启动方法，确保并发环境下不会创建多个定时任务
     * @returns {void}
     * @throws {Exception} 当任务调度失败时抛出异常
     * @example
     *          timerPromptGenerator.startTimer();
     * 
     * @since 2025-08-06
     * @author Cline
     * @version 1.1.0 - 修复并发状态不一致问题
     */
    public synchronized void startTimer() {
        if (!isRunning) {
            isRunning = true; // 先设置状态标志，避免时间窗口问题
            try {
                // 使用统一配置的固定延迟时间
                scheduledTask = taskScheduler.scheduleWithFixedDelay(this::printScheduledMessage,
                        java.time.Duration.ofMinutes(schedulingProperties.getTimer().getFixedDelayMinutes()));
                System.out.println("定时器已启动，间隔: " + schedulingProperties.getTimer().getFixedDelayMinutes() + "分钟");
            } catch (Exception e) {
                isRunning = false; // 异常时重置状态，确保状态一致性
                throw e;
            }
        } else {
            System.out.println("定时器已在运行中");
        }
    }

    /**
     * 停止定时器
     * 
     * 该方法会取消已调度的定时任务并更新运行状态
     * 
     * @function stopTimer
     * @description 线程安全的定时器停止方法，确保并发环境下状态一致性
     * @returns {void}
     * @example
     *          timerPromptGenerator.stopTimer();
     * 
     * @since 2025-08-06
     * @author Cline
     * @version 1.1.0 - 修复并发状态不一致问题
     */
    public synchronized void stopTimer() {
        if (isRunning && scheduledTask != null) {
            scheduledTask.cancel(false);
            isRunning = false;
            System.out.println("定时器已停止");
        } else {
            System.out.println("定时器未在运行");
        }
    }

    /**
     * 定时执行的任务
     * 
     * 该方法按顺序执行以下操作：
     * 1. 调用PatientStatusUpdateService的updateAllPatientStatus方法更新患者状态
     * 2. 调用AlertRuleService的processActiveTimeRules方法处理时间告警规则
     * 3. 调用AlertRuleService的processActiveStatusRules方法处理状态告警规则
     * 
     * 每个服务调用都有独立的异常处理，确保单个服务的异常不会影响其他服务的执行
     * 
     * @function printScheduledMessage
     * @description 定时执行的核心任务方法，包含患者状态更新和告警规则处理
     * @returns {void}
     * @throws {Error} 单个服务的异常会被捕获并记录，不会中断整个定时任务
     * @since 2025-08-26
     * @version 1.2.0 - 添加完整的异常处理机制
     * 
     * @example
     *          // 定时任务执行示例
     *          printScheduledMessage();
     *          // 输出:
     *          // 定时器工作中
     *          // 患者状态更新任务执行完成
     *          // 时间告警规则处理任务执行完成
     *          // 状态告警规则处理任务执行完成
     *          // 定时器任务执行完成
     */
    private void printScheduledMessage() {
        System.out.println("定时器工作中");

        // 调用PatientStatusUpdateService的updateAllPatientStatus方法
        try {
            patientStatusUpdateService.updateAllPatientStatus();
            System.out.println("患者状态更新任务执行完成");
        } catch (Exception e) {
            System.err.println("患者状态更新任务执行失败: " + e.getMessage());
            e.printStackTrace();
            // 记录错误但继续执行后续任务
        }

        // 调用AlertRuleService的processActiveTimeRules方法
        try {
            alertRuleService.processActiveTimeRules();
            System.out.println("时间告警规则处理任务执行完成");
        } catch (Exception e) {
            System.err.println("时间告警规则处理任务执行失败: " + e.getMessage());
            e.printStackTrace();
            // 记录错误但继续执行后续任务
        }

        // 调用AlertRuleService的processActiveStatusRules方法
        try {
            alertRuleService.processActiveStatusRules();
            System.out.println("状态告警规则处理任务执行完成");
        } catch (Exception e) {
            System.err.println("状态告警规则处理任务执行失败: " + e.getMessage());
            e.printStackTrace();
            // 记录错误但继续执行后续任务
        }

        System.out.println("定时器任务执行完成");
    }

    /**
     * 检查定时器是否正在运行
     * 
     * @return 如果定时器正在运行则返回true，否则返回false
     */
    public boolean isTimerRunning() {
        return isRunning;
    }

    /**
     * 手动执行手术分析任务（供API调用）
     * 定时任务已迁移到独立的SurgeryAnalysisService中
     * 
     * @function executeSurgeryAnalysis
     * @description 手动执行手术记录分析任务，为有未分析手术记录的患者分析手术名称
     * @returns {Map<String, Object>} 执行结果统计信息
     * @throws {Error} 当任务执行过程中发生异常时抛出错误
     * @example
     *          // 手动调用手术分析
     *          Map<String, Object> result = executeSurgeryAnalysis();
     * 
     * @since 2025-09-26
     * @author Cline
     * @version 1.2.0 - 移除定时任务，保留手动执行功能
     */
    public Map<String, Object> executeSurgeryAnalysis() {
        System.out.println("手动执行手术记录分析任务");
        
        try {
            Map<String, Object> result = new HashMap<>();
            long startTime = System.currentTimeMillis();
            
            // 使用原子变量来统计处理结果
            java.util.concurrent.atomic.AtomicInteger totalPatientsProcessed = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger totalAnalysesCompleted = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger totalAnalysesFailed = new java.util.concurrent.atomic.AtomicInteger(0);

            // 使用线程池控制并发
            ExecutorService executor = Executors.newFixedThreadPool(schedulingProperties.getTimer().getMaxConcurrency());
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            try {
                // 获取有手术记录但未分析的患者列表
                List<String> patientsWithUnanalyzedOperations = findPatientsWithUnanalyzedOperations();
                
                if (patientsWithUnanalyzedOperations.isEmpty()) {
                    result.put("message", "没有需要分析手术记录的患者");
                    result.put("totalPatientsProcessed", 0);
                    result.put("analysesCompleted", 0);
                    result.put("analysesFailed", 0);
                    result.put("durationSeconds", 0);
                    return result;
                }

                System.out.println("找到 " + patientsWithUnanalyzedOperations.size() + " 个有未分析手术记录的患者");

                // 为每个患者执行手术分析
                for (String patientId : patientsWithUnanalyzedOperations) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            boolean analysisSuccess = analyzePatientOperations(patientId);
                            if (analysisSuccess) {
                                totalAnalysesCompleted.incrementAndGet();
                                System.out.println("患者 " + patientId + " 的手术记录分析完成");
                            } else {
                                totalAnalysesFailed.incrementAndGet();
                                System.err.println("患者 " + patientId + " 的手术记录分析失败");
                            }
                        } catch (Exception e) {
                            totalAnalysesFailed.incrementAndGet();
                            System.err.println("患者 " + patientId + " 的手术记录分析异常: " + e.getMessage());
                        }
                    }, executor);

                    futures.add(future);
                    totalPatientsProcessed.incrementAndGet();

                    // 控制并发任务数量
                    if (futures.size() >= schedulingProperties.getTimer().getMaxConcurrency()) {
                        CompletableFuture.anyOf(futures.toArray(new CompletableFuture[0])).join();
                        futures.removeIf(CompletableFuture::isDone);
                    }
                }

                // 等待所有任务完成
                if (!futures.isEmpty()) {
                    try {
                        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                                .orTimeout(300, TimeUnit.SECONDS) // 5分钟超时
                                .join();
                    } catch (Exception e) {
                        System.err.println("手术分析任务超时或失败: " + e.getMessage());
                    }
                    futures.clear();
                }

            } catch (Exception e) {
                System.err.println("手术分析任务执行失败: " + e.getMessage());
                e.printStackTrace();
                result.put("error", e.getMessage());
            } finally {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            long endTime = System.currentTimeMillis();
            long duration = (endTime - startTime) / 1000;
            
            // 构建返回结果
            result.put("totalPatientsProcessed", totalPatientsProcessed.get());
            result.put("analysesCompleted", totalAnalysesCompleted.get());
            result.put("analysesFailed", totalAnalysesFailed.get());
            result.put("durationSeconds", duration);
            result.put("message", "手术分析任务完成");
            
            System.out.println("手术分析任务完成");
            System.out.println("总共处理患者: " + totalPatientsProcessed.get() + " 个");
            System.out.println("成功分析: " + totalAnalysesCompleted.get() + " 个");
            System.out.println("分析失败: " + totalAnalysesFailed.get() + " 个");
            System.out.println("总耗时: " + duration + " 秒");
            
            return result;
            
        } catch (Exception e) {
            System.err.println("手动执行手术分析任务失败: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            errorResult.put("status", "FAILED");
            return errorResult;
        }
    }

    /**
     * 每日定时自动生成Prompt任务
     * 使用专用线程池，避免与手术分析任务竞争资源
     * 优化数据库连接和并发控制，解决卡顿问题
     * 
     * @function dailyPromptGeneration
     * @description 定时执行每日Prompt生成任务，为所有在院患者生成诊断分析和诊疗计划Prompt
     * @returns {void}
     * @throws {Error} 当任务执行过程中发生异常时抛出错误
     * @example
     *          // 每天7:00自动执行
     *          dailyPromptGeneration();
     * 
     * @since 2025-08-06
     * @author Cline
     * @version 1.3.0 - 优化数据库连接和并发控制，解决卡顿问题
     */
    @Async("promptGenerationExecutor")
    @Scheduled(cron = "${scheduling.timer.daily-time:0 0 7 * * *}")
    public void dailyPromptGeneration() {
        System.out.println("开始执行每日Prompt生成任务 - 优化版");
        long startTime = System.currentTimeMillis();
        
        // 使用原子变量来统计处理结果
        java.util.concurrent.atomic.AtomicInteger totalPatientsProcessed = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger totalPromptsGenerated = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger totalFailures = new java.util.concurrent.atomic.AtomicInteger(0);

        try {
            int page = 0;
            int pageSize = 20; // 进一步减少每页大小，降低数据库压力
            boolean hasMore = true;

            while (hasMore) {
                // 分页获取在院患者，避免一次性加载所有数据到内存
                List<Patient> inHospitalPatients = findInHospitalPatientsByPage(page, pageSize);

                if (inHospitalPatients.isEmpty()) {
                    hasMore = false;
                    break;
                }

                System.out.println("处理第 " + page + " 页，共 " + inHospitalPatients.size() + " 个患者");

                // 使用更保守的并发控制 - 串行处理每个患者
                for (Patient patient : inHospitalPatients) {
                    final String patientId = patient.getPatientId();
                    
                    try {
                        // 串行处理每个患者，避免数据库连接竞争
                        generateAndSavePromptOptimized(patientId, "诊断分析", "诊断分析");
                        generateAndSavePromptOptimized(patientId, "诊疗计划", "诊疗计划补充");
                        // 生成病情小结（promptType为"资料总结"）
                        generateAndSavePromptOptimized(patientId, "资料总结", "病情小结");
                        totalPromptsGenerated.addAndGet(3);
                        System.out.println("患者 " + patientId + " 的Prompt生成完成（诊断分析、诊疗计划、病情小结）");
                    } catch (Exception e) {
                        totalFailures.incrementAndGet();
                        System.err.println("患者ID " + patientId + " 的Prompt生成失败: " + e.getMessage());
                        // 继续处理其他患者，不中断整个任务
                    }
                    
                    totalPatientsProcessed.incrementAndGet();
                    
                    // 每个患者处理完成后添加短暂延迟，避免数据库压力过大
                    try {
                        Thread.sleep(500); // 500ms延迟
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                page++;

                // 每页处理完成后添加更长的延迟
                try {
                    Thread.sleep(2000); // 2秒延迟
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("每日Prompt生成任务执行失败: " + e.getMessage());
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime) / 1000;
        System.out.println("每日Prompt生成任务完成");
        System.out.println("总共处理患者: " + totalPatientsProcessed.get() + " 个");
        System.out.println("总共生成Prompt: " + totalPromptsGenerated.get() + " 个");
        System.out.println("生成失败: " + totalFailures.get() + " 个");
        System.out.println("总耗时: " + duration + " 秒");
    }

    /**
     * 分页查询在院患者（支持科室过滤）
     * 
     * @param page     页码（从0开始）
     * @param pageSize 每页大小
     * @return 当前页的患者列表
     */
    public List<Patient> findInHospitalPatientsByPage(int page, int pageSize) {
        // 使用真正的数据库分页查询，避免内存占用过高
        PageRequest pageable = PageRequest.of(page, pageSize);

        // 检查是否启用科室过滤
        if (schedulingProperties.getTimer().isDepartmentFilterEnabled()) {
            List<String> targetDepartments = schedulingProperties.getTimer().getTargetDepartments();
            
            // 记录科室过滤状态 - 添加详细调试信息
            System.out.println("=== 科室过滤调试信息 ===");
            System.out.println("departmentFilterEnabled: " + schedulingProperties.getTimer().isDepartmentFilterEnabled());
            System.out.println("targetDepartments: " + targetDepartments);
            System.out.println("targetDepartments is null: " + (targetDepartments == null));
            System.out.println("targetDepartments isEmpty: " + (targetDepartments != null && targetDepartments.isEmpty()));
            if (targetDepartments != null) {
                System.out.println("targetDepartments size: " + targetDepartments.size());
                for (int i = 0; i < targetDepartments.size(); i++) {
                    System.out.println("  targetDepartments[" + i + "]: '" + targetDepartments.get(i) + "'");
                }
            }
            
            if (targetDepartments != null && !targetDepartments.isEmpty()) {
                System.out.println("启用科室过滤，目标科室: " + targetDepartments);
            } else {
                System.out.println("启用科室过滤，但目标科室列表为空，返回空结果");
            }
            
            // 使用安全方法查询目标科室患者
            org.springframework.data.domain.Page<Patient> patientPage = 
                patientRepository.findByDepartmentsAndIsInHospitalSafe(targetDepartments, true, pageable);
            System.out.println("查询结果: " + patientPage.getContent().size() + " 个患者");
            System.out.println("=== 科室过滤调试信息结束 ===");
            return patientPage.getContent();
        } else {
            // 未启用科室过滤，查询所有在院患者
            System.out.println("未启用科室过滤，处理所有在院患者");
            org.springframework.data.domain.Page<Patient> patientPage = patientRepository.findByIsInHospital(true, pageable);
            return patientPage.getContent();
        }
    }

    /**
     * 生成并保存Prompt（优化版）
     * 优化数据库操作，减少连接竞争，解决高并发场景下的数据库性能瓶颈
     * 
     * @function generateAndSavePromptOptimized
     * @description 该方法生成并保存Prompt记录，采用串行处理方式避免数据库连接竞争
     *              如果未获取到有效的患者数据或返回内容包含“无入院记录数据”标记则跳过保存，确保仅在存在入院病历资料时执行诊断/诊疗类分析，
     *              从而与患者分析准入逻辑保持一致。
     * @param {string} patientId - 患者ID
     * @param {string} promptType - Prompt类型
     * @param {string} promptName - Prompt名称
     * @returns {void}
     * @throws {Error} 当保存过程中发生异常时抛出错误
     * @since 2025-09-27
     * @author Cline
     * @version 1.3.0 - 优化数据库连接和并发控制
     * @example
     *          generateAndSavePromptOptimized("12345", "诊断分析", "诊断分析");
     */
    private void generateAndSavePromptOptimized(String patientId, String promptType, String promptName) {
        try {
            // 调用API获取患者数据
            String patientData = callPatientDataApi(patientId, promptType, promptName);

            // 检查是否成功获取到患者数据
            if (patientData == null || patientData.isEmpty() || patientData.contains("失败") || patientData.contains("无入院记录数据")) {
                System.err.println("未获取到有效的患者数据，跳过保存 - 患者ID: " + patientId +
                        ", 类型: " + promptType +
                        ", 名称: " + promptName);
                return;
            }

            // 调用API获取模板数据
            String promptTemplate = callPromptApi(promptType, promptName);

            // 创建并保存Prompt对象
            savePromptToDatabase(patientId, promptType, promptName, patientData, promptTemplate);

        } catch (Exception e) {
            System.err.println("生成Prompt失败 - 患者ID: " + patientId +
                    ", 类型: " + promptType +
                    ", 名称: " + promptName +
                    ", 错误: " + e.getMessage());
            throw e; // 重新抛出异常，让调用方知道失败
        }
    }

    /**
     * 保存Prompt到数据库（优化版）
     * 使用更高效的数据库操作方式，立即刷新到数据库，避免连接池竞争
     * 
     * @function savePromptToDatabase
     * @description 该方法使用saveAndFlush替代save，立即将数据刷新到数据库
     *              减少数据库连接占用时间，提高并发处理能力
     * @param {string} patientId - 患者ID
     * @param {string} promptType - Prompt类型
     * @param {string} promptName - Prompt名称
     * @param {string} patientData - 患者数据
     * @param {string} promptTemplate - 模板内容
     * @returns {void}
     * @throws {Error} 当数据库操作失败时抛出错误
     * @since 2025-09-27
     * @author Cline
     * @version 1.3.0 - 优化数据库操作性能
     * @example
     *          savePromptToDatabase("12345", "诊断分析", "诊断分析", patientData, template);
     */
    private void savePromptToDatabase(String patientId, String promptType, String promptName, 
                                     String patientData, String promptTemplate) {
        try {
            // 创建Prompt对象
            Prompt prompt = new Prompt();
            prompt.setPatientId(patientId);
            prompt.setPromptTemplateName(promptName);
            prompt.setStatusName("待处理");
            prompt.setSubmissionTime(LocalDateTime.now());
            prompt.setExecutionTime(null);
            prompt.setPriority(5);
            prompt.setRetryCount(0);
            prompt.setGeneratedBy("System");
            prompt.setSortNumber(0);
            prompt.setUserId(0);
            prompt.setObjectiveContent(patientData);
            prompt.setPromptTemplateContent(promptTemplate);
            prompt.setExecutionResult(null);

            // 使用更高效的保存方式
            promptRepository.saveAndFlush(prompt); // 立即刷新到数据库
            
            // 记录Prompt生成日志（科室、病人ID、床号、姓名）
            promptGenerationLogService.logPromptGeneration(patientId, promptType, promptName);
            
            System.out.println("成功保存Prompt - 患者ID: " + patientId +
                    ", 模板名称: " + promptName + ", 状态: 待处理");

        } catch (Exception e) {
            System.err.println("保存Prompt到数据库失败 - 患者ID: " + patientId +
                    ", 类型: " + promptType +
                    ", 名称: " + promptName +
                    ", 错误: " + e.getMessage());
            throw e;
        }
    }


    /**
     * 调用患者数据API获取患者数据（优化版 - 带超时和重试机制）
     * 
     * @param {string} patientId - 患者ID
     * @param {string} promptType - Prompt类型
     * @param {string} promptName - Prompt名称
     * @returns {string|null} 处理后的患者数据字符串，已去除SSE格式的"data:"前缀，如果获取失败返回null
     * @throws {Error} 当API调用发生异常时抛出错误
     * @description 该方法通过REST API调用获取患者数据，支持超时控制和重试机制
     * @example
     *          const patientData = callPatientDataApi("12345", "诊断分析", "诊断分析");
     *          if (patientData) {
     *          // 处理有效数据
     *          }
     */
    private String callPatientDataApi(String patientId, String promptType, String promptName) {
        return callPatientDataApiWithRetry(patientId, promptType, promptName, 
                apiProperties.getBase().getRetry().getMaxAttempts());
    }

    /**
     * 带重试机制的API调用函数
     * 
     * @param patientId 患者ID
     * @param promptType Prompt类型
     * @param promptName Prompt名称
     * @param maxRetries 最大重试次数
     * @return 处理后的患者数据字符串，如果获取失败返回null
     */
    private String callPatientDataApiWithRetry(String patientId, String promptType, String promptName, int maxRetries) {
        String url = "";
        int attempt = 0;
        
        while (attempt <= maxRetries) {
            long startTime = System.currentTimeMillis();
            totalApiCalls.incrementAndGet();
            
            try {
                // 调用患者数据API - 使用配置的基础URL
                url = apiProperties.getBase().getUrl() + "/api/ai/patient-data?patientId=" + patientId +
                        "&promptType=" + promptType +
                        "&promptName=" + promptName;
                
                System.out.println("第" + (attempt + 1) + "次尝试获取患者数据 - URL: " + url);
                
                String result = restTemplate.getForObject(url, String.class);
                
                // 去除每行开头的"data:"前缀
                if (result != null) {
                    result = result.replaceAll("(?m)^data:", "").trim();
                    // 检查结果是否为空或包含错误信息
                    if (result.isEmpty() || result.contains("失败") || result.contains("错误")) {
                        System.err.println("获取的患者数据无效 - URL: " + url + ", 结果: " + result);
                        failedApiCalls.incrementAndGet();
                        return null;
                    }
                }
                
                long endTime = System.currentTimeMillis();
                long processingTime = endTime - startTime;
                totalProcessingTime.addAndGet(processingTime);
                successfulApiCalls.incrementAndGet();
                
                System.out.println("成功获取患者数据 - URL: " + url + ", 结果长度: " + (result != null ? result.length() : 0) + ", 耗时: " + processingTime + "ms");
                return result;
                
            } catch (Exception e) {
                long endTime = System.currentTimeMillis();
                long processingTime = endTime - startTime;
                totalProcessingTime.addAndGet(processingTime);
                
                attempt++;
                if (attempt > maxRetries) {
                    failedApiCalls.incrementAndGet();
                    System.err.println("调用患者数据API失败，达到最大重试次数(" + maxRetries + ") - URL: " + url + ", 错误: " + e.getMessage() + ", 耗时: " + processingTime + "ms");
                    e.printStackTrace();
                    return null;
                } else {
                    System.err.println("第" + attempt + "次调用患者数据API失败，准备重试 - URL: " + url + ", 错误: " + e.getMessage() + ", 耗时: " + processingTime + "ms");
                    // 简单的重试延迟
                    try {
                        Thread.sleep(1000 * attempt); // 指数退避的简单实现
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * 调用Prompt模板API获取模板内容
     * 
     * @param promptType Prompt类型
     * @param promptName Prompt名称
     * @return 处理后的模板内容字符串，已去除SSE格式的"data:"前缀
     */
    private String callPromptApi(String promptType, String promptName) {
        String url = "";
        try {
            // 调用Prompt模板API - 使用配置的基础URL
            url = apiProperties.getBase().getUrl() + "/api/ai/prompt?promptType=" +
                    promptType +
                    "&promptName=" + promptName;
            String result = restTemplate.getForObject(url, String.class);
            // 去除每行开头的"data:"前缀
            if (result != null) {
                result = result.replaceAll("(?m)^data:", "").trim();
            }
            System.out.println("成功获取Prompt模板 - URL: " + url + ", 结果: " + result);
            return result;
        } catch (Exception e) {
            System.err.println("调用Prompt API失败 - URL: " + url + ", 错误: " + e.getMessage());
            e.printStackTrace();
            return "获取Prompt模板失败";
        }
    }

    /**
     * 查找有未分析手术记录的患者列表（优化版 - 只查询在院患者）
     * 
     * @return 有未分析手术记录的患者ID列表
     * 
     * @description
     * 通过三表JOIN查询，一次性获取在院且有未分析手术记录的患者。
     * 相比原方案，大幅减少数据量，提高查询效率。
     * 
     * @process
     * 1. 查询PATIENTS、EMR_CONTENT、SURGERYNAME三表
     * 2. 过滤条件：在院患者 + 有手术记录 + 未分析
     * 3. 返回患者ID列表
     * 
     * @example
     * List<String> patients = findPatientsWithUnanalyzedOperations();
     * // 返回如 ["99050801275226_1", "99050801275226_2"] 的列表
     */
    private List<String> findPatientsWithUnanalyzedOperations() {
        long startTime = System.currentTimeMillis();
        List<String> patientsWithUnanalyzedOperations = new ArrayList<>();
        
        try {
            // 使用优化后的三表JOIN查询（从EMR_CONTENT表）
            patientsWithUnanalyzedOperations = emrContentRepository.findInHospitalPatientsWithUnanalyzedOperations();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            System.out.println("查询在院未分析手术记录患者完成");
            System.out.println("找到患者数: " + patientsWithUnanalyzedOperations.size());
            System.out.println("查询耗时: " + duration + "ms");
            
        } catch (Exception e) {
            System.err.println("查找未分析手术记录患者失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return patientsWithUnanalyzedOperations;
    }

    /**
     * 分析患者的手术记录
     * 
     * @param patientId 患者ID
     * @return 分析是否成功
     * 
     * @description
     * 调用手术分析API对指定患者的手术记录进行分析，并将结果保存到数据库。
     * 
     * @process
     * 1. 调用手术分析API
     * 2. 检查API响应状态
     * 3. 记录分析结果
     * 
     * @example
     * boolean success = analyzePatientOperations("99050801275226_1");
     * // 返回true表示分析成功，false表示失败
     */
    private boolean analyzePatientOperations(String patientId) {
        try {
            // 调用手术分析API
            String url = apiProperties.getBase().getUrl() + "/api/operations/analyze-names?patientId=" + patientId;
            
            System.out.println("调用手术分析API: " + url);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                System.out.println("患者 " + patientId + " 的手术分析成功: " + responseBody);
                return true;
            } else {
                System.err.println("患者 " + patientId + " 的手术分析失败，状态码: " + response.getStatusCode());
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("患者 " + patientId + " 的手术分析异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取性能监控指标
     * 
     * @return 性能指标统计信息
     * 
     * @description
     * 返回当前系统的性能监控指标，包括API调用统计、处理时间、成功率等。
     * 
     * @process
     * 1. 收集所有性能指标
     * 2. 计算成功率
     * 3. 返回格式化结果
     * 
     * @example
     * Map<String, Object> metrics = getPerformanceMetrics();
     * // 返回包含各种性能指标的Map
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        long totalApi = totalApiCalls.get();
        long successfulApi = successfulApiCalls.get();
        long failedApi = failedApiCalls.get();
        long totalTime = totalProcessingTime.get();
        long totalPatients = totalPatientsProcessed.get();
        
        // 计算成功率
        double successRate = totalApi > 0 ? (double) successfulApi / totalApi * 100 : 0.0;
        double averageProcessingTime = totalPatients > 0 ? (double) totalTime / totalPatients : 0.0;
        
        metrics.put("totalApiCalls", totalApi);
        metrics.put("successfulApiCalls", successfulApi);
        metrics.put("failedApiCalls", failedApi);
        metrics.put("successRate", String.format("%.2f%%", successRate));
        metrics.put("totalProcessingTimeMs", totalTime);
        metrics.put("totalPatientsProcessed", totalPatients);
        metrics.put("averageProcessingTimeMs", String.format("%.2f", averageProcessingTime));
        metrics.put("lastUpdate", LocalDateTime.now().toString());
        
        return metrics;
    }

    /**
     * 重置性能监控指标
     * 
     * @description
     * 重置所有性能监控指标，用于重新开始统计。
     * 
     * @process
     * 1. 将所有原子计数器重置为0
     * 2. 记录重置时间
     * 
     * @example
     * resetPerformanceMetrics();
     * // 所有性能指标重置为0
     */
    public void resetPerformanceMetrics() {
        totalApiCalls.set(0);
        successfulApiCalls.set(0);
        failedApiCalls.set(0);
        totalProcessingTime.set(0);
        totalPatientsProcessed.set(0);
        System.out.println("性能监控指标已重置 - " + LocalDateTime.now());
    }

    /**
     * 打印性能监控报告
     * 
     * @description
     * 打印当前性能监控指标的详细报告到控制台。
     * 
     * @process
     * 1. 获取当前性能指标
     * 2. 格式化输出报告
     * 3. 打印到控制台
     * 
     * @example
     * printPerformanceReport();
     * // 在控制台输出性能报告
     */
    public void printPerformanceReport() {
        Map<String, Object> metrics = getPerformanceMetrics();
        
        System.out.println("=== Prompt生成性能监控报告 ===");
        System.out.println("总API调用次数: " + metrics.get("totalApiCalls"));
        System.out.println("成功API调用次数: " + metrics.get("successfulApiCalls"));
        System.out.println("失败API调用次数: " + metrics.get("failedApiCalls"));
        System.out.println("API调用成功率: " + metrics.get("successRate"));
        System.out.println("总处理时间(ms): " + metrics.get("totalProcessingTimeMs"));
        System.out.println("总处理患者数: " + metrics.get("totalPatientsProcessed"));
        System.out.println("平均处理时间(ms): " + metrics.get("averageProcessingTimeMs"));
        System.out.println("最后更新时间: " + metrics.get("lastUpdate"));
        System.out.println("=================================");
    }

    /**
     * AI分析阅读状态检查
     * 
     * 检查患者当天的AI分析阅读情况，确保至少有1条AI分析（诊断分析、鉴别诊断分析、诊疗计划建议）已读
     * 
     * @param patientId 患者ID
     * @param todayStart 当天开始时间
     * @return true如果readCount>=AI_ANALYSIS_READ_THRESHOLD，false否则
     * 
     * @since 2026-01-31
     * @author TDD
     * @version 1.2 - 修改：阅读门槛调整为readCount>=1
     */
    public boolean checkReadStatus(String patientId, LocalDateTime todayStart) {
        int readCount = promptResultRepository.countReadAiResultsSince(patientId, todayStart);
        boolean isQualified = readCount >= AI_ANALYSIS_READ_THRESHOLD;
        
        if (!isQualified) {
            System.out.println("患者 " + patientId + " AI分析未阅读，跳过 (readCount=" + readCount + ")");
        }
        
        return isQualified;
    }

    /**
     * 确定查房记录模板和时间间隔
     * 
     * 根据患者状态映射到对应的查房记录模板和时间间隔：
     * - 病危 → "病危每日查房记录" + 24小时
     * - 病重 → "病重每2日查房记录" + 48小时
     * - 其他 → "查房记录" + 24小时
     * 
     * @param status 患者状态
     * @return 模板配置对象
     * 
     * @since 2026-01-31
     * @author TDD
     * @version 1.1 - 重构：提取常量，使用常量替代硬编码
     */
    public TemplateConfig determineTemplateAndInterval(String status) {
        if ("病危".equals(status)) {
            return new TemplateConfig(TEMPLATE_CRITICAL_DAILY, INTERVAL_DAILY);
        } else if ("病重".equals(status)) {
            return new TemplateConfig(TEMPLATE_SERIOUS_BIDAILY, INTERVAL_BIDAILY);
        } else {
            // 默认使用标准查房记录模板
            return new TemplateConfig(TEMPLATE_STANDARD, INTERVAL_DAILY);
        }
    }

    /**
     * 检查时间间隔是否满足
     * 
     * 检查患者上次生成同模板查房记录距今是否满足规定的时间间隔。
     * 如果无历史记录，则允许生成（首次生成）。
     * 
     * @param patientId 患者ID
     * @param templateName 模板名称
     * @param intervalHours 时间间隔（小时）
     * @param currentTime 当前时间
     * @return true如果满足间隔要求（可以生成），false否则（跳过）
     * 
     * @since 2026-01-31
     * @author TDD
     * @version 1.1 - 重构：添加日志记录
     */
    public boolean checkTimeInterval(String patientId, String templateName, int intervalHours, LocalDateTime currentTime) {
        Optional<Prompt> lastPromptOpt = promptRepository
            .findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(patientId, templateName);

        if (lastPromptOpt.isPresent()) {
            Duration duration = Duration.between(
                lastPromptOpt.get().getSubmissionTime(), 
                currentTime);
            long hoursElapsed = duration.toHours();
            
            if (hoursElapsed < intervalHours) {
                System.out.println("患者 " + patientId + " 模板[" + templateName + "]未到生成时间，跳过 (已过" + hoursElapsed + "小时，需" + intervalHours + "小时)");
                return false; // 间隔未满足，跳过
            }
        }
        
        // 无历史记录或间隔已满足，允许生成
        return true;
    }

    /**
     * 检查当天是否已生成同模板查房记录
     * 
     * 检查患者当天是否已经生成过相同模板的查房记录。
     * 用于防止同一天重复生成相同模板的记录。
     * 
     * <p>注意：不同模板互不影响，即使当天已生成模板A，仍可以生成模板B。</p>
     * 
     * @param patientId 患者ID
     * @param templateName 模板名称
     * @param currentTime 当前时间
     * @return true如果当天已生成（应跳过），false如果当天未生成（允许生成）
     * 
     * @since 2026-01-31
     * @author TDD
     * @version 1.1 - 重构：添加日志记录，改进注释
     */
    public boolean hasGeneratedToday(String patientId, String templateName, LocalDateTime currentTime) {
        LocalDateTime todayStart = currentTime.toLocalDate().atStartOfDay();
        
        long count = promptRepository.countByPatientIdAndPromptTemplateNameAndSubmissionTimeAfter(
            patientId, templateName, todayStart);
        
        boolean hasGenerated = count > 0;
        
        if (hasGenerated) {
            System.out.println("患者 " + patientId + " 模板[" + templateName + "]当天已生成，跳过 (当天已有" + count + "条记录)");
        }
        
        return hasGenerated;
    }

    /**
     * 为中午12点查房记录生成并保存Prompt
     * 
     * 整合所有检查逻辑：
     * 1. AI分析阅读状态检查
     * 2. 时间间隔检查
     * 3. 当天重复生成检查
     * 4. 调用API获取数据和模板
     * 5. 保存到数据库
     * 
     * @description 执行中午查房记录生成逻辑，包含以下检查：
     *              1. 检查诊断分析是否已阅读完成；
     *              2. 检查当天是否已生成同模板查房记录；
     *              3. 检查与最近一次同模板记录之间的时间间隔；
     *              4. 调用 /api/ai/patient-data 获取患者数据，仅在返回内容非空、不包含“失败”且不包含“无入院记录数据”时才允许生成查房 Prompt，
     *                 以保证仅在存在入院病历资料的前提下生成查房分析。
     * 
     * @param patientId 患者ID
     * @param templateName 模板名称
     * @param promptType Prompt类型
     * @param currentTime 当前时间
     * @return 成功生成的Prompt对象，如果跳过则返回null
     * 
     * @since 2026-01-31
     * @author TDD
     * @version 1.0 - 绿阶段：最小可行实现
     */
    public Prompt generateAndSavePromptForNoonWardRound(String patientId, String templateName, 
                                                         String promptType, LocalDateTime currentTime) {
        try {
            // 步顈1：检查诊断分析阅读状态（至少有1条诊断分析已读）
            LocalDateTime todayStart = currentTime.toLocalDate().atStartOfDay();
            int readCount = promptResultRepository.countReadDiagnosisAnalysisSince(patientId, todayStart);
            if (readCount < AI_ANALYSIS_READ_THRESHOLD) {
                return null; // 诊断分析未阅读完成，跳过
            }
            
            // 步顈2：检查当天是否已生成
            if (hasGeneratedToday(patientId, templateName, currentTime)) {
                return null; // 当天已生成，跳过
            }
            
            // 步顈3：检查时间间隔（根据模板名称确定间隔）
            int intervalHours = determineInterval(templateName);
            if (!checkTimeInterval(patientId, templateName, intervalHours, currentTime)) {
                return null; // 间隔未满足，跳过
            }
            
            // 步顺4：使用新的buildWardRoundObjectiveContent方法构建查房记录内容
            String patientData = buildWardRoundObjectiveContent(patientId, templateName, currentTime);
            if (patientData == null || patientData.isEmpty()) {
                return null; // 数据构建失败，跳过
            }
            
            // 步顈5：调用API获取模板内容
            String promptTemplate = callPromptApi(promptType, templateName);
            
            // 步顈6：保存到数据库
            Prompt prompt = new Prompt();
            prompt.setPatientId(patientId);
            prompt.setPromptTemplateName(templateName);
            prompt.setStatusName("待处理");
            prompt.setSubmissionTime(currentTime);
            prompt.setExecutionTime(null);
            prompt.setPriority(5);
            prompt.setRetryCount(0);
            prompt.setGeneratedBy("System-NoonWardRound");
            prompt.setSortNumber(0);
            prompt.setUserId(0);
            prompt.setObjectiveContent(patientData);
            prompt.setPromptTemplateContent(promptTemplate);
            prompt.setExecutionResult(null);
            
            promptRepository.saveAndFlush(prompt);
            
            // 记录日志
            promptGenerationLogService.logPromptGeneration(patientId, promptType, templateName);
            
            return prompt;
            
        } catch (Exception e) {
            System.err.println("生成Prompt失败 - 患者ID: " + patientId + ", 错误: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 根据模板名称确定时间间隔
     * 
     * @param templateName 模板名称
     * @return 时间间隔（小时）
     */
    private int determineInterval(String templateName) {
        if (TEMPLATE_CRITICAL_DAILY.equals(templateName)) {
            return INTERVAL_DAILY; // 24小时
        } else if (TEMPLATE_SERIOUS_BIDAILY.equals(templateName)) {
            return INTERVAL_BIDAILY; // 48小时
        }
        return INTERVAL_DAILY; // 默认24小时
    }

   /**
     * 查房提醒任务联动
     * 
     * 当病重患者生成查房记录后，自动更新对应的查房提醒任务状态。
     * 只有"病重每2日查房记录"模板会触发任务更新。
     * 
     * <p>设计原则：</p>
     * <ul>
     *   <li>只针对病重患者（"病重每2日查房记录"模板）</li>
     *   <li>异常不中断主流程，确保记录生成成功</li>
     *   <li>详细记录任务更新日志</li>
     * </ul>
     * 
     * @param patientId 患者ID
     * @param templateName 模板名称
     * @param currentTime 当前时间
     * 
     * @since 2026-01-31
     * @author TDD
     * @version 1.1 - 重构：增强日志记录，改进注释
     */
    public void linkToWardRoundReminderTask(String patientId, String templateName, LocalDateTime currentTime) {
        // 只有病重患者需要联动任务
        if (!TEMPLATE_SERIOUS_BIDAILY.equals(templateName)) {
            return;
        }
        
        try {
            System.out.println("开始更新查房提醒任务 - 患者ID: " + patientId + ", 模板: " + templateName);
            promptsTaskUpdater.updateTaskStatusForPrompt(templateName, patientId);
            System.out.println("查房提醒任务更新成功 - 患者ID: " + patientId);
        } catch (Exception e) {
            System.err.println("更新查房提醒任务失败 - 患者ID: " + patientId 
                + ", 模板: " + templateName 
                + ", 错误: " + e.getMessage());
            // 不中断流程：任务更新失败不应影响查房记录的生成
        }
    }

    /**
     * 查找患者上次查房时间
     * 
     * 通过查询Prompt表中最近一条同模板名的查房记录，获取其submissionTime作为lastRoundTime。
     * 用于后续按时间区间筛选化验、检查、医嘱等数据。
     * 
     * @param patientId    患者ID
     * @param templateName 查房模板名称（如"病危每日查房记录"）
     * @return 上次查房时间；若无历史记录则返回null
     * 
     * @since 2026-02-09
     * @author TDD
     * @version 1.0 - 绿阶段完成：实现最小功能并通过所有测试
     */
   private LocalDateTime findLastWardRoundTime(String patientId, String templateName) {
        return promptRepository
            .findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(patientId, templateName)
            .map(Prompt::getSubmissionTime)
            .orElse(null);
    }

    /**
     * 获取本次病情记录文本（按时间区间筛选）
     * 
     * 根据lastRoundTime和currentTime时间区间，筛选出该区间内的病历记录，
     * 用于生成查房记录中的"本次病情记录"段落。
     * 
     * @param patientId     患者ID
     * @param lastRoundTime 上次查房时间；若为null则为首次查房
     * @param currentTime   本次查房时间
     * @return 病历记录文本；区间内无记录时返回固定提示语
     * 
     * @since 2026-02-09
     * @author TDD
     * @version 1.1 - 重构：提取常量和辅助方法，提高代码可维护性
     */
    private String getCurrentMedicalRecordText(String patientId, LocalDateTime lastRoundTime, LocalDateTime currentTime) {
        // 获取所有病历记录（按时间倒序）
        List<com.example.medaiassistant.model.MedicalRecord> allRecords = 
            medicalRecordRepository.findByPatientIdAndDeletedOrderByRecordTimeDesc(patientId, 0);
        
        // 如果是首次查房（lastRoundTime为null），取最近N条记录
        if (lastRoundTime == null) {
            return formatFirstWardRoundRecords(allRecords);
        }
        
        // 非首次查房，按时间区间筛选
        return formatIntervalRecords(allRecords, lastRoundTime, currentTime);
    }

    /**
     * 格式化首次查房的病历记录
     * 
     * @param allRecords 所有病历记录
     * @return 格式化后的病历记录文本
     */
    private String formatFirstWardRoundRecords(List<com.example.medaiassistant.model.MedicalRecord> allRecords) {
        int maxRecords = Math.min(DEFAULT_MEDICAL_RECORDS_COUNT, allRecords.size());
        if (maxRecords == 0) {
            return "本次查房时间段内无病程记录";
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < maxRecords; i++) {
            appendMedicalRecordText(result, allRecords.get(i));
        }
        return result.toString().trim();
    }

    /**
     * 格式化时间区间内的病历记录
     * 
     * @param allRecords 所有病历记录
     * @param lastRoundTime 上次查房时间
     * @param currentTime 本次查房时间
     * @return 格式化后的病历记录文本
     */
    private String formatIntervalRecords(List<com.example.medaiassistant.model.MedicalRecord> allRecords, 
                                         LocalDateTime lastRoundTime, LocalDateTime currentTime) {
        StringBuilder result = new StringBuilder();
        int count = 0;
        
        for (com.example.medaiassistant.model.MedicalRecord record : allRecords) {
            LocalDateTime recordTime = convertDateToLocalDateTime(record.getRecordTime());
            
            // 检查是否在区间内 [lastRoundTime, currentTime]
            if (!recordTime.isBefore(lastRoundTime) && !recordTime.isAfter(currentTime)) {
                appendMedicalRecordText(result, record);
                count++;
            }
        }
        
        // 如果区间内无记录，返回固定提示语
        return count == 0 ? "本次查房时间段内无病程记录" : result.toString().trim();
    }

    /**
     * 将Date转换为LocalDateTime
     * 
     * @param date Date对象
     * @return LocalDateTime对象
     */
    private LocalDateTime convertDateToLocalDateTime(Date date) {
        return date.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
    }

    /**
     * 将病历记录追加到StringBuilder中
     * 
     * @param builder StringBuilder对象
     * @param record 病历记录
     */
    private void appendMedicalRecordText(StringBuilder builder, com.example.medaiassistant.model.MedicalRecord record) {
        builder.append("【记录时间：")
              .append(record.getRecordTime())
              .append(" 记录医生：")
              .append(record.getRecordingDoctor())
              .append("】\n")
              .append(record.getMedicalContent())
              .append("\n\n");
    }
    
    /**
     * 获取自上次查房以来的化验结果文本（按时间区间筛选）
     * 
     * @param patientId     患者ID
     * @param lastRoundTime 上次查房时间；若为null则为首次查房，使用最近2天的化验结果
     * @param currentTime   本次查房时间
     * @return 化验结果文本；区间内无记录时返回固定提示语
     * 
     * @author TDD
     * @version 1.2 - 重构：使用通用过滤方法，减少代码重复
     */
    private String getLabResultsSinceLastRound(String patientId, LocalDateTime lastRoundTime, LocalDateTime currentTime) {
        List<com.example.medaiassistant.model.LabResult> allResults = 
            labResultRepository.findByPatientId(patientId);
        
        List<com.example.medaiassistant.model.LabResult> filtered = filterResultsByTimeRange(
            allResults,
            lastRoundTime,
            currentTime,
            com.example.medaiassistant.model.LabResult::getLabReportTime
        );
        
        return filtered.isEmpty() ? "自上次查房以来无新的化验结果" : formatLabResults(filtered);
    }
    
    /**
     * 获取自上次查房以来的检查结果文本（按时间区间筛选）
     * 
     * @param patientId     患者ID
     * @param lastRoundTime 上次查房时间；若为null则为首次查房，使用最近2天的检查结果
     * @param currentTime   本次查房时间
     * @return 检查结果文本；区间内无记录时返回固定提示语
     * 
     * @author TDD
     * @version 1.1 - 重构：使用通用过滤方法，减少代码重复
     */
    private String getExaminationResultsSinceLastRound(String patientId, LocalDateTime lastRoundTime, LocalDateTime currentTime) {
        List<com.example.medaiassistant.model.ExaminationResult> allResults = 
            examinationResultRepository.findByPatientId(patientId);
        
        List<com.example.medaiassistant.model.ExaminationResult> filtered = filterResultsByTimeRange(
            allResults,
            lastRoundTime,
            currentTime,
            com.example.medaiassistant.model.ExaminationResult::getCheckReportTime
        );
        
        return filtered.isEmpty() ? "自上次查房以来无新的检查结果" : formatExaminationResults(filtered);
    }
    
    /**
     * 获取自上次查房以来的医嘱变更文本（按时间区间筛选）
     * <p>
     * 包含长期医嘱和临时医嘱，通过orderDate字段进行时间区间筛选。
     * 首次查房（lastRoundTime为null）时使用最近2天的医嘱。
     * </p>
     * 
     * @param patientId     患者ID
     * @param lastRoundTime 上次查房时间；若为null则为首次查房，使用最近2天的医嘱
     * @param currentTime   本次查房时间
     * @return 医嘱变更文本；区间内无记录时返回固定提示语
     * 
     * @author TDD
     * @version 1.0 - 新增：医嘱变更区间筛选
     */
    private String getTreatmentChangesSinceLastRound(String patientId, LocalDateTime lastRoundTime, LocalDateTime currentTime) {
        // 获取所有长期医嘱和临时医嘱
        List<com.example.medaiassistant.model.LongTermOrder> longTermOrders = 
            longTermOrderRepository.findByPatientIdAndRepeatIndicatorOrderByOrderDateAsc(patientId, 1);
        List<com.example.medaiassistant.model.LongTermOrder> temporaryOrders = 
            longTermOrderRepository.findByPatientIdAndRepeatIndicatorOrderByOrderDateAsc(patientId, 0);
        
        // 合并所有医嘱
        List<com.example.medaiassistant.model.LongTermOrder> allOrders = new ArrayList<>();
        allOrders.addAll(longTermOrders);
        allOrders.addAll(temporaryOrders);
        
        // 使用通用过滤方法筛选时间区间
        List<com.example.medaiassistant.model.LongTermOrder> filtered = filterResultsByTimeRange(
            allOrders,
            lastRoundTime,
            currentTime,
            com.example.medaiassistant.model.LongTermOrder::getOrderDate
        );
        
        return filtered.isEmpty() ? "自上次查房以来无新的治疗医嘱调整" : formatTreatmentOrders(filtered);
    }
    
    /**
     * 通用的时间区间过滤方法（支持任意类型）
     * 
     * @param <T>            结果类型
     * @param allResults     所有结果列表
     * @param lastRoundTime  上次查房时间；若为null则使用回溯天数
     * @param currentTime    本次查房时间
     * @param timeExtractor  时间字段提取器
     * @return 过滤后的结果列表
     * 
     * @author TDD
     * @version 1.0 - 重构：提取公共过滤逻辑
     */
    private <T> List<T> filterResultsByTimeRange(
            List<T> allResults,
            LocalDateTime lastRoundTime,
            LocalDateTime currentTime,
            java.util.function.Function<T, java.sql.Timestamp> timeExtractor) {
        
        // 计算时间区间
        LocalDateTime fromTime = (lastRoundTime == null) 
            ? currentTime.minusDays(DEFAULT_LAB_RESULTS_FALLBACK_DAYS)
            : lastRoundTime;
        
        // 过滤结果
        List<T> filtered = new ArrayList<>();
        for (T result : allResults) {
            java.sql.Timestamp timestamp = timeExtractor.apply(result);
            LocalDateTime reportTime = convertTimestampToLocalDateTime(timestamp);
            
            // 闭区间 [fromTime, currentTime]
            if (!reportTime.isBefore(fromTime) && !reportTime.isAfter(currentTime)) {
                filtered.add(result);
            }
        }
        
        return filtered;
    }
    
    /**
     * 将Timestamp转换为LocalDateTime
     */
    private LocalDateTime convertTimestampToLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
    }
    
    /**
     * 格式化化验结果为文本
     * （简单实现，用于测试通过）
     */
    private String formatLabResults(List<com.example.medaiassistant.model.LabResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (com.example.medaiassistant.model.LabResult result : results) {
            sb.append(result.getLabName() != null ? result.getLabName() : "未知项目")
              .append(" ")
              .append(result.getLabResult() != null ? result.getLabResult() : "")
              .append(" ")
              .append(result.getUnit() != null ? result.getUnit() : "")
              .append("\n");
        }
        
        return sb.toString().trim();
    }
    
    /**
     * 格式化检查结果为文本
     * （简单实现，用于测试通过）
     */
    private String formatExaminationResults(List<com.example.medaiassistant.model.ExaminationResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (com.example.medaiassistant.model.ExaminationResult result : results) {
            sb.append(result.getCheckName() != null ? result.getCheckName() : "未知检查项目")
              .append(" ")
              .append(result.getCheckType() != null ? result.getCheckType() : "")
              .append("\n");
        }
        
        return sb.toString().trim();
    }
    
    /**
     * 格式化医嘱为文本
     * （包含长期医嘱和临时医嘱）
     * 
     * @param orders 医嘱列表
     * @return 格式化后的文本
     * @author TDD
     * @version 1.0 - 新增：医嘱格式化
     */
    private String formatTreatmentOrders(List<com.example.medaiassistant.model.LongTermOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (com.example.medaiassistant.model.LongTermOrder order : orders) {
            // 医嘱名称
            sb.append(order.getOrderName() != null ? order.getOrderName() : "未知医嘱")
              .append(" ");
            
            // 剂量
            if (order.getDosage() != null && !order.getDosage().isEmpty()) {
                sb.append(order.getDosage());
                if (order.getUnit() != null && !order.getUnit().isEmpty()) {
                    sb.append(order.getUnit());
                }
                sb.append(" ");
            }
            
            // 频次
            if (order.getFrequency() != null && !order.getFrequency().isEmpty()) {
                sb.append(order.getFrequency()).append(" ");
            }
            
            // 给药途径
            if (order.getRoute() != null && !order.getRoute().isEmpty()) {
                sb.append(order.getRoute());
            }
            
            sb.append("\n");
        }
        
        return sb.toString().trim();
    }
    
    /**
     * 中午12点查房记录自动生成任务
     * 
     * 每天中午12点触发,为符合条件的患者生成查房记录。
     * 
     * <p>处理流程：</p>
     * <ol>
     *   <li>分页获取所有在院患者</li>
     *   <li>对每个患者执行以下检查：
     *     <ul>
     *       <li>AI分析阅读状态检查</li>
     *       <li>根据患者状态选择模板</li>
     *       <li>时间间隔检查</li>
     *       <li>当天重复生成检查</li>
     *     </ul>
     *   </li>
     *   <li>生成并保存Prompt</li>
     *   <li>联动查房提醒任务（仅病重患者）</li>
     *   <li>输出统计信息</li>
     * </ol>
     * 
     * @since 2026-01-31
     * @author TDD
     * @version 1.0 - 绿阶段：最小可行实现
     */
    @Scheduled(cron = "${scheduling.timer.noon-ward-round-time:0 0 12 * * *}")
    public void generateNoonWardRoundPrompts() {
        System.out.println("开始执行中午12点查房记录生成任务");
        long startTime = System.currentTimeMillis();
        
        // 统计变量
        int totalPatientsProcessed = 0;
        int totalPromptsGenerated = 0;
        int totalFailures = 0;
        
        try {
            int page = 0;
            int pageSize = 20;
            boolean hasMore = true;
            
            while (hasMore) {
                // 分页获取在院患者
                List<Patient> inHospitalPatients = findInHospitalPatientsByPage(page, pageSize);
                
                if (inHospitalPatients.isEmpty()) {
                    hasMore = false;
                    break;
                }
                
                System.out.println("处理第 " + page + " 页，共 " + inHospitalPatients.size() + " 个患者");
                
                // 处理每个患者
                for (Patient patient : inHospitalPatients) {
                    try {
                        totalPatientsProcessed++;
                        
                        String patientId = patient.getPatientId();
                        String patientStatus = patient.getStatus();
                        LocalDateTime now = LocalDateTime.now();
                        
                        // 步顷1：确定模板和间隔
                        TemplateConfig config = determineTemplateAndInterval(patientStatus);
                        String templateName = config.getTemplateName();
                        int intervalHours = config.getIntervalHours();
                        
                        // 步顷2：执行所有检查并生成Prompt
                        Prompt prompt = generateAndSavePromptForNoonWardRound(
                            patientId, templateName, "病情小结", now
                        );
                        
                        if (prompt != null) {
                            totalPromptsGenerated++;
                            
                            // 步顷3：联动查房提醒任务
                            linkToWardRoundReminderTask(patientId, templateName, now);
                        }
                        
                    } catch (Exception e) {
                        totalFailures++;
                        System.err.println("处理患者失败 - 患者ID: " + patient.getPatientId() 
                            + ", 错误: " + e.getMessage());
                        // 继续处理下一个患者
                    }
                }
                
                page++;
            }
            
        } catch (Exception e) {
            System.err.println("中午12点查房记录生成任务失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 输出统计信息
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("\n========== 中午12点查房记录生成任务完成 ==========" );
        System.out.println("处理患者总数: " + totalPatientsProcessed);
        System.out.println("生成Prompt数: " + totalPromptsGenerated);
        System.out.println("失败次数: " + totalFailures);
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("====================================================\n");
    }

    /**
     * 获取病人基本信息
     * <p>
     * 包含性别、年龄、入院时间、住院天数等基础信息。
     * </p>
     * 
     * @param patientId 患者ID
     * @return 病人基本信息文本
     * 
     * @author TDD
     * @version 1.0 - 任务6新增
     */
    private String getPatientBasicInfo(String patientId) {
        Optional<Patient> patientOpt = patientRepository.findById(patientId);
        if (!patientOpt.isPresent()) {
            return "未找到患者信息";
        }

        Patient patient = patientOpt.get();

        // 使用安全的年龄计算方法
        int age = AgeCalculator.calculateAge(patient.getDateOfBirth(), 0);

        // 计算住院天数
        java.time.LocalDate admissionDate = patient.getAdmissionTime().toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
        long days = java.time.temporal.ChronoUnit.DAYS.between(admissionDate, java.time.LocalDate.now());

        // 格式化日期
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy年MM月dd日 HH:mm");
        String admissionTime = sdf.format(patient.getAdmissionTime());

        // 转换性别为中文
        String genderInChinese;
        String gender = patient.getGender();
        if (gender != null) {
            switch (gender.toUpperCase()) {
                case "M":
                case "1":
                    genderInChinese = "男";
                    break;
                case "F":
                case "2":
                    genderInChinese = "女";
                    break;
                default:
                    genderInChinese = "未确定";
            }
        } else {
            genderInChinese = "未确定";
        }

        return String.format("性别：%s，年龄：%d岁，入院时间：%s，住院时间：%d天。",
                genderInChinese, age, admissionTime, days);
    }

    /**
     * 获取上次查房记录摘要
     * <p>
     * 若存在上次查房记录，返回其objectiveContent的前500字符作为摘要；
     * 若无上次记录，返回"首次查房"。
     * </p>
     * 
     * @param patientId    患者ID
     * @param templateName 模板名称
     * @return 上次查房记录摘要文本
     * 
     * @author TDD
     * @version 1.0 - 任务6新增
     */
    private String getLastRoundSummary(String patientId, String templateName) {
        Optional<Prompt> lastPromptOpt = promptRepository
            .findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(patientId, templateName);
        
        if (!lastPromptOpt.isPresent()) {
            return "首次查房，无历史记录";
        }
        
        Prompt lastPrompt = lastPromptOpt.get();
        String objectiveContent = lastPrompt.getObjectiveContent();
        
        if (objectiveContent == null || objectiveContent.isEmpty()) {
            return "上次查房记录为空";
        }
        
        // 返回前500字符作为摘要
        if (objectiveContent.length() > 500) {
            return objectiveContent.substring(0, 500) + "...";
        }
        
        return objectiveContent;
    }

    /**
     * 获取目前诊断
     * <p>
     * 返回患者的当前诊断信息。
     * 目前从Patient实体获取重要信息字段或返回默认值。
     * 未来可扩展从EMR记录或其他诊断表中获取。
     * </p>
     * 
     * @param patientId 患者ID
     * @return 目前诊断文本
     * 
     * @author TDD
     * @version 1.0 - 任务6新增
     */
    private String getCurrentDiagnosis(String patientId) {
        Optional<Patient> patientOpt = patientRepository.findById(patientId);
        if (!patientOpt.isPresent()) {
            return "未找到患者信息";
        }
        
        Patient patient = patientOpt.get();
        
        // 尝试从重要信息字段获取（可能包含诊断信息）
        String importantInfo = patient.getImportantInformation();
        if (importantInfo != null && !importantInfo.trim().isEmpty()) {
            // 如果重要信息字段包含诊断相关内容，返回前200字符
            if (importantInfo.length() > 200) {
                return importantInfo.substring(0, 200) + "...";
            }
            return importantInfo;
        }
        
        // TODO: 未来可扩展从EMR记录或专门的诊断表中获取诊断信息
        return "暂无诊断信息";
    }

    /**
     * 构建查房记录的objectiveContent
     * <p>
     * 按固定顺序将7个数据块结构化拼装：
     * </p>
     * <ol>
     *   <li>【病人基本信息】</li>
     *   <li>【上次查房记录摘要】</li>
     *   <li>【本次病情记录（含主诉与查体原文）】</li>
     *   <li>【自上次查房以来的化验结果】</li>
     *   <li>【自上次查房以来的检查结果】</li>
     *   <li>【目前诊断】</li>
     *   <li>【最近医嘱和治疗措施】</li>
     * </ol>
     * 
     * @param patientId    患者ID
     * @param templateName 模板名称
     * @param currentTime  当前查房时间
     * @return 结构化的查房记录文本
     * 
     * @author TDD
     * @version 1.0 - 任务6新增
     */
    private String buildWardRoundObjectiveContent(String patientId, String templateName, LocalDateTime currentTime) {
        // 获取上次查房时间
        LocalDateTime lastRoundTime = findLastWardRoundTime(patientId, templateName);
        
        // 获取各数据块
        String basicInfo = getPatientBasicInfo(patientId);
        String lastRoundSummary = getLastRoundSummary(patientId, templateName);
        String currentMedicalRecord = getCurrentMedicalRecordText(patientId, lastRoundTime, currentTime);
        String labResults = getLabResultsSinceLastRound(patientId, lastRoundTime, currentTime);
        String examResults = getExaminationResultsSinceLastRound(patientId, lastRoundTime, currentTime);
        String diagnosis = getCurrentDiagnosis(patientId);
        String treatmentChanges = getTreatmentChangesSinceLastRound(patientId, lastRoundTime, currentTime);
        
        // 处理null值，替换为默认占位符
        basicInfo = (basicInfo != null && !basicInfo.isEmpty()) ? basicInfo : "无基本信息";
        lastRoundSummary = (lastRoundSummary != null && !lastRoundSummary.isEmpty()) ? lastRoundSummary : "无上次查房记录";
        currentMedicalRecord = (currentMedicalRecord != null && !currentMedicalRecord.isEmpty()) ? currentMedicalRecord : "本次查房时间段内无病程记录";
        labResults = (labResults != null && !labResults.isEmpty()) ? labResults : "自上次查房以来无新的化验结果";
        examResults = (examResults != null && !examResults.isEmpty()) ? examResults : "自上次查房以来无新的检查结果";
        diagnosis = (diagnosis != null && !diagnosis.isEmpty()) ? diagnosis : "暂无诊断信息";
        treatmentChanges = (treatmentChanges != null && !treatmentChanges.isEmpty()) ? treatmentChanges : "自上次查房以来无新的治疗医嘱调整";
        
        // 按固定顺序拼装
        StringBuilder content = new StringBuilder();
        content.append("【病人基本信息】\n");
        content.append(basicInfo).append("\n\n");
        
        content.append("【上次查房记录摘要】\n");
        content.append(lastRoundSummary).append("\n\n");
        
        content.append("【本次病情记录（含主诉与查体原文）】\n");
        content.append(currentMedicalRecord).append("\n\n");
        
        content.append("【自上次查房以来的化验结果】\n");
        content.append(labResults).append("\n\n");
        
        content.append("【自上次查房以来的检查结果】\n");
        content.append(examResults).append("\n\n");
        
        content.append("【目前诊断】\n");
        content.append(diagnosis).append("\n\n");
        
        content.append("【最近医嘱和治疗措施】\n");
        content.append(treatmentChanges).append("\n");
        
        return content.toString();
    }

}
