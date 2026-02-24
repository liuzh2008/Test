package com.example.medaiassistant.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ICD编码解析工具类测试
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-16
 */
@DisplayName("ICD编码解析工具类测试")
class IcdCodeParserTest {
    
    private IcdCodeParser icdCodeParser;
    
    @BeforeEach
    void setUp() {
        icdCodeParser = new IcdCodeParser();
    }
    
    @Test
    @DisplayName("测试提取ICD编码")
    void testExtractIcdCode() {
        // 测试标准ICD编码
        String text = "I48.000 阵发性心房颤动";
        String icdCode = icdCodeParser.extractIcdCode(text);
        assertEquals("I48.000", icdCode, "应正确提取标准ICD编码");
        
        // 测试带x扩展的ICD编码
        text = "I48.900x003 心房扑动";
        icdCode = icdCodeParser.extractIcdCode(text);
        assertEquals("I48.900x003", icdCode, "应正确提取带x扩展的ICD编码");
        
        // 测试简单ICD编码
        text = "E11.9 2型糖尿病";
        icdCode = icdCodeParser.extractIcdCode(text);
        assertEquals("E11.9", icdCode, "应正确提取简单ICD编码");
        
        // 测试不包含ICD编码的文本
        text = "普通感冒";
        icdCode = icdCodeParser.extractIcdCode(text);
        assertEquals("", icdCode, "不包含ICD编码的文本应返回空字符串");
        
        // 测试null输入
        icdCode = icdCodeParser.extractIcdCode(null);
        assertEquals("", icdCode, "null输入应返回空字符串");
    }
    
    @Test
    @DisplayName("测试提取诊断名称")
    void testExtractName() {
        // 测试标准格式
        String text = "I48.000 阵发性心房颤动";
        String name = icdCodeParser.extractName(text);
        assertEquals("阵发性心房颤动", name, "应正确提取诊断名称");
        
        // 测试带别名的诊断名称
        text = "I48.000 阵发性心房颤动[房颤]";
        name = icdCodeParser.extractName(text);
        assertEquals("阵发性心房颤动[房颤]", name, "应正确提取带别名的诊断名称");
        
        // 测试不包含ICD编码的文本
        text = "普通感冒";
        name = icdCodeParser.extractName(text);
        assertEquals("普通感冒", name, "不包含ICD编码的文本应返回原文本");
        
        // 测试null输入
        name = icdCodeParser.extractName(null);
        assertEquals("", name, "null输入应返回空字符串");
    }
    
    @Test
    @DisplayName("测试提取主要名称")
    void testExtractMainName() {
        // 测试包含别名的诊断名称
        String name = "阵发性心房颤动[房颤]";
        String mainName = icdCodeParser.extractMainName(name);
        assertEquals("阵发性心房颤动", mainName, "应正确提取主要名称");
        
        // 测试不包含别名的诊断名称
        name = "普通感冒";
        mainName = icdCodeParser.extractMainName(name);
        assertEquals("普通感冒", mainName, "不包含别名的诊断名称应返回原文本");
        
        // 测试null输入
        mainName = icdCodeParser.extractMainName(null);
        assertEquals("", mainName, "null输入应返回空字符串");
    }
    
    @Test
    @DisplayName("测试提取别名")
    void testExtractAlias() {
        // 测试包含别名的诊断名称
        String name = "阵发性心房颤动[房颤]";
        String alias = icdCodeParser.extractAlias(name);
        assertEquals("房颤", alias, "应正确提取别名");
        
        // 测试不包含别名的诊断名称
        name = "普通感冒";
        alias = icdCodeParser.extractAlias(name);
        assertEquals("", alias, "不包含别名的诊断名称应返回空字符串");
        
        // 测试null输入
        alias = icdCodeParser.extractAlias(null);
        assertEquals("", alias, "null输入应返回空字符串");
    }
    
