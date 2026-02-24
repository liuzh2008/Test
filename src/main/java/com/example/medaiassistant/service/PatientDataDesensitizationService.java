package com.example.medaiassistant.service;

import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 患者数据脱敏服务类
 * 
 * 该类负责从patients表中获取患者敏感信息，并在文本中进行脱敏处理。
 * 主要用于在保存Prompt时对患者隐私信息进行保护，将敏感数据替换为"***"。
 * 
 * 主要功能包括：
 * - 根据患者ID查询敏感信息（姓名、身份证号、病历号、患者编号、床位号等）
 * - 对文本内容进行精确的敏感信息替换
 * - 特殊处理床位号以避免误替换年龄等数字信息
 * - 支持批量脱敏处理
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2025-09-01
 */
@Service
public class PatientDataDesensitizationService {

    private final PatientRepository patientRepository;

    public PatientDataDesensitizationService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    /**
     * 根据患者ID获取敏感信息映射表
     * 
     * 该方法通过患者ID从patients表中查询患者的敏感信息，包括姓名、身份证号、
     * 病历号、患者编号、床位号等信息，并返回一个包含这些信息的映射表。
     * 
     * @param patientId 患者ID，用于查询患者信息
     * @return Map<String, String> 敏感信息映射表，key为敏感信息类型（如"姓名"、"身份证号"等），
     *         value为对应的敏感信息值。如果患者不存在或信息为空，返回空映射表。
     * @throws RuntimeException 如果数据库查询过程中发生异常
     */
    public Map<String, String> getSensitiveInfo(String patientId) {
        Map<String, String> sensitiveInfo = new HashMap<>();

        Optional<Patient> patientOpt = patientRepository.findById(patientId);
        if (patientOpt.isPresent()) {
            Patient patient = patientOpt.get();

            // 添加需要脱敏的敏感信息
            if (patient.getName() != null && !patient.getName().trim().isEmpty()) {
                sensitiveInfo.put("姓名", patient.getName());
            }
            if (patient.getIdCard() != null && !patient.getIdCard().trim().isEmpty()) {
                sensitiveInfo.put("身份证号", patient.getIdCard());
            }
            if (patient.getMedicalRecordNumber() != null && !patient.getMedicalRecordNumber().trim().isEmpty()) {
                sensitiveInfo.put("病历号", patient.getMedicalRecordNumber());
            }
            if (patient.getPatientNo() != null && !patient.getPatientNo().trim().isEmpty()) {
                sensitiveInfo.put("患者编号", patient.getPatientNo());
            }
            if (patient.getBedNumber() != null && !patient.getBedNumber().trim().isEmpty()) {
                sensitiveInfo.put("床位号", patient.getBedNumber());
            }
            if (patient.getPatientId() != null && !patient.getPatientId().trim().isEmpty()) {
                sensitiveInfo.put("患者ID", patient.getPatientId());
            }
        }

        return sensitiveInfo;
    }

    /**
     * 对文本进行脱敏处理
     * 
     * 该方法接收原始文本和敏感信息映射表，对文本中的所有敏感信息进行替换处理。
     * 敏感信息将被替换为"***"，其中床位号会特殊处理以避免误替换年龄等数字信息。
     * 
     * 处理逻辑：
     * 1. 检查输入文本是否为空，为空则直接返回
     * 2. 遍历敏感信息映射表中的每个条目
     * 3. 对床位号进行特殊处理：只替换"数字+床"格式的内容
     * 4. 对其他敏感信息进行精确替换
     * 5. 返回脱敏后的文本
     * 
     * @param text          原始文本内容，需要进行脱敏处理的字符串
     * @param sensitiveInfo 敏感信息映射表，包含需要脱敏的敏感信息键值对
     * @return String 脱敏后的文本，所有敏感信息已被替换为"***"
     */
    public String desensitizeText(String text, Map<String, String> sensitiveInfo) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String desensitizedText = text;

        // 对每个敏感信息进行替换
        for (Map.Entry<String, String> entry : sensitiveInfo.entrySet()) {
            String sensitiveValue = entry.getValue();
            String sensitiveType = entry.getKey();

            if (sensitiveValue != null && !sensitiveValue.trim().isEmpty()) {
                // 特殊处理床位号，避免误替换年龄等数字
                if ("床位号".equals(sensitiveType)) {
                    // 只替换包含"床"字的床位号，避免误替换其他数字
                    String bedPattern = sensitiveValue + "床";
                    desensitizedText = desensitizedText.replace(bedPattern, "***床");
                } else {
                    // 替换其他敏感信息为"***"
                    desensitizedText = desensitizedText.replace(sensitiveValue, "***");
                }
            }
        }

        return desensitizedText;
    }

    /**
     * 根据患者ID对文本进行脱敏处理
     * 
     * 该方法结合了获取敏感信息和文本脱敏两个步骤，提供便捷的一站式脱敏服务。
     * 首先根据患者ID获取敏感信息，然后对文本进行脱敏处理。
     * 
     * 使用场景：
     * - 在保存Prompt时对objectiveContent和dailyRecords进行脱敏
     * - 需要根据特定患者信息进行精确脱敏的场景
     * 
     * @param text      原始文本内容，需要进行脱敏处理的字符串
     * @param patientId 患者ID，用于查询该患者的敏感信息
     * @return String 脱敏后的文本，所有敏感信息已被替换为"***"
     * @throws RuntimeException 如果患者不存在或数据库查询失败
     */
    public String desensitizeTextByPatientId(String text, String patientId) {
        Map<String, String> sensitiveInfo = getSensitiveInfo(patientId);
        return desensitizeText(text, sensitiveInfo);
    }

    /**
     * 批量脱敏处理
     * 
     * 该方法支持对多个文本进行批量脱敏处理，适用于需要同时处理多个字段的场景。
     * 如同时处理objectiveContent和dailyRecords字段。
     * 
     * @param texts     原始文本数组，包含多个需要脱敏的文本内容
     * @param patientId 患者ID，用于查询该患者的敏感信息
     * @return String[] 脱敏后的文本数组，每个元素的敏感信息已被替换为"***"
     * @throws IllegalArgumentException 如果输入数组为空或患者ID为空
     * @throws RuntimeException         如果患者不存在或数据库查询失败
     */
    public String[] desensitizeTexts(String[] texts, String patientId) {
        Map<String, String> sensitiveInfo = getSensitiveInfo(patientId);
        String[] desensitizedTexts = new String[texts.length];

        for (int i = 0; i < texts.length; i++) {
            desensitizedTexts[i] = desensitizeText(texts[i], sensitiveInfo);
        }

        return desensitizedTexts;
    }
}
