package com.example.medaiassistant.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 长期医嘱实体类
 * <p>
 * 对应数据库表 LONGTERMORDERS，用于存储患者的长期医嘱信息。
 * 支持从HIS系统同步医嘱数据，包括医嘱名称、剂量、频次、给药途径等。
 * </p>
 * 
 * <h2>字段映射说明</h2>
 * <ul>
 *   <li>{@code orderId} - 主键，对应ORDERID（NUMBER(10,0)）</li>
 *   <li>{@code patientId} - 患者ID，对应PATIENTID</li>
 *   <li>{@code physician} - 开单医生，对应PHYSICIAN</li>
 *   <li>{@code orderName} - 医嘱名称，对应ORDERNAME</li>
 *   <li>{@code dosage} - 剂量，对应DOSAGE</li>
 *   <li>{@code unit} - 剂量单位，对应UNIT</li>
 *   <li>{@code frequency} - 频次，对应FREQUENCY</li>
 *   <li>{@code route} - 给药途径，对应ROUTE</li>
 *   <li>{@code orderDate} - 开始时间，对应ORDERDATE（TIMESTAMP(6)）</li>
 *   <li>{@code stopTime} - 停止时间，对应STOPTIME（TIMESTAMP(6)）</li>
 *   <li>{@code isAnalyzed} - 是否已分析，默认值0</li>
 *   <li>{@code isTriggered} - 是否已触发，默认值0</li>
 *   <li>{@code visitId} - 就诊ID，对应VISIT_ID（NUMBER(10,0)）</li>
 * </ul>
 * 
 * @author TDD Generator
 * @version 1.2
 * @since 2026-01-10
 * @see LongTermOrderRepository
 */
@Entity
@Table(name = "longtermorders")
public class LongTermOrder {
    
    /**
     * 医嘱唯一标识（主键）
     * <p>对应数据库字段 ORDERID，类型 NUMBER(10,0)，由数据库触发器自动生成</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderID")
    private Long orderId;

    /**
     * 患者ID
     * <p>对应数据库字段 PATIENTID，用于关联患者信息</p>
     */
    @Column(name = "PatientID")
    private String patientId;

    /**
     * 重复标识
     * <p>对应数据库字段 REPEAT_INDICATOR</p>
     */
    @Column(name = "REPEAT_INDICATOR")
    private Integer repeatIndicator;

    /**
     * 开单医生姓名
     * <p>对应数据库字段 PHYSICIAN</p>
     */
    @Column(name = "Physician")
    private String physician;

    /**
     * 医嘱名称
     * <p>对应数据库字段 ORDERNAME，包含药品名称或医嘱项目名称</p>
     */
    @Column(name = "OrderName")
    private String orderName;

    /**
     * 剂量
     * <p>对应数据库字段 DOSAGE，如"10mg"、"500ml"等</p>
     */
    @Column(name = "Dosage")
    private String dosage;

    /**
     * 剂量单位
     * <p>对应数据库字段 UNIT，如"mg"、"ml"、"片"等</p>
     */
    @Column(name = "Unit")
    private String unit;

    /**
     * 用药频次
     * <p>对应数据库字段 FREQUENCY，如"qd"、"bid"、"tid"等</p>
     */
    @Column(name = "Frequency")
    private String frequency;

    /**
     * 给药途径
     * <p>对应数据库字段 ROUTE，如"口服"、"静脉注射"等</p>
     */
    @Column(name = "Route")
    private String route;

    /**
     * 医嘱开始时间
     * <p>对应数据库字段 ORDERDATE，类型 TIMESTAMP(6)</p>
     */
    @Column(name = "OrderDate")
    private Timestamp orderDate;

    /**
     * 医嘱停止时间
     * <p>对应数据库字段 STOPTIME，类型 TIMESTAMP(6)，null表示医嘱仍在执行</p>
     */
    @Column(name = "stoptime")
    private Timestamp stopTime;

    /**
     * 是否已分析标志
     * <p>对应数据库字段 ISANALYZED，0=未分析，1=已分析</p>
     */
    @Column(name = "IsAnalyzed")
    private Integer isAnalyzed = 0;

    /**
     * 就诊ID
     * <p>对应数据库字段 VISIT_ID，类型 NUMBER(10,0)，用于关联就诊记录</p>
     */
    @Column(name = "VISIT_ID")
    private Long visitId;

