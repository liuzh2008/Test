package com.example.medaiassistant.service;

import com.example.medaiassistant.dto.EmrRecordListDTO;
import com.example.medaiassistant.dto.EmrRecordContentDTO;
import com.example.medaiassistant.model.EmrRecord;
import com.example.medaiassistant.model.EmrContent;
import com.example.medaiassistant.repository.EmrRecordRepository;
import com.example.medaiassistant.repository.EmrContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * EMR_RECORD表业务服务类
 * 提供病历记录相关的业务逻辑处理
 * 
 * @since 1.0.0
 */
@Service
public class EmrRecordService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmrRecordService.class);
    
    @Autowired
    private EmrRecordRepository emrRecordRepository;
    
    @Autowired
    private EmrContentRepository emrContentRepository;
    
    /**
     * 根据PATIENTID查询未删除的病历记录
     * 数据来源：EMR_CONTENT表
     * 
     * @param patientId 患者ID
     * @return 病历记录列表
     */
    public List<EmrContent> getEmrRecordsByPatientId(String patientId) {
        return emrContentRepository.findByPatientId(patientId);
    }

    /**
     * 根据患者ID获取入院记录内容
     * 数据来源：EMR_CONTENT表（替代原来的EMR_RECORD表）
     * 
     * 修改说明：
     * - 添加详细日志记录，帮助诊断入院记录查询问题
     * - 当查询失败时，输出患者所有EMR记录类型以便排查
     * 
     * @param patientId 患者ID
     * @return 入院记录内容列表
     */
    public List<String> getAdmissionRecordContent(String patientId) {
        logger.info("查询患者 {} 的入院记录", patientId);
        List<String> contents = emrContentRepository.findAdmissionRecordContentByPatientId(patientId);
        
        if (contents == null || contents.isEmpty()) {
            logger.warn("患者 {} 未找到入院记录内容，开始诊断...", patientId);
            
            // 检查是否存在入院记录（不包含content字段）
            boolean hasRecord = emrContentRepository.hasAdmissionRecord(patientId);
            logger.warn("患者 {} 是否存在入院记录（不含内容）：{}", patientId, hasRecord);
            
            // 获取该患者所有EMR记录以便排查
            List<EmrContent> allRecords = emrContentRepository.findByPatientId(patientId);
            logger.warn("患者 {} 的所有EMR记录数量：{}", patientId, allRecords.size());
            
            if (!allRecords.isEmpty()) {
                logger.warn("患者 {} 的记录类型列表（用于排查入院记录匹配问题）：", patientId);
                allRecords.stream()
                    .map(EmrContent::getDocTypeName)
                    .distinct()
                    .limit(20)
                    .forEach(docType -> logger.warn("  - 记录类型: [{}]", docType));
                
                // 检查是否有包含"入院"关键字的记录
                long admissionRelatedCount = allRecords.stream()
                    .filter(r -> r.getDocTypeName() != null && r.getDocTypeName().contains("入院"))
                    .count();
                logger.warn("患者 {} 包含'入院'关键字的记录数量：{}", patientId, admissionRelatedCount);
            }
        } else {
            logger.info("患者 {} 成功找到 {} 条入院记录", patientId, contents.size());
        }
        
        return contents != null ? contents : Collections.emptyList();
    }
    
    /**
     * 检查患者是否有入院记录
     * 数据来源：EMR_CONTENT表
     * 
     * @param patientId 患者ID
     * @return 存在返回true，不存在返回false
     */
    public boolean hasAdmissionRecord(String patientId) {
        return emrContentRepository.hasAdmissionRecord(patientId);
    }

    /**
     * 查找手术记录数据
     * 数据来源：EMR_CONTENT表
     * 
     * @param patientId 病人ID
     * @param visitId 就诊

ID（当前实现中未使用，保留参数以符合接口要求）
     * @return 合并后的手术记录内容，如果未找到记录则返回空字符串
     */
    public String findOperations(String patientId, String visitId) {
        try {
            // 通过EmrContentRepository从EMR_CONTENT表获取手术记录
            List<EmrContent> operationRecords = emrContentRepository.findOperationsByPatientId(patientId);
            
            // 调试信息：查看返回的数据
            System.out.println("查询患者 " + patientId + " 的手术记录，返回记录数: " + (operationRecords != null ? operationRecords.size() : 0));
            
            // 使用StringBuilder合并所有手术记录的“段落内容”
            StringBuilder combinedContent = new StringBuilder();
            
            if (operationRecords != null && !operationRecords.isEmpty()) {
                int validContentCount = 0;
                for (EmrContent record : operationRecords) {
                    if (record != null && record.getContent() != null && !record.getContent().trim().isEmpty()) {
                        combinedContent.append(record.getContent()).append("\n\n");
                        validContentCount++;
                    }
                }
                System.out.println("有效内容记录数: " + validContentCount);
            } else {
                System.out.println("未找到手术记录或记录为空");
            }
            
            String result = combinedContent.toString().trim();
            System.out.println("最终返回内容长度: " + result.length());
            return result;
        } catch (Exception e) {
            // 记录错误日志并返回空字符串
            System.err.println("查询手术记录时发生错误: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 根据患者ID和文档类型查询病历记录
     * 数据来源：EMR_CONTENT表
     * 
     * @param patientId 患者ID
     * @param docTypeName 文档类型名称
     * @return 病历记录列表
     */
    public List<EmrContent> getEmrRecordsByPatientIdAndDocType(String patientId, String docTypeName) {
        return emrContentRepository.findByPatientIdAndDocTypeName(patientId, docTypeName);
    }

    /**
     * 根据患者ID和文档类型列表查询病历记录
     * 数据来源：EMR_CONTENT表
     * 
     * @param patientId 患者ID
     * @param docTypeNames 文档类型列表
     * @return 病历记录列表
     */
    public List<EmrContent> getEmrRecordsByPatientIdAndDocTypes(String patientId, List<String> docTypeNames) {
        return emrContentRepository.findByPatientIdAndDocTypeNames(patientId, docTypeNames);
    }

    /**
     * 获取病人病历记录列表
     * 用于接口1：根据病人ID获取病历记录列表
     * 数据来源：EMR_CONTENT表
     * 
     * @param patientId 病人ID
     * @return 病历记录列表DTO
     */
    public List<EmrRecordListDTO> getEmrRecordListByPatientId(String patientId) {
        try {
            List<Object[]> results = emrContentRepository.findByPatientIdAndDeleteMarkZero(patientId);
            List<EmrRecordListDTO> dtoList = new ArrayList<>();
            
            for (Object[] result : results) {
                try {
                    Long id = (Long) result[0];
                    String docTypeName = (String) result[1];
                    java.sql.Timestamp docTitleTime = (java.sql.Timestamp) result[2];
                    
                    EmrRecordListDTO dto = new EmrRecordListDTO(String.valueOf(id), docTypeName, docTitleTime);
                    dtoList.add(dto);
                } catch (ClassCastException e) {
                    // 跳过数据格式错误的记录
                    System.err.println("数据格式错误，跳过记录: " + e.getMessage());
                    continue;
                }
            }
            
            return dtoList;
        } catch (Exception e) {
            System.err.println("获取病历记录列表时发生错误: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 根据ID获取病历记录内容
     * 用于接口2：根据记录ID获取病历内容
     * 数据来源：EMR_CONTENT表
     * 
     * @param id 记录ID
     * @return 病历内容DTO
     */
    public EmrRecordContentDTO getEmrRecordContentById(String id) {
        try {
            Long recordId = Long.parseLong(id);
            String content = emrContentRepository.findContentById(recordId);
            if (content != null) {
                return new EmrRecordContentDTO(content);
            } else {
                return new EmrRecordContentDTO("");
            }
        } catch (NumberFormatException e) {
            System.err.println("ID格式错误，无法转换为Long: " + id);
            return new EmrRecordContentDTO("");
        } catch (Exception e) {
            System.err.println("获取病历记录内容时发生错误: " + e.getMessage());
            e.printStackTrace();
            return new EmrRecordContentDTO("");
        }
    }
}
