package com.example.medaiassistant.controller;

import com.example.medaiassistant.model.User;
import com.example.medaiassistant.repository.UserRepository;
import com.example.medaiassistant.repository.UserDepartmentRepository;
import com.example.medaiassistant.util.PasswordUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDepartmentRepository userDepartmentRepository;

    @InjectMocks
    private UserController userController;

    @Test
    public void testLoginWithPlainTextPassword() {
        // 准备测试数据 - 用户存储的是明文密码
        User user = new User();
        user.setId("testUser");
        user.setPasswordHash("123"); // 明文密码
        user.setIsActive(1);

        // 模拟repository行为
        when(userRepository.findById("testUser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // 创建登录请求
        UserController.LoginRequest loginRequest = new UserController.LoginRequest();
        loginRequest.setId("testUser");
        loginRequest.setPassword("123");

        // 执行测试
        boolean result = userController.login(loginRequest);

        // 验证结果
        assertTrue(result, "登录应该成功");

        // 验证密码被升级为哈希格式
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    public void testLoginWithHashedPassword() {
        // 准备测试数据 - 用户存储的是哈希密码
        String hashedPassword = PasswordUtil.hashPassword("123");
        User user = new User();
        user.setId("testUser");
        user.setPasswordHash(hashedPassword); // 哈希密码
        user.setIsActive(1);

        // 模拟repository行为
        when(userRepository.findById("testUser")).thenReturn(Optional.of(user));

        // 创建登录请求
        UserController.LoginRequest loginRequest = new UserController.LoginRequest();
        loginRequest.setId("testUser");
        loginRequest.setPassword("123");

        // 执行测试
        boolean result = userController.login(loginRequest);

        // 验证结果
        assertTrue(result, "登录应该成功");

        // 验证密码没有被重复保存（已经是哈希格式）
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testLoginWithWrongPassword() {
        // 准备测试数据
        User user = new User();
        user.setId("testUser");
        user.setPasswordHash("123"); // 明文密码
        user.setIsActive(1);

        // 模拟repository行为
        when(userRepository.findById("testUser")).thenReturn(Optional.of(user));

        // 创建登录请求 - 错误密码
        UserController.LoginRequest loginRequest = new UserController.LoginRequest();
        loginRequest.setId("testUser");
        loginRequest.setPassword("wrongPassword");

        // 执行测试
        boolean result = userController.login(loginRequest);

        // 验证结果
        assertFalse(result, "登录应该失败");

        // 验证密码没有被保存（验证失败）
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testLoginWithInactiveUser() {
        // 准备测试数据 - 未激活用户
        User user = new User();
        user.setId("testUser");
        user.setPasswordHash("123"); // 明文密码
        user.setIsActive(0); // 未激活

        // 模拟repository行为
        when(userRepository.findById("testUser")).thenReturn(Optional.of(user));

        // 创建登录请求
        UserController.LoginRequest loginRequest = new UserController.LoginRequest();
        loginRequest.setId("testUser");
        loginRequest.setPassword("123");

        // 执行测试
        boolean result = userController.login(loginRequest);

        // 验证结果
        assertFalse(result, "登录应该失败（用户未激活）");

        // 验证密码没有被保存（用户未激活）
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testLoginWithNonExistentUser() {
        // 模拟用户不存在
        when(userRepository.findById("nonExistentUser")).thenReturn(Optional.empty());

        // 创建登录请求
        UserController.LoginRequest loginRequest = new UserController.LoginRequest();
        loginRequest.setId("nonExistentUser");
        loginRequest.setPassword("123");

        // 执行测试
        boolean result = userController.login(loginRequest);

        // 验证结果
        assertFalse(result, "登录应该失败（用户不存在）");
    }
}
