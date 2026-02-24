package com.example.medaiassistant.util;

import com.example.medaiassistant.constant.AIDisclaimerConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * AI内容响应包装工具类
 * 
 * <p>用于在API响应中统一添加AI免责声明字段，满足《生成式人工智能服务管理暂行办法》合规要求。</p>
 * 
 * <p>本工具类提供多种包装方法，适用于不同的响应场景：</p>
 * <ul>
 *   <li>{@link #wrapWithDisclaimer(Object)} - 包装任意对象为标准响应格式</li>
 *   <li>{@link #addDisclaimerToMap(Map)} - 向现有Map添加免责声明字段</li>
 *   <li>{@link #createStreamResponse(String, String)} - 创建流式响应格式</li>
 *   <li>{@link #createFullResponse(String, String, String)} - 创建完整的非流式响应</li>
 * </ul>
 * 
 * <p>使用示例：</p>
 * <pre>{@code
 * // 包装列表数据
 * List<PatientPromptResultDTO> results = repository.findAll();
 * return ResponseEntity.ok(AIContentResponseWrapper.wrapWithDisclaimer(results));
 * 
 * // 输出格式:
 * // {
 * //   "data": [...],
 * //   "aiDisclaimer": "本内容由AI生成，仅供参考"
 * // }
 * }</pre>
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2026-02-16
 * @see AIDisclaimerConstants
 */
public final class AIContentResponseWrapper {

    /**
     * 私有构造函数，防止实例化
     * 
     * <p>本类仅提供静态工具方法，不应被实例化。</p>
     */
    private AIContentResponseWrapper() {
        // 防止实例化
    }

    /**
     * 包装任意对象，添加AI免责声明
     * 
     * <p>将原始数据对象包装到标准响应格式中，自动添加AI免责声明字段。</p>
     * 
     * <p>输出格式：</p>
     * <pre>{@code
     * {
     *   "data": <原始数据>,
     *   "aiDisclaimer": "本内容由AI生成，仅供参考"
     * }
     * }</pre>
     * 
     * @param data 原始数据对象，可以是任意类型（List、DTO、String等）
     * @return 包含data和aiDisclaimer字段的Map，不会返回null
     */
    public static Map<String, Object> wrapWithDisclaimer(Object data) {
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put(AIDisclaimerConstants.DATA_FIELD, data);
        wrapper.put(AIDisclaimerConstants.AI_DISCLAIMER_FIELD, AIDisclaimerConstants.AI_DISCLAIMER);
        return wrapper;
    }

    /**
     * 向现有Map添加AI免责声明字段
     * 
     * <p>在不改变原有数据结构的情况下，向Map中添加免责声明字段。
     * 注意：此方法会直接修改传入的Map对象。</p>
     * 
     * @param map 原始Map对象，如果为null则直接返回null
     * @return 添加了aiDisclaimer字段的Map（原地修改），如果输入为null则返回null
     */
    public static Map<String, Object> addDisclaimerToMap(Map<String, Object> map) {
        if (map != null) {
            map.put(AIDisclaimerConstants.AI_DISCLAIMER_FIELD, AIDisclaimerConstants.AI_DISCLAIMER);
        }
        return map;
    }

    /**
     * 创建包含AI免责声明的新Map
     * 
     * <p>创建一个仅包含免责声明字段的新Map，可用于后续添加其他数据。</p>
     * 
     * @return 包含aiDisclaimer字段的新HashMap实例
     */
    public static Map<String, Object> createMapWithDisclaimer() {
        Map<String, Object> map = new HashMap<>();
        map.put(AIDisclaimerConstants.AI_DISCLAIMER_FIELD, AIDisclaimerConstants.AI_DISCLAIMER);
        return map;
    }

    /**
     * 创建包含内容和AI免责声明的Map（用于流式响应）
     * 
     * <p>专为流式AI响应设计，创建包含content、可选的reasoning_content和免责声明的响应Map。</p>
     * 
     * <p>输出格式：</p>
     * <pre>{@code
     * {
     *   "content": "AI生成内容",
     *   "reasoning_content": "推理过程（可选）",
     *   "aiDisclaimer": "本内容由AI生成，仅供参考"
     * }
     * }</pre>
     * 
     * @param content AI生成的内容，如果为null则设为空字符串
     * @param reasoningContent 推理内容，可为null（为null时不包含该字段）
     * @return 包含content、可选reasoning_content和aiDisclaimer字段的Map
     */
    public static Map<String, Object> createStreamResponse(String content, String reasoningContent) {
        Map<String, Object> response = new HashMap<>();
        response.put("content", content != null ? content : "");
        if (reasoningContent != null) {
            response.put("reasoning_content", reasoningContent);
        }
        response.put(AIDisclaimerConstants.AI_DISCLAIMER_FIELD, AIDisclaimerConstants.AI_DISCLAIMER);
        return response;
    }

    /**
     * 创建完整的AI响应Map（用于非流式响应）
     * 
     * <p>创建包含所有标准字段的完整AI响应Map，适用于非流式API响应。</p>
     * 
     * <p>输出格式：</p>
     * <pre>{@code
     * {
     *   "content": "AI生成内容",
     *   "reasoning_content": "推理过程",
     *   "error": null,
     *   "aiDisclaimer": "本内容由AI生成，仅供参考"
     * }
     * }</pre>
     * 
     * @param content AI生成的内容，如果为null则设为空字符串
     * @param reasoningContent 推理内容，如果为null则设为空字符串
     * @param error 错误信息，可为null
     * @return 包含完整响应字段和aiDisclaimer的Map
     */
    public static Map<String, Object> createFullResponse(String content, String reasoningContent, String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("content", content != null ? content : "");
        response.put("reasoning_content", reasoningContent != null ? reasoningContent : "");
        response.put("error", error);
        response.put(AIDisclaimerConstants.AI_DISCLAIMER_FIELD, AIDisclaimerConstants.AI_DISCLAIMER);
        return response;
    }

    /**
     * 获取AI免责声明文本
     * 
     * <p>返回标准的AI免责声明文本常量。</p>
     * 
     * @return AI免责声明文本："本内容由AI生成，仅供参考"
     */
    public static String getDisclaimer() {
        return AIDisclaimerConstants.AI_DISCLAIMER;
    }

    /**
     * 获取AI免责声明字段名
     * 
     * <p>返回在JSON响应中使用的免责声明字段名。</p>
     * 
     * @return AI免责声明字段名："aiDisclaimer"
     */
    public static String getDisclaimerFieldName() {
        return AIDisclaimerConstants.AI_DISCLAIMER_FIELD;
    }
}
