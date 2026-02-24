package com.example.medaiassistant.service;

import com.example.medaiassistant.config.ApiProperties;
import com.example.medaiassistant.config.SchedulingProperties;
import com.example.medaiassistant.repository.EmrRecordRepository;
import com.example.medaiassistant.repository.EmrContentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 独立的手术分析服务类
 * 
 * 该服务专门处理手术分析任务，与TimerPromptGenerator完全分离
 * 使用专用线程池执行异步任务，避免受其他定时任务影响
 * 
 * @author Cline
 * @since 2025-09-27
 */
@Service
@EnableAsync
@Slf4j
public class SurgeryAnalysisService {
    
    private final EmrRecordRepository emrRecordRepository;
    private final EmrContentRepository emrContentRepository;
    private final RestTemplate restTemplate;
    private final ApiProperties apiProperties;
    private final SchedulingProperties schedulingProperties;
    
    public SurgeryAnalysisService(EmrRecordRepository emrRecordRepository,
                                EmrContentRepository emrContentRepository,
                                RestTemplate restTemplate,
                                ApiProperties apiProperties,
                                SchedulingProperties schedulingProperties) {
        this.emrRecordRepository = emrRecordRepository;
        this.emrContentRepository = emrContentRepository;
        this.restTemplate = restTemplate;
        this.apiProperties = apiProperties;
        this.schedulingProperties = schedulingProperties;
    }
    
