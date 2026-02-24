package com.example.medaiassistant.dto.drg;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DRG目录数据传输对象
 * 
 * 用于封装DRG记录集合，为匹配算法提供数据源
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-22
 */
@Getter
public class DrgCatalog {
    
    /**
     * DRG记录列表
     */
    private final List<DrgParsedRecord> drgRecords;
    
    /**
     * 构造函数
     * 
     * @param drgRecords DRG记录列表，如果为null则创建空列表
     */
    public DrgCatalog(List<DrgParsedRecord> drgRecords) {
        this.drgRecords = drgRecords != null ? new ArrayList<>(drgRecords) : new ArrayList<>();
    }
    
    /**
     * 判断目录是否为空
     * 
     * @return 如果目录为空返回true，否则返回false
     */
    public boolean isEmpty() {
        return drgRecords.isEmpty();
    }
    
    /**
     * 获取DRG记录数量
     * 
     * @return DRG记录数量
     */
    public int size() {
        return drgRecords.size();
    }
    
    /**
     * 获取不可修改的DRG记录列表
     * 
     * @return 不可修改的DRG记录列表
     */
    public List<DrgParsedRecord> getDrgRecords() {
        return Collections.unmodifiableList(drgRecords);
    }
    
    @Override
    public String toString() {
        return "DrgCatalog{" +
                "drgRecords=" + drgRecords +
                '}';
    }
}
