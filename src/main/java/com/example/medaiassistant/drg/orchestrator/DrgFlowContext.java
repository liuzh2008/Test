package com.example.medaiassistant.drg.orchestrator;

import java.util.HashMap;
import java.util.Map;

/**
 * DRGs流程上下文
 * 用于在处理器间传递数据和状态
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-11
 */
public class DrgFlowContext {

    private final Map<String, Object> attributes;

    /**
     * 构造函数
     */
    public DrgFlowContext() {
        this.attributes = new HashMap<>();
    }

    /**
     * 设置属性值
     * 
     * @param key 属性键
     * @param value 属性值
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 获取属性值
     * 
     * @param key 属性键
     * @return 属性值，如果不存在则返回null
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * 获取属性值，如果不存在则返回默认值
     * 
     * @param key 属性键
     * @param defaultValue 默认值
     * @return 属性值或默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, T defaultValue) {
        Object value = attributes.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 检查是否包含指定属性
     * 
     * @param key 属性键
     * @return 如果包含则返回true
     */
    public boolean containsAttribute(String key) {
        return attributes.containsKey(key);
    }

    /**
     * 移除属性
     * 
     * @param key 属性键
     * @return 被移除的属性值
     */
    public Object removeAttribute(String key) {
        return attributes.remove(key);
    }

    /**
     * 获取所有属性
     * 
     * @return 属性映射
     */
    public Map<String, Object> getAttributes() {
        return new HashMap<>(attributes);
    }

    /**
     * 清空所有属性
     */
    public void clear() {
        attributes.clear();
    }

    /**
     * 获取属性数量
     * 
     * @return 属性数量
     */
    public int size() {
        return attributes.size();
    }

    /**
     * 检查是否为空
     * 
     * @return 如果为空则返回true
     */
    public boolean isEmpty() {
        return attributes.isEmpty();
    }
}
