package com.example.medaiassistant.hospital.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置验证结果
 * 包含验证是否通过以及详细的错误信息
 */
public class ConfigValidationResult {
    
    private boolean valid;
    private List<String> errors;
    
    public ConfigValidationResult() {
        this.valid = true;
        this.errors = new ArrayList<>();
    }
    
    public ConfigValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors != null ? errors : new ArrayList<>();
    }
    
    /**
     * 创建验证通过的结果
     */
    public static ConfigValidationResult valid() {
        return new ConfigValidationResult(true, new ArrayList<>());
    }
    
    /**
     * 创建验证失败的结果
     */
    public static ConfigValidationResult invalid(String error) {
        ConfigValidationResult result = new ConfigValidationResult(false, new ArrayList<>());
        result.addError(error);
        return result;
    }
    
    /**
     * 添加错误信息
     */
    public void addError(String error) {
        this.valid = false;
        this.errors.add(error);
    }
    
    /**
     * 合并另一个验证结果
     */
    public void merge(ConfigValidationResult other) {
        if (!other.isValid()) {
            this.valid = false;
            this.errors.addAll(other.getErrors());
        }
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public String getErrorMessage() {
        if (errors.isEmpty()) {
            return "配置验证通过";
        }
        return String.join("; ", errors);
    }
    
    @Override
    public String toString() {
        return "ConfigValidationResult{" +
                "valid=" + valid +
                ", errors=" + errors +
                '}';
    }
}
