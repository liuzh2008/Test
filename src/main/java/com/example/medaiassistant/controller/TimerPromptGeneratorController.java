package com.example.medaiassistant.controller;

import com.example.medaiassistant.service.TimerPromptGenerator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TimerPromptGenerator控制器类
 * 提供启动、停止和查询定时器状态的接口
 */
@RestController
@RequestMapping("/api/timer-prompt-generator")
public class TimerPromptGeneratorController {

    private final TimerPromptGenerator timerPromptGenerator;

    /**
     * 构造函数注入TimerPromptGenerator服务
     * 
     * @param timerPromptGenerator 定时器服务实例
     */
    public TimerPromptGeneratorController(TimerPromptGenerator timerPromptGenerator) {
        this.timerPromptGenerator = timerPromptGenerator;
    }

    /**
     * 启动定时器接口
     * 
     * @return 启动结果信息
     */
    @GetMapping("/start")
    public String startTimer() {
        timerPromptGenerator.startTimer();
        return "定时器启动请求已发送";
    }

    /**
     * 停止定时器接口
     * 
     * @return 停止结果信息
     */
    @GetMapping("/stop")
    public String stopTimer() {
        timerPromptGenerator.stopTimer();
        return "定时器停止请求已发送";
    }

    /**
     * 查询定时器状态接口
     * 
     * @return 定时器当前状态信息
     */
    @GetMapping("/status")
    public String timerStatus() {
        if (timerPromptGenerator.isTimerRunning()) {
            return "定时器正在运行";
        } else {
            return "定时器未运行";
        }
    }

    /**
     * 手动触发每日Prompt生成（可临时修改配置）
     * 
     * @param time           临时执行时间(cron表达式)
     * @param maxConcurrency 临时最大并发数
     * @return 执行结果信息
     */
    @GetMapping("/trigger-daily")
    public String triggerDailyPromptGeneration(
            @RequestParam(required = false) String time,
            @RequestParam(required = false) Integer maxConcurrency) {

        // 临时修改配置
        String originalTime = timerPromptGenerator.getDailyTaskTime();
        int originalConcurrency = timerPromptGenerator.getMaxConcurrency();

        try {
            if (time != null) {
                timerPromptGenerator.setDailyTaskTime(time);
            }
            if (maxConcurrency != null) {
                timerPromptGenerator.setMaxConcurrency(maxConcurrency);
            }

            // 执行任务
            timerPromptGenerator.dailyPromptGeneration();
            return "每日Prompt生成任务已手动触发" +
                    (time != null ? " (临时时间:" + time + ")" : "") +
                    (maxConcurrency != null ? " (临时并发数:" + maxConcurrency + ")" : "");
        } finally {
            // 恢复原始配置
            timerPromptGenerator.setDailyTaskTime(originalTime);
            timerPromptGenerator.setMaxConcurrency(originalConcurrency);
        }
    }
}
