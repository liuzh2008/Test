package com.example.medaiassistant.controller;

import com.example.medaiassistant.hospital.dto.PatientSyncResult;
import com.example.medaiassistant.hospital.service.PatientSyncService;
import com.example.medaiassistant.hospital.service.SyncLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 病人数据同步控制器
 * 
 * 提供病人数据同步的完整REST API接口，支持以下功能：
 * 1. 单科室病人数据同步
 * 2. 批量科室病人数据同步
 * 3. 同步状态查询
 * 4. 同步统计信息查询
 * 5. 健康检查
 * 
 * 接口路径：/api/patient-sync
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-10
 */
@RestController
@RequestMapping("/api/patient-sync")
@Slf4j
public class PatientSyncController {

    private final PatientSyncService patientSyncService;
    private final SyncLogService syncLogService;

    @Autowired
    public PatientSyncController(PatientSyncService patientSyncService, SyncLogService syncLogService) {
        this.patientSyncService = patientSyncService;
        this.syncLogService = syncLogService;
    }

    /**
     * 执行病人数据同步
     * 
     * 接口：POST /api/patient-sync/sync
     * 功能：执行指定医院和科室的病人数据同步，采用三向对比算法（新增、更新、标记出院）
     * 
     * @param request 同步请求参数，包含医院ID和科室名称
     * @return 同步结果，包含同步统计信息和执行状态
     * 
     * @apiNote 请求体示例：
     * {
     *   "hospitalId": "hospital-001",
     *   "department": "心血管一病区"
     * }
     * 
     * @apiNote 响应示例（成功）：
     * {
     *   "hospitalId": "hospital-001",
     *   "department": "心血管一病区",
     *   "oraclePatientCount": 25,
     *   "mainServerPatientCount": 20,
     *   "addedCount": 5,
     *   "updatedCount": 3,
     *   "dischargedCount": 2,
     *   "executionTime": 1250,
     *   "success": true
     * }
     * 
     * @apiNote 响应示例（失败）：
     * {
     *   "hospitalId": "hospital-xxx",
     *   "department": "心血管一病区",
     *   "oraclePatientCount": 0,
     *   "mainServerPatientCount": 0,
     *   "addedCount": 0,
     *   "updatedCount": 0,
     *   "dischargedCount": 0,
     *   "executionTime": 125,
     *   "success": false,
     *   "errorMessage": "医院配置未找到: hospital-xxx"
     * }
     */
    @PostMapping("/sync")
    public ResponseEntity<PatientSyncResult> syncPatients(@RequestBody SyncRequest request) {
        log.info("收到病人数据同步请求 - 医院: {}, 科室: {}", request.getHospitalId(), request.getDepartment());
        
        try {
            PatientSyncResult result = patientSyncService.syncPatients(
                request.getHospitalId(), 
                request.getDepartment()
            );
            
            log.info("病人数据同步完成 - 医院: {}, 科室: {}, 结果: {}", 
                request.getHospitalId(), request.getDepartment(), result.isSuccess());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("病人数据同步异常 - 医院: {}, 科室: {}", 
                request.getHospitalId(), request.getDepartment(), e);
            
            PatientSyncResult errorResult = new PatientSyncResult(
                request.getHospitalId(), 
                request.getDepartment()
            );
            errorResult.setSuccess(false);
            errorResult.setErrorMessage("同步过程发生异常: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 批量执行病人数据同步
     * 
     * 接口：POST /api/patient-sync/sync/batch
     * 功能：批量执行多个科室的病人数据同步，支持并发处理
     * 
     * @param batchRequest 批量同步请求，包含多个同步任务
     * @return 批量同步结果，包含每个任务的执行结果和总体统计
     * 
     * @apiNote 请求体示例：
     * {
     *   "tasks": [
     *     {
     *       "hospitalId": "hospital-001",
     *       "department": "心血管一病区"
     *     },
     *     {
     *       "hospitalId": "hospital-001",
     *       "department": "呼吸内科"
     *     }
     *   ]
     * }
     * 
     * @apiNote 响应示例：
     * {
     *   "startTime": "2025-12-10T11:36:14.123",
     *   "endTime": "2025-12-10T11:36:15.456",
     *   "totalTasks": 2,
     *   "successCount": 2,
     *   "failureCount": 0,
     *   "success": true,
     *   "results": [
     *     {
     *       "hospitalId": "hospital-001",
     *       "department": "心血管一病区",
     *       "oraclePatientCount": 25,
     *       "mainServerPatientCount": 20,
     *       "addedCount": 5,
     *       "updatedCount": 3,
     *       "dischargedCount": 2,
     *       "executionTime": 1250,
     *       "success": true
     *     },
     *     {
     *       "hospitalId": "hospital-001",
     *       "department": "呼吸内科",
     *       "oraclePatientCount": 18,
     *       "mainServerPatientCount": 15,
     *       "addedCount": 3,
     *       "updatedCount": 2,
     *       "dischargedCount": 1,
     *       "executionTime": 980,
     *       "success": true
     *     }
     *   ]
     * }
     */
    @PostMapping("/sync/batch")
    public ResponseEntity<BatchSyncResult> syncPatientsBatch(@RequestBody BatchSyncRequest batchRequest) {
        log.info("收到批量病人数据同步请求 - 任务数: {}", batchRequest.getTasks().size());
        
        BatchSyncResult batchResult = new BatchSyncResult();
        batchResult.setStartTime(LocalDateTime.now());
        
        int successCount = 0;
        int failureCount = 0;
        
        for (SyncRequest task : batchRequest.getTasks()) {
            try {
                PatientSyncResult result = patientSyncService.syncPatients(
                    task.getHospitalId(), 
                    task.getDepartment()
                );
                
                batchResult.getResults().add(result);
                
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    failureCount++;
                }
                
            } catch (Exception e) {
                log.error("批量同步任务异常 - 医院: {}, 科室: {}", 
                    task.getHospitalId(), task.getDepartment(), e);
                
                PatientSyncResult errorResult = new PatientSyncResult(
                    task.getHospitalId(), 
                    task.getDepartment()
                );
                errorResult.setSuccess(false);
                errorResult.setErrorMessage("任务执行异常: " + e.getMessage());
                batchResult.getResults().add(errorResult);
                failureCount++;
            }
        }
        
        batchResult.setEndTime(LocalDateTime.now());
        batchResult.setTotalTasks(batchRequest.getTasks().size());
        batchResult.setSuccessCount(successCount);
        batchResult.setFailureCount(failureCount);
        batchResult.setSuccess(failureCount == 0);
        
        log.info("批量病人数据同步完成 - 总数: {}, 成功: {}, 失败: {}", 
            batchRequest.getTasks().size(), successCount, failureCount);
        
        return ResponseEntity.ok(batchResult);
    }

    /**
     * 获取同步状态
     * 
     * 接口：GET /api/patient-sync/status
     * 功能：查询病人数据同步的实时状态，包括最近的同步记录和统计信息
     * 
     * @param hospitalId 医院ID（可选），用于过滤特定医院的同步状态
     * @param department 科室名称（可选），用于过滤特定科室的同步状态
     * @return 同步状态信息，包含同步统计、成功率和最近的同步记录
     * 
     * @apiNote 请求示例：
     * GET /api/patient-sync/status?hospitalId=hospital-001&department=心血管一病区
     * 
     * @apiNote 响应示例：
     * {
     *   "queryTime": "2025-12-10T11:36:14.123",
     *   "totalSyncCount": 42,
     *   "successRate": 95.24,
     *   "averageExecutionTime": 1125.5,
     *   "recentSyncs": [
     *     {
     *       "hospitalId": "hospital-001",
     *       "department": "心血管一病区",
     *       "syncTime": "2025-12-10T11:30:00.123",
     *       "success": true,
     *       "oraclePatientCount": 25,
     *       "mainServerPatientCount": 20,
     *       "addedCount": 5,
     *       "updatedCount": 3,
     *       "dischargedCount": 2,
     *       "executionTime": 1250
     *     }
     *   ]
     * }
     */
    @GetMapping("/status")
    public ResponseEntity<SyncStatus> getSyncStatus(
            @RequestParam(required = false) String hospitalId,
            @RequestParam(required = false) String department) {
        
        log.debug("查询同步状态 - 医院: {}, 科室: {}", hospitalId, department);
        
        SyncStatus status = new SyncStatus();
        status.setQueryTime(LocalDateTime.now());
        
        // 获取最近的同步记录
        List<Map<String, Object>> recentSyncs = syncLogService.getRecentPatientSyncLogs(
            hospitalId, department, 10
        );
        
        status.setRecentSyncs(recentSyncs);
        status.setTotalSyncCount(syncLogService.getPatientSyncCount(hospitalId, department));
        
        // 计算成功率
        long successCount = syncLogService.getPatientSyncSuccessCount(hospitalId, department);
        long totalCount = status.getTotalSyncCount();
        
        if (totalCount > 0) {
            status.setSuccessRate((double) successCount / totalCount * 100);
        } else {
            status.setSuccessRate(0.0);
        }
        
        // 获取平均执行时间
        status.setAverageExecutionTime(syncLogService.getAveragePatientSyncTime(hospitalId, department));
        
        return ResponseEntity.ok(status);
    }

    /**
     * 获取同步统计信息
     * 
     * 接口：GET /api/patient-sync/statistics
     * 功能：获取病人数据同步的历史统计信息，支持按医院和时间范围过滤
     * 
     * @param hospitalId 医院ID（可选），用于过滤特定医院的统计信息
     * @param startDate 开始日期（可选，格式：yyyy-MM-dd），默认最近7天
     * @param endDate 结束日期（可选，格式：yyyy-MM-dd），默认当前日期
     * @return 同步统计信息，包含总体统计、科室分布和时间趋势
     * 
     * @apiNote 请求示例：
     * GET /api/patient-sync/statistics?hospitalId=hospital-001&startDate=2025-12-01&endDate=2025-12-10
     * 
     * @apiNote 响应示例：
     * {
     *   "queryTime": "2025-12-10T11:36:14.123",
     *   "hospitalId": "hospital-001",
     *   "dateRange": "2025-12-01 至 2025-12-10",
     *   "totalSyncCount": 42,
     *   "successCount": 40,
     *   "failureCount": 2,
     *   "successRate": 95.24,
     *   "departmentDistribution": [
     *     {
     *       "department": "心血管一病区",
     *       "syncCount": 14,
     *       "successCount": 14,
     *       "failureCount": 0,
     *       "successRate": 100.0
     *     }
     *   ],
     *   "timeTrend": [
     *     {
     *       "date": "2025-12-01",
     *       "syncCount": 6,
     *       "successCount": 6,
     *       "failureCount": 0
     *     }
     *   ]
     * }
     */
    @GetMapping("/statistics")
    public ResponseEntity<SyncStatistics> getStatistics(
            @RequestParam(required = false) String hospitalId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        log.debug("查询同步统计 - 医院: {}, 日期范围: {} 到 {}", hospitalId, startDate, endDate);
        
        SyncStatistics statistics = new SyncStatistics();
        statistics.setQueryTime(LocalDateTime.now());
        statistics.setHospitalId(hospitalId);
        statistics.setDateRange(startDate + " 至 " + endDate);
        
        // 获取同步统计
        Map<String, Object> stats = syncLogService.getPatientSyncStatistics(
            hospitalId, startDate, endDate
        );
        
        statistics.setTotalSyncCount((Long) stats.getOrDefault("totalCount", 0L));
        statistics.setSuccessCount((Long) stats.getOrDefault("successCount", 0L));
        statistics.setFailureCount((Long) stats.getOrDefault("failureCount", 0L));
        
        // 计算成功率
        if (statistics.getTotalSyncCount() > 0) {
            statistics.setSuccessRate(
                (double) statistics.getSuccessCount() / statistics.getTotalSyncCount() * 100
            );
        } else {
            statistics.setSuccessRate(0.0);
        }
        
        // 获取科室分布
        statistics.setDepartmentDistribution(
            syncLogService.getPatientSyncDepartmentDistribution(hospitalId, startDate, endDate)
        );
        
        // 获取时间趋势
        statistics.setTimeTrend(
            syncLogService.getPatientSyncTimeTrend(hospitalId, startDate, endDate)
        );
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * 健康检查端点
     * 
     * 接口：GET /api/patient-sync/health
     * 功能：检查病人数据同步服务的健康状态
     * 
     * @return 健康状态信息，包含服务状态、时间戳和支持的功能
     * 
     * @apiNote 响应示例：
     * {
     *   "status": "UP",
     *   "service": "patient-sync",
     *   "timestamp": "2025-12-10T11:36:14.123",
     *   "supportedHospitals": ["cdwyy", "hospital-001"],
     *   "features": ["single-sync", "batch-sync", "status-query", "statistics"]
     * }
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.debug("病人数据同步服务健康检查");
        
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "patient-sync",
            "timestamp", LocalDateTime.now().toString(),
            "supportedHospitals", List.of("cdwyy", "hospital-001"),
            "features", List.of("single-sync", "batch-sync", "status-query", "statistics")
        ));
    }

