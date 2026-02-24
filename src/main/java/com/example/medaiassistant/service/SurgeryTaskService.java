package com.example.medaiassistant.service;

import com.example.medaiassistant.dto.SurgeryTaskDTO;
import com.example.medaiassistant.model.SurgeryTask;
import com.example.medaiassistant.repository.SurgeryTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 手术任务服务类
 * 提供手术任务的业务逻辑处理
 */
@Service
public class SurgeryTaskService {

    @Autowired
    private SurgeryTaskRepository surgeryTaskRepository;

    /**
     * 根据患者ID获取手术任务列表
     * @param patientId 患者ID
     * @return 手术任务DTO列表
     */
    public List<SurgeryTaskDTO> getSurgeryTasksByPatientId(String patientId) {
        List<SurgeryTask> surgeryTasks = surgeryTaskRepository.findByPatientId(patientId);
        return surgeryTasks.stream()
                .map(SurgeryTaskDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 根据患者ID和状态获取手术任务列表
     * @param patientId 患者ID
     * @param status 任务状态
     * @return 手术任务DTO列表
     */
    public List<SurgeryTaskDTO> getSurgeryTasksByPatientIdAndStatus(String patientId, SurgeryTask.TaskStatus status) {
        List<SurgeryTask> surgeryTasks = surgeryTaskRepository.findByPatientIdAndTaskStatus(patientId, status);
        return surgeryTasks.stream()
                .map(SurgeryTaskDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有手术任务
     * @return 手术任务DTO列表
     */
    public List<SurgeryTaskDTO> getAllSurgeryTasks() {
        List<SurgeryTask> surgeryTasks = surgeryTaskRepository.findAll();
        return surgeryTasks.stream()
                .map(SurgeryTaskDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 根据任务ID获取手术任务
     * @param taskId 任务ID
     * @return 手术任务DTO对象，如果不存在则返回null
     */
    public SurgeryTaskDTO getSurgeryTaskById(Integer taskId) {
        return surgeryTaskRepository.findById(taskId)
                .map(SurgeryTaskDTO::fromEntity)
                .orElse(null);
    }

    /**
     * 创建手术任务
     * @param surgeryTaskDTO 手术任务DTO对象
     * @return 创建后的手术任务DTO对象
     */
    public SurgeryTaskDTO createSurgeryTask(SurgeryTaskDTO surgeryTaskDTO) {
        SurgeryTask surgeryTask = surgeryTaskDTO.toEntity();
        SurgeryTask savedTask = surgeryTaskRepository.save(surgeryTask);
        return SurgeryTaskDTO.fromEntity(savedTask);
    }

    /**
     * 更新手术任务
     * @param taskId 任务ID
     * @param surgeryTaskDTO 手术任务DTO对象
     * @return 更新后的手术任务DTO对象，如果任务不存在则返回null
     */
    public SurgeryTaskDTO updateSurgeryTask(Integer taskId, SurgeryTaskDTO surgeryTaskDTO) {
        return surgeryTaskRepository.findById(taskId)
                .map(existingTask -> {
                    SurgeryTask updatedTask = surgeryTaskDTO.toEntity();
                    updatedTask.setTaskId(taskId);
                    // 保留原有的创建时间，设置新的更新时间
                    updatedTask.setCreatedAt(existingTask.getCreatedAt());
                    updatedTask.setUpdatedAt(new java.util.Date());
                    SurgeryTask savedTask = surgeryTaskRepository.save(updatedTask);
                    return SurgeryTaskDTO.fromEntity(savedTask);
                })
                .orElse(null);
    }

    /**
     * 删除手术任务
     * @param taskId 任务ID
     * @return 删除成功返回true，任务不存在返回false
     */
    public boolean deleteSurgeryTask(Integer taskId) {
        if (surgeryTaskRepository.existsById(taskId)) {
            surgeryTaskRepository.deleteById(taskId);
            return true;
        }
        return false;
    }
}
