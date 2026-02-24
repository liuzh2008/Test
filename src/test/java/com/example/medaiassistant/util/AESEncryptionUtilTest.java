package com.example.medaiassistant.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AESEncryptionUtil 测试类
 * 测试加密、解密功能及性能
 */
class AESEncryptionUtilTest {

    // 测试用的密钥和盐值
    private static final String TEST_SECRET_KEY = "TestAES256EncryptionKeyForTesting!";
    private static final String TEST_SALT = "TestSaltValueForAESTesting";

    @Test
    void testEncryptionDecryption() throws Exception {
        // 测试数据
        String originalText = "这是一段需要加密的敏感医疗数据，包含患者信息和诊断结果。";

        // 加密
        String encrypted = AESEncryptionUtil.encrypt(originalText, TEST_SECRET_KEY, TEST_SALT);
        
        // 验证加密结果不为空且与原文不同
        assertNotNull(encrypted);
        assertNotEquals(originalText, encrypted);
        
        // 解密
        String decrypted = AESEncryptionUtil.decrypt(encrypted, TEST_SECRET_KEY, TEST_SALT);
        
        // 验证解密结果与原文一致
        assertEquals(originalText, decrypted);
    }

    @Test
    void testEncryptionDecryptionWithDifferentData() throws Exception {
        // 测试各种类型的数据
        String[] testData = {
            "短文本",
            "这是一段中等长度的文本，用于测试加密功能",
            "这是一段很长的文本，包含各种特殊字符：!@#$%^&*()_+-=[]{};':\",./<>?\\|`~，以及中文和英文混合内容。Long text with mixed Chinese and English content and special characters.",
            "", // 空字符串
            "1234567890", // 纯数字
            "!@#$%^&*()", // 特殊字符
            "这是一个包含换行符的文本\n第二行内容\n第三行内容" // 多行文本
        };

        for (String data : testData) {
            if (data.isEmpty()) {
                // 空字符串应该抛出异常
                assertThrows(IllegalArgumentException.class, () -> {
                    AESEncryptionUtil.encrypt(data, TEST_SECRET_KEY, TEST_SALT);
                });
                continue;
            }

            String encrypted = AESEncryptionUtil.encrypt(data, TEST_SECRET_KEY, TEST_SALT);
            String decrypted = AESEncryptionUtil.decrypt(encrypted, TEST_SECRET_KEY, TEST_SALT);
            
            assertEquals(data, decrypted, "加密解密后数据不一致: " + data);
        }
    }

    @Test
    void testPerformance() throws Exception {
        // 性能测试 - 加密解密100次并计算平均时间
        String testText = "这是一段典型的医疗Prompt数据，包含患者基本信息、诊断结果、治疗方案等敏感信息。";
        
        int iterations = 100;
        long totalEncryptTime = 0;
        long totalDecryptTime = 0;
        
        String encrypted = null;
        
        for (int i = 0; i < iterations; i++) {
            // 加密性能测试
            long startTime = System.nanoTime();
            encrypted = AESEncryptionUtil.encrypt(testText, TEST_SECRET_KEY, TEST_SALT);
            long encryptTime = System.nanoTime() - startTime;
            totalEncryptTime += encryptTime;
            
            // 解密性能测试
            startTime = System.nanoTime();
            String decrypted = AESEncryptionUtil.decrypt(encrypted, TEST_SECRET_KEY, TEST_SALT);
            long decryptTime = System.nanoTime() - startTime;
            totalDecryptTime += decryptTime;
            
            assertEquals(testText, decrypted);
        }
        
        double avgEncryptTimeMs = (totalEncryptTime / iterations) / 1_000_000.0;
        double avgDecryptTimeMs = (totalDecryptTime / iterations) / 1_000_000.0;
        
        System.out.println("加密性能测试结果 (" + iterations + "次迭代):");
        System.out.println("平均加密时间: " + String.format("%.3f", avgEncryptTimeMs) + " ms");
        System.out.println("平均解密时间: " + String.format("%.3f", avgDecryptTimeMs) + " ms");
        System.out.println("总加密时间: " + String.format("%.3f", totalEncryptTime / 1_000_000.0) + " ms");
        System.out.println("总解密时间: " + String.format("%.3f", totalDecryptTime / 1_000_000.0) + " ms");
        
        // 性能要求：单次加密解密应该在100ms以内
        assertTrue(avgEncryptTimeMs < 100, "加密性能不达标: " + avgEncryptTimeMs + " ms");
        assertTrue(avgDecryptTimeMs < 100, "解密性能不达标: " + avgDecryptTimeMs + " ms");
    }

    @Test
    void testDifferentKeySaltCombinations() throws Exception {
        String originalText = "测试不同的密钥和盐值组合";
        
        // 测试不同的密钥和盐值
        String[] keys = {
            "Key1-ThisIsATestEncryptionKeyForAES256!",
            "Key2-AnotherTestKeyWithDifferentLength123",
            "Key3-VeryLongEncryptionKeyForAES256AlgorithmTestingPurpose!"
        };
        
        String[] salts = {
            "Salt1-TestSaltValue",
            "Salt2-DifferentSaltForTesting",
            "Salt3-AnotherSaltValueWithMoreCharacters"
        };
        
        for (String key : keys) {
            for (String salt : salts) {
                String encrypted = AESEncryptionUtil.encrypt(originalText, key, salt);
                String decrypted = AESEncryptionUtil.decrypt(encrypted, key, salt);
                
                assertEquals(originalText, decrypted, 
                    "密钥: " + key + ", 盐值: " + salt + " 组合测试失败");
            }
        }
    }