    // ========== 请求和响应DTO类 ==========

    /**
     * 同步请求DTO
     * 
     * 用于接收病人数据同步请求的参数
     * 
     * @property hospitalId 医院ID，如 "hospital-001"、"cdwyy"
     * @property department 科室名称，如 "心血管一病区"、"呼吸内科"
     */
    public static class SyncRequest {
        private String hospitalId;
        private String department;

        // Getters and Setters
        public String getHospitalId() {
            return hospitalId;
        }

        public void setHospitalId(String hospitalId) {
            this.hospitalId = hospitalId;
        }

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }
    }

    /**
     * 批量同步请求DTO
     * 
     * 用于接收批量病人数据同步请求的参数
     * 
     * @property tasks 同步任务列表，每个任务包含医院ID和科室名称
     */
    public static class BatchSyncRequest {
        private List<SyncRequest> tasks;

        // Getters and Setters
        public List<SyncRequest> getTasks() {
            return tasks;
        }

        public void setTasks(List<SyncRequest> tasks) {
            this.tasks = tasks;
        }
    }

    /**
     * 批量同步结果DTO
     * 
     * 用于返回批量病人数据同步的结果
     * 
     * @property startTime 批量同步开始时间
     * @property endTime 批量同步结束时间
     * @property totalTasks 总任务数
     * @property successCount 成功任务数
     * @property failureCount 失败任务数
     * @property success 批量同步是否全部成功
     * @property results 每个任务的详细结果列表
     */
    public static class BatchSyncResult {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int totalTasks;
        private int successCount;
        private int failureCount;
        private boolean success;
        private List<PatientSyncResult> results = new java.util.ArrayList<>();

        // Getters and Setters
        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }

        public int getTotalTasks() {
            return totalTasks;
        }

        public void setTotalTasks(int totalTasks) {
            this.totalTasks = totalTasks;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(int successCount) {
            this.successCount = successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public void setFailureCount(int failureCount) {
            this.failureCount = failureCount;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public List<PatientSyncResult> getResults() {
            return results;
        }

        public void setResults(List<PatientSyncResult> results) {
            this.results = results;
        }
    }

    /**
     * 同步状态DTO
     * 
     * 用于返回病人数据同步的实时状态信息
     * 
     * @property queryTime 查询时间
     * @property totalSyncCount 总同步次数
     * @property successRate 同步成功率（百分比）
     * @property averageExecutionTime 平均执行时间（毫秒）
     * @property recentSyncs 最近的同步记录列表
     */
    public static class SyncStatus {
        private LocalDateTime queryTime;
        private long totalSyncCount;
        private double successRate;
        private double averageExecutionTime;
        private List<Map<String, Object>> recentSyncs = new java.util.ArrayList<>();

        // Getters and Setters
        public LocalDateTime getQueryTime() {
            return queryTime;
        }

        public void setQueryTime(LocalDateTime queryTime) {
            this.queryTime = queryTime;
        }

        public long getTotalSyncCount() {
            return totalSyncCount;
        }

        public void setTotalSyncCount(long totalSyncCount) {
            this.totalSyncCount = totalSyncCount;
        }

        public double getSuccessRate() {
            return successRate;
        }

        public void setSuccessRate(double successRate) {
            this.successRate = successRate;
        }

        public double getAverageExecutionTime() {
            return averageExecutionTime;
        }

        public void setAverageExecutionTime(double averageExecutionTime) {
            this.averageExecutionTime = averageExecutionTime;
        }

        public List<Map<String, Object>> getRecentSyncs() {
            return recentSyncs;
        }

        public void setRecentSyncs(List<Map<String, Object>> recentSyncs) {
            this.recentSyncs = recentSyncs;
        }
    }

    /**
     * 同步统计DTO
     * 
     * 用于返回病人数据同步的历史统计信息
     * 
     * @property queryTime 查询时间
     * @property hospitalId 医院ID
     * @property dateRange 统计日期范围
     * @property totalSyncCount 总同步次数
     * @property successCount 成功次数
     * @property failureCount 失败次数
     * @property successRate 成功率（百分比）
     * @property departmentDistribution 科室分布统计
     * @property timeTrend 时间趋势统计
     */
    public static class SyncStatistics {
        private LocalDateTime queryTime;
        private String hospitalId;
        private String dateRange;
        private long totalSyncCount;
        private long successCount;
        private long failureCount;
        private double successRate;
        private List<Map<String, Object>> departmentDistribution = new java.util.ArrayList<>();
        private List<Map<String, Object>> timeTrend = new java.util.ArrayList<>();

        // Getters and Setters
        public LocalDateTime getQueryTime() {
            return queryTime;
        }

        public void setQueryTime(LocalDateTime queryTime) {
            this.queryTime = queryTime;
        }

        public String getHospitalId() {
            return hospitalId;
        }

        public void setHospitalId(String hospitalId) {
            this.hospitalId = hospitalId;
        }

        public String getDateRange() {
            return dateRange;
        }

        public void setDateRange(String dateRange) {
            this.dateRange = dateRange;
        }

        public long getTotalSyncCount() {
            return totalSyncCount;
        }

        public void setTotalSyncCount(long totalSyncCount) {
            this.totalSyncCount = totalSyncCount;
        }

        public long getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(long successCount) {
            this.successCount = successCount;
        }

        public long getFailureCount() {
            return failureCount;
        }

        public void setFailureCount(long failureCount) {
            this.failureCount = failureCount;
        }

        public double getSuccessRate() {
            return successRate;
        }

        public void setSuccessRate(double successRate) {
            this.successRate = successRate;
        }

        public List<Map<String, Object>> getDepartmentDistribution() {
            return departmentDistribution;
        }

        public void setDepartmentDistribution(List<Map<String, Object>> departmentDistribution) {
            this.departmentDistribution = departmentDistribution;
        }

        public List<Map<String, Object>> getTimeTrend() {
            return timeTrend;
        }

        public void setTimeTrend(List<Map<String, Object>> timeTrend) {
            this.timeTrend = timeTrend;
        }
    }
}
