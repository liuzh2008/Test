package com.example.medaiassistant.drg.orchestrator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DRGs流程处理器注册表
 * 负责管理流程处理器的动态注册和获取
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-11
 */
public class DrgFlowProcessorRegistry {

    private final Map<String, DrgFlowProcessor> processorMap;
    private final List<DrgFlowProcessor> processorList;
    private boolean needsSorting;

    /**
     * 构造函数
     */
    public DrgFlowProcessorRegistry() {
        this.processorMap = new ConcurrentHashMap<>();
        this.processorList = new ArrayList<>();
        this.needsSorting = false;
    }

    /**
     * 注册处理器
     * 
     * @param processor 要注册的处理器
     * @throws IllegalArgumentException 如果处理器名称为空或已存在同名处理器
     */
    public void register(DrgFlowProcessor processor) {
        if (processor == null) {
            throw new IllegalArgumentException("处理器不能为null");
        }
        
        String name = processor.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("处理器名称不能为空");
        }

        synchronized (this) {
            if (processorMap.containsKey(name)) {
                throw new IllegalArgumentException("处理器名称已存在: " + name);
            }
            
            processorMap.put(name, processor);
            processorList.add(processor);
            needsSorting = true;
        }
    }

    /**
     * 按名称获取处理器
     * 
     * @param name 处理器名称
     * @return 对应的处理器，如果不存在则返回null
     */
    public DrgFlowProcessor getProcessor(String name) {
        return processorMap.get(name);
    }

    /**
     * 获取所有处理器（按执行顺序排序）
     * 
     * @return 按执行顺序排序的处理器列表
     */
    public List<DrgFlowProcessor> getAllProcessors() {
        synchronized (this) {
            if (needsSorting) {
                processorList.sort(Comparator.comparingInt(DrgFlowProcessor::getOrder));
                needsSorting = false;
            }
            return new ArrayList<>(processorList);
        }
    }

    /**
     * 检查处理器是否存在
     * 
     * @param name 处理器名称
     * @return 如果存在则返回true
     */
    public boolean containsProcessor(String name) {
        return processorMap.containsKey(name);
    }

    /**
     * 获取处理器数量
     * 
     * @return 已注册的处理器数量
     */
    public int size() {
        return processorMap.size();
    }

    /**
     * 检查注册表是否为空
     * 
     * @return 如果为空则返回true
     */
    public boolean isEmpty() {
        return processorMap.isEmpty();
    }

    /**
     * 移除处理器
     * 
     * @param name 要移除的处理器名称
     * @return 被移除的处理器，如果不存在则返回null
     */
    public DrgFlowProcessor removeProcessor(String name) {
        synchronized (this) {
            DrgFlowProcessor removed = processorMap.remove(name);
            if (removed != null) {
                processorList.remove(removed);
                needsSorting = true;
            }
            return removed;
        }
    }

    /**
     * 清空所有处理器
     */
    public void clear() {
        synchronized (this) {
            processorMap.clear();
            processorList.clear();
            needsSorting = false;
        }
    }

    /**
     * 获取所有处理器名称
     * 
     * @return 处理器名称集合
     */
    public Set<String> getProcessorNames() {
        return new HashSet<>(processorMap.keySet());
    }
}
