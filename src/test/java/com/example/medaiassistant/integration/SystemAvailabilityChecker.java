package com.example.medaiassistant.integration;

import org.junit.jupiter.api.Assumptions;

import java.awt.GraphicsEnvironment;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.swing.JOptionPane;

/**
 * 系统可用性检查工具类
 * 提供统一的系统启动检测和用户交互功能
 * 供外部集成测试使用
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-04
 */
public class SystemAvailabilityChecker {

    private static final int DEFAULT_WAIT_SECONDS = 120;
    private static final int CHECK_INTERVAL_SECONDS = 10;

    /**
     * 检查系统可用性，如果系统未运行则显示警告并等待
     * 
     * @param baseUrl 系统基础URL
     * @return 如果系统可用返回true，否则跳过测试
     */
    public static boolean ensureSystemRunning(String baseUrl) {
        String healthCheckUrl = buildHealthCheckUrl(baseUrl);
        
        System.out.println("检查系统可用性: " + healthCheckUrl);
        
        if (isSystemRunning(healthCheckUrl)) {
            System.out.println("✅ 系统运行正常，开始执行测试");
            return true;
        }
        
        // 系统未运行，显示警告并等待用户选择
        boolean userConfirmed = showSystemNotRunningWarning();
        
        if (userConfirmed) {
            // 用户点击"已经启动系统"，立即检查系统状态
            if (isSystemRunning(healthCheckUrl)) {
                System.out.println("✅ 系统已启动，开始执行测试");
                return true;
            } else {
                // 如果系统仍未启动，等待指定时间
                waitForSystemStartup(healthCheckUrl, DEFAULT_WAIT_SECONDS);
                if (isSystemRunning(healthCheckUrl)) {
                    System.out.println("✅ 系统已启动，开始执行测试");
                    return true;
                } else {
                    System.out.println("❌ 系统未启动，跳过所有测试");
                    Assumptions.assumeTrue(false, "后端服务未在指定端口运行，请先启动服务再运行测试");
                    return false;
                }
            }
        } else {
            // 用户点击"取消"，直接跳过测试
            System.out.println("❌ 用户取消测试执行");
            Assumptions.assumeTrue(false, "用户取消测试执行");
            return false;
        }
    }

    /**
     * 检查系统是否正在运行
     * 
     * @param baseUrl 系统基础URL
     * @return 系统运行状态
     */
    public static boolean checkSystemStatus(String baseUrl) {
        String healthCheckUrl = buildHealthCheckUrl(baseUrl);
        return isSystemRunning(healthCheckUrl);
    }

    /**
     * 等待系统启动
     * 
     * @param baseUrl 系统基础URL
     * @param waitSeconds 等待秒数
     * @return 等待期间系统是否启动
     */
    public static boolean waitForSystem(String baseUrl, int waitSeconds) {
        String healthCheckUrl = buildHealthCheckUrl(baseUrl);
        return waitForSystemStartup(healthCheckUrl, waitSeconds);
    }

