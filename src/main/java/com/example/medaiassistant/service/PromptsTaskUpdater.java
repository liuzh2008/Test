package com.example.medaiassistant.service;

import com.example.medaiassistant.model.AlertTask;
import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.repository.AlertTaskRepository;
import com.example.medaiassistant.repository.PromptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 处理Prompts与AlertTask状态更新的服务类
 */
@Service
public class PromptsTaskUpdater {
    @Autowired
    private AlertTaskRepository alertTaskRepository;
    
    @Autowired
    private PromptRepository promptRepository;

    /**
     * 根据prompt模板名称和患者ID更新任务状态
     * @param promptTemplateName prompt模板名称
     * @param patientId 患者ID
     */
    @Transactional
    public void updateTaskStatusForPrompt(String promptTemplateName, String patientId) {
        if (!"病重每2日查房记录".equals(promptTemplateName)) {
            return;
        }

        // 检查prompts表中是否有对应记录
        List<Prompt> prompts = promptRepository.findByPatientIdAndPromptTemplateName(
            patientId,
            "病重每2日查房记录");
            
        if (prompts.isEmpty()) {
            return;
        }

        // 查找患者对应的待处理任务
        List<AlertTask> tasks = alertTaskRepository.findByPatientIdAndTaskTypeAndTaskStatus(
            patientId, 
            "病重每2日查房提醒", 
            AlertTask.TaskStatus.待处理);

        // 更新任务状态
        tasks.forEach(task -> {
            // 额外检查48小时间隔
            if (task.getCompletedTime() == null || 
                LocalDateTime.now().isAfter(task.getCompletedTime().plusHours(48))) {
                
                task.setTaskStatus(AlertTask.TaskStatus.已完成);
                task.setCompletedTime(LocalDateTime.now());
                task.setNextTaskTime(LocalDateTime.now().plusHours(48));
                alertTaskRepository.save(task);
            }
        });
    }
}
