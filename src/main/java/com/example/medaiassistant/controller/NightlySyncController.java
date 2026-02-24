package com.example.medaiassistant.controller;

import com.example.medaiassistant.service.NightlySyncService;
import com.example.medaiassistant.service.NightlySyncResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 夜间同步任务控制器
 * 
 * <p>提供夜间同步任务的REST API接口，用于监控和管理夜间定时数据同步任务。</p>
 * 
 * <h3>接口列表：</h3>
 * <ul>
 *   <li>{@code GET /api/nightly-sync/status} - 查询任务运行状态</li>
 *   <li>{@code POST /api/nightly-sync/trigger} - 手动触发同步任务</li>
 *   <li>{@code GET /api/nightly-sync/health} - 服务健康检查</li>
 *   <li>{@code GET /api/nightly-sync/last-result} - 获取最近同步结果统计</li>
 * </ul>
 * 
 * <h3>响应格式：</h3>
 * <p>所有接口返回统一的JSON格式：</p>
 * <pre>
 * {
 *   "timestamp": "2026-01-13T19:00:00",  // 响应时间戳
 *   "success": true,                       // 操作是否成功（部分接口）
 *   "message": "...",                      // 描述信息
 *   ...                                    // 其他业务字段
 * }
 * </pre>
 * 
 * <h3>异常处理：</h3>
 * <p>所有接口均包含异常捕获，发生异常时返回500状态码和错误信息。</p>
 * 
 * @author System
 * @version 1.1
 * @since 2026-01-13
 * @see NightlySyncService 夜间同步服务
 * @see NightlySyncResult 同步结果统计类
 */
@RestController
@RequestMapping("/api/nightly-sync")
@Slf4j
public class NightlySyncController {

    private final NightlySyncService nightlySyncService;

    @Autowired
    public NightlySyncController(NightlySyncService nightlySyncService) {
        this.nightlySyncService = nightlySyncService;
    }