    @Test
    @DisplayName("测试解析诊断条目")
    void testParseDiagnosisEntry() {
        // 测试标准诊断条目
        String entry = "I48.000 阵发性心房颤动[房颤]";
        IcdCodeParser.DiagnosisEntry result = icdCodeParser.parseDiagnosisEntry(entry);
        
        assertEquals("I48.000", result.getIcdCode(), "ICD编码应正确解析");
        assertEquals("阵发性心房颤动[房颤]", result.getDiagnosisName(), "诊断名称应正确解析");
        assertEquals("阵发性心房颤动", result.getMainName(), "主要名称应正确解析");
        assertEquals("房颤", result.getAlias(), "别名应正确解析");
        assertTrue(result.hasIcdCode(), "应包含ICD编码");
        assertTrue(result.hasAlias(), "应包含别名");
        
        // 测试不包含别名的诊断条目
        entry = "E11.9 2型糖尿病";
        result = icdCodeParser.parseDiagnosisEntry(entry);
        
        assertEquals("E11.9", result.getIcdCode(), "ICD编码应正确解析");
        assertEquals("2型糖尿病", result.getDiagnosisName(), "诊断名称应正确解析");
        assertEquals("2型糖尿病", result.getMainName(), "主要名称应正确解析");
        assertEquals("", result.getAlias(), "别名应为空");
        assertTrue(result.hasIcdCode(), "应包含ICD编码");
        assertFalse(result.hasAlias(), "不应包含别名");
        
        // 测试不包含ICD编码的条目
        entry = "普通感冒";
        result = icdCodeParser.parseDiagnosisEntry(entry);
        
        assertEquals("", result.getIcdCode(), "ICD编码应为空");
        assertEquals("普通感冒", result.getDiagnosisName(), "诊断名称应正确解析");
        assertEquals("普通感冒", result.getMainName(), "主要名称应正确解析");
        assertEquals("", result.getAlias(), "别名应为空");
        assertFalse(result.hasIcdCode(), "不应包含ICD编码");
        assertFalse(result.hasAlias(), "不应包含别名");
        
        // 测试null输入
        result = icdCodeParser.parseDiagnosisEntry(null);
        assertNotNull(result, "null输入应返回非空对象");
        assertEquals("", result.getIcdCode(), "ICD编码应为空");
        assertEquals("", result.getDiagnosisName(), "诊断名称应为空");
    }
    
    @Test
    @DisplayName("测试判断是否包含有效ICD编码")
    void testHasValidIcdCode() {
        // 测试包含有效ICD编码
        String text = "I48.000 阵发性心房颤动";
        boolean hasValid = icdCodeParser.hasValidIcdCode(text);
        assertTrue(hasValid, "应识别包含有效ICD编码的文本");
        
        // 测试不包含有效ICD编码
        text = "普通感冒";
        hasValid = icdCodeParser.hasValidIcdCode(text);
        assertFalse(hasValid, "应识别不包含有效ICD编码的文本");
        
        // 测试null输入
        hasValid = icdCodeParser.hasValidIcdCode(null);
        assertFalse(hasValid, "null输入应返回false");
    }
    
    @Test
    @DisplayName("测试判断是否包含别名")
    void testHasAlias() {
        // 测试包含别名
        String name = "阵发性心房颤动[房颤]";
        boolean hasAlias = icdCodeParser.hasAlias(name);
        assertTrue(hasAlias, "应识别包含别名的诊断名称");
        
        // 测试不包含别名
        name = "普通感冒";
        hasAlias = icdCodeParser.hasAlias(name);
        assertFalse(hasAlias, "应识别不包含别名的诊断名称");
        
        // 测试null输入
        hasAlias = icdCodeParser.hasAlias(null);
        assertFalse(hasAlias, "null输入应返回false");
    }
    
    @Test
    @DisplayName("测试标准化ICD编码")
    void testNormalizeIcdCode() {
        // 测试标准ICD编码
        String icdCode = "I48.000";
        String normalized = icdCodeParser.normalizeIcdCode(icdCode);
        assertEquals("I48.000", normalized, "标准ICD编码应保持不变");
        
        // 测试带空格的ICD编码
        icdCode = " I48.000 ";
        normalized = icdCodeParser.normalizeIcdCode(icdCode);
        assertEquals("I48.000", normalized, "应去除前后空格");
        
        // 测试小写ICD编码
        icdCode = "i48.000";
        normalized = icdCodeParser.normalizeIcdCode(icdCode);
        assertEquals("I48.000", normalized, "应转换为大写");
        
        // 测试无效ICD编码
        icdCode = "无效编码";
        normalized = icdCodeParser.normalizeIcdCode(icdCode);
        assertEquals("", normalized, "无效ICD编码应返回空字符串");
        
        // 测试null输入
        normalized = icdCodeParser.normalizeIcdCode(null);
        assertEquals("", normalized, "null输入应返回空字符串");
    }
    
