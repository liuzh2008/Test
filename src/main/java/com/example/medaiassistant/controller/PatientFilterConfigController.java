package com.example.medaiassistant.controller;

import com.example.medaiassistant.config.SchedulingProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * 患者过滤配置查询控制器
 * 用于查询实际运行时的科室及床号过滤配置情况
 */
@RestController
@RequestMapping("/api/patient-filter-config")
public class PatientFilterConfigController {

    private final SchedulingProperties schedulingProperties;

    @Autowired
    public PatientFilterConfigController(SchedulingProperties schedulingProperties) {
        this.schedulingProperties = schedulingProperties;
    }

    /**
     * 获取当前患者过滤配置信息
     * 
     * @return 过滤配置详情
     */
    @GetMapping(value = "/current", produces = "application/json;charset=UTF-8")
    public Map<String, Object> getCurrentFilterConfig() {
        Map<String, Object> result = new LinkedHashMap<>();
        
        SchedulingProperties.TimerConfig timerConfig = schedulingProperties.getTimer();
        
        // 基本配置信息
        result.put("success", true);
        result.put("timestamp", new Date());
        
        // 过滤模式
        SchedulingProperties.TimerConfig.FilterMode filterMode = timerConfig.getCurrentFilterMode();
        result.put("filterMode", filterMode.name());
        result.put("filterModeDescription", getFilterModeDescription(filterMode));
        
        // 过滤开关状态
        Map<String, Boolean> filterSwitches = new LinkedHashMap<>();
        filterSwitches.put("departmentFilterEnabled", timerConfig.isDepartmentFilterEnabled());
        filterSwitches.put("bedFilterEnabled", false); // 床号过滤已删除
        result.put("filterSwitches", filterSwitches);
        
        // 科室配置
        if (timerConfig.isDepartmentFilterEnabled()) {
            Map<String, Object> departmentConfig = new LinkedHashMap<>();
            
            // 目标科室列表（仅科室过滤模式使用）
            if (filterMode == SchedulingProperties.TimerConfig.FilterMode.DEPARTMENT_ONLY) {
                departmentConfig.put("targetDepartments", timerConfig.getTargetDepartments());
                departmentConfig.put("targetDepartmentsCount", timerConfig.getTargetDepartments().size());
            }
            
            result.put("departmentConfig", departmentConfig);
        }
        
        // 定时任务配置
        Map<String, Object> schedulingConfig = new LinkedHashMap<>();
        schedulingConfig.put("enabled", timerConfig.isEnabled());
        schedulingConfig.put("dailyTime", timerConfig.getDailyTime());
        schedulingConfig.put("fixedDelayMinutes", timerConfig.getFixedDelayMinutes());
        schedulingConfig.put("maxConcurrency", timerConfig.getMaxConcurrency());
        result.put("schedulingConfig", schedulingConfig);
        
        // 配置来源说明
        result.put("configSource", "application.yml 或 application.properties");
        
        return result;
    }
    
    /**
     * 获取过滤配置摘要
     * 
     * @return 配置摘要信息
     */
    @GetMapping(value = "/summary", produces = "application/json;charset=UTF-8")
    public Map<String, Object> getFilterConfigSummary() {
        Map<String, Object> result = new LinkedHashMap<>();
        
        SchedulingProperties.TimerConfig timerConfig = schedulingProperties.getTimer();
        SchedulingProperties.TimerConfig.FilterMode filterMode = timerConfig.getCurrentFilterMode();
        
        result.put("success", true);
        result.put("timestamp", new Date());
        result.put("filterMode", filterMode.name());
        result.put("filterModeDescription", getFilterModeDescription(filterMode));
        
        // 根据过滤模式生成摘要信息
        switch (filterMode) {
            case NONE:
                result.put("summary", "当前未启用任何过滤，将处理所有在院患者的病危/病重状态");
                result.put("scope", "全部在院患者");
                break;
                
            case DEPARTMENT_ONLY:
                result.put("summary", "当前启用科室过滤，仅处理指定科室的在院患者");
                result.put("scope", "指定科室：" + String.join(", ", timerConfig.getTargetDepartments()));
                result.put("departmentCount", timerConfig.getTargetDepartments().size());
                break;
                
            // DEPARTMENT_AND_BED 模式已删除
        }
        
        return result;
    }
    
    /**
     * 验证当前过滤配置是否有效
     * 
     * @return 验证结果
     */
    @GetMapping(value = "/validate", produces = "application/json;charset=UTF-8")
    public Map<String, Object> validateFilterConfig() {
        Map<String, Object> result = new LinkedHashMap<>();
        
        try {
            SchedulingProperties.TimerConfig timerConfig = schedulingProperties.getTimer();
            
            // 执行配置验证
            timerConfig.validateFilterConfiguration();
            
            result.put("success", true);
            result.put("valid", true);
            result.put("message", "过滤配置验证通过");
            result.put("timestamp", new Date());
            
        } catch (IllegalStateException e) {
            result.put("success", false);
            result.put("valid", false);
            result.put("message", "过滤配置验证失败");
            result.put("error", e.getMessage());
            result.put("timestamp", new Date());
        }
        
        return result;
    }
    
    /**
     * 获取指定科室的床号列表
     * 
     * @param department 科室名称
     * @return 床号列表
     */
    @GetMapping(value = "/department-beds", produces = "application/json;charset=UTF-8")
    public Map<String, Object> getDepartmentBeds(String department) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        result.put("success", false);
        result.put("message", "床号过滤功能已删除");
        result.put("timestamp", new Date());
        
        return result;
    }
    
    /**
     * 获取所有配置的科室列表
     * 
     * @return 科室列表
     */
    @GetMapping(value = "/departments", produces = "application/json;charset=UTF-8")
    public Map<String, Object> getConfiguredDepartments() {
        Map<String, Object> result = new LinkedHashMap<>();
        
        SchedulingProperties.TimerConfig timerConfig = schedulingProperties.getTimer();
        
        result.put("success", true);
        result.put("timestamp", new Date());
        
        if (timerConfig.isDepartmentFilterEnabled()) {
            List<String> departments = timerConfig.getTargetDepartments();
            result.put("departments", departments);
            result.put("departmentCount", departments.size());
            result.put("source", "target-departments 配置");
        } else {
            result.put("departments", Collections.emptyList());
            result.put("departmentCount", 0);
            result.put("source", "未启用科室过滤");
        }
        
        return result;
    }
    
    /**
     * 获取过滤模式描述
     */
    private String getFilterModeDescription(SchedulingProperties.TimerConfig.FilterMode filterMode) {
        switch (filterMode) {
            case NONE:
                return "无过滤 - 处理所有在院患者";
            case DEPARTMENT_ONLY:
                return "仅科室过滤 - 处理指定科室的在院患者";
            default:
                return "未知模式";
        }
    }
}
