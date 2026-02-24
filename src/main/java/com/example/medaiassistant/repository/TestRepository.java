package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.TestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestRepository extends JpaRepository<TestEntity, Long> {
}
