package com.example.medaiassistant.controller;

import com.example.medaiassistant.dto.SurgeryDictionaryDTO;
import com.example.medaiassistant.model.SurgeryDictionary;
import com.example.medaiassistant.service.SurgeryDictionaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 手术字典控制器
 * 提供手术字典相关的API接口
 * 
 * @api {get} /api/surgery-dictionary/all 获取所有手术字典记录
 * @api {get} /api/surgery-dictionary/content 根据条件获取字典内容
 * @api {post} /api/surgery-dictionary/add 新增手术字典记录
 * @api {put} /api/surgery-dictionary/update/{dictId} 更新手术字典记录(路径变量)
 * @api {put} /api/surgery-dictionary/update 更新手术字典记录(查询参数)
 * @api {delete} /api/surgery-dictionary/delete/{dictId} 删除手术字典记录
 * 
 * @since 1.0
 * @author MedAiAssistant
 */
@RestController
@RequestMapping("/api/surgery-dictionary")
public class SurgeryDictionaryController {

    @Autowired
    private SurgeryDictionaryService surgeryDictionaryService;

    /**
     * 获取所有手术字典记录
     * 
     * 接口路径: GET /api/surgery-dictionary/all
     * 
     * 该接口用于获取手术字典表中的所有记录，不进行任何过滤条件
     * 直接调用Service层的getAllSurgeryDictionaries方法来执行业务逻辑
     * 
     * @return List<SurgeryDictionary> 所有手术字典记录列表
     *         列表中的每个元素包含以下字段：
     *         - dictId: 字典ID (Integer)
     *         - dictName: 字典名称 (String)
     *         - dictContent: 字典内容 (String, JSON格式)
     *         - department: 科室信息 (String, JSON数组格式)
     *         - groupName: 组名信息 (String, JSON数组格式)
     *         
     * @apiExample {curl} 示例请求:
     *     curl -X GET http://localhost:8080/api/surgery-dictionary/all
     * 
     * @apiSuccess {Array} array 手术字典记录数组
     * @apiSuccess {Integer} array.dictId 字典ID
     * @apiSuccess {String} array.dictName 字典名称
     * @apiSuccess {String} array.dictContent 字典内容(JSON格式)
     * @apiSuccess {String} array.department 科室信息(JSON数组格式)
     * @apiSuccess {String} array.groupName 组名信息(JSON数组格式)
     * 
     * @apiSuccessExample {json} 成功响应示例:
     *     HTTP/1.1 200 OK
     *     [
     *       {
     *         "dictId": 1,
     *         "dictName": "心脏手术",
     *         "dictContent": "{\"description\": \"心脏相关手术\", \"riskLevel\": \"high\"}",
     *         "department": "[\"心血管内科\", \"心脏外科\"]",
     *         "groupName": "[\"心脏组\", \"血管组\"]"
     *       },
     *       {
     *         "dictId": 2,
     *         "dictName": "骨科手术",
     *         "dictContent": "{\"description\": \"骨骼相关手术\", \"riskLevel\": \"medium\"}",
     *         "department": "[\"骨科\"]",
     *         "groupName": "[\"脊柱组\", \"关节组\"]"
     *       }
     *     ]
     * 
     * @see SurgeryDictionaryService#getAllSurgeryDictionaries()
     * @since 1.0
     * @author MedAiAssistant
     */
    @GetMapping("/all")
    public List<SurgeryDictionary> getAllSurgeryDictionaries() {
        return surgeryDictionaryService.getAllSurgeryDictionaries();
    }

