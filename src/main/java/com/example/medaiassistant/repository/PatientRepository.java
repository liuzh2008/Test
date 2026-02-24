package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 患者数据访问接口
 * 
 * 该接口提供了对患者数据的访问方法，包括基本的CRUD操作以及特定的查询方法，
 * 用于支持告警规则处理和患者状态更新功能。
 * 
 * @author Cline
 * @since 2025-08-06
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, String> {
    
    /**
     * 根据科室、在院状态和入院时间查询患者
     * 
     * 该方法会查询指定科室中在院且入院时间早于给定截止时间的患者。这是一个自定义JPQL查询方法，
     * 用于支持基于时间规则的患者筛选功能。
     * 
     * 查询逻辑说明：
     * 1. 首先根据科室名称筛选患者
     * 2. 然后根据在院状态筛选患者（true表示在院，false表示已出院）
     * 3. 最后筛选入院时间早于指定截止时间的患者
     * 
     * 使用场景：
     * 主要用于时间规则触发的患者筛选，例如：筛选入院超过一定时间的在院患者，
     * 以便进行相关的提醒或处理操作。
     * 
     * @param department 科室名称，用于筛选指定科室的患者
     * @param isInHospital 是否在院状态，true表示在院患者，false表示已出院患者
     * @param offsetHours 偏移小时数，用于计算截止时间（实际查询中不直接使用，仅用于日志记录）
     * @param cutoffTime 截止时间，用于筛选入院时间早于该时间的患者
     * @return 符合条件的患者列表，如果无符合条件的患者则返回空列表
     * @see Patient
     */
    @Query("SELECT p FROM Patient p WHERE p.department = :department " +
           "AND p.isInHospital = :isInHospital " +
           "AND p.admissionTime < :cutoffTime")
    List<Patient> findByDepartmentAndAdmissionTimeBeforeOffset(
        @Param("department") String department,
        @Param("isInHospital") Boolean isInHospital,
        @Param("offsetHours") Integer offsetHours,
        @Param("cutoffTime") Date cutoffTime
    );
    
    /**
     * 根据科室和在院状态查询患者
     * 
     * @param department 科室名称
     * @param isInHospital 是否在院
     * @return 符合条件的患者列表
     */
    List<Patient> findByDepartmentAndIsInHospital(String department, boolean isInHospital);
    
    /**
     * 根据在院状态查询患者
     * 
     * 该方法用于查询所有在院或已出院的患者，主要用于患者状态更新功能。
     * 
     * @param isInHospital 是否在院
     * @return 符合条件的患者列表
     */
    @Query("SELECT p FROM Patient p WHERE p.isInHospital = :isInHospital")
    List<Patient> findByIsInHospital(@Param("isInHospital") boolean isInHospital);
    
    /**
     * 根据在院状态分页查询患者
     * 
     * 该方法用于分页查询在院或已出院的患者，避免一次性加载大量数据到内存。
     * 主要用于定时任务处理大量患者数据时的内存优化。
     * 
     * @param isInHospital 是否在院
     * @param pageable 分页参数
     * @return 分页的患者数据
     */
    @Query("SELECT p FROM Patient p WHERE p.isInHospital = :isInHospital")
    Page<Patient> findByIsInHospital(@Param("isInHospital") boolean isInHospital, Pageable pageable);
    
    /**
     * 根据患者ID更新患者状态
     * 
     * 该方法用于更新指定患者的当前状态，如从"普通"更新为"病危"或"病重"，
     * 或从"病危"或"病重"更新为"普通"。
     * 
     * @param patientId 患者ID
     * @param status 新的状态
     * @return 更新的记录数
     */
    @Modifying
    @Transactional
    @Query("UPDATE Patient p SET p.status = :status WHERE p.patientId = :patientId")
    int updateStatusByPatientId(@Param("patientId") String patientId, @Param("status") String status);
    
    /**
     * 根据患者状态和在院状态查询患者
     * 
     * 该方法使用模糊匹配查询患者状态，可以匹配包含指定状态字符串的患者记录。
     * 例如，当患者状态字段值为"入院，病重"时，传入参数status为"病重"也能正确匹配到记录。
     * 
     * 查询逻辑说明：
     * 1. 使用LIKE操作符进行模糊匹配，匹配包含指定状态字符串的患者记录
     * 2. 结合在院状态进行二次筛选，确保查询结果的准确性
     * 
     * 使用场景：
     * 主要用于根据患者状态进行筛选，支持复合状态的匹配查询，例如：
     * - 查询所有病重的在院患者（状态可能为"病重"或"入院，病重"等）
     * - 查询所有病危的在院患者（状态可能为"病危"或"病重，病危"等）
     * 
     * @param status 患者状态字符串，支持模糊匹配
     * @param isInHospital 是否在院状态，true表示在院患者，false表示已出院患者
     * @return 符合条件的患者列表，如果无符合条件的患者则返回空列表
     * @see Patient
     */
    @Query("SELECT p FROM Patient p WHERE p.status LIKE %:status% AND p.isInHospital = :isInHospital")
    List<Patient> findByStatusAndIsInHospital(@Param("status") String status, @Param("isInHospital") boolean isInHospital);
    
    /**
     * 根据患者ID查询患者
     *
     * 该方法用于根据患者ID查询患者信息，包括当前状态。
     *
     * @param patientId 患者ID
     * @return 患者对象，如果未找到则返回null
     */
    @Query("SELECT p FROM Patient p WHERE p.patientId = :patientId")
    Patient findByPatientId(@Param("patientId") String patientId);

    /**
     * 根据科室查询7天内出院的患者，按出院时间倒序排列
     *
     * 该方法用于查询指定科室在过去7天内出院的患者信息，
     * 并按出院时间从最新到最旧排序。
     *
     * @param department 科室名称
     * @param startDate 开始日期（7天前的日期）
     * @param endDate 结束日期（当前日期）
     * @return 符合条件的患者列表，按出院时间倒序排列
     */
    @Query("SELECT p FROM Patient p WHERE p.department = :department " +
           "AND p.isInHospital = false " +
           "AND p.dischargeTime BETWEEN :startDate AND :endDate " +
           "ORDER BY p.dischargeTime DESC")
    List<Patient> findDischargedPatientsByDepartmentInLast7Days(
        @Param("department") String department,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate
    );

    /**
     * 根据科室分页查询7天内出院的患者，按出院时间倒序排列
     *
     * 该方法用于分页查询指定科室在过去7天内出院的患者信息，
     * 并按出院时间从最新到最旧排序，支持分页功能。
     *
     * @param department 科室名称
     * @param startDate 开始日期（7天前的日期）
     * @param endDate 结束日期（当前日期）
     * @param pageable 分页参数
     * @return 分页的患者数据，按出院时间倒序排列
     */
    @Query("SELECT p FROM Patient p WHERE p.department = :department " +
           "AND p.isInHospital = false " +
           "AND p.dischargeTime BETWEEN :startDate AND :endDate " +
           "ORDER BY p.dischargeTime DESC")
    Page<Patient> findDischargedPatientsByDepartmentInLast7Days(
        @Param("department") String department,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate,
        Pageable pageable
    );

    /**
     * 根据科室列表和在院状态查询患者
     *
     * 该方法用于查询指定科室列表中的在院或已出院患者，
     * 支持定时任务科室过滤功能的数据访问需求。
     *
     * @param departments 科室名称列表
     * @param isInHospital 是否在院状态
     * @return 符合条件的患者列表
     */
    @Query("SELECT p FROM Patient p WHERE p.department IN :departments " +
           "AND p.isInHospital = :isInHospital")
    List<Patient> findByDepartmentsAndIsInHospital(
        @Param("departments") List<String> departments,
        @Param("isInHospital") boolean isInHospital
    );

    /**
     * 根据科室列表和在院状态分页查询患者
     *
     * 该方法用于分页查询指定科室列表中的在院或已出院患者，
     * 支持定时任务科室过滤功能的分页数据访问需求。
     *
     * @param departments 科室名称列表
     * @param isInHospital 是否在院状态
     * @param pageable 分页参数
     * @return 分页的患者数据
     */
    @Query("SELECT p FROM Patient p WHERE p.department IN :departments " +
           "AND p.isInHospital = :isInHospital")
    Page<Patient> findByDepartmentsAndIsInHospital(
        @Param("departments") List<String> departments,
        @Param("isInHospital") boolean isInHospital,
        Pageable pageable
    );

    /**
     * 安全方法：根据科室列表和在院状态查询患者（处理空或null列表）
     *
     * 该方法是对findByDepartmentsAndIsInHospital的安全封装，
     * 当传入空列表或null时返回空结果，避免数据库查询异常。
     *
     * @param departments 科室名称列表（可为空或null）
     * @param isInHospital 是否在院状态
     * @return 符合条件的患者列表，如果科室列表为空或null则返回空列表
     */
    default List<Patient> findByDepartmentsAndIsInHospitalSafe(List<String> departments, boolean isInHospital) {
        if (departments == null || departments.isEmpty()) {
            return Collections.emptyList();
        }
        return findByDepartmentsAndIsInHospital(departments, isInHospital);
    }

    /**
     * 安全方法：根据科室列表和在院状态分页查询患者（处理空或null列表）
     *
     * 该方法是对findByDepartmentsAndIsInHospital的安全封装，
     * 当传入空列表或null时返回空分页结果，避免数据库查询异常。
     *
     * @param departments 科室名称列表（可为空或null）
     * @param isInHospital 是否在院状态
     * @param pageable 分页参数
     * @return 分页的患者数据，如果科室列表为空或null则返回空分页
     */
    default Page<Patient> findByDepartmentsAndIsInHospitalSafe(List<String> departments, boolean isInHospital, Pageable pageable) {
        if (departments == null || departments.isEmpty()) {
            return Page.empty(pageable);
        }
        return findByDepartmentsAndIsInHospital(departments, isInHospital, pageable);
    }
    
    /**
     * 根据床号列表和在院状态查询患者
     *
     * 该方法用于查询指定床号列表中的在院或已出院患者，
     * 支持床号过滤功能的数据访问需求。
     *
     * @param bedNumbers 床号列表
     * @param isInHospital 是否在院状态
     * @return 符合条件的患者列表
     */
    @Query("SELECT p FROM Patient p WHERE p.bedNumber IN :bedNumbers " +
           "AND p.isInHospital = :isInHospital")
    List<Patient> findByBedNumbersAndIsInHospital(
        @Param("bedNumbers") List<String> bedNumbers,
        @Param("isInHospital") boolean isInHospital
    );
    
    /**
     * 根据床号列表和在院状态分页查询患者
     *
     * 该方法用于分页查询指定床号列表中的在院或已出院患者，
     * 支持床号过滤功能的分页数据访问需求。
     *
     * @param bedNumbers 床号列表
     * @param isInHospital 是否在院状态
     * @param pageable 分页参数
     * @return 分页的患者数据
     */
    @Query("SELECT p FROM Patient p WHERE p.bedNumber IN :bedNumbers " +
           "AND p.isInHospital = :isInHospital")
    Page<Patient> findByBedNumbersAndIsInHospital(
        @Param("bedNumbers") List<String> bedNumbers,
        @Param("isInHospital") boolean isInHospital,
        Pageable pageable
    );
    
    /**
     * 根据科室列表、床号列表和在院状态查询患者
     *
     * 该方法用于查询指定科室列表和床号列表中的在院或已出院患者，
     * 支持科室+床号组合过滤功能的数据访问需求。
     *
     * @param departments 科室名称列表
     * @param bedNumbers 床号列表
     * @param isInHospital 是否在院状态
     * @return 符合条件的患者列表
     */
    @Query("SELECT p FROM Patient p WHERE p.department IN :departments " +
           "AND p.bedNumber IN :bedNumbers " +
           "AND p.isInHospital = :isInHospital")
    List<Patient> findByDepartmentsAndBedNumbersAndIsInHospital(
        @Param("departments") List<String> departments,
        @Param("bedNumbers") List<String> bedNumbers,
        @Param("isInHospital") boolean isInHospital
    );
    
    /**
     * 根据科室列表、床号列表和在院状态分页查询患者
     *
     * 该方法用于分页查询指定科室列表和床号列表中的在院或已出院患者，
     * 支持科室+床号组合过滤功能的分页数据访问需求。
     *
     * @param departments 科室名称列表
     * @param bedNumbers 床号列表
     * @param isInHospital 是否在院状态
     * @param pageable 分页参数
     * @return 分页的患者数据
     */
    @Query("SELECT p FROM Patient p WHERE p.department IN :departments " +
           "AND p.bedNumber IN :bedNumbers " +
           "AND p.isInHospital = :isInHospital")
    Page<Patient> findByDepartmentsAndBedNumbersAndIsInHospital(
        @Param("departments") List<String> departments,
        @Param("bedNumbers") List<String> bedNumbers,
        @Param("isInHospital") boolean isInHospital,
        Pageable pageable
    );
    
    /**
     * 安全方法：根据床号列表和在院状态查询患者（处理空或null列表）
     *
     * 该方法是对findByBedNumbersAndIsInHospital的安全封装，
     * 当传入空列表或null时返回空结果，避免数据库查询异常。
     *
     * @param bedNumbers 床号列表（可为空或null）
     * @param isInHospital 是否在院状态
     * @return 符合条件的患者列表，如果床号列表为空或null则返回空列表
     */
    default List<Patient> findByBedNumbersAndIsInHospitalSafe(List<String> bedNumbers, boolean isInHospital) {
        if (bedNumbers == null || bedNumbers.isEmpty()) {
            return Collections.emptyList();
        }
        return findByBedNumbersAndIsInHospital(bedNumbers, isInHospital);
    }
    
    /**
     * 安全方法：根据床号列表和在院状态分页查询患者（处理空或null列表）
     *
     * 该方法是对findByBedNumbersAndIsInHospital的安全封装，
     * 当传入空列表或null时返回空分页结果，避免数据库查询异常。
     *
     * @param bedNumbers 床号列表（可为空或null）
     * @param isInHospital 是否在院状态
     * @param pageable 分页参数
     * @return 分页的患者数据，如果床号列表为空或null则返回空分页
     */
    default Page<Patient> findByBedNumbersAndIsInHospitalSafe(List<String> bedNumbers, boolean isInHospital, Pageable pageable) {
        if (bedNumbers == null || bedNumbers.isEmpty()) {
            return Page.empty(pageable);
        }
        return findByBedNumbersAndIsInHospital(bedNumbers, isInHospital, pageable);
    }
    
    /**
     * 安全方法：根据科室列表、床号列表和在院状态查询患者（处理空或null列表）
     *
     * 该方法是对findByDepartmentsAndBedNumbersAndIsInHospital的安全封装，
     * 当传入空列表或null时返回空结果，避免数据库查询异常。
     *
     * @param departments 科室名称列表（可为空或null）
     * @param bedNumbers 床号列表（可为空或null）
     * @param isInHospital 是否在院状态
     * @return 符合条件的患者列表，如果科室列表或床号列表为空或null则返回空列表
     */
    default List<Patient> findByDepartmentsAndBedNumbersAndIsInHospitalSafe(
            List<String> departments, List<String> bedNumbers, boolean isInHospital) {
        if ((departments == null || departments.isEmpty()) || 
            (bedNumbers == null || bedNumbers.isEmpty())) {
            return Collections.emptyList();
        }
        return findByDepartmentsAndBedNumbersAndIsInHospital(departments, bedNumbers, isInHospital);
    }
    
    /**
     * 安全方法：根据科室列表、床号列表和在院状态分页查询患者（处理空或null列表）
     *
     * 该方法是对findByDepartmentsAndBedNumbersAndIsInHospital的安全封装，
     * 当传入空列表或null时返回空分页结果，避免数据库查询异常。
     *
     * @param departments 科室名称列表（可为空或null）
     * @param bedNumbers 床号列表（可为空或null）
     * @param isInHospital 是否在院状态
     * @param pageable 分页参数
     * @return 分页的患者数据，如果科室列表或床号列表为空或null则返回空分页
     */
    default Page<Patient> findByDepartmentsAndBedNumbersAndIsInHospitalSafe(
            List<String> departments, List<String> bedNumbers, boolean isInHospital, Pageable pageable) {
        if ((departments == null || departments.isEmpty()) || 
            (bedNumbers == null || bedNumbers.isEmpty())) {
            return Page.empty(pageable);
        }
        return findByDepartmentsAndBedNumbersAndIsInHospital(departments, bedNumbers, isInHospital, pageable);
    }
}
