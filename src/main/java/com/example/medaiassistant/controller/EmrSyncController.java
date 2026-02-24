package com.example.medaiassistant.controller;

import com.example.medaiassistant.hospital.service.EmrSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EMR病历内容同步控制器
 * 提供EMR病历内容同步的REST API接口
 * 
 * <p><strong>功能包括</strong>：</p>
 * <ol>
 *   <li>导入单个患者的EMR病历内容</li>
 *   <li>批量导入患者的EMR病历内容</li>
 *   <li>获取服务健康状态</li>
 *   <li>获取同步统计信息</li>
 * </ol>
 * 
 * <p><strong>接口路径</strong>：/api/emr-sync</p>
 * 
 * @author TDD
 * @version 1.0
 * @since 2026-01-11
 * @see com.example.medaiassistant.hospital.service.EmrSyncService
 */
@RestController
@RequestMapping("/api/emr-sync")
@Slf4j
public class EmrSyncController {

    private final EmrSyncService emrSyncService;

    @Autowired
    public EmrSyncController(EmrSyncService emrSyncService) {
        this.emrSyncService = emrSyncService;
    }

    /**
     * 健康检查端点
     * 
     * <p>接口：GET /api/emr-sync/health</p>
     * <p>功能：检查EMR病历内容同步服务的健康状态</p>
     * 
     * @return 健康状态信息
     * 
     * @apiNote 响应示例：
     * <pre>
     * {
     *   "status": "UP",
     *   "service": "EmrSyncService",
     *   "timestamp": "2026-01-11T10:00:00",
     *   "features": ["import-emr-content", "batch-import"]
     * }
     * </pre>
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.debug("执行命令: GET /api/emr-sync/health - EMR病历内容同步服务健康检查");
        
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "EmrSyncService",
            "timestamp", LocalDateTime.now().toString(),
            "features", List.of("import-emr-content", "batch-import")
        ));
    }

    /**
     * 导入单个患者的EMR病历内容
     * 
     * <p>接口：POST /api/emr-sync/import</p>
     * <p>功能：根据主服务器病人ID从 Oracle HIS 系统导入EMR病历内容</p>
     * 
     * @param request 导入请求参数，包含 patientId
     * @return 导入结果
     * 
     * @apiNote 请求示例：
     * <pre>
     * {
     *   "patientId": "990500000178405-1"
     * }
     * </pre>
     * 
     * @apiNote 响应示例（成功）：
     * <pre>
     * {
     *   "success": true,
     *   "message": "EMR病历内容导入成功",
     *   "importedCount": 5,
     *   "patientId": "990500000178405-1",
     *   "timestamp": "2026-01-11T10:00:00"
     * }
     * </pre>
     * 
     * @apiNote 响应示例（失败）：
     * <pre>
     * {
     *   "success": false,
     *   "message": "EMR病历内容导入失败",
     *   "importedCount": -1,
     *   "patientId": "990500000178405-1",
     *   "timestamp": "2026-01-11T10:00:00"
     * }
     * </pre>
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importEmrContent(@RequestBody Map<String, String> request) {
        log.info("执行命令: POST /api/emr-sync/import - 接收到导入EMR病历内容请求: {}", request);
        
        try {
            String patientId = request.get("patientId");
            
            // 参数校验：patientId为null或空字符串返回400
            if (patientId == null || patientId.trim().isEmpty()) {
                log.warn("执行命令: POST /api/emr-sync/import - patientId为空，返回400");
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "patientId不能为空",
                    "importedCount", -1,
                    "patientId", patientId != null ? patientId : "",
                    "timestamp", LocalDateTime.now().toString()
                ));
            }
            
            // 调用服务层执行导入
            int importedCount = emrSyncService.importEmrContent(patientId);
            
            // 获取统计信息
            Map<String, Object> stats = emrSyncService.getEmrContentStats(patientId);
            
            if (importedCount >= 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", importedCount == 0 ? "未找到新的EMR病历数据" : "EMR病历内容导入成功");
                response.put("importedCount", importedCount);
                response.put("patientId", patientId);
                response.put("localEmrCount", stats.get("localEmrCount"));
                response.put("timestamp", LocalDateTime.now().toString());
                
                log.info("执行命令: POST /api/emr-sync/import - EMR病历内容导入成功 - patientId: {}, importedCount: {}", patientId, importedCount);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "EMR病历内容导入失败",
                    "importedCount", importedCount,
                    "patientId", patientId,
                    "timestamp", LocalDateTime.now().toString()
                );
                
                log.error("执行命令: POST /api/emr-sync/import - EMR病历内容导入失败 - patientId: {}, 返回码: {}", patientId, importedCount);
                return ResponseEntity.internalServerError().body(errorResponse);
            }
            
        } catch (Exception e) {
            log.error("执行命令: POST /api/emr-sync/import - 导入EMR病历内容异常", e);
            
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "导入EMR病历内容异常: " + e.getMessage(),
                "importedCount", -1,
                "timestamp", LocalDateTime.now().toString()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 批量导入患者的EMR病历内容
     * 
     * <p>接口：POST /api/emr-sync/batch-import</p>
     * <p>功能：批量导入多个患者的EMR病历内容</p>
     * 
     * @param request 批量导入请求参数，包含 patients 列表
     * @return 批量导入结果
     * 
     * @apiNote 请求示例：
     * <pre>
     * {
     *   "patients": [
     *     {"patientId": "990500000178405-1"},
     *     {"patientId": "990500000178406-1"}
     *   ]
     * }
     * </pre>
     * 
     * @apiNote 响应示例：
     * <pre>
     * {
     *   "success": true,
     *   "message": "批量导入完成",
     *   "totalPatients": 2,
     *   "successCount": 2,
     *   "failCount": 0,
     *   "totalImported": 10,
     *   "results": [...],
     *   "timestamp": "2026-01-11T10:00:00"
     * }
     * </pre>
     */
    @PostMapping("/batch-import")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> batchImportEmrContent(@RequestBody Map<String, Object> request) {
        log.info("执行命令: POST /api/emr-sync/batch-import - 接收到批量导入EMR病历内容请求");
        
        try {
            List<Map<String, String>> patients = (List<Map<String, String>>) request.get("patients");
            
            // 参数校验：patients列表为null或空返回400
            if (patients == null || patients.isEmpty()) {
                log.warn("执行命令: POST /api/emr-sync/batch-import - patients列表为空，返回400");
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "patients列表不能为空",
                    "timestamp", LocalDateTime.now().toString()
                ));
            }
            
