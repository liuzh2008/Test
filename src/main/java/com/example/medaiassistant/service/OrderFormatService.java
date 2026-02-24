package com.example.medaiassistant.service;

import com.example.medaiassistant.dto.OrderTimelineDTO;
import com.example.medaiassistant.model.LongTermOrder;
import com.example.medaiassistant.repository.LongTermOrderRepository;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.StringJoiner;

@Service
public class OrderFormatService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final LongTermOrderRepository orderRepository;

    public OrderFormatService(LongTermOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public String formatTemporaryOrders(List<OrderTimelineDTO> orders) {
        if (orders == null || orders.isEmpty()) {
            return "";
        }

        // 按日期时间分组医嘱内容
        Map<String, List<String>> groupedOrders = new LinkedHashMap<>();
        for (OrderTimelineDTO order : orders) {
            String[] parts = order.getOrderKey().split(" ", 2);
            if (parts.length < 2) {
                continue;
            }

            String dateTimeStr = parts[0];
            String orderContent = parts[1];

            groupedOrders.computeIfAbsent(dateTimeStr, k -> new ArrayList<>())
                        .add(orderContent);
        }

        // 按日期时间排序
        List<String> sortedTimes = new ArrayList<>(groupedOrders.keySet());
        sortedTimes.sort(Comparator.naturalOrder());

        // 构建结果字符串
        StringBuilder sb = new StringBuilder();
        for (String time : sortedTimes) {
            sb.append(time).append("\n");
            for (String content : groupedOrders.get(time)) {
                sb.append(content).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString().trim();
    }

    public List<String> formatLongTermOrders(String patientId) {
        List<LongTermOrder> orders = orderRepository.findByPatientIdAndRepeatIndicatorOrderByOrderDateAsc(patientId, 1);
        
        Map<String, List<LongTermOrder>> groupedOrders = orders.stream()
            .collect(Collectors.groupingBy(this::getOrderKey, LinkedHashMap::new, Collectors.toList()));
        
        return groupedOrders.entrySet().stream()
            .map(entry -> {
                List<String> timelines = buildTimeline(entry.getValue());
                return new OrderTimelineDTO(entry.getKey(), timelines).formatOrder();
            })
            .collect(Collectors.toList());
    }

    private String getOrderKey(LongTermOrder order) {
        if (order == null || order.getOrderName() == null) {
            return "";
        }
        
        StringJoiner joiner = new StringJoiner(" ");
        joiner.add(order.getOrderName());
        
        if (order.getDosage() != null && order.getUnit() != null) {
            joiner.add(order.getDosage() + order.getUnit());
        } else if (order.getDosage() != null) {
            joiner.add(order.getDosage());
        } else if (order.getUnit() != null) {
            joiner.add(order.getUnit());
        }
        
        if (order.getFrequency() != null) {
            joiner.add(order.getFrequency());
        }
        
        if (order.getRoute() != null) {
            joiner.add(order.getRoute());
        }
        
        return joiner.toString();
    }

    private List<String> buildTimeline(List<LongTermOrder> orders) {
        List<String> timelines = new ArrayList<>();
        if (orders == null || orders.isEmpty()) {
            return timelines;
        }

        int i = 0;
        while (i < orders.size()) {
            LongTermOrder current = orders.get(i);
            if (current == null || current.getOrderDate() == null) {
                i++;
                continue;
            }
            
            if (current.getStopTime() == null) {
                i++;
                continue;
            }

            Date stopTime = current.getStopTime();
            while (i + 1 < orders.size()) {
                LongTermOrder next = orders.get(i + 1);
                if (next == null || next.getOrderDate() == null || stopTime == null) {
                    i++;
                    continue;
                }
                
                long hoursDiff = (next.getOrderDate().getTime() - stopTime.getTime()) / (1000 * 60 * 60);
                if (hoursDiff <= 24 && next.getStopTime() != null) {
                    stopTime = next.getStopTime();
                    i++;
                } else {
                    break;
                }
            }

            if (stopTime != null) {
                timelines.add(String.format("%s至%s停用",
                    current.getOrderDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FORMAT),
                    stopTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FORMAT)));
            }
            i++;
        }
        return timelines;
    }
}
