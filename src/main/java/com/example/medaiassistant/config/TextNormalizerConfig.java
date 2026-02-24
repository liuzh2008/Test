package com.example.medaiassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * TextNormalizer配置类
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-20
 */
@Configuration
@ConfigurationProperties(prefix = "text.normalizer")
public class TextNormalizerConfig {
    
    /**
     * 修饰词列表 - 这些词会被从文本中去除
     */
    private List<String> modifiers = Arrays.asList(
        "未特指",
        "左侧", "右侧", "双侧", "单侧", "单发", "多发", "复发性",
        "早期", "晚期", "初期", "末期", 
        "典型", "非典型", "特殊类型", "普通型", "重型", "危重型"
    );
    
    /**
     * 数字转换映射 - 格式为 "原格式:目标格式"
     */
    private List<String> numberConversions = Arrays.asList(
        "Ⅰ:1", "Ⅱ:2", "Ⅲ:3", "Ⅳ:4", "Ⅴ:5",
        "Ⅵ:6", "Ⅶ:7", "Ⅷ:8", "Ⅸ:9", "Ⅹ:10",
        "型1:1型", "型2:2型", "型3:3型", "型4:4型", "型5:5型",
        "型一:1型", "型二:2型", "型三:3型", "型四:4型", "型五:5型"
    );
    
    /**
     * 全角字符
     */
    private String fullWidthChars = "，。！？；：＂＇（）［］｛｝《》＜＞＼｜＾～｀＠＃＄％＾＆＊＋－＝　１２３４５６７８９０ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ";
    
    /**
     * 半角字符
     */
    private String halfWidthChars = ",.!?;:\"'()[]{}《》<>\\|^~`@#$%^&*+-= 1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    // Getters and Setters
    public List<String> getModifiers() {
        return modifiers;
    }

    public void setModifiers(List<String> modifiers) {
        this.modifiers = modifiers;
    }

    public List<String> getNumberConversions() {
        return numberConversions;
    }

    public void setNumberConversions(List<String> numberConversions) {
        this.numberConversions = numberConversions;
    }

    public String getFullWidthChars() {
        return fullWidthChars;
    }

    public void setFullWidthChars(String fullWidthChars) {
        this.fullWidthChars = fullWidthChars;
    }

    public String getHalfWidthChars() {
        return halfWidthChars;
    }

    public void setHalfWidthChars(String halfWidthChars) {
        this.halfWidthChars = halfWidthChars;
    }
}
