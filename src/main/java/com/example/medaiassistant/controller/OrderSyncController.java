package com.example.medaiassistant.controller;

import com.example.medaiassistant.hospital.service.OrderSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 医嘱同步REST API控制器
 * 
 * <p>提供从医院HIS系统（Oracle）同步医嘱数据到主服务器数据库的REST API接口</p>
 * 
 * <h2>API端点</h2>
 * <ul>
 *   <li>{@code GET /api/order-sync/health} - 健康检查，确认服务状态</li>
 *   <li>{@code POST /api/order-sync/import} - 导入单个患者的医嘱数据</li>
 *   <li>{@code POST /api/order-sync/batch-import} - 批量导入多个患者的医嘱数据</li>
 * </ul>
 * 
 * <h2>数据流程</h2>
 * <pre>
 * Oracle HIS (V_SS_ORDERS) → OrderSyncService → LONGTERMORDER表
 * </pre>
 * 
 * <h2>字段映射</h2>
 * <ul>
 *   <li>ORDER_ID → orderId（主键）</li>
 *   <li>PATIENT_ID_OLD → patientId</li>
 *   <li>PHYSICIAN → physician（医生）</li>
 *   <li>ORDER_NAME → orderName（医嘱名称）</li>
 *   <li>DOSAGE → dosage（剂量）</li>
 *   <li>UNIT → unit（剂量单位）</li>
 *   <li>FREQUENCY → frequency（频次）</li>
 *   <li>ROUTE → route（给药途径）</li>
 * </ul>
 * 
 * <h2>默认值设置</h2>
 * <ul>
 *   <li>ISANALYZED = 0（新记录未分析）</li>
 *   <li>ISTRIGGERED = 0（新记录未触发）</li>
 * </ul>
 * 
 * @author TDD
 * @version 1.0
 * @since 2026-01-10
 * @see com.example.medaiassistant.hospital.service.OrderSyncService
 * @see com.example.medaiassistant.model.LongTermOrder
 */
@RestController
@RequestMapping("/api/order-sync")
@Slf4j
public class OrderSyncController {

    /** 
     * 医嘱同步服务
     * <p>负责实际的数据同步操作，包括查询Oracle和写入主服务器数据库</p>
     */
    private final OrderSyncService orderSyncService;

    /**
     * 构造函数
     * 
     * @param orderSyncService 医嘱同步服务实例，由Spring自动注入
     */
    @Autowired
    public OrderSyncController(OrderSyncService orderSyncService) {
        this.orderSyncService = orderSyncService;
    }

