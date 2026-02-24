package com.example.medaiassistant.controller;

import com.example.medaiassistant.hospital.service.LabSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * LIS检验结果同步控制器
 * 提供LIS检验结果同步的REST API接口
 * 
 * 功能包括：
 * 1. 导入单个患者的LIS检验结果
 * 2. 批量导入患者的LIS检验结果
 * 3. 获取导入状态和统计信息
 * 
 * 接口路径：/api/lab-sync
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-12
 */
@RestController
@RequestMapping("/api/lab-sync")
@Slf4j
public class LabSyncController {

    private final LabSyncService labSyncService;

    @Autowired
    public LabSyncController(LabSyncService labSyncService) {
        this.labSyncService = labSyncService;
    }

    /**
     * 导入单个患者的LIS检验结果
     * 
     * 接口：POST /api/lab-sync/import
     * 功能：根据主服务器病人ID自动获取入院日期并导入检验结果
     * 
     * @param request 导入请求参数
     * @return 导入结果
     * 
     * @apiNote 请求示例：
     * {
     *   "patientId": "990500000178405-1"
     * }
     * 
     * @apiNote 响应示例（成功）：
     * {
     *   "success": true,
     *   "message": "LIS检验结果导入成功",
     *   "importedCount": 15,
     *   "patientId": "990500000178405-1",
     *   "timestamp": "2025-12-12T19:30:00.123"
     * }
     * 
     * @apiNote 响应示例（失败）：
     * {
     *   "success": false,
     *   "message": "LIS检验结果导入失败: 主服务器病人ID不能为空",
     *   "importedCount": -1,
     *   "patientId": "",
     *   "timestamp": "2025-12-12T19:30:00.123"
     * }
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importLabResults(@RequestBody Map<String, String> request) {
        log.info("接收到导入LIS检验结果请求: {}", request);
        
        try {
            String patientId = request.get("patientId");
            
            if (patientId == null || patientId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("主服务器病人ID不能为空", patientId, ""));
            }
            
            // 执行导入
            int importedCount = labSyncService.importLabResults(patientId);
            
            if (importedCount >= 0) {
                Map<String, Object> response = Map.of(
                    "success", true,
                    "message", importedCount == 0 ? "未找到新的检验结果数据" : "LIS检验结果导入成功",
                    "importedCount", importedCount,
                    "patientId", patientId,
                    "timestamp", java.time.LocalDateTime.now().toString()
                );
                
                log.info("LIS检验结果导入成功 - 主服务器病人ID: {}, 导入记录数: {}", patientId, importedCount);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "LIS检验结果导入失败",
                    "importedCount", importedCount,
                    "patientId", patientId,
                    "timestamp", java.time.LocalDateTime.now().toString()
                );
                
                log.error("LIS检验结果导入失败 - 主服务器病人ID: {}, 返回码: {}", patientId, importedCount);
                return ResponseEntity.internalServerError().body(errorResponse);
            }
            
        } catch (Exception e) {
            log.error("导入LIS检验结果异常", e);
            
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "导入LIS检验结果异常: " + e.getMessage(),
                "importedCount", -1,
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 批量导入患者的LIS检验结果
     * 
     * 接口：POST /api/lab-sync/batch-import
     * 功能：批量导入多个患者的LIS检验结果
     * 
     * @param request 批量导入请求参数
     * @return 批量导入结果
     * 
     * @apiNote 请求示例：
     * {
     *   "patients": [
     *     {
     *       "patientId": "990500000178405-1"
     *     },
     *     {
     *       "patientId": "990500000178406-1"
     *     }
     *   ]
     * }
     * 
     * @apiNote 响应示例：
     * {
     *   "success": true,
     *   "message": "批量导入完成",
     *   "totalPatients": 2,
     *   "successCount": 2,
     *   "failureCount": 0,
     *   "totalImportedCount": 30,
     *   "results": [
     *     {
     *       "patientId": "990500000178405-1",
     *       "success": true,
     *       "importedCount": 15,
     *       "message": "导入成功"
     *     },
     *     {
     *       "patientId": "990500000178406-1",
     *       "success": true,
     *       "importedCount": 15,
     *       "message": "导入成功"
     *     }
     *   ],
     *   "timestamp": "2025-12-12T19:30:00.123"
     * }
     */
    @PostMapping("/batch-import")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> batchImportLabResults(@RequestBody Map<String, Object> request) {
        log.info("接收到批量导入LIS检验结果请求");
        
        try {
            // 提取患者列表
            java.util.List<Map<String, String>> patients = (java.util.List<Map<String, String>>) request.get("patients");
            
            if (patients == null || patients.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "患者列表不能为空",
                    "timestamp", java.time.LocalDateTime.now().toString()
                ));
            }
            
