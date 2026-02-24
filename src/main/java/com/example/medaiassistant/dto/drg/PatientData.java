package com.example.medaiassistant.dto.drg;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 患者数据容器数据传输对象
 * 
 * 用于封装患者的诊断和手术数据，为DRG匹配提供基础数据结构
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-21
 */
@Getter
public class PatientData {
    
    /**
     * 患者诊断列表
     */
    private final List<PatientDiagnosis> diagnoses;
    
    /**
     * 患者手术列表
     */
    private final List<PatientProcedure> procedures;
    
    /**
     * 构造函数
     * 
     * @param diagnoses 诊断列表，如果为null则创建空列表
     * @param procedures 手术列表，如果为null则创建空列表
     */
    public PatientData(List<PatientDiagnosis> diagnoses, List<PatientProcedure> procedures) {
        this.diagnoses = diagnoses != null ? new ArrayList<>(diagnoses) : new ArrayList<>();
        this.procedures = procedures != null ? new ArrayList<>(procedures) : new ArrayList<>();
    }
    
    /**
     * 判断患者是否有手术
     * 
     * @return 如果有手术返回true，否则返回false
     */
    public boolean hasProcedures() {
        return !procedures.isEmpty();
    }
    
    /**
     * 判断患者是否有诊断
     * 
     * @return 如果有诊断返回true，否则返回false
     */
    public boolean hasDiagnoses() {
        return !diagnoses.isEmpty();
    }
    
    /**
     * 获取诊断数量
     * 
     * @return 诊断数量
     */
    public int getDiagnosisCount() {
        return diagnoses.size();
    }
    
    /**
     * 获取手术数量
     * 
     * @return 手术数量
     */
    public int getProcedureCount() {
        return procedures.size();
    }
    
    /**
     * 获取不可修改的诊断列表
     * 
     * @return 不可修改的诊断列表
     */
    public List<PatientDiagnosis> getDiagnoses() {
        return Collections.unmodifiableList(diagnoses);
    }
    
    /**
     * 获取不可修改的手术列表
     * 
     * @return 不可修改的手术列表
     */
    public List<PatientProcedure> getProcedures() {
        return Collections.unmodifiableList(procedures);
    }
    
    @Override
    public String toString() {
        return "PatientData{" +
                "diagnoses=" + diagnoses +
                ", procedures=" + procedures +
                '}';
    }
}
