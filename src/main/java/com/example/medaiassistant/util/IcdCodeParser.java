package com.example.medaiassistant.util;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ICD-10编码解析工具类
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-16
 */
@Component
public class IcdCodeParser {
    
    // ICD-10编码正则表达式
    // 支持格式: [A-Z]\d+(?:\.\d+)?(?:x\d+)?
    // 示例: I48.000, I48.900x003, E11.9
    private static final Pattern ICD_CODE_PATTERN = Pattern.compile("^([A-Z]\\d+(?:\\.\\d+)?(?:x\\d+)?)");
    
    // 诊断条目格式正则表达式
    // 格式: [编码] [空格] [名称]
    // 示例: "I48.000 阵发性心房颤动", "I48.900x003 心房扑动"
    private static final Pattern DIAGNOSIS_ENTRY_PATTERN = Pattern.compile("^([A-Z]\\d+(?:\\.\\d+)?(?:x\\d+)?)\\s+(.+)$");
    
    // 别名提取正则表达式
    // 格式: [主要名称][别名]
    // 示例: "心房颤动[心房纤颤]"
    private static final Pattern ALIAS_PATTERN = Pattern.compile("^(.+?)(?:\\[(.+)\\])?$");
    
    /**
     * 从文本中提取ICD编码
     * 
     * @param text 包含ICD编码的文本
     * @return ICD编码，如果未找到则返回空字符串
     */
    public String extractIcdCode(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        
        Matcher matcher = ICD_CODE_PATTERN.matcher(text.trim());
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return "";
    }
    
    /**
     * 从诊断条目中提取诊断名称
     * 
     * @param text 诊断条目文本
     * @return 诊断名称，如果未找到则返回空字符串
     */
    public String extractName(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        
        // 先尝试匹配标准格式
        Matcher matcher = DIAGNOSIS_ENTRY_PATTERN.matcher(text.trim());
        if (matcher.find()) {
            return matcher.group(2).trim();
        }
        
        // 如果没有ICD编码，直接返回整个文本
        String icdCode = extractIcdCode(text);
        if (icdCode.isEmpty()) {
            return text.trim();
        }
        
        // 去除ICD编码部分
        return text.replaceFirst(Pattern.quote(icdCode), "").trim();
    }
    