    /**
     * 构建健康检查URL
     * 使用 /api/hospital-config/health 而不是 /actuator/health
     */
    private static String buildHealthCheckUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? 
            baseUrl + "api/hospital-config/health" : 
            baseUrl + "/api/hospital-config/health";
    }

    /**
     * 检查系统是否运行
     */
    private static boolean isSystemRunning(String url) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 显示系统未运行警告
     */
    private static boolean showSystemNotRunningWarning() {
        if (isHeadlessEnvironment()) {
            System.out.println("\n⚠️  ===========================================");
            System.out.println("⚠️  后端服务未在指定端口运行！");
            System.out.println("⚠️  请启动系统，以便于测试继续进行。");
            System.out.println("⚠️  请执行以下操作：");
            System.out.println("⚠️  1. 启动后端服务");
            System.out.println("⚠️  2. 等待服务完全启动");
            System.out.println("⚠️  3. 重新运行测试");
            System.out.println("⚠️  ");
            System.out.println("⚠️  测试将在2分钟后自动跳过...");
            System.out.println("⚠️  ===========================================\n");
            // 在无头环境中，默认等待系统启动
            return true;
        } else {
            try {
                int result = JOptionPane.showOptionDialog(
                    null,
                    "后端服务未在指定端口运行！\n\n" +
                    "请启动系统，以便于测试继续进行。\n\n" +
                    "请执行以下操作：\n" +
                    "1. 启动后端服务\n" +
                    "2. 等待服务完全启动\n" +
                    "3. 点击'已经启动系统'继续测试\n\n" +
                    "如果选择'取消'，将跳过所有测试。",
                    "系统未启动警告",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    new Object[]{"已经启动系统", "取消"},
                    "已经启动系统"
                );
                return result == JOptionPane.YES_OPTION;
            } catch (Exception e) {
                // 如果图形界面失败，回退到控制台输出
                System.out.println("\n⚠️  图形界面失败，使用控制台模式");
                System.out.println("⚠️  后端服务未在指定端口运行！");
                System.out.println("⚠️  请启动系统，以便于测试继续进行。");
                System.out.println("⚠️  测试将在2分钟后自动跳过...\n");
                return true;
            }
        }
    }

    /**
     * 等待系统启动
     */
    private static boolean waitForSystemStartup(String url, int seconds) {
        System.out.println("⏳ 等待系统启动，最多等待 " + seconds + " 秒...");
        int totalChecks = seconds / CHECK_INTERVAL_SECONDS;
        
        for (int i = 0; i < totalChecks; i++) {
            try {
                Thread.sleep(CHECK_INTERVAL_SECONDS * 1000L);
                
                if (isSystemRunning(url)) {
                    System.out.println("✅ 系统已启动，继续执行测试");
                    return true;
                }
                
                int elapsedSeconds = (i + 1) * CHECK_INTERVAL_SECONDS;
                int remainingSeconds = seconds - elapsedSeconds;
                System.out.println("⏳ 等待中... (" + elapsedSeconds + "秒已过，" + remainingSeconds + "秒剩余)");
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("❌ 等待被中断");
                break;
            }
        }
        return false;
    }

    /**
     * 检查是否为无头环境
     */
    private static boolean isHeadlessEnvironment() {
        return GraphicsEnvironment.isHeadless() || 
               System.getenv("CI") != null ||
               System.getProperty("java.awt.headless") != null;
    }

    /**
     * 解析基础URL（支持环境变量和系统属性）
     */
    public static String resolveBaseUrl() {
        String env = System.getenv("HEALTH_BASE_URL");
        if (env == null || env.isBlank()) {
            env = System.getProperty("health.baseUrl");
        }
        if (env == null || env.isBlank()) {
            env = "http://localhost:8081";
        }
        return env;
    }

    /**
     * 构建Actuator端点URL
     * @param baseUrl 根地址（如 http://localhost:8081）
     * @param path 端点路径（如 actuator/health）
     * @return 完整URL
     */
    public static String buildActuatorUrl(String baseUrl, String path) {
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        return baseUrl.endsWith("/") ? baseUrl + cleanPath : baseUrl + "/" + cleanPath;
    }

    /**
     * 构建业务API端点URL
     * @param baseUrl 根地址（如 http://localhost:8081）
     * @param path 端点路径（如 configuration/validate）
     * @return 完整URL（如 http://localhost:8081/api/configuration/validate）
     */
    public static String buildApiUrl(String baseUrl, String path) {
        String cleanPath = path.startsWith("api/") ? path : "api/" + path;
        cleanPath = cleanPath.startsWith("/") ? cleanPath.substring(1) : cleanPath;
        return baseUrl.endsWith("/") ? baseUrl + cleanPath : baseUrl + "/" + cleanPath;
    }
}
