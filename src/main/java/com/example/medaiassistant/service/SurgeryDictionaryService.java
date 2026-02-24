package com.example.medaiassistant.service;

import com.example.medaiassistant.model.SurgeryDictionary;
import com.example.medaiassistant.repository.SurgeryDictionaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 手术字典服务类
 * 提供手术字典相关的业务逻辑处理
 */
@Service
public class SurgeryDictionaryService {

    @Autowired
    private SurgeryDictionaryRepository surgeryDictionaryRepository;

    /**
     * 获取所有手术字典记录
     * 
     * 该方法用于获取手术字典表中的所有记录，不进行任何过滤条件
     * 直接调用Repository层的findAllRecords方法来执行数据查询
     * 
     * @return List<SurgeryDictionary> 所有手术字典记录列表
     *         列表中的每个元素包含以下字段：
     *         - dictId: 字典ID (Integer)
     *         - dictName: 字典名称 (String)
     *         - dictContent: 字典内容 (String, JSON格式)
     *         - department: 科室信息 (String, JSON数组格式)
     *         - groupName: 组名信息 (String, JSON数组格式)
     *         
     * @see SurgeryDictionaryRepository#findAllRecords()
     * @since 1.0
     * @author MedAiAssistant
     */
    public List<SurgeryDictionary> getAllSurgeryDictionaries() {
        return surgeryDictionaryRepository.findAllRecords();
    }

    /**
     * 根据条件获取字典内容
     * 
     * 查询逻辑：
     * 1. 如果没有department参数，则返回全部符合dictName的记录
     * 2. 如果有department但没有groupName参数，则返回符合dictName和department的记录
     * 3. 如果三个参数都有，则返回符合所有三个条件的记录
     * 
     * @param dictName 字典名称，必填
     * @param department 科室，可选
     * @param groupName 组名，可选
     * @return 符合条件的字典记录列表
     */
    public List<SurgeryDictionary> getDictionaryContent(String dictName, String department, String groupName) {
        // 如果没有department，则返回全部符合dict_name的记录
        if (department == null || department.isEmpty()) {
            return surgeryDictionaryRepository.findByDictName(dictName);
        }
        
        // 如果有department但没有group_name，则返回符合dict_name、department的记录
        if (groupName == null || groupName.isEmpty()) {
            return surgeryDictionaryRepository.findByDictNameAndDepartment(dictName, department);
        }
        
        // 如果三个参数都有，则返回符合所有三个条件的记录
        return surgeryDictionaryRepository.findByDictNameAndDepartmentAndGroupName(dictName, department, groupName);
    }
    
