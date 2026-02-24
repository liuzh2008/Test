package com.example.medaiassistant.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 科室过滤配置验证器
 * 用于验证科室过滤配置的合法性
 */
@Component
public class DepartmentFilterConfigValidator {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentFilterConfigValidator.class);

    /**
     * 验证科室过滤配置
     *
     * @param departmentFilterEnabled 是否启用科室过滤
     * @param targetDepartments 目标科室列表
     * @return 验证结果
     */
    public ValidationResult validateDepartmentFilterConfig(boolean departmentFilterEnabled, List<String> targetDepartments) {
        ValidationResult result = new ValidationResult();
        
        // 如果启用了科室过滤，检查目标科室列表
        if (departmentFilterEnabled) {
            if (targetDepartments == null || targetDepartments.isEmpty()) {
                result.setValid(false);
                result.setMessage("启用了科室过滤，但目标科室列表为空。定时任务将不会过滤任何科室。");
                logger.warn("科室过滤配置警告: {}", result.getMessage());
            } else {
                result.setValid(true);
                result.setMessage(String.format("科室过滤配置有效 - 启用过滤，目标科室: %s", targetDepartments));
                logger.info("科室过滤配置验证通过: {}", result.getMessage());
            }
        } else {
            result.setValid(true);
            result.setMessage("科室过滤已禁用，定时任务将处理所有科室");
            logger.info("科室过滤配置验证通过: {}", result.getMessage());
        }
        
        return result;
    }

    /**
     * 检查目标科室列表是否包含指定科室
     *
     * @param targetDepartments 目标科室列表
     * @param department 要检查的科室
     * @return 如果包含则返回true
     */
    public boolean isDepartmentInTargetList(List<String> targetDepartments, String department) {
        if (targetDepartments == null || targetDepartments.isEmpty() || department == null) {
            return false;
        }
        
        return targetDepartments.stream()
                .anyMatch(target -> target.equalsIgnoreCase(department.trim()));
    }

    /**
     * 验证单个科室名称的合法性
     *
     * @param department 科室名称
     * @return 验证结果
     */
    public ValidationResult validateDepartmentName(String department) {
        ValidationResult result = new ValidationResult();
        
        if (department == null || department.trim().isEmpty()) {
            result.setValid(false);
            result.setMessage("科室名称不能为空");
            return result;
        }
        
        String trimmedDepartment = department.trim();
        
        // 检查科室名称长度
        if (trimmedDepartment.length() > 40) {
            result.setValid(false);
            result.setMessage("科室名称长度不能超过40个字符");
            return result;
        }
        
        // 检查科室名称是否包含非法字符
        if (trimmedDepartment.contains(";") || trimmedDepartment.contains("|") || trimmedDepartment.contains("\"")) {
            result.setValid(false);
            result.setMessage("科室名称包含非法字符");
            return result;
        }
        
        result.setValid(true);
        result.setMessage("科室名称验证通过");
        return result;
    }

    /**
     * 验证配置结果类
     */
    public static class ValidationResult {
        private boolean valid;
        private String message;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