    /**
     * 从诊断名称中提取主要名称（去除方括号内容）
     * 
     * @param name 诊断名称
     * @return 主要名称
     */
    public String extractMainName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        
        Matcher matcher = ALIAS_PATTERN.matcher(name.trim());
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        return name.trim();
    }
    
    /**
     * 从诊断名称中提取别名（方括号内容）
     * 
     * @param name 诊断名称
     * @return 别名，如果没有则返回空字符串
     */
    public String extractAlias(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        
        Matcher matcher = ALIAS_PATTERN.matcher(name.trim());
        if (matcher.find() && matcher.group(2) != null) {
            return matcher.group(2).trim();
        }
        
        return "";
    }
    
    /**
     * 解析诊断条目
     * 
     * @param entry 诊断条目文本
     * @return 诊断条目对象
     */
    public DiagnosisEntry parseDiagnosisEntry(String entry) {
        if (entry == null || entry.trim().isEmpty()) {
            return new DiagnosisEntry("", "", "", "");
        }
        
        String text = entry.trim();
        String icdCode = extractIcdCode(text);
        String fullName = extractName(text);
        String mainName = extractMainName(fullName);
        String alias = extractAlias(fullName);
        
        return new DiagnosisEntry(icdCode, fullName, mainName, alias);
    }
    
    /**
     * 判断文本是否包含有效的ICD编码
     * 
     * @param text 文本
     * @return 是否包含有效的ICD编码
     */
    public boolean hasValidIcdCode(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        return !extractIcdCode(text).isEmpty();
    }
    
    /**
     * 判断诊断名称是否包含别名
     * 
     * @param name 诊断名称
     * @return 是否包含别名
     */
    public boolean hasAlias(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return !extractAlias(name).isEmpty();
    }
    
    /**
     * 标准化ICD编码格式
     * 
     * @param icdCode ICD编码
     * @return 标准化后的ICD编码
     */
    public String normalizeIcdCode(String icdCode) {
        if (icdCode == null || icdCode.trim().isEmpty()) {
            return "";
        }
        
        String code = icdCode.trim().toUpperCase();
        
        // 确保格式正确
        if (!hasValidIcdCode(code)) {
            return "";
        }
        
        return code;
    }
    
    /**
     * 获取ICD编码的主类别
     * 
     * @param icdCode ICD编码
     * @return 主类别（第一个字母），如果无效则返回空字符串
     */
    public String getIcdMainCategory(String icdCode) {
        if (icdCode == null || icdCode.trim().isEmpty()) {
            return "";
        }
        
        String code = normalizeIcdCode(icdCode);
        if (code.isEmpty()) {
            return "";
        }
        
        return code.substring(0, 1);
    }
    
    /**
     * 获取ICD编码的章节
     * 
     * @param icdCode ICD编码
     * @return 章节描述，如果无效则返回"未知章节"
     */
    public String getIcdChapter(String icdCode) {
        if (icdCode == null || icdCode.trim().isEmpty()) {
            return "未知章节";
        }
        
        String category = getIcdMainCategory(icdCode);
        if (category.isEmpty()) {
            return "未知章节";
        }
        
        switch (category) {
            case "A", "B":
                return "某些传染病和寄生虫病";
            case "C":
                return "肿瘤";
            case "D":
                return "血液及造血器官疾病和某些涉及免疫机制的疾患";
            case "E":
                return "内分泌、营养和代谢疾病";
            case "F":
                return "精神和行为障碍";
            case "G":
                return "神经系统疾病";
            case "H":
                return "眼和附器疾病";
            case "I":
                return "循环系统疾病";
            case "J":
                return "呼吸系统疾病";
            case "K":
                return "消化系统疾病";
            case "L":
                return "皮肤和皮下组织疾病";
            case "M":
                return "肌肉骨骼系统和结缔组织疾病";
            case "N":
                return "泌尿生殖系统疾病";
            case "O":
                return "妊娠、分娩和产褥期";
            case "P":
                return "起源于围生期的某些情况";
            case "Q":
                return "先天性畸形、变形和染色体异常";
            case "R":
                return "症状、体征和临床与实验室异常所见，不可归类在他处者";
            case "S", "T":
                return "损伤、中毒和外因的某些其他后果";
            case "V", "W", "X", "Y":
                return "疾病和死亡的外因";
            case "Z":
                return "影响健康状态和与保健机构接触的因素";
            default:
                return "未知章节";
        }
    }
    
    /**
     * 验证ICD编码格式
     * 
     * @param icdCode ICD编码
     * @return 是否有效
     */
    public boolean isValidIcdCode(String icdCode) {
        if (icdCode == null || icdCode.trim().isEmpty()) {
            return false;
        }
        return ICD_CODE_PATTERN.matcher(icdCode.trim()).matches();
    }
    
    /**
     * 诊断条目类
     */
    public static class DiagnosisEntry {
        private final String icdCode;
        private final String diagnosisName;
        private final String mainName;
        private final String alias;
        
        public DiagnosisEntry(String icdCode, String diagnosisName, String mainName, String alias) {
            this.icdCode = icdCode != null ? icdCode : "";
            this.diagnosisName = diagnosisName != null ? diagnosisName : "";
            this.mainName = mainName != null ? mainName : "";
            this.alias = alias != null ? alias : "";
        }
        
        public String getIcdCode() {
            return icdCode;
        }
        
        public String getDiagnosisName() {
            return diagnosisName;
        }
        
        public String getMainName() {
            return mainName;
        }
        
        public String getAlias() {
            return alias;
        }
        
        public boolean hasIcdCode() {
            return !icdCode.isEmpty();
        }
        
        public boolean hasAlias() {
            return !alias.isEmpty();
        }
        
        public String getDisplayName() {
            if (hasAlias()) {
                return mainName + "[" + alias + "]";
            }
            return diagnosisName;
        }
        
        @Override
        public String toString() {
            return String.format("DiagnosisEntry{icdCode='%s', diagnosisName='%s', mainName='%s', alias='%s'}",
                    icdCode, diagnosisName, mainName, alias);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            DiagnosisEntry that = (DiagnosisEntry) obj;
            return icdCode.equals(that.icdCode) && 
                   diagnosisName.equals(that.diagnosisName);
        }
        
        @Override
        public int hashCode() {
            int result = icdCode.hashCode();
            result = 31 * result + diagnosisName.hashCode();
            return result;
        }
    }
}
