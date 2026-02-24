package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.SurgeryDictionary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 手术字典数据访问接口
 * 提供对手术字典表(surgerydictionary)的数据访问操作
 */
@Repository
public interface SurgeryDictionaryRepository extends JpaRepository<SurgeryDictionary, Integer> {
    
    /**
     * 获取所有手术字典记录
     * 
     * 该方法通过执行原生SQL查询"SELECT * FROM surgerydictionary"来获取手术字典表中的所有记录
     * 不进行任何过滤条件，返回表中的全部数据
     * 
     * @return List<SurgeryDictionary> 所有手术字典记录列表
     *         列表中的每个元素包含以下字段：
     *         - dictId: 字典ID (Integer)
     *         - dictName: 字典名称 (String)
     *         - dictContent: 字典内容 (String, JSON格式)
     *         - department: 科室信息 (String, JSON数组格式)
     *         - groupName: 组名信息 (String, JSON数组格式)
     *         
     * @since 1.0
     * @author MedAiAssistant
     */
    @Query(value = "SELECT * FROM surgerydictionary", nativeQuery = true)
    List<SurgeryDictionary> findAllRecords();
    
    
    /**
     * 根据字典名称获取所有记录
     * @param dictName 字典名称
     * @return 符合条件的手术字典记录列表
     */
    @Query(value = "SELECT * FROM surgerydictionary WHERE dict_name = :dictName", nativeQuery = true)
    List<SurgeryDictionary> findByDictName(@Param("dictName") String dictName);
    
    /**
     * 根据字典名称和科室获取记录
     * 
     * 该方法通过执行原生SQL查询来获取符合字典名称和科室条件的手术字典记录
     * 使用Oracle兼容的字符串连接语法进行模糊查询
     * 
     * @param dictName 字典名称，必填参数
     *                 数据类型：String
     *                 说明：要查询的字典名称，精确匹配
     *                 
     * @param department 科室名称，必填参数
     *                   数据类型：String
     *                   说明：要查询的科室名称，使用模糊匹配（LIKE '%科室名称%'）
     *                   
     * @return List<SurgeryDictionary> 符合条件的手术字典记录列表
     *         列表中的每个元素包含以下字段：
     *         - dictId: 字典ID (Integer)
     *         - dictName: 字典名称 (String)
     *         - dictContent: 字典内容 (String, JSON格式)
     *         - department: 科室信息 (String, JSON数组格式)
     *         - groupName: 组名信息 (String, JSON数组格式)
     *         
     * @apiNote 此方法使用Oracle兼容的SQL语法，支持Oracle和MySQL数据库
     *          查询条件：dict_name = :dictName AND department LIKE '%' || :department || '%'
     *          
     * @since 1.0
     * @author MedAiAssistant
     */
    @Query(value = "SELECT * FROM surgerydictionary WHERE dict_name = :dictName AND department LIKE '%' || :department || '%'", nativeQuery = true)
    List<SurgeryDictionary> findByDictNameAndDepartment(@Param("dictName") String dictName, @Param("department") String department);
    
    /**
     * 根据字典名称、科室和组名获取记录
     * 
     * 该方法通过执行原生SQL查询来获取符合字典名称、科室和组名条件的手术字典记录
     * 使用Oracle兼容的字符串连接语法进行模糊查询
     * 
     * @param dictName 字典名称，必填参数
     *                 数据类型：String
     *                 说明：要查询的字典名称，精确匹配
     *                 
     * @param department 科室名称，必填参数
     *                   数据类型：String
     *                   说明：要查询的科室名称，使用模糊匹配（LIKE '%科室名称%'）
     *                   
     * @param groupName 组名，必填参数
     *                  数据类型：String
     *                  说明：要查询的组名，使用模糊匹配（LIKE '%组名%'）
     *                  
     * @return List<SurgeryDictionary> 符合条件的手术字典记录列表
     *         列表中的每个元素包含以下字段：
     *         - dictId: 字典ID (Integer)
     *         - dictName: 字典名称 (String)
     *         - dictContent: 字典内容 (String, JSON格式)
     *         - department: 科室信息 (String, JSON数组格式)
     *         - groupName: 组名信息 (String, JSON数组格式)
     *         
     * @apiNote 此方法使用Oracle兼容的SQL语法，支持Oracle和MySQL数据库
     *          查询条件：dict_name = :dictName AND department LIKE '%' || :department || '%' AND group_name LIKE '%' || :groupName || '%'
     *          
     * @since 1.0
     * @author MedAiAssistant
     */
    @Query(value = "SELECT * FROM surgerydictionary WHERE dict_name = :dictName AND department LIKE '%' || :department || '%' AND group_name LIKE '%' || :groupName || '%'", nativeQuery = true)
    List<SurgeryDictionary> findByDictNameAndDepartmentAndGroupName(@Param("dictName") String dictName, @Param("department") String department, @Param("groupName") String groupName);
    
    /**
     * 根据字典ID删除记录
     * @param dictId 字典ID
     * @return 删除的记录数
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM surgerydictionary WHERE dict_id = :dictId", nativeQuery = true)
    int deleteByDictId(@Param("dictId") Integer dictId);
}
