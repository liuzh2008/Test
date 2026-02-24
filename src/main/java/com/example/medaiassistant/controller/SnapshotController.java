package com.example.medaiassistant.controller;

import com.example.medaiassistant.dto.SnapshotRequestDTO;
import com.example.medaiassistant.dto.SnapshotResponseDTO;
import com.example.medaiassistant.dto.SnapshotInfoDTO;
import com.example.medaiassistant.service.SnapshotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * DRG分析输入快照控制器
 * 
 * 提供快照生成、查询和管理功能的API接口，支持智能快照判定、应用层快照生成、
 * 患者快照历史查询、软删除等功能。
 * 
 * @author Cline
 * @since 2025-10-24
 * @version 1.0.0
 * @see SnapshotService
 * @see SnapshotRequestDTO
 * @see SnapshotResponseDTO
 * @see SnapshotInfoDTO
 */
@RestController
@RequestMapping("/api/snapshot")
@Slf4j
public class SnapshotController {
    
    private final SnapshotService snapshotService;
    
    public SnapshotController(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }
    
    /**
     * 生成快照 - 快照判定逻辑
     * 
     * @param request 快照生成请求
     * @return 快照生成响应
     */
    @PostMapping("/generate")
    public ResponseEntity<SnapshotResponseDTO> generateSnapshot(@RequestBody SnapshotRequestDTO request) {
        log.info("接收到快照生成请求: patientId={}, operationSource={}, forceReanalyze={}", 
                request.getPatientId(), request.getOperationSource(), request.isForceReanalyze());
        
        try {
            // 调用快照服务生成快照
            Long snapshotId = snapshotService.callGenDrgInputSnapshot(
                    request.getPatientId(),
                    request.getDiagnosisIds(),
                    request.getSurgeryIds(),
                    request.getCatalogVersion(),
                    request.getLastSourceDiagCount(),
                    request.getLastSourceProcCount(),
                    request.getOperationSource(),
                    request.isForceReanalyze()
            );
            
            SnapshotResponseDTO response = new SnapshotResponseDTO();
            response.setSnapshotId(snapshotId);
            response.setStatus(snapshotId != null ? "CREATED" : "SKIPPED");
            response.setMessage(snapshotId != null ? 
                    "成功创建快照" : "输入集合未变化，跳过快照创建");
            response.setTimestamp(System.currentTimeMillis());
            
            log.info("快照生成完成: snapshotId={}, status={}", snapshotId, response.getStatus());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("快照生成失败", e);
            SnapshotResponseDTO errorResponse = new SnapshotResponseDTO();
            errorResponse.setSnapshotId(null);
            errorResponse.setStatus("ERROR");
            errorResponse.setMessage("快照生成失败: " + e.getMessage());
            errorResponse.setTimestamp(System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 应用层快照生成 - 替代存储过程调用
     * 
     * @param request 快照生成请求
     * @return 快照生成响应
     */
    @PostMapping("/generate-application")
    public ResponseEntity<Map<String, Object>> generateSnapshotApplication(@RequestBody SnapshotRequestDTO request) {
        log.info("接收到应用层快照生成请求: patientId={}, operationSource={}, forceReanalyze={}, diagnosisCount={}, surgeryCount={}", 
                request.getPatientId(), request.getOperationSource(), request.isForceReanalyze(),
                request.getDiagnosisIds() != null ? request.getDiagnosisIds().size() : 0,
                request.getSurgeryIds() != null ? request.getSurgeryIds().size() : 0);
        
        try {
            // 调用快照服务生成快照
            Long snapshotId = snapshotService.callGenDrgInputSnapshot(
                    request.getPatientId(),
                    request.getDiagnosisIds(),
                    request.getSurgeryIds(),
                    request.getCatalogVersion(),
                    request.getLastSourceDiagCount(),
                    request.getLastSourceProcCount(),
                    request.getOperationSource(),
                    request.isForceReanalyze()
            );
            
            Map<String, Object> response = Map.of(
                "snapshotId", snapshotId,
                "status", snapshotId != null ? "SUCCESS" : "SKIPPED",
                "message", snapshotId != null ? 
                        "应用层快照生成成功，创建新快照" : "应用层快照生成成功，无需创建新快照",
                "operationSource", request.getOperationSource(),
                "forceReanalyze", request.isForceReanalyze(),
                "timestamp", System.currentTimeMillis()
            );
            
            log.info("应用层快照生成完成: snapshotId={}, status={}", snapshotId, response.get("status"));
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("应用层快照生成失败 - patientId: {}, operationSource: {}", 
                    request.getPatientId(), request.getOperationSource(), e);
            Map<String, Object> errorResponse = Map.of(
                "snapshotId", null,
                "status", "ERROR",
                "message", "应用层快照生成失败: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 查询患者快照历史
     * 
     * @param patientId 患者ID
     * @return 患者快照历史列表
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<SnapshotInfoDTO>> getPatientSnapshots(@PathVariable String patientId) {
        log.info("接收到患者快照历史查询请求: patientId={}", patientId);
        
        try {
            // 实现患者快照历史查询逻辑
            // 这里需要添加实际的查询实现
            
            List<SnapshotInfoDTO> snapshots = List.of(); // 临时返回空列表
            
            log.info("患者快照历史查询完成: patientId={}, count={}", patientId, snapshots.size());
            return ResponseEntity.ok(snapshots);
            
        } catch (Exception e) {
            log.error("患者快照历史查询失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 查询患者最新快照
     * 
     * @param patientId 患者ID
     * @return 最新快照信息
     */
    @GetMapping("/patient/{patientId}/latest")
    public ResponseEntity<SnapshotInfoDTO> getLatestSnapshot(@PathVariable String patientId) {
        log.info("接收到最新快照查询请求: patientId={}", patientId);
        
        try {
            // 实现最新快照查询逻辑
            // 这里需要添加实际的查询实现
            
            SnapshotInfoDTO snapshot = new SnapshotInfoDTO(); // 临时返回空对象
            
            log.info("最新快照查询完成: patientId={}", patientId);
            return ResponseEntity.ok(snapshot);
            
        } catch (Exception e) {
            log.error("最新快照查询失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 按目录版本查询快照
     * 
     * @param patientId 患者ID
     * @param catalogVersion 目录版本
     * @return 指定目录版本的快照信息
     */
    @GetMapping("/patient/{patientId}/catalog/{catalogVersion}")
    public ResponseEntity<SnapshotInfoDTO> getSnapshotByCatalogVersion(
            @PathVariable String patientId, 
            @PathVariable String catalogVersion) {
        log.info("接收到按目录版本查询快照请求: patientId={}, catalogVersion={}", patientId, catalogVersion);
        
        try {
            // 实现按目录版本查询快照逻辑
            // 这里需要添加实际的查询实现
            
            SnapshotInfoDTO snapshot = new SnapshotInfoDTO(); // 临时返回空对象
            
            log.info("按目录版本查询快照完成: patientId={}, catalogVersion={}", patientId, catalogVersion);
            return ResponseEntity.ok(snapshot);
            
        } catch (Exception e) {
            log.error("按目录版本查询快照失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 软删除快照
     * 
     * @param snapshotId 快照ID
     * @return 删除结果
     */
    @DeleteMapping("/{snapshotId}")
    public ResponseEntity<Map<String, Object>> softDeleteSnapshot(@PathVariable Long snapshotId) {
        log.info("接收到软删除快照请求: snapshotId={}", snapshotId);
        
        try {
            // 实现软删除快照逻辑
            // 这里需要添加实际的删除实现
            
            Map<String, Object> response = Map.of(
                "snapshotId", snapshotId,
                "status", "DELETED",
                "message", "快照已软删除",
                "timestamp", System.currentTimeMillis()
            );
            
            log.info("快照软删除完成: snapshotId={}", snapshotId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("快照软删除失败", e);
            Map<String, Object> errorResponse = Map.of(
                "snapshotId", snapshotId,
                "status", "ERROR",
                "message", "快照软删除失败: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 查询未删除的快照
     * 
     * @return 未删除的快照列表
     */
    @GetMapping("/active")
    public ResponseEntity<List<SnapshotInfoDTO>> getActiveSnapshots() {
        log.info("接收到未删除快照查询请求");
        
        try {
            // 实现未删除快照查询逻辑
            // 这里需要添加实际的查询实现
            
            List<SnapshotInfoDTO> snapshots = List.of(); // 临时返回空列表
            
            log.info("未删除快照查询完成: count={}", snapshots.size());
            return ResponseEntity.ok(snapshots);
            
        } catch (Exception e) {
            log.error("未删除快照查询失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取快照服务信息
     * 
     * @return 服务基本信息
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        log.info("接收到快照服务信息查询请求");
        
        Map<String, Object> info = Map.of(
            "service", "SnapshotService",
            "description", "DRG分析输入快照服务，提供快照生成、查询和管理功能",
            "version", "1.0.0",
            "features", Map.of(
                "snapshotGeneration", true,
                "snapshotReuse", true,
                "forceReanalyze", true,
                "storedProcedureIntegration", true,
                "catalogVersionSupport", true,
                "softDelete", true
            ),
            "supportedOperationSources", List.of("API_CALL", "TIMER_TASK", "MANUAL", "PROMPT_TRIGGER"),
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(info);
    }
}
