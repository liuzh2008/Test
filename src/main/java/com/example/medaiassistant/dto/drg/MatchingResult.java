package com.example.medaiassistant.dto.drg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 匹配结果数据传输对象
 * 
 * 用于封装DRG匹配的结果，包含主要诊断和主要手术列表
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-21
 */
public class MatchingResult {
    
    /**
     * 主要诊断名称列表
     */
    private final List<String> primaryDiagnoses;
    
    /**
     * 主要手术名称列表
     */
    private final List<String> primaryProcedures;
    
    /**
     * 构造函数
     * 
     * @param primaryDiagnoses 主要诊断列表，如果为null则创建空列表
     * @param primaryProcedures 主要手术列表，如果为null则创建空列表
     */
    public MatchingResult(List<String> primaryDiagnoses, List<String> primaryProcedures) {
        this.primaryDiagnoses = primaryDiagnoses != null ? new ArrayList<>(primaryDiagnoses) : new ArrayList<>();
        this.primaryProcedures = primaryProcedures != null ? new ArrayList<>(primaryProcedures) : new ArrayList<>();
    }
    
    /**
     * 获取主要诊断名称列表
     * 
     * @return 主要诊断名称列表
     */
    @JsonProperty("primaryDiagnoses")
    public List<String> getPrimaryDiagnoses() {
        return Collections.unmodifiableList(primaryDiagnoses);
    }
    
    /**
     * 获取主要手术名称列表
     * 
     * @return 主要手术名称列表
     */
    @JsonProperty("primaryProcedures")
    public List<String> getPrimaryProcedures() {
        return Collections.unmodifiableList(primaryProcedures);
    }
    
    /**
     * 判断是否有主要诊断
     * 
     * @return 如果有主要诊断返回true，否则返回false
     */
    @JsonIgnore
    public boolean hasPrimaryDiagnoses() {
        return !primaryDiagnoses.isEmpty();
    }
    
    /**
     * 判断是否有主要手术
     * 
     * @return 如果有主要手术返回true，否则返回false
     */
    @JsonIgnore
    public boolean hasPrimaryProcedures() {
        return !primaryProcedures.isEmpty();
    }
    
    /**
     * 获取主要诊断数量
     * 
     * @return 主要诊断数量
     */
    @JsonIgnore
    public int getPrimaryDiagnosisCount() {
        return primaryDiagnoses.size();
    }
    
    /**
     * 获取主要手术数量
     * 
     * @return 主要手术数量
     */
    @JsonIgnore
    public int getPrimaryProcedureCount() {
        return primaryProcedures.size();
    }
    
    @Override
    public String toString() {
        return "MatchingResult{" +
                "primaryDiagnoses=" + primaryDiagnoses +
                ", primaryProcedures=" + primaryProcedures +
                '}';
    }
}
