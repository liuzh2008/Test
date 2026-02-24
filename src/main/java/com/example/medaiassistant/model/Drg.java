package com.example.medaiassistant.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * DRG主实体类
 * 对应数据库表: drgs
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-16
 */
@Entity
@Table(name = "drgs")
@Data
public class Drg {
    
    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DRGID")
    private Long drgId;
    
    /**
     * DRG编码
     */
    @Column(name = "DRGCODE", columnDefinition = "CLOB")
    private String drgCode;
    
    /**
     * DRG名称
     */
    @Column(name = "DRGNAME", columnDefinition = "CLOB")
    private String drgName;
    
    /**
     * 主要诊断列表
     * 格式: [编码] [空格] [名称]
     * 示例: "I48.000 阵发性心房颤动\nI48.100 持续性心房颤动"
     */
    @Column(name = "MAINDIAGNOSES", columnDefinition = "CLOB")
    private String mainDiagnoses;
    
    /**
     * 主要手术列表
     * 格式: [编码] [空格] [名称]
     * 示例: "37.9000x001 经皮左心耳封堵术"
     */
    @Column(name = "MAINPROCEDURES", columnDefinition = "CLOB")
    private String mainProcedures;
    
    /**
     * 权重
     */
    @Column(name = "WEIGHT")
    private BigDecimal weight;
    
    /**
     * 出院人数
     */
    @Column(name = "DISCHARGECOUNT")
    private BigDecimal dischargeCount;
    
    /**
     * 总患者费用
     */
    @Column(name = "TOTALPATIENTCOST")
    private BigDecimal totalPatientCost;
    
    /**
     * 保险支付金额（排序关键）
     */
    @Column(name = "INSURANCEPAYMENT")
    private BigDecimal insurancePayment;
    
    /**
     * 70%保险支付
     */
    @Column(name = "INSURANCEPAYMENT70")
    private BigDecimal insurancePayment70;
    
    /**
     * 140%保险支付
     */
    @Column(name = "INSURANCEPAYMENT140")
    private BigDecimal insurancePayment140;
    
    /**
     * 总盈亏
     */
    @Column(name = "TOTALPROFITLOSS")
    private BigDecimal totalProfitLoss;
    
    /**
     * 每例平均盈亏
     */
    @Column(name = "AVGPROFITLOSSPERCASE")
    private BigDecimal avgProfitLossPerCase;
    
    /**
     * 默认构造函数
     */
    public Drg() {
    }
    
    /**
     * 带参数构造函数
     */
    public Drg(Long drgId, String drgCode, String drgName, String mainDiagnoses, 
               String mainProcedures, BigDecimal weight, BigDecimal insurancePayment) {
        this.drgId = drgId;
        this.drgCode = drgCode;
        this.drgName = drgName;
        this.mainDiagnoses = mainDiagnoses;
        this.mainProcedures = mainProcedures;
        this.weight = weight;
        this.insurancePayment = insurancePayment;
    }
    
    /**
     * 获取保险支付金额，如果为空则返回0
     */
    public BigDecimal getInsurancePaymentSafe() {
        return insurancePayment != null ? insurancePayment : BigDecimal.ZERO;
    }
    
    /**
     * 判断是否有手术要求
     */
    public boolean hasProcedures() {
        return mainProcedures != null && !mainProcedures.trim().isEmpty();
    }
    
    /**
     * 判断是否有诊断要求
     */
    public boolean hasDiagnoses() {
        return mainDiagnoses != null && !mainDiagnoses.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return "Drg{" +
                "drgId=" + drgId +
                ", drgCode='" + drgCode + '\'' +
                ", drgName='" + drgName + '\'' +
                ", insurancePayment=" + insurancePayment +
                '}';
    }
}
