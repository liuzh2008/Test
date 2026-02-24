package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.DicInput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DicInputRepository extends JpaRepository<DicInput, Long> {
}