    /**
     * 根据条件获取字典内容
     * 
     * 接口路径: GET /api/surgery-dictionary/content
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
     * 
     * 示例请求：
     * 1. 仅根据字典名称查询:
     *    GET /api/surgery-dictionary/content?dictName=手术名称字典
     *    
     * 2. 根据字典名称和科室查询:
     *    GET /api/surgery-dictionary/content?dictName=手术名称字典&department=心血管内科
     *    
     * 3. 根据字典名称、科室和组名查询:
     *    GET /api/surgery-dictionary/content?dictName=手术名称字典&department=心血管内科&groupName=心脏组
     */
    @GetMapping("/content")
    public List<SurgeryDictionary> getDictionaryContent(
            @RequestParam String dictName,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String groupName) {
        return surgeryDictionaryService.getDictionaryContent(dictName, department, groupName);
    }
    
    /**
     * 新增手术字典记录
     * 
     * 接口路径: POST /api/surgery-dictionary/add
     * 
     * 该接口用于向手术字典表中添加新的记录
     * 
     * @param {SurgeryDictionaryDTO} surgeryDictionaryDTO - 手术字典DTO对象
     * @param {string} surgeryDictionaryDTO.dictName - 字典名称，最大长度100字符
     * @param {string} surgeryDictionaryDTO.dictContent - 字典内容，JSON格式存储
     * @param {string} surgeryDictionaryDTO.department - 科室信息，JSON数组格式存储
     * @param {string} surgeryDictionaryDTO.groupName - 组名信息，JSON数组格式存储
     * @returns {Integer} 新增记录的字典ID
     * 
     * @example
     * // 请求示例
     * POST /api/surgery-dictionary/add
     * Content-Type: application/json
     * {
     *   "dictName": "心脏手术",
     *   "dictContent": "{\"description\": \"心脏相关手术\", \"riskLevel\": \"high\"}",
     *   "department": "[\"心血管内科\", \"心脏外科\"]",
     *   "groupName": "[\"心脏组\", \"血管组\"]"
     * }
     * 
     * @example
     * // 响应示例
     * HTTP/1.1 200 OK
     * 1
     * 
     * @apiSuccess {Integer} dictId 新增记录的字典ID
     * @apiSuccessExample {json} 成功响应:
     *     HTTP/1.1 200 OK
     *     1
     *     
     * @apiError (Error 400) BadRequest 请求参数缺失或格式错误
     * @apiError (Error 500) InternalServerError 服务器内部错误
     * 
     * @since 1.0
     * @author MedAiAssistant
     */
    @PostMapping("/add")
    public Integer addSurgeryDictionary(@RequestBody SurgeryDictionaryDTO surgeryDictionaryDTO) {
        return surgeryDictionaryService.addSurgeryDictionary(
                surgeryDictionaryDTO.getDictName(),
                surgeryDictionaryDTO.getDictContent(),
                surgeryDictionaryDTO.getDepartment(),
                surgeryDictionaryDTO.getGroupName());
    }
    
