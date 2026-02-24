package com.example.medaiassistant;

import com.example.medaiassistant.model.TestEntity;
import com.example.medaiassistant.model.User;
import com.example.medaiassistant.repository.TestRepository;
import com.example.medaiassistant.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HomeController {

    private final TestRepository testRepository;
    private final UserRepository userRepository;

    public HomeController(TestRepository testRepository, UserRepository userRepository) {
        this.testRepository = testRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String home() {
        return "Medical AI Assistant Backend is running!";
    }

    @PostMapping("/test")
    public TestEntity createTest(@RequestBody TestEntity testEntity) {
        return testRepository.save(testEntity);
    }

    @GetMapping("/test/{id}")
    public TestEntity getTest(@PathVariable Long id) {
        return testRepository.findById(id).orElse(null);
    }

    @GetMapping("/db-status")
    public String checkDbStatus() {
        try {
            testRepository.count();
            return "Database connection is active";
        } catch (Exception e) {
            return "Database connection error: " + e.getMessage();
        }
    }

    @GetMapping("/users")
    public Iterable<User> getAllUsers() {
        return userRepository.findAll();
    }
}