    /**
     * 是否已触发标志
     * <p>对应数据库字段 ISTRIGGERED，0=未触发，1=已触发</p>
     */
    @Column(name = "IsTriggered")
    private Integer isTriggered = 0;


    // ==================== Getters and Setters ====================
    
    /**
     * 获取医嘱ID
     * @return 医嘱唯一标识
     */
    public Long getOrderId() {
        return orderId;
    }

    /**
     * 设置医嘱ID
     * @param orderId 医嘱唯一标识
     */
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    /**
     * 获取患者ID
     * @return 患者标识
     */
    public String getPatientId() {
        return patientId;
    }

    /**
     * 设置患者ID
     * @param patientId 患者标识
     */
    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    /**
     * 获取重复指示
     * @return 重复指示值
     */
    public Integer getRepeatIndicator() {
        return repeatIndicator;
    }

    /**
     * 设置重复指示
     * @param repeatIndicator 重复指示值
     */
    public void setRepeatIndicator(Integer repeatIndicator) {
        this.repeatIndicator = repeatIndicator;
    }

    /**
     * 获取开单医生
     * @return 医生姓名
     */
    public String getPhysician() {
        return physician;
    }

    /**
     * 设置开单医生
     * @param physician 医生姓名
     */
    public void setPhysician(String physician) {
        this.physician = physician;
    }

    /**
     * 获取医嘱名称
     * @return 医嘱名称
     */
    public String getOrderName() {
        return orderName;
    }

    /**
     * 设置医嘱名称
     * @param orderName 医嘱名称
     */
    public void setOrderName(String orderName) {
        this.orderName = orderName;
    }

    /**
     * 获取剂量
     * @return 剂量值
     */
    public String getDosage() {
        return dosage;
    }

    /**
     * 设置剂量
     * @param dosage 剂量值
     */
    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    /**
     * 获取剂量单位
     * @return 单位名称
     */
    public String getUnit() {
        return unit;
    }

    /**
     * 设置剂量单位
     * @param unit 单位名称
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * 获取用药频次
     * @return 频次编码
     */
    public String getFrequency() {
        return frequency;
    }

    /**
     * 设置用药频次
     * @param frequency 频次编码
     */
    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    /**
     * 获取给药途径
     * @return 给药途径
     */
    public String getRoute() {
        return route;
    }

    /**
     * 设置给药途径
     * @param route 给药途径
     */
    public void setRoute(String route) {
        this.route = route;
    }

    /**
     * 获取医嘱开始时间
     * @return 开始时间戳
     */
    public Timestamp getOrderDate() {
        return orderDate;
    }

    /**
     * 设置医嘱开始时间
     * @param orderDate 开始时间戳
     */
    public void setOrderDate(Timestamp orderDate) {
        this.orderDate = orderDate;
    }

    /**
     * 获取医嘱停止时间
     * @return 停止时间戳，null表示医嘱仍在执行
     */
    public Timestamp getStopTime() {
        return stopTime;
    }

    /**
     * 设置医嘱停止时间
     * @param stopTime 停止时间戳
     */
    public void setStopTime(Timestamp stopTime) {
        this.stopTime = stopTime;
    }

    /**
     * 获取分析状态
     * @return 0=未分析，1=已分析
     */
    public Integer getIsAnalyzed() {
        return isAnalyzed;
    }

    /**
     * 设置分析状态
     * @param isAnalyzed 0=未分析，1=已分析
     */
    public void setIsAnalyzed(Integer isAnalyzed) {
        this.isAnalyzed = isAnalyzed;
    }

    /**
     * 获取就诊ID
     * @return 就诊记录标识
     */
    public Long getVisitId() {
        return visitId;
    }

    /**
     * 设置就诊ID
     * @param visitId 就诊记录标识
     */
    public void setVisitId(Long visitId) {
        this.visitId = visitId;
    }

    /**
     * 获取触发状态
     * @return 0=未触发，1=已触发
     */
    public Integer getIsTriggered() {
        return isTriggered;
    }

    /**
     * 设置触发状态
     * @param isTriggered 0=未触发，1=已触发
     */
    public void setIsTriggered(Integer isTriggered) {
        this.isTriggered = isTriggered;
    }
}
