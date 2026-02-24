package com.example.medaiassistant.controller;

import com.example.medaiassistant.service.NightlySyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * NightlySyncController API测试
 * 
 * <p>任务5：NightlySyncController控制器TDD测试</p>
 * 
 * <p><strong>测试策略</strong>：使用Mockito进行纯单元测试，
 * 不加载Spring上下文，直接调用控制器方法进行测试，
 * 遵循"最小化加载原则"以减少测试启动时间</p>
 * 
 * <p><strong>测试覆盖</strong>：</p>
 * <table border="1">
 *   <tr><th>测试组</th><th>数量</th><th>覆盖内容</th></tr>
 *   <tr><td>状态查询测试</td><td>2</td><td>GET /api/nightly-sync/status 空闲/执行中状态</td></tr>
 *   <tr><td>手动触发测试</td><td>2</td><td>POST /api/nightly-sync/trigger 成功/拒绝</td></tr>
 *   <tr><td>健康检查测试</td><td>1</td><td>GET /api/nightly-sync/health UP状态</td></tr>
 *   <tr><td>性能测试</td><td>3</td><td>响应时间&lt;50ms，批量调用&lt;500ms</td></tr>
 *   <tr><td><strong>总计</strong></td><td><strong>8</strong></td><td></td></tr>
 * </table>
 * 
 * <p><strong>TDD阶段</strong>：</p>
 * <ul>
 *   <li>✅ 红阶段：编写失败测试用例</li>
 *   <li>✅ 绿阶段：实现三个HTTP端点</li>
 *   <li>✅ 性能测试：响应时间验证</li>
 *   <li>🔵 重构阶段：统一响应格式、添加异常处理</li>
 * </ul>
 * 
 * <p><strong>测试结果</strong>：8/8 通过</p>
 * <p><strong>执行时间</strong>：约0.9秒</p>
 * 
 * @author System
 * @version 1.1
 * @since 2026-01-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NightlySyncController API测试")
class NightlySyncControllerTest {

    @Mock
    private NightlySyncService nightlySyncService;

    @InjectMocks
    private NightlySyncController controller;

    // ==================== 状态查询测试 ====================
    
    @Nested
    @DisplayName("GET /api/nightly-sync/status - 状态查询测试")
    class StatusTests {
        
        @Test
        @DisplayName("空闲状态 - 应返回isRunning=false")
        void getStatus_WhenIdle_ReturnsNotRunning() {
            // Given: 夜间同步任务处于空闲状态
            when(nightlySyncService.isRunning()).thenReturn(false);

            // When: 调用状态查询接口
            ResponseEntity<Map<String, Object>> response = controller.getStatus();

            // Then: 验证响应
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertEquals(false, body.get("isRunning"));
            assertEquals("夜间同步任务空闲", body.get("message"));
            assertNotNull(body.get("timestamp"));
        }

        @Test
        @DisplayName("执行中状态 - 应返回isRunning=true")
        void getStatus_WhenRunning_ReturnsRunning() {
            // Given: 夜间同步任务正在执行中
            when(nightlySyncService.isRunning()).thenReturn(true);

            // When: 调用状态查询接口
            ResponseEntity<Map<String, Object>> response = controller.getStatus();

            // Then: 验证响应
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertEquals(true, body.get("isRunning"));
            assertEquals("夜间同步任务正在执行中", body.get("message"));
            assertNotNull(body.get("timestamp"));
        }
    }

    // ==================== 手动触发测试 ====================
    
    @Nested
    @DisplayName("POST /api/nightly-sync/trigger - 手动触发测试")
    class TriggerTests {
        
        @Test
        @DisplayName("成功触发 - 应返回success=true")
        void triggerSync_WhenIdle_ReturnsSuccess() {
            // Given: 任务空闲，可以触发
            when(nightlySyncService.triggerManualSync()).thenReturn(true);

            // When: 调用手动触发接口
            ResponseEntity<Map<String, Object>> response = controller.triggerSync();

            // Then: 验证响应
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertEquals(true, body.get("success"));
            assertEquals("夜间同步任务已触发，请查看日志了解执行进度", body.get("message"));
            assertNotNull(body.get("timestamp"));
        }

        @Test
        @DisplayName("执行中拒绝 - 应返回success=false")
        void triggerSync_WhenRunning_ReturnsFailure() {
            // Given: 任务正在执行中，无法重复触发
            when(nightlySyncService.triggerManualSync()).thenReturn(false);

            // When: 调用手动触发接口
            ResponseEntity<Map<String, Object>> response = controller.triggerSync();

            // Then: 验证响应
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertEquals(false, body.get("success"));
            assertEquals("夜间同步任务正在执行中，无法重复触发", body.get("message"));
            assertNotNull(body.get("timestamp"));
        }
    }

    // ==================== 健康检查测试 ====================
    
    @Nested
    @DisplayName("GET /api/nightly-sync/health - 健康检查测试")
    class HealthTests {
        
        @Test
        @DisplayName("健康检查 - 应返回UP状态")
        void health_ReturnsUp() {
            // When: 调用健康检查接口
            ResponseEntity<Map<String, Object>> response = controller.health();

            // Then: 验证响应
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertEquals("UP", body.get("status"));
            assertEquals("NightlySyncService", body.get("service"));
            assertNotNull(body.get("timestamp"));
        }
    }

    // ==================== 性能测试 ====================
    
    @Nested
    @DisplayName("性能测试")
    class PerformanceTests {
        
        @Test
        @DisplayName("状态查询接口响应时间应小于50ms")
        void getStatus_ShouldRespondWithin50ms() {
            // Given
            when(nightlySyncService.isRunning()).thenReturn(false);
            
            // When: 测量响应时间
            long startTime = System.nanoTime();
            controller.getStatus();
            long endTime = System.nanoTime();
            
            // Then: 验证响应时间
            long durationMs = (endTime - startTime) / 1_000_000;
            assertTrue(durationMs < 50, 
                "状态查询接口响应时间应小于50ms，实际: " + durationMs + "ms");
        }
        
        @Test
        @DisplayName("健康检查接口响应时间应小于50ms")
        void health_ShouldRespondWithin50ms() {
            // When: 测量响应时间
            long startTime = System.nanoTime();
            controller.health();
            long endTime = System.nanoTime();
            
            // Then: 验证响应时间
            long durationMs = (endTime - startTime) / 1_000_000;
            assertTrue(durationMs < 50, 
                "健康检查接口响应时间应小于50ms，实际: " + durationMs + "ms");
        }
        
        @Test
        @DisplayName("批量调用性能 - 100次调用应在500ms内完成")
        void batchCalls_ShouldCompleteWithin500ms() {
            // Given
            when(nightlySyncService.isRunning()).thenReturn(false);
            
            // When: 批量调用100次
            long startTime = System.nanoTime();
            for (int i = 0; i < 100; i++) {
                controller.getStatus();
                controller.health();
            }
            long endTime = System.nanoTime();
            
            // Then: 验证总时间
            long durationMs = (endTime - startTime) / 1_000_000;
            assertTrue(durationMs < 500, 
                "100次批量调用应在500ms内完成，实际: " + durationMs + "ms");
        }
    }
}