    /**
     * 新增手术字典记录
     * 
     * 该方法用于向手术字典表中添加新的记录，包含对空值的处理逻辑，避免JSON数据截断错误
     * 此方法会创建新的手术字典记录，并对JSON格式的字段进行特殊处理以避免数据库错误
     * 
     * 处理逻辑：
     * 1. 创建新的SurgeryDictionary对象
     * 2. 设置dictName字段
     * 3. 对于dictContent、department、groupName三个JSON字段，分别进行空值检查：
     *    - 如果字段值不为空，则直接使用传入的值
     *    - 如果字段值为空（null或空字符串），则设置默认值以避免数据库JSON格式错误
     * 4. 保存新记录到数据库
     * 5. 返回新增记录的字典ID
     * 
     * @param dictName 字典名称，最大长度100字符
     *                 数据类型：String
     *                 数据库字段：dict_name
     *                 可为空：否
     *                 
     * @param dictContent 字典内容，JSON格式存储，如果为空则设置默认值"{}"
     *                   数据类型：String (JSON格式)
     *                   数据库字段：dict_content
     *                   可为空：是（但会设置默认值"{}"）
     *                   
     * @param department 科室信息，JSON数组格式存储，如果为空则设置默认值"[]"
     *                  数据类型：String (JSON数组格式)
     *                  数据库字段：department
     *                  可为空：是（但会设置默认值"[]"）
     *                  
     * @param groupName 组名信息，JSON数组格式存储，如果为空则设置默认值"[]"
     *                 数据类型：String (JSON数组格式)
     *                 数据库字段：group_name
     *                 可为空：是（但会设置默认值"[]"）
     *                 
     * @return 新增记录的字典ID
     *         数据类型：Integer
     *         成功时：返回新增记录的dictId
     *         
     * @apiNote 此方法会处理空值情况，确保数据库中的JSON字段不会出现空值导致的错误
     *          对于JSON字段，空值会被替换为适当的默认值以保持数据库完整性
     *          
     * @implSpec 对于可选的JSON字段（dictContent、department、groupName），如果传入空值或null，系统会自动设置默认值
     *           这样可以避免数据库出现"Data truncation: Invalid JSON text"错误
     *           
     * @implNote dictContent默认值为"{}"（空JSON对象），department和groupName默认值为"[]"（空JSON数组）
     *           这种设计确保了即使在没有提供数据的情况下，数据库字段也能保持有效的JSON格式
     *           
     * @example
     * // 调用示例
     * Integer dictId = surgeryDictionaryService.addSurgeryDictionary(
     *     "心脏手术",
     *     "{\"description\": \"心脏相关手术\", \"riskLevel\": \"high\"}",
     *     "[\"心血管内科\", \"心脏外科\"]",
     *     "[\"心脏组\", \"血管组\"]"
     * );
     * 
     * @example
     * // 空值处理示例
     * Integer dictId = surgeryDictionaryService.addSurgeryDictionary(
     *     "心脏手术",
     *     null,  // 将被设置为"{}"
     *     "",    // 将被设置为"[]"
     *     null   // 将被设置为"[]"
     * );
     * 
     * @since 1.0
     * @author MedAiAssistant
     */
    public Integer addSurgeryDictionary(String dictName, String dictContent, String department, String groupName) {
        SurgeryDictionary surgeryDictionary = new SurgeryDictionary();
        surgeryDictionary.setDictName(dictName);
        
        // 处理dictContent空值情况
        if (dictContent != null && !dictContent.isEmpty()) {
            surgeryDictionary.setDictContent(dictContent);
        } else {
            surgeryDictionary.setDictContent("{}"); // 设置默认空JSON对象
        }
        
        // 处理department空值情况
        if (department != null && !department.isEmpty()) {
            surgeryDictionary.setDepartment(department);
        } else {
            surgeryDictionary.setDepartment("[]"); // 设置默认空JSON数组
        }
        
        // 处理groupName空值情况
        if (groupName != null && !groupName.isEmpty()) {
            surgeryDictionary.setGroupName(groupName);
        } else {
            surgeryDictionary.setGroupName("[]"); // 设置默认空JSON数组
        }
        
        SurgeryDictionary savedDictionary = surgeryDictionaryRepository.save(surgeryDictionary);
        return savedDictionary.getDictId();
    }
    