    @Test
    void testErrorConditions() {
        String validText = "有效文本";
        String validKey = "ValidKey1234567890!";
        String validSalt = "ValidSaltValue";
        
        // 测试空文本
        assertThrows(IllegalArgumentException.class, () -> {
            AESEncryptionUtil.encrypt("", validKey, validSalt);
        });
        
        // 测试空密钥
        assertThrows(IllegalArgumentException.class, () -> {
            AESEncryptionUtil.encrypt(validText, "", validSalt);
        });
        
        // 测试空盐值
        assertThrows(IllegalArgumentException.class, () -> {
            AESEncryptionUtil.encrypt(validText, validKey, "");
        });
        
        // 测试null文本
        assertThrows(IllegalArgumentException.class, () -> {
            AESEncryptionUtil.encrypt(null, validKey, validSalt);
        });
        
        // 测试无效的加密数据
        assertThrows(Exception.class, () -> {
            AESEncryptionUtil.decrypt("无效的加密数据", validKey, validSalt);
        });
    }

    @Test
    void testRandomKeyGeneration() {
        // 测试随机密钥生成
        String key1 = AESEncryptionUtil.generateRandomKey();
        String key2 = AESEncryptionUtil.generateRandomKey();
        
        assertNotNull(key1);
        assertNotNull(key2);
        assertNotEquals(key1, key2, "生成的随机密钥应该不同");
        
        // 验证密钥长度（Base64编码的32字节数据应该是44字符）
        assertEquals(44, key1.length());
        
        // 测试随机盐值生成
        String salt1 = AESEncryptionUtil.generateRandomSalt();
        String salt2 = AESEncryptionUtil.generateRandomSalt();
        
        assertNotNull(salt1);
        assertNotNull(salt2);
        assertNotEquals(salt1, salt2, "生成的随机盐值应该不同");
        
        // 验证盐值长度（Base64编码的16字节数据应该是24字符）
        assertEquals(24, salt1.length());
    }

    @Test
    void testTestEncryptionMethod() {
        // 测试testEncryption方法
        boolean result = AESEncryptionUtil.testEncryption("测试文本", TEST_SECRET_KEY, TEST_SALT);
        assertTrue(result, "testEncryption方法应该返回true");
        
        // 测试使用空密钥（真正无效的情况）
        boolean invalidResult = AESEncryptionUtil.testEncryption("测试文本", "", TEST_SALT);
        assertFalse(invalidResult, "使用空密钥应该返回false");
        
        // 测试使用null密钥
        boolean nullResult = AESEncryptionUtil.testEncryption("测试文本", null, TEST_SALT);
        assertFalse(nullResult, "使用null密钥应该返回false");
        
        // 测试使用空盐值
        boolean emptySaltResult = AESEncryptionUtil.testEncryption("测试文本", TEST_SECRET_KEY, "");
        assertFalse(emptySaltResult, "使用空盐值应该返回false");
    }

    @Test
    void testLargeDataEncryption() throws Exception {
        // 测试大数据量加密（模拟医疗Prompt数据）
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeText.append("患者ID: PT2024001, 诊断: 高血压, 治疗方案: 口服降压药。");
            largeText.append("检查结果: 血压140/90mmHg, 心率75次/分。");
            largeText.append("注意事项: 定期复查, 低盐饮食, 适当运动。\n");
        }
        
        String largeData = largeText.toString();
        System.out.println("测试数据大小: " + largeData.length() + " 字符");
        
        long startTime = System.nanoTime();
        String encrypted = AESEncryptionUtil.encrypt(largeData, TEST_SECRET_KEY, TEST_SALT);
        long encryptTime = System.nanoTime() - startTime;
        
        startTime = System.nanoTime();
        String decrypted = AESEncryptionUtil.decrypt(encrypted, TEST_SECRET_KEY, TEST_SALT);
        long decryptTime = System.nanoTime() - startTime;
        
        assertEquals(largeData, decrypted, "大数据量加密解密失败");
        
        System.out.println("大数据量性能测试:");
        System.out.println("加密时间: " + String.format("%.3f", encryptTime / 1_000_000.0) + " ms");
        System.out.println("解密时间: " + String.format("%.3f", decryptTime / 1_000_000.0) + " ms");
        System.out.println("加密后数据大小: " + encrypted.length() + " 字符");
        System.out.println("数据膨胀率: " + String.format("%.2f", (encrypted.length() * 100.0 / largeData.length())) + "%");
        
        // 验证加密后的数据大小（Base64编码会有约33%的膨胀）
        assertTrue(encrypted.length() > largeData.length(), "加密后数据应该比原文大");
    }
}
