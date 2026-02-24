package com.example.medaiassistant.service;

import com.example.medaiassistant.model.AlertTask;
import com.example.medaiassistant.repository.AlertTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 告警任务服务层测试
 * 使用Mockito进行单元测试，不加载Spring上下文
 * 
 * @author Cline
 * @since 2025-12-04
 */
@ExtendWith(MockitoExtension.class)
class AlertTaskServiceTest {

    @Mock
    private AlertTaskRepository alertTaskRepository;

    @InjectMocks
    private AlertTaskService alertTaskService;

    /**
     * 测试更新告警任务状态 - 成功场景
     */
    @Test
    void updateTaskStatus_ShouldReturnTrueWhenUpdateSuccessful() {
        // Arrange: 准备测试数据
        Integer taskId = 1;
        AlertTask.TaskStatus newStatus = AlertTask.TaskStatus.已完成;
        
        // 模拟Repository行为
        when(alertTaskRepository.updateTaskStatusByTaskId(taskId, newStatus))
                .thenReturn(1); // 模拟更新1行成功

        // Act: 执行方法
        boolean result = alertTaskService.updateTaskStatus(taskId, newStatus);

        // Assert: 验证结果
        assertTrue(result, "更新状态应返回true");
        verify(alertTaskRepository, times(1)).updateTaskStatusByTaskId(taskId, newStatus);
    }

    /**
     * 测试更新告警任务状态 - 失败场景（任务不存在）
     */
    @Test
    void updateTaskStatus_ShouldReturnFalseWhenTaskNotFound() {
        // Arrange: 准备测试数据
        Integer taskId = 999; // 不存在的任务ID
        AlertTask.TaskStatus newStatus = AlertTask.TaskStatus.已完成;
        
        // 模拟Repository行为
        when(alertTaskRepository.updateTaskStatusByTaskId(taskId, newStatus))
                .thenReturn(0); // 模拟更新0行（任务不存在）

        // Act: 执行方法
        boolean result = alertTaskService.updateTaskStatus(taskId, newStatus);

        // Assert: 验证结果
        assertFalse(result, "更新不存在的任务应返回false");
        verify(alertTaskRepository, times(1)).updateTaskStatusByTaskId(taskId, newStatus);
    }

    /**
     * 测试根据任务ID获取告警任务 - 成功场景
     */
    @Test
    void getTaskById_ShouldReturnTaskWhenExists() {
        // Arrange: 准备测试数据
        Integer taskId = 1;
        AlertTask expectedTask = new AlertTask();
        expectedTask.setTaskId(taskId);
        expectedTask.setTaskStatus(AlertTask.TaskStatus.待处理);
        expectedTask.setCreatedTime(LocalDateTime.now());
        
        // 模拟Repository行为
        when(alertTaskRepository.findById(taskId))
                .thenReturn(Optional.of(expectedTask));

        // Act: 执行方法
        AlertTask result = alertTaskService.getTaskById(taskId);

        // Assert: 验证结果
        assertNotNull(result, "返回的任务不应为null");
        assertEquals(taskId, result.getTaskId(), "任务ID应匹配");
        assertEquals(AlertTask.TaskStatus.待处理, result.getTaskStatus(), "任务状态应匹配");
        verify(alertTaskRepository, times(1)).findById(taskId);
    }

    /**
     * 测试根据任务ID获取告警任务 - 任务不存在场景
     */
    @Test
    void getTaskById_ShouldReturnNullWhenTaskNotFound() {
        // Arrange: 准备测试数据
        Integer taskId = 999; // 不存在的任务ID
        
        // 模拟Repository行为
        when(alertTaskRepository.findById(taskId))
                .thenReturn(Optional.empty());

        // Act: 执行方法
        AlertTask result = alertTaskService.getTaskById(taskId);

        // Assert: 验证结果
        assertNull(result, "不存在的任务应返回null");
        verify(alertTaskRepository, times(1)).findById(taskId);
    }

    /**
     * 测试根据患者ID获取待处理的告警任务
     */
    @Test
    void getPendingTasksByPatientId_ShouldReturnPendingTasks() {
        // Arrange: 准备测试数据
        String patientId = "P001";
        
        // 模拟Repository行为
        when(alertTaskRepository.findByPatientIdAndTaskStatus(patientId, AlertTask.TaskStatus.待处理))
                .thenReturn(java.util.Collections.emptyList());

        // Act: 执行方法
        var result = alertTaskService.getPendingTasksByPatientId(patientId);

        // Assert: 验证结果
        assertNotNull(result, "返回的列表不应为null");
        verify(alertTaskRepository, times(1)).findByPatientIdAndTaskStatus(patientId, AlertTask.TaskStatus.待处理);
    }
}
