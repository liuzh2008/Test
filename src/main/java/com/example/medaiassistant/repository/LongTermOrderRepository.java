package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.LongTermOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Date;
import java.util.Optional;

/**
 * 长期医嘱数据访问接口
 * <p>
 * 该接口提供了对长期医嘱数据的访问方法，包括：
 * </p>
 * <ul>
 *   <li>基本CRUD操作（继承自JpaRepository）</li>
 *   <li>医嘱同步扩展方法（findByOrderId, existsByOrderId）</li>
 *   <li>患者状态查询方法（病危/病重医嘱查询）</li>
 *   <li>统计查询方法（最近医嘱查询）</li>
 * </ul>
 * 
 * <h2>医嘱同步功能</h2>
 * <p>
 * 支持从HIS系统同步医嘱数据，通过OrderId进行重复检测和upsert策略。
 * 主要方法：
 * </p>
 * <ul>
 *   <li>{@link #findByOrderId(Long)} - 查找已存在的医嘱记录</li>
 *   <li>{@link #existsByOrderId(Long)} - 快速检查医嘱是否存在</li>
 * </ul>
 * 
 * @author Cline
 * @version 1.1
 * @since 2025-08-10
 * @see com.example.medaiassistant.model.LongTermOrder
 */
@Repository
public interface LongTermOrderRepository extends JpaRepository<LongTermOrder, Long> {
	
	// ==================== 医嘱同步扩展方法 ====================
	
	/**
	 * 检查未停用的医嘱是否已存在（用于同步时判重）
	 * <p>
	 * 判断条件：PatientID + OrderName + OrderDate + RepeatIndicator + StopTime is Null
	 * 已停用的医嘱（StopTime is not Null）不视为重复
	 * 临时医嘱和长期医嘱分别判重，避免相同名称和时间的医嘱被误判为重复
	 * </p>
	 * 
	 * @param patientId 患者ID
	 * @param orderName 医嘱名称
	 * @param orderDate 医嘱开立时间
	 * @param repeatIndicator 重复标识符（1=长期医嘱，0=临时医嘱）
	 * @return 存在返回true，否则返回false
	 */
	@Query("SELECT CASE WHEN COUNT(lto) > 0 THEN true ELSE false END FROM LongTermOrder lto " +
	       "WHERE lto.patientId = :patientId AND lto.orderName = :orderName " +
	       "AND lto.orderDate = :orderDate AND lto.repeatIndicator = :repeatIndicator AND lto.stopTime IS NULL")
	boolean existsActiveOrder(@Param("patientId") String patientId, 
	                          @Param("orderName") String orderName, 
	                          @Param("orderDate") java.sql.Timestamp orderDate,
	                          @Param("repeatIndicator") Integer repeatIndicator);
	
	/**
	 * 根据医嘱ID查询医嘱记录
	 * <p>
	 * 用于医嘱同步时检查记录是否已存在，实现upsert策略。
	 * 当记录存在时，返回完整的医嘱实体以便更新。
	 * </p>
	 * 
	 * @param orderId 医嘱唯一标识（对应数据库ORDERID字段）
	 * @return 包含医嘱记录的Optional，不存在则返回空Optional
	 * @see #existsByOrderId(Long)
	 */
	Optional<LongTermOrder> findByOrderId(Long orderId);
	
	/**
	 * 检查指定医嘱ID的记录是否存在
	 * <p>
	 * 用于医嘱同步时快速判断记录是否已存在，
	 * 避免加载完整实体，提高性能。
	 * </p>
	 * 
	 * @param orderId 医嘱唯一标识（对应数据库ORDERID字段）
	 * @return 存在返回true，否则返回false
	 * @see #findByOrderId(Long)
	 */
	boolean existsByOrderId(Long orderId);
	
	// ==================== 原有方法 ====================
	
	/**
	 * 根据患者ID和重复标识符查询长期医嘱，按开立时间升序排列
	 * <p>
	 * 使用Spring Data JPA命名约定自动生成SQL查询，
	 * 相当于SQL：{@code ORDER BY OrderDate ASC}
	 * </p>
	 * 
	 * @param patientId 患者ID
	 * @param repeatIndicator 重复标识符（1=长期医嘱，0=临时医嘱）
	 * @return 符合条件的长期医嘱列表，按开立时间升序排序
	 * @since 2026-01-29
	 * @see LongTermOrder#getOrderDate()
	 */
	List<LongTermOrder> findByPatientIdAndRepeatIndicatorOrderByOrderDateAsc(String patientId, int repeatIndicator);
	
	/**
	 * 查询患者未停止的长期医嘱中包含"病危"或"病重"的记录
	 * 
	 * 该方法用于查找指定患者ID的未停止（stopTime为NULL）且医嘱名称包含"病危"或"病重"的长期医嘱，
	 * 用于确定是否需要将患者状态更新为"病危"或"病重"。
	 * 
	 * @param patientId 患者ID
	 * @return 符合条件的长期医嘱列表
	 */
	@Query("SELECT lto FROM LongTermOrder lto WHERE lto.patientId = :patientId " +
	       "AND lto.stopTime IS NULL " +
	       "AND (lto.orderName LIKE '%病危%' OR lto.orderName LIKE '%病重%')")
	List<LongTermOrder> findActiveCriticalOrSeriousOrdersByPatientId(@Param("patientId") String patientId);
	
	/**
	 * 查询患者状态为"病危"或"病重"且医嘱已停止的长期医嘱
	 * 
	 * 该方法用于查找指定患者ID的已停止（stopTime不为NULL）且医嘱名称包含"病危"或"病重"的长期医嘱，
	 * 用于确定是否需要将患者状态从"病危"或"病重"更新为"普通"。
	 * 
	 * @param patientId 患者ID
	 * @return 符合条件的长期医嘱列表
	 */
	@Query("SELECT lto FROM LongTermOrder lto WHERE lto.patientId = :patientId " +
	       "AND lto.stopTime IS NOT NULL " +
	       "AND (lto.orderName LIKE '%病危%' OR lto.orderName LIKE '%病重%')")
	List<LongTermOrder> findStoppedCriticalOrSeriousOrdersByPatientId(@Param("patientId") String patientId);
	
	/**
	 * 查询各患者最近一条医嘱，且医嘱时间不晚于指定截止时间
	 */
	@Query(value = "SELECT l.* FROM longtermorders l " +
	        "INNER JOIN (SELECT PatientID, MAX(OrderDate) AS max_date FROM longtermorders GROUP BY PatientID) t " +
	        "ON l.PatientID = t.PatientID AND l.OrderDate = t.max_date " +
	        "WHERE l.OrderDate <= :cutoff", nativeQuery = true)
	List<LongTermOrder> findLatestOrdersBefore(@Param("cutoff") Date cutoff);
	
	/**
	 * 查询各患者最近一条医嘱，且医嘱时间在指定时间范围内
	 */
	@Query(value = "SELECT l.* FROM longtermorders l " +
	        "INNER JOIN (SELECT PatientID, MAX(OrderDate) AS max_date FROM longtermorders GROUP BY PatientID) t " +
	        "ON l.PatientID = t.PatientID AND l.OrderDate = t.max_date " +
	        "WHERE l.OrderDate BETWEEN :startTime AND :endTime", nativeQuery = true)
	List<LongTermOrder> findLatestOrdersInTimeRange(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
}