    /**
     * 健康检查端点
     * 
     * <p>检查医嘱同步服务的健康状态，用于监控和负载均衡。</p>
     * 
     * <h3>接口信息</h3>
     * <ul>
     *   <li><strong>路径</strong>: GET /api/order-sync/health</li>
     *   <li><strong>HTTP方法</strong>: GET</li>
     *   <li><strong>请求参数</strong>: 无</li>
     * </ul>
     * 
     * <h3>响应字段</h3>
     * <ul>
     *   <li>{@code status} - 服务状态（UP/DOWN）</li>
     *   <li>{@code service} - 服务名称</li>
     *   <li>{@code timestamp} - 响应时间戳</li>
     *   <li>{@code features} - 支持的功能列表</li>
     * </ul>
     * 
     * @return ResponseEntity 包含健康状态信息的响应实体
     * 
     * @apiNote 响应示例:
     * <pre>
     * {
     *   "status": "UP",
     *   "service": "OrderSyncService",
     *   "timestamp": "2026-01-10T10:00:00",
     *   "features": ["import-orders", "batch-import"]
     * }
     * </pre>
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.debug("[执行命令] GET /api/order-sync/health - 医嘱同步服务健康检查");
        
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "OrderSyncService",
            "timestamp", LocalDateTime.now().toString(),
            "features", List.of("import-orders", "batch-import")
        ));
    }

    /**
     * 导入单个患者的医嘱
     * 
     * <p>根据主服务器病人ID从 Oracle HIS 系统导入患者的长期医嘱数据。</p>
     * 
     * <h3>接口信息</h3>
     * <ul>
     *   <li><strong>路径</strong>: POST /api/order-sync/import</li>
     *   <li><strong>HTTP方法</strong>: POST</li>
     *   <li><strong>Content-Type</strong>: application/json</li>
     * </ul>
     * 
     * <h3>请求参数</h3>
     * <ul>
     *   <li>{@code patientId} (必填) - 主服务器病人ID，格式为"原始患者ID_就诊序号"（如: "990500000178405_1"）</li>
     * </ul>
     * 
     * <h3>处理逻辑</h3>
     * <ol>
     *   <li>验证patientId参数，空或null返回400错误</li>
     *   <li>解析patientId获取原始患者ID和就诊序号</li>
     *   <li>从 Oracle HIS的V_SS_ORDERS视图查询医嘱数据</li>
     *   <li>字段映射转换，重复记录检测</li>
     *   <li>写入主服务器数据库</li>
     * </ol>
     * 
     * <h3>响应字段</h3>
     * <ul>
     *   <li>{@code success} - 是否成功</li>
     *   <li>{@code message} - 响应消息</li>
     *   <li>{@code importedCount} - 导入的医嘱记录数量（-1表示失败）</li>
     *   <li>{@code patientId} - 请求的患者ID</li>
     *   <li>{@code sourceOrderCount} - Oracle源系统中的医嘱数量</li>
     *   <li>{@code timestamp} - 响应时间戳</li>
     * </ul>
     * 
     * @param request 导入请求参数，包含 patientId
     * @return ResponseEntity 包含导入结果的响应实体
     * 
     * @apiNote 请求示例:
     * <pre>
     * {
     *   "patientId": "990500000178405_1"
     * }
     * </pre>
     * 
     * @apiNote 成功响应示例:
     * <pre>
     * {
     *   "success": true,
     *   "message": "医嘱导入成功",
     *   "importedCount": 12,
     *   "patientId": "990500000178405_1",
     *   "sourceOrderCount": 15,
     *   "timestamp": "2026-01-10T10:00:00"
     * }
     * </pre>
     * 
     * @apiNote 参数错误响应示例 (HTTP 400):
     * <pre>
     * {
     *   "success": false,
     *   "message": "patientId不能为空",
     *   "importedCount": -1,
     *   "patientId": "",
     *   "timestamp": "2026-01-10T10:00:00"
     * }
     * </pre>
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importOrders(@RequestBody Map<String, String> request) {
        log.info("[执行命令] POST /api/order-sync/import - 接收到导入医嘱请求: {}", request);
        
        try {
            String patientId = request.get("patientId");
            
            // 参数验证：空或null的patientId返回400
            if (patientId == null || patientId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "patientId不能为空",
                    "importedCount", -1,
                    "patientId", patientId != null ? patientId : "",
                    "timestamp", LocalDateTime.now().toString()
                ));
            }
            
            int importedCount = orderSyncService.importOrders(patientId);
            
            // 获取统计信息
            Map<String, Object> stats = orderSyncService.getOrderStats(patientId);
            
            if (importedCount >= 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", importedCount == 0 ? "未找到新的医嘱数据" : "医嘱导入成功");
                response.put("importedCount", importedCount);
                response.put("patientId", patientId);
                response.put("sourceOrderCount", stats.get("sourceOrderCount"));
                response.put("timestamp", LocalDateTime.now().toString());
                
                log.info("[执行命令] POST /api/order-sync/import - 医嘱导入成功 - patientId: {}, importedCount: {}", patientId, importedCount);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "医嘱导入失败",
                    "importedCount", importedCount,
                    "patientId", patientId,
                    "timestamp", LocalDateTime.now().toString()
                );
                
                log.error("[执行命令] POST /api/order-sync/import - 医嘱导入失败 - patientId: {}, 返回码: {}", patientId, importedCount);
                return ResponseEntity.internalServerError().body(errorResponse);
            }
            
        } catch (Exception e) {
            log.error("[执行命令] POST /api/order-sync/import - 导入医嘱异常", e);
            
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "导入医嘱异常: " + e.getMessage(),
                "importedCount", -1,
                "timestamp", LocalDateTime.now().toString()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 批量导入患者的医嘱
     * 
     * <p>批量导入多个患者的长期医嘱数据，每个患者独立处理。</p>
     * 
     * <h3>接口信息</h3>
     * <ul>
     *   <li><strong>路径</strong>: POST /api/order-sync/batch-import</li>
     *   <li><strong>HTTP方法</strong>: POST</li>
     *   <li><strong>Content-Type</strong>: application/json</li>
     * </ul>
     * 
     * <h3>请求参数</h3>
     * <ul>
     *   <li>{@code patients} (必填) - 患者列表，每个元素包含patientId</li>
     * </ul>
     * 
     * <h3>处理逻辑</h3>
     * <ol>
     *   <li>验证patients列表，空列表返回400错误</li>
     *   <li>遍历患者列表，逐个调用导入逻辑</li>
     *   <li>统计成功/失败数量</li>
     *   <li>返回合并结果</li>
     * </ol>
     * 
     * <h3>响应字段</h3>
     * <ul>
     *   <li>{@code success} - 整体是否成功</li>
     *   <li>{@code message} - 响应消息</li>
     *   <li>{@code totalPatients} - 总患者数</li>
     *   <li>{@code successCount} - 成功导入的患者数</li>
     *   <li>{@code failCount} - 失败的患者数</li>
     *   <li>{@code totalImported} - 总导入医嘱记录数</li>
     *   <li>{@code results} - 每个患者的导入结果数组</li>
     *   <li>{@code timestamp} - 响应时间戳</li>
     * </ul>
     * 
     * @param request 批量导入请求参数，包含 patients 列表
     * @return ResponseEntity 包含批量导入结果的响应实体
     * 
     * @apiNote 请求示例:
     * <pre>
     * {
     *   "patients": [
     *     {"patientId": "990500000178405_1"},
     *     {"patientId": "990500000178406_1"}
     *   ]
     * }
     * </pre>
     * 
     * @apiNote 响应示例:
     * <pre>
     * {
     *   "success": true,
     *   "message": "批量导入完成",
     *   "totalPatients": 2,
     *   "successCount": 2,
     *   "failCount": 0,
     *   "totalImported": 25,
     *   "results": [
     *     {
     *       "patientId": "990500000178405_1",
     *       "success": true,
     *       "importedCount": 12,
     *       "message": "导入成功"
     *     },
     *     {
     *       "patientId": "990500000178406_1",
     *       "success": true,
     *       "importedCount": 13,
     *       "message": "导入成功"
     *     }
     *   ],
     *   "timestamp": "2026-01-10T10:00:00"
     * }
     * </pre>
     */
    @PostMapping("/batch-import")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> batchImportOrders(@RequestBody Map<String, Object> request) {
        log.info("[执行命令] POST /api/order-sync/batch-import - 接收到批量导入医嘱请求");
        
        try {
            List<Map<String, String>> patients = (List<Map<String, String>>) request.get("patients");
            
            // 参数验证：空列表返回400
            if (patients == null || patients.isEmpty()) {
                log.warn("[执行命令] POST /api/order-sync/batch-import - patients列表为空，返回400");
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
                        int importedCount = orderSyncService.importOrders(patientId);
                        
                        if (importedCount >= 0) {
                            result.put("success", true);
                            result.put("importedCount", importedCount);
                            result.put("message", importedCount == 0 ? "未找到新的医嘱" : "导入成功");
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
                    log.error("批量导入患者医嘱异常 - patientId: {}", patientId, e);
                    
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
            
            log.info("[执行命令] POST /api/order-sync/batch-import - 批量导入医嘱完成 - totalPatients: {}, successCount: {}, failCount: {}, totalImported: {}", 
                patients.size(), successCount, failCount, totalImported);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("[执行命令] POST /api/order-sync/batch-import - 批量导入医嘱异常", e);
            
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "批量导入医嘱异常: " + e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