    /**
     * 更新手术字典记录
     * 
     * 该方法用于更新手术字典表中的记录，包含对空值的处理逻辑，避免JSON数据截断错误
     * 此方法会根据提供的字典ID查找对应的记录，如果找到则更新记录的各个字段
     * 对于JSON格式的字段（dictContent、department、groupName），会进行特殊处理以避免数据库错误
     * 
     * 处理逻辑：
     * 1. 根据dictId查找对应的手术字典记录
     * 2. 如果记录存在，则更新dictName字段
     * 3. 对于dictContent、department、groupName三个JSON字段，分别进行空值检查：
     *    - 如果字段值不为空，则直接使用传入的值
     *    - 如果字段值为空（null或空字符串），则设置默认值以避免数据库JSON格式错误
     * 4. 保存更新后的记录到数据库
     * 5. 返回更新记录的字典ID
     * 
     * @param dictId 字典ID，用于定位要更新的记录，不能为空
     *               数据类型：Integer
     *               数据库字段：dict_id (主键)
     *               
     * @param dictName 字典名称，最大长度100字符
     *                 数据类型：String
     *                 数据库字段：dict_name
     *                 可为空：否
     *                 
     * @param dictContent 字典内容，JSON格式存储，如果为空则设置默认值"{}"
     *                   数据类型：String (JSON格式)
     *                   数据库字段：dict_content
     *                   可为空：是（但会设置默认值"{}"）
     *                   
     * @param department 科室信息，JSON数组格式存储，如果为空则设置默认值"[]"
     *                  数据类型：String (JSON数组格式)
     *                  数据库字段：department
     *                  可为空：是（但会设置默认值"[]"）
     *                  
     * @param groupName 组名信息，JSON数组格式存储，如果为空则设置默认值"[]"
     *                 数据类型：String (JSON数组格式)
     *                 数据库字段：group_name
     *                 可为空：是（但会设置默认值"[]"）
     *                 
     * @return 更新记录的字典ID，如果未找到对应记录则返回null
     *         数据类型：Integer
     *         成功时：返回更新记录的dictId
     *         失败时：返回null（当根据dictId未找到对应记录时）
     *         
     * @throws IllegalArgumentException 如果dictId为null
     * 
     * @apiNote 此方法会处理空值情况，确保数据库中的JSON字段不会出现空值导致的错误
     *          对于JSON字段，空值会被替换为适当的默认值以保持数据库完整性
     *          
     * @implSpec 对于可选的JSON字段（dictContent、department、groupName），如果传入空值或null，系统会自动设置默认值
     *           这样可以避免数据库出现"Data truncation: Invalid JSON text"错误
     *           
     * @implNote dictContent默认值为"{}"（空JSON对象），department和groupName默认值为"[]"（空JSON数组）
     *           这种设计确保了即使在没有提供数据的情况下，数据库字段也能保持有效的JSON格式
     *           
     * @example
     * // 调用示例
     * Integer dictId = surgeryDictionaryService.updateSurgeryDictionary(
     *     1,
     *     "心脏手术",
     *     "{\"description\": \"心脏相关手术\", \"riskLevel\": \"high\"}",
     *     "[\"心血管内科\", \"心脏外科\"]",
     *     "[\"心脏组\", \"血管组\"]"
     * );
     * 
     * @example
     * // 空值处理示例
     * Integer dictId = surgeryDictionaryService.updateSurgeryDictionary(
     *     1,
     *     "心脏手术",
     *     null,  // 将被设置为"{}"
     *     "",    // 将被设置为"[]"
     *     null   // 将被设置为"[]"
     * );
     * 
     * @since 1.0
     * @author MedAiAssistant
     */
    public Integer updateSurgeryDictionary(Integer dictId, String dictName, String dictContent, String department, String groupName) {
        SurgeryDictionary surgeryDictionary = surgeryDictionaryRepository.findById(dictId).orElse(null);
        if (surgeryDictionary != null) {
            surgeryDictionary.setDictName(dictName);
            
            // 处理dictContent空值情况
            if (dictContent != null && !dictContent.isEmpty()) {
                surgeryDictionary.setDictContent(dictContent);
            } else {
                surgeryDictionary.setDictContent("{}"); // 设置默认空JSON对象
            }
            
            // 处理department空值情况
            if (department != null && !department.isEmpty()) {
                surgeryDictionary.setDepartment(department);
            } else {
                surgeryDictionary.setDepartment("[]"); // 设置默认空JSON数组
            }
            
            // 处理groupName空值情况
            if (groupName != null && !groupName.isEmpty()) {
                surgeryDictionary.setGroupName(groupName);
            } else {
                surgeryDictionary.setGroupName("[]"); // 设置默认空JSON数组
            }
            
            SurgeryDictionary updatedDictionary = surgeryDictionaryRepository.save(surgeryDictionary);
            return updatedDictionary.getDictId();
        }
        return null;
    }
    
    /**
     * 根据字典ID删除手术字典记录
     * 
     * 该方法用于根据字典ID删除手术字典表中的记录
     * 
     * @param dictId 字典ID
     * @return 删除的记录数，如果为0表示没有找到对应的记录
     * 
     * @since 1.0
     * @author MedAiAssistant
     */
    public int deleteSurgeryDictionary(Integer dictId) {
        return surgeryDictionaryRepository.deleteByDictId(dictId);
    }
}
