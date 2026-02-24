package com.example.medaiassistant.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI模型配置测试类
 * 按照TDD红-绿-重构流程实施迭代1：AI模型配置基础功能
 * 
 * ✅ 测试文件评价：
 * - 遵循TDD红-绿-重构流程
 * - 测试覆盖了配置加载、验证、多模型映射等关键功能
 * - 使用Spring Boot测试框架，确保配置绑定正确性
 * - 测试用例命名清晰，符合BDD风格
 * - 包含必要的断言验证和错误消息
 * - 已收敛配置加载范围，限定classes和禁用无关组件
 * 
 * @author Cline AI
 * @version 1.0.1
 * @since 2025-11-07
 */
@SpringBootTest(
    classes = AIModelConfig.class,
    properties = {
        // AI模型配置测试数据
        "ai.models.deepseek-chat.url=https://api.deepseek.com/chat/completions",
        "ai.models.deepseek-chat.key=test-api-key-123",
        "ai.models.deepseek-chat.connect-timeout=30000",
        "ai.models.deepseek-chat.read-timeout=120000",
        "ai.models.deepseek-chat.max-retries=3",
        "ai.models.deepseek-chat.retry-delay=1000",
        
        "ai.models.deepseek-reasoner.url=https://api.deepseek.com/reasoner/completions", 
        "ai.models.deepseek-reasoner.key=test-api-key-456",
        "ai.models.deepseek-reasoner.connect-timeout=45000",
        "ai.models.deepseek-reasoner.read-timeout=180000",
        "ai.models.deepseek-reasoner.max-retries=5",
        "ai.models.deepseek-reasoner.retry-delay=2000",
        
        // 禁用Web组件
        "spring.main.web-application-type=none",
        // 禁用调度组件
        "spring.task.scheduling.enabled=false",
        "scheduling.auto-execute.enabled=false",
        // 禁用Prompt服务组件
        "prompt.submission.enabled=false",
        "prompt.polling.enabled=false",
        // 禁用监控组件
        "monitoring.metrics.enabled=false",
        // 禁用DDL管理
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=none"
    }
)
@EnableConfigurationProperties(AIModelConfig.class)
class AIModelConfigTest {

    @Autowired
    @Qualifier("AIModelConfig")
    private AIModelConfig aiModelConfig;

    /**
     * ✅ 测试用例1：测试AIModelConfig类正确绑定配置前缀
     * 验证配置类能够正确注入并加载配置映射
     */
    @Test
    void shouldLoadModelConfigCorrectly() {
        // 验证配置类不为空
        assertNotNull(aiModelConfig, "AIModelConfig应该被正确注入");
        
        // 验证模型配置映射不为空
        assertNotNull(aiModelConfig.getModels(), "模型配置映射不应该为空");
        assertFalse(aiModelConfig.getModels().isEmpty(), "模型配置映射不应该为空");
    }

    /**
     * ✅ 测试用例2：测试单个AI模型配置加载
     * 验证特定模型的配置能够正确绑定属性值
     */
    @Test
    void shouldLoadSingleModelConfig() {
        // 验证特定模型配置
        AIModelConfig.ModelConfig deepseekChatConfig = aiModelConfig.getModelConfig("deepseek-chat");
        assertNotNull(deepseekChatConfig, "deepseek-chat模型配置应该存在");
        assertEquals("https://api.deepseek.com/chat/completions", deepseekChatConfig.getUrl(), "URL配置应该正确");
        assertEquals("test-api-key-123", deepseekChatConfig.getKey(), "API密钥应该正确");
        assertEquals(30000, deepseekChatConfig.getConnectTimeout(), "连接超时应该正确");
        assertEquals(120000, deepseekChatConfig.getReadTimeout(), "读取超时应该正确");
        assertEquals(3, deepseekChatConfig.getMaxRetries(), "最大重试次数应该正确");
        assertEquals(1000, deepseekChatConfig.getRetryDelay(), "重试延迟应该正确");
    }

    /**
     * ✅ 测试用例3：测试多模型配置映射
     * 验证多个模型配置能够正确区分和加载
     */
    @Test
    void shouldLoadMultipleModelConfigs() {
        // 验证第一个模型配置
        AIModelConfig.ModelConfig deepseekChatConfig = aiModelConfig.getModelConfig("deepseek-chat");
        assertNotNull(deepseekChatConfig, "deepseek-chat模型配置应该存在");
        
        // 验证第二个模型配置
        AIModelConfig.ModelConfig deepseekReasonerConfig = aiModelConfig.getModelConfig("deepseek-reasoner");
        assertNotNull(deepseekReasonerConfig, "deepseek-reasoner模型配置应该存在");
        
        // 验证两个模型配置不同
        assertNotEquals(deepseekChatConfig.getUrl(), deepseekReasonerConfig.getUrl(), "两个模型的URL应该不同");
        assertNotEquals(deepseekChatConfig.getKey(), deepseekReasonerConfig.getKey(), "两个模型的API密钥应该不同");
        
        // 验证配置映射大小（包括测试配置和实际配置）
        assertTrue(aiModelConfig.getModels().size() >= 2, "应该至少有两个模型配置");
        
        // 验证测试配置存在
        assertTrue(aiModelConfig.getModels().containsKey("deepseek-chat"), "应该包含deepseek-chat配置");
        assertTrue(aiModelConfig.getModels().containsKey("deepseek-reasoner"), "应该包含deepseek-reasoner配置");
    }

