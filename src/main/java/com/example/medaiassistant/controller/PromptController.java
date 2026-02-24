package com.example.medaiassistant.controller;

import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.service.PromptService;
import com.example.medaiassistant.service.TimerPromptGenerator;
import com.example.medaiassistant.service.PromptPollingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/prompts")
public class PromptController {
    private final PromptService promptService;
    private final TimerPromptGenerator timerPromptGenerator;
    private final PromptPollingService promptPollingService;

    public PromptController(PromptService promptService, TimerPromptGenerator timerPromptGenerator, PromptPollingService promptPollingService) {
        this.promptService = promptService;
        this.timerPromptGenerator = timerPromptGenerator;
        this.promptPollingService = promptPollingService;
    }

    @GetMapping("/pending")
    public Map<String, Object> getPendingPrompts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Prompt> promptPage = promptService.getPendingPrompts(PageRequest.of(page, size));
        Map<String, Object> response = new HashMap<>();
        response.put("prompts", promptPage.getContent());
        response.put("totalCount", promptPage.getTotalElements());
        return response;
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<Map<String, String>> getPromptResult(@PathVariable Integer id) {
        Map<String, String> response = new HashMap<>();
        response.put("executionResult", promptService.getPromptResult(id));
        return ResponseEntity.ok(response);
    }

    /**
     * 手动生成所有Prompt接口
     * 用于测试线程池分离效果，模拟定时任务生成所有在院患者的Prompt
     * 
     * @return 生成结果统计信息
     */
    @PostMapping("/generate-all-manual")
    public ResponseEntity<Map<String, Object>> generateAllPromptsManually() {
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", "SUCCESS");
        result.put("message", "手动生成所有Prompt任务已提交");
        result.put("timestamp", System.currentTimeMillis());
        
        // 异步执行Prompt生成任务，避免阻塞HTTP请求
        new Thread(() -> {
            try {
                System.out.println("开始手动生成所有Prompt任务");
                
                // 直接调用TimerPromptGenerator的dailyPromptGeneration方法
                // 使用@Async注解，确保在专用线程池中执行
                timerPromptGenerator.dailyPromptGeneration();
                
                System.out.println("手动生成所有Prompt任务完成");
            } catch (Exception e) {
                System.err.println("手动生成所有Prompt任务失败: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取轮询状态统计
     * 统计Prompts表中待处理、SUBMISSION_STARTED的记录
     * 
     * @return 轮询状态统计响应
     */
    @GetMapping("/polling/status")
    public ResponseEntity<Map<String, Object>> getPollingStatus() {
        Map<String, Object> response = new HashMap<>();
        try {
            // 获取Prompt表状态统计
            String promptStats = promptPollingService.getPromptStatusStats();
            
            // 获取轮询服务启用状态
            boolean pollingEnabled = promptPollingService.isPollingEnabled();
            
            response.put("status", "SUCCESS");
            response.put("promptStats", promptStats);
            response.put("pollingEnabled", pollingEnabled);
            response.put("message", "轮询状态统计获取成功");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "获取轮询状态统计失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取Prompts记录数量
     * 统计Prompts表中未提交和已提交的记录数量
     * 
     * @return 包含未提交和已提交记录数量的响应
     */
    @GetMapping("/counts")
    public Map<String, Object> getPromptCounts() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 获取未提交和已提交的记录数量
            long pendingCount = promptService.countPendingPrompts();
            long submittedCount = promptService.countSubmittedPrompts();
            
            result.put("status", "UP");
            result.put("pendingCount", pendingCount);
            result.put("submittedCount", submittedCount);
            result.put("timestamp", System.currentTimeMillis());
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("errorMessage", e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
        }
        return result;
    }
}
