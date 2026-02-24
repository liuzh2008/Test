package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    
    @Query("SELECT u FROM User u WHERE u.id = :id")
    User getFreshUser(@Param("id") String id);
    
    Optional<User> findByUsername(String username);
}