            java.util.List<Map<String, Object>> results = new java.util.ArrayList<>();
            int successCount = 0;
            int failureCount = 0;
            int totalImportedCount = 0;
            
            // 批量导入每个患者
            for (Map<String, String> patient : patients) {
                String patientId = patient.get("patientId");
                
                Map<String, Object> result = new java.util.HashMap<>();
                result.put("patientId", patientId);
                
                try {
                    // 参数验证
                    if (patientId == null || patientId.trim().isEmpty()) {
                        result.put("success", false);
                        result.put("importedCount", -1);
                        result.put("message", "主服务器病人ID不能为空");
                        failureCount++;
                    } else {
                        // 执行导入
                        int importedCount = labSyncService.importLabResults(patientId);
                        
                        if (importedCount >= 0) {
                            result.put("success", true);
                            result.put("importedCount", importedCount);
                            result.put("message", importedCount == 0 ? "未找到新的检验结果数据" : "导入成功");
                            successCount++;
                            totalImportedCount += importedCount;
                        } else {
                            result.put("success", false);
                            result.put("importedCount", importedCount);
                            result.put("message", "导入失败");
                            failureCount++;
                        }
                    }
                } catch (Exception e) {
                    log.error("批量导入患者检验结果异常 - 患者ID: {}", patientId, e);
                    
                    result.put("success", false);
                    result.put("importedCount", -1);
                    result.put("message", "导入异常: " + e.getMessage());
                    failureCount++;
                }
                
                results.add(result);
            }
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "批量导入完成",
                "totalPatients", patients.size(),
                "successCount", successCount,
                "failureCount", failureCount,
                "totalImportedCount", totalImportedCount,
                "results", results,
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            log.info("批量导入LIS检验结果完成 - 总患者数: {}, 成功: {}, 失败: {}, 总导入记录数: {}", 
                patients.size(), successCount, failureCount, totalImportedCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("批量导入LIS检验结果异常", e);
            
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "批量导入LIS检验结果异常: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 健康检查端点
     * 
     * 接口：GET /api/lab-sync/health
     * 功能：检查LIS检验结果同步服务的健康状态
     * 
     * @return 健康状态信息
     * 
     * @apiNote 响应示例：
     * {
     *   "status": "UP",
     *   "service": "lab-sync",
     *   "timestamp": "2025-12-12T19:30:00.123",
     *   "features": ["import-lab-results", "batch-import"]
     * }
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.debug("LIS检验结果同步服务健康检查");
        
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "lab-sync",
            "timestamp", java.time.LocalDateTime.now().toString(),
            "features", java.util.List.of("import-lab-results", "batch-import")
        ));
    }

    /**
     * 获取服务信息
     * 
     * 接口：GET /api/lab-sync/info
     * 功能：获取LIS检验结果同步服务的信息
     * 
     * @return 服务信息
     * 
     * @apiNote 响应示例：
     * {
     *   "serviceName": "LIS检验结果同步服务",
     *   "version": "1.0",
     *   "description": "从医院LIS系统同步检验结果到主服务器数据库",
     *   "supportedHospitals": ["cdwyy"],
     *   "timestamp": "2025-12-12T19:30:00.123"
     * }
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        log.debug("接收到获取LIS检验结果同步服务信息请求");
        
        try {
            Map<String, Object> info = Map.of(
                "serviceName", "LIS检验结果同步服务",
                "version", "1.0",
                "description", "从医院LIS系统同步检验结果到主服务器数据库",
                "supportedHospitals", java.util.List.of("cdwyy"),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            return ResponseEntity.ok(info);
            
        } catch (Exception e) {
            log.error("获取LIS检验结果同步服务信息失败", e);
            
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "获取服务信息失败: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message, String patientId, String targetPatientId) {
        return Map.of(
            "success", false,
            "message", message,
            "importedCount", -1,
            "patientId", patientId != null ? patientId : "",
            "targetPatientId", targetPatientId != null ? targetPatientId : "",
            "timestamp", java.time.LocalDateTime.now().toString()
        );
    }
}
