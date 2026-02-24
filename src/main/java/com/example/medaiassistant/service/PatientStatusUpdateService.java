package com.example.medaiassistant.service;

import com.example.medaiassistant.config.SchedulingProperties;
import com.example.medaiassistant.model.LongTermOrder;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.LongTermOrderRepository;
import com.example.medaiassistant.repository.PatientRepository;
import com.example.medaiassistant.service.filter.PatientFilterStrategy;
import com.example.medaiassistant.service.filter.PatientFilterStrategyFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 患者状态更新服务类
 * 
 * 该服务类负责定时检查和更新患者的状态，包括：
 * 1. 查询长期医嘱中有未停止的"病危"或"病重"，更改病人状态为"病危"或"病重"
 * 2. 查询病人状态"病危"或"病重"，如果医嘱已停止，则更改病人状态为"普通"
 * 
 * 支持按科室+床位过滤模式运行：
 * - 无过滤模式：处理所有在院患者
 * - 仅科室过滤：仅处理指定科室的在院患者
 * - 科室+床位过滤：仅处理指定科室指定床位的在院患者
 * 
 * @author Cline
 * @since 2025-08-10
 * @version 3.0.0 - 添加科室+床位过滤支持
 */
@Service
public class PatientStatusUpdateService {
    
    /**
     * 患者数据访问对象，用于查询和更新患者信息
     */
    private final PatientRepository patientRepository;
    
    /**
     * 长期医嘱数据访问对象，用于查询患者医嘱信息
     */
    private final LongTermOrderRepository longTermOrderRepository;
    
    /**
     * 调度配置属性，用于获取科室和床位过滤配置
     */
    private final SchedulingProperties schedulingProperties;
    
    /**
     * 患者过滤策略工厂，用于根据配置选择合适的过滤策略
     */
    private final PatientFilterStrategyFactory patientFilterStrategyFactory;
    
    /**
     * 构造函数，通过依赖注入初始化数据访问对象和过滤策略
     * 
     * @param patientRepository 患者数据访问对象
     * @param longTermOrderRepository 长期医嘱数据访问对象
     * @param schedulingProperties 调度配置属性
     * @param patientFilterStrategyFactory 患者过滤策略工厂
     */
    public PatientStatusUpdateService(PatientRepository patientRepository, 
                                   LongTermOrderRepository longTermOrderRepository,
                                   SchedulingProperties schedulingProperties,
                                   PatientFilterStrategyFactory patientFilterStrategyFactory) {
        this.patientRepository = patientRepository;
        this.longTermOrderRepository = longTermOrderRepository;
        this.schedulingProperties = schedulingProperties;
        this.patientFilterStrategyFactory = patientFilterStrategyFactory;
    }
    
    /**
     * 更新所有患者的状态（支持科室+床位过滤）
     * 
     * 该方法会执行以下操作：
     * 1. 根据配置的过滤模式（无过滤/仅科室/科室+床位）查询符合条件的在院患者
     * 2. 对每个患者检查是否有未停止的"病危"或"病重"医嘱，如果有则更新患者状态
     * 3. 对状态为"病危"或"病重"的患者检查其医嘱是否已停止，如果已停止则更新为"普通"
     * 
     * 过滤模式说明：
     * - NONE: 处理所有在院患者
     * - DEPARTMENT_ONLY: 仅处理配置的目标科室的在院患者
     * - DEPARTMENT_AND_BED: 仅处理配置的科室-床位映射中的在院患者
     * 
     * @function updateAllPatientStatus
     * @description 批量更新符合过滤条件的在院患者状态，使用事务确保数据一致性
     * @returns {void}
     * @throws {Exception} 当更新过程中发生异常时抛出，事务将自动回滚
     * @transactional 启用事务管理，确保批量操作要么全部成功，要么全部回滚
     * @since 2025-08-26
     * @version 3.0.0 - 添加科室+床位过滤支持
     */
    @Transactional(transactionManager = "transactionManager")
    public void updateAllPatientStatus() {
        // 获取定时任务配置
        SchedulingProperties.TimerConfig timerConfig = schedulingProperties.getTimer();
        
        // 获取当前过滤模式
        SchedulingProperties.TimerConfig.FilterMode filterMode = timerConfig.getCurrentFilterMode();
        
        System.out.println("患者状态更新任务启动 - 当前过滤模式: " + filterMode);
        
        // 根据过滤模式获取合适的过滤策略
        PatientFilterStrategy filterStrategy = patientFilterStrategyFactory.getStrategy(filterMode);
        
        System.out.println("使用过滤策略: " + filterStrategy.getStrategyName());
        
        // 打印过滤配置详情
        if (filterMode == SchedulingProperties.TimerConfig.FilterMode.DEPARTMENT_ONLY) {
            System.out.println("目标科室: " + timerConfig.getTargetDepartments());
        }
        
        // 使用分页查询避免一次性加载大量数据
        int pageSize = 100;
        int pageNumber = 0;
        List<Patient> allPatients = new ArrayList<>();
        
        // 分页查询所有符合条件的患者
        while (true) {
            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            List<Patient> pagePatients = filterStrategy.filterPatients(patientRepository, timerConfig, pageable);
            
            if (pagePatients.isEmpty()) {
                break;
            }
            
            allPatients.addAll(pagePatients);
            
            // 如果返回的数据少于页面大小，说明已经是最后一页
            if (pagePatients.size() < pageSize) {
                break;
            }
            
            pageNumber++;
        }
        
        System.out.println("找到 " + allPatients.size() + " 个符合过滤条件的在院患者");
        
        // 对每个符合条件的患者检查并更新状态
        for (Patient patient : allPatients) {
            System.out.println("处理患者: " + patient.getPatientId() + 
                             ", 科室: " + patient.getDepartment() + 
                             ", 床号: " + patient.getBedNumber() + 
                             ", 当前状态: " + patient.getStatus());
            updatePatientStatus(patient);
        }
        
        System.out.println("患者状态更新任务完成，共处理 " + allPatients.size() + " 个患者");
    }
    
