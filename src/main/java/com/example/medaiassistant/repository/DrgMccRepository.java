package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.DrgMcc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 并发症字典Repository接口
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-16
 */
@Repository
public interface DrgMccRepository extends JpaRepository<DrgMcc, Long> {
    
    /**
     * 根据MCC编码查找并发症记录
     * 
     * @param mccCode MCC编码
     * @return 并发症记录列表
     */
    List<DrgMcc> findByMccCode(String mccCode);
    
    /**
     * 根据MCC名称查找并发症记录
     * 
     * @param mccName MCC名称
     * @return 并发症记录列表
     */
    List<DrgMcc> findByMccNameContainingIgnoreCase(String mccName);
    
    /**
     * 根据MCC类型查找并发症记录
     * 
     * @param mccType MCC类型："MCC"严重/"CC"一般
     * @return 并发症记录列表
     */
    List<DrgMcc> findByMccType(String mccType);
    
    /**
     * 查找所有严重并发症记录
     * 
     * @return 严重并发症记录列表
     */
    @Query("SELECT m FROM DrgMcc m WHERE m.mccType = 'MCC'")
    List<DrgMcc> findSevereMcc();
    
    /**
     * 查找所有一般并发症记录
     * 
     * @return 一般并发症记录列表
     */
    @Query("SELECT m FROM DrgMcc m WHERE m.mccType = 'CC'")
    List<DrgMcc> findCommonMcc();
    
    /**
     * 根据MCC编码和类型查找并发症记录
     * 
     * @param mccCode MCC编码
     * @param mccType MCC类型
     * @return 并发症记录列表
     */
    List<DrgMcc> findByMccCodeAndMccType(String mccCode, String mccType);
    
    /**
     * 查找有排除条件的并发症记录
     * 
     * @return 有排除条件的并发症记录列表
     */
    @Query("SELECT m FROM DrgMcc m WHERE m.mccExcept IS NOT NULL AND m.mccExcept != ''")
    List<DrgMcc> findMccWithExclusions();
    
    /**
     * 查找无排除条件的并发症记录
     * 
     * @return 无排除条件的并发症记录列表
     */
    @Query("SELECT m FROM DrgMcc m WHERE m.mccExcept IS NULL OR m.mccExcept = ''")
    List<DrgMcc> findMccWithoutExclusions();
    
    /**
     * 统计并发症记录总数
     * 
     * @return 并发症记录总数
     */
    long count();
    
    /**
     * 统计严重并发症记录数量
     * 
     * @return 严重并发症记录数量
     */
    @Query("SELECT COUNT(m) FROM DrgMcc m WHERE m.mccType = 'MCC'")
    long countSevereMcc();
    
    /**
     * 统计一般并发症记录数量
     * 
     * @return 一般并发症记录数量
     */
    @Query("SELECT COUNT(m) FROM DrgMcc m WHERE m.mccType = 'CC'")
    long countCommonMcc();
    
    /**
     * 统计有排除条件的并发症记录数量
     * 
     * @return 有排除条件的并发症记录数量
     */
    @Query("SELECT COUNT(m) FROM DrgMcc m WHERE m.mccExcept IS NOT NULL AND m.mccExcept != ''")
    long countMccWithExclusions();
    
    /**
     * 统计无排除条件的并发症记录数量
     * 
     * @return 无排除条件的并发症记录数量
     */
    @Query("SELECT COUNT(m) FROM DrgMcc m WHERE m.mccExcept IS NULL OR m.mccExcept = ''")
    long countMccWithoutExclusions();
    
    /**
     * 根据MCC名称列表查找并发症记录
     * 
     * @param mccNames MCC名称列表
     * @return 并发症记录列表
     */
    @Query("SELECT m FROM DrgMcc m WHERE m.mccName IN ?1")
    List<DrgMcc> findByMccNameIn(List<String> mccNames);
    
    /**
     * 根据MCC编码列表查找并发症记录
     * 
     * @param mccCodes MCC编码列表
     * @return 并发症记录列表
     */
    @Query("SELECT m FROM DrgMcc m WHERE m.mccCode IN ?1")
    List<DrgMcc> findByMccCodeIn(List<String> mccCodes);
}
