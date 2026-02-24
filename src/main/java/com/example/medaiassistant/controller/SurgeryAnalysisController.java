package com.example.medaiassistant.controller;

import com.example.medaiassistant.config.SchedulingProperties;
import com.example.medaiassistant.service.SurgeryAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 独立手术分析服务控制器
 * 
 * 提供手术分析任务的API接口，支持手动执行和状态查询
 * 
 * @author Cline
 * @since 2025-09-27
 */
@RestController
@RequestMapping("/api/surgery-analysis")
@Slf4j
public class SurgeryAnalysisController {
    
    private final SurgeryAnalysisService surgeryAnalysisService;
    private final SchedulingProperties schedulingProperties;
    
    public SurgeryAnalysisController(SurgeryAnalysisService surgeryAnalysisService,
                                   SchedulingProperties schedulingProperties) {
        this.surgeryAnalysisService = surgeryAnalysisService;
        this.schedulingProperties = schedulingProperties;
    }
    
    /**
     * 手动执行手术分析任务
     * 
     * @return 包含执行结果的响应
     */
    @PostMapping("/manual")
    public ResponseEntity<Map<String, Object>> executeSurgeryAnalysisManual() {
        log.info("接收到手动执行手术分析任务请求");
        
        try {
            // 异步执行手术分析任务，返回CompletableFuture但不阻塞等待结果
            surgeryAnalysisService.executeSurgeryAnalysisManual();
            
            // 异步返回，避免阻塞
            return ResponseEntity.accepted().body(Map.of(
                "status", "ACCEPTED",
                "message", "手术分析任务已提交，正在异步执行",
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("手动执行手术分析任务失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR",
                "message", "任务提交失败: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * 获取手术分析任务状态
     * 
     * @return 任务状态信息
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getTaskStatus() {
        log.info("接收到手术分析任务状态查询请求");
        
        try {
            Map<String, Object> status = surgeryAnalysisService.getTaskStatus();
            status.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("获取手术分析任务状态失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR",
                "message", "状态查询失败: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * 启动手术分析定时任务
     * 
     * @return 启动结果
     */
    @PostMapping("/schedule/start")
    public ResponseEntity<Map<String, Object>> startScheduledTask() {
        log.info("接收到启动手术分析定时任务请求");
        
        try {
            schedulingProperties.getTimer().setSurgeryAnalysisEnabled(true);
            
            Map<String, Object> result = Map.of(
                "status", "SUCCESS",
                "message", "手术分析定时任务已启动",
                "scheduledTaskEnabled", true,
                "cronExpression", schedulingProperties.getTimer().getSurgeryAnalysisTime(),
                "timestamp", System.currentTimeMillis()
            );
            
            log.info("手术分析定时任务启动成功");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("启动手术分析定时任务失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR",
                "message", "启动定时任务失败: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * 停止手术分析定时任务
     * 
     * @return 停止结果
     */
    @PostMapping("/schedule/stop")
    public ResponseEntity<Map<String, Object>> stopScheduledTask() {
        log.info("接收到停止手术分析定时任务请求");
        
        try {
            schedulingProperties.getTimer().setSurgeryAnalysisEnabled(false);
            
            Map<String, Object> result = Map.of(
                "status", "SUCCESS",
                "message", "手术分析定时任务已停止",
                "scheduledTaskEnabled", false,
                "cronExpression", schedulingProperties.getTimer().getSurgeryAnalysisTime(),
                "timestamp", System.currentTimeMillis()
            );
            
            log.info("手术分析定时任务停止成功");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("停止手术分析定时任务失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR",
                "message", "停止定时任务失败: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * 获取手术分析定时任务状态
     * 
     * @return 定时任务状态信息
     */
    @GetMapping("/schedule/status")
    public ResponseEntity<Map<String, Object>> getScheduleStatus() {
        log.info("接收到手术分析定时任务状态查询请求");
        
        try {
            Map<String, Object> status = Map.of(
                "scheduledTaskEnabled", schedulingProperties.getTimer().isSurgeryAnalysisEnabled(),
                "cronExpression", schedulingProperties.getTimer().getSurgeryAnalysisTime(),
                "description", "每天8:00自动执行手术分析任务",
                "nextExecutionTime", "2025-09-28T08:00:00", // 这里可以计算下一次执行时间
                "lastExecutionTime", "2025-09-27T08:00:00", // 这里可以记录最后一次执行时间
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("获取手术分析定时任务状态失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR",
                "message", "获取定时任务状态失败: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * 立即执行手术分析任务（忽略定时任务开关）
     * 
     * @return 执行结果
     */
    @PostMapping("/execute-now")
    public ResponseEntity<Map<String, Object>> executeNow() {
        log.info("接收到立即执行手术分析任务请求");
        
        try {
            // 异步执行手术分析任务，返回CompletableFuture但不阻塞等待结果
            surgeryAnalysisService.executeSurgeryAnalysisNow();
            
            // 异步返回，避免阻塞
            return ResponseEntity.accepted().body(Map.of(
                "status", "ACCEPTED",
                "message", "手术分析任务已提交，正在立即执行",
                "scheduledTaskEnabled", schedulingProperties.getTimer().isSurgeryAnalysisEnabled(),
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("立即执行手术分析任务失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR",
                "message", "任务提交失败: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * 获取手术分析服务信息
     * 
     * @return 服务基本信息
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        log.info("接收到手术分析服务信息查询请求");
        
        Map<String, Object> info = Map.of(
            "service", "SurgeryAnalysisService",
            "description", "独立的手术分析服务，使用专用线程池执行",
            "version", "1.1.0",
            "features", Map.of(
                "asyncExecution", true,
                "dedicatedThreadPool", true,
                "scheduledTask", true,
                "manualExecution", true,
                "scheduleControl", true,
                "immediateExecution", true
            ),
            "threadPool", Map.of(
                "corePoolSize", 3,
                "maxPoolSize", 5,
                "queueCapacity", 50,
                "threadNamePrefix", "surgery-analysis-"
            ),
            "scheduledTime", "每天8:00执行",
            "scheduleControlEnabled", true,
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(info);
    }
}