    @ParameterizedTest
    @DisplayName("测试获取ICD编码主类别")
    @CsvSource({
        "I48.000, I",
        "E11.9, E", 
        "J18.9, J",
        "无效编码, ",
        ", "
    })
    void testGetIcdMainCategory(String icdCode, String expectedCategory) {
        String category = icdCodeParser.getIcdMainCategory(icdCode);
        // 处理空字符串和null的期望值
        if (expectedCategory == null || expectedCategory.isEmpty()) {
            assertTrue(category == null || category.isEmpty(), 
                "ICD编码主类别提取不正确: 期望空值或空字符串，实际得到: " + category);
        } else {
            assertEquals(expectedCategory, category, "ICD编码主类别提取不正确");
        }
    }
    
    @ParameterizedTest
    @DisplayName("测试获取ICD编码章节")
    @CsvSource({
        "I48.000, 循环系统疾病",
        "E11.9, 内分泌、营养和代谢疾病",
        "J18.9, 呼吸系统疾病",
        "C50.9, 肿瘤",
        "无效编码, 未知章节"
    })
    void testGetIcdChapter(String icdCode, String expectedChapter) {
        String chapter = icdCodeParser.getIcdChapter(icdCode);
        assertEquals(expectedChapter, chapter, "ICD编码章节提取不正确");
    }
    
    @Test
    @DisplayName("测试验证ICD编码格式")
    void testIsValidIcdCode() {
        // 测试有效ICD编码
        assertTrue(icdCodeParser.isValidIcdCode("I48.000"), "标准ICD编码应有效");
        assertTrue(icdCodeParser.isValidIcdCode("I48.900x003"), "带x扩展的ICD编码应有效");
        assertTrue(icdCodeParser.isValidIcdCode("E11.9"), "简单ICD编码应有效");
        
        // 测试无效ICD编码
        assertFalse(icdCodeParser.isValidIcdCode("无效编码"), "无效编码应无效");
        assertFalse(icdCodeParser.isValidIcdCode(""), "空字符串应无效");
        assertFalse(icdCodeParser.isValidIcdCode(null), "null应无效");
    }
    
    @Test
    @DisplayName("测试DiagnosisEntry类")
    void testDiagnosisEntryClass() {
        // 测试构造函数
        IcdCodeParser.DiagnosisEntry entry = new IcdCodeParser.DiagnosisEntry(
            "I48.000", "阵发性心房颤动[房颤]", "阵发性心房颤动", "房颤"
        );
        
        assertEquals("I48.000", entry.getIcdCode());
        assertEquals("阵发性心房颤动[房颤]", entry.getDiagnosisName());
        assertEquals("阵发性心房颤动", entry.getMainName());
        assertEquals("房颤", entry.getAlias());
        assertTrue(entry.hasIcdCode());
        assertTrue(entry.hasAlias());
        
        // 测试显示名称
        assertEquals("阵发性心房颤动[房颤]", entry.getDisplayName());
        
        // 测试不包含别名的情况
        IcdCodeParser.DiagnosisEntry entryWithoutAlias = new IcdCodeParser.DiagnosisEntry(
            "E11.9", "2型糖尿病", "2型糖尿病", ""
        );
        assertEquals("2型糖尿病", entryWithoutAlias.getDisplayName());
        assertFalse(entryWithoutAlias.hasAlias());
        
        // 测试toString方法
        assertNotNull(entry.toString());
        
        // 测试equals和hashCode方法
        IcdCodeParser.DiagnosisEntry sameEntry = new IcdCodeParser.DiagnosisEntry(
            "I48.000", "阵发性心房颤动[房颤]", "阵发性心房颤动", "房颤"
        );
        assertEquals(entry, sameEntry);
        assertEquals(entry.hashCode(), sameEntry.hashCode());
        
        IcdCodeParser.DiagnosisEntry differentEntry = new IcdCodeParser.DiagnosisEntry(
            "E11.9", "2型糖尿病", "2型糖尿病", ""
        );
        assertNotEquals(entry, differentEntry);
    }
    