    /**
     * 更新手术字典记录（使用路径变量）
     * 
     * 接口路径: PUT /api/surgery-dictionary/update/{dictId}
     * 
     * 该接口用于更新手术字典表中的记录，通过路径变量传递字典ID
     * 此方法会调用Service层的updateSurgeryDictionary方法执行具体的业务逻辑
     * 对于JSON格式的字段（dictContent、department、groupName），Service层会进行特殊处理以避免数据库错误
     * 
     * 处理逻辑：
     * 1. 从路径变量中获取dictId参数
     * 2. 从请求体中获取SurgeryDictionaryDTO对象，包含要更新的字段值
     * 3. 调用performUpdate私有方法执行更新操作
     * 4. 如果更新成功，返回更新记录的字典ID
     * 5. 如果未找到对应记录，抛出运行时异常
     * 
     * @param {Integer} dictId - 字典ID（路径变量）
     *                         数据类型：Integer
     *                         必填：是
     *                         位置：URL路径变量
     *                         说明：用于定位要更新的记录的唯一标识符
     *                         
     * @param {SurgeryDictionaryDTO} surgeryDictionaryDTO - 手术字典DTO对象（请求体）
     *                                                     数据类型：SurgeryDictionaryDTO
     *                                                     必填：是
     *                                                     位置：请求体（@RequestBody）
     *                                                     说明：包含要更新的字段值的对象
     *                                                     
     * @param {string} surgeryDictionaryDTO.dictName - 字典名称，最大长度100字符
     *                                                数据类型：String
     *                                                必填：是
     *                                                
     * @param {string} surgeryDictionaryDTO.dictContent - 字典内容，JSON格式存储
     *                                                  数据类型：String (JSON格式)
     *                                                  必填：否（但会设置默认值"{}"）
     *                                                  
     * @param {string} surgeryDictionaryDTO.department - 科室信息，JSON数组格式存储
     *                                                 数据类型：String (JSON数组格式)
     *                                                 必填：否（但会设置默认值"[]"）
     *                                                 
     * @param {string} surgeryDictionaryDTO.groupName - 组名信息，JSON数组格式存储
     *                                                数据类型：String (JSON数组格式)
     *                                                必填：否（但会设置默认值"[]"）
     *                                                
     * @returns {Integer} 更新记录的字典ID
     *                   数据类型：Integer
     *                   成功时：返回更新记录的dictId
     *                   失败时：抛出RuntimeException（当根据dictId未找到对应记录时）
     *                   
     * @throws RuntimeException 如果根据dictId未找到对应记录
     *                         异常信息格式："未找到ID为 {dictId} 的手术字典记录"
     *                         
     * @apiNote 此接口支持JSON格式的请求体，Content-Type应设置为application/json
     *          对于JSON字段，空值会被Service层替换为适当的默认值以保持数据库完整性
     *          
     * @implSpec 此方法通过调用performUpdate私有方法来执行更新操作
     *           performUpdate方法会进一步调用Service层的updateSurgeryDictionary方法
     *           
     * @implNote 如果Service层返回null（表示未找到对应记录），此方法会抛出运行时异常
     *           这样可以确保API调用者能够清楚地知道更新操作是否成功
     *           
     * @example
     * // 请求示例（使用路径变量）
     * PUT /api/surgery-dictionary/update/1
     * Content-Type: application/json
     * {
     *   "dictName": "心脏手术",
     *   "dictContent": "{\"description\": \"心脏相关手术\", \"riskLevel\": \"high\"}",
     *   "department": "[\"心血管内科\", \"心脏外科\"]",
     *   "groupName": "[\"心脏组\", \"血管组\"]"
     * }
     * 
     * @example
     * // 响应示例（成功）
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     * 1
     * 
     * @example
     * // 响应示例（未找到记录）
     * HTTP/1.1 500 Internal Server Error
     * Content-Type: application/json
     * {
     *   "timestamp": "2025-08-19T10:30:00.000+00:00",
     *   "status": 500,
     *   "error": "Internal Server Error",
     *   "message": "未找到ID为 1 的手术字典记录",
     *   "path": "/api/surgery-dictionary/update/1"
     * }
     * 
     * @apiSuccess {Integer} dictId 更新记录的字典ID
     * @apiSuccessExample {json} 成功响应:
     *     HTTP/1.1 200 OK
     *     Content-Type: application/json
     *     1
     *     
     * @apiError (Error 400) BadRequest 请求参数缺失或格式错误
     * @apiError (Error 500) InternalServerError 服务器内部错误（如未找到记录）
     * 
     * @since 1.0
     * @author MedAiAssistant
     */
    @PutMapping(value = "/update/{dictId}", name = "updateWithPathVariable")
    public Integer updateSurgeryDictionaryWithPathVariable(@PathVariable Integer dictId, @RequestBody SurgeryDictionaryDTO surgeryDictionaryDTO) {
        return performUpdate(dictId, surgeryDictionaryDTO);
    }
    
