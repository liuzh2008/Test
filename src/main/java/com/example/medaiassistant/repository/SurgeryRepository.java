package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.Surgery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface SurgeryRepository extends JpaRepository<Surgery, Integer> {
    List<Surgery> findByPatientId(String patientId);
}
