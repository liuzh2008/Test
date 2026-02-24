package com.example.medaiassistant.drg.matching;

/**
 * 名称相似度匹配器
 * 
 * 实现诊断和手术名称的相似度匹配功能
 * 使用Levenshtein距离算法计算相似度
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-22
 */
public class NameMatcher {

    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.7;

    private NameMatcher() {
        // 工具类，防止实例化
    }

    /**
     * 执行名称相似度匹配（使用默认阈值）
     * 
     * 匹配规则：
     * - 使用Levenshtein距离算法计算相似度
     * - 默认相似度阈值为0.7
     * - 如果任一名称为null或空字符串，返回false
     * 
     * @param patientName 患者的诊断或手术名称
     * @param drgName DRG记录中的诊断或手术名称
     * @return 如果名称相似度达到阈值返回true，否则返回false
     */
    public static boolean similarityMatch(String patientName, String drgName) {
        return similarityMatch(patientName, drgName, DEFAULT_SIMILARITY_THRESHOLD);
    }

    /**
     * 执行名称相似度匹配（使用自定义阈值）
     * 
     * 匹配规则：
     * - 使用Levenshtein距离算法计算相似度
     * - 如果任一名称为null或空字符串，返回false
     * - 相似度必须达到或超过指定阈值
     * 
     * @param patientName 患者的诊断或手术名称
     * @param drgName DRG记录中的诊断或手术名称
     * @param threshold 相似度阈值（0.0-1.0）
     * @return 如果名称相似度达到阈值返回true，否则返回false
     */
    public static boolean similarityMatch(String patientName, String drgName, double threshold) {
        // 处理null值
        if (patientName == null || drgName == null) {
            return false;
        }
        
        // 处理空字符串
        if (patientName.isEmpty() || drgName.isEmpty()) {
            return false;
        }
        
        // 验证阈值范围
        if (threshold <= 0.0 || threshold >= 1.0) {
            throw new IllegalArgumentException("相似度阈值必须在0.0到1.0之间");
        }
        
        // 计算相似度并比较阈值
        double similarity = calculateSimilarity(patientName, drgName);
        return similarity >= threshold;
    }

    /**
     * 计算两个名称之间的相似度
     * 
     * 使用改进的相似度算法，结合Levenshtein距离和字符相似度：
     * 1. 如果名称完全相同，返回1.0
     * 2. 如果名称包含关系，返回较高相似度
     * 3. 使用Levenshtein距离作为基础
     * 
     * @param name1 第一个名称
     * @param name2 第二个名称
     * @return 相似度值（0.0-1.0）
     */
    public static double calculateSimilarity(String name1, String name2) {
        // 处理null值
        if (name1 == null || name2 == null) {
            return 0.0;
        }
        
        // 处理空字符串
        if (name1.isEmpty() || name2.isEmpty()) {
            return 0.0;
        }
        
        // 如果名称完全相同，相似度为1.0
        if (name1.equals(name2)) {
            return 1.0;
        }
        
        // 检查包含关系（双向）
        if (name1.contains(name2) || name2.contains(name1)) {
            int shorterLength = Math.min(name1.length(), name2.length());
            int longerLength = Math.max(name1.length(), name2.length());
            return 0.8 + 0.2 * ((double) shorterLength / longerLength);
        }
        
        // 计算Levenshtein距离
        int distance = calculateLevenshteinDistance(name1, name2);
        
        // 计算相似度
        int maxLength = Math.max(name1.length(), name2.length());
        if (maxLength == 0) {
            return 1.0; // 两个空字符串
        }
        
        double levenshteinSimilarity = 1.0 - (double) distance / maxLength;
        
        // 对于短字符串，相似度要求更高
        if (maxLength <= 3) {
            return levenshteinSimilarity * 0.8;
        }
        
        return levenshteinSimilarity;
    }

    /**
     * 计算两个字符串之间的Levenshtein距离
     * 
     * Levenshtein距离是指两个字符串之间，由一个转换成另一个所需的最少编辑操作次数。
     * 允许的编辑操作包括插入一个字符、删除一个字符、替换一个字符。
     * 
     * @param str1 第一个字符串
     * @param str2 第二个字符串
     * @return Levenshtein距离
     */
    private static int calculateLevenshteinDistance(String str1, String str2) {
        int len1 = str1.length();
        int len2 = str2.length();
        
        // 创建距离矩阵
        int[][] distance = new int[len1 + 1][len2 + 1];
        
        // 初始化第一行和第一列
        for (int i = 0; i <= len1; i++) {
            distance[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            distance[0][j] = j;
        }
        
        // 填充距离矩阵
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1;
                
                distance[i][j] = Math.min(
                    Math.min(
                        distance[i - 1][j] + 1,     // 删除
                        distance[i][j - 1] + 1      // 插入
                    ),
                    distance[i - 1][j - 1] + cost   // 替换
                );
            }
        }
        
        return distance[len1][len2];
    }

    /**
     * 检查名称是否有效
     * 
     * 有效性规则：
     * - 非null
     * - 非空字符串
     * - 至少包含一个非空白字符
     * 
     * @param name 名称
     * @return 如果名称有效返回true，否则返回false
     */
    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty();
    }


    /**
     * 获取默认相似度阈值
     * 
     * @return 默认相似度阈值
     */
    public static double getDefaultSimilarityThreshold() {
        return DEFAULT_SIMILARITY_THRESHOLD;
    }
}
