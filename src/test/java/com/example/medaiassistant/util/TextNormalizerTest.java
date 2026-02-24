package com.example.medaiassistant.util;

import com.example.medaiassistant.config.TextNormalizerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TextNormalizer单元测试
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-16
 */
@DisplayName("文本标准化工具类测试")
class TextNormalizerTest {
    
    private TextNormalizer textNormalizer;
    
    @BeforeEach
    void setUp() {
        TextNormalizerConfig config = new TextNormalizerConfig();
        textNormalizer = new TextNormalizer(config);
    }
    
    @Test
    @DisplayName("测试完整标准化流程")
    void testNormalize() {
        // 测试用例：包含全角字符、多余空格、修饰词、方括号内容
        // 注意："急性"和"原发性"现在是DRGs主要诊断中的关键词，不再被去除
        String input = "急性　原发性　高血压[高血压病]　（重度）";
        String expected = "急性 原发性 高血压 (重度)";
        String result = textNormalizer.normalize(input);
        
        assertEquals(expected, result, "完整标准化流程应保留DRGs关键词并去除方括号内容");
    }
    
    @Test
    @DisplayName("测试全角转半角")
    void testFullWidthToHalfWidth() {
        // 测试全角字符转换
        String input = "Ｈｅｌｌｏ　Ｗｏｒｌｄ！";
        String expected = "Hello World!";
        String result = textNormalizer.fullWidthToHalfWidth(input);
        
        assertEquals(expected, result, "全角字符应正确转换为半角字符");
        
        // 测试混合字符
        input = "测试，全角。字符！";
        expected = "测试,全角.字符!";
        result = textNormalizer.fullWidthToHalfWidth(input);
        
        assertEquals(expected, result, "混合字符应正确转换");
    }
    
    @Test
    @DisplayName("测试去除多余空格")
    void testRemoveExtraSpaces() {
        // 测试多个空格
        String input = "测试    文本   标准化";
        String expected = "测试 文本 标准化";
        String result = textNormalizer.removeExtraSpaces(input);
        
        assertEquals(expected, result, "多余空格应被正确去除");
        
        // 测试前后空格
        input = "   测试文本   ";
        expected = "测试文本";
        result = textNormalizer.removeExtraSpaces(input);
        
        assertEquals(expected, result, "前后空格应被正确去除");
    }
    
    @Test
    @DisplayName("测试去除修饰词")
    void testRemoveModifiers() {
        // 测试保留的关键词（DRGs主要诊断中的关键词）
        String input = "急性心肌梗死";
        String expected = "急性心肌梗死";  // "急性"不再被去除
        String result = textNormalizer.removeModifiers(input);
        
        assertEquals(expected, result, "DRGs主要诊断中的关键词应被保留");
        
        // 测试会被去除的修饰词
        input = "未特指高血压";
        expected = "高血压";
        result = textNormalizer.removeModifiers(input);
        
        assertEquals(expected, result, "非DRGs关键词应被正确去除");
        
        // 测试不包含修饰词的文本
        input = "普通感冒";
        expected = "普通感冒";
        result = textNormalizer.removeModifiers(input);
        
        assertEquals(expected, result, "不包含修饰词的文本应保持不变");
    }
    
    @Test
    @DisplayName("测试统一数字格式")
    void testNormalizeNumbers() {
        // 测试罗马数字转换
        String input = "糖尿病Ⅱ型";
        String expected = "糖尿病2型";
        String result = textNormalizer.normalizeNumbers(input);
        
        assertEquals(expected, result, "罗马数字应正确转换为阿拉伯数字");
        
        // 测试中文数字转换
        input = "糖尿病型二";
        expected = "糖尿病2型";
        result = textNormalizer.normalizeNumbers(input);
        
        assertEquals(expected, result, "中文数字应正确转换为阿拉伯数字");
    }
    
    @Test
    @DisplayName("测试去除方括号内容")
    void testRemoveBracketContent() {
        // 测试包含方括号的文本
        String input = "高血压[高血压病]";
        String expected = "高血压";
        String result = textNormalizer.removeBracketContent(input);
        
        assertEquals(expected, result, "方括号内容应被正确去除");
        
        // 测试不包含方括号的文本
        input = "普通高血压";
        expected = "普通高血压";
        result = textNormalizer.removeBracketContent(input);
        
        assertEquals(expected, result, "不包含方括号的文本应保持不变");
    }
    
