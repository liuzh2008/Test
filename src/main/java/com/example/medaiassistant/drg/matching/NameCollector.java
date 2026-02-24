package com.example.medaiassistant.drg.matching;

import com.example.medaiassistant.dto.drg.DiagnosisEntry;
import com.example.medaiassistant.dto.drg.DrgParsedRecord;
import com.example.medaiassistant.dto.drg.ProcedureEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 名称收集器
 * 
 * 负责收集并聚合匹配成功的诊断和手术名称，支持自动去重和Top-K限制
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-22
 */
public class NameCollector {
    
    /**
     * 主要诊断名称集合（用于去重）
     */
    private final Set<String> primaryDiagnosisSet;
    
    /**
     * 主要手术名称集合（用于去重）
     */
    private final Set<String> primaryProcedureSet;
    
    /**
     * 是否启用Top-K限制
     */
    private final boolean topKEnabled;
    
    /**
     * Top-K限制数量
     */
    private final int topKLimit;
    
    /**
     * 默认构造函数，禁用Top-K限制
     */
    public NameCollector() {
        this(false, 0);
    }
    
    /**
     * 构造函数
     * 
     * @param topKEnabled 是否启用Top-K限制
     * @param topKLimit Top-K限制数量，仅在启用时有效
     */
    public NameCollector(boolean topKEnabled, int topKLimit) {
        this.primaryDiagnosisSet = new HashSet<>();
        this.primaryProcedureSet = new HashSet<>();
        this.topKEnabled = topKEnabled;
        this.topKLimit = topKEnabled ? Math.max(0, topKLimit) : 0;
    }
    
    /**
     * 收集DRG记录中的诊断和手术名称
     * 
     * @param drgRecord DRG解析记录
     * @param collectDiagnoses 是否收集诊断名称
     * @param collectProcedures 是否收集手术名称
     */
    public void collectDrgNames(DrgParsedRecord drgRecord, boolean collectDiagnoses, boolean collectProcedures) {
        if (drgRecord == null) {
            return;
        }
        
        if (collectDiagnoses) {
            collectDiagnosisNames(drgRecord);
        }
        
        if (collectProcedures) {
            collectProcedureNames(drgRecord);
        }
    }
    
    /**
     * 收集诊断名称
     * 
     * 根据迭代方案要求，只收集主要诊断名称，不收集别名
     */
    private void collectDiagnosisNames(DrgParsedRecord drgRecord) {
        if (drgRecord.getDiagnoses() == null) {
            return;
        }
        
        for (DiagnosisEntry diagnosis : drgRecord.getDiagnoses()) {
            // 只添加主诊断名称，不添加别名
            addDiagnosisName(diagnosis.getDiagnosisNameSafe());
        }
    }
    
    /**
     * 收集手术名称
     */
    private void collectProcedureNames(DrgParsedRecord drgRecord) {
        if (drgRecord.getProcedures() == null) {
            return;
        }
        
        for (ProcedureEntry procedure : drgRecord.getProcedures()) {
            addProcedureName(procedure.getProcedureNameSafe());
        }
    }
    
    /**
     * 添加诊断名称到集合
     */
    private void addDiagnosisName(String diagnosisName) {
        if (diagnosisName != null && !diagnosisName.trim().isEmpty()) {
            primaryDiagnosisSet.add(diagnosisName.trim());
        }
    }
    
    /**
     * 添加手术名称到集合
     */
    private void addProcedureName(String procedureName) {
        if (procedureName != null && !procedureName.trim().isEmpty()) {
            primaryProcedureSet.add(procedureName.trim());
        }
    }
    
    /**
     * 获取主要诊断名称列表
     * 
     * @return 主要诊断名称列表（已去重）
     */
    public List<String> getPrimaryDiagnoses() {
        List<String> diagnoses = new ArrayList<>(primaryDiagnosisSet);
        
        if (topKEnabled) {
            if (topKLimit == 0) {
                // Top-K限制为0时返回空列表
                return Collections.emptyList();
            } else if (diagnoses.size() > topKLimit) {
                // 应用Top-K限制
                diagnoses = diagnoses.subList(0, topKLimit);
            }
        }
        
        return Collections.unmodifiableList(diagnoses);
    }
    
    /**
     * 获取主要手术名称列表
     * 
     * @return 主要手术名称列表（已去重）
     */
    public List<String> getPrimaryProcedures() {
        List<String> procedures = new ArrayList<>(primaryProcedureSet);
        
        if (topKEnabled) {
            if (topKLimit == 0) {
                // Top-K限制为0时返回空列表
                return Collections.emptyList();
            } else if (procedures.size() > topKLimit) {
                // 应用Top-K限制
                procedures = procedures.subList(0, topKLimit);
            }
        }
        
        return Collections.unmodifiableList(procedures);
    }
    
    /**
     * 判断是否有主要诊断
     * 
     * @return 如果有主要诊断返回true，否则返回false
     */
    public boolean hasPrimaryDiagnoses() {
        return !primaryDiagnosisSet.isEmpty();
    }
    
    /**
     * 判断是否有主要手术
     * 
     * @return 如果有主要手术返回true，否则返回false
     */
    public boolean hasPrimaryProcedures() {
        return !primaryProcedureSet.isEmpty();
    }
    
    /**
     * 获取主要诊断数量
     * 
     * @return 主要诊断数量
     */
    public int getPrimaryDiagnosisCount() {
        return primaryDiagnosisSet.size();
    }
    
    /**
     * 获取主要手术数量
     * 
     * @return 主要手术数量
     */
    public int getPrimaryProcedureCount() {
        return primaryProcedureSet.size();
    }
    
    /**
     * 清空所有收集的名称
     */
    public void clear() {
        primaryDiagnosisSet.clear();
        primaryProcedureSet.clear();
    }
    
    /**
     * 获取是否启用Top-K限制
     * 
     * @return 如果启用Top-K限制返回true，否则返回false
     */
    public boolean isTopKEnabled() {
        return topKEnabled;
    }
    
    /**
     * 获取Top-K限制数量
     * 
     * @return Top-K限制数量
     */
    public int getTopKLimit() {
        return topKLimit;
    }
    
    @Override
    public String toString() {
        return "NameCollector{" +
                "primaryDiagnoses=" + getPrimaryDiagnoses() +
                ", primaryProcedures=" + getPrimaryProcedures() +
                ", topKEnabled=" + topKEnabled +
                ", topKLimit=" + topKLimit +
                '}';
    }
}
