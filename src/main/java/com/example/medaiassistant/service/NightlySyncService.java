package com.example.medaiassistant.service;

import com.example.medaiassistant.config.SchedulingProperties;
import com.example.medaiassistant.hospital.model.HospitalConfig;
import com.example.medaiassistant.hospital.service.*;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.PatientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 夜间数据同步服务
 * 每天凌晨1点执行病人列表、化验、检查、EMR病历、医嘱的同步任务
 * 
 * <p><strong>执行顺序</strong>：</p>
 * <ol>
 *   <li>病人列表同步 - 从HIS系统同步在院病人列表</li>
 *   <li>化验同步 - 为每个在院病人同步LIS检验结果</li>
 *   <li>检查同步 - 为每个在院病人同步检查结果</li>
 *   <li>EMR病历同步 - 为每个在院病人同步EMR病历内容</li>
 *   <li>医嘱同步 - 为每个在院病人同步长期医嘱数据</li>
 * </ol>
 * 
 * @author System
 * @version 1.1
 * @since 2026-01-13
 */
@Service
@EnableScheduling
@Slf4j
public class NightlySyncService {

    private final PatientSyncService patientSyncService;
    private final LabSyncService labSyncService;
    private final ExaminationSyncService examinationSyncService;
    private final EmrSyncService emrSyncService;
    private final OrderSyncService orderSyncService;
    private final HospitalConfigService hospitalConfigService;
    private final PatientRepository patientRepository;
    private final SchedulingProperties schedulingProperties;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    /**
     * 最近一次同步任务的执行结果
     * <p>用于查询和监控同步任务的统计信息</p>
     */
    private volatile NightlySyncResult lastSyncResult;

    @Value("${nightly.sync.enabled:true}")
    private boolean nightlySyncEnabled;

    @Value("${nightly.sync.on-startup:false}")
    private boolean syncOnStartup;

    @Value("${hospital.default.id:hospital-Local}")
    private String defaultHospitalId;

    /**
     * 构造函数，通过依赖注入初始化所有同步服务
     *
     * @param patientSyncService 病人列表同步服务，负责从HIS同步在院病人
     * @param labSyncService 化验同步服务，负责同步LIS检验结果
     * @param examinationSyncService 检查同步服务，负责同步检查结果
     * @param emrSyncService EMR同步服务，负责同步电子病历内容
     * @param orderSyncService 医嘱同步服务，负责同步长期医嘱数据
     * @param hospitalConfigService 医院配置服务，提供医院ID等配置
     * @param patientRepository 病人数据访问层，查询在院病人列表
     * @param schedulingProperties 调度配置属性，提供科室过滤等配置
     */
    public NightlySyncService(
            PatientSyncService patientSyncService,
            LabSyncService labSyncService,
            ExaminationSyncService examinationSyncService,
            EmrSyncService emrSyncService,
            OrderSyncService orderSyncService,
            HospitalConfigService hospitalConfigService,
            PatientRepository patientRepository,
            SchedulingProperties schedulingProperties) {
        this.patientSyncService = patientSyncService;
        this.labSyncService = labSyncService;
        this.examinationSyncService = examinationSyncService;
        this.emrSyncService = emrSyncService;
        this.orderSyncService = orderSyncService;
        this.hospitalConfigService = hospitalConfigService;
        this.patientRepository = patientRepository;
        this.schedulingProperties = schedulingProperties;
    }

