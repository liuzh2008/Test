package com.example.medaiassistant.dto;

import java.util.List;

public class OrderTimelineDTO {
    private String orderKey; // 药品名+剂量+单位+频次+途径
    private List<String> timelines; // 时间线列表
    
    public OrderTimelineDTO(String orderKey, List<String> timelines) {
        this.orderKey = orderKey;
        this.timelines = timelines;
    }

    public String getOrderKey() {
        return orderKey;
    }

    public List<String> getTimelines() {
        return timelines;
    }

    public String formatOrder() {
        if (timelines == null || timelines.isEmpty()) {
            return orderKey;
        }
        return orderKey + "（" + String.join("，", timelines) + "）";
    }
}
