package com.example.medaiassistant.controller;

import com.example.medaiassistant.model.AlertTask;
import com.example.medaiassistant.service.AlertTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 告警任务控制器
 * 处理与告警任务相关的HTTP请求
 * 
 * @author Cline
 * @since 2025-08-06
 */
@RestController
@RequestMapping("/api/alert-tasks")
public class AlertTaskController {
    
    @Autowired
    private AlertTaskService alertTaskService;
    
    /**
     * 根据患者ID获取待处理的告警任务
     * 
     * @param patientId 患者ID
     * @return 待处理的告警任务列表
     */
    @GetMapping(value = "/pending", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<AlertTask>> getPendingTasksByPatientId(@RequestParam String patientId) {
        List<AlertTask> pendingTasks = alertTaskService.getPendingTasksByPatientId(patientId);
        return Mono.just(pendingTasks);
    }
    
    /**
     * 更新告警任务状态API
     * 
     * 该接口用于更新指定告警任务的状态。支持将任务状态更新为"待处理"、"进行中"或"已完成"三种状态之一。
     * 接口会验证任务ID是否存在，确保数据安全性。如果任务不存在，将返回404状态码；如果更新失败，将返回400状态码。
     * 
     * @param taskId 任务ID，用于标识要更新的告警任务，必须是数据库中已存在的任务ID
     * @param taskStatus 新的任务状态，可选值包括：待处理、进行中、已完成
     * @return Mono<String> 响应实体，包含更新结果信息
     *         - 成功：HTTP 200 OK，返回"任务状态更新成功"
     *         - 任务不存在：HTTP 404 Not Found
     *         - 更新失败：HTTP 400 Bad Request，返回"任务状态更新失败"
     * @since 2025-08-07
     * @author Cline
     * @see AlertTaskService#updateTaskStatus(Integer, AlertTask.TaskStatus)
     * @see AlertTaskService#getTaskById(Integer)
     */
    @PutMapping(value = "/{taskId}/status", produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<String> updateTaskStatus(
            @PathVariable Integer taskId,
            @RequestParam AlertTask.TaskStatus taskStatus) {
        // 检查任务是否存在
        AlertTask task = alertTaskService.getTaskById(taskId);
        if (task == null) {
            return Mono.just("任务不存在").map(msg -> {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
            });
        }
        
        // 更新任务状态
        boolean updated = alertTaskService.updateTaskStatus(taskId, taskStatus);
        if (updated) {
            return Mono.just("任务状态更新成功");
        } else {
            return Mono.just("任务状态更新失败").map(msg -> {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
            });
        }
    }
    
}
