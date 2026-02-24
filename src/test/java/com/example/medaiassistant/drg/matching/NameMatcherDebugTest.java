package com.example.medaiassistant.drg.matching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 名称相似度匹配器调试测试
 * 
 * 用于调试相似度计算的具体值
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-22
 */
@DisplayName("名称相似度匹配器调试测试")
class NameMatcherDebugTest {

    @Test
    @DisplayName("调试相似度计算")
    void debugSimilarityCalculation() {
        // 测试用例1: 完全相同名称
        String name1 = "高血压";
        String name2 = "高血压";
        double similarity = NameMatcher.calculateSimilarity(name1, name2);
        System.out.println("'高血压' vs '高血压': " + similarity);
        
        // 测试用例2: 包含关系名称
        name1 = "急性心肌梗死";
        name2 = "心肌梗死";
        similarity = NameMatcher.calculateSimilarity(name1, name2);
        System.out.println("'急性心肌梗死' vs '心肌梗死': " + similarity);
        
        // 测试用例3: 部分相似名称
        name1 = "慢性阻塞性肺疾病";
        name2 = "慢性阻塞性肺病";
        similarity = NameMatcher.calculateSimilarity(name1, name2);
        System.out.println("'慢性阻塞性肺疾病' vs '慢性阻塞性肺病': " + similarity);
        
        // 测试用例4: 相似名称
        name1 = "冠状动脉粥样硬化性心脏病";
        name2 = "冠状动脉粥样硬化性心脏病";
        similarity = NameMatcher.calculateSimilarity(name1, name2);
        System.out.println("'冠状动脉粥样硬化性心脏病' vs '冠状动脉粥样硬化性心脏病': " + similarity);
        
        // 测试用例5: 低相似度名称
        name1 = "高血压";
        name2 = "低血压";
        similarity = NameMatcher.calculateSimilarity(name1, name2);
        System.out.println("'高血压' vs '低血压': " + similarity);
        
        // 测试用例6: 完全不同名称
        name1 = "糖尿病";
        name2 = "低血糖";
        similarity = NameMatcher.calculateSimilarity(name1, name2);
        System.out.println("'糖尿病' vs '低血糖': " + similarity);
    }
}