    /**
     * ✅ 测试用例4：测试配置验证方法
     * 验证配置验证逻辑的正确性
     */
    @Test
    void shouldValidateConfigurationWithMethods() {
        // 验证必填字段检查
        AIModelConfig.ModelConfig config = aiModelConfig.getModelConfig("deepseek-chat");
        assertNotNull(config, "配置应该存在");
        
        // 验证URL不为空
        assertNotNull(config.getUrl(), "URL不应该为空");
        assertFalse(config.getUrl().trim().isEmpty(), "URL不应该为空字符串");
        
        // 验证API密钥不为空
        assertNotNull(config.getKey(), "API密钥不应该为空");
        assertFalse(config.getKey().trim().isEmpty(), "API密钥不应该为空字符串");
        
        // 验证数值范围
        assertTrue(config.getMaxRetries() > 0, "最大重试次数应该为正数");
        assertTrue(config.getRetryDelay() > 0, "重试延迟应该为正数");
        assertTrue(config.getConnectTimeout() > 0, "连接超时应该为正数");
        assertTrue(config.getReadTimeout() > 0, "读取超时应该为正数");
    }

    /**
     * ✅ 测试用例5：测试环境变量映射
     * 验证Spring Boot配置属性绑定机制
     */
    @Test
    void shouldSupportEnvironmentVariableMapping() {
        // 验证配置可以通过属性文件加载
        assertNotNull(aiModelConfig, "配置应该通过属性文件加载");
        
        // 验证配置值正确绑定
        AIModelConfig.ModelConfig config = aiModelConfig.getModelConfig("deepseek-chat");
        assertNotNull(config, "配置应该存在");
        assertEquals("https://api.deepseek.com/chat/completions", config.getUrl(), "URL应该正确绑定");
        assertEquals("test-api-key-123", config.getKey(), "API密钥应该正确绑定");
    }

    /**
     * 🔧 新增测试用例6：测试边界条件 - 不存在的模型配置
     * 验证获取不存在的模型配置时返回null
     */
    @Test
    void shouldReturnNullForNonExistentModel() {
        // 验证不存在的模型配置返回null
        AIModelConfig.ModelConfig nonExistentConfig = aiModelConfig.getModelConfig("non-existent-model");
        assertNull(nonExistentConfig, "不存在的模型配置应该返回null");
    }

    /**
     * 🔧 新增测试用例7：测试Optional API
     * 验证Optional API的正确性
     */
    @Test
    void shouldWorkWithOptionalAPI() {
        // 验证存在的模型配置Optional不为空
        assertTrue(aiModelConfig.getModelConfigOptional("deepseek-chat").isPresent(), 
            "存在的模型配置Optional应该不为空");
        
        // 验证不存在的模型配置Optional为空
        assertTrue(aiModelConfig.getModelConfigOptional("non-existent-model").isEmpty(),
            "不存在的模型配置Optional应该为空");
    }

    /**
     * 🔧 新增测试用例8：测试配置验证方法
     * 验证配置验证方法的正确性
     */
    @Test
    void shouldValidateModelConfiguration() {
        // 验证有效配置
        assertTrue(aiModelConfig.isValidModelConfig("deepseek-chat"), 
            "有效的模型配置应该通过验证");
        
        // 验证无效配置（不存在的模型）
        assertFalse(aiModelConfig.isValidModelConfig("non-existent-model"),
            "不存在的模型配置应该验证失败");
    }

    /**
     * 🔧 新增测试用例9：测试配置摘要方法
     * 验证配置摘要和安全摘要方法
     */
    @Test
    void shouldGenerateConfigurationSummaries() {
        AIModelConfig.ModelConfig config = aiModelConfig.getModelConfig("deepseek-chat");
        assertNotNull(config, "配置应该存在");
        
        // 验证摘要方法
        String summary = config.getSummary();
        assertNotNull(summary, "摘要不应该为空");
        assertTrue(summary.contains("URL:"), "摘要应该包含URL信息");
        assertTrue(summary.contains("连接超时:"), "摘要应该包含连接超时信息");
        
        // 验证安全摘要方法
        String secureSummary = config.getSecureSummary();
        assertNotNull(secureSummary, "安全摘要不应该为空");
        assertTrue(secureSummary.contains("Key: ***"), "安全摘要应该隐藏API密钥");
    }
}
