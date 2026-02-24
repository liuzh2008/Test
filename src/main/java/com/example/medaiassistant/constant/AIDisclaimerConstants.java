package com.example.medaiassistant.constant;

/**
 * AI免责声明常量类
 * 
 * <p>根据《生成式人工智能服务管理暂行办法》合规要求，
 * 所有AI生成内容需统一标注免责声明。本类集中管理所有与AI免责声明相关的常量。</p>
 * 
 * <p>使用示例：</p>
 * <pre>{@code
 * Map<String, Object> response = new HashMap<>();
 * response.put(AIDisclaimerConstants.AI_DISCLAIMER_FIELD, AIDisclaimerConstants.AI_DISCLAIMER);
 * }</pre>
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2026-02-16
 * @see com.example.medaiassistant.util.AIContentResponseWrapper
 */
public final class AIDisclaimerConstants {

    /**
     * 私有构造函数，防止实例化
     * 
     * <p>本类仅提供静态常量，不应被实例化。</p>
     */
    private AIDisclaimerConstants() {
        // 防止实例化
    }

    /**
     * AI生成内容免责声明文本
     * 
     * <p>该文本将展示在所有AI生成内容的响应中，告知用户内容由AI生成，仅供参考。</p>
     * 
     * <p>值: "本内容由AI生成，仅供参考"</p>
     */
    public static final String AI_DISCLAIMER = "本内容由AI生成，仅供参考";

    /**
     * AI免责声明字段名
     * 
     * <p>在API响应JSON中，该字段名用于存放免责声明文本。</p>
     * 
     * <p>值: "aiDisclaimer"</p>
     */
    public static final String AI_DISCLAIMER_FIELD = "aiDisclaimer";

    /**
     * 数据字段名（用于包装响应）
     * 
     * <p>在API响应JSON中，该字段名用于存放实际的业务数据。</p>
     * 
     * <p>值: "data"</p>
     */
    public static final String DATA_FIELD = "data";
}