    @Test
    @DisplayName("测试复杂医疗诊断条目解析")
    void testComplexMedicalDiagnosisEntries() {
        // 复杂医疗诊断条目测试用例
        String[][] testCases = {
            {
                "I48.000 阵发性心房颤动[房颤]",
                "I48.000", "阵发性心房颤动[房颤]", "阵发性心房颤动", "房颤"
            },
            {
                "E11.9 2型糖尿病",
                "E11.9", "2型糖尿病", "2型糖尿病", ""
            },
            {
                "J18.9 肺炎[肺部感染]",
                "J18.9", "肺炎[肺部感染]", "肺炎", "肺部感染"
            },
            {
                "I10 原发性高血压[高血压病]",
                "I10", "原发性高血压[高血压病]", "原发性高血压", "高血压病"
            },
            {
                "普通感冒",
                "", "普通感冒", "普通感冒", ""
            }
        };
        
        for (String[] testCase : testCases) {
            String entry = testCase[0];
            String expectedIcdCode = testCase[1];
            String expectedDiagnosisName = testCase[2];
            String expectedMainName = testCase[3];
            String expectedAlias = testCase[4];
            
            IcdCodeParser.DiagnosisEntry result = icdCodeParser.parseDiagnosisEntry(entry);
            
            assertEquals(expectedIcdCode, result.getIcdCode(), 
                "ICD编码解析失败: " + entry);
            assertEquals(expectedDiagnosisName, result.getDiagnosisName(), 
                "诊断名称解析失败: " + entry);
            assertEquals(expectedMainName, result.getMainName(), 
                "主要名称解析失败: " + entry);
            assertEquals(expectedAlias, result.getAlias(), 
                "别名解析失败: " + entry);
        }
    }
    
    @Test
    @DisplayName("测试边界情况和错误处理")
    void testEdgeCasesAndErrorHandling() {
        // 测试空字符串
        IcdCodeParser.DiagnosisEntry result = icdCodeParser.parseDiagnosisEntry("");
        assertNotNull(result, "空字符串输入应返回非空对象");
        assertEquals("", result.getIcdCode());
        assertEquals("", result.getDiagnosisName());
        
        // 测试纯空格
        result = icdCodeParser.parseDiagnosisEntry("   ");
        assertNotNull(result, "纯空格输入应返回非空对象");
        assertEquals("", result.getIcdCode());
        assertEquals("", result.getDiagnosisName());
        
        // 测试只有ICD编码没有名称
        String entry = "I48.000";
        result = icdCodeParser.parseDiagnosisEntry(entry);
        assertEquals("I48.000", result.getIcdCode());
        assertEquals("", result.getDiagnosisName());
        
        // 测试ICD编码在中间的情况（不符合标准格式）
        entry = "诊断：I48.000 阵发性心房颤动";
        result = icdCodeParser.parseDiagnosisEntry(entry);
        assertEquals("", result.getIcdCode()); // 不符合标准格式，无法提取
        assertEquals("诊断：I48.000 阵发性心房颤动", result.getDiagnosisName());
    }
    
    @Test
    @DisplayName("测试所有ICD章节")
    void testAllIcdChapters() {
        // 测试所有ICD章节分类
        String[][] testCases = {
            {"A00.0", "某些传染病和寄生虫病"},
            {"B20.0", "某些传染病和寄生虫病"},
            {"C50.9", "肿瘤"},
            {"D50.9", "血液及造血器官疾病和某些涉及免疫机制的疾患"},
            {"E11.9", "内分泌、营养和代谢疾病"},
            {"F20.0", "精神和行为障碍"},
            {"G40.9", "神经系统疾病"},
            {"H10.9", "眼和附器疾病"},
            {"I48.0", "循环系统疾病"},
            {"J18.9", "呼吸系统疾病"},
            {"K35.9", "消化系统疾病"},
            {"L20.9", "皮肤和皮下组织疾病"},
            {"M15.9", "肌肉骨骼系统和结缔组织疾病"},
            {"N18.9", "泌尿生殖系统疾病"},
            {"O80.9", "妊娠、分娩和产褥期"},
            {"P00.0", "起源于围生期的某些情况"},
            {"Q20.0", "先天性畸形、变形和染色体异常"},
            {"R50.9", "症状、体征和临床与实验室异常所见，不可归类在他处者"},
            {"S00.0", "损伤、中毒和外因的某些其他后果"},
            {"T00.0", "损伤、中毒和外因的某些其他后果"},
            {"V00.0", "疾病和死亡的外因"},
            {"W00.0", "疾病和死亡的外因"},
            {"X00.0", "疾病和死亡的外因"},
            {"Y00.0", "疾病和死亡的外因"},
            {"Z00.0", "影响健康状态和与保健机构接触的因素"},
            {"无效编码", "未知章节"}
        };
        
        for (String[] testCase : testCases) {
            String icdCode = testCase[0];
            String expectedChapter = testCase[1];
            String actualChapter = icdCodeParser.getIcdChapter(icdCode);
            
            assertEquals(expectedChapter, actualChapter, 
                "ICD章节分类测试失败: " + icdCode);
        }
    }
}