    /**
     * 定时执行手术分析任务
     * 根据配置决定是否执行，支持动态启停
     */
    @Async("surgeryAnalysisExecutor")
    @Scheduled(cron = "${scheduling.timer.surgery-analysis-time:0 0 8 * * *}")
    public void executeSurgeryAnalysisAsync() {
        // 检查定时任务是否启用
        if (!schedulingProperties.getTimer().isSurgeryAnalysisEnabled()) {
            log.info("手术分析定时任务已禁用，跳过执行");
            return;
        }
        
        log.info("开始定时执行手术分析任务 - 使用专用线程池");
        
        // 定时任务不需要返回结果，直接执行内部逻辑
        try {
            executeSurgeryAnalysisInternal().get(300, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("定时手术分析任务执行失败", e);
        }
    }
    
    /**
     * 立即执行手术分析任务（忽略定时任务开关）
     * 
     * @return 包含执行结果的CompletableFuture
     */
    @Async("surgeryAnalysisExecutor")
    public CompletableFuture<Map<String, Object>> executeSurgeryAnalysisNow() {
        log.info("立即执行手术分析任务 - 忽略定时任务开关");
        return executeSurgeryAnalysisInternal();
    }
    
    /**
     * 内部执行逻辑 - 实际的业务处理
     * 
     * @return 包含执行结果的CompletableFuture
     */
    private CompletableFuture<Map<String, Object>> executeSurgeryAnalysisInternal() {
        long startTime = System.currentTimeMillis();
        
        Map<String, Object> result = new HashMap<>();
        AtomicInteger totalPatientsProcessed = new AtomicInteger(0);
        AtomicInteger totalAnalysesCompleted = new AtomicInteger(0);
        AtomicInteger totalAnalysesFailed = new AtomicInteger(0);
        
        try {
            // 查找有未分析手术记录的患者
            List<String> patients = findPatientsWithUnanalyzedOperations();
            
            if (patients.isEmpty()) {
                log.info("没有需要分析手术记录的患者");
                result.put("message", "没有需要分析手术记录的患者");
                result.put("status", "COMPLETED");
                result.put("totalPatientsProcessed", 0);
                result.put("analysesCompleted", 0);
                result.put("analysesFailed", 0);
                result.put("durationSeconds", 0);
                return CompletableFuture.completedFuture(result);
            }
            
            log.info("找到 {} 个有未分析手术记录的患者", patients.size());
            
            // 使用专用线程池执行并发任务
            ExecutorService executor = Executors.newFixedThreadPool(5);
            List<CompletableFuture<Boolean>> futures = patients.stream()
                .map(patientId -> CompletableFuture.supplyAsync(() -> 
                    analyzePatientOperations(patientId), executor))
                .collect(Collectors.toList());
            
            // 等待所有任务完成，设置超时时间
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .orTimeout(300, TimeUnit.SECONDS)
                .join();
            
            // 统计执行结果
            for (CompletableFuture<Boolean> future : futures) {
                try {
                    if (future.get()) {
                        totalAnalysesCompleted.incrementAndGet();
                    } else {
                        totalAnalysesFailed.incrementAndGet();
                    }
                    totalPatientsProcessed.incrementAndGet();
                } catch (Exception e) {
                    totalAnalysesFailed.incrementAndGet();
                    log.error("获取任务结果失败", e);
                }
            }
            
            // 优雅关闭线程池
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
        } catch (Exception e) {
            log.error("手术分析任务执行失败", e);
            result.put("error", e.getMessage());
            result.put("status", "FAILED");
        }
        
        long duration = (System.currentTimeMillis() - startTime) / 1000;
        result.put("totalPatientsProcessed", totalPatientsProcessed.get());
        result.put("analysesCompleted", totalAnalysesCompleted.get());
        result.put("analysesFailed", totalAnalysesFailed.get());
        result.put("durationSeconds", duration);
        result.put("status", "COMPLETED");
        
        log.info("手术分析任务完成 - 处理患者: {}, 成功: {}, 失败: {}, 耗时: {}秒", 
                totalPatientsProcessed.get(), totalAnalysesCompleted.get(), 
                totalAnalysesFailed.get(), duration);
        
        return CompletableFuture.completedFuture(result);
    }
    
    /**
     * 手动执行手术分析任务（供API调用）
     * 
     * @return 执行结果统计信息
     */
    @Async("surgeryAnalysisExecutor")
    public CompletableFuture<Map<String, Object>> executeSurgeryAnalysisManual() {
        log.info("手动执行手术分析任务");
        return executeSurgeryAnalysisInternal();
    }
    
    /**
     * 查找有未分析手术记录的患者列表（支持科室过滤）
     * 
     * @return 患者ID列表
     */
    List<String> findPatientsWithUnanalyzedOperations() {
        long startTime = System.currentTimeMillis();
        List<String> patientsWithUnanalyzedOperations;
        
        try {
            // 检查是否启用科室过滤
            if (schedulingProperties.getTimer().isDepartmentFilterEnabled()) {
                List<String> targetDepartments = schedulingProperties.getTimer().getTargetDepartments();
                
                // 记录科室过滤状态
                if (targetDepartments != null && !targetDepartments.isEmpty()) {
                    log.info("启用科室过滤，目标科室: {}", targetDepartments);
                } else {
                    log.info("启用科室过滤，但目标科室列表为空，返回空结果");
                }
                
                // 使用安全方法查询目标科室患者（从EMR_CONTENT表）
                patientsWithUnanalyzedOperations = emrContentRepository.findInHospitalPatientsWithUnanalyzedOperationsByDepartmentsSafe(targetDepartments);
            } else {
                // 未启用科室过滤，查询所有在院患者（从EMR_CONTENT表）
                log.info("未启用科室过滤，处理所有在院患者");
                patientsWithUnanalyzedOperations = emrContentRepository.findInHospitalPatientsWithUnanalyzedOperations();
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("查询在院未分析手术记录患者完成 - 找到患者数: {}, 查询耗时: {}ms", 
                    patientsWithUnanalyzedOperations.size(), duration);
            
        } catch (Exception e) {
            log.error("查找未分析手术记录患者失败", e);
            throw new RuntimeException("查询患者数据失败", e);
        }
        
        return patientsWithUnanalyzedOperations;
    }
    
    /**
     * 分析患者的手术记录
     * 
     * @param patientId 患者ID
     * @return 分析是否成功
     */
    private boolean analyzePatientOperations(String patientId) {
        try {
            String url = apiProperties.getBase().getUrl() + "/api/operations/analyze-names?patientId=" + patientId;
            
            log.debug("调用手术分析API: {}", url);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("患者 {} 的手术分析成功", patientId);
                return true;
            } else {
                log.error("患者 {} 的手术分析失败，状态码: {}", patientId, response.getStatusCode());
                return false;
            }
            
        } catch (Exception e) {
            log.error("患者 {} 的手术分析异常", patientId, e);
            return false;
        }
    }
    
    /**
     * 获取手术分析任务状态
     * 
     * @return 任务状态信息
     */
    public Map<String, Object> getTaskStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "SurgeryAnalysisService");
        status.put("description", "独立的手术分析服务");
        status.put("scheduledTime", schedulingProperties.getTimer().getSurgeryAnalysisTime());
        status.put("maxConcurrency", 5);
        status.put("status", "ACTIVE");
        status.put("departmentFilterEnabled", isDepartmentFilterEnabled());
        status.put("targetDepartments", getTargetDepartments());
        return status;
    }

    /**
     * 检查是否启用科室过滤
     * 
     * @return 如果启用科室过滤则返回true，否则返回false
     */
    public boolean isDepartmentFilterEnabled() {
        return schedulingProperties.getTimer().isDepartmentFilterEnabled();
    }

    /**
     * 获取目标科室列表
     * 
     * @return 目标科室列表，如果未配置则返回空列表
     */
    public List<String> getTargetDepartments() {
        List<String> targetDepartments = schedulingProperties.getTimer().getTargetDepartments();
        return targetDepartments != null ? targetDepartments : Collections.emptyList();
    }
}