    /**
     * 获取夜间同步任务运行状态
     * 
     * <p>查询当前夜间同步任务是否正在执行。</p>
     * 
     * <h4>响应字段：</h4>
     * <ul>
     *   <li>{@code isRunning} - boolean，任务是否正在运行</li>
     *   <li>{@code message} - String，状态描述信息</li>
     *   <li>{@code timestamp} - String，响应时间戳</li>
     * </ul>
     * 
     * <h4>响应示例：</h4>
     * <pre>
     * {
     *   "timestamp": "2026-01-13T19:00:00",
     *   "isRunning": false,
     *   "message": "夜间同步任务空闲"
     * }
     * </pre>
     * 
     * @return {@link ResponseEntity} 包含任务状态的响应体
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        try {
            Map<String, Object> response = buildBaseResponse();
            boolean isRunning = nightlySyncService.isRunning();
            response.put("isRunning", isRunning);
            response.put("message", isRunning ? "夜间同步任务正在执行中" : "夜间同步任务空闲");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查询夜间同步状态失败", e);
            return ResponseEntity.internalServerError().body(buildErrorResponse(e.getMessage()));
        }
    }

    /**
     * 手动触发夜间同步任务
     * 
     * <p>手动启动夜间同步任务。如果任务已在运行中，将返回失败。</p>
     * 
     * <h4>触发条件：</h4>
     * <ul>
     *   <li>当前无同步任务正在执行</li>
     *   <li>返回时任务已开始异步执行</li>
     * </ul>
     * 
     * <h4>响应字段：</h4>
     * <ul>
     *   <li>{@code success} - boolean，触发是否成功</li>
     *   <li>{@code message} - String，结果描述信息</li>
     *   <li>{@code timestamp} - String，响应时间戳</li>
     * </ul>
     * 
     * <h4>响应示例（成功）：</h4>
     * <pre>
     * {
     *   "timestamp": "2026-01-13T19:00:00",
     *   "success": true,
     *   "message": "夜间同步任务已触发，请查看日志了解执行进度"
     * }
     * </pre>
     * 
     * <h4>响应示例（失败-任务运行中）：</h4>
     * <pre>
     * {
     *   "timestamp": "2026-01-13T19:00:00",
     *   "success": false,
     *   "message": "夜间同步任务正在执行中，无法重复触发"
     * }
     * </pre>
     * 
     * @return {@link ResponseEntity} 包含触发结果的响应体
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerSync() {
        try {
            Map<String, Object> response = buildBaseResponse();
            boolean triggered = nightlySyncService.triggerManualSync();
            
            if (triggered) {
                log.info("手动触发夜间同步任务成功");
                response.put("success", true);
                response.put("message", "夜间同步任务已触发，请查看日志了解执行进度");
            } else {
                log.warn("夜间同步任务正在执行中，拒绝重复触发");
                response.put("success", false);
                response.put("message", "夜间同步任务正在执行中，无法重复触发");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("触发夜间同步任务失败", e);
            return ResponseEntity.internalServerError().body(buildErrorResponse(e.getMessage()));
        }
    }

    /**
     * 健康检查接口
     * 
     * <p>检查夜间同步服务的健康状态，用于监控和运维。</p>
     * 
     * <h4>响应字段：</h4>
     * <ul>
     *   <li>{@code status} - String，健康状态（"UP"表示正常）</li>
     *   <li>{@code service} - String，服务名称</li>
     *   <li>{@code timestamp} - String，响应时间戳</li>
     * </ul>
     * 
     * <h4>响应示例：</h4>
     * <pre>
     * {
     *   "timestamp": "2026-01-13T19:00:00",
     *   "status": "UP",
     *   "service": "NightlySyncService"
     * }
     * </pre>
     * 
     * @return {@link ResponseEntity} 包含健康状态的响应体
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = buildBaseResponse();
        response.put("status", "UP");
        response.put("service", "NightlySyncService");
        return ResponseEntity.ok(response);
    }

    /**
     * 获取最近一次同步结果统计
     * 
     * <p>查询上次夜间同步任务的执行结果和统计信息。</p>
     * 
     * <h4>响应字段（有结果时）：</h4>
     * <ul>
     *   <li>{@code hasResult} - boolean，是否有执行记录</li>
     *   <li>{@code success} - boolean，同步是否成功</li>
     *   <li>{@code durationMs} - long，执行耗时（毫秒）</li>
     *   <li>{@code totalPatients} - int，在院病人总数</li>
     *   <li>{@code patientSyncSuccessDepts} - int，病人列表同步成功科室数</li>
     *   <li>{@code patientSyncFailedDepts} - int，病人列表同步失败科室数</li>
     *   <li>{@code labSyncSuccess/labSyncFailed} - int，化验同步成功/失败数</li>
     *   <li>{@code examSyncSuccess/examSyncFailed} - int，检查同步成功/失败数</li>
     *   <li>{@code emrSyncSuccess/emrSyncFailed} - int，EMR同步成功/失败数</li>
     *   <li>{@code errorMessage} - String，错误信息（可选）</li>
     * </ul>
     * 
     * <h4>响应示例（有结果）：</h4>
     * <pre>
     * {
     *   "timestamp": "2026-01-13T19:00:00",
     *   "hasResult": true,
     *   "success": true,
     *   "durationMs": 12500,
     *   "totalPatients": 150,
     *   "patientSyncSuccessDepts": 10,
     *   "patientSyncFailedDepts": 0,
     *   "labSyncSuccess": 148,
     *   "labSyncFailed": 2
     * }
     * </pre>
     * 
     * <h4>响应示例（无结果）：</h4>
     * <pre>
     * {
     *   "timestamp": "2026-01-13T19:00:00",
     *   "hasResult": false,
     *   "message": "尚未执行过同步任务"
     * }
     * </pre>
     * 
     * @return {@link ResponseEntity} 包含同步结果统计的响应体
     * @see NightlySyncResult 详细统计字段定义
     */
    @GetMapping("/last-result")
    public ResponseEntity<Map<String, Object>> getLastResult() {
        try {
            Map<String, Object> response = buildBaseResponse();
            NightlySyncResult result = nightlySyncService.getLastSyncResult();
            
            if (result != null) {
                response.put("hasResult", true);
                response.put("success", result.isSuccess());
                response.put("durationMs", result.getDurationMs());
                response.put("totalPatients", result.getTotalPatients());
                response.put("patientSyncSuccessDepts", result.getPatientSyncSuccessDepts());
                response.put("patientSyncFailedDepts", result.getPatientSyncFailedDepts());
                response.put("labSyncSuccess", result.getLabSyncSuccess());
                response.put("labSyncFailed", result.getLabSyncFailed());
                response.put("examSyncSuccess", result.getExamSyncSuccess());
                response.put("examSyncFailed", result.getExamSyncFailed());
                response.put("emrSyncSuccess", result.getEmrSyncSuccess());
                response.put("emrSyncFailed", result.getEmrSyncFailed());
                response.put("orderSyncSuccess", result.getOrderSyncSuccess());
                response.put("orderSyncFailed", result.getOrderSyncFailed());
                if (result.getErrorMessage() != null) {
                    response.put("errorMessage", result.getErrorMessage());
                }
            } else {
                response.put("hasResult", false);
                response.put("message", "尚未执行过同步任务");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取最近同步结果失败", e);
            return ResponseEntity.internalServerError().body(buildErrorResponse(e.getMessage()));
        }
    }

    /**
     * 构建基础响应对象
     * 
     * <p>创建包含时间戳的基础响应Map，供所有接口使用。</p>
     * 
     * @return 包含timestamp字段的Map对象
     */
    private Map<String, Object> buildBaseResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    /**
     * 构建错误响应对象
     * 
     * <p>创建包含错误信息的响应Map，用于异常情况的响应。</p>
     * 
     * @param errorMessage 错误描述信息
     * @return 包含timestamp、success=false、error字段的Map对象
     */
    private Map<String, Object> buildErrorResponse(String errorMessage) {
        Map<String, Object> response = buildBaseResponse();
        response.put("success", false);
        response.put("error", errorMessage);
        return response;
    }
}
