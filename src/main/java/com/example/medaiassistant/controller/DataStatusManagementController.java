package com.example.medaiassistant.controller;

import com.example.medaiassistant.model.DataStatus;
import com.example.medaiassistant.model.EncryptedDataTemp;
import com.example.medaiassistant.service.EncryptedDataTempService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 数据状态管理控制器
 * 用于诊断和手动处理数据状态问题
 * 
 * @author System
 * @version 1.0
 * @since 2025-10-01
 */
@RestController
@RequestMapping("/api/data-status")
public class DataStatusManagementController {

    private static final Logger logger = LoggerFactory.getLogger(DataStatusManagementController.class);

    @Autowired
    private EncryptedDataTempService encryptedDataTempService;

    /**
     * 查询数据状态
     * 
     * @param dataId 数据ID
     * @return 数据状态信息
     */
    @GetMapping("/{dataId}")
    public ResponseEntity<Map<String, Object>> getDataStatus(@PathVariable String dataId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<EncryptedDataTemp> dataOptional = encryptedDataTempService.findById(dataId);
            if (dataOptional.isPresent()) {
                EncryptedDataTemp data = dataOptional.get();
                response.put("status", "SUCCESS");
                response.put("dataId", dataId);
                response.put("currentStatus", data.getStatus());
                response.put("requestId", data.getRequestId());
                response.put("source", data.getSource());
                response.put("receivedTime", data.getReceivedTime());
                response.put("processedTime", data.getProcessedTime());
                response.put("createdAt", data.getCreatedAt());
                response.put("updatedAt", data.getUpdatedAt());
                response.put("isFinalStatus", data.getStatus().isFinal());
                
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "NOT_FOUND");
                response.put("message", "未找到数据ID: " + dataId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("查询数据状态失败", e);
            response.put("status", "ERROR");
            response.put("message", "查询数据状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 手动标记数据为错误状态
     * 用于处理超时或异常的数据
     * 
     * @param dataId 数据ID
     * @param request 请求体，包含错误原因
     * @return 操作结果
     */
    @PostMapping("/{dataId}/mark-error")
    public ResponseEntity<Map<String, Object>> markAsError(
            @PathVariable String dataId,
            @RequestBody Map<String, String> request) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            String errorReason = request.getOrDefault("reason", "手动标记为错误状态");
            
            // 检查当前状态
            Optional<DataStatus> currentStatusOpt = encryptedDataTempService.getCurrentStatus(dataId);
            if (currentStatusOpt.isEmpty()) {
                response.put("status", "NOT_FOUND");
                response.put("message", "未找到数据ID: " + dataId);
                return ResponseEntity.notFound().build();
            }
            
            DataStatus currentStatus = currentStatusOpt.get();
            logger.info("手动标记数据为错误状态 - 数据ID: {}, 当前状态: {}, 错误原因: {}", 
                    dataId, currentStatus, errorReason);
            
            // 标记为错误状态
            encryptedDataTempService.markAsError(dataId, errorReason);
            
            response.put("status", "SUCCESS");
            response.put("message", "数据已标记为错误状态");
            response.put("dataId", dataId);
            response.put("previousStatus", currentStatus);
            response.put("currentStatus", DataStatus.ERROR);
            response.put("errorReason", errorReason);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            logger.warn("状态转换不允许 - 数据ID: {}: {}", dataId, e.getMessage());
            response.put("status", "INVALID_TRANSITION");
            response.put("message", "状态转换不允许: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("手动标记错误状态失败 - 数据ID: {}", dataId, e);
            response.put("status", "ERROR");
            response.put("message", "操作失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取处理超时的数据列表
     * 帮助识别可能需要手动干预的数据
     * 
     * @param timeoutMinutes 超时时间（分钟），默认30分钟
     * @return 超时数据列表
     */
    @GetMapping("/timeout")
    public ResponseEntity<Map<String, Object>> getTimeoutData(
            @RequestParam(defaultValue = "30") int timeoutMinutes) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            // 这里需要实现查询超时数据的逻辑
            // 目前返回基本的响应结构
            response.put("status", "SUCCESS");
            response.put("message", "查询超时数据功能待实现");
            response.put("timeoutMinutes", timeoutMinutes);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询超时数据失败", e);
            response.put("status", "ERROR");
            response.put("message", "查询超时数据失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
