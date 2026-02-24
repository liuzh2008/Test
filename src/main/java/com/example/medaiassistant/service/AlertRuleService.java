package com.example.medaiassistant.service;

import com.example.medaiassistant.dto.AlertRuleContentDTO;
import com.example.medaiassistant.model.AlertRule;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.model.AlertTask;
import com.example.medaiassistant.repository.AlertRuleRepository;
import com.example.medaiassistant.repository.PatientRepository;
import com.example.medaiassistant.repository.PromptRepository;
import com.example.medaiassistant.repository.AlertTaskRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 告警规则服务类
 * 
 * 该服务类负责处理与告警规则相关的业务逻辑，包括获取激活的时间规则、
 * 处理激活的规则以及处理符合条件的患者数据。
 * 
 * @author Cline
 * @since 2025-08-06
 */
@Service
public class AlertRuleService {
    private final AlertRuleRepository alertRuleRepository;
    private final PatientRepository patientRepository;
    private final PromptRepository promptRepository;
    private final AlertTaskRepository alertTaskRepository;

    /**
     * 构造函数
     * 
     * @param alertRuleRepository 告警规则数据访问对象
     * @param patientRepository 患者数据访问对象
     * @param promptRepository Prompt数据访问对象
     * @param alertTaskRepository 告警任务数据访问对象
     */
    public AlertRuleService(AlertRuleRepository alertRuleRepository, PatientRepository patientRepository, PromptRepository promptRepository, AlertTaskRepository alertTaskRepository) {
        this.alertRuleRepository = alertRuleRepository;
        this.patientRepository = patientRepository;
        this.promptRepository = promptRepository;
        this.alertTaskRepository = alertTaskRepository;
    }

    /**
     * 获取所有激活的时间规则并按优先级排序
     * 
     * @return 激活的时间规则列表，按优先级升序排列
     */
    public List<AlertRule> getActiveTimeRulesOrderByPriority() {
        return alertRuleRepository.findActiveTimeRulesOrderByPriority();
    }
    
    /**
     * 获取所有激活的状态规则并按优先级排序
     * 
     * @return 激活的状态规则列表，按优先级升序排列
     */
    public List<AlertRule> getActiveStatusRulesOrderByPriority() {
        return alertRuleRepository.findActiveStatusRulesOrderByPriority();
    }
    
    /**
     * 根据规则名称获取激活的告警规则内容信息
     * 
     * @param ruleName 规则名称
     * @return 符合条件的告警规则内容信息列表
     */
    public List<AlertRuleContentDTO> getActiveRuleContentByName(String ruleName) {
        return alertRuleRepository.findActiveRuleContentByName(ruleName);
    }

    /**
     * 处理所有激活的时间规则
     * 
     * 该方法会遍历所有激活的时间规则，解析规则中的触发条件，
     * 计算截止时间，并查询符合条件的患者数据进行处理。
     * 
     * 主要处理逻辑：
     * 1. 获取所有激活的时间规则（按优先级排序）
     * 2. 遍历每个规则，解析触发条件
     * 3. 如果规则包含offset_hours条件且不包含cycle条件，则计算截止时间
     * 4. 根据截止时间和科室查询符合条件的患者
     * 5. 处理查询到的患者数据
     * 
     * @function processActiveTimeRules
     * @description 批量处理激活的时间告警规则，使用事务确保数据一致性
     * @returns {void}
     * @throws {Exception} 当处理过程中发生异常时抛出，事务将自动回滚
     * @transactional 启用事务管理，确保批量操作要么全部成功，要么全部回滚
     * @since 2025-08-26
     * @version 2.0.0 - 添加事务支持，修复数据一致性问题
     * @version 2.2.0 - 重构：提取JSON解析逻辑，提高代码可读性
     */
    @Transactional(transactionManager = "transactionManager")
    public void processActiveTimeRules() {
        // 获取所有激活的时间规则并按优先级排序
        List<AlertRule> activeRules = getActiveTimeRulesOrderByPriority();
        
        // 遍历所有激活的规则
        for (AlertRule rule : activeRules) {
            try {
                // 解析触发条件JSON
                Map<String, Object> conditions = parseTriggerConditions(rule.getTriggerConditions());
                
                // 检查是否包含offset_hours条件
                if (conditions != null && conditions.containsKey("offset_hours")) {
                    Integer offsetHours = (Integer) conditions.get("offset_hours");
                    Object cycle = conditions.get("cycle");
                    
                    // 只处理包含offset_hours但不包含cycle的规则
                    if (offsetHours != null && cycle == null) {
                        // 使用现代时间API计算截止时间，避免GregorianCalendar问题
                        LocalDateTime cutoffDateTime = LocalDateTime.now().minusHours(offsetHours);
                        Date cutoffTime = Date.from(cutoffDateTime.atZone(ZoneId.systemDefault()).toInstant());
                        
                        // 查询符合条件的患者数据
                        List<Patient> patients = patientRepository.findByDepartmentAndAdmissionTimeBeforeOffset(
                            "心血管一病区", 
                            true,
                            offsetHours,
                            cutoffTime
                        );
                        // 处理获取到的病人列表
                        processPatients(patients, rule);
                    }
                }
            } catch (Exception e) {
                // 处理JSON解析异常
                handleJsonParseException(e, rule);
            }
        }
    }