    /**
     * 更新手术字典记录（使用查询参数）
     * 
     * 接口路径: PUT /api/surgery-dictionary/update?dictId={dictId}
     * 
     * 该接口用于更新手术字典表中的记录，通过查询参数传递字典ID
     * 此方法会调用Service层的updateSurgeryDictionary方法执行具体的业务逻辑
     * 对于JSON格式的字段（dictContent、department、groupName），Service层会进行特殊处理以避免数据库错误
     * 
     * 处理逻辑：
     * 1. 从查询参数中获取dictId参数
     * 2. 从请求体中获取SurgeryDictionaryDTO对象，包含要更新的字段值
     * 3. 调用performUpdate私有方法执行更新操作
     * 4. 如果更新成功，返回更新记录的字典ID
     * 5. 如果未找到对应记录，抛出运行时异常
     * 
     * @param {Integer} dictId - 字典ID（查询参数）
     *                         数据类型：Integer
     *                         必填：是
     *                         位置：URL查询参数
     *                         说明：用于定位要更新的记录的唯一标识符
     *                         
     * @param {SurgeryDictionaryDTO} surgeryDictionaryDTO - 手术字典DTO对象（请求体）
     *                                                     数据类型：SurgeryDictionaryDTO
     *                                                     必填：是
     *                                                     位置：请求体（@RequestBody）
     *                                                     说明：包含要更新的字段值的对象
     *                                                     
     * @param {string} surgeryDictionaryDTO.dictName - 字典名称，最大长度100字符
     *                                                数据类型：String
     *                                                必填：是
     *                                                
     * @param {string} surgeryDictionaryDTO.dictContent - 字典内容，JSON格式存储
     *                                                  数据类型：String (JSON格式)
     *                                                  必填：否（但会设置默认值"{}"）
     *                                                  
     * @param {string} surgeryDictionaryDTO.department - 科室信息，JSON数组格式存储
     *                                                 数据类型：String (JSON数组格式)
     *                                                 必填：否（但会设置默认值"[]"）
     *                                                 
     * @param {string} surgeryDictionaryDTO.groupName - 组名信息，JSON数组格式存储
     *                                                数据类型：String (JSON数组格式)
     *                                                必填：否（但会设置默认值"[]"）
     *                                                
     * @returns {Integer} 更新记录的字典ID
     *                   数据类型：Integer
     *                   成功时：返回更新记录的dictId
     *                   失败时：抛出RuntimeException（当根据dictId未找到对应记录时）
     *                   
     * @throws RuntimeException 如果根据dictId未找到对应记录
     *                         异常信息格式："未找到ID为 {dictId} 的手术字典记录"
     *                         
     * @apiNote 此接口支持JSON格式的请求体，Content-Type应设置为application/json
     *          对于JSON字段，空值会被Service层替换为适当的默认值以保持数据库完整性
     *          
     * @implSpec 此方法通过调用performUpdate私有方法来执行更新操作
     *           performUpdate方法会进一步调用Service层的updateSurgeryDictionary方法
     *           
     * @implNote 如果Service层返回null（表示未找到对应记录），此方法会抛出运行时异常
     *           这样可以确保API调用者能够清楚地知道更新操作是否成功
     *           
     * @example
     * // 请求示例（使用查询参数）
     * PUT /api/surgery-dictionary/update?dictId=1
     * Content-Type: application/json
     * {
     *   "dictName": "心脏手术",
     *   "dictContent": "{\"description\": \"心脏相关手术\", \"riskLevel\": \"high\"}",
     *   "department": "[\"心血管内科\", \"心脏外科\"]",
     *   "groupName": "[\"心脏组\", \"血管组\"]"
     * }
     * 
     * @example
     * // 响应示例（成功）
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     * 1
     * 
     * @example
     * // 响应示例（未找到记录）
     * HTTP/1.1 500 Internal Server Error
     * Content-Type: application/json
     * {
     *   "timestamp": "2025-08-19T10:30:00.000+00:00",
     *   "status": 500,
     *   "error": "Internal Server Error",
     *   "message": "未找到ID为 1 的手术字典记录",
     *   "path": "/api/surgery-dictionary/update?dictId=1"
     * }
     * 
     * @apiSuccess {Integer} dictId 更新记录的字典ID
     * @apiSuccessExample {json} 成功响应:
     *     HTTP/1.1 200 OK
     *     Content-Type: application/json
     *     1
     *     
     * @apiError (Error 400) BadRequest 请求参数缺失或格式错误
     * @apiError (Error 500) InternalServerError 服务器内部错误（如未找到记录）
     * 
     * @since 1.0
     * @author MedAiAssistant
     */
    @PutMapping("/update")
    public Integer updateSurgeryDictionaryWithRequestParam(@RequestParam Integer dictId, @RequestBody SurgeryDictionaryDTO surgeryDictionaryDTO) {
        return performUpdate(dictId, surgeryDictionaryDTO);
    }
    
