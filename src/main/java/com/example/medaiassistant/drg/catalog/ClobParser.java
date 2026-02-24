package com.example.medaiassistant.drg.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CLOB解析器实现
 * 
 * 负责将DRGs表中的CLOB字段解析为结构化的诊断和手术数据。
 * 支持ICD编码提取、诊断名称解析、别名解析等功能。
 * 
 * @author MedAiAssistant Team
 * @version 1.0
 * @since 2025-10-22
 */
public class ClobParser {

    // 改进的正则表达式：更精确地匹配CLOB格式
    // 格式：编码 + 空格 + 名称 + [别名1,别名2,...]
    // 支持名称中包含方括号的情况
    private static final Pattern ENTRY_PATTERN = Pattern.compile("^(\\S+)\\s+([^\\[]+?)(?:\\[(.+)\\])?$|^(\\S+)\\s+(.+)$");
    
    // 别名分隔符模式
    private static final Pattern ALIAS_DELIMITER = Pattern.compile("\\s*,\\s*");

    /**
     * 解析诊断CLOB
     * @param clob 诊断CLOB字符串
     * @return 诊断条目列表
     */
    public List<DiagnosisEntry> parseDiagnoses(String clob) {
        return parseClob(clob, (code, name, aliases) -> {
            DiagnosisEntry entry = new DiagnosisEntry(code, name);
            aliases.forEach(entry::addAlias);
            return entry;
        });
    }

    /**
     * 解析手术CLOB
     * @param clob 手术CLOB字符串
     * @return 手术条目列表
     */
    public List<ProcedureEntry> parseProcedures(String clob) {
        return parseClob(clob, (code, name, aliases) -> {
            ProcedureEntry entry = new ProcedureEntry(code, name);
            aliases.forEach(entry::addAlias);
            return entry;
        });
    }

    /**
     * 通用的CLOB解析逻辑
     * @param clob CLOB字符串
     * @param entryFactory 条目工厂
     * @param <T> 条目类型
     * @return 条目列表
     */
    private <T> List<T> parseClob(String clob, EntryFactory<T> entryFactory) {
        List<T> entries = new ArrayList<>();
        
        if (clob == null || clob.trim().isEmpty()) {
            return entries;
        }
        
        String[] lines = clob.split("\\r?\\n");
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                continue;
            }
            
            // 尝试两种模式匹配
            Matcher matcher = ENTRY_PATTERN.matcher(trimmedLine);
            if (matcher.matches()) {
                String code, name, aliasPart;
                
                // 检查是哪种模式匹配成功
                if (matcher.group(1) != null) {
                    // 模式1: 编码 + 空格 + 名称 + [别名]
                    code = matcher.group(1).trim();
                    name = matcher.group(2).trim();
                    aliasPart = matcher.group(3);
                } else {
                    // 模式2: 编码 + 空格 + 名称（包含方括号）
                    code = matcher.group(4).trim();
                    name = matcher.group(5).trim();
                    aliasPart = null;
                }
                
                List<String> aliases = parseAliases(aliasPart);
                T entry = entryFactory.create(code, name, aliases);
                entries.add(entry);
            } else {
                // 记录格式错误的行，但不抛出异常以保持容错性
                System.err.println("无法解析CLOB行: " + trimmedLine);
            }
        }
        
        return entries;
    }

    /**
     * 解析别名列表
     * @param aliasPart 别名部分字符串
     * @return 别名列表
     */
    private List<String> parseAliases(String aliasPart) {
        List<String> aliases = new ArrayList<>();
        
        if (aliasPart != null && !aliasPart.trim().isEmpty()) {
            String[] aliasArray = ALIAS_DELIMITER.split(aliasPart.trim());
            for (String alias : aliasArray) {
                String trimmedAlias = alias.trim();
                if (!trimmedAlias.isEmpty()) {
                    aliases.add(trimmedAlias);
                }
            }
        }
        
        return aliases;
    }

    /**
     * 条目工厂接口
     * @param <T> 条目类型
     */
    @FunctionalInterface
    private interface EntryFactory<T> {
        T create(String code, String name, List<String> aliases);
    }
}