    /**
     * 更新单个患者的状态
     * 
     * 该方法会执行以下操作：
     * 1. 查询患者是否有未停止的"病危"或"病重"医嘱，如果有则更新患者状态为相应的状态
     * 2. 如果患者当前状态为"病危"或"病重"，但没有未停止的相应医嘱，则将状态更新为"普通"
     * 
     * @param patient 患者对象
     */
    private void updatePatientStatus(Patient patient) {
        String patientId = patient.getPatientId();
        
        // 1. 查询长期医嘱中有未停止的"病危"或"病重"
        List<LongTermOrder> activeCriticalOrders = longTermOrderRepository
            .findActiveCriticalOrSeriousOrdersByPatientId(patientId);
        
        System.out.println("患者 " + patientId + " 有 " + activeCriticalOrders.size() + " 个未停止的病危/病重医嘱");
        
        // 如果有未停止的"病危"或"病重"医嘱，则更新患者状态
        if (!activeCriticalOrders.isEmpty()) {
            // 确定是"病危"还是"病重"
            String status = determineCriticalStatus(activeCriticalOrders);
            System.out.println("确定患者状态应为: " + status);
            if (!status.equals(patient.getStatus())) {
                int updatedRows = patientRepository.updateStatusByPatientId(patientId, status);
                System.out.println("患者 " + patientId + " 状态已更新为: " + status + " (更新了 " + updatedRows + " 行)");
            } else {
                System.out.println("患者 " + patientId + " 状态已经是: " + status + "，无需更新");
            }
        } else {
            // 2. 查询病人状态"病危"或"病重"，如果医嘱已停止，则更改病人状态为"普通"
            if ("病危".equals(patient.getStatus()) || "病重".equals(patient.getStatus())) {
                System.out.println("患者 " + patientId + " 当前状态为 " + patient.getStatus() + "，检查是否有已停止的病危/病重医嘱");
                // 检查是否有已停止的"病危"或"病重"医嘱
                List<LongTermOrder> stoppedCriticalOrders = longTermOrderRepository
                    .findStoppedCriticalOrSeriousOrdersByPatientId(patientId);
                
                System.out.println("患者 " + patientId + " 有 " + stoppedCriticalOrders.size() + " 个已停止的病危/病重医嘱");
                
                // 如果有已停止的"病危"或"病重"医嘱，则将患者状态更新为"普通"
                if (!stoppedCriticalOrders.isEmpty()) {
                    int updatedRows = patientRepository.updateStatusByPatientId(patientId, "普通");
                    System.out.println("患者 " + patientId + " 状态已从 " + patient.getStatus() + " 更新为: 普通 (更新了 " + updatedRows + " 行)");
                }
            } else {
                System.out.println("患者 " + patientId + " 当前状态为 " + patient.getStatus() + "，无需处理");
            }
        }
    }
    
    /**
     * 根据医嘱确定患者状态（病危或病重）
     * 
     * 该方法会遍历医嘱列表，优先检查是否有"病危"医嘱，如果有则返回"病危"；
     * 如果没有"病危"医嘱但有"病重"医嘱，则返回"病重"。
     * 
     * @param orders 医嘱列表
     * @return 患者状态（"病危"或"病重"）
     */
    private String determineCriticalStatus(List<LongTermOrder> orders) {
        for (LongTermOrder order : orders) {
            if (order.getOrderName().contains("病危")) {
                return "病危";
            }
        }
        // 如果没有找到"病危"，则返回"病重"
        return "病重";
    }
    