    @Test
    @DisplayName("测试提取方括号内容")
    void testExtractBracketContent() {
        // 测试包含方括号的文本
        String input = "高血压[高血压病]";
        String expected = "高血压病";
        String result = textNormalizer.extractBracketContent(input);
        
        assertEquals(expected, result, "方括号内容应被正确提取");
        
        // 测试不包含方括号的文本
        input = "普通高血压";
        expected = "";
        result = textNormalizer.extractBracketContent(input);
        
        assertEquals(expected, result, "不包含方括号的文本应返回空字符串");
    }
    
    @Test
    @DisplayName("测试判断是否包含修饰词")
    void testContainsModifier() {
        // 测试包含修饰词
        String text = "急性阑尾炎";
        String modifier = "急性";
        boolean result = textNormalizer.containsModifier(text, modifier);
        
        assertTrue(result, "应正确识别包含的修饰词");
        
        // 测试不包含修饰词
        text = "普通阑尾炎";
        modifier = "急性";
        result = textNormalizer.containsModifier(text, modifier);
        
        assertFalse(result, "应正确识别不包含的修饰词");
    }
    
    @Test
    @DisplayName("测试获取主要名称")
    void testGetMainName() {
        // 测试包含方括号的文本
        String input = "高血压[高血压病]";
        String expected = "高血压";
        String result = textNormalizer.getMainName(input);
        
        assertEquals(expected, result, "应正确获取主要名称");
        
        // 测试不包含方括号的文本
        input = "普通高血压";
        expected = "普通高血压";
        result = textNormalizer.getMainName(input);
        
        assertEquals(expected, result, "不包含方括号的文本应返回原文本");
    }
    
    @Test
    @DisplayName("测试获取别名")
    void testGetAlias() {
        // 测试包含方括号的文本
        String input = "高血压[高血压病]";
        String expected = "高血压病";
        String result = textNormalizer.getAlias(input);
        
        assertEquals(expected, result, "应正确获取别名");
        
        // 测试不包含方括号的文本
        input = "普通高血压";
        expected = "";
        result = textNormalizer.getAlias(input);
        
        assertEquals(expected, result, "不包含方括号的文本应返回空字符串");
    }
    
    @Test
    @DisplayName("测试判断是否包含方括号内容")
    void testHasBracketContent() {
        // 测试包含方括号的文本
        String text = "高血压[高血压病]";
        boolean result = textNormalizer.hasBracketContent(text);
        
        assertTrue(result, "应正确识别包含方括号内容的文本");
        
        // 测试不包含方括号的文本
        text = "普通高血压";
        result = textNormalizer.hasBracketContent(text);
        
        assertFalse(result, "应正确识别不包含方括号内容的文本");
    }
    
    @Test
    @DisplayName("测试空值和边界情况")
    void testNullAndEdgeCases() {
        // 测试null输入
        assertDoesNotThrow(() -> {
            String result = textNormalizer.normalize(null);
            assertEquals("", result, "null输入应返回空字符串");
        });
        
        // 测试空字符串
        String result = textNormalizer.normalize("");
        assertEquals("", result, "空字符串输入应返回空字符串");
        
        // 测试纯空格
        result = textNormalizer.normalize("   ");
        assertEquals("", result, "纯空格输入应返回空字符串");
    }
    
    @Test
    @DisplayName("测试修饰词管理")
    void testModifierManagement() {
        // 测试添加自定义修饰词
        textNormalizer.addModifier("测试修饰词");
        String input = "测试修饰词疾病";
        String expected = "疾病";
        String result = textNormalizer.removeModifiers(input);
        
        assertEquals(expected, result, "自定义修饰词应被正确去除");
        
        // 测试移除自定义修饰词
        textNormalizer.removeModifier("测试修饰词");
        result = textNormalizer.removeModifiers(input);
        
        assertEquals(input, result, "移除的修饰词不应被去除");
    }
    
    @Test
    @DisplayName("测试复杂医疗文本标准化")
    void testComplexMedicalText() {
        // 复杂医疗文本测试用例 - 更新为反映新的修饰词集合
        String[] testCases = {
            "急性　未特指　高血压[高血压病]　（重度）",
            "早期　稳定性　心绞痛[冠心病]",
            "糖尿病Ⅱ型[2型糖尿病]　伴并发症",
            "肺炎[肺部感染]　（重症）"
        };
        
        String[] expectedResults = {
            "急性 高血压 (重度)",  // "急性"保留，"未特指"去除
            "稳定性 心绞痛",  // "早期"去除，"稳定性"保留（DRGs关键词）
            "糖尿病2型 伴并发症", 
            "肺炎 (重症)"
        };
        
        for (int i = 0; i < testCases.length; i++) {
            String result = textNormalizer.normalize(testCases[i]);
            assertEquals(expectedResults[i], result, 
                "复杂医疗文本标准化测试用例 " + (i + 1) + " 失败");
        }
    }
}
