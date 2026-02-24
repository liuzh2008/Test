package com.example.medaiassistant.controller;

import com.example.medaiassistant.dto.DepartmentDTO;
import com.example.medaiassistant.model.User;
import com.example.medaiassistant.repository.UserRepository;
import com.example.medaiassistant.repository.UserDepartmentRepository;
import com.example.medaiassistant.util.PasswordUtil;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 用户控制器 - 处理用户相关的HTTP请求
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserDepartmentRepository userDepartmentRepository;

    /**
     * 构造函数 - 依赖注入UserRepository和UserDepartmentRepository
     * 
     * @param userRepository           用户数据访问接口
     * @param userDepartmentRepository 用户科室关系数据访问接口
     */
    public UserController(UserRepository userRepository, UserDepartmentRepository userDepartmentRepository) {
        this.userRepository = userRepository;
        this.userDepartmentRepository = userDepartmentRepository;
    }

    /**
     * 根据ID获取用户信息
     * 
     * @param id 用户ID (路径参数)
     * @return 用户对象
     * @throws RuntimeException 当用户不存在时抛出异常
     */
    @GetMapping("/{id}")
    public User getUserById(@PathVariable String id) {
        // 打印查询日志
        System.out.println("查询用户ID: " + id);

        // 从数据库查询用户
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // 打印返回数据日志
        System.out.println("返回用户数据: " + user);

        return user;
    }

    /**
     * 获取用户所属科室列表
     * 
     * @param id 用户ID (路径参数)
     * @return 用户科室列表
     */
    @GetMapping("/{id}/departments")
    public List<DepartmentDTO> getUserDepartments(@PathVariable String id) {
        System.out.println("查询用户科室列表，用户ID: " + id);

        List<DepartmentDTO> departments = userDepartmentRepository.findDepartmentsByUserId(id);

        System.out.println("返回科室数量: " + departments.size());
        return departments;
    }

    /**
     * 用户登录验证
     * 
     * @param loginRequest 包含用户名和密码的请求体
     * @return 验证结果
     */
    @PostMapping("/login")
    public boolean login(@RequestBody LoginRequest loginRequest) {
        // 根据ID查询用户
        User user = userRepository.findById(loginRequest.getId()).orElse(null);

        if (user == null) {
            System.out.println("用户不存在: " + loginRequest.getId());
            return false;
        }

        System.out.println("验证密码 - 输入密码: " + loginRequest.getPassword());
        System.out.println("验证密码 - 存储密码: " + user.getPasswordHash());

        // 检查密码是否为空
        if (user.getPasswordHash() == null || loginRequest.getPassword() == null) {
            System.out.println("密码为空");
            return false;
        }

        boolean passwordMatch;
        
        /**
         * 检查存储的密码格式并执行相应的验证逻辑
         * - 如果密码是Argon2哈希格式（以$argon2开头），使用Argon2算法验证
         * - 如果是明文密码，直接比较明文，验证成功后自动升级为哈希密码
         */
        if (user.getPasswordHash().startsWith("$argon2")) {
            // 使用Argon2算法验证密码哈希
            passwordMatch = PasswordUtil.verifyPassword(user.getPasswordHash(), loginRequest.getPassword());
            System.out.println("Argon2哈希验证结果: " + passwordMatch);
        } else {
            // 处理明文密码的情况 - 直接比较明文
            passwordMatch = user.getPasswordHash().equals(loginRequest.getPassword());
            System.out.println("明文密码比较结果: " + passwordMatch);
            
            /**
             * 自动密码升级机制
             * - 仅在密码验证成功且用户已激活时执行
             * - 将明文密码升级为Argon2哈希密码
             * - 自动保存到数据库，提高系统安全性
             */
            if (passwordMatch && user.getIsActive() == 1) {
                System.out.println("检测到明文密码，自动升级为哈希密码");
                String hashedPassword = PasswordUtil.hashPassword(loginRequest.getPassword());
                user.setPasswordHash(hashedPassword);
                userRepository.save(user);
                System.out.println("密码已升级为哈希: " + hashedPassword);
            }
        }

        if (!passwordMatch) {
            System.out.println("密码验证失败");
            return false;
        }

        // 检查用户是否激活
        if (user.getIsActive() != 1) {
            System.out.println("用户未激活: " + user.getId());
            return false;
        }

        System.out.println("登录成功");
        return true;
    }

    /**
     * 登录请求DTO
     */
    public static class LoginRequest {
        private String id;
        private String password;

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * 修改用户密码
     * 
     * @param id 用户ID (路径参数)
     * @param newPassword 新密码
     * @return 修改结果
     */
    @PutMapping("/{id}/password")
    public boolean changePassword(@PathVariable String id, @RequestBody String newPassword) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            System.out.println("用户不存在: " + id);
            return false;
        }

        // 使用Argon2加密新密码
        String hashedPassword = PasswordUtil.hashPassword(newPassword);
        user.setPasswordHash(hashedPassword);
        
        // 保存并立即刷新到数据库
        userRepository.saveAndFlush(user);
        System.out.println("密码修改成功，新密码哈希: " + hashedPassword);
        return true;
    }
}
