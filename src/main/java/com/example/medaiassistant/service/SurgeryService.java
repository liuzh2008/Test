package com.example.medaiassistant.service;

import com.example.medaiassistant.model.Surgery;
import com.example.medaiassistant.model.EmrRecord;
import com.example.medaiassistant.model.EmrContent;
import com.example.medaiassistant.repository.SurgeryRepository;
import com.example.medaiassistant.repository.EmrRecordRepository;
import com.example.medaiassistant.repository.EmrContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SurgeryService {
    private static final Logger logger = LoggerFactory.getLogger(SurgeryService.class);

    @Autowired
    private SurgeryRepository surgeryRepository;

    @Autowired
    private EmrRecordRepository emrRecordRepository;
    
    @Autowired
    private EmrContentRepository emrContentRepository;

    public List<Surgery> getAllSurgeries() {
        return surgeryRepository.findAll();
    }

    public List<Surgery> getSurgeriesByPatientId(String patientId) {
        return surgeryRepository.findByPatientId(patientId);
    }

/**
 * 检查手术名称是否已存在
 * 
 * @param patientId 患者ID
 * @param surgeryName 手术名称
 * @param surgeryDate 手术日期
 * @return 如果已存在返回true，否则返回false
 * 
 * @description
 * 通过查询数据库检查指定患者、手术名称和手术日期的手术记录是否已存在。
 * 用于避免重复保存相同的手术名称记录。
 * 
 * @process
 * 1. 根据患者ID查询所有手术记录
 * 2. 遍历手术记录，检查是否有匹配的手术名称和手术日期
 * 3. 返回检查结果
 * 
 * @errorHandling
 * - 如果查询过程中发生异常，记录错误日志并返回false
 * - 确保不会因为数据库错误影响主流程
 * 
 * @example
 * boolean exists = surgeryService.isSurgeryNameExists("99050801275226_1", "阑尾切除术", new Date());
 * // 如果存在返回true，否则返回false
 */
    public boolean isSurgeryNameExists(String patientId, String surgeryName, Date surgeryDate) {
        try {
            List<Surgery> existingSurgeries = surgeryRepository.findByPatientId(patientId);
            for (Surgery surgery : existingSurgeries) {
                if (surgeryName.equals(surgery.getSurgeryName()) && 
                    surgeryDate.equals(surgery.getSurgeryDate())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            logger.error("检查手术名称是否存在时发生错误: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 保存手术名称到数据库
     * 
     * @param patientId 患者ID
     * @param surgeryName 手术名称
     * @param surgeryDate 手术日期
     * @param isPrimary 是否为主要手术
     * @param surgeryCode 手术代码
     * @return 保存成功的手术对象
     * 
     * @description
     * 将手术名称信息保存到surgeryname表中，支持设置主要手术标志和手术代码。
     * 该方法会创建新的Surgery实体对象并设置所有必要字段。
     * 
     * @process
     * 1. 创建新的Surgery实体对象
     * 2. 设置患者ID、手术名称、手术日期等字段
     * 3. 设置是否为主要手术（默认为0）
     * 4. 设置手术代码（默认为空字符串）
     * 5. 调用Repository保存到数据库
     * 6. 记录成功日志并返回保存的对象
     * 
     * @errorHandling
     * - 如果保存过程中发生异常，记录详细错误日志
     * - 抛出RuntimeException包含原始异常信息
     * - 确保调用方能够正确处理保存失败的情况
     * 
     * @example
     * Surgery savedSurgery = surgeryService.saveSurgeryName(
     *     "99050801275226_1", 
     *     "阑尾切除术", 
     *     new Date(), 
     *     1, 
     *     "APPENDECTOMY"
     * );
     * // 返回保存成功的手术对象
     */
    public Surgery saveSurgeryName(String patientId, String surgeryName, Date surgeryDate,
                                  Integer isPrimary, String surgeryCode) {
        try {
            Surgery surgery = new Surgery();
            surgery.setPatientId(patientId);
            surgery.setSurgeryName(surgeryName);
            surgery.setSurgeryDate(surgeryDate);
            surgery.setIsPrimary(isPrimary != null ? isPrimary : 0);
            surgery.setSurgeryCode(surgeryCode != null ? surgeryCode : "");
            
            Surgery savedSurgery = surgeryRepository.save(surgery);
            logger.info("成功保存手术名称: {} - 患者: {}", surgeryName, patientId);
            return savedSurgery;
        } catch (Exception e) {
            logger.error("保存手术名称失败: {} - 患者: {}", surgeryName, patientId, e);
            throw new RuntimeException("保存手术名称失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理并保存手术名称分析结果
     * 
     * @param patientId 患者ID
     * @param operationResult 手术分析结果
     * @return 保存的手术记录数量
     * 
     * @description
     * 处理AI分析返回的手术名称结果，进行数据解析、去重检查和数据库保存。
     * 支持批量处理多个手术记录，每个记录包含多个手术名称。
     * 
     * @process
     * 1. 验证手术分析结果格式是否正确
     * 2. 提取SurgeryRecords数组
     * 3. 遍历每个手术记录，解析手术日期和手术名称列表
     * 4. 对每个手术名称进行去重检查
     * 5. 保存新的手术名称到数据库
     * 6. 统计并返回保存的记录数量
     * 
     * @validation
     * - 检查operationResult是否为null
     * - 检查是否包含SurgeryRecords字段
     * - 检查SurgeryRecords是否为List类型
     * - 检查每个手术记录是否包含SurgeryDate和SurgeryName字段
     * 
     * @errorHandling
     * - 如果格式不正确，记录警告日志并返回0
     * - 如果处理过程中发生异常，记录详细错误日志
     * - 抛出RuntimeException包含原始异常信息
     * 
     * @example
     * Map<String, Object> operationResult = Map.of(
     *     "SurgeryRecords", List.of(
     *         Map.of(
     *             "SurgeryDate", "2025-09-19",
     *             "SurgeryName", List.of("阑尾切除术", "腹腔镜探查")
     *         )
     *     )
     * );
     * int savedCount = surgeryService.processAndSaveOperationNames("99050801275226_1", operationResult);
     * // 返回保存的手术记录数量
     */
    public int processAndSaveOperationNames(String patientId, Map<String, Object> operationResult) {
        try {
            logger.info("开始处理并保存手术名称 - 患者ID: {}", patientId);
            
            if (operationResult == null || !operationResult.containsKey("SurgeryRecords")) {
                logger.warn("手术分析结果为空或格式不正确");
                return 0;
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> surgeryRecords = (List<Map<String, Object>>) operationResult.get("SurgeryRecords");
            int savedCount = 0;
            
            for (Map<String, Object> record : surgeryRecords) {
                // 提取手术日期
                String surgeryDateStr = (String) record.get("SurgeryDate");
                Date surgeryDate = parseSurgeryDate(surgeryDateStr);
                
                // 提取手术名称列表
                @SuppressWarnings("unchecked")
                List<String> surgeryNames = (List<String>) record.get("SurgeryName");
                
                if (surgeryNames != null && !surgeryNames.isEmpty()) {
                    for (int i = 0; i < surgeryNames.size(); i++) {
                        String surgeryName = surgeryNames.get(i);
                        
                        // 去重检查
                        if (!isSurgeryNameExists(patientId, surgeryName, surgeryDate)) {
                            // 保存手术名称
                            Integer isPrimary = (i == 0) ? 1 : 0; // 第一个手术为主要手术
                            saveSurgeryName(patientId, surgeryName, surgeryDate, isPrimary, null);
                            savedCount++;
                        } else {
                            logger.info("手术名称已存在，跳过保存: {} - 患者: {}", surgeryName, patientId);
                        }
                    }
                }
            }
            
            logger.info("手术名称处理完成 - 患者: {}, 保存记录数: {}", patientId, savedCount);
            return savedCount;
            
        } catch (Exception e) {
            logger.error("处理并保存手术名称失败 - 患者: {}", patientId, e);
            throw new RuntimeException("处理并保存手术名称失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析手术日期字符串
     * 
     * @param dateStr 日期字符串 (格式: yyyy-MM-dd)
     * @return 解析后的Date对象
     * 
     * @description
     * 将字符串格式的手术日期解析为Date对象，支持yyyy-MM-dd格式。
     * 如果日期字符串为空或解析失败，使用当前日期作为默认值。
     * 
     * @process
     * 1. 检查日期字符串是否为空或空白
     * 2. 如果为空，返回当前日期
     * 3. 使用SimpleDateFormat解析日期字符串
     * 4. 如果解析失败，记录警告日志并使用当前日期
     * 
     * @errorHandling
     * - 如果解析失败，记录警告日志但不抛出异常
     * - 使用当前日期作为默认值，确保流程继续
     * - 避免因为日期格式问题影响整体处理
     * 
     * @example
     * Date surgeryDate = parseSurgeryDate("2025-09-19");
     * // 返回解析后的Date对象
     * 
     * Date defaultDate = parseSurgeryDate(null);
     * // 返回当前日期
     */
    private Date parseSurgeryDate(String dateStr) {
        try {
            if (dateStr == null || dateStr.trim().isEmpty()) {
                return new Date(); // 如果日期为空，使用当前日期
            }
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            return dateFormat.parse(dateStr);
        } catch (Exception e) {
            logger.warn("解析手术日期失败: {}, 使用当前日期", dateStr);
            return new Date();
        }
    }

    /**
     * 清理JSON响应，移除json和标记，确保纯JSON格式
     * 
     * @param jsonResponse AI返回的JSON响应
     * @return 清理后的纯JSON字符串
     * 
     * @description
     * 清理AI返回的JSON响应，移除可能的json标记、```json标记和其他文本包装。
     * 确保返回的是纯JSON格式，便于后续的JSON解析处理。
     * 
     * @process
     * 1. 检查JSON响应是否为空或空白
     * 2. 移除开头的"json"标记（不区分大小写）
     * 3. 移除开头的"```json"标记
     * 4. 移除结尾的"```"标记
     * 5. 移除前后的"```"标记（使用正则表达式）
     * 6. 返回清理后的纯JSON字符串
     * 
     * @cleaningRules
     * - 移除开头的"json"标记（如："json { ... }"）
     * - 移除开头的"```json"标记（如："```json { ... }```"）
     * - 移除结尾的"```"标记
     * - 移除所有前后的"```"标记
     * 
     * @example
     * String cleaned = cleanJsonResponse("json {\"SurgeryRecords\": [...]}");
     * // 返回 "{\"SurgeryRecords\": [...]}"
     * 
     * String cleaned2 = cleanJsonResponse("```json {\"SurgeryRecords\": [...]}```");
     * // 返回 "{\"SurgeryRecords\": [...]}"
     */
    public String cleanJsonResponse(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            return jsonResponse;
        }
        
        // 移除json标记和可能的文本包装
        String cleaned = jsonResponse.trim();
        
        // 如果以json开头，移除json标记
        if (cleaned.toLowerCase().startsWith("json")) {
            cleaned = cleaned.substring(4).trim();
        }
        
        // 移除可能的```json和```标记
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        // 移除前后的```标记
        cleaned = cleaned.replaceAll("^```|```$", "").trim();
        
        return cleaned;
    }
}