            List<Map<String, Object>> results = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;
            int totalImported = 0;
            
            for (Map<String, String> patient : patients) {
                String patientId = patient.get("patientId");
                
                Map<String, Object> result = new HashMap<>();
                result.put("patientId", patientId);
                
                try {
                    if (patientId == null || patientId.trim().isEmpty()) {
                        result.put("success", false);
                        result.put("importedCount", -1);
                        result.put("message", "patientId不能为空");
                        failCount++;
                    } else {
                        int importedCount = emrSyncService.importEmrContent(patientId);
                        
                        if (importedCount >= 0) {
                            result.put("success", true);
                            result.put("importedCount", importedCount);
                            result.put("message", importedCount == 0 ? "未找到新的EMR病历" : "导入成功");
                            successCount++;
                            totalImported += importedCount;
                        } else {
                            result.put("success", false);
                            result.put("importedCount", importedCount);
                            result.put("message", "导入失败");
                            failCount++;
                        }
                    }
                } catch (Exception e) {
                    log.error("批量导入患者EMR病历内容异常 - patientId: {}", patientId, e);
                    
                    result.put("success", false);
                    result.put("importedCount", -1);
                    result.put("message", "导入异常: " + e.getMessage());
                    failCount++;
                }
                
                results.add(result);
            }
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "批量导入完成",
                "totalPatients", patients.size(),
                "successCount", successCount,
                "failCount", failCount,
                "totalImported", totalImported,
                "results", results,
                "timestamp", LocalDateTime.now().toString()
            );
            
            log.info("执行命令: POST /api/emr-sync/batch-import - 批量导入EMR病历内容完成 - totalPatients: {}, successCount: {}, failCount: {}, totalImported: {}", 
                patients.size(), successCount, failCount, totalImported);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("执行命令: POST /api/emr-sync/batch-import - 批量导入EMR病历内容异常", e);
            
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "批量导入EMR病历内容异常: " + e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 获取EMR病历内容同步统计信息
     * 
     * <p>接口：GET /api/emr-sync/stats</p>
     * <p>功能：获取指定患者的EMR病历内容统计信息</p>
     * 
     * @param patientId 患者ID
     * @return 统计信息
     * 
     * @apiNote 响应示例：
     * <pre>
     * {
     *   "patientId": "990500000178405-1",
     *   "localEmrCount": 5,
     *   "timestamp": "2026-01-11T10:00:00"
     * }
     * </pre>
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(@RequestParam String patientId) {
        log.debug("执行命令: GET /api/emr-sync/stats - 获取EMR病历内容统计信息 - patientId: {}", patientId);
        
        try {
            Map<String, Object> stats = emrSyncService.getEmrContentStats(patientId);
            stats.put("timestamp", LocalDateTime.now().toString());
            
            log.info("执行命令: GET /api/emr-sync/stats - 获取统计信息成功 - patientId: {}, localEmrCount: {}", 
                patientId, stats.get("localEmrCount"));
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("执行命令: GET /api/emr-sync/stats - 获取EMR统计信息异常 - patientId: {}", patientId, e);
            
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "获取统计信息异常: " + e.getMessage(),
                "patientId", patientId,
                "timestamp", LocalDateTime.now().toString()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
