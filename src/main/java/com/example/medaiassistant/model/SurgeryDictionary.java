package com.example.medaiassistant.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 手术字典实体类
 * 映射surgerydictionary表，用于存储手术相关的字典数据
 * 
 * 该类用于与数据库中的surgerydictionary表进行ORM映射
 * 包含了手术字典的所有字段，用于持久化存储
 * 使用Lombok的@Data注解自动生成getter、setter、toString等方法
 * 使用JPA注解进行数据库映射
 * 
 * 表结构：
 * - 表名：surgerydictionary
 * - 主键：dict_id (自增长)
 * - 字段：dict_name, dict_content, department, group_name
 * 
 * @since 1.0
 * @author MedAiAssistant
 * 
 * @example
 * // 创建实体实例
 * SurgeryDictionary surgeryDict = new SurgeryDictionary();
 * surgeryDict.setDictName("心脏手术");
 * surgeryDict.setDictContent("{\"description\": \"心脏相关手术\", \"riskLevel\": \"high\"}");
 * surgeryDict.setDepartment("[\"心血管内科\", \"心脏外科\"]");
 * surgeryDict.setGroupName("[\"心脏组\", \"血管组\"]");
 * 
 * @example
 * // 使用Lombok生成的getter方法
 * String name = surgeryDict.getDictName();
 * String content = surgeryDict.getDictContent();
 */
@Entity
@Table(name = "surgerydictionary")
@Data
public class SurgeryDictionary {
    /**
     * 字典ID，主键，自增长
     * 
     * 数据库字段：dict_id
     * 数据类型：INT
     * 约束：主键，自增长
     * 
     * 用于唯一标识一条手术字典记录
     * 在新增记录时由数据库自动生成
     * 在更新和删除操作中用于定位记录
     * 
     * @since 1.0
     * 
     * @example
     * // 设置字典ID（通常由数据库生成，不需要手动设置）
     * surgeryDictionary.setDictId(1);
     * 
     * @example
     * // 获取字典ID
     * Integer id = surgeryDictionary.getDictId();
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dict_id")
    private Integer dictId;

    /**
     * 字典名称，最大长度100字符
     * 
     * 数据库字段：dict_name
     * 数据类型：VARCHAR(100)
     * 约束：非空
     * 
     * 手术字典的名称，用于标识和显示
     * 最大长度为100个字符
     * 不能为空
     * 
     * @since 1.0
     * 
     * @example
     * // 设置字典名称
     * surgeryDictionary.setDictName("心脏手术");
     * 
     * @example
     * // 获取字典名称
     * String name = surgeryDictionary.getDictName();
     */
    @Column(name = "dict_name", length = 100)
    private String dictName;

    /**
     * 字典内容，JSON格式存储
     * 
     * 数据库字段：dict_content
     * 数据类型：JSON
     * 约束：可为空
     * 
     * 手术字典的详细内容，以JSON格式存储
     * 可以包含手术的描述、风险等级、操作步骤等信息
     * 如果为空，在Service层会被设置默认值"{}"
     * 
     * @since 1.0
     * 
     * @example
     * // 设置字典内容
     * surgeryDictionary.setDictContent("{\"description\": \"心脏相关手术\", \"riskLevel\": \"high\"}");
     * 
     * @example
     * // 获取字典内容
     * String content = surgeryDictionary.getDictContent();
     */
    @Column(name = "dict_content", columnDefinition = "json")
    private String dictContent;

    /**
     * 科室信息，JSON数组格式存储
     * 
     * 数据库字段：department
     * 数据类型：JSON
     * 约束：可为空
     * 
     * 与该手术字典相关的科室信息，以JSON数组格式存储
     * 可以包含多个科室
     * 如果为空，在Service层会被设置默认值"[]"
     * 
     * @since 1.0
     * 
     * @example
     * // 设置科室信息
     * surgeryDictionary.setDepartment("[\"心血管内科\", \"心脏外科\"]");
     * 
     * @example
     * // 获取科室信息
     * String department = surgeryDictionary.getDepartment();
     */
    @Column(name = "department", columnDefinition = "json")
    private String department;

    /**
     * 组名信息，JSON数组格式存储
     * 
     * 数据库字段：group_name
     * 数据类型：JSON
     * 约束：可为空
     * 
     * 与该手术字典相关的组名信息，以JSON数组格式存储
     * 可以包含多个组名
     * 如果为空，在Service层会被设置默认值"[]"
     * 
     * @since 1.0
     * 
     * @example
     * // 设置组名信息
     * surgeryDictionary.setGroupName("[\"心脏组\", \"血管组\"]");
     * 
     * @example
     * // 获取组名信息
     * String groupName = surgeryDictionary.getGroupName();
     */
    @Column(name = "group_name", columnDefinition = "json")
    private String groupName;
}
