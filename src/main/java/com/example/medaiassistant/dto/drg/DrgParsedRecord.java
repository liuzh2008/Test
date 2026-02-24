package com.example.medaiassistant.dto.drg;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DRG解析记录数据传输对象
 * 
 * 用于表示解析后的DRG记录，包含DRG基本信息、诊断和手术列表
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-21
 */
@Getter
public class DrgParsedRecord {
    
    /**
     * DRG ID
     */
    private final Long drgId;
    
    /**
     * DRG编码
     */
    private final String drgCode;
    
    /**
     * DRG名称
     */
    private final String drgName;
    
    /**
     * 保险支付金额
     */
    private final BigDecimal insurancePayment;
    
    /**
     * 诊断条目列表
     */
    private final List<DiagnosisEntry> diagnoses;
    
    /**
     * 手术条目列表
     */
    private final List<ProcedureEntry> procedures;
    
    /**
     * 构造函数
     * 
     * @param drgId DRG ID
     * @param drgCode DRG编码
     * @param drgName DRG名称
     * @param insurancePayment 保险支付金额
     * @param diagnoses 诊断条目列表，如果为null则创建空列表
     * @param procedures 手术条目列表，如果为null则创建空列表
     */
    public DrgParsedRecord(Long drgId, String drgCode, String drgName, 
                          BigDecimal insurancePayment, 
                          List<DiagnosisEntry> diagnoses, 
                          List<ProcedureEntry> procedures) {
        this.drgId = drgId;
        this.drgCode = drgCode;
        this.drgName = drgName;
        this.insurancePayment = insurancePayment;
        this.diagnoses = diagnoses != null ? new ArrayList<>(diagnoses) : new ArrayList<>();
        this.procedures = procedures != null ? new ArrayList<>(procedures) : new ArrayList<>();
    }
    
    /**
     * 判断是否有手术要求
     * 
     * @return 如果有手术要求返回true，否则返回false
     */
    public boolean hasProcedures() {
        return !procedures.isEmpty();
    }
    
    /**
     * 判断是否有诊断要求
     * 
     * @return 如果有诊断要求返回true，否则返回false
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
     * 获取保险支付金额，如果为空则返回0
     * 
     * @return 保险支付金额
     */
    public BigDecimal getInsurancePaymentSafe() {
        return insurancePayment != null ? insurancePayment : BigDecimal.ZERO;
    }
    
    /**
     * 获取不可修改的诊断列表
     * 
     * @return 不可修改的诊断列表
     */
    public List<DiagnosisEntry> getDiagnoses() {
        return Collections.unmodifiableList(diagnoses);
    }
    
    /**
     * 获取不可修改的手术列表
     * 
     * @return 不可修改的手术列表
     */
    public List<ProcedureEntry> getProcedures() {
        return Collections.unmodifiableList(procedures);
    }
    
    @Override
    public String toString() {
        return "DrgParsedRecord{" +
                "drgId=" + drgId +
                ", drgCode='" + drgCode + '\'' +
                ", drgName='" + drgName + '\'' +
                ", insurancePayment=" + insurancePayment +
                ", diagnoses=" + diagnoses +
                ", procedures=" + procedures +
                '}';
    }
}