    /**
     * 根据传入的参数更新患者状态
     *
     * 智能处理逻辑：
     * 1. 如果患者当前状态为空，直接设置为新状态
     * 2. 如果新状态与当前状态相同，返回提示信息
     * 3. 如果新状态有"普通"或"病重"或"病危"，则用新状态中的"普通"或"病重"或"病危"替换原状态中的"普通"或"病重"或"病危"
     * 4. 如果新状态有"入院"或"出院"，则用新状态中的"入院"或"出院"替换原状态中的"入院"或"出院"
     *
     * @param patientId 患者ID
     * @param newStatus 新的状态值
     * @return 更新结果消息
     */
    public String updatePatientStatusByParam(String patientId, String newStatus) {
        // 获取患者当前信息
        Patient patient = patientRepository.findByPatientId(patientId);
        if (patient == null) {
            return "未找到患者，patientId: " + patientId;
        }
        
        // 获取患者当前状态
        String currentStatus = patient.getStatus();
        if (currentStatus == null) {
            currentStatus = "";
        }
        
        // 如果患者当前状态为空，直接设置为新状态
        if (currentStatus.isEmpty()) {
            int updatedRows = patientRepository.updateStatusByPatientId(patientId, newStatus);
            if (updatedRows > 0) {
                return "患者状态更新成功，从 '' 更新为 '" + newStatus + "'";
            } else {
                return "患者状态更新失败";
            }
        }
        
        // 如果新状态与当前状态相同，返回提示信息
        if (currentStatus.equals(newStatus)) {
            return "患者状态已经是 '" + newStatus + "'，无需更新";
        }
        
        String updatedStatus = currentStatus;
        
        // 如果新状态有"普通"或"病重"或"病危"，则用新状态中的"普通"或"病重"或"病危"替换原状态中的"普通"或"病重"或"病危"
        if (newStatus.contains("普通") || newStatus.contains("病重") || newStatus.contains("病危")) {
            // 移除原状态中的"普通"、"病重"、"病危"
            updatedStatus = updatedStatus.replaceAll("(普通|病重|病危)", "");
            // 清理多余的逗号
            updatedStatus = updatedStatus.replaceAll("，+", "，");
            updatedStatus = updatedStatus.replaceAll("^，|，$", "");
            
            // 添加新状态中的"普通"、"病重"、"病危"
            if (newStatus.contains("普通")) {
                if (!updatedStatus.isEmpty()) {
                    updatedStatus = "普通，" + updatedStatus;
                } else {
                    updatedStatus = "普通";
                }
            } else if (newStatus.contains("病重")) {
                if (!updatedStatus.isEmpty()) {
                    updatedStatus = "病重，" + updatedStatus;
                } else {
                    updatedStatus = "病重";
                }
            } else if (newStatus.contains("病危")) {
                if (!updatedStatus.isEmpty()) {
                    updatedStatus = "病危，" + updatedStatus;
                } else {
                    updatedStatus = "病危";
                }
            }
        }
        
        // 如果新状态有"入院"或"出院"，则用新状态中的"入院"或"出院"替换原状态中的"入院"或"出院"
        if (newStatus.contains("入院") || newStatus.contains("出院")) {
            // 移除原状态中的"入院"、"出院"
            updatedStatus = updatedStatus.replaceAll("(入院|出院)([^，]*)?", "");
            // 清理多余的逗号
            updatedStatus = updatedStatus.replaceAll("，+", "，");
            updatedStatus = updatedStatus.replaceAll("^，|，$", "");
            
            // 添加新状态中的"入院"、"出院"
            if (newStatus.contains("入院")) {
                // 提取入院相关的内容
                String admissionStatus = newStatus.replaceAll(".*(入院[^，]*).*", "$1");
                if (admissionStatus.equals(newStatus)) {
                    admissionStatus = newStatus;
                }
                if (!updatedStatus.isEmpty()) {
                    updatedStatus = admissionStatus + "，" + updatedStatus;
                } else {
                    updatedStatus = admissionStatus;
                }
            } else if (newStatus.contains("出院")) {
                // 提取出院相关的内容
                String dischargeStatus = newStatus.replaceAll(".*(出院[^，]*).*", "$1");
                if (dischargeStatus.equals(newStatus)) {
                    dischargeStatus = newStatus;
                }
                if (!updatedStatus.isEmpty()) {
                    updatedStatus = dischargeStatus + "，" + updatedStatus;
                } else {
                    updatedStatus = dischargeStatus;
                }
            }
        }
        
        // 更新患者状态
        int updatedRows = patientRepository.updateStatusByPatientId(patientId, updatedStatus);
        if (updatedRows > 0) {
            return "患者状态更新成功，从 '" + currentStatus + "' 更新为 '" + updatedStatus + "'";
        } else {
            return "患者状态更新失败";
        }
    }
}