    /**
     * 处理所有激活的状态规则
     * 
     * 该方法会遍历所有激活的状态规则，解析规则中的触发条件，
     * 根据患者状态查询符合条件的患者数据进行处理。
     * 
     * 主要处理逻辑：
     * 1. 获取所有激活的状态规则（按优先级排序）
     * 2. 遍历每个规则，解析触发条件
     * 3. 如果规则类型为状态，则根据patient_status查询相应状态的患者
     * 4. 处理查询到的患者数据，生成查房提醒任务
     * 
     * 规则触发条件格式：
     * {
     *   "type": "状态",
     *   "cycle": "24h", // 或 "48h"
     *   "fixed_time": "08:00",
     *   "patient_status": "病危" // 或 "病重" 等状态，也可以是数组形式 ["病危", "病重"]
     * }
     * 
     * @function processActiveStatusRules
     * @description 批量处理激活的状态告警规则，使用事务确保数据一致性
     * @returns {void}
     * @throws {Exception} 当处理过程中发生异常时抛出，事务将自动回滚
     * @transactional 启用事务管理，确保批量操作要么全部成功，要么全部回滚
     * @since 2025-08-26
     * @version 2.0.0 - 添加事务支持，修复数据一致性问题
     * @version 2.2.0 - 重构：提取JSON解析逻辑，提高代码可读性
     */
    @Transactional(transactionManager = "transactionManager")
    public void processActiveStatusRules() {
        // 获取所有激活的状态规则并按优先级排序
        List<AlertRule> activeRules = getActiveStatusRulesOrderByPriority();
        
        // 遍历所有激活的规则
        for (AlertRule rule : activeRules) {
            try {
                // 解析触发条件JSON
                Map<String, Object> conditions = parseTriggerConditions(rule.getTriggerConditions());
                
                // 检查规则类型是否为状态
                if (conditions != null && "状态".equals(conditions.get("type"))) {
                    Object patientStatusObj = conditions.get("patient_status");
                    
                    // 处理patient_status为字符串或数组的情况
                    if (patientStatusObj instanceof String) {
                        String patientStatus = (String) patientStatusObj;
                        // 如果patient_status不为空，则查询相应状态的患者
                        if (patientStatus != null && !patientStatus.isEmpty()) {
                            // 查询指定状态且在院的患者
                            List<Patient> patients = patientRepository.findByStatusAndIsInHospital(patientStatus, true);
                            
                            // 处理获取到的病人列表
                            processCriticalPatients(patients, rule, conditions);
                        }
                    } else if (patientStatusObj instanceof List) {
                        // 处理patient_status为数组的情况
                        @SuppressWarnings("unchecked")
                        List<String> patientStatusList = (List<String>) patientStatusObj;
                        
                        // 遍历所有状态
                        for (String patientStatus : patientStatusList) {
                            if (patientStatus != null && !patientStatus.isEmpty()) {
                                // 查询指定状态且在院的患者
                                List<Patient> patients = patientRepository.findByStatusAndIsInHospital(patientStatus, true);
                                
                                // 处理获取到的病人列表
                                processCriticalPatients(patients, rule, conditions);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 处理JSON解析异常
                handleJsonParseException(e, rule);
            }
        }
    }

    /**
     * 处理病危患者数据
     * 
     * 该方法会遍历病危患者列表，为每个患者生成首次查房任务，
     * 并根据cycle设置下次任务时间。在创建任务时，会将告警规则中的
     * alert_content和required_actions信息合并到task_content字段中。
     * 
     * @function processCriticalPatients
     * @description 处理病危患者数据并创建告警任务
     * @param {List<Patient>} patients - 病危患者列表
     * @param {AlertRule} rule - 相关联的告警规则对象
     * @param {Map<String, Object>} conditions - 规则的触发条件映射
     * @returns {void}
     * @since 2025-08-26
     * @version 2.0.0 - 添加事务支持和数据一致性保证
     * @version 2.1.0 - 在task_content中添加required_actions字段
     */
    private void processCriticalPatients(List<Patient> patients, AlertRule rule, Map<String, Object> conditions) {
        String cycle = (String) conditions.get("cycle");
        String fixedTime = (String) conditions.get("fixed_time");
        
        // 计算下次任务时间
        int daysToAdd = "24h".equals(cycle) ? 1 : 2; // 24h为1天，48h为2天
        
        // 解析固定时间，默认为08:00
        String[] timeParts = (fixedTime != null ? fixedTime : "08:00").split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        
        // 遍历患者列表
        for (Patient patient : patients) {
            // 检查是否已经存在相同的待处理任务，避免重复创建
            List<AlertTask> existingTasks = alertTaskRepository.findByRuleIdAndPatientIdAndTaskStatus(
                rule.getRuleId(), patient.getPatientId(), AlertTask.TaskStatus.待处理);
            
            if (existingTasks.isEmpty()) {
                // 创建AlertTask对象
                AlertTask alertTask = new AlertTask();
                alertTask.setRuleId(rule.getRuleId());
                alertTask.setPatientId(patient.getPatientId());
                alertTask.setTaskType(rule.getRuleName());
                
                // 创建task_content JSON内容，包含告警内容和必需操作
                String taskContent = "{\"alert_content\":\"" + rule.getAlertContent() + 
                                    "\",\"required_actions\":" + rule.getRequiredActions() + "}";
                alertTask.setTaskContent(taskContent);
                
                // 设置assigneeRoles为rule的targetRoles，确保任务分配给正确的角色
                alertTask.setAssigneeRoles(rule.getTargetRoles());
                
                alertTask.setTaskStatus(AlertTask.TaskStatus.待处理);
                alertTask.setCreatedTime(LocalDateTime.now());
                
                // 设置下次任务时间
                LocalDateTime nextTaskTime = LocalDateTime.now().plusDays(daysToAdd).withHour(hour).withMinute(minute).withSecond(0).withNano(0);
                alertTask.setNextTaskTime(nextTaskTime);
                
                // 保存到数据库
                alertTaskRepository.save(alertTask);
                
                System.out.println("病人ID: " + patient.getPatientId() + 
                                 ", 姓名: " + patient.getName() + 
                                 ", 状态: " + patient.getStatus() +
                                 ", 已创建查房提醒任务，下次任务时间: " + nextTaskTime);
            } else {
                // 如果已存在相同的待处理任务，则不创建新任务
                System.out.println("病人ID: " + patient.getPatientId() + 
                                 ", 姓名: " + patient.getName() + 
                                 ", 状态: " + patient.getStatus() +
                                 ", 已存在相同的待处理任务");
            }
        }
    }

    /**
     * 处理符合条件的患者数据
     * 
     * 该方法会遍历患者列表，在控制台输出每个患者的ID号和入院时间。
     * 同时检查规则条件中的not_exist条件，如果患者没有相应的prompt记录，则添加alert_tasks记录。
     * 在创建任务时，会将告警规则中的alert_content、notExistCondition和required_actions
     * 信息合并到task_content字段中，为任务处理提供完整的上下文信息。
     * 
     * @function processPatients
     * @description 处理符合条件的患者数据并创建告警任务
     * @param {List<Patient>} patients - 符合条件的患者列表
     * @param {AlertRule} rule - 相关联的告警规则对象
     * @returns {void}
     * @since 2025-08-26
     * @version 2.0.0 - 添加事务支持和数据一致性保证
     * @version 2.1.0 - 在task_content中添加required_actions字段
     * @version 2.2.0 - 重构：提取JSON解析逻辑，提高代码可读性
     */
    private void processPatients(List<Patient> patients, AlertRule rule) {
        try {
            // 解析触发条件JSON
            Map<String, Object> conditions = parseTriggerConditions(rule.getTriggerConditions());
            
            // 检查是否有not_exist条件
            if (conditions != null && conditions.containsKey("not_exist")) {
                String notExistCondition = (String) conditions.get("not_exist");

                // 遍历患者列表
                for (Patient patient : patients) {
                    // 查询该患者是否有相应的prompt记录
                    List<Prompt> prompts = promptRepository.findByPatientIdAndPromptTemplateName(
                        patient.getPatientId(), 
                        notExistCondition
                    );
                    
                    // 如果没有相应的prompt记录，则添加alert_tasks记录
                    if (prompts.isEmpty()) {
                        // 检查是否已经存在相同的待处理任务，避免重复创建
                        List<AlertTask> existingTasks = alertTaskRepository.findByRuleIdAndPatientIdAndTaskStatus(
                            rule.getRuleId(), patient.getPatientId(), AlertTask.TaskStatus.待处理);
                        
                        if (existingTasks.isEmpty()) {
                            // 创建AlertTask对象
                            AlertTask alertTask = new AlertTask();
                            alertTask.setRuleId(rule.getRuleId());
                            alertTask.setPatientId(patient.getPatientId());
                            // 使用notExistCondition作为任务类型，替代固定的"缺失记录提醒"字符串
                            alertTask.setTaskType(notExistCondition);
                            
                            // 创建task_content JSON内容，包含告警内容、缺失条件和必需操作
                            String taskContent = "{\"alert_content\":\"" + rule.getAlertContent() + 
                                                "\",\"notExistCondition\":\"" + notExistCondition + 
                                                "\",\"required_actions\":" + rule.getRequiredActions() + "}";
                            alertTask.setTaskContent(taskContent);
                            
                            // 设置assigneeRoles为rule的targetRoles，确保任务分配给正确的角色
                            alertTask.setAssigneeRoles(rule.getTargetRoles());
                            
                            alertTask.setTaskStatus(AlertTask.TaskStatus.待处理);
                            alertTask.setCreatedTime(LocalDateTime.now());
                            
                            // 保存到数据库
                            alertTaskRepository.save(alertTask);
                            
                            System.out.println("病人ID: " + patient.getPatientId() + 
                                             ", 入院时间: " + patient.getAdmissionTime() + 
                                             ", 缺少记录: " + notExistCondition + 
                                             ", 已创建告警任务");
                        } else {
                            // 如果已存在相同的待处理任务，则不创建新任务
                            System.out.println("病人ID: " + patient.getPatientId() + 
                                             ", 入院时间: " + patient.getAdmissionTime() + 
                                             ", 缺少记录: " + notExistCondition + 
                                             ", 已存在相同的待处理任务");
                        }
                    }
                }
            } else {
                // 如果没有not_exist条件，则按原来的方式处理
                for (Patient patient : patients) {
                    System.out.println("病人ID: " + patient.getPatientId() + ", 入院时间: " + patient.getAdmissionTime());
                }
            }
        } catch (Exception e) {
            // 处理JSON解析异常
            handleJsonParseException(e, rule);
            // 如果解析失败，则按原来的方式处理
            for (Patient patient : patients) {
                System.out.println("病人ID: " + patient.getPatientId() + ", 入院时间: " + patient.getAdmissionTime());
            }
        }
    }

    /**
     * 解析触发条件JSON字符串
     * 
     * @param triggerConditions JSON格式的触发条件字符串
     * @return 解析后的触发条件映射，如果解析失败则返回null
     */
    private Map<String, Object> parseTriggerConditions(String triggerConditions) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(
                triggerConditions, 
                new TypeReference<Map<String, Object>>() {}
            );
        } catch (Exception e) {
            throw new RuntimeException("解析触发条件JSON失败: " + triggerConditions, e);
        }
    }

    /**
     * 处理JSON解析异常
     * 
     * @param e 异常对象
     * @param rule 相关的告警规则
     */
    private void handleJsonParseException(Exception e, AlertRule rule) {
        System.err.println("处理告警规则时发生JSON解析异常，规则ID: " + rule.getRuleId() + 
                         ", 规则名称: " + rule.getRuleName() + 
                         ", 错误信息: " + e.getMessage());
        e.printStackTrace();
    }
}