    private Integer performUpdate(Integer dictId, SurgeryDictionaryDTO surgeryDictionaryDTO) {
        Integer result = surgeryDictionaryService.updateSurgeryDictionary(
                dictId,
                surgeryDictionaryDTO.getDictName(),
                surgeryDictionaryDTO.getDictContent(),
                surgeryDictionaryDTO.getDepartment(),
                surgeryDictionaryDTO.getGroupName());
        
        if (result == null) {
            // 如果返回null，说明没有找到对应的记录
            throw new RuntimeException("未找到ID为 " + dictId + " 的手术字典记录");
        }
        
        return result;
    }
    
    /**
     * 根据字典ID删除手术字典记录
     * 
     * 接口路径: DELETE /api/surgery-dictionary/delete/{dictId}
     * 
     * 该接口用于根据字典ID删除手术字典表中的记录
     * 此方法会调用Service层的deleteSurgeryDictionary方法执行具体的业务逻辑
     * 
     * 处理逻辑：
     * 1. 从路径变量中获取dictId参数
     * 2. 调用Service层的deleteSurgeryDictionary方法执行删除操作
     * 3. 返回删除的记录数
     * 
     * @param {Integer} dictId - 字典ID（路径变量）
     *                         数据类型：Integer
     *                         必填：是
     *                         位置：URL路径变量
     *                         说明：用于定位要删除的记录的唯一标识符
     *                         
     * @return {int} 删除的记录数
     *              数据类型：int
     *              成功时：返回删除的记录数（0表示没有找到对应的记录）
     *              
     * @apiNote 此接口通过HTTP DELETE方法调用，路径参数为字典ID
     *          
     * @implSpec 此方法直接调用Service层的deleteSurgeryDictionary方法执行删除操作
     *           
     * @implNote 删除操作是幂等的，即使多次调用同一个ID，也不会产生副作用
     *           如果要删除的记录不存在，方法会返回0
     *           
     * @example
     * // 请求示例
     * DELETE /api/surgery-dictionary/delete/1
     * 
     * @example
     * // 响应示例（成功删除）
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     * 1
     * 
     * @example
     * // 响应示例（未找到记录）
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     * 0
     * 
     * @apiSuccess {Integer} count 删除的记录数，如果为0表示没有找到对应的记录
     * @apiSuccessExample {json} 成功响应（删除记录）:
     *     HTTP/1.1 200 OK
     *     Content-Type: application/json
     *     1
     *     
     * @apiSuccessExample {json} 成功响应（未找到记录）:
     *     HTTP/1.1 200 OK
     *     Content-Type: application/json
     *     0
     *     
     * @apiError (Error 400) BadRequest 请求参数缺失或格式错误
     * @apiError (Error 500) InternalServerError 服务器内部错误
     * 
     * @apiExample {curl} 示例请求:
     *     curl -X DELETE http://localhost:8080/api/surgery-dictionary/delete/1
     * 
     * @since 1.0
     * @author MedAiAssistant
     */
    @DeleteMapping("/delete/{dictId}")
    public int deleteSurgeryDictionary(@PathVariable Integer dictId) {
        return surgeryDictionaryService.deleteSurgeryDictionary(dictId);
    }
}