    /**
     * 系统启动完成后自动触发夜间同步
     * 
     * <p>当配置项 {@code nightly.sync.on-startup=true} 时，
     * 系统启动完成后会自动触发一次夜间同步任务。</p>
     * 
     * <p>使用 {@link ApplicationReadyEvent} 确保所有Bean初始化完成后再执行，
     * 避免依赖服务未就绪导致的问题。</p>
     * 
     * @see #triggerManualSync() 手动触发方法
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (syncOnStartup && nightlySyncEnabled) {
            log.info("========== 系统启动完成，自动触发夜间同步任务 ==========");
            // 异步执行，不阻塞启动流程
            new Thread(() -> {
                try {
                    // 延迟5秒，确保所有服务完全就绪
                    Thread.sleep(5000);
                    triggerManualSync();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("启动同步任务被中断");
                }
            }, "nightly-sync-startup").start();
        } else if (syncOnStartup) {
            log.info("夜间同步已禁用(nightly.sync.enabled=false)，跳过启动时自动同步");
        }
    }

    /**
     * 夜间定时同步任务入口方法
     * 
     * <p>每天凌晨1点自动执行，按顺序执行以下四步同步：</p>
     * <ol>
     *   <li>病人列表同步 - 遍历配置的科室，同步在院病人</li>
     *   <li>化验同步 - 为<strong>指定科室</strong>的在院病人同步LIS检验结果</li>
     *   <li>检查同步 - 为<strong>指定科室</strong>的在院病人同步检查报告</li>
     *   <li>EMR同步 - 为<strong>指定科室</strong>的在院病人同步电子病历</li>
     * </ol>
     * 
     * <p><strong>同步范围</strong>：只同步配置文件中 {@code scheduling.timer.target-departments} 指定科室的病人，
     * 不同步其他科室的病人数据</p>
     * <p><strong>并发控制</strong>：使用AtomicBoolean确保同一时间只有一个任务执行</p>
     * <p><strong>异常处理</strong>：单个病人/科室失败不影响其他同步继续执行</p>
     * 
     * @see #triggerManualSync() 手动触发入口
     * @see #getTargetDepartments() 获取目标科室列表
     */
    @Scheduled(cron = "${nightly.sync.cron:0 0 1 * * ?}")
    public void executeNightlySync() {
        if (!nightlySyncEnabled) {
            log.info("夜间同步任务已禁用，跳过执行");
            return;
        }

        if (!isRunning.compareAndSet(false, true)) {
            log.warn("夜间同步任务正在执行中，跳过本次执行");
            return;
        }

        long startTime = System.currentTimeMillis();
        String startTimeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        log.info("========== 夜间同步任务开始执行 ==========");
        log.info("开始时间: {}", startTimeStr);

        NightlySyncResult result = new NightlySyncResult();

        try {
            // 第一步：病人列表同步
            log.info("【第1步】开始病人列表同步...");
            syncPatientList(result);
            log.info("【第1步】病人列表同步完成 - 成功科室: {}, 失败科室: {}", 
                result.getPatientSyncSuccessDepts(), result.getPatientSyncFailedDepts());

            // 获取指定科室的在院病人列表（只同步指定科室的病人）
            List<String> targetDepartments = getTargetDepartments();
            List<Patient> inHospitalPatients = new ArrayList<>();
            if (!targetDepartments.isEmpty()) {
                for (String deptName : targetDepartments) {
                    List<Patient> deptPatients = patientRepository.findByDepartmentAndIsInHospital(deptName, true);
                    inHospitalPatients.addAll(deptPatients);
                }
                log.info("获取到指定科室 {} 共 {} 个在院病人，准备进行化验、检查、EMR同步", 
                    targetDepartments, inHospitalPatients.size());
            } else {
                log.warn("未配置目标科室，跳过化验、检查、EMR同步");
            }
            result.setTotalPatients(inHospitalPatients.size());

            // 第二步：化验同步
            log.info("【第2步】开始化验同步...");
            syncLabResults(inHospitalPatients, result);
            log.info("【第2步】化验同步完成 - 成功: {}, 失败: {}", 
                result.getLabSyncSuccess(), result.getLabSyncFailed());

            // 第三步：检查同步
            log.info("【第3步】开始检查同步...");
            syncExaminationResults(inHospitalPatients, result);
            log.info("【第3步】检查同步完成 - 成功: {}, 失败: {}", 
                result.getExamSyncSuccess(), result.getExamSyncFailed());

            // 第四步：EMR病历同步
            log.info("【第4步】开始EMR病历同步...");
            syncEmrContent(inHospitalPatients, result);
            log.info("【第4步】EMR病历同步完成 - 成功: {}, 失败: {}", 
                result.getEmrSyncSuccess(), result.getEmrSyncFailed());

            // 第五步：医嘱同步
            log.info("【第5步】开始医嘱同步...");
            syncOrders(inHospitalPatients, result);
            log.info("【第5步】医嘱同步完成 - 成功: {}, 失败: {}", 
                result.getOrderSyncSuccess(), result.getOrderSyncFailed());

            result.setSuccess(true);

        } catch (Exception e) {
            log.error("夜间同步任务执行异常", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        } finally {
            isRunning.set(false);
            long duration = System.currentTimeMillis() - startTime;
            result.setDurationMs(duration);
            // 保存本次同步结果，供外部查询
            this.lastSyncResult = result;

            log.info("========== 夜间同步任务执行完成 ==========");
            log.info("总耗时: {} ms ({} 分钟)", duration, duration / 60000);
            log.info("同步结果汇总:");
            log.info("  - 病人列表: 成功科室 {}, 失败科室 {}", 
                result.getPatientSyncSuccessDepts(), result.getPatientSyncFailedDepts());
            log.info("  - 总病人数: {}", result.getTotalPatients());
            log.info("  - 化验同步: 成功 {}, 失败 {}", result.getLabSyncSuccess(), result.getLabSyncFailed());
            log.info("  - 检查同步: 成功 {}, 失败 {}", result.getExamSyncSuccess(), result.getExamSyncFailed());
            log.info("  - EMR同步: 成功 {}, 失败 {}", result.getEmrSyncSuccess(), result.getEmrSyncFailed());
            log.info("  - 医嘱同步: 成功 {}, 失败 {}", result.getOrderSyncSuccess(), result.getOrderSyncFailed());
            log.info("============================================");
        }
    }

    /**
     * 同步病人列表（第一步）
     * 
     * <p>遍历配置的目标科室，逐个调用PatientSyncService同步在院病人。</p>
     * <p>单个科室同步失败不影响其他科室继续执行。</p>
     *
     * @param result 同步结果对象，用于记录成功/失败科室数量
     */
    private void syncPatientList(NightlySyncResult result) {
        String hospitalId = getActiveHospitalId();
        List<String> targetDepartments = getTargetDepartments();

        if (targetDepartments.isEmpty()) {
            log.warn("未配置目标科室，跳过病人列表同步");
            return;
        }

        log.info("准备同步 {} 个科室的病人列表", targetDepartments.size());

        int successCount = 0;
        int failedCount = 0;

        for (String deptName : targetDepartments) {
            try {
                log.info("正在同步科室 [{}] 的病人列表...", deptName);
                com.example.medaiassistant.hospital.dto.PatientSyncResult syncResult = 
                    patientSyncService.syncPatients(hospitalId, deptName);
                
                if (syncResult.isSuccess()) {
                    successCount++;
                    log.info("科室 [{}] 病人列表同步成功 - 新增: {}, 更新: {}, 出院: {}", 
                        deptName, syncResult.getAddedCount(), 
                        syncResult.getUpdatedCount(), syncResult.getDischargedCount());
                } else {
                    failedCount++;
                    log.error("科室 [{}] 病人列表同步失败: {}", deptName, syncResult.getErrorMessage());
                }
            } catch (Exception e) {
                failedCount++;
                log.error("科室 [{}] 病人列表同步异常: {}", deptName, e.getMessage(), e);
            }
        }

        result.setPatientSyncSuccessDepts(successCount);
        result.setPatientSyncFailedDepts(failedCount);
    }

    /**
     * 同步化验结果（第二步）
     * 
     * <p>遍历指定科室的在院病人，逐个调用LabSyncService导入LIS检验结果。</p>
     * <p>单个病人同步失败不影响其他病人继续执行。</p>
     *
     * @param patients 指定科室的在院病人列表
     * @param result 同步结果对象，用于记录成功/失败病人数量
     */
    private void syncLabResults(List<Patient> patients, NightlySyncResult result) {
        SyncStatistics stats = syncPatientData(patients, 
            patientId -> labSyncService.importLabResults(patientId),
            "化验");
        result.setLabSyncSuccess(stats.successCount);
        result.setLabSyncFailed(stats.failedCount);
    }

    /**
     * 同步检查结果（第三步）
     * 
     * <p>遍历指定科室的在院病人，逐个调用ExaminationSyncService导入检查报告。</p>
     * <p>单个病人同步失败不影响其他病人继续执行。</p>
     *
     * @param patients 指定科室的在院病人列表
     * @param result 同步结果对象，用于记录成功/失败病人数量
     */
    private void syncExaminationResults(List<Patient> patients, NightlySyncResult result) {
        SyncStatistics stats = syncPatientData(patients, 
            patientId -> examinationSyncService.importExaminationResults(patientId),
            "检查");
        result.setExamSyncSuccess(stats.successCount);
        result.setExamSyncFailed(stats.failedCount);
    }

    /**
     * 同步EMR病历内容（第四步）
     * 
     * <p>遍历指定科室的在院病人，逐个调用EmrSyncService导入电子病历内容。</p>
     * <p>单个病人同步失败不影响其他病人继续执行。</p>
     *
     * @param patients 指定科室的在院病人列表
     * @param result 同步结果对象，用于记录成功/失败病人数量
     */
    private void syncEmrContent(List<Patient> patients, NightlySyncResult result) {
        SyncStatistics stats = syncPatientData(patients, 
            patientId -> emrSyncService.importEmrContent(patientId),
            "EMR");
        result.setEmrSyncSuccess(stats.successCount);
        result.setEmrSyncFailed(stats.failedCount);
    }

    /**
     * 同步医嘱数据（第五步）
     * 
     * <p>遍历指定科室的在院病人，逐个调用OrderSyncService导入长期医嘱数据。</p>
     * <p>单个病人同步失败不影响其他病人继续执行。</p>
     *
     * @param patients 指定科室的在院病人列表
     * @param result 同步结果对象，用于记录成功/失败病人数量
     */
    private void syncOrders(List<Patient> patients, NightlySyncResult result) {
        SyncStatistics stats = syncPatientData(patients, 
            patientId -> orderSyncService.importOrders(patientId),
            "医嘱");
        result.setOrderSyncSuccess(stats.successCount);
        result.setOrderSyncFailed(stats.failedCount);
    }
    
    /**
     * 通用病人数据同步模板方法
     * 
     * <p>提取了三种同步方法的公共逻辑：</p>
     * <ol>
     *   <li>遍历病人列表</li>
     *   <li>调用同步操作（策略模式）</li>
     *   <li>处理返回值和异常</li>
     *   <li>统计成功/失败数量</li>
     * </ol>
     * 
     * @param patients 在院病人列表
     * @param syncOperation 同步操作策略（函数式接口）
     * @param syncType 同步类型名称（用于日志）
     * @return 同步统计结果
     */
    private SyncStatistics syncPatientData(List<Patient> patients, 
                                            PatientSyncOperation syncOperation, 
                                            String syncType) {
        int successCount = 0;
        int failedCount = 0;

        for (Patient patient : patients) {
            try {
                String patientId = patient.getPatientId();
                int importedCount = syncOperation.execute(patientId);
                
                if (importedCount >= 0) {
                    successCount++;
                    if (importedCount > 0) {
                        log.debug("病人 [{}] {}同步成功，导入 {} 条记录", patientId, syncType, importedCount);
                    }
                } else {
                    failedCount++;
                    log.warn("病人 [{}] {}同步失败", patientId, syncType);
                }
            } catch (Exception e) {
                failedCount++;
                log.error("病人 [{}] {}同步异常: {}", patient.getPatientId(), syncType, e.getMessage());
            }
        }

        return new SyncStatistics(successCount, failedCount);
    }
    
    /**
     * 病人同步操作策略接口
     * 
     * <p>函数式接口，用于定义不同类型的同步操作</p>
     */
    @FunctionalInterface
    private interface PatientSyncOperation {
        /**
         * 执行同步操作
         * @param patientId 病人ID
         * @return 导入的记录数，>=0表示成功，<0表示失败
         */
        int execute(String patientId);
    }
    
    /**
     * 同步统计结果类
     * 
     * <p>用于封装同步操作的统计结果</p>
     */
    private static class SyncStatistics {
        final int successCount;
        final int failedCount;
        
        SyncStatistics(int successCount, int failedCount) {
            this.successCount = successCount;
            this.failedCount = failedCount;
        }
    }

    /**
     * 获取当前活动的医院ID
     * 
     * <p>优先从配置服务获取第一个医院配置的ID，如果没有配置则返回默认值。</p>
     *
     * @return 医院ID字符串
     */
    private String getActiveHospitalId() {
        List<HospitalConfig> configs = hospitalConfigService.getAllConfigs();
        if (configs != null && !configs.isEmpty()) {
            return configs.get(0).getId();
        }
        return defaultHospitalId;
    }

    /**
     * 获取目标科室列表
     * 
     * <p>从SchedulingProperties配置中获取启用科室过滤后的目标科室列表。</p>
     * <p>如果未启用科室过滤，返回空列表并记录警告。</p>
     *
     * @return 目标科室名称列表，未配置时返回空列表
     */
    private List<String> getTargetDepartments() {
        SchedulingProperties.TimerConfig timerConfig = schedulingProperties.getTimer();
        
        if (timerConfig.isDepartmentFilterEnabled()) {
            return timerConfig.getTargetDepartments();
        }
        
        log.warn("未启用科室过滤，请在配置中设置 scheduling.timer.department-filter-enabled=true 并配置 target-departments");
        return new ArrayList<>();
    }

    /**
     * 手动触发夜间同步任务
     * 
     * <p>提供一个程序化的入口，允许在需要时手动触发同步任务。</p>
     * <p>如果任务已在执行中，则拒绝本次触发并返回false。</p>
     *
     * @return {@code true} 如果成功触发任务，{@code false} 如果任务正在执行中
     * @see #executeNightlySync() 实际执行的同步方法
     * @see #isRunning() 查询任务运行状态
     */
    public boolean triggerManualSync() {
        if (isRunning.get()) {
            log.warn("夜间同步任务正在执行中，无法手动触发");
            return false;
        }

        log.info("手动触发夜间同步任务");
        executeNightlySync();
        return true;
    }

    /**
     * 获取任务运行状态
     * 
     * <p>查询当前夜间同步任务是否正在执行。</p>
     * <p>可用于在触发手动同步前检查任务状态。</p>
     *
     * @return {@code true} 如果任务正在执行，{@code false} 如果任务空闲
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * 获取最近一次同步任务的执行结果
     * 
     * <p>返回上次执行的同步结果统计信息，包括：</p>
     * <ul>
     *   <li>病人列表同步统计（成功/失败科室数）</li>
     *   <li>化验/检查/EMR同步统计（成功/失败病人数）</li>
     *   <li>执行总时长、整体状态等</li>
     * </ul>
     * 
     * <p>如果还未执行过同步任务，返回null。</p>
     *
     * @return 最近一次同步结果，未执行过时返回null
     * @see NightlySyncResult
     */
    public NightlySyncResult getLastSyncResult() {
        return lastSyncResult;
    }
}
