package com.example.medaiassistant.drg.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CLOB解析器测试类
 * 完善版本：包含边界情况和异常场景测试
 */
class ClobParserTest {

    private final ClobParser clobParser = new ClobParser();

    @Test
    void shouldParseDiagnosisClobWithIcdCodeAndName() {
        // 给定一个标准的诊断CLOB
        String clob = "I48.000 阵发性心房颤动";
        
        // 当解析CLOB时
        List<DiagnosisEntry> result = clobParser.parseDiagnoses(clob);
        
        // 那么应该正确解析出诊断条目
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIcdCode()).isEqualTo("I48.000");
        assertThat(result.get(0).getDiagnosisName()).isEqualTo("阵发性心房颤动");
    }

    @Test
    void shouldParseProcedureClobWithCodeAndName() {
        // 给定一个标准的手术CLOB
        String clob = "37.9000x001 经皮左心耳封堵术";
        
        // 当解析CLOB时
        List<ProcedureEntry> result = clobParser.parseProcedures(clob);
        
        // 那么应该正确解析出手续条目
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProcedureCode()).isEqualTo("37.9000x001");
        assertThat(result.get(0).getProcedureName()).isEqualTo("经皮左心耳封堵术");
    }

    @Test
    void shouldHandleEmptyDiagnosisClob() {
        // 给定一个空的诊断CLOB
        String clob = "";
        
        // 当解析CLOB时
        List<DiagnosisEntry> result = clobParser.parseDiagnoses(clob);
        
        // 那么应该返回空列表
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleEmptyProcedureClob() {
        // 给定一个空的手术CLOB
        String clob = "";
        
        // 当解析CLOB时
        List<ProcedureEntry> result = clobParser.parseProcedures(clob);
        
        // 那么应该返回空列表
        assertThat(result).isEmpty();
    }

    @Test
    void shouldParseDiagnosisWithAlias() {
        // 给定一个包含别名的诊断CLOB
        String clob = "I48.000 阵发性心房颤动[房颤]";
        
        // 当解析CLOB时
        List<DiagnosisEntry> result = clobParser.parseDiagnoses(clob);
        
        // 那么应该正确解析出诊断条目和别名
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIcdCode()).isEqualTo("I48.000");
        assertThat(result.get(0).getDiagnosisName()).isEqualTo("阵发性心房颤动");
        assertThat(result.get(0).getAliases()).contains("房颤");
    }

    @Test
    void shouldHandleNullClob() {
        // 给定一个null的CLOB
        // 当解析CLOB时
        List<DiagnosisEntry> result = clobParser.parseDiagnoses(null);
        
        // 那么应该返回空列表
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleMalformedClob() {
        // 给定一个格式错误的CLOB
        String clob = "InvalidFormatWithoutSpace";
        
        // 当解析CLOB时
        List<DiagnosisEntry> result = clobParser.parseDiagnoses(clob);
        
        // 那么应该返回空列表
        assertThat(result).isEmpty();
    }

    @Test
    void shouldParseMultipleDiagnosisEntries() {
        // 给定一个包含多个诊断的CLOB
        String clob = "I48.000 阵发性心房颤动\nI10.x00 原发性高血压\nE11.900 2型糖尿病";
        
        // 当解析CLOB时
        List<DiagnosisEntry> result = clobParser.parseDiagnoses(clob);
        
        // 那么应该正确解析出所有诊断条目
        assertThat(result).hasSize(3);
        assertThat(result).extracting(DiagnosisEntry::getIcdCode)
            .containsExactly("I48.000", "I10.x00", "E11.900");
        assertThat(result).extracting(DiagnosisEntry::getDiagnosisName)
            .containsExactly("阵发性心房颤动", "原发性高血压", "2型糖尿病");
    }

    @Test
    void shouldParseMultipleProcedureEntries() {
        // 给定一个包含多个手术的CLOB
        String clob = "37.9000x001 经皮左心耳封堵术\n88.7901 冠状动脉造影\n35.2400 二尖瓣置换术";
        
        // 当解析CLOB时
        List<ProcedureEntry> result = clobParser.parseProcedures(clob);
        
        // 那么应该正确解析出所有手术条目
        assertThat(result).hasSize(3);
        assertThat(result).extracting(ProcedureEntry::getProcedureCode)
            .containsExactly("37.9000x001", "88.7901", "35.2400");
        assertThat(result).extracting(ProcedureEntry::getProcedureName)
            .containsExactly("经皮左心耳封堵术", "冠状动脉造影", "二尖瓣置换术");
    }

    @Test
    void shouldParseDiagnosisWithMultipleAliases() {
        // 给定一个包含多个别名的诊断CLOB
        String clob = "I48.000 阵发性心房颤动[房颤,心房纤颤,AF]";
        
        // 当解析CLOB时
        List<DiagnosisEntry> result = clobParser.parseDiagnoses(clob);
        
        // 那么应该正确解析出诊断条目和所有别名
        assertThat(result).hasSize(1);
        DiagnosisEntry entry = result.get(0);
        assertThat(entry.getIcdCode()).isEqualTo("I48.000");
        assertThat(entry.getDiagnosisName()).isEqualTo("阵发性心房颤动");
        assertThat(entry.getAliases()).containsExactly("房颤", "心房纤颤", "AF");
    }

    @Test
    void shouldParseProcedureWithMultipleAliases() {
        // 给定一个包含多个别名的手术CLOB
        String clob = "37.9000x001 经皮左心耳封堵术[左心耳封堵,LAA封堵]";
        
        // 当解析CLOB时
        List<ProcedureEntry> result = clobParser.parseProcedures(clob);
        
        // 那么应该正确解析出手术条目和所有别名
        assertThat(result).hasSize(1);
        ProcedureEntry entry = result.get(0);
        assertThat(entry.getProcedureCode()).isEqualTo("37.9000x001");
        assertThat(entry.getProcedureName()).isEqualTo("经皮左心耳封堵术");
        assertThat(entry.getAliases()).containsExactly("左心耳封堵", "LAA封堵");
    }

    @Test
    void shouldHandleWhitespaceInClob() {
        // 给定一个包含多余空格的CLOB
        String clob = "  I48.000   阵发性心房颤动   [ 房颤 , 心房纤颤 ]  ";
        
        // 当解析CLOB时
        List<DiagnosisEntry> result = clobParser.parseDiagnoses(clob);
        
        // 那么应该正确解析并去除多余空格
        assertThat(result).hasSize(1);
        DiagnosisEntry entry = result.get(0);
        assertThat(entry.getIcdCode()).isEqualTo("I48.000");
        assertThat(entry.getDiagnosisName()).isEqualTo("阵发性心房颤动");
        assertThat(entry.getAliases()).containsExactly("房颤", "心房纤颤");
    }

    @Test
    void shouldHandleEmptyLinesInClob() {
        // 给定一个包含空行的CLOB
        String clob = "I48.000 阵发性心房颤动\n\nI10.x00 原发性高血压\n\n\nE11.900 2型糖尿病";
        
        // 当解析CLOB时
        List<DiagnosisEntry> result = clobParser.parseDiagnoses(clob);
        
        // 那么应该忽略空行并正确解析有效条目
        assertThat(result).hasSize(3);
        assertThat(result).extracting(DiagnosisEntry::getIcdCode)
            .containsExactly("I48.000", "I10.x00", "E11.900");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\n", "\r\n", "\t"})
    void shouldHandleVariousEmptyValues(String emptyValue) {
        // 给定各种形式的空值
        // 当解析CLOB时
        List<DiagnosisEntry> result = clobParser.parseDiagnoses(emptyValue);
        
        // 那么应该返回空列表
        assertThat(result).isEmpty();
    }

    @Test
    void shouldParseComplexProcedureWithSpecialCharacters() {
        // 给定一个包含特殊字符的手术CLOB
        String clob = "35.2400 二尖瓣置换术(机械瓣)[MVR,二尖瓣机械瓣置换]";
        
        // 当解析CLOB时
        List<ProcedureEntry> result = clobParser.parseProcedures(clob);
        
        // 那么应该正确解析手术条目
        assertThat(result).hasSize(1);
        ProcedureEntry entry = result.get(0);
        assertThat(entry.getProcedureCode()).isEqualTo("35.2400");
        assertThat(entry.getProcedureName()).isEqualTo("二尖瓣置换术(机械瓣)");
        assertThat(entry.getAliases()).containsExactly("MVR", "二尖瓣机械瓣置换");
    }

    @Test
    void shouldParseDiagnosisWithBracketsInName() {
        // 给定一个诊断名称中包含括号的CLOB
        String clob = "I25.110 陈旧性心肌梗死(前壁)";
        
        // 当解析CLOB时
        List<DiagnosisEntry> result = clobParser.parseDiagnoses(clob);
        
        // 那么应该正确解析诊断条目
        assertThat(result).hasSize(1);
        DiagnosisEntry entry = result.get(0);
        assertThat(entry.getIcdCode()).isEqualTo("I25.110");
        assertThat(entry.getDiagnosisName()).isEqualTo("陈旧性心肌梗死(前壁)");
        assertThat(entry.getAliases()).isEmpty();
    }

    @Test
    void shouldParseProcedureWithSquareBracketsInName() {
        // 给定一个手术名称中包含方括号的CLOB
        String clob = "93.9000x002 无创呼吸机辅助通气(双水平气道正压[BiPAP])";
        
        // 当解析CLOB时
        List<ProcedureEntry> result = clobParser.parseProcedures(clob);
        
        // 那么应该正确解析手术条目
        assertThat(result).hasSize(1);
        ProcedureEntry entry = result.get(0);
        assertThat(entry.getProcedureCode()).isEqualTo("93.9000x002");
        assertThat(entry.getProcedureName()).isEqualTo("无创呼吸机辅助通气(双水平气道正压[BiPAP])");
        assertThat(entry.getAliases()).isEmpty();
    }

    @Test
    void shouldParseProcedureWithMultipleSquareBracketsInName() {
        // 给定一个手术名称中包含多个方括号的CLOB
        String clob = "93.9000x003 无创呼吸机辅助通气(高频通气[HFPPV])";
        
        // 当解析CLOB时
        List<ProcedureEntry> result = clobParser.parseProcedures(clob);
        
        // 那么应该正确解析手术条目
        assertThat(result).hasSize(1);
        ProcedureEntry entry = result.get(0);
        assertThat(entry.getProcedureCode()).isEqualTo("93.9000x003");
        assertThat(entry.getProcedureName()).isEqualTo("无创呼吸机辅助通气(高频通气[HFPPV])");
        assertThat(entry.getAliases()).isEmpty();
    }

    @Test
    void shouldParseDiagnosisWithSquareBracketsInName() {
        // 给定一个诊断名称中包含方括号的CLOB
        String clob = "O13.x00 妊娠[妊娠引起的]高血压";
        
        // 当解析CLOB时
        List<DiagnosisEntry> result = clobParser.parseDiagnoses(clob);
        
        // 那么应该正确解析诊断条目
        assertThat(result).hasSize(1);
        DiagnosisEntry entry = result.get(0);
        assertThat(entry.getIcdCode()).isEqualTo("O13.x00");
        assertThat(entry.getDiagnosisName()).isEqualTo("妊娠[妊娠引起的]高血压");
        assertThat(entry.getAliases()).isEmpty();
    }

    @Test
    void shouldParseMixedEntriesWithAndWithoutSquareBrackets() {
        // 给定一个混合了包含和不包含方括号的CLOB
        String clob = "93.9000x002 无创呼吸机辅助通气(双水平气道正压[BiPAP])\n" +
                     "93.9000x003 无创呼吸机辅助通气(高频通气[HFPPV])\n" +
                     "O13.x00 妊娠[妊娠引起的]高血压\n" +
                     "I48.000 阵发性心房颤动[房颤]";
        
        // 当解析CLOB时
        List<DiagnosisEntry> result = clobParser.parseDiagnoses(clob);
        
        // 那么应该正确解析所有条目
        assertThat(result).hasSize(4);
        assertThat(result).extracting(DiagnosisEntry::getIcdCode)
            .containsExactly("93.9000x002", "93.9000x003", "O13.x00", "I48.000");
        assertThat(result).extracting(DiagnosisEntry::getDiagnosisName)
            .containsExactly(
                "无创呼吸机辅助通气(双水平气道正压[BiPAP])",
                "无创呼吸机辅助通气(高频通气[HFPPV])",
                "妊娠[妊娠引起的]高血压",
                "阵发性心房颤动"
            );
        // 检查别名
        assertThat(result.get(3).getAliases()).containsExactly("房颤");
    }
}
