package com.example.medaiassistant.dto;

import lombok.Data;

/**
 * 手术字典DTO类
 * 用于接收手术字典记录的请求参数
 * 
 * 该类用于在API接口中传输手术字典相关的数据
 * 包含了手术字典的所有字段，用于新增和更新操作
 * 使用Lombok的@Data注解自动生成getter、setter、toString等方法
 * 
 * 使用场景：
 * 1. 新增手术字典记录时，作为请求体参数
 * 2. 更新手术字典记录时，作为请求体参数
 * 3. 在Controller和Service层之间传递数据
 * 
 * @since 1.0
 * @author MedAiAssistant
 * 
 * @example
 * // 创建DTO实例
 * SurgeryDictionaryDTO dto = new SurgeryDictionaryDTO();
 * dto.setDictName("心脏手术");
 * dto.setDictContent("{\"description\": \"心脏相关手术\", \"riskLevel\": \"high\"}");
 * dto.setDepartment("[\"心血管内科\", \"心脏外科\"]");
 * dto.setGroupName("[\"心脏组\", \"血管组\"]");
 * 
 * @example
 * // 使用Lombok生成的getter方法
 * String name = dto.getDictName();
 * String content = dto.getDictContent();
 */
@Data
public class SurgeryDictionaryDTO {
    /**
     * 字典ID
     * 用于唯一标识一条手术字典记录
     * 在更新操作中用于定位要更新的记录
     * 在新增操作中通常为空或不设置
     * 
     * @type {Integer}
     * @since 1.0
     * 
     * @example
     * // 设置字典ID
     * surgeryDictionaryDTO.setDictId(1);
     * 
     * @example
     * // 获取字典ID
     * Integer id = surgeryDictionaryDTO.getDictId();
     */
    private Integer dictId;
    
    /**
     * 字典名称
     * 手术字典的名称，用于标识和显示
     * 最大长度为100个字符
     * 不能为空
     * 
     * @type {string}
     * @maxLength 100
     * @since 1.0
     * 
     * @example
     * // 设置字典名称
     * surgeryDictionaryDTO.setDictName("心脏手术");
     * 
     * @example
     * // 获取字典名称
     * String name = surgeryDictionaryDTO.getDictName();
     */
    private String dictName;
    
    /**
     * 字典内容 (JSON格式)
     * 手术字典的详细内容，以JSON格式存储
     * 可以包含手术的描述、风险等级、操作步骤等信息
     * 如果为空，在Service层会被设置默认值"{}"
     * 
     * @type {string}
     * @format JSON
     * @since 1.0
     * 
     * @example
     * // 设置字典内容
     * surgeryDictionaryDTO.setDictContent("{\"description\": \"心脏相关手术\", \"riskLevel\": \"high\"}");
     * 
     * @example
     * // 获取字典内容
     * String content = surgeryDictionaryDTO.getDictContent();
     */
    private String dictContent;
    
    /**
     * 科室信息 (JSON格式)
     * 与该手术字典相关的科室信息，以JSON数组格式存储
     * 可以包含多个科室
     * 如果为空，在Service层会被设置默认值"[]"
     * 
     * @type {string}
     * @format JSON Array
     * @since 1.0
     * 
     * @example
     * // 设置科室信息
     * surgeryDictionaryDTO.setDepartment("[\"心血管内科\", \"心脏外科\"]");
     * 
     * @example
     * // 获取科室信息
     * String department = surgeryDictionaryDTO.getDepartment();
     */
    private String department;
    
    /**
     * 组名信息 (JSON格式)
     * 与该手术字典相关的组名信息，以JSON数组格式存储
     * 可以包含多个组名
     * 如果为空，在Service层会被设置默认值"[]"
     * 
     * @type {string}
     * @format JSON Array
     * @since 1.0
     * 
     * @example
     * // 设置组名信息
     * surgeryDictionaryDTO.setGroupName("[\"心脏组\", \"血管组\"]");
     * 
     * @example
     * // 获取组名信息
     * String groupName = surgeryDictionaryDTO.getGroupName();
     */
    private String groupName;
}
