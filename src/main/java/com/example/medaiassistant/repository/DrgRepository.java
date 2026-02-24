package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.Drg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DRG主表Repository接口
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-16
 */
@Repository
public interface DrgRepository extends JpaRepository<Drg, Long> {
    
    /**
     * 根据DRG编码查找DRG记录
     * 
     * @param drgCode DRG编码
     * @return DRG记录列表
     */
    List<Drg> findByDrgCodeContainingIgnoreCase(String drgCode);
    
    /**
     * 根据DRG名称查找DRG记录
     * 
     * @param drgName DRG名称
     * @return DRG记录列表
     */
    List<Drg> findByDrgNameContainingIgnoreCase(String drgName);
    
    /**
     * 查找所有DRG记录，按保险支付金额降序排序
     * 
     * @return 按保险支付金额降序排序的DRG记录列表
     */
    @Query("SELECT d FROM Drg d ORDER BY d.insurancePayment DESC NULLS LAST")
    List<Drg> findAllOrderByInsurancePaymentDesc();
    
    /**
     * 查找有手术要求的DRG记录
     * 
     * @return 有手术要求的DRG记录列表
     */
    @Query("SELECT d FROM Drg d WHERE d.mainProcedures IS NOT NULL AND d.mainProcedures != ''")
    List<Drg> findDrgsWithProcedures();
    
    /**
     * 查找无手术要求的DRG记录
     * 
     * @return 无手术要求的DRG记录列表
     */
    @Query("SELECT d FROM Drg d WHERE d.mainProcedures IS NULL OR d.mainProcedures = ''")
    List<Drg> findDrgsWithoutProcedures();
    
    /**
     * 根据权重范围查找DRG记录
     * 
     * @param minWeight 最小权重
     * @param maxWeight 最大权重
     * @return 权重范围内的DRG记录列表
     */
    List<Drg> findByWeightBetween(Double minWeight, Double maxWeight);
    
    /**
     * 根据保险支付金额范围查找DRG记录
     * 
     * @param minPayment 最小保险支付金额
     * @param maxPayment 最大保险支付金额
     * @return 保险支付金额范围内的DRG记录列表
     */
    @Query("SELECT d FROM Drg d WHERE d.insurancePayment BETWEEN ?1 AND ?2 ORDER BY d.insurancePayment DESC")
    List<Drg> findByInsurancePaymentBetween(Double minPayment, Double maxPayment);
    
    /**
     * 统计DRG记录总数
     * 
     * @return DRG记录总数
     */
    long count();
    
    /**
     * 统计有手术要求的DRG记录数量
     * 
     * @return 有手术要求的DRG记录数量
     */
    @Query("SELECT COUNT(d) FROM Drg d WHERE d.mainProcedures IS NOT NULL AND d.mainProcedures != ''")
    long countDrgsWithProcedures();
    
    /**
     * 统计无手术要求的DRG记录数量
     * 
     * @return 无手术要求的DRG记录数量
     */
    @Query("SELECT COUNT(d) FROM Drg d WHERE d.mainProcedures IS NULL OR d.mainProcedures = ''")
    long countDrgsWithoutProcedures();
}
