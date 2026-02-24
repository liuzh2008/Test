package com.example.medaiassistant.service;

import com.example.medaiassistant.model.AlertTask;
import com.example.medaiassistant.repository.AlertTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 告警任务服务类
 * 
 * 该服务类负责处理与告警任务相关的业务逻辑。
 * 
 * @author Cline
 * @since 2025-08-06
 */
@Service
public class AlertTaskService {
    
    private final AlertTaskRepository alertTaskRepository;
    
    /**
     * 构造函数
     * 
     * @param alertTaskRepository 告警任务数据访问对象
     */
    public AlertTaskService(AlertTaskRepository alertTaskRepository) {
        this.alertTaskRepository = alertTaskRepository;
    }
    
    /**
     * 根据患者ID获取待处理的告警任务
     * 
     * @param patientId 患者ID
     * @return 待处理的告警任务列表
     */
    public List<AlertTask> getPendingTasksByPatientId(String patientId) {
        return alertTaskRepository.findByPatientIdAndTaskStatus(patientId, AlertTask.TaskStatus.待处理);
    }
    
    /**
     * 更新告警任务状态
     * 
     * 该方法用于更新指定告警任务的状态。通过调用数据访问层方法执行数据库更新操作，
     * 并根据更新结果返回操作是否成功。该方法使用@Transactional注解确保操作的事务性，
     * 如果更新过程中发生异常，事务将回滚，保证数据一致性。
     * 
     * @param taskId 任务ID，用于标识要更新的告警任务，必须是数据库中已存在的任务ID
     * @param taskStatus 新的任务状态，必须是AlertTask.TaskStatus枚举中的有效值
     * @return boolean 更新结果，true表示更新成功，false表示更新失败
     * @since 2025-08-07
     * @author Cline
     * @see AlertTaskRepository#updateTaskStatusByTaskId(Integer, AlertTask.TaskStatus)
     * @see AlertTask.TaskStatus
     */
    @Transactional
    public boolean updateTaskStatus(Integer taskId, AlertTask.TaskStatus taskStatus) {
        int updatedRows = alertTaskRepository.updateTaskStatusByTaskId(taskId, taskStatus);
        return updatedRows > 0;
    }
    
    /**
     * 根据任务ID获取告警任务
     * 
     * @param taskId 任务ID
     * @return 告警任务对象
     */
    public AlertTask getTaskById(Integer taskId) {
        return alertTaskRepository.findById(taskId).orElse(null);
    }
}
